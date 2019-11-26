package com.robot.easyframe.core.engine;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.common.DataContainerInterface;
import com.ai.appframe2.complex.cache.CacheFactory;
import com.ai.appframe2.complex.cache.ICache;

import com.robot.easyframe.core.Query;
import com.robot.easyframe.core.cache.BaseCache;
import com.robot.easyframe.core.dao.BaseDao;
import com.robot.easyframe.model.Pagination;
import com.robot.easyframe.util.Convert;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * @author luozhan
 * @date 2019-05
 */
public class DataEngine {
    private static Log log = LogFactory.getLog(DataEngine.class);

    /**
     * 缓存中获取符合条件的数据
     *
     * @param cacheClass 缓存class
     * @param cond       查询条件
     * @param page       分页对象
     * @param <T>
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainer> T[] getBeans(Class<? extends BaseCache<? extends BaseDao<T>>> cacheClass, DataContainerInterface cond, Pagination page) throws Exception {
        T[] data = (T[]) CacheFactory.get(cacheClass, cacheClass);
        return getBeans(data, cond, page);
    }

    /**
     * 从指定数据数组中获取符合条件的数据(带分页)
     *
     * @param data 数据源
     * @param cond 查询条件
     * @param page 分页对象
     * @param <T>
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainer> T[] getBeans(T[] data, DataContainerInterface cond, Pagination page) {
        List<Predicate<T>> condList = bean2List(cond);
        if (condList.size() == 0 && page == null) {
            // 1.无查询条件，无分页条件
            return data;
        }
        Stream<T> result = Arrays.stream(data);
        if (condList.size() != 0) {
            // 2.条件过滤
            Predicate<T> predicate = condList.stream().reduce(Predicate::and).get();
            result = result.filter(predicate);
        }
        if (page != null) {
            // 3.分页处理
            result = result.skip(page.getStart() - 1).limit(page.getPageSize());
        }
        return result.toArray(value -> (T[]) Array.newInstance(data.getClass().getComponentType(), value));
    }

    /**
     * 从指定数据数组中获取符合条件的数据（不带分页）
     *
     * @param data 数据源
     * @param cond 查询条件
     * @param <T>
     * @return
     */
    public static <T extends DataContainer> T[] getBeans(T[] data, DataContainerInterface cond) {
        return getBeans(data, cond, null);
    }

