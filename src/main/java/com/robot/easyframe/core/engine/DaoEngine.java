package com.robot.easyframe.core.engine;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.bo.DataContainerFactory;
import com.ai.appframe2.bo.ObjectTypeNull;
import com.ai.appframe2.common.*;
import com.ai.appframe2.complex.cache.CacheFactory;
import com.ai.appframe2.complex.cache.impl.BatchIdGeneratorCacheImpl;
import com.ai.appframe2.complex.tab.id.BatchSequence;
import com.ai.appframe2.complex.transaction.interfaces.IMutilTransactionDatasource;
import com.ai.appframe2.privilege.UserInfoInterface;
import com.robot.easyframe.core.Query;
import com.robot.easyframe.model.Pagination;
import com.robot.easyframe.util.Convert;
import com.robot.easyframe.util.LambdaExceptionUtil;
import com.robot.easyframe.util.ResCommonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 基于每个Bo的Engine类提取出来的公共类
 * 扩展了面向对象方式持久化的功能
 *
 * @author luozhan
 * @date 2019-01
 * @see com.robot.easyframe.core.dao.BaseDao
 * @see com.robot.easyframe.core.dao.BaseDaoImpl
 */
public class DaoEngine {

    private static Log log = LogFactory.getLog(DaoEngine.class);

    private static final Pattern PATTERN_COND = Pattern.compile("\\((\\s*([^\\(\\)]*?)\\s*)\\)");

    private static final Pattern PATTERN_ORDER = Pattern.compile(":orderBy(Asc|Desc)-(\\d*)");

    /**
     * 获取bo的ObjectType
     */
    private static ObjectType getBoType(Class<? extends DataContainerInterface> bo) throws Exception {
        return bo.newInstance().getObjectType();
    }

    /**
     * 根据主键查询
     *
     * @param clazz BO类
     * @param id    主键
     * @return 目标bo 查询不到返回null
     */
    public static <T extends DataContainerInterface> T getBean(Class<T> clazz, Long id) throws Exception {
        String keyName = clazz.newInstance().getObjectType().getMainAttr();
        String conditionSql = keyName + " = :" + keyName;
        Map<String, Long> map = new HashMap<>(1);
        map.put(keyName, id);
        T[] result = getBeans(clazz, conditionSql, map);
        return result.length == 0 ? null : result[0];
    }

    /**
     * 根据条件bean获取
     *
     * @param clazz     bo类
     * @param condition 查询条件
     * @return 符合条件的bo数组，查询不到返回空数组
     */
    public static <T extends DataContainerInterface> T[] getBeans(Class<T> clazz, DataContainerInterface condition) throws Exception {
        return getBeans(clazz, condition, null);
    }

    /**
     * 根据条件bean获取(分页)
     *
     * @param clazz     bo类
     * @param condition 查询条件
     * @param page      分页
     * @return 符合条件的bo数组，查询不到返回空数组
     */
    public static <T extends DataContainerInterface> T[] getBeans(Class<T> clazz, DataContainerInterface condition, Pagination page) throws Exception {

        Map<String, Object> map = bean2sql(condition, true);
        String conditionSql = (String) map.get("sql");
        Map paramMap = (Map) map.get("paramMap");
        // if (conditionSql.length() == 0 && page == null) {
        //     // 如果不传任何查询条件，限制查8000条数据，避免性能问题
        //     page = new Pagination(8000);
        // }
        return getBeans(clazz, conditionSql, paramMap, page);

    }

    /**
     * 根据condition-sql查询
     *
     * @param clazz        查询bo的类型
     * @param conditionSql where后的sql语句（不需要拼where）
     * @param parameter    查询参数
     * @return 符合条件的bo数组，查询不到返回空数组
     */
    public static <T extends DataContainerInterface> T[] getBeans(Class<T> clazz, String conditionSql, Map parameter) throws Exception {
        return getBeans(clazz, null, conditionSql, parameter, null);
    }

