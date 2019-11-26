package com.robot.easyframe.core;

import com.robot.easyframe.util.Convert;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 查询条件包装类
 *
 * @author luozhan
 * @date 2019-02
 */
@SuppressWarnings("Duplicates")
public class Query {
    public static final String LIKE = "like:";

    public static final String NOT_LIKE = "not-like:";

    public static final String IN = "in:";

    public static final String NOT_IN = "not-in:";

    public static final String IS_NULL = "is-null:";

    public static final String IS_NOT_NULL = "is-not-null:";

    public static final String GT = ">:";

    public static final String LT = "<:";

    public static final String GTE = ">=:";

    public static final String LTE = "<=:";

    public static final String BETWEEN = "between:";

    public static final String AND = "and:";

    public static final String OR = "or:";

    private static final String ASC = ":orderByAsc";

    private static final String DESC = ":orderByDesc";

    private static final String STR = "str:";

    private static final String DATE = "date:";

    private static final String TIME = "time:";

    private static final String NUM = "num:";

    private static final String SPACE = " ";


    /**
     * 查询条件：is null
     *
     * @return 查询条件表达式
     */
    public static String isNull() {
        return IS_NULL;
    }

    /**
     * 查询条件：is not null
     *
     * @return 查询条件表达式
     */
    public static String isNotNull() {
        return IS_NOT_NULL;
    }

    /**
     * 包装in参数
     * 示例：
     * in("1","2","3") => in ('1','2','3')
     * in("1,2,3")  =>  in ('1','2','3')
     * 注，如果传空字符串或空数组，将查不出任何数据：
     * in("")  =>  in ('')
     *
     * @param data String数组 or 以","分隔的String
     * @return 查询条件表达式
     */
    public static String in(String... data) {
        return IN + StringUtils.join(data, ",");
    }

    public static String in(List data) {
        return IN + StringUtils.join(data, ",");
    }

    /**
     * 包装not in参数
     * not-in("1","2","3") => not in ('1','2','3')
     * not-in("1,2,3") => not in ('1','2','3')
     *
     * @param data String数组 or 以","分隔的String，如果传空字符串或空数组，将忽略此条件
     * @return 查询条件表达式
     */
    public static String notIn(String... data) {
        if (data.length == 0 ||
                (data.length == 1 && data[0].length() == 0)) {
            return "";
        }
        return NOT_IN + StringUtils.join(data, ",");
    }

    /**
     * 不等于
     *
     * @param data String，如果是数字先转成String
     * @return
     */
    public static String ne(String data) {
        return notIn(data);
    }
    /**
     * 包装like参数
     * like("%1234%") => like '%1234%'
     * like("%1", "%2") => like '%1' or like '%2'
     * 注：
     * 1.如果传入字符串中不含"%"和"_"，则直接存原值，底层将用"="查询
     * 2.如果传入的字符串有空串，将被忽略
     *
     * @param data String或数组，使用"%"和"_"
     * @return 查询条件表达式
     */
    public static String like(String... data) {

        List<String> list = new ArrayList<>();
        for (String s : data) {
            if (!StringUtils.isEmpty(s)) {
                if (!s.contains("%") && !s.contains("_")) {
                    list.add(s);
                } else {
                    list.add(LIKE + s);
                }
            }
        }
        return Query.or(list.toArray(new String[0]));
    }

    /**
     * 包装like参数
     * notLike("%1234%") => not like '%1234%'
     * notLike("%1", "%2") =>  not like '%1' and  not like '%2'
     *
     * @param data String或数组，使用"%"和"_"
     * @return 查询条件表达式
     */
    public static String notLike(String... data) {
        List<String> list = new ArrayList<>();
        for (String s : data) {
            if (!StringUtils.isEmpty(s)) {
                list.add(NOT_LIKE + s);
            }
        }
        return Query.and(list.toArray(new String[0]));
    }

