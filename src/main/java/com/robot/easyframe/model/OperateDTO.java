package com.robot.easyframe.model;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.common.DataStructInterface;
import com.ai.appframe2.common.DataType;
import com.robot.easyframe.util.DateUtil;

import java.sql.Timestamp;

/**
 * 操作对象DTO
 *
 * @author luozhan
 * @date 2019-01
 */
public class OperateDTO extends DataContainer implements DataStructInterface {
    private static String m_boName = "com.asiainfo.res.core.atom.instance.base.model.OperateDTO";

    public final static String S_OrgId = "ORG_ID";
    public final static String S_OpId = "OP_ID";
    public final static String S_DoneCode = "DONE_CODE";
    public final static String S_DoneDate = "DONE_DATE";

    public OperateDTO() {

    }

    public OperateDTO(String opId, String orgId, long doneCode) {
        this.setOpId(opId);
        this.setOrgId(orgId);
        this.setDoneCode(doneCode);
        this.setDoneDate(DateUtil.now());
    }

    public void initOrgId(String value) {
        this.initProperty(S_OrgId, value);
    }

    public void setOrgId(String value) {
        this.set(S_OrgId, value);
    }

    public void setOrgIdNull() {
        this.set(S_OrgId, null);
    }

    public String getOrgId() {
        return DataType.getAsString(this.get(S_OrgId));
    }

    public String getOrgIdInitialValue() {
        return DataType.getAsString(this.getOldObj(S_OrgId));
    }

    public void initOpId(String value) {
        this.initProperty(S_OpId, value);
    }

    public void setOpId(String value) {
        this.set(S_OpId, value);
    }

    public void setOpIdNull() {
        this.set(S_OpId, null);
    }

    public String getOpId() {
        return DataType.getAsString(this.get(S_OpId));
    }

    public String getOpIdInitialValue() {
        return DataType.getAsString(this.getOldObj(S_OpId));
    }

    public void initDoneCode(long value) {
        this.initProperty(S_DoneCode, value);
    }

    public void setDoneCode(long value) {
        this.set(S_DoneCode, value);
    }

    public void setDoneCodeNull() {
        this.set(S_DoneCode, null);
    }

    public long getDoneCode() {
        return DataType.getAsLong(this.get(S_DoneCode));
    }

    public long getDoneCodeInitialValue() {
        return DataType.getAsLong(this.getOldObj(S_DoneCode));
    }

    public void initDoneDate(Timestamp value) {
        this.initProperty(S_DoneDate, value);
    }

    public void setDoneDate(Timestamp value) {
        this.set(S_DoneDate, value);
    }

    public void setDoneDateNull() {
        this.set(S_DoneDate, null);
    }

    public Timestamp getDoneDate() {
        return DataType.getAsDateTime(this.get(S_DoneDate));
    }

    public Timestamp getDoneDateInitialValue() {
        return DataType.getAsDateTime(this.getOldObj(S_DoneDate));
    }

}
