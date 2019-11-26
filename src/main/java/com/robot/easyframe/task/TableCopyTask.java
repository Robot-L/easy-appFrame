package com.robot.easyframe.task;

import com.ai.appframe2.complex.datasource.DataSourceFactory;
import com.asiainfo.appframe.ext.exeframe.task.interfaces.ITask;
import com.asiainfo.appframe.ext.exeframe.tf.config.table.Column;
import com.asiainfo.appframe.ext.exeframe.tf.util.QueryUtil;
import com.robot.easyframe.annotation.TableCopyConfig;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 跨中心的表数据拷贝进程
 * 先删除目标表数据，然后根据指定sql将源表数据拷贝到目标表
 *
 * 用法：
 * 继承本类后，用注解@TableCopyConfig在类名上进行配置
 * 参数列表见{@link TableCopyConfig}
 *
 * @author luozhan
 * @date 2019-10
 */
public class TableCopyTask implements ITask {
    @Override
    public String doTask(long taskId) throws Exception {
        // 获取子类配置参数
        TableCopyConfig tableCopyConfig = this.getClass().getAnnotation(TableCopyConfig.class);
        if (tableCopyConfig == null) {
            throw new RuntimeException("继承TableCopyTask的类未标注@TableCopyConfig注解，类名" + this.getClass().getName());
        }
        String srcCenterName = tableCopyConfig.srcCenter();
        String destCenterName = tableCopyConfig.destCenterName();
        String selectSql = tableCopyConfig.srcSql();
        String destTable = tableCopyConfig.destTable();

        // 目标表删除sql
        String deleteSql = "delete from " + destTable + " nologging";

        Column[] columns = QueryUtil.readTableColumns(destCenterName, destTable);
        String colString = Stream.of(columns).map(Column::getName).collect(Collectors.joining(","));
        String colMask = Stream.of(columns).map(c -> "?").collect(Collectors.joining(","));
        // 目标表插入sql
        String insertSql = MessageFormat.format("insert /*+ append */ into {0} ( {1} ) values ( {2} )", destTable, colString, colMask);

        return executeCopy(srcCenterName, destCenterName, selectSql, deleteSql, insertSql, columns);
    }

    /**
     * 执行数据拷贝操作
     *
     * @param srcCenterName  数据源中心
     * @param destCenterName 目标表中心
     * @param selectSql      数据源查询sql
     * @param deleteSql      目标表删除sql
     * @param insertSql      目标表插入sql
     * @param columns        目标表字段数组
     * @return 执行结果
     */
    private String executeCopy(String srcCenterName, String destCenterName, String selectSql, String deleteSql, String insertSql, Column[] columns) throws Exception {
        // 准备资源
        try (Connection srcCon = DataSourceFactory.getDataSource().getConnectionFromDataSource(srcCenterName);
             Connection destCon = DataSourceFactory.getDataSource().getConnectionFromDataSource(destCenterName);
             PreparedStatement srcSelectPreparedStatement = srcCon.prepareStatement(selectSql);
             PreparedStatement destDeletePreparedStatement = destCon.prepareStatement(deleteSql);
             PreparedStatement destInsertPreparedStatement = destCon.prepareStatement(insertSql);
             ResultSet srcResultSet = srcSelectPreparedStatement.executeQuery()) {
            try {
                // 删除目标表数据
                int deleteCount = destDeletePreparedStatement.executeUpdate();

                // 从源表拷贝数据到目标表
                srcSelectPreparedStatement.setFetchSize(1000);
                int count = 0;
                while (srcResultSet.next()) {
                    count++;
                    for (int i = 0; i < columns.length; ++i) {
                        destInsertPreparedStatement.setObject(i + 1, srcResultSet.getObject(columns[i].getName()));
                    }
                    destInsertPreparedStatement.addBatch();
                    if (count % 1000 == 0) {
                        destInsertPreparedStatement.executeBatch();
                    }
                }
                destInsertPreparedStatement.executeBatch();
                destCon.commit();

                return "删除目标表原有的" + deleteCount + "条记录，新同步写入" + count + "条记录";
            } catch (Exception e) {
                destCon.rollback();
                throw e;
            }
        }
    }

}