    /**
     * 根据condition-sql查询（分页&可选查询的列）
     * root
     *
     * @param clazz        BO类
     * @param cols         查询的列
     * @param conditionSql where后的sql语句（不需要拼where）
     * @param parameter    查询参数
     * @param page         分页
     * @return 符合条件的bo数组，查询不到返回空数组
     */
    public static <T extends DataContainerInterface> T[] getBeans(Class<T> clazz, String[] cols, String conditionSql, Map parameter,
                                                                  Pagination page) throws Exception {
        log.debug("条件sql：" + conditionSql);
        log.debug("参数：" + parameter);
        int startNum = -1, endNum = -1;
        if (page != null) {
            startNum = page.getStart();
            endNum = page.getEnd();
        }
        try (Connection conn = ServiceManager.getSession().getConnection()) {
            return (T[]) ServiceManager.getDataStore().retrieve(conn, clazz, getBoType(clazz), cols, conditionSql, parameter, startNum, endNum, false, false, null);
        }
    }

    /**
     * 连表查询（不建议使用）
     * 会扫描主表bo文件中的外键配置属性，合并主表的属性一起查询出来
     * 已过期，建议直接使用连表查询sql或者先单表查询后再使用字典翻译功能实现
     *
     * @param clazz 主表BO类
     * @param cond  条件bean
     * @param page  分页对象
     * @return DataContainer数组，没有数据返回空数组
     */
    @Deprecated
    public static DataContainer[] getDcs(Class<? extends DataContainerInterface> clazz, DataContainerInterface cond, Pagination page) throws Exception {
        Map<String, Object> map = bean2sql(cond, true);
        String conditionSql = (String) map.get("sql");
        Map paramMap = (Map) map.get("paramMap");
        int startNum = -1, endNum = -1;
        if (page != null) {
            startNum = page.getStart();
            endNum = page.getEnd();
        }
        try (Connection conn = ServiceManager.getSession().getConnection();
             ResultSet resultset = ServiceManager.getDataStore().retrieve(conn, getBoType(clazz), null, conditionSql, paramMap, startNum, endNum, true, false, null)) {
            return convert(resultset, DataContainer.class);
        }
    }

    /**
     * 将ResultSet转换成bo数组
     */
    @SuppressWarnings("unchecked")
    private static <T extends DataContainerInterface> T[] convert(ResultSet rs, Class<T> clazz) throws Exception {
        if (rs == null) {
            return null;
        }
        ResultSetMetaData metaData = rs.getMetaData();
        int count = metaData.getColumnCount();
        List<T> result = new ArrayList<>();
        Boolean isDc = null;
        while (rs.next()) {
            T dc = clazz.newInstance();
            if (isDc == null) {
                isDc = dc.getObjectType() instanceof ObjectTypeNull;
            }
            for (int i = 1; i <= count; i++) {
                String columnName = metaData.getColumnName(i);
                if (isDc || dc.getObjectType().hasProperty(columnName)) {
                    dc.set(columnName, rs.getObject(i));
                }
            }
            result.add(dc);
        }
        return result.toArray((T[]) Array.newInstance(clazz, 0));
    }

    /**
     * 根据condition-sql查询（分页）
     *
     * @param clazz
     * @param conditionSql
     * @param parameter
     * @param page
     * @param <T>
     * @return
     * @throws Exception
     */
    public static <T extends DataContainerInterface> T[] getBeans(Class<T> clazz, String conditionSql, Map parameter,
                                                                  Pagination page) throws Exception {
        return getBeans(clazz, null, conditionSql, parameter, page);
    }


    /**
     * 根据条件bean查询记录数量（count(*)）
     *
     * @param clazz     bo类型
     * @param condition 查询条件
     * @return 数量
     */
    public static int getBeansCount(Class<? extends DataContainer> clazz, DataContainerInterface condition) throws Exception {
        Map<String, Object> map = bean2sql(condition, true);
        String conditionSql = (String) map.get("sql");
        Map paramMap = (Map) map.get("paramMap");

        return getBeansCount(clazz, conditionSql, paramMap);
    }

