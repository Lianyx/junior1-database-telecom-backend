import java.lang.reflect.Field;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

public class Telecom {
    public static final String DB_NAME = "jn1_telecom", USERNAME = "root", PASSWORD = "123456";

    private static PreparedStatement operationSql, basicSql, comboSql,
            callSql, textSql, trafficSql,
            selectCallSql, selectTextSql, selectTrafficSql,
            selectBasicSql, selectComboSql, comboInfoSql;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Driver loaded");

            Connection connection = DriverManager
                    .getConnection("jdbc:mysql://localhost/" + DB_NAME,
                            USERNAME, PASSWORD);
            System.out.println("connected");

            operationSql = connection.prepareStatement(
                    "INSERT INTO operation(username, operation, combo_id, operation_time, effectuate_time) " +
                            "VALUES(?, ?, ?, ?, ?);"
            );
            callSql = connection.prepareStatement(
                    "INSERT INTO user_calls VALUES(?, ?, ?);")
            ;
            textSql = connection.prepareStatement(
                    "INSERT INTO user_texts VALUES(?, ?);"
            );
            trafficSql = connection.prepareStatement(
                    "INSERT INTO user_traffic VALUES (?, ?, ?, ?);"
            );
            basicSql = connection.prepareStatement(
                    "INSERT INTO basic VALUES(?, ?, ?, ?, ?);"
            );
            comboSql = connection.prepareStatement(
                    "INSERT INTO combo(cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic) " +
                            "VALUES(?, ?, ?, ?, ?);"
            );


