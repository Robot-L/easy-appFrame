package com.robot.easyframe.util;

import com.ai.appframe2.common.ServiceManager;
import com.ai.appframe2.common.SessionManager;
import com.ai.appframe2.privilege.UserInfoInterface;
import com.ai.appframe2.service.ServiceFactory;
import com.robot.easyframe.def.Constants;
import com.robot.easyframe.def.Constants.ReturnCode;
import com.robot.easyframe.model.OperateDTO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;



/**
 * Service工具类
 *
 * @author luozhan
 * @create 2019-01
 */
public class ServiceUtil {
    private static transient final Log log = LogFactory.getLog(ServiceUtil.class);


    /**
     * 包装操作类接口执行成功的Map数据
     * （已过时,请使用createResultMap方法）
     *
     * @param recordId  业务编码
     * @param total     操作总数
     * @param errorData 错误数据
     * @param info      提示信息
     * @return
     * @author luozhan
     */
    @Deprecated
    public static Map createSuccessMap(long recordId, int total, List<Map> errorData, String info) {
        Map<String, Object> returnMap = new HashMap<>(6);
        returnMap.put(ReturnCode.RECORD_ID, recordId);
        int failureNum = errorData == null ? 0 : errorData.size();
        returnMap.put(ReturnCode.FAILURE_NUM, failureNum);
        returnMap.put(ReturnCode.SUCCESS_NUM, total - failureNum);
        returnMap.put(ReturnCode.ERROR_DATA, errorData);
        returnMap.put(ReturnCode.RESULTINFO, info);
        returnMap.put(ReturnCode.RESULTCODE, Constants.ResultCode.SUCCESS);
        return returnMap;
    }

    /**
     * 包装操作类接口执行结果的Map数据
     * 根据操作成功数量自动判断业务操作结果（成功or失败）
     *
     * @param recordId    业务编码
     * @param successData 成功列表
     * @param errorData   错误数据(List中包含Map，含有两个key：RES_NO和CHECK_INFO)
     * @param info        提示信息
     * @return
     * @author luozhan
     */
    public static Map<String, Object> createResultMap(long recordId, List<?> successData, List<?> errorData, String info) {
        if (successData == null) {
            successData = Collections.emptyList();
        }
        if (errorData == null) {
            errorData = Collections.emptyList();
        }
        if (info == null || info.isEmpty()) {
            info = "执行完毕。";
        }
        Map<String, Object> returnMap = new HashMap<>(8);
        returnMap.put(ReturnCode.RECORD_ID, recordId);
        returnMap.put(ReturnCode.TOTAL, successData.size() + errorData.size());
        returnMap.put(ReturnCode.SUCCESS_NUM, successData.size());
        // 成功数据暂时屏蔽，以后有需要时再放开
        returnMap.put(ReturnCode.SUCCESS_DATA, null);
        returnMap.put(ReturnCode.FAILURE_NUM, errorData.size());
        // 限制错误记录数，数量过大(5000)将会导致setAjax方法奇慢无比
        returnMap.put(ReturnCode.ERROR_DATA, errorData.stream().limit(Constants.CheckInfo.MAX_ERR_RECORD).collect(Collectors.toList()));
        returnMap.put(ReturnCode.RESULTINFO, info);
        returnMap.put(ReturnCode.RESULTCODE, successData.size() == 0 ? Constants.ResultCode.FAILURE : Constants.ResultCode.SUCCESS);
        return returnMap;
    }


    /**
     * 包装操作类接口执行失败的Map数据
     *
     * @param errorData 错误内容
     * @return
     * @author luozhan
     */
    public static Map createFailureMap(List<Map> errorData) {
        return createResultMap(0, Collections.EMPTY_LIST, errorData, null);
    }

    /**
     * 包装操作类接口执行失败的Map数据(使用批量校验类CheckResult)
     *
     * @param recordId    流水号
     * @param checkResult 批量校验类
     * @return
     */
    public static Map<String, Object> createResultMap(long recordId, CheckResult checkResult) {
        return createResultMap(recordId, checkResult.getSuccessList(), checkResult.getErrorList(), null);
    }

    /**
     * 从Session中获取操作员信息
     *
     * @return OperateDTO
     */
    public static OperateDTO getOperateInfo(long recordId) {
        UserInfoInterface user = SessionManager.getUser();
        if (user == null) {
            throw new RuntimeException("获取操作员信息失败，请在session中设置user信息！");
        }
        OperateDTO operate = new OperateDTO(String.valueOf(user.getID()), String.valueOf(user.getOrgId()), recordId);
        operate.setDoneCode(recordId);
        operate.setOpId(String.valueOf(user.getID()));
        operate.setOrgId(String.valueOf(user.getOrgId()));
        operate.setDoneDate(DateUtil.now());
        return operate;
    }




    /**
     * 从Session中获取操作员信息(无DoneCode)
     *
     * @return OperateDTO
     */
    public static OperateDTO getOperateInfo() {
        return getOperateInfo(0L);
    }

    /**
     * 获取SV或者DAO实例
     *
     * @param clazz SV或者Dao的Class类型
     * @return SV或者DAO实例
     * @author luozhan
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> clazz) {
        return (T) ServiceFactory.getService(clazz);
    }


    /**
     * 往session中设置操作员信息
     *
     * @param opId
     * @param orgId
     * @throws Exception
     */
    public static void setUser(long opId, long orgId) throws Exception {
        UserInfoInterface userInfo = ServiceManager.getNewBlankUserInfo();
        userInfo.setID(opId);
        userInfo.setOrgId(orgId);
        SessionManager.setUser(userInfo);
    }

}
