package com.robot.easyframe.util;

import com.ai.appframe2.bo.DataContainer;
import com.ai.common.i18n.CrmLocaleFactory;
import com.asiainfo.common.exception.core.spi.BusiException;
import com.robot.easyframe.core.dao.BaseDao;
import com.robot.easyframe.def.Constants;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.robot.easyframe.util.LambdaExceptionUtil.FunctionWithExceptions;

/**
 * 业务校验类
 *
 * 用于批量校验数据的业务合法性，校验类型包括：
 * <pre>
 *     1.数据中指定字段必须在指定数据库表中存在、或不存在
 *     2.数据中指定字段必须与指定值相等、或不相等
 *     2.数据中指定字段必须在指定范围中、或不在指定范围中
 * </pre>
 *
 * 注：
 * 此类只进行业务型校验（和数据库交互），基本数据校验请在csv层进行
 *
 * @author luozhan
 * @date 2019-02
 */
public class CheckResult<T extends DataContainer> {


    private String resNoField;
    private T[] source;
    private T[] existData;
    private List<T> successList = new ArrayList<>();
    private List<Map<String, String>> errorList = new ArrayList<>();

    private boolean lazyMode = false;
    private boolean isThrowException = false;

    private CheckResult() {
    }

    /**
     * 初始化CheckResult对象
     * 指定校验源source和数据库已存在的数据existData
     *
     * @param source     待校验数据，程序包装的bo数组，须包含有入参resNoField的属性
     * @param existData  从数据库中查询得到的数据
     * @param resNoField 代表资源号码的属性名，如号码业务是"MSISDN"、sim卡业务是"ICCID"
     */
    public static <K extends DataContainer> CheckResult<K> init(K[] source, K[] existData, String resNoField) {
        CheckResult<K> check = new CheckResult<>();
        check.source = source;
        check.resNoField = resNoField;
        check.existData = existData;
        // 初始化数据库存在的数据都为合法数据
        Collections.addAll(check.successList, existData);
        return check;
    }

    /**
     * 初始化CheckResult对象
     * 此方法会根据source和resNoField去数据库查询出完整bo列表
     *
     * @param source     待校验数据，程序包装的bo数组，须包含有入参resNoField的属性
     * @param resNoField 代表资源号码的属性名，如号码业务是"MSISDN"、sim卡业务是"ICCID"
     * @param dao        校验数据对应的dao实例
     */
    public static <K extends DataContainer> CheckResult<K> init(K[] source, String resNoField, BaseDao<K> dao) throws Exception {
        K[] existData = dao.getByField(resNoField, source);
        return init(source, existData, resNoField);
    }

    /**
     * 初始化CheckResult对象
     * 适用于校验那些已经从数据库中查出的数据
     *
     * @param existData  从数据库中查询获得的bo数组
     * @param resNoField 代表资源号码的属性名，如号码业务是"MSISDN"、sim卡业务是"ICCID"
     * @param <K>        bo
     * @return CheckResult<K>
     */
    public static <K extends DataContainer> CheckResult<K> initByExist(K[] existData, String resNoField) {
        // 此时existData和source相同
        return init(existData, existData, resNoField);
    }

    /**
     * 开启抛异常模式
     * 遇到校验不通过直接抛出异常，否则记录校验失败数据供后面处理
     */
    public CheckResult exceptionMode() {
        this.isThrowException = true;
        return this;
    }

    /**
     * 开启懒加载模式
     * 非抛异常模式下，遇到某个检查规则校验不过后不再进行后续校验，节省时间
     */
    public CheckResult lazyMode() {
        this.lazyMode = true;
        return this;
    }


    public void setSuccessList(List<T> successList) {
        this.successList = successList;
    }

    public void setErrorList(List<Map<String, String>> errorList) {
        this.errorList = errorList;
    }

    /**
     * 获取校验成功列表
     *
     * @return successList
     */
    public List<T> getSuccessList() {
        return successList;
    }

    /**
     * 获取校验失败列表
     * 列表中只包含resNo和checkInfo
     *
     * @return errorList
     */
    public List<Map<String, String>> getErrorList() {
        return errorList;
    }

    /**
     * 获取成功数目
     *
     * @return int
     */
    public int getSuccessNum() {
        return successList.size();
    }