    /**
     * 根据condition-sql查询记录数量
     *
     * @param clazz        bo类型
     * @param conditionSql 条件sql，不带where
     * @param parameter    查询参数
     * @return
     * @throws Exception
     */
    public static int getBeansCount(Class<? extends DataContainer> clazz, String conditionSql, Map parameter) throws Exception {
        try (Connection conn = ServiceManager.getSession().getConnection()) {
            return ServiceManager.getDataStore().retrieveCount(conn, getBoType(clazz), conditionSql, parameter, null);
        }
    }

    /**
     * 保存（包含新增、更新、删除，通过bean.isNew()/isDeleted()/isModified()依次判断）
     *
     * @param bean bo
     */
    public static int save(DataContainerInterface bean) throws Exception {
        try (Connection conn = ServiceManager.getSession().getConnection()) {
            return ServiceManager.getDataStore().save2(conn, bean);
        }
    }


    /**
     * 批量保存（包含新增、更新、删除，通过bean.isNew()/isDeleted()/isModified()依次判断）
     *
     * @param beans 对象数组
     */
    public static int save(DataContainerInterface[] beans) throws Exception {
        try (Connection conn = ServiceManager.getSession().getConnection()) {
            return ServiceManager.getDataStore().saveBatch2(conn, beans);
        }
    }

    /**
     * 异步执行入库代码
     *
     * @param supplier 入库的回调函数，函数入参为空，出参为操作数
     */
    public static <R extends Integer, E extends Exception> void asynExecute(LambdaExceptionUtil.SupplierWithExceptions<R, E> supplier) throws E {
        Session currentSession = ServiceManager.getSession();
        String currentDataSource = ((IMutilTransactionDatasource) currentSession).getCurDataSource();
        UserInfoInterface currentUser = SessionManager.getUser();
        CompletableFuture.runAsync(() -> {
            Session session = ServiceManager.getSession();
            try {
                // 在异步方法里请求csf服务时，session中必须有用户信息，否则调用服务时会抛异常
                SessionManager.setUser(currentUser);
                // 开启新事务
                session.startTransaction();
                // 设置当前数据源
                ((IMutilTransactionDatasource) session).setCurDataSource(currentDataSource);
                // 执行操作
                int count = supplier.get();
                session.commitTransaction();
                log.debug("异步入库成功，操作数目：" + count);
            } catch (Exception e) {
                log.error("异步入库失败，失败原因：" + e);
                try {
                    session.rollbackTransaction();
                } catch (Exception ex) {
                    log.error(ex);
                }
            }
        });
    }

    /**
     * 根据完整sql查询
     * root
     *
     * @param sql       完整的sql，变量用"：paramA"代替
     * @param parameter {paramA:"",paramB:""}
     */
    public static DataContainer[] getBeansFromSql(String sql, Map parameter) throws Exception {
        try (Connection conn = ServiceManager.getSession().getConnection();
             ResultSet rs = ServiceManager.getDataStore().retrieve(conn, sql, parameter)) {
            DataContainer[] result = convert(rs, DataContainer.class);
            return result == null ? new DataContainer[0] : result;
        }
    }

    /**
     * 执行增、删、改的sql
     *
     * @param sql
     * @param params
     * @return
     * @throws Exception
     */
    public static long execSQL(String sql, Map params) throws Exception {
        try (Connection conn = ServiceManager.getSession().getConnection()) {
            return ServiceManager.getDataStore().execute(conn, sql, params);
        }
    }

