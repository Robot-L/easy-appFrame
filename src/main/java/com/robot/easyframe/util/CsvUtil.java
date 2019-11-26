package com.robot.easyframe.util;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.common.SessionManager;
import com.ai.appframe2.privilege.UserInfoInterface;
import com.robot.easyframe.def.Constants;
import com.robot.easyframe.model.Pagination;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * csv常用方法工具类
 *
 * @author luozhan
 * @date 2019年10月22日
 */
public class CsvUtil {

    private static transient final Log log = LogFactory.getLog(CsvUtil.class);


    /**
     * 包装接口返回Map数据
     *
     * @param datas 数据集，
     * @param total 用于前台分页组件展示记录总数，注意 total 并不等于 datas.size()
     * @param info  成功提示或错误提示信息
     * @param code  结果编码 ResultCode.SUCCESS/ResultCode.FAILURE
     * @return
     */
    public static Map createReturnMap(List<?> datas, int total, String info, String code) {
        Map<String, Object> returnMap = new HashMap<>(4);
        returnMap.put(Constants.ReturnCode.OUTDATA, datas);
        returnMap.put(Constants.ReturnCode.TOTAL, total);
        returnMap.put(Constants.ReturnCode.RESULTINFO, info);
        returnMap.put(Constants.ReturnCode.RESULTCODE, code);
        return returnMap;
    }

    /**
     * 包装接口返回Map数据
     *
     * @param datas
     * @param total
     * @param info
     * @return
     */
    public static Map createReturnMap(List<?> datas, int total, String info) {
        return createReturnMap(datas, total, info, Constants.ResultCode.SUCCESS);
    }

    /**
     * 包装接口返回Map数据（分页查询时常用）
     *
     * @param datas
     * @param total
     * @return
     */
    public static Map createReturnMap(List<?> datas, int total) {
        return createReturnMap(datas, total, "执行成功", Constants.ResultCode.SUCCESS);
    }

    /**
     * 包装接口返回Map数据（分页查询时常用）
     *
     * @param datas DataContainer数组
     * @param total 总数量信息（用于分页）
     * @return
     */
    public static Map createReturnMap(DataContainer[] datas, int total) throws Exception {
        List<Map> transResult = TranslateUtil.parse(datas);
        return createReturnMap(transResult, total, "执行成功", Constants.ResultCode.SUCCESS);
    }

    /**
     * 包装接口返回Map数据（查询下拉框时常用）
     *
     * @param datas DataContainer数组
     * @return
     */
    public static Map createReturnMap(DataContainer[] datas) throws Exception {
        List<Map> transResult = TranslateUtil.parse(datas);
        return createReturnMap(transResult, 0, "执行成功", Constants.ResultCode.SUCCESS);
    }

    /**
     * 包装接口返回Map数据(对外查询接口常用)
     *
     * @param datas 数据集， 可以用Convert.toList()将DataContainer[]转换成List
     * @return
     */
    public static Map createReturnMap(List<?> datas) {
        return createReturnMap(datas, 0, "执行成功", Constants.ResultCode.SUCCESS);
    }

    /**
     * 包装接口返回Map数据(不需要返回具体数据时)
     * eg. createReturnMap("执行成功")
     *
     * @param info 主要是为了看出是成功返回,给个定值"执行成功"即可
     * @return
     */
    public static Map createReturnMap(String info) {
        return createReturnMap(null, 0, info, Constants.ResultCode.SUCCESS);
    }

    /**
     * 从Session中获取分页信息（只适用于Session中有分页信息的场景，一般为前台调用）
     * 如果获取不到将返回null
     * <p>
     * 注：建议使用{@link #getPageInfo(Map)}，以适用非前台调用（无Session）的场景
     *
     * @return Pagination
     * @author luozhan
     */
    public static Pagination getPageInfo() throws Exception {
        UserInfoInterface user = SessionManager.getUser();
        if (user != null) {
            Map pageInfo = (Map) user.get(Constants.ReturnCode.PAGE_INFO);
            if (pageInfo != null) {
                Integer currentPage = MapUtils.getInteger(pageInfo, Pagination.CURRENT_PAGE);
                Integer pageSize = MapUtils.getInteger(pageInfo, Pagination.PAGE_SIZE);
                if (currentPage != null && pageSize != null) {
                    return new Pagination(pageSize, currentPage);
                }
            }
        }
        return null;
    }

    /**
     * 从入参Map中获取分页信息，如果获取不到取Session中的
     * 如果都没有，返回null
     *
     * @param input 如果是主动传分页参数，参数Map中须包含"CURRENT_PAGE"和"PAGE_SIZE"
     * @return Pagination
     * @author luozhan
     */
    public static Pagination getPageInfo(Map<?, ?> input) throws Exception {
        Integer currentPage = MapUtils.getInteger(input, Pagination.CURRENT_PAGE);
        Integer pageSize = MapUtils.getInteger(input, Pagination.PAGE_SIZE);
        if (currentPage == null || pageSize == null) {
            return getPageInfo();
        } else {
            return new Pagination(pageSize, currentPage);
        }

    }

    /**
     * 获取非空的字符串
     * 如果为空会抛出"入参不能为空"异常
     *
     * @param input 参数Map
     * @param key   key
     * @return String
     * @author luozhan
     */
    public static String getNotNullString(Map input, String key) {
        Object obj = input.get(key);
        if (obj == null || "".equals(obj)) {
            throw new RuntimeException("入参不存在或为空字符串：" + key);
        }
        return String.valueOf(obj);
    }

    /**
     * 获取非空的数字（接受数字类型值或数字型字符串）
     * 如果为空会抛出"入参不能为空"异常
     *
     * @param input 参数Map
     * @param key   key
     * @return int
     * @author luozhan
     */
    public static int getNotNullNumber(Map input, String key) {
        Object obj = input.get(key);
        if (obj == null || "".equals(obj)) {
            throw new RuntimeException("入参不存在或为空字符串：" + key);
        }
        return Integer.parseInt(String.valueOf(obj));
    }

    /**
     * 获取非空的bool值（接受bool类型或字符串"true"，"false"（忽略大小写））
     * 如果为空会抛出"入参不能为空"异常
     * 如果不为可接受的格式，抛出"入参不合法"异常
     *
     * @param input 参数Map
     * @param key   key
     * @return boolean
     * @author luozhan
     */
    public static boolean getNotNullBool(Map input, String key) {
        Object obj = input.get(key);
        if (obj == null || "".equals(obj)) {
            throw new RuntimeException("入参不存在或为空字符串：" + key);

        }
        String str = String.valueOf(obj);
        boolean bool = Boolean.parseBoolean(str);
        if (!bool && !String.valueOf(false).equalsIgnoreCase(str)) {
            throw new RuntimeException("接口入参不合法：" + key + "=" + str);

        }
        return bool;
    }

    /**
     * 在Map中获取List
     *
     * @param map
     * @param key
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T> List<T> getList(Map map, String key) {
        return map == null || map.isEmpty() ? new ArrayList() : (List<T>) map.get(key);
    }

}