    /**
     * 获取失败数目
     *
     * @return int
     */
    public int getErrorNum() {
        return errorList.size();
    }

    /**
     * 获取总数
     *
     * @return int
     */
    public int getTotalNum() {
        return source.length;
    }

    private boolean canStop() {
        return lazyMode && errorList.size() > 0;
    }

    private void addError(String resNo, String errorInfo) {
        Map<String, String> map = new HashMap<>(2);
        map.put(Constants.CheckInfo.KEY, resNo);
        map.put(Constants.CheckInfo.VALUE, errorInfo);
        this.errorList.add(map);
    }

    /**
     * 必须存在校验
     *
     * @param errorInfo 不存在的错误提示语
     * @return this
     */
    public CheckResult checkExist(String errorInfo) throws BusiException {
        if (lazyMode && existData.length > 0) {
            return this;
        }
        List<T> notExistData = ResCommonUtil.minus(source, existData, resNoField);
        if (notExistData.size() > 0 && isThrowException) {
            String resNos = notExistData.stream().map(v -> v.getAsString(resNoField)).collect(Collectors.joining(","));
            this.throwException(errorInfo, resNos);
        }
        for (T bean : notExistData) {
            this.addError(bean.getAsString(resNoField), errorInfo);
        }
        return this;
    }

    /**
     * 必须不存在校验
     * 若存在，则加入errorList
     * 一般用于生成业务
     *
     * @param errorInfo 存在的错误提示语
     * @return this
     */
    public CheckResult checkNotExist(String errorInfo) {
        if (canStop()) {
            return this;
        }
        // 执行此方法代表是生成业务，此时existDatas因为是数据库查出来数据，都是存在数据
        // 所以successList应等于source减去existDatas，并且existDatas要移动到errorList
        this.successList = ResCommonUtil.minus(source, existData, resNoField);
        for (T existBo : existData) {
            this.addError(existBo.getAsString(resNoField), errorInfo);
        }
        return this;
    }

    /**
     * 必须不存在校验（指定一个Dao，在其对应的表中按唯一属性进行比较）
     * 若存在，则加入errorList
     * 注意，必须先调用checkNotExist(String errorInfo)校验，才能调用此方法
     *
     * @param errorInfo 存在的错误提示语，当throwExceptionMode开启时传的是异常信息编码（第一个占位符是唯一属性对应值，第二个占位符是equalValue）
     * @return this
     */
    public <K extends DataContainer, D extends BaseDao<K>> CheckResult checkNotExist(String errorInfo, D dao) throws Exception {
        if (canStop()) {
            return this;
        }
        K[] existData = dao.getByField(resNoField, successList);
        // 下列逻辑和checkNotExist(String)相同
        for (K existBo : existData) {
            this.addError(existBo.getAsString(resNoField), errorInfo);
            ResCommonUtil.match(successList, Arrays.asList(existData), resNoField, (a, b) -> successList.remove(a));
        }
        return this;
    }

    /**
     * 属性相等校验，
     * 将属性值不等于equalValue的数据移动到errorList
     *
     * @param errorInfo  属性不相等的错误提示语，当throwExceptionMode开启时传的是异常信息编码（第一个占位符是唯一属性对应值，第二个占位符是equalValue）
     * @param checkField 校验字段
     * @param equalValue 比对的值，传多个的时候只要有一个相等就算校验通过
     * @return this
     */
    public CheckResult checkEqual(String errorInfo, String checkField, Object... equalValue) throws Exception {
        return checkEqual(errorInfo, checkField, null, null, equalValue);
    }