            comboInfoSql = connection.prepareStatement(
                    "SELECT * from (SELECT * FROM operation WHERE username = ?) as A JOIN combo ON A.combo_id = combo.id;"
            );
            selectCallSql = connection.prepareStatement(
                    "SELECT * FROM user_calls WHERE username = ? AND ? <= begin_time AND begin_time < ? ORDER BY begin_time ASC;"
            );
            selectTextSql = connection.prepareStatement(
                    "SELECT * FROM user_texts WHERE username = ? AND ? <= send_time AND send_time < ? ORDER BY send_time ASC;"
            );
            selectTrafficSql = connection.prepareStatement(
                    "SELECT * FROM user_traffic WHERE username = ? AND ? <= request_time AND request_time < ? ORDER BY request_time ASC;"
            );
            selectBasicSql = connection.prepareStatement(
                    "SELECT * FROM basic WHERE begin_time = ALL (SELECT MAX(begin_time) FROM basic WHERE begin_time <= ?);"
            );
            selectComboSql = connection.prepareStatement(
                    "" +
                            "SELECT id, effectuate_time, cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic\n" +
                            "FROM (SELECT combo_id, effectuate_time FROM operation o1\n" +
                            "      WHERE effectuate_time < ? AND operation = 'order'\n" +
                            "            AND NOT EXISTS(SELECT * FROM operation o2\n" +
                            "                           WHERE effectuate_time < ? AND operation = 'cancel'\n" +
                            "                                 AND o1.combo_id = o2.combo_id\n" +
                            "                                 AND o1.operation_time <= o2.operation_time)\n" +
                            ") AS O, combo\n" +
                            "WHERE O.combo_id = combo.id\n" +
                            "ORDER BY effectuate_time ASC;"
            );
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        // 反正整個代码也沒有考虑exception。。。
    }

    /**
     * convenient methods
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String stringify(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }

    public static LocalDateTime destringify(String str) {
        if (str.contains(".")) str = str.substring(0, str.length() - 2);
        return LocalDateTime.parse(str, formatter);
    }

    private static LocalDateTime thisMonth(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate().with(TemporalAdjusters.firstDayOfMonth()), LocalTime.MIN);
    }

    private static LocalDateTime nextMonth(LocalDateTime localDateTime) {
        return LocalDateTime.of(localDateTime.toLocalDate().with(TemporalAdjusters.firstDayOfNextMonth()), LocalTime.MIN);
    }

    private static boolean isBetween(LocalDateTime c, LocalDateTime beginTime, LocalDateTime endTime) {
        return beginTime.isBefore(c) && c.isBefore(endTime);
    }

    // a lot of thanks to https://stackoverflow.com/questions/11777103/set-parameters-dynamically-to-prepared-statement-in-jdbc
    private static void mapParams(PreparedStatement ps, Object... args) throws SQLException {
        // 并不检查問号和arg的数量一致
        int i = 1;
        for (Object arg : args) {
            if (arg instanceof LocalDateTime) {
                ps.setString(i++, stringify((LocalDateTime) arg));
            } else if (arg instanceof Integer) {
                ps.setInt(i++, (Integer) arg);
            } else if (arg instanceof Double) {
                ps.setDouble(i++, (Double) arg);
            } else {
                ps.setString(i++, (String) arg);
            }
        }
    }

    private static void mapThenExecute(PreparedStatement ps, Object... args) throws SQLException {
        mapParams(ps, args);
        ps.execute();
    }

    // a lot of thanks to https://stackoverflow.com/questions/21956042/mapping-a-jdbc-resultset-to-an-object
    private static <T> List<T> mapThenExecuteQuery(Class<T> clazz, PreparedStatement ps, Object... args) throws Exception {
        mapParams(ps, args);
        ResultSet resultSet = ps.executeQuery();

        List<Field> fields = Arrays.asList(clazz.getDeclaredFields());
        for (Field field : fields) field.setAccessible(true);

        List<T> list = new ArrayList<>();
        while (resultSet.next()) {
            T dto = clazz.getConstructor().newInstance();

            for (Field field : fields) {
                String name = field.getName();
                if (name.startsWith("_")) continue;

                Class<?> type = field.getType();
                if (type.equals(LocalDateTime.class)) {
                    field.set(dto, destringify(resultSet.getString(name)));
                } else if (type.equals(Integer.TYPE)) {
                    field.set(dto, resultSet.getInt(name));
                } else if (type.equals(Double.TYPE)) {
                    field.set(dto, resultSet.getDouble(name));
                } else {
                    field.set(dto, resultSet.getString(name));
                }
            }

            list.add(dto);
        }
        return list;
    }

    /**
     * public methods
     */
    public static void addBasic(int year, int month, double call_cost_per_min, double text_cost_per_message, double local_traffic_cost_per_mb, double domestic_traffic_cost_per_mb) throws Exception {
        mapThenExecute(basicSql, LocalDateTime.of(LocalDate.of(year, month, 1), LocalTime.MIN), call_cost_per_min, text_cost_per_message, local_traffic_cost_per_mb, domestic_traffic_cost_per_mb);
    }

    public static void addCombo(double cost_per_month, double free_call_min, int free_messages, double free_local_traffic, double free_domestic_traffic) throws Exception {
        mapThenExecute(comboSql, cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic);
    }

    public static void order(String username, int combo_id, boolean rightAway) throws Exception { // 不检查有沒有这個combId
        mapThenExecute(operationSql, username, "order", combo_id, LocalDateTime.now(), rightAway ? LocalDateTime.now() : nextMonth(LocalDateTime.now()));
    }

    public static void cancel(String username, int combo_id, boolean rightAway) throws Exception { // 沒检查有之前沒有购买這個id的
        mapThenExecute(operationSql, username, "cancel", combo_id, LocalDateTime.now(), rightAway ? LocalDateTime.now() : nextMonth(LocalDateTime.now()));
    }

    public static void call(String username, LocalDateTime begin_time, double duration) throws Exception {
        mapThenExecute(callSql, username, begin_time, duration);
    }

    public static void text(String username, LocalDateTime send_time) throws Exception {
        mapThenExecute(textSql, username, send_time);
    }

    public static void useLocalTraffic(String username, LocalDateTime request_time, double mb) throws Exception {
        mapThenExecute(trafficSql, username, request_time, mb, "local");
    }

    public static void useDomesticTraffic(String username, LocalDateTime request_time, double mb) throws Exception {
        mapThenExecute(trafficSql, username, request_time, mb, "domestic");
    }

    public static List<Combo> comboInfo(String username) throws Exception {
        return mapThenExecuteQuery(Combo.class, comboInfoSql, username);
    }

    public static MonthBill monthBill(String username, int year, int month) throws Exception {
        LocalDateTime begin = LocalDateTime.of(LocalDate.of(year, month, 1), LocalTime.MIN),
                end = nextMonth(begin);

        Basic basic = mapThenExecuteQuery(Basic.class, selectBasicSql, begin).get(0);
        List<Combo> combos = mapThenExecuteQuery(Combo.class, selectComboSql, end, end);

        // text還是需要重复那個逻輯，又有点想提出來了
        List<TextDetail> textDetails = mapThenExecuteQuery(TextDetail.class, selectTextSql, username, begin, end);
        int free = 0, combo_ptr = 0, chargeable = 0;
        for (TextDetail l : textDetails) {
            while (combo_ptr < combos.size() && combos.get(combo_ptr).effectuate_time.isBefore(l.send_time)) {
                free += combos.get(combo_ptr++).free_messages;
            }
            if (free > 0) {
                free--;
            } else {
                chargeable++;
            }
        }

        MonthBill result = new MonthBill();
        result.username = username;
        result.combos = combos;
        result.callFee = callDetail(username, begin, end).stream().mapToDouble(c -> c._fee).sum();
        result.textFee = chargeable * basic.text_cost_per_message;
        result.trafficFee = trafficDetail(username, begin, end).stream().mapToDouble(c -> c._fee).sum();
        result.comboFee = combos.stream().mapToDouble(c -> c.cost_per_month).sum();
        result.total = result.callFee + result.textFee + result.trafficFee + result.comboFee;

        return result;
    }

    public static List<CallDetail> callDetail(String username, LocalDateTime beginTime, LocalDateTime endTime) throws Exception {
        List<CallDetail> result = new ArrayList<>();
        for (LocalDateTime month = thisMonth(beginTime);
             month.compareTo(thisMonth(endTime)) <= 0;
             month = nextMonth(month)) {

            List<CallDetail> detailsThisMonth = mapThenExecuteQuery(CallDetail.class, selectCallSql, username, month, nextMonth(month));
            List<Combo> combos = mapThenExecuteQuery(Combo.class, selectComboSql, nextMonth(month), nextMonth(month));
            final double basicCost = mapThenExecuteQuery(Basic.class, selectBasicSql, month).get(0).call_cost_per_min;
            double freeMinutes = 0;
            int combo_ptr = 0;

            for (CallDetail l : detailsThisMonth) {
                while (combo_ptr < combos.size() && combos.get(combo_ptr).effectuate_time.isBefore(l.begin_time)) {
                    freeMinutes += combos.get(combo_ptr++).free_call_min;
                }
                double amount = l.duration;
                if (freeMinutes >= amount) {
                    l._fee = 0;
                    freeMinutes -= amount;
                } else {
                    double chargeable = amount - freeMinutes;
                    l._fee = chargeable * basicCost;
                    freeMinutes = 0;
                }
            }

            detailsThisMonth = detailsThisMonth.stream().filter(c -> isBetween(c.begin_time, beginTime, endTime)).collect(Collectors.toList());

            result.addAll(detailsThisMonth);
        }
        return result;
    }

    public static List<TrafficDetail> trafficDetail(String username, LocalDateTime beginTime, LocalDateTime endTime) throws Exception {
        List<TrafficDetail> result = new ArrayList<>();

        for (LocalDateTime month = thisMonth(beginTime);
             month.compareTo(thisMonth(endTime)) <= 0;
             month = nextMonth(month)) {

            List<TrafficDetail> detailsThisMonth = mapThenExecuteQuery(TrafficDetail.class, selectTrafficSql, username, month, nextMonth(month));
            List<Combo> combos = mapThenExecuteQuery(Combo.class, selectComboSql, nextMonth(month), nextMonth(month));
            Basic basic = mapThenExecuteQuery(Basic.class, selectBasicSql, month).get(0);
            final double basicLocalCost = basic.local_traffic_cost_per_mb, basicDomesticCost = basic.domestic_traffic_cost_per_mb;
            double localFree = 0, domesticFree = 0;
            int combo_ptr = 0;

            for (TrafficDetail l : detailsThisMonth) {
                while (combo_ptr < combos.size() && combos.get(combo_ptr).effectuate_time.isBefore(l.request_time)) {
                    Combo combo = combos.get(combo_ptr++);
                    localFree += combo.free_local_traffic;
                    domesticFree += combo.free_domestic_traffic;
                }

                double amount = l.mb;
                boolean isLocal;
                if (isLocal = "local".equals(l.type)) {
                    if (amount <= localFree) {
                        l._fee = 0;
                        localFree -= amount;
                        continue;
                    } else {
                        amount = amount - localFree;
                        localFree = 0;
                    }
                }

                if (amount <= domesticFree) {
                    l._fee = 0;
                    domesticFree -= amount;
                } else {
                    double left = amount - domesticFree;
                    domesticFree = 0;
                    l._fee = left * (isLocal ? basicLocalCost : basicDomesticCost);
                }
            }

            detailsThisMonth = detailsThisMonth.stream().filter(t -> isBetween(t.request_time, beginTime, endTime)).collect(Collectors.toList());

            result.addAll(detailsThisMonth);
        }
        return result;
    }

    public static class Detail {
        double _fee;
    }

    public static class CallDetail extends Detail {
        LocalDateTime begin_time;
        double duration;
    }

    public static class TextDetail extends Detail {
        LocalDateTime send_time;
    }

    public static class TrafficDetail extends Detail {
        LocalDateTime request_time;
        double mb;
        String type;
    }

    public static class Basic {
        double call_cost_per_min, text_cost_per_message, local_traffic_cost_per_mb, domestic_traffic_cost_per_mb;
    }

    public static class Combo {
        int id, free_messages;
        double free_call_min, cost_per_month, free_local_traffic, free_domestic_traffic;
        LocalDateTime effectuate_time;
    }

    public static class MonthBill {
        String username;
        List<Combo> combos;
        double callFee, textFee, trafficFee, comboFee, total;
    }

    public static void main(String[] args) throws Exception {
        System.out.println();
    }
}
