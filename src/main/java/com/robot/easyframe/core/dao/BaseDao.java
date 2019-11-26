package com.robot.easyframe.core.dao;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.common.DataContainerInterface;
import com.sun.istack.internal.Nullable;
import com.robot.easyframe.model.Pagination;

import java.util.List;
import java.util.Map;

/**
 * BaseDao
 *
 * @author luozhan
 * @date 2019-01
 * @link https://robot-l.github.io/BaseDao
 */
public interface BaseDao<T extends DataContainer> {

    /**
     * 获取Dao对应的Bo类型
     *
     * @return
     */
    Class<T> getBoClass();

    /**
     * 开启或禁用缓存查询
     * <p>
     * 注：
     * 1.该方法只能在配置了缓存Bean的Dao实例中调用，否则抛出RuntimeException
     * 2.此状态只会在同一个Service.get(Dao)生成的实例中有效，并不是永久的
     * 4.缓存开关只对查询生效，禁用缓存并不会影响增删改之后的刷新缓存操作
     *
     * @param useCache 是否使用缓存
     * @return
     */
    BaseDao<T> useCache(boolean useCache);

    /**
     * 从对应表序列中获取新ID
     *
     * @return 主键
     * @throws Exception
     */
    long getNewId() throws Exception;

    /**
     * 根据id查询
     *
     * @param id
     * @return
     * @throws Exception
     */
    T getById(Long id) throws Exception;

    /**
     * 根据条件查询第一条数据，查不到返回null
     *
     * @param conditionBean 条件bean
     * @return
     */
    T getOne(DataContainerInterface conditionBean) throws Exception;

    /**
     * 根据条件查询第一条数据，查不到返回null
     *
     * @param fieldName  指定通过哪个属性查询
     * @param fieldValue 由逗号分隔的字符串，或者包含指定属性的数组
     * @return
     */
    @Nullable
    T getOne(String fieldName, String fieldValue) throws Exception;

    /**
     * 根据条件bean查询
     *
     * @param conditionBean
     * @return 对象数组（没有数据为空数组，不会为null）
     */
    T[] getBy(DataContainerInterface conditionBean) throws Exception;

    /**
     * 根据条件bean查询(分页）
     *
     * @param conditionBean 条件bean
     * @param page
     * @return
     */
    T[] getBy(DataContainerInterface conditionBean, Pagination page) throws Exception;

    /**
     * 根据条件bean查询（带分页，查询结果中同时包含外键属性）
     * 该方法能实现多表查询，但无法获取分页前的总数，请注意
     *
     * @param conditionBean 条件bean
     * @param page
     * @return
     * @throws Exception
     */
    @Deprecated
    DataContainer[] getWithFkBy(DataContainerInterface conditionBean, Pagination page) throws Exception;

    /**
     * 根据指定属性对fieldValues中的值进行查询
     *
     * @param fieldName   指定通过哪个属性查询
     * @param fieldValues 由逗号分隔的字符串，或者包含指定属性的数组
     * @return
     */
    T[] getByField(String fieldName, String... fieldValues) throws Exception;

    /**
     * 根据指定属性对数组中每个bo对应属性的值进行查询
     *
     * @param fieldName   指定通过哪个属性查询
     * @param fieldValues 包含指定属性的bo数组
     * @return
     */
    T[] getByField(String fieldName, DataContainerInterface... fieldValues) throws Exception;

    /**
     * 根据指定属性对数组中每个bo对应属性的值进行查询
     *
     * @param fieldName   指定通过哪个属性查询
     * @param fieldValues 包含指定属性的bo数组
     * @return
     */
    T[] getByField(String fieldName, List<? extends DataContainerInterface> fieldValues) throws Exception;

    /**
     * 查询所有数据
     * 注：为避免性能问题，只会展示前8000条，如果想查更多，请用getAll(new Pagination(查询条数))
     *
     * @return
     */
    T[] getAll() throws Exception;

    /**
     * 查询所有数据(分页)
     *
     * @param page 分页对象
     * @return
     */
    T[] getAll(Pagination page) throws Exception;

    /**
     * 查询所有数据(强制从数据库中查询，不用缓存)
     *
     * @return
     * @throws Exception
     */
    T[] getAllFromDB() throws Exception;

