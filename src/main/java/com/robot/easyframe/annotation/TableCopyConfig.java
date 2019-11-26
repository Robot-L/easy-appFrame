package com.robot.easyframe.annotation;

import com.robot.easyframe.task.TableCopyTask;

import java.lang.annotation.*;

/**
 * 表数据拷贝进程的配置
 * 配合{@link TableCopyTask}使用
 *
 * @author luozhan
 * @create 2019-10
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TableCopyConfig {
    /**
     * 执行查询sql的中心名
     * 如："RES"
     */
    String srcCenter();

    /**
     * 数据源的查询sql（查询的字段名须和destTable表的列名一致，不一致须用别名）
     * 如："select * from RES_STOCK"
     */
    String srcSql();

    /**
     * 目标表所在中心名
     * 如："ORD"
     */
    String destCenterName();

    /**
     * 目标表
     * 如："RES_STOCK"
     */
    String destTable();

}
