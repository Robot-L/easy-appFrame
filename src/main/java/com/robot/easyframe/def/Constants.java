package com.robot.easyframe.def;

/**
 * @author luozhan
 * @date 2019-10
 */
public interface Constants {
    /**
     * 操作结果信息常量定义
     */
    interface ReturnCode {
        /** RESULTCODE:操作返回的编码 */
        String RESULTCODE = "RESULTCODE";

        /** RESULTINFO:操作返回的结果信息 */
        String RESULTINFO = "RESULTINFO";

        /** OUTDATA:操作返回的记录集 */
        String OUTDATA = "OUTDATA";


        /** TOTAL:操作条数 */
        String TOTAL = "TOTAL";

        /** SUCCESS_NUM:操作成功条数 */
        String SUCCESS_NUM = "SUCCESS_NUM";

        /** FAILURE_NUM:操作失败条数 */
        String FAILURE_NUM = "FAILURE_NUM";

        /** ERROR_DATA:操作成功数据 */
        String SUCCESS_DATA = "SUCCESS_DATA";

        /** ERROR_DATA:操作失败数据 */
        String ERROR_DATA = "ERROR_DATA";

        /** ERROR_FILE_URL:错误信息文件名 */
        String ERROR_FILE_URL = "ERROR_FILE_URL";

        /** RECORD_ID:操作业务流水 */
        String RECORD_ID = "RECORD_ID";

        /** PAGE_INFO:分页信息 */
        String PAGE_INFO = "PAGE_INFO";

        /** FILE_NAME:文件名 */
        String FILE_NAME = "FILE_NAME";

        /** MSG:q前台返回的错误信息描述 */
        String MSG = "MSG";

        /** MSG_TYPE:消息类型 */
        String MSG_TYPE = "MSG_TYPE";
    }
    /**
     * 操作结果编码
     */
    interface ResultCode {
        /** SUCCESS:操作成功 */
        String SUCCESS = "0";

        /** FAILED:操作失败 */
        String FAILURE = "-1";
    }

    interface CheckInfo {
        /**
         * 最大错误记录数
         * 如果错误数据过大，会影响页面setAjax的速度
         */
        int MAX_ERR_RECORD = 5000;
        /** 校验类校验结果的key */
        String KEY = "RES_NO";
        /** 校验类校验结果的value */
        String VALUE = "CHECK_INFO";
    }

}