    /**
     * 设置前缀条件，多参数使用逻辑或连接
     * 若传一个空字符串或单元素的空字符串数组，将返回空字符串
     *
     * @param prefix
     * @return 查询条件表达式
     */
    public static String prefix(String... prefix) {
        List<String> list = new ArrayList<>();
        for (String s : prefix) {
            if (!StringUtils.isEmpty(s)) {
                list.add(Query.like(s + "%"));
            }
        }
        return Query.or(list.toArray(new String[0]));
    }

    /**
     * 设置后缀条件，多参数使用逻辑或连接
     *
     * @param suffix
     * @return 查询条件表达式
     */
    public static String suffix(String... suffix) {
        List<String> list = new ArrayList<>();
        for (String s : suffix) {
            if (!StringUtils.isEmpty(s)) {
                list.add(Query.like("%" + s));
            }
        }
        return Query.or(list.toArray(new String[0]));
    }

    /**
     * 设置包含条件，多参数使用逻辑或连接
     *
     * @param include
     * @return 查询条件表达式
     */
    public static String include(String... include) {
        List<String> list = new ArrayList<>();
        for (String s : include) {
            if (!StringUtils.isEmpty(s)) {
                list.add(Query.like("%" + s + "%"));
            }
        }
        return Query.or(list.toArray(new String[0]));
    }

    /**
     * 设置排除条件，多参数使用逻辑且连接
     *
     * @param exclude
     * @return 查询条件表达式
     */
    public static String exclude(String... exclude) {
        List<String> list = new ArrayList<>();
        for (String s : exclude) {
            if (!StringUtils.isEmpty(s)) {
                list.add(Query.notLike("%" + s + "%"));
            }
        }
        return Query.and(list.toArray(new String[0]));
    }

