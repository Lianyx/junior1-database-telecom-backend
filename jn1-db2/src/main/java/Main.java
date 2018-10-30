import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;

public class Main {
    private static final double DELTA = 0.00001;

    private static Connection connection;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");

            connection = DriverManager
                    .getConnection("jdbc:mysql://localhost/" + Telecom.DB_NAME,
                            Telecom.USERNAME, Telecom.PASSWORD);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static void executeSql(String str) {
        try {
            Statement statement = connection.createStatement();
            statement.execute(str);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void clearAll() {
        Stream.of(
                "delete from basic;",
                "delete from combo;",
                "delete from operation;",
                "delete from user_calls;",
                "delete from user_texts;",
                "delete from user_traffic;"
        ).forEach(Main::executeSql);
    }

    /*
    * 测试monthBill（查看月帐單），callDetail（通话情况下的资费生成）, trafficDetail（使用流量情况下的资费生成）三個方法
    * 這個用例构造的用户使用情形如下：

    基础资费
    0.5, 0.1, ￥2/M, ￥5/M

    优惠套餐
    1：20元，100分钟电话
    2：10元，200条短信
    3：30元，1000M本地流量
    4：50元，500M本地，2000M国内流量

    用户"189"很早之前订了套餐1。
    1月3日订了套餐4和立即生效；1月4日使用本地流量300，使用本地500, 使用国内流量2000。打電话100分种。發3条短信
    测试1：测trafficDetail, 此時只有最後一次300M的国内流量收费（1500元）
    後用户立即取消套餐4，再查
    测试2：测trafficDetail, 三次都有，分別为600, 1000，10000
    後用户立即重新订购此套餐。
    测试3：测trafficDetail, 结果与测试2仍相同。
    测试4：测callDetail, 不收钱。
    测试5：测monthBill，text要0.3元





    對於其他方法的測試：
    // 這個測試只是證明沒有异常
    // 在正常分支最後一句加断點，運行到此處時手動在數據庫中查看，插入都是成功的。

    * */
    public static void main(String[] args) throws Exception {
        clearAll();
        Stream.of(("" +
                "INSERT INTO basic VALUES ('1998-01-01 00:00:00', 0.5, 0.1, 2, 5);\n" +
                "INSERT INTO combo VALUES (1, 20, 100, 0, 0, 0);\n" +
                "INSERT INTO combo VALUES (2, 10, 0, 200, 0, 0);\n" +
                "INSERT INTO combo VALUES (3, 10, 0, 0, 1000, 0);\n" +
                "INSERT INTO combo VALUES (4, 50, 0, 0, 500, 2000);\n" +
                "INSERT INTO operation VALUES (1, '189', 'order', 1, '1998-01-01 00:00:00', '1998-01-01 00:00:00');\n" +
                "INSERT INTO operation VALUES (2, '189', 'order', 4, '2018-01-03 00:00:00', '2018-01-03 00:00:00');\n" +
                "INSERT INTO user_traffic VALUES ('189', '2018-01-04 00:00:00', 300, 'local');\n" +
                "INSERT INTO user_traffic VALUES ('189', '2018-01-04 00:00:02', 2000, 'domestic');\n" +
                "INSERT INTO user_traffic VALUES ('189', '2018-01-04 00:00:01', 500, 'local');\n" +
                "INSERT INTO user_calls VALUES ('189', '2018-01-04 00:00:02', 100);\n" +
                "INSERT INTO user_texts VALUES ('189', '2018-01-04 00:00:02');\n" +
                "INSERT INTO user_texts VALUES ('189', '2018-01-04 00:00:05');\n" +
                "INSERT INTO user_texts VALUES ('189', '2018-01-04 00:00:04');"
        ).split("\n")).forEach(Main::executeSql);

        long before;

        // 1
        before = System.currentTimeMillis();
        List<Telecom.TrafficDetail> trafficDetails = Telecom.trafficDetail("189", Telecom.destringify("2018-01-01 00:00:00"),
                Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(3, trafficDetails.size());
        Assert.assertEquals(0, trafficDetails.get(0)._fee, DELTA);
        Assert.assertEquals(0, trafficDetails.get(1)._fee, DELTA);
        Assert.assertEquals(1500, trafficDetails.get(2)._fee, DELTA);
        System.out.println(trafficDetails);
        System.out.println("operation1 time = " + -(before - System.currentTimeMillis()));
        System.out.println();

        // 2
        executeSql("INSERT INTO operation VALUES (3, '189', 'cancel', 4, '2018-01-05 00:00:00', '2018-01-05 00:00:00');");

        before = System.currentTimeMillis();
        trafficDetails = Telecom.trafficDetail("189", Telecom.destringify("2018-01-01 00:00:00"),
                Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(600, trafficDetails.get(0)._fee, DELTA);
        Assert.assertEquals(1000, trafficDetails.get(1)._fee, DELTA);
        Assert.assertEquals(10000, trafficDetails.get(2)._fee, DELTA);
        System.out.println(trafficDetails);
        System.out.println("operation2 time = " + -(before - System.currentTimeMillis()));
        System.out.println();

        // 3
        executeSql("INSERT INTO operation VALUES (4, '189', 'order', 4, '2018-01-05 00:00:01', '2018-01-05 00:00:01');");

        before = System.currentTimeMillis();
        trafficDetails = Telecom.trafficDetail("189", Telecom.destringify("2018-01-01 00:00:00"),
                Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(600, trafficDetails.get(0)._fee, DELTA);
        Assert.assertEquals(1000, trafficDetails.get(1)._fee, DELTA);
        Assert.assertEquals(10000, trafficDetails.get(2)._fee, DELTA);
        System.out.println(trafficDetails);
        System.out.println("operation3 time = " + -(before - System.currentTimeMillis()));
        System.out.println();

        // 4
        before = System.currentTimeMillis();
        List<Telecom.CallDetail> callDetails = Telecom.callDetail("189", Telecom.destringify("2018-01-01 00:00:00"),
                Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(0, callDetails.get(0)._fee, DELTA);
        System.out.println(callDetails);
        System.out.println("operation4 time = " + -(before - System.currentTimeMillis()));
        System.out.println();

        // 5
        before = System.currentTimeMillis();
        Telecom.MonthBill monthBill = Telecom.monthBill("189", 2018, 1);
        Assert.assertEquals(0.3, monthBill.textFee, DELTA);
        System.out.println(monthBill);
        System.out.println("operation5 time = " + -(before - System.currentTimeMillis()));
        System.out.println();

        try {
            Telecom.addBasic(2018, 1, 1, 2, 3, 4);
            Telecom.addCombo(100, 100, 100, 100, 100);
            Telecom.call("981", LocalDateTime.now(), 13);
            Telecom.text("981", LocalDateTime.now());
            Telecom.useDomesticTraffic("981", LocalDateTime.now(), 5);
            Telecom.useLocalTraffic("981", LocalDateTime.now(), 24);

            // 6
            before = System.currentTimeMillis();
            Telecom.order("981", 1, true);
            Telecom.cancel("981", 2, true);
            System.out.println("operation6 time = " + -(before - System.currentTimeMillis()));
            System.out.println();

            // 7
            before = System.currentTimeMillis();
            List<Telecom.Combo> combos = Telecom.comboInfo();
            List<Telecom.OperationInfo> operationInfos = Telecom.operationInfo("981");
            System.out.println(combos);
            System.out.println(operationInfos);
            System.out.println("operation7 time = " + -(before - System.currentTimeMillis()));
            System.out.println();
            IntStream.rangeClosed(0, 20).forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals("Exception occurs", true, false);
        }
    }
}
