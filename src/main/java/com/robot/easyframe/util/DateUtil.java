package com.robot.easyframe.util;

import com.ai.appframe2.complex.cache.CacheFactory;
import com.ai.appframe2.complex.cache.impl.SysDateCacheImpl;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;

/**
 * 简单的日期相关类
 * 日期转换相关的请使用{@link Convert}
 *
 * @author luozhan
 * @date 2019-11
 */
public class DateUtil {
    private static transient final Log log = LogFactory.getLog(DateUtil.class);

    /**
     * 当前时间（数据库时间）
     *
     * @return Timestamp
     */
    public static Timestamp now() {
        long diff = 0;
        try {
            diff = (long) CacheFactory.get(SysDateCacheImpl.class, "BASE");
        } catch (Exception e) {
            log.error("通过缓存获取数据库时差出现异常，直接使用主机系统时间", e);
        }
        return new Timestamp(System.currentTimeMillis() + diff);
    }


    /**
     * 获取指定日期偏移指定时间后的时间，生成的偏移日期不影响原日期
     *
     * @param date      基准日期
     * @param dateField 偏移的粒度大小（小时、天、月等），使用Calendar类的常量
     * @param offset    偏移量，正数为向后偏移，负数为向前偏移
     * @return 偏移后的日期
     */
    public static Timestamp offset(Date date, int dateField, int offset) {
        final Calendar cal = toCalendar(date.getTime());
        cal.add(dateField, offset);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * 获取指定日期偏移指定天后的时间，生成的偏移日期不影响原日期
     *
     * @param date      基准日期
     * @param offset    偏移量，正数为向后偏移，负数为向前偏移
     * @return 偏移后的日期
     */
    public static Timestamp offsetDay(Date date, int offset) {
        final Calendar cal = toCalendar(date.getTime());
        cal.add(Calendar.DAY_OF_MONTH, offset);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * 获取指定日期偏移指定小时后的时间，生成的偏移日期不影响原日期
     *
     * @param date      基准日期
     * @param offset    偏移量，正数为向后偏移，负数为向前偏移
     * @return 偏移后的日期
     */
    public static Timestamp offsetHour(Date date, int offset) {
        final Calendar cal = toCalendar(date.getTime());
        cal.add(Calendar.HOUR, offset);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * 获取指定日期偏移指定分钟后的时间，生成的偏移日期不影响原日期
     *
     * @param date      基准日期
     * @param offset    偏移量，正数为向后偏移，负数为向前偏移
     * @return 偏移后的日期
     */
    public static Timestamp offsetMin(Date date, int offset) {
        final Calendar cal = toCalendar(date.getTime());
        cal.add(Calendar.MINUTE, offset);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * 获取指定日期偏移指定秒后的时间，生成的偏移日期不影响原日期
     *
     * @param date      基准日期
     * @param offset    偏移量，正数为向后偏移，负数为向前偏移
     * @return 偏移后的日期
     */
    public static Timestamp offsetSecond(Date date, int offset) {
        final Calendar cal = toCalendar(date.getTime());
        cal.add(Calendar.SECOND, offset);
        return new Timestamp(cal.getTimeInMillis());
    }

    /**
     * 转换为Calendar对象
     *
     * @param millis 时间戳
     * @return Calendar对象
     */
    public static Calendar toCalendar(long millis) {
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(millis);
        return cal;
    }

}