    /**
     * 包装小于等于参数
     * lt("123") => <='123'
     *
     * @param data String
     * @return 查询条件表达式
     */
    public static String lte(String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }
        return LTE + (isTime(data) ? TIME : (isDate(data) ? DATE : STR)) + data;
    }

    /**
     * 包装大于等于参数
     * gt("123") => >='123'
     *
     * @param data String
     * @return 查询条件表达式
     */
    public static String gte(String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }
        return GTE + (isTime(data) ? TIME : (isDate(data) ? DATE : STR)) + data;
    }

    /**
     * 包装小于参数
     * lt("123") => <'123'
     *
     * @param data String
     * @return 查询条件表达式
     */
    public static String lt(String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }
        return LT + (isTime(data) ? TIME : (isDate(data) ? DATE : STR)) + data;
    }

    /**
     * 包装大于参数
     * gt("123") => >'123'
     *
     * @param data String
     * @return 查询条件表达式
     */
    public static String gt(String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }
        return GT + (isTime(data) ? TIME : (isDate(data) ? DATE : STR)) + data;
    }

    private static boolean isDate(String data) {
        return Pattern.matches("\\d{4}-\\d{2}-\\d{2}", data);
    }

    private static boolean isTime(String data) {
        return Pattern.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}", data);
    }

    /**
     * 包装BETWEEN参数
     * between("123","133") => between '123' and '133'
     * 注意，如果是传递日期类型，该方法的返回值是Timestamp类型，
     * 因为PartTool.toBo()会损失时间精度（只保留yyyy-mm-dd,忽略hh:mm:ss）
     *
     * @param start 最小边界
     * @param end   最大边界
     * @return 查询条件表达式
     */
    public static String between(String start, String end) {
        boolean startIsEmpty = StringUtils.isEmpty(start);
        boolean endIsEmpty = StringUtils.isEmpty(end);
        if (startIsEmpty && endIsEmpty) {
            return "";
        }
        if (startIsEmpty) {
            return lte(end);
        }
        if (endIsEmpty) {
            return gte(start);
        }
        // !startIsEmpty && !endIsEmpty
        boolean isDate = isDate(start) && isDate(end);
        if (isDate) {
            return BETWEEN + DATE + start + "," + end;
        }
        boolean isTime = isTime(start) && isTime(end);
        if (isTime) {
            return BETWEEN + TIME + start + "," + end;
        }
        return BETWEEN + STR + start + "," + end;

    }

    /**
     * 包装小于等于参数
     * lt("123") => <='123'
     *
     * @param data long 数字
     * @return 查询条件表达式
     */
    public static String lte(long data) {

        return LTE + NUM + data;
    }

    /**
     * 包装大于等于参数
     * gt("123") => >='123'
     *
     * @param data long 数字
     * @return 查询条件表达式
     */
    public static String gte(long data) {

        return GTE + NUM + data;
    }

    /**
     * 包装小于参数
     * lt("123") => <'123'
     *
     * @param data String
     * @return 查询条件表达式
     */
    public static String lt(long data) {
        return LT + NUM + data;
    }

    /**
     * 包装大于参数
     * gt("123") => >'123'
     *
     * @param data String
     * @return 查询条件表达式
     */
    public static String gt(long data) {
        return GT + NUM + data;
    }

    /**
     * 包装小于参数(时间)
     *
     * @param date Date或TimeStamp
     * @return 查询条件表达式
     */
    public static String lt(Date date) {
        return getDateEqualQueryString(date, LT);
    }

    /**
     * 包装大于参数(时间)
     *
     * @param date Date或TimeStamp
     * @return 查询条件表达式
     */
    public static String gt(Date date) {
        return getDateEqualQueryString(date, GT);
    }

    private static String getDateEqualQueryString(Date date, String symbol) {
        String type;
        String dateString;
        if (date instanceof java.sql.Date) {
            // date instanceof java.sql.Date
            type = DATE;
            dateString = Convert.toStr(date, "yyyy-MM-dd");
        } else if (date instanceof java.sql.Timestamp) {
            type = TIME;
            dateString = Convert.toStr(date, "yyyy-MM-dd HH:mm:ss");
        } else {
            throw new IllegalArgumentException("入参为时间时只支持Timestamp/java.sql.Date/java.util.Date");
        }
        return symbol + type + dateString;
    }

    /**
     * 包装BETWEEN参数
     * between("123","133") => between '123' and '133'
     *
     * @param start 最小边界
     * @param end   最大边界
     * @return 查询条件表达式
     */
    public static String between(long start, long end) {
        return BETWEEN + NUM + start + "," + end;
    }

    /**
     * 以and连接多个条件
     *
     * @param cond 表达式子句
     * @return 查询条件表达式
     */
    public static String and(String... cond) {
        List<String> list = new ArrayList<>();
        for (String s : cond) {
            if (!StringUtils.isEmpty(s)) {
                list.add(s);
            }
        }
        // 包裹括号
        if (list.size() > 1) {
            for (int i = 0; i < list.size(); i++) {
                list.set(i, " ( " + list.get(i) + " ) ");
            }
        }

        return StringUtils.join(list, SPACE + AND);
    }

    /**
     * 以or连接多个条件
     *
     * @param cond 表达式子句
     * @return 表达式
     */
    public static String or(String... cond) {

        List<String> list = new ArrayList<>();
        // 去空值
        for (String s : cond) {
            if (!StringUtils.isEmpty(s)) {
                list.add(s);
            }
        }
        // 包裹括号
        if (list.size() > 1) {
            for (int i = 0; i < list.size(); i++) {
                list.set(i, " ( " + list.get(i) + " ) ");
            }
        }

        return StringUtils.join(list, SPACE + OR);
    }

    /**
     * 升序
     *
     * @param order 顺序
     * @return 查询条件表达式
     */
    public static String orderByAsc(int order) {
        return ASC + "-" + order;
    }

    /**
     * 升序
     *
     * @return 查询条件表达式
     */
    public static String orderByAsc() {
        return orderByAsc(0);
    }

    /**
     * 降序
     *
     * @param order 顺序
     * @return 查询条件表达式
     */
    public static String orderByDesc(int order) {
        return DESC + "-" + order;
    }

    /**
     * 降序
     *
     * @return 查询条件表达式
     */
    public static String orderByDesc() {
        return orderByDesc(0);
    }


}