    /**
     * 根据完整sql查询，根据结果生成传入class的数组
     *
     * @param clazz
     * @param sql
     * @param parameter
     * @param <T>
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainerInterface> T[] getBeansFromSql(Class<T> clazz, String sql, Map parameter) throws Exception {
        try (Connection conn = ServiceManager.getSession().getConnection();
             ResultSet rs = ServiceManager.getDataStore().retrieve(conn, sql, parameter)) {
            T[] result = convert(rs, clazz);
            return result == null ? (T[]) Array.newInstance(clazz, 0) : result;
        }
    }

    /**
     * 获取新id
     *
     * @return
     * @throws Exception
     */
    public static long getNewId(Class<? extends DataContainerInterface> clazz) throws Exception {
        return ServiceManager.getIdGenerator().getNewId(getBoType(clazz)).longValue();
    }

    /**
     * 获取新id(批量)
     *
     * @return
     * @throws Exception
     */
    public static String[] getNewId(Class<? extends DataContainerInterface> clazz, int amount) throws Exception {
        String tableName = getBoType(clazz).getMapingEnty();
        BatchSequence batchSequence = (BatchSequence) CacheFactory.get(BatchIdGeneratorCacheImpl.class, tableName.toUpperCase());
        Field seqName = BatchSequence.class.getDeclaredField("seqName");
        seqName.setAccessible(true);
        String sequenceName = (String) seqName.get(batchSequence);
        String sql = "" +
                "select {0}.NEXTVAL " +
                "from (select 1 " +
                "      from all_objects " +
                "      where ROWNUM <= :ROWNUM ) ";
        sql = MessageFormat.format(sql, sequenceName);

        Map<String, Object> params = new HashMap<>(1);
        List<DataContainer> sequenceList = new ArrayList<>();
        // 当all_objects表的行数小于需要的序列数时，将多次查询该表获取足够数量
        while (sequenceList.size() < amount) {
            params.put("ROWNUM", amount - sequenceList.size());
            sequenceList.addAll(Arrays.asList(getBeansFromSql(sql, params)));
        }
        return ResCommonUtil.getFieldValues(sequenceList, "NEXTVAL");
    }

    /**
     * 从源bean拷贝属性生成该dao关联的新bean
     * 详见DataContainerFactory.copy()
     *
     * @param source
     * @param colMatch 两个bean的属性映射关系
     * @param clazz    新bo的类
     * @return <T>
     */
    public static <T extends DataContainerInterface> T copyFrom(DataContainerInterface source, Map colMatch, Class<T> clazz) throws Exception {
        T result = clazz.newInstance();
        DataContainerFactory.copy(source, result, colMatch);
        return result;
    }

