package com.robot.easyframe.util;


import com.ai.appframe2.bo.DataContainer;
import com.robot.easyframe.model.OperateDTO;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.robot.easyframe.util.LambdaExceptionUtil.uncheck;

/**
 * 各种花里胡哨的转换
 *
 * @author luozhan
 * @date 2019-10
 */
public class Convert {

    //---------------------------------- 时间相关转换 ---------------------------------------//

    /**
     * 任意类型 → String
     * 注：传入null将返回null
     *
     * @param anyType 任意类型数据，如日期、数字、集合、数组、异常等
     * @return str
     */
    public static String toStr(Object anyType) {
        if (null == anyType) {
            return null;
        }
        if (anyType instanceof Date) {
            return toStr((Date) anyType, "yyyy-MM-dd HH:mm:ss");
        }
        if (anyType.getClass().isArray()) {
            return Arrays.deepToString((Object[]) anyType);
        }
        if (anyType instanceof Throwable){
            StringWriter sw = new StringWriter();
            try(PrintWriter pw = new PrintWriter(sw)){
                ((Throwable)anyType).printStackTrace(pw);
            }
            return sw.toString();
        }
        //Number、Collection、Map、8大基本类型及其包装类等
        return String.valueOf(anyType);
    }

    /**
     * Date → 指定格式的字符串
     *
     * @param date   日期（此处也可以传Timestamp）
     * @param format 格式，如"yyyy-MM-dd HH:mm:ss"，注意大小写
     * @return str
     */
    public static String toStr(Date date, String format) {
        return new SimpleDateFormat(format).format(date);
    }

    /**
     * 字符串 → TimeStamp，按照默认格式："yyyy-MM-dd HH:mm:ss"
     *
     * @param str 日期字符串
     * @return TimeStamp
     */
    public static Timestamp toTimeStamp(String str) {
        return toTimeStamp(str, "yyyy-MM-dd HH:mm:ss");
    }

    /**
     * 字符串 → TimeStamp，按照指定格式
     *
     * @param str    日期字符串
     * @param format 日期格式
     * @return TimeStamp
     */
    public static Timestamp toTimeStamp(String str, String format) {
        try {
            return new Timestamp(new SimpleDateFormat(format).parse(str).getTime());
        } catch (ParseException e) {
            throw new RuntimeException("转换TimeStamp失败，源字符串：" + str + "，解析格式：" + format, e);
        }
    }

    //---------------------------------- BO/DataContainer相关转换 ---------------------------------------//

    /**
     * DataContainer → Map
     * 注：会将DataContainer中m_back和m_front依次写到Map中，并将value转成字符串
     *
     * @param dc DataContainer
     * @return Map
     */
    public static Map<String, String> toMap(DataContainer dc) {
        HashMap<String, Object> allProperties = dc.getProperties();
        Map<String, String> map = new HashMap<>(allProperties.size());
        allProperties.forEach((key, value) -> map.put(key, Convert.toStr(value)));
        return map;
    }

    /**
     * Map → 指定BO
     * <p>
     * 注：
     * 如果type是BO，Map中的数据会被自动转换成BO中的对应属性的类型，警惕类型转换异常，BO不存在的属性将被忽略
     * 如果type是DataContainer，将包含所有属性，建议直接使用toDc()方法
     *
     * @param map  源数据Map，要求Map中的key是String类型
     * @param type 指定BO类型
     * @return T
     */
    public static <T extends DataContainer> T toBo(Map<String, ?> map, Class<T> type) {
        T bo = uncheck(type::newInstance);
        map.forEach((key, value) -> {
            if (bo.hasPropertyName(key)) {
                bo.set(key, value);
            }
        });
        return bo;
    }

    /**
     * Map → DataContainer
     *
     * @param map 源数据
     * @return DataContainer
     */
    public static DataContainer toDc(Map<String, ?> map) {
        return toBo(map, DataContainer.class);
    }

    /**
     * DataContainer[] → List(Map)
     * 注：会将每个DataContainer中m_back和m_front依次写到Map中，并将value转成字符串
     *
     * @param dcArr DataContainer或其子类数组
     * @return List(Map)
     */
    public static List<Map<String, String>> toList(DataContainer[] dcArr) {
        return Stream.of(dcArr).map(Convert::toMap).collect(Collectors.toList());
    }

    /**
     * List[Map] → DataContainer[]
     *
     * @param mapList 源数据List，要求List中每个Map的key是String类型
     * @return DataContainer[]
     */
    public static DataContainer[] toDcArr(List<Map<String, ?>> mapList) {
        return mapList.stream().map(Convert::toDc).toArray(DataContainer[]::new);
    }

    /**
     * List[Map] → 指定类型的BO数组
     *
     * 注：每个Map中的数据会被自动转换成BO中的对应属性的类型，警惕类型转换异常，BO中不存在的属性将被忽略
     *
     * @param mapList 源数据List，要求List中每个Map的key是String类型
     * @param type    指定BO类型
     * @return 指定类型的BO数组
     */
    public static <T extends DataContainer> T[] toBoArr(List<Map<String, ?>> mapList, Class<T> type) {
        return mapList.stream().map(map -> toBo(map, type)).toArray(i -> (T[]) Array.newInstance(type, i));
    }

    public static void main(String[] args) {
        // toMap
        OperateDTO dc = new OperateDTO();
        OperateDTO[] dcs = {dc};
        System.out.println(dcs instanceof DataContainer[]);
        List<Map<String, String>> maps = toList(dcs);
        System.out.println(dc);


        dc.set("1", 1);
        dc.set("2", new Timestamp(System.currentTimeMillis()));
        dc.set("3", "string");
        dc.set("4", null);
        Map<String, String> stringStringMap = toMap(dc);
        System.out.println(stringStringMap);

        //    toBo
        Map<String, String> m = new HashMap<>();
        m.put(OperateDTO.S_DoneCode, "23");
        m.put(OperateDTO.S_OpId, "23");
        System.out.println(toBo(m, OperateDTO.class));
    }
}
