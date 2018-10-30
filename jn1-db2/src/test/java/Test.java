import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

public class Test {
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

    private void executeSql(String str) {
        try {
            Statement statement = connection.createStatement();
            statement.execute(str);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void insertData() {
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
        ).split("\n")).forEach(this::executeSql);
    }

    @After
    public void after() {
        clearAll();
    }

    private void clearAll() {
        Stream.of(
                "delete from basic;",
                "delete from combo;",
                "delete from operation;",
                "delete from user_calls;",
                "delete from user_texts;",
                "delete from user_traffic;"
        ).forEach(this::executeSql);
    }

    @org.junit.Test
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

    * */
    public void test1() throws Exception {
        // 1
        List<Telecom.TrafficDetail> trafficDetails = Telecom.trafficDetail("189", Telecom.destringify("2018-01-01 00:00:00"), Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(3, trafficDetails.size());
        Assert.assertEquals(0, trafficDetails.get(0)._fee, DELTA);
        Assert.assertEquals(0, trafficDetails.get(1)._fee, DELTA);
        Assert.assertEquals(1500, trafficDetails.get(2)._fee, DELTA);

        executeSql("INSERT INTO operation VALUES (3, '189', 'cancel', 4, '2018-01-05 00:00:00', '2018-01-05 00:00:00');");

        // 2
        trafficDetails = Telecom.trafficDetail("189", Telecom.destringify("2018-01-01 00:00:00"), Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(600, trafficDetails.get(0)._fee, DELTA);
        Assert.assertEquals(1000, trafficDetails.get(1)._fee, DELTA);
        Assert.assertEquals(10000, trafficDetails.get(2)._fee, DELTA);

        executeSql("INSERT INTO operation VALUES (4, '189', 'order', 4, '2018-01-05 00:00:01', '2018-01-05 00:00:01');");
        // 3
        trafficDetails = Telecom.trafficDetail("189", Telecom.destringify("2018-01-01 00:00:00"), Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(600, trafficDetails.get(0)._fee, DELTA);
        Assert.assertEquals(1000, trafficDetails.get(1)._fee, DELTA);
        Assert.assertEquals(10000, trafficDetails.get(2)._fee, DELTA);

        // 4
        List<Telecom.CallDetail> callDetails = Telecom.callDetail("189", Telecom.destringify("2018-01-01 00:00:00"), Telecom.destringify("2018-02-04 00:00:05"));
        Assert.assertEquals(0, callDetails.get(0)._fee, DELTA);

        // 5
        Telecom.MonthBill monthBill = Telecom.monthBill("189", 2018, 1);
        Assert.assertEquals(0.3, monthBill.textFee, DELTA);
    }

    @org.junit.Test
    // 這個測試只是證明沒有异常
    // 在正常分支最後一句加断點，運行到此處時手動在數據庫中查看，插入都是成功的。
    public void testInsert() {
        try {
            Telecom.addBasic(2018, 1, 1, 2, 3, 4);
            Telecom.addCombo(100, 100, 100, 100, 100);
            Telecom.call("981", LocalDateTime.now(), 13);
            Telecom.text("981", LocalDateTime.now());
            Telecom.useDomesticTraffic("981", LocalDateTime.now(), 5);
            Telecom.useLocalTraffic("981", LocalDateTime.now(), 24);
            Telecom.order("981", 1, true);
            Telecom.cancel("981", 2, true);
            List<Telecom.Combo> combos = Telecom.comboInfo("981");
            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertEquals("Exception occurs", true, false);
        }
    }
}
