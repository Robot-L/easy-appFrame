package com.robot.easyframe.enums;


import java.util.Calendar;

/**
 * 日期各个部分的枚举<br>
 * 与Calendar相应值对应
 * @author luozhan
 * @date 2019-11
 */
public enum DateField {

    /*  最常用的 */
    /**
     * 年
     *
     * @see Calendar#YEAR
     */
    YEAR(Calendar.YEAR),
    /**
     * 月
     *
     * @see Calendar#MONTH
     */
    MONTH(Calendar.MONTH),
    /**
     * 日
     *
     * @see Calendar#DAY_OF_YEAR
     */
    DAY_OF_YEAR(Calendar.DAY_OF_MONTH),
    /**
     * 世纪
     *
     * @see Calendar#ERA
     */
    ERA(Calendar.ERA),

    /**
     * 一年中第几周
     *
     * @see Calendar#WEEK_OF_YEAR
     */
    WEEK_OF_YEAR(Calendar.WEEK_OF_YEAR),
    /**
     * 一月中第几周
     *
     * @see Calendar#WEEK_OF_MONTH
     */
    WEEK_OF_MONTH(Calendar.WEEK_OF_MONTH),
    /**
     * 一月中的第几天
     *
     * @see Calendar#DAY_OF_MONTH
     */
    DAY_OF_MONTH(Calendar.DAY_OF_MONTH),

    /**
     * 周几，1表示周日，2表示周一
     *
     * @see Calendar#DAY_OF_WEEK
     */
    DAY_OF_WEEK(Calendar.DAY_OF_WEEK),
    /**
     * 天所在的周是这个月的第几周
     *
     * @see Calendar#DAY_OF_WEEK_IN_MONTH
     */
    DAY_OF_WEEK_IN_MONTH(Calendar.DAY_OF_WEEK_IN_MONTH),
    /**
     * 上午或者下午
     *
     * @see Calendar#AM_PM
     */
    AM_PM(Calendar.AM_PM),
    /**
     * 小时，用于12小时制
     *
     * @see Calendar#HOUR
     */
    HOUR(Calendar.HOUR),
    /**
     * 小时，用于24小时制
     *
     * @see Calendar#HOUR
     */
    HOUR_OF_DAY(Calendar.HOUR_OF_DAY),
    /**
     * 分钟
     *
     * @see Calendar#MINUTE
     */
    MINUTE(Calendar.MINUTE),
    /**
     * 秒
     *
     * @see Calendar#SECOND
     */
    SECOND(Calendar.SECOND),
    /**
     * 毫秒
     *
     * @see Calendar#MILLISECOND
     */
    MILLISECOND(Calendar.MILLISECOND);

    private int value;

    DateField(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }
}
