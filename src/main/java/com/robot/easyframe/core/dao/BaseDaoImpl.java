package com.robot.easyframe.core.dao;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.common.DataContainerInterface;
import com.ai.appframe2.common.SessionManager;
import com.robot.easyframe.annotation.Cache;
import com.robot.easyframe.core.Query;
import com.robot.easyframe.core.cache.BaseCache;
import com.robot.easyframe.core.engine.DaoEngine;
import com.robot.easyframe.core.engine.DataEngine;
import com.robot.easyframe.def.Constants;
import com.robot.easyframe.model.Pagination;
import com.robot.easyframe.util.ResCommonUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * BaseDaoImpl
 *
 * @author luozhan
 * @date 2019-01
 * @link https://robot-l.github.io/BaseDao
 */
public class BaseDaoImpl<T extends DataContainer> implements BaseDao<T> {

    private static Log log = LogFactory.getLog(BaseDaoImpl.class);

    private Class<T> boClass;
    private boolean isUseCache = false;
    private Class<? extends BaseCache<? extends BaseDao<T>>> cacheClass;

    @SuppressWarnings("unchecked")
    public BaseDaoImpl() {
        // 获取泛型参数T的class
        Type genType = this.getClass().getGenericSuperclass();
        Type[] params = ((ParameterizedType) genType).getActualTypeArguments();
        if (params.length == 0) {
            log.error("继承BaseDao及BaseDaoImpl时必须添加泛型！！");
        }
        boClass = (Class<T>) params[0];
        if (boClass.isAnnotationPresent(Cache.class)) {
            isUseCache = true;
            Cache annotationConfig = boClass.getAnnotation(Cache.class);
            cacheClass = (Class<? extends BaseCache<? extends BaseDao<T>>>) annotationConfig.value();
        }
    }

    @Override
    public Class<T> getBoClass() {
        return boClass;
    }

    @Override
    public BaseDao<T> useCache(boolean useCache) {
        if (this.cacheClass == null) {
            throw new RuntimeException(String.format("Dao实例%s关联的Bean未配置缓存，无法使用useCache()方法，请先在Bean类名上使用注解@Cache", this.getClass().getSimpleName()));
        }
        log.info(String.format("Dao实例%s当前使用缓存查询：isUseCache=%b，更新isUseCache=%b", this.getClass().getSimpleName(), this.isUseCache, useCache));
        this.isUseCache = useCache;
        return this;
    }

    @Override
    public long getNewId() throws Exception {
        return DaoEngine.getNewId(boClass);
    }

    @Override
    public T getById(Long id) throws Exception {
        return DaoEngine.getBean(boClass, id);
    }

    @Override
    public T getOne(DataContainerInterface conditionBean) throws Exception {
        T[] result = getBy(conditionBean, new Pagination(1));
        return result.length == 0 ? null : result[0];
    }

    @Override
    public T getOne(String fieldName, String fieldValue) throws Exception {
        T[] result = getByField(fieldName, fieldValue);
        return result.length == 0 ? null : result[0];
    }

    @Override
    public T[] getBy(DataContainerInterface bean) throws Exception {
        return getBy(bean, null);
    }

    @Override
    public T[] getBy(DataContainerInterface bean, Pagination page) throws Exception {
        log.info(String.format("当前查询Bean：%s, 启用缓存查询：%s", this.boClass.getSimpleName(), String.valueOf(this.isUseCache)));
        return isUseCache ?
                DataEngine.getBeans(cacheClass, bean, page) :
                DaoEngine.getBeans(boClass, bean, page);
    }

    @Override
    public DataContainer[] getWithFkBy(DataContainerInterface conditionBean, Pagination page) throws Exception {
        return DaoEngine.getDcs(boClass, conditionBean, page);
    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] getByField(String fieldName, String... fieldValues) throws Exception {
        DataContainerInterface condition = new DataContainer();
        if (fieldValues.length == 0) {
            return (T[]) Array.newInstance(boClass, 0);
        }
        if (fieldValues.length == 1) {
            fieldValues = fieldValues[0].split(",");
        }
        if (fieldValues.length > 10 && isContinuousArray(fieldValues)) {
            // 如果查询值是连续的，改用between语句，提高性能
            String start = fieldValues[0], end = fieldValues[fieldValues.length - 1];
            if (start.compareTo(end) > 0) {
                String temp = start;
                start = end;
                end = temp;
            }
            condition.set(fieldName, Query.between(start, end));
        } else {
            condition.set(fieldName, Query.in(StringUtils.join(fieldValues, ",")));
        }
        if(getBy(condition)!=null){return null;}
        return getBy(condition);
    }