    /**
     * 属性相等校验（可对校验的值做处理）
     * 将属性值不等于equalValue的数据移动到errorList
     *
     * @param errorInfo  错误提示语，当throwExceptionMode开启时传的是异常信息编码（第一个占位符是唯一属性对应值，第二个占位符是equalValue）
     * @param checkField 校验字段
     * @param f1         针对校验的值的处理函数，如只校验号码的前4位（v->v.substring(0,4)）
     * @param f2         针对对比的值的处理函数，如只需要比对值的前4位（v->v.substring(0,4)）
     * @param equalValue 比对的值，传多个的时候只要有一个相等就算校验通过
     * @return this
     */
    @SuppressWarnings("Duplicates")
    public CheckResult checkEqual(String errorInfo,
                                  String checkField,
                                  FunctionWithExceptions<String, String, Exception> f1,
                                  FunctionWithExceptions<String, String, Exception> f2,
                                  Object... equalValue) throws Exception {
        if (canStop()) {
            return this;
        }
        f1 = Optional.ofNullable(f1).orElse(FunctionWithExceptions.identity());
        f2 = Optional.ofNullable(f2).orElse(FunctionWithExceptions.identity());
        List<T> temp = new ArrayList<>(successList);
        for (T bo : temp) {
            boolean anyOneEqual = false;
            for (Object o : equalValue) {
                Object fieldValue = bo.get(checkField);
                if (f2.apply(String.valueOf(o)).equals(f1.apply(String.valueOf(fieldValue)))) {
                    anyOneEqual = true;
                    break;
                }
            }
            if (!anyOneEqual) {
                if (isThrowException) {
                    this.throwException(errorInfo, bo.getAsString(resNoField), StringUtils.join(equalValue, ","));
                } else {
                    this.addError(bo.getAsString(resNoField), errorInfo);
                    successList.remove(bo);
                }
            }
        }
        return this;
    }

    /**
     * 属性相等校验（校验源数据和数据库查出数据的同一个bo的指定属性是否相等）
     *
     * @param errorInfo  错误信息，或异常编码(第一个占位符是唯一属性对应值，第二个占位符是checkField对应值)
     * @param checkField 校验字段
     * @return this
     */
    public CheckResult checkEqual(String errorInfo, String checkField) throws Exception {
        if (canStop()) {
            return this;
        }
        ResCommonUtil.match(source, existData, resNoField, (a, b) -> {
            String fieldA = String.valueOf(a.get(checkField));
            String fieldB = String.valueOf(b.get(checkField));
            if (!fieldA.equals(fieldB)) {
                if (isThrowException) {
                    this.throwException(errorInfo, a.getAsString(resNoField), fieldA);
                } else {
                    this.addError(a.getAsString(resNoField), errorInfo);
                }
            }
        });
        return this;
    }


    /**
     * 属性不相等校验
     * 将属性值等于equalValue的数据移动到errorList
     *
     * @param errorInfo     属性相等的错误提示语
     * @param checkField    校验字段
     * @param f1            针对校验的值的处理函数，如只校验号码的前4位（v->v.substring(0,4)）
     * @param f2            针对对比的值的处理函数，如号码是否等于某值的前4位时（v->v.substring(0,4)）
     * @param notEqualValue 比对的值，传多个的时候全部不相等才算校验通过
     * @return this
     */
    @SuppressWarnings("Duplicates")
    public CheckResult checkNotEqual(String errorInfo, String checkField, Function<String, String> f1, Function<String, String> f2, Object... notEqualValue) throws BusiException {
        if (canStop()) {
            return this;
        }
        f1 = Optional.ofNullable(f1).orElse(Function.identity());
        f2 = Optional.ofNullable(f2).orElse(Function.identity());
        List<T> temp = new ArrayList<>(successList);
        for (T bo : temp) {
            boolean allNotEqual = true;
            for (Object o : notEqualValue) {
                Object fieldValue = bo.get(checkField);
                if (f2.apply(String.valueOf(o)).equals(f1.apply(String.valueOf(fieldValue)))) {
                    allNotEqual = false;
                    break;
                }
            }
            if (!allNotEqual) {
                if (isThrowException) {
                    this.throwException(errorInfo, bo.getAsString(resNoField), StringUtils.join(notEqualValue, ","));
                } else {
                    this.addError(bo.getAsString(resNoField), errorInfo);
                    successList.remove(bo);
                }
            }
        }
        return this;
    }

    /**
     * 属性不相等校验
     * 将属性值等于equalValue的数据移动到errorList
     *
     * @param errorInfo     属性相等的错误提示语
     * @param checkField    校验字段
     * @param notEqualValue 比对的值，传多个的时候全部不相等才算校验通过
     * @return this
     */
    public CheckResult checkNotEqual(String errorInfo, String checkField, Object... notEqualValue) throws BusiException {
        return checkNotEqual(errorInfo, checkField, null, null, notEqualValue);
    }

    private void throwException(String errorCode, String... params) throws BusiException {
        throw new BusiException(errorCode, CrmLocaleFactory.getFormatResource(errorCode, (Object[]) params));
    }

}