    /**
     * 更新/批量更新
     * 根据主键值查找记录并update设值了的属性
     * 注意：add/update/delete方法调用后都会调用bo的setStsToOld方法
     *
     * @param beans
     * @return 操作数
     * @throws Exception
     */
    int update(T... beans) throws Exception;

    /**
     * 批量更新
     * 注意：add/update/delete方法调用后都会调用bo的setStsToOld方法
     *
     * @param beans
     * @return 操作数
     * @throws Exception
     */
    int update(List<T> beans) throws Exception;

    /**
     * 新增/批量新增
     * 如果没有主键，将自动设置主键
     * 注意：add/update/delete方法调用后都会调用bo的setStsToOld方法
     *
     * @param beans
     * @return 操作数
     * @throws Exception
     */
    int add(T... beans) throws Exception;

    /**
     * 批量新增
     * 如果没有主键，将自动设置主键
     * 注意：add/update/delete方法调用后都会调用bo的setStsToOld方法
     *
     * @param beans
     * @return 操作数
     * @throws Exception
     */

    int add(List<T> beans) throws Exception;

    /**
     * 删除/批量删除
     *
     * @param beans
     * @return 操作数
     * @throws Exception
     */
    int delete(T... beans) throws Exception;

    /**
     * 批量删除
     *
     * @param beans
     * @return 操作数
     * @throws Exception
     */
    int delete(List<T> beans) throws Exception;

    /**
     * 移动到另一个表（A表删除+B表新增）
     * 注意，主键也将保持一致
     *
     * @param destDao 目标表的dao实例
     * @param beans   数据
     * @return 操作数
     * @throws Exception
     */
    <K extends DataContainer> int moveTo(BaseDao<K> destDao, T... beans) throws Exception;

    /**
     * 移动到另一个表（A表删除+B表新增）
     * 注意，主键也将保持一致
     *
     * @param destDao 目标表的dao实例
     * @param beans   数据
     * @param <K>
     * @return 操作数
     * @throws Exception
     */
    <K extends DataContainer> int moveTo(BaseDao<K> destDao, List<T> beans) throws Exception;

    /**
     * 获取数量（主要用于获得分页查询业务的count总数）
     *
     * @param conditionBean 条件bo
     * @return 操作数
     * @throws Exception
     */
    int count(DataContainerInterface conditionBean) throws Exception;

    /**
     * 获取总数
     *
     * @return 总数
     * @throws Exception
     */
    int countAll() throws Exception;

    /**
     * 通过sql执行特殊查询
     *
     * @param sql    完整sql语句，执行特殊查询（如聚合函数等）
     * @param params 参数Map
     * @return 查询的数据可能不在同一个bo类中，所以用DataContainer包装
     * @throws Exception
     */
    DataContainer[] executeQuery(String sql, Map params) throws Exception;

    /**
     * 通过sql执行分页查询（针对不能用getBy()方法查询的sql）
     * 获取查询结果的总数请使用ResCommonUtil.getTotal()
     *
     * @param sql    完整sql语句
     * @param params 参数Map
     * @param page   分页信息
     * @return bo
     * @throws Exception
     */
    T[] executeQuery(String sql, Map params, Pagination page) throws Exception;

    /**
     * 通过sql执行分页查询（针对不能用getBy()方法查询的sql）
     * 获取查询结果的总数请使用ResCommonUtil.getTotal()
     *
     * @param sql    sql语句（sql中须包含"where"，结尾处不需要拼"and"）
     * @param cond   普通条件
     * @param page   分页信息
     * @return bo
     * @throws Exception
     */
    T[] executeQuery(String sql, DataContainer cond, Pagination page) throws Exception;

    /**
     * 连表查询
     *
     * @param baseSql 连表的基本sql，要指定关联条件，如select ... from A,B where A.x = B.x
     * @param cond    条件bean，set条件时请指定属性所属表的别名，如set("A.ID","111");
     * @param page    page对象，若想查询前n条时可以传new Pagination(n);
     * @return
     * @throws Exception
     */
    DataContainer[] executeUnionQuery(String baseSql, DataContainer cond, Pagination page) throws Exception;
}