    @Override
    public T[] getByField(String fieldName, DataContainerInterface... fieldValues) throws Exception {
        String[] fieldValuesStr = ResCommonUtil.getFieldValues(fieldValues, fieldName);
        return getByField(fieldName, fieldValuesStr);
    }

    @Override
    public T[] getByField(String fieldName, List<? extends DataContainerInterface> fieldValues) throws Exception {
        return getByField(fieldName, fieldValues.toArray(new DataContainerInterface[0]));
    }

    @Override
    public T[] getAll() throws Exception {
        return getAll(null);
    }

    @Override
    public T[] getAll(Pagination page) throws Exception {
        return getBy(null, page);
    }

    @Override
    public T[] getAllFromDB() throws Exception {
        return DaoEngine.getBeans(boClass, null);
    }

    @Override
    public int update(T... beans) throws Exception {
        if (beans.length == 0) {
            return 0;
        }
        for (DataContainerInterface bean : beans) {
            if (!bean.isModified()) {
                bean.forceStsToUpdate();
            }
        }
        int result = (beans.length == 1) ? DaoEngine.save(beans[0]) : DaoEngine.save(beans);
        if (this.cacheClass != null) {
            BaseCache.refresh(cacheClass);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int update(List<T> beans) throws Exception {
        return update(beans.toArray((T[]) Array.newInstance(boClass, beans.size())));
    }

    @Override
    public int add(T... beans) throws Exception {
        if (beans.length == 0) {
            return 0;
        }
        handleNewBeans(beans);
        int result = (beans.length == 1) ? DaoEngine.save(beans[0]) : DaoEngine.save(beans);
        if (this.cacheClass != null) {
            BaseCache.refresh(cacheClass);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int add(List<T> beans) throws Exception {
        return add(beans.toArray((T[]) Array.newInstance(boClass, beans.size())));
    }

    @Override
    public int delete(T... beans) throws Exception {
        if (beans.length == 0) {
            return 0;
        }
        for (DataContainerInterface bean : beans) {
            bean.delete();
        }
        int result = (beans.length == 1) ? DaoEngine.save(beans[0]) : DaoEngine.save(beans);
        if (this.cacheClass != null) {
            BaseCache.refresh(cacheClass);
        }
        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public int delete(List<T> beans) throws Exception {
        return delete(beans.toArray((T[]) Array.newInstance(boClass, beans.size())));
    }

    @Override
    public <K extends DataContainer> int moveTo(BaseDao<K> destDao, T... beans) throws Exception {
        K[] destBeans = ResCommonUtil.copyBoArray(beans, destDao.getBoClass());
        delete(beans);
        return destDao.add(destBeans);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <K extends DataContainer> int moveTo(BaseDao<K> destDao, List<T> beans) throws Exception {
        return moveTo(destDao, beans.toArray((T[]) Array.newInstance(boClass, 0)));
    }

    @Override
    public int count(DataContainerInterface bean) throws Exception {
        return isUseCache ?
                DataEngine.count(cacheClass, bean) :
                DaoEngine.getBeansCount(boClass, bean);
    }

    @Override
    public int countAll() throws Exception {
        return count(null);
    }

    @Override
    public DataContainer[] executeQuery(String sql, Map params) throws Exception {
        return DaoEngine.getBeansFromSql(sql, params);
    }

    @Override
    public T[] executeQuery(String sql, Map params, Pagination page) throws Exception {
        sql = DaoEngine.wrapPage(sql, page, params);

        return DaoEngine.getBeansFromSql(boClass, sql, params);

    }

    @Override
    @SuppressWarnings("unchecked")
    public T[] executeQuery(String sql, DataContainer cond, Pagination page) throws Exception {
        Map<String, Object> map = DaoEngine.bean2sql(cond, false);
        sql = sql + " " + map.get("sql");
        Map paramMap = (Map) map.get("paramMap");
        if (page != null) {
            // 传分页条件则先获取数据条数，如果大于0再拼装分页参数进行查询
            int total = count(sql, paramMap);
            SessionManager.getUser().set(Constants.ReturnCode.TOTAL, total);
            if (total == 0) {
                return (T[])Array.newInstance(boClass, 0);
            }
            sql = DaoEngine.wrapPage(sql, page, paramMap);
            return DaoEngine.getBeansFromSql(boClass, sql, paramMap);
        } else {
            // 不传分页条件直接查询，总条数即查询结果的数量
            T[] result = DaoEngine.getBeansFromSql(boClass, sql, paramMap);
            SessionManager.getUser().set(Constants.ReturnCode.TOTAL, result.length);
            return result;
        }

    }

    @Override
    public DataContainer[] executeUnionQuery(String baseSql, DataContainer cond, Pagination page) throws Exception {
        Map<String, Object> map = DaoEngine.bean2sql(cond, false);
        String sql = baseSql + " " + map.get("sql");
        Map paramMap = (Map) map.get("paramMap");

        if (page != null) {
            // 传分页条件则先获取数据条数，如果大于0再拼装分页参数进行查询
            int total = count(sql, paramMap);
            SessionManager.getUser().set(Constants.ReturnCode.TOTAL, total);
            if (total == 0) {
                return new DataContainer[0];
            }
            sql = DaoEngine.wrapPage(sql, page, paramMap);
            return DaoEngine.getBeansFromSql(sql, paramMap);
        } else {
            // 不传分页条件直接查询，总条数即查询结果的数量
            DataContainer[] result = DaoEngine.getBeansFromSql(sql, paramMap);
            SessionManager.getUser().set(Constants.ReturnCode.TOTAL, result.length);
            return result;
        }
    }

    /**
     * 对sql进行计数
     *
     * @param sql      完整sql
     * @param paramMap 参数Map
     * @return
     * @throws Exception
     */
    private int count(String sql, Map paramMap) throws Exception {
        //    获取sql
        String template = "SELECT COUNT(*) TOTAL FROM ( {0} )";
        sql = MessageFormat.format(template, sql);
        return DaoEngine.getBeansFromSql(sql, paramMap)[0].getAsInt(Constants.ReturnCode.TOTAL);
    }


    /**
     * 处理新增beans数据，设置新增状态&添加主键
     */
    private void handleNewBeans(DataContainerInterface... beans) throws Exception {
        // 获取主键，如果有多个只会取其中一个
        String keyName = boClass.newInstance().getObjectType().getMainAttr();

        String[] sequence = null;
        for (int i = 0; i < beans.length; i++) {
            DataContainerInterface bean = beans[i];
            if (!bean.isNew()) {
                bean.setStsToNew();
            }
            Object value = bean.get(keyName);
            // 没有主键则新增主键
            if (value == null || "".equals(value.toString())) {
                if (sequence == null) {
                    sequence = DaoEngine.getNewId(boClass, beans.length);
                }
                bean.set(keyName, sequence[i]);
            }
        }
    }

    /**
     * 判断是否是连续数组
     *
     * @param array 数组
     * @return boolean
     */
    private boolean isContinuousArray(String[] array) {
        for (String item : array) {
            if (item.length() >= String.valueOf(Long.MAX_VALUE).length() || !StringUtils.isNumeric(item)) {
                return false;
            }
        }

        Arrays.sort(array);
        long result = Math.abs(Long.parseLong(array[0]) - Long.parseLong(array[array.length - 1]));
        return result <= array.length - 1;
    }

    public static void main(String[] args) {
        DataContainer d = new DataContainer();
        d.setDiaplayAttr("or", "state", "4");
        d.setDiaplayAttr("or", "time", "4");
        d.setExtAttr("or", d);
        d.set("state", "2");
        d.set("or state", "4");
        DaoEngine.bean2sql(d, true);
    }
}