    /**
     * 获取数量（主要用于获得分页查询业务的count总数）
     *
     * @param cacheClass
     * @param cond
     * @param <T>
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainerInterface> int count(Class<? extends ICache> cacheClass, DataContainerInterface cond) throws Exception {
        T[] data = (T[]) CacheFactory.get(cacheClass, cacheClass);
        List<Predicate<T>> condList = bean2List(cond);
        if (condList.size() == 0) {
            // 1.无查询条件
            return data.length;
        }
        Stream<T> result = Arrays.stream(data);
        // 2.条件过滤
        Predicate<T> predicate = condList.stream().reduce(Predicate::and).get();
        return (int) Arrays.stream(data).filter(predicate).count();
    }

    private static <T extends DataContainerInterface> List<Predicate<T>> bean2List(DataContainerInterface cond) {
        List<Predicate<T>> condList = new ArrayList<>();
        if (cond != null) {
            // 1.处理条件
            Map params = cond.getProperties();
            for (Object o : params.entrySet()) {
                Map.Entry param = (Map.Entry) o;
                if (param.getValue() == null || "".equals(param.getValue())) {
                    // 空的时候代表用户没有传值，应忽略
                    continue;
                }
                // 属性名，属性值（Query条件）
                String paramName = param.getKey().toString(), paramValue = param.getValue().toString();
                condList.add(getPredicate(paramName, paramValue));
            }
        }
        return condList;
    }

    private static <T extends DataContainerInterface> Predicate<T> getPredicate(String field, String statement) {
        String type = statement.substring(0, statement.indexOf(":") + 1);
        switch (type) {
            case Query.IN: {
                // in:1,2,3
                List<String> list = Arrays.asList(statement.substring(Query.IN.length()).split(","));
                return bo -> list.contains(bo.getAsString(field));
            }
            case Query.NOT_IN: {
                // not-in:1,2,3
                List<String> list = Arrays.asList(statement.substring(Query.NOT_IN.length()).split(","));
                return bo -> !list.contains(bo.getAsString(field));
            }
            case Query.LT:
            case Query.GT:
            case Query.LTE:
            case Query.GTE:
                throw new RuntimeException("暂不支持缓存查询中使用表达式" + type + "，请联系Robot增加功能");
            case Query.BETWEEN:
                return handleBetweenCond(field, statement);
            case Query.LIKE: {
                String regex = statement.substring(Query.LIKE.length())
                        // 所有正则特殊符号进行转义 ^$*.|()\
                        .replaceAll("[\\^$*+?.|()\\\\]", "[$0]")
                        .replaceAll("%", ".*")
                        .replaceAll("_", ".");
                Pattern pattern = Pattern.compile(regex);
                return bo -> bo.getAsString(field) != null && pattern.matcher(bo.getAsString(field)).matches();
            }
            case Query.NOT_LIKE: {
                String regex = statement.substring(Query.NOT_LIKE.length())
                        // 所有正则特殊符号进行转义 ^$*.|()\
                        .replaceAll("[\\^$*+?.|()\\\\]", "[$0]")
                        .replaceAll("%", ".*")
                        .replaceAll("_", ".");
                Pattern pattern = Pattern.compile(regex);
                return bo -> bo.getAsString(field) != null && !pattern.matcher(bo.getAsString(field)).matches();
            }
            default:
                return (bo -> statement.equals(bo.getAsString(field)));
        }
    }

    private static boolean isTimeBetween(String destTime, String startTime, String endTime) {
        try {
            Timestamp startFinalValue = Convert.toTimeStamp(startTime);
            Timestamp endFinalValue = Convert.toTimeStamp(endTime);
            Timestamp destTimeStamp = Convert.toTimeStamp(destTime);
            return destTimeStamp.compareTo(startFinalValue) >= 0 && destTimeStamp.compareTo(endFinalValue) <= 0;
        } catch (Exception e) {
            log.error("between语句解析：日期转换失败！at DaoEngine.handleSqlSnippet()");
            return false;
        }

    }

    private static <T extends DataContainerInterface> Predicate<T> handleBetweenCond(String fieldName, String statement) {
        String startValue = statement.substring(StringUtils.ordinalIndexOf(statement, ":", 2) + 1, statement.indexOf(","));
        String endValue = statement.substring(statement.indexOf(",") + 1);

        boolean isString = Pattern.matches(".*str:.*", statement);
        boolean isDate = !isString && Pattern.matches(".*date:.*", statement);
        boolean isTime = Pattern.matches(".*time:.*", statement);
        boolean isNumber = !isString && !isDate && Pattern.matches(".*num:.*", statement);
        if (isDate) {
            return bo -> isTimeBetween(bo.getAsString(fieldName), startValue + " 00:00:00", endValue + " 23:59:59");

        }
        if (isTime) {
            return bo -> isTimeBetween(bo.getAsString(fieldName), startValue, endValue);
        }
        if (isNumber) {
            return bo -> (bo.get(fieldName) != null
                    && bo.getAsLong(fieldName) >= Long.parseLong(startValue))
                    && bo.getAsLong(fieldName) <= Long.parseLong(endValue);
        }
        if (isString) {
            return bo -> bo.get(fieldName) != null
                    && bo.getAsString(fieldName).compareTo(startValue) >= 0
                    && bo.getAsString(fieldName).compareTo(endValue) <= 0;
        }
        return bo -> false;
    }

}