    /**
     * 给sql中in语句的每个值加上单引号
     *
     * @param in 形如 "123, 234, 345"
     * @return 形如"'123', '234', '345'"
     */
    private static String addQuote(String in) {
        if (in == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        //排除空格的干扰
        for (String item : in.replace(" ", "").split(",")) {
            sb.append(",");
            sb.append("'").append(item).append("'");
        }
        return sb.substring(1);
    }

    /**
     * bean转sql-condition语句
     *
     * @param dc             条件bean
     * @param deleteFirstAnd false-生成的sql以and开头
     * @return map 包装了转换后的 condition-sql 和 paramMap
     */
    public static Map<String, Object> bean2sql(DataContainerInterface dc, boolean deleteFirstAnd) {
        Map<String, Object> result = new HashMap<>(2);
        StringBuilder conditionSql = new StringBuilder();
        Map params = new HashMap();
        if (dc != null) {
            Map sqlParams = dc.getProperties();
            // 保存排序条件，结构[[顺序，列名，排序方式],...]
            List<List<String>> orderConditions = new ArrayList<>();
            // 循环解析每个属性中包含的条件
            for (Object o : sqlParams.entrySet()) {
                Map.Entry param = (Map.Entry) o;
                if (param.getValue() == null || param.getValue().equals("")) {
                    // 空的时候代表用户没有传值，应忽略
                    continue;
                }
                // 属性名，属性值（Query条件）
                String paramName = param.getKey().toString(), paramValue = param.getValue().toString();
                // 每个属性条件解析后的sql语句片段
                String sqlSnippet;

                // 1.处理orderBy排序条件，正则":orderBy(Asc|Desc)-(\\d*)"
                Matcher matcher = PATTERN_ORDER.matcher(paramValue);
                if (matcher.find()) {
                    // 获得排序方式（Asc-升序，Desc-降序）
                    String orderType = matcher.group(1);
                    // 获得当前属性名的排序顺序（0，1，2...）
                    String order = matcher.group(2);
                    orderConditions.add(Arrays.asList(order, paramName, orderType));
                    // 解析完毕，删除排序条件
                    paramValue = paramValue.replace(matcher.group(), "");
                    // 查询条件中只有排序条件，无普通条件
                    if (paramValue.length() == 0) {
                        continue;
                    }
                }

                // 2.处理普通sql条件
                if (paramValue.contains(Query.AND) || paramValue.contains(Query.OR)) {
                    // 2.1 处理单个属性含多个条件的情况，如 "( in:1,2,3 ) and: ((like:%5%) or: (like:'%4%'))"
                    Matcher m = PATTERN_COND.matcher(paramValue);
                    paramValue = " (" + paramValue + ") ";
                    int i = 0;
                    while (m.find()) {
                        // 每个括号中的子语句
                        String subStatement = m.group(2);
                        // 处理每个子条件，加上i以区别每个绑定变量的值
                        String subSqlSnippet = handleSqlSnippet(paramName, paramName + i, subStatement, params);
                        // 最后将处理完毕的sql语句回填到原始语句结构中
                        paramValue = paramValue.replaceFirst(subStatement, subSqlSnippet);
                        i++;
                    }
                    sqlSnippet = paramValue.replace(Query.AND, "and").replace(Query.OR, "or");
                } else {
                    // 2.2 单属性单条件的简单情况
                    sqlSnippet = handleSqlSnippet(paramName, paramName, paramValue, params);
                }

                // 3.两个sql语句之间拼一个and（deleteFirstAnd为false时，sql语句开头也会加一个and）
                if (conditionSql.length() > 0) {
                    conditionSql.append(" and ");
                } else if (!deleteFirstAnd) {
                    conditionSql.append(" and ");
                }
                conditionSql.append(sqlSnippet);
            }

            // 4.所有条件处理完毕，最后加上排序语句
            if (orderConditions.size() > 0) {
                // 根据顺序进行排序，拼接成以逗号分隔的字符串（如："列名 asc, 列名2 desc"）
                String orderStr = orderConditions.stream()
                        .sorted(Comparator.comparing(list -> list.get(0)))
                        .map(list -> list.get(1) + " " + list.get(2))
                        .collect(Collectors.joining(","));
                conditionSql.append(" order by ").append(orderStr);
            }
        }
        result.put("sql", conditionSql.toString());
        result.put("paramMap", params);
        return result;
    }

    /**
     * 处理每个sql片段，一个属性对应一个sql片段
     *
     * @param fieldName     属性名
     * @param bindValueName 绑定变量名
     * @param statement     Query条件语句
     * @param params        sql参数
     * @return
     */
    @SuppressWarnings("unchecked")
    private static String handleSqlSnippet(String fieldName, String bindValueName, String statement, Map params) {
        if (StringUtils.isEmpty(statement)) {
            return "";
        }
        // sql条件片段
        String sqlSnippet;
        // 条件类型
        String type = statement.substring(0, statement.indexOf(":") + 1);
        switch (type) {
            case Query.IN: {
                // in:1,2,3
                // 将in参数数组转换成sql-in语句
                // 注：in语句的拼装没使用绑定变量，因为快不了多少
                String[] inArray = statement.substring(Query.IN.length()).split(",");
                sqlSnippet = getOracleSQLIn(fieldName, inArray, false);
                break;
            }
            case Query.NOT_IN: {
                // not-in:1,2,3
                String[] notInArray = statement.substring(Query.NOT_IN.length()).split(",");
                sqlSnippet = getOracleSQLIn(fieldName, notInArray, true);
                break;
            }
            case Query.IS_NULL: {
                sqlSnippet = fieldName + " is null ";
                break;
            }
            case Query.IS_NOT_NULL: {
                sqlSnippet = fieldName + " is not null ";
                break;
            }
            case Query.LT:
            case Query.GT:
            case Query.LTE:
            case Query.GTE: {
                // <:str:123123 或 >:num:123123 或 >=:date:2018-12-12
                sqlSnippet = handleCompareCond(fieldName, bindValueName, statement, params);
                break;
            }
            case Query.BETWEEN: {
                // between:num: 123 and 223 或 between:date: 2019-01-20 and 2019-01-24
                sqlSnippet = handleBetweenCond(fieldName, bindValueName, statement, params);
                break;
            }
            case Query.LIKE: {
                // like:%123_
                sqlSnippet = fieldName + " like :" + bindValueName;
                params.put(bindValueName, statement.replaceFirst("like:", ""));
                break;
            }
            case Query.NOT_LIKE: {
                // not-like:%123_
                sqlSnippet = fieldName + " not like :" + bindValueName;
                params.put(bindValueName, statement.replaceFirst("not-like:", ""));
                break;
            }
            default: {
                // 普通“=”的条件
                sqlSnippet = fieldName + " = :" + bindValueName;
                params.put(bindValueName, statement);
            }
        }

        return sqlSnippet;
    }

    private static String handleBetweenCond(String fieldName, String bindValueName, String statement, Map params) {
        String sqlSnippet;
        String startName = bindValueName + "_start";
        String endName = bindValueName + "_end";

        String startValue = statement.substring(StringUtils.ordinalIndexOf(statement, ":", 2) + 1, statement.indexOf(","));
        String endValue = statement.substring(statement.indexOf(",") + 1);

        boolean isString = Pattern.matches(".*str:.*", statement);
        boolean isTime = !isString && Pattern.matches(".*time:.*", statement);
        boolean isDate = !isString && !isTime && Pattern.matches(".*date:.*", statement);
        boolean isNumber = !isString && !isTime && !isDate && Pattern.matches(".*num:.*", statement);

        Object startFinalValue = null, endFinalValue = null;
        if (isDate) {
            try {
                startFinalValue = Convert.toTimeStamp(startValue + " 00:00:00");
                endFinalValue = Convert.toTimeStamp(endValue + " 23:59:59");
            } catch (Exception e) {
                isString = true;
                log.error("between语句解析：日期转换失败！at DaoEngine.handleSqlSnippet()");
            }
        }
        if (isTime) {
            try {
                startFinalValue = Convert.toTimeStamp(startValue);
                endFinalValue = Convert.toTimeStamp(endValue);
            } catch (Exception e) {
                isString = true;
                log.error("between语句解析：时间转换失败！at DaoEngine.handleSqlSnippet()");
            }
        }
        if (isNumber) {
            startFinalValue = Long.parseLong(startValue);
            endFinalValue = Long.parseLong(endValue);
        }
        if (isString) {
            startFinalValue = startValue;
            endFinalValue = endValue;
        }
        sqlSnippet = fieldName + " between :" + startName + " and :" + endName;
        params.put(startName, startFinalValue);
        params.put(endName, endFinalValue);
        if (isString) {
            // 在后面加上length是确保查询出的结果符合预期，否则查between('111','322')会查出'2222'
            String lengthName = bindValueName + "_length";
            sqlSnippet += " and length(" + fieldName + ") = :" + lengthName;
            params.put(lengthName, startValue.length());
        }
        return sqlSnippet;
    }

    private static String handleCompareCond(String fieldName, String bindValueName, String statement, Map params) {
        String sqlSnippet;
        // 获取数据类型
        boolean isString = Pattern.matches(".*str:.*", statement);
        boolean isTime = !isString && Pattern.matches(".*time:.*", statement);
        boolean isDate = !isString && !isTime && Pattern.matches(".*date:.*", statement);
        boolean isNumber = !isString && !isTime && !isDate && Pattern.matches(".*num:.*", statement);
        String value = statement.replaceFirst(".{1,2}:[a-z]+:", "");
        Object finalValue = null;
        if (isDate) {
            try {
                // 在日期中，如果是小于等于，或者大于，应在日期上加23:59:59才符合预期
                finalValue = Pattern.matches("(<=:|>:):.*", statement) ?
                        Convert.toTimeStamp(value + " 23:59:59")
                        : Convert.toTimeStamp(value + "00:00:00");
            } catch (Exception e) {
                isString = true;
                log.error("DaoEngine.handleSqlSnippet()中日期转换失败，将以字符串类型进行比较！" + value);
            }
        }
        if (isNumber) {
            finalValue = Long.parseLong(value);
        }
        if (isString) {
            finalValue = value;
        }
        if (isTime) {
            try {
                finalValue = Convert.toTimeStamp(value);
            } catch (Exception e) {
                isString = true;
                log.error("between语句解析：时间转换失败！at DaoEngine.handleSqlSnippet()");
            }
        }
        sqlSnippet = fieldName + statement.substring(0, statement.indexOf(":")) + " :" + bindValueName;
        params.put(bindValueName, finalValue);
        if (isString) {
            String lengthName = bindValueName + "_length";
            sqlSnippet += " and length(" + fieldName + ") = :" + lengthName;
            params.put(lengthName, value.length());
        }
        return sqlSnippet;
    }

    /**
     * 处理oracle sql 语句in子句参数超过1000项就会报错的问题
     * 拆分成 where id (1, 2, ..., 1000) or id (1001, ...)
     *
     * @param fieldName 属性名（数据库查询字段）
     * @param inArray   包含in参数的数组
     * @return 形如"field in (...) or field in (...) "
     * @throws Exception
     */
    private static String getOracleSQLIn(String fieldName, String[] inArray, boolean isNotIn) {
        int maxLength = 1000;
        if (inArray.length <= maxLength) {
            // in参数小于1000无需拆分
            return fieldName + (isNotIn ? " NOT IN " : " IN ") + " (" + addQuote(StringUtils.join(inArray, ",")) + ") ";
        }
        // in参数超过1000个进行in语句拆分
        StringBuilder result = new StringBuilder();
        String[] tempArray = new String[maxLength];
        // 循环截取每1000个值拼成in语句
        for (int i = 0; i < (double) inArray.length / maxLength; i++) {
            // 循环拷贝inArray的1000个数据到到tempArray中, 最后一次拷贝的是inArray最后剩余的数据，可能不足1000个
            int leftNum = inArray.length - i * maxLength;
            if (leftNum < maxLength) {
                tempArray = new String[leftNum];
            }
            System.arraycopy(inArray, i * maxLength, tempArray, 0, tempArray.length);
            // 生成in语句
            String inStr = StringUtils.join(tempArray, ",");
            // 每个值都加上单引号，否则会影响使用索引
            inStr = addQuote(inStr);
            result.append(" or ").append(fieldName).append(" IN (").append(inStr).append(") ");
        }
        // 去除第一个" or "
        return "(" + result.substring(3) + ")";
    }

    /**
     * 包裹分页sql
     *
     * @param sql      完整sql
     * @param page     分页对象
     * @param paramMap 参数map
     * @return 包装分页语句后的sql
     */
    @SuppressWarnings("unchecked")
    public static String wrapPage(String sql, Pagination page, Map paramMap) {
        if (page == null) {
            return sql;
        }
        String template = "" +
                "SELECT * \n" +
                "FROM (SELECT \n" +
                "        BASE_.*, \n" +
                "        ROWNUM RN \n" +
                "      FROM ( {0} ) BASE_ \n" +
                "      WHERE ROWNUM <= :X_END ) \n" +
                "WHERE RN >= :X_START ";
        paramMap.put("X_START", page.getStart());
        paramMap.put("X_END", page.getEnd());
        log.debug(sql);
        return MessageFormat.format(template, sql);
    }

}
