import java.lang.reflect.Field;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Telecom {
    private static final String DB_NAME = "jn1_telecom", USERNAME = "root", PASSWORD = "123456";

    private static PreparedStatement operationSql, basicSql, comboSql,
            callSql, textSql, localTrafficSql, domesticTrafficSql,
            selectCallSql, selectTextSql, selectLocalSql, selectDomesticSql,
            selectBasicSql, selectComboSql, comboInfoSql;

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            System.out.println("Driver loaded");

            Connection connection = DriverManager
                    .getConnection("jdbc:mysql://localhost/" + DB_NAME,
                            USERNAME, PASSWORD);
            System.out.println("connected");

            operationSql = connection.prepareStatement("INSERT INTO operation(username, operation, combo_id, operation_time, effectuate_month) VALUES(?, ?, ?, ?, ?);");
            callSql = connection.prepareStatement("INSERT INTO user_calls VALUES(?, ?, ?);");
            textSql = connection.prepareStatement("INSERT INTO user_texts VALUES(?, ?);");
            localTrafficSql = connection.prepareStatement("INSERT INTO user_local_traffic VALUES(?, ?, ?);");
            domesticTrafficSql = connection.prepareStatement("INSERT INTO user_domestic_traffic VALUES(?, ?, ?);");
            basicSql = connection.prepareStatement("INSERT INTO basic VALUES(?, ?, ?, ?, ?);");
            comboSql = connection.prepareStatement("INSERT INTO combo(cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic) VALUES(?, ?, ?, ?, ?);");

            comboInfoSql = connection.prepareStatement("SELECT * FROM operation WHERE username = ?;");
            selectCallSql = connection.prepareStatement("SELECT * FROM user_calls WHERE username = ? AND (? < begin_time < ? OR ? < end_time < ?);");
            selectTextSql = connection.prepareStatement("SELECT * FROM user_texts WHERE username = ? AND ? < send_time < ?;");
            selectLocalSql = connection.prepareStatement("SELECT * FROM user_local_traffic WHERE username = ? AND ? < request_time < ?;");
            selectDomesticSql = connection.prepareStatement("SELECT * FROM user_domestic_traffic WHERE username = ? AND ? < request_time < ?;");
            selectBasicSql = connection.prepareStatement("SELECT * FROM basic WHERE begin_time = ALL (SELECT MAX(begin_time) FROM basic WHERE begin_time < ?);");
            selectComboSql = connection.prepareStatement("SELECT * FROM combo\n" +
                    "WHERE id IN (SELECT combo_id FROM operation o1\n" +
                    "             WHERE effectuate_month <= ? AND operation = 'order'\n" +
                    "                   AND NOT EXISTS(SELECT * FROM operation o2\n" +
                    "                                  WHERE effectuate_month <= ? AND o1.combo_id = o2.combo_id\n" +
                    "                                        AND o1.operation_time < o2.operation_time)");
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
        // 反正整個代码也沒有考虑exception。。。
    }

    /**
     * private methods
     */
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static String stringify(LocalDateTime dateTime) {
        return dateTime.format(formatter);
    }

    private static LocalDateTime destringify(String str) {
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
    private static <T> List<T> mapThenExecuteQuery(Class<T> clazz, PreparedStatement ps, Object... args) {
        try {
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
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static List<CallDetail> queryCallDetails(String username, LocalDateTime begin, LocalDateTime end) {
        try {
            List<CallDetail> callDetails = mapThenExecuteQuery(CallDetail.class, selectCallSql, username, begin, end, begin, end);
            callDetails.sort(Comparator.comparing(p -> p.begin_time));
            callDetails.get(0).begin_time = callDetails.get(0).begin_time.isBefore(begin) ? begin : callDetails.get(0).begin_time;
            callDetails.get(callDetails.size() - 1).end_time = callDetails.get(callDetails.size() - 1).end_time.isAfter(end) ? end : callDetails.get(callDetails.size() - 1).end_time;
            return callDetails;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    private static Combo combineCombos(List<Combo> combos) {
        Combo result = new Combo();
        result.free_call_min = combos.stream().mapToInt(c -> c.free_call_min).sum();
        result.free_messages = combos.stream().mapToInt(c -> c.free_messages).sum();
        result.free_local_traffic = combos.stream().mapToDouble(c -> c.free_local_traffic).sum();
        result.free_domestic_traffic = combos.stream().mapToDouble(c -> c.free_domestic_traffic).sum();
        return result;
    }

    // TODO 這裡int转double感覺还是不太好
    private static <T extends Detail> List<T> detailReport(LocalDateTime beginTime, LocalDateTime endTime,
                                                           Function<LocalDateTime, List<T>> getDetailsThisMonth, Function<Combo, Double> getTotalFree,
                                                           Function<Basic, Double> getBasicCost, Function<T, Double> getAmount, Predicate<T> predicate) throws Exception {
        List<T> result = null;
        for (LocalDateTime month = thisMonth(beginTime);
             month.compareTo(thisMonth(endTime)) <= 0;
             month = nextMonth(month)) {
            List<T> detailsThisMonth = getDetailsThisMonth.apply(month);

            final double totalFree = getTotalFree.apply(combineCombos(mapThenExecuteQuery(Combo.class, selectComboSql, month, month)));
            final double basicCost = getBasicCost.apply(mapThenExecuteQuery(Basic.class, selectBasicSql, month).get(0));
            double total = 0;

            for (T l : detailsThisMonth) {
                double amount = getAmount.apply(l);
                if (total >= totalFree) {
                    l._fee = amount * basicCost;
                } else if (total + amount <= totalFree) {
                    l._fee = 0;
                } else {
                    l._fee = (total + amount - totalFree) * basicCost;
                }
                total += amount;
            }

            detailsThisMonth = detailsThisMonth.stream().filter(predicate).collect(Collectors.toList());

            if (result == null) {
                result = detailsThisMonth;
            } else {
                result.addAll(detailsThisMonth);
            }
        }
        return result;
    }

    /**
     * public methods
     */
    public static void addBasic(LocalDateTime begin_time, double call_cost_per_min, double text_cost_per_message, double local_traffic_cost_per_mb, double domestic_traffic_cost_per_mb) throws Exception {
        mapThenExecute(basicSql, begin_time, call_cost_per_min, text_cost_per_message, local_traffic_cost_per_mb, domestic_traffic_cost_per_mb);
    }

    public static void addCombo(double cost_per_month, double free_call_min, double free_messages, double free_local_traffic, double free_domestic_traffic) throws Exception {
        mapThenExecute(comboSql, cost_per_month, free_call_min, free_messages, free_local_traffic, free_domestic_traffic);
    }

    public static void order(String username, int combo_id) throws Exception { // order 只能next month吧；不检查有沒有这個combId
        mapThenExecute(operationSql, username, "order", combo_id, LocalDateTime.now(), nextMonth(LocalDateTime.now()));
    }

    public static void cancel(String username, int combo_id, boolean rightAway) throws Exception { // 沒检查有之前沒有购买這個id的
        mapThenExecute(operationSql, username, "cancel", combo_id, LocalDateTime.now(), rightAway ? thisMonth(LocalDateTime.now()) : nextMonth(LocalDateTime.now()));
    }

    public static void call(String username, LocalDateTime begin_time, LocalDateTime end_time) throws Exception {
        mapThenExecute(callSql, username, begin_time, end_time);
    }

    public static void text(String username, LocalDateTime send_time) throws Exception {
        mapThenExecute(textSql, username, send_time);
    }

    public static void useLocalTraffic(String username, LocalDateTime request_time, double mb) throws Exception {
        mapThenExecute(localTrafficSql, username, request_time, mb);
    }

    public static void useDomesticTraffic(String username, LocalDateTime request_time, double mb) throws Exception {
        mapThenExecute(domesticTrafficSql, username, request_time, mb);
    }

    public static List<Combo> comboInfo(String username) throws Exception {
        return mapThenExecuteQuery(Combo.class, comboInfoSql, username);
    }

    public static MonthBill monthBill(String username, int year, int month) throws Exception {
        LocalDateTime begin = thisMonth(LocalDateTime.of(LocalDate.of(year, month, 0), LocalTime.MIN)),
                end = nextMonth(begin);

        Basic basic = mapThenExecuteQuery(Basic.class, selectBasicSql, begin).get(0);

        List<Combo> combos = mapThenExecuteQuery(Combo.class, selectComboSql, begin, begin);
        Combo combinedCombo = combineCombos(combos);

        int callTime = queryCallDetails(username, begin, end)
                .stream().mapToInt(r -> (int) ChronoUnit.MINUTES.between(r.begin_time, r.end_time) + 1).sum();
        int textCount = mapThenExecuteQuery(TextDetail.class, selectTextSql, username, begin, end)
                .size();
        double localTraffic = mapThenExecuteQuery(TrafficDetail.class, selectLocalSql, username, begin, end)
                .stream().mapToDouble(d -> d.mb).sum();
        double domesticTraffic = mapThenExecuteQuery(TrafficDetail.class, selectDomesticSql, username, begin, end)
                .stream().mapToDouble(d -> d.mb).sum();

        MonthBill result = new MonthBill();
        result.username = username;
        result.combos = combos;
        result.callItem = new Item(callTime, basic.call_cost_per_min, combinedCombo.free_call_min);
        result.textItem = new Item(textCount, basic.text_cost_per_message, combinedCombo.free_messages);
        result.localItem = new Item(localTraffic, basic.local_traffic_cost_per_mb, combinedCombo.free_local_traffic);
        result.domesticItem = new Item(domesticTraffic, basic.domestic_traffic_cost_per_mb, combinedCombo.free_domestic_traffic);
        result.total = combos.stream().mapToDouble(c -> c.cost_per_month).sum()
                + result.callItem.fee + result.textItem.fee + result.localItem.fee + result.domesticItem.fee;
        return result;
    }

    public static List<CallDetail> callDetail(String username, LocalDateTime beginTime, LocalDateTime endTime) throws Exception {
        return detailReport(beginTime, endTime, (month) -> queryCallDetails(username, month, nextMonth(month)),
                c -> (double)c.free_call_min, b -> b.call_cost_per_min, c -> (double)ChronoUnit.MINUTES.between(c.begin_time, c.end_time) + 1,
                c -> isBetween(c.begin_time, beginTime, endTime) || isBetween(c.end_time, beginTime, endTime));
    }

    public static TrafficDetailReport trafficDetailReport(String username, LocalDateTime beginTime, LocalDateTime endTime) throws Exception {
        TrafficDetailReport result = new TrafficDetailReport();

        result.local = detailReport(beginTime, endTime,
                (month) -> mapThenExecuteQuery(TrafficDetail.class, selectLocalSql, username, month, nextMonth(month)),
                c -> c.free_local_traffic, b -> b.local_traffic_cost_per_mb, t -> t.mb, t -> isBetween(t.request_time, beginTime, endTime));

        result.domestic = detailReport(beginTime, endTime,
                (month) -> mapThenExecuteQuery(TrafficDetail.class, selectDomesticSql, username, month, nextMonth(month)),
                c -> c.free_domestic_traffic, b -> b.domestic_traffic_cost_per_mb, t -> t.mb, t -> isBetween(t.request_time, beginTime, endTime));

        return result;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(ChronoUnit.MINUTES.between(LocalDateTime.now(), LocalDateTime.now().plusMinutes(1).minusSeconds(10)));
    }

    static class Detail {
        double _fee;
    }

    static class CallDetail extends Detail {
        LocalDateTime begin_time, end_time;
    }

    static class TextDetail extends Detail {
        LocalDateTime send_time;
    }

    static class TrafficDetail extends Detail {
        LocalDateTime request_time;
        double mb;
    }

    static class Basic {
        double call_cost_per_min, text_cost_per_message, local_traffic_cost_per_mb, domestic_traffic_cost_per_mb;
    }

    static class Combo {
        int id, free_call_min, free_messages;
        double cost_per_month, free_local_traffic, free_domestic_traffic;
    }

    static class Item {
        double consume, excess, fee;

        Item(double consume, double fee_per, double total_free) {
            this.consume = consume;
            this.excess = total_free >= consume ? 0 : consume - total_free;
            this.fee = this.excess * fee_per;
        }
    }

    static class MonthBill {
        String username;
        List<Combo> combos;
        Item callItem, textItem, localItem, domesticItem;
        double total;
    }

    static class TrafficDetailReport {
        List<TrafficDetail> local, domestic;
    }
}
