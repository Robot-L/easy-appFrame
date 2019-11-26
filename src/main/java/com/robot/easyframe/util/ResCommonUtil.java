package com.robot.easyframe.util;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.bo.DataContainerFactory;
import com.ai.appframe2.common.AIException;
import com.ai.appframe2.common.DataContainerInterface;
import com.ai.appframe2.common.DataStructInterface;
import com.ai.appframe2.common.SessionManager;
import com.robot.easyframe.core.Query;
import com.robot.easyframe.def.Constants;
import com.robot.easyframe.model.Pagination;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Array;
import java.util.*;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

/**
 * 公用工具类
 *
 * @author luozhan
 */
public class ResCommonUtil {

    private static transient final Log log = LogFactory.getLog(ResCommonUtil.class);


    /**
     * 两个DataContainer数组的差集
     *
     * 示例：（省略DC中其他属性，只以要比较的属性示例）
     * [1,2,3] - [1,2] = [3]
     * 注意，如果第二个数组中含有第一个数组中没有的元素，将不会被保留：
     * [1,2,3] - [1,2,5,6] = [3]
     *
     * @param big       原数组
     * @param little    被减的数组
     * @param uniqueCol 唯一标志的属性（唯一约束）
     * @return 相减后的数组
     */
    public static <T extends DataContainer> List<T> minus(T[] big, T[] little, String uniqueCol) {
        if (little.length == 0) {
            return new ArrayList<>(Arrays.asList(big));
        }
        ArrayList<T> result = new ArrayList<>();
        // 分别排序
        sort(big, uniqueCol);
        sort(little, uniqueCol);

        int i = 0, j = 0;
        while (i < big.length) {
            if (j == little.length) {
                result.add(big[i++]);
                continue;
            }
            String valueInBig = big[i].getAsString(uniqueCol);
            String valueInLittle = little[j].getAsString(uniqueCol);
            if (valueInBig.equals(valueInLittle)) {
                i++;
                j++;
            } else if (valueInBig.compareTo(valueInLittle) > 0) {
                j++;
            } else if (valueInBig.compareTo(valueInLittle) < 0) {
                result.add(big[i++]);
            }
        }
        return result;
    }

    /**
     * 两个(String/Long/Integer)数组的差集
     *
     * 示例：
     * [1,2,3] - [1,2] = [3]
     * 注意，如果第二个数组中含有第一个数组中没有的元素，将不会被保留：
     * [1,2,3] - [1,2,5,6] = [3]
     *
     * @param big       原数组
     * @param little    被减的数组
     * @return 相减后的数组
     */
    public static <T extends Comparable<T>> List<T> minus(T[] big, T[] little) {
        if (little.length == 0) {
            return new ArrayList<>(Arrays.asList(big));
        }
        Arrays.sort(big);
        Arrays.sort(little);
        ArrayList<T> result = new ArrayList<>();
        int i = 0, j = 0;
        while (i < big.length) {
            if (j == little.length) {
                result.add(big[i++]);
                continue;
            }
            T valueInBig = big[i];
            T valueInLittle = little[j];
            if (valueInBig.equals(valueInLittle)) {
                i++;
                j++;
            } else if (valueInBig.compareTo(valueInLittle) > 0) {
                j++;
            } else if (valueInBig.compareTo(valueInLittle) < 0) {
                result.add(big[i++]);
            }
        }
        return result;
    }

    /**
     * 根据指定属性排序（升序）
     *
     * @param arr   待排序数组，数组中如果有空元素会排在末尾
     * @param field 注意数组中指定属性的值如果为空，会被转成"null"进行排序
     */
    public static void sort(DataContainer[] arr, String field) {
        if (arr.length == 0) {
            return;
        }
        Arrays.sort(arr, Comparator.nullsLast(Comparator.comparing(bo -> String.valueOf(bo.get(field)))));
    }
    /**
     * 根据指定属性排序（升序）
     *
     * @param list   待排序List，支持List<Map>和List<DataContainer>
     * @param field  注意数组中指定属性的值如果为空，会被转成"null"进行排序
     */
    @SuppressWarnings("unchecked")
    public static void sort(List<?> list, String field) {
        if (list.size() == 0) {
            return;
        }
        List<DataContainer> listOfDc;
        List<Map> listOfMap;
        if(list.get(0) instanceof DataContainer){
            listOfDc = (List<DataContainer>)list;
            listOfDc.sort(Comparator.nullsLast(Comparator.comparing(bo -> String.valueOf(bo.get(field)))));
        }else if (list.get(0) instanceof Map){
            listOfMap = (List<Map>)list;
            listOfMap.sort(Comparator.nullsLast(Comparator.comparing(bo -> String.valueOf(bo.get(field)))));
        }
    }

    /**
     * 合并多个bo数组，bo类型须一致
     *
     * @param boArray
     * @param anotherBoArrays
     * @param <T>
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainer> T[] concat(T[] boArray, T[]... anotherBoArrays) {
        Class<?> clazz = anotherBoArrays.getClass().getComponentType().getComponentType();
        return Stream.concat(Stream.of(boArray), Stream.of(anotherBoArrays)
                .flatMap(Stream::of))
                .toArray(size -> (T[]) Array.newInstance(clazz, size));
    }

    /**
     * 合并两个数组，匹配两个数组中的相同的对象(由入参field判断)，
     * 如果匹配了相同的对象，除开主键，将其他属性由srcArray数组拷贝到destArray数组同一对象中
     *
     * @param srcArray  原数组
     * @param destArray 目标数组
     * @param field     唯一性的属性
     * @param <T>
     * @return 返回destArray
     */
    public static <T extends DataContainer> T[] concat(T[] srcArray, T[] destArray, String field) throws Exception {
        sort(srcArray, field);
        sort(destArray, field);
        String keyName = ((DataContainer) destArray.getClass().getComponentType().newInstance()).getObjectType().getMainAttr();
        int i = 0, j = 0;
        while (i < srcArray.length && j < destArray.length) {
            String srcBo = srcArray[i].getAsString(field);
            String destBo = destArray[j].getAsString(field);
            if (srcBo.equals(destBo)) {
                long keyValue = destArray[i].getAsLong(keyName);
                DataContainerFactory.copyNoClearData(srcArray[i], destArray[i]);
                destArray[i].set(keyName, keyValue);
                i++;
                j++;
            }
            if (srcBo.compareTo(destBo) > 0) {
                j++;
            }
            if (srcBo.compareTo(destBo) < 0) {
                i++;
            }
        }
        return destArray;
    }

    /**
     * 匹配两个BOList的数据，指定匹配后的操作
     *
     * @param sourceList   boListA 支持List<? extend Map>和List<? extend DataContainer>
     * @param destList   boListB 支持List<? extend Map>和List<? extend DataContainer>
     * @param field   确定匹配的属性
     * @param operate 对匹配的两个数据的操作，如拷贝值：（a, b）->a.setSomeValue(b.getSomeValue())
     * @param <T>
     */
    public static <T ,K> void match(List<T> sourceList, List<K> destList, String field, LambdaExceptionUtil.BiConsumerWithExceptions<T, K, Exception> operate) throws Exception {
        if(CollectionUtils.isEmpty(sourceList) || CollectionUtils.isEmpty(destList)){
            return;
        }
        // 判断List中的数据类型，只能为Map或DataContainer
        boolean isMapOfSource, isMapOfDest;
        if(sourceList.get(0) instanceof DataContainer){
            isMapOfSource = false;
        }else if(sourceList.get(0) instanceof Map){
            isMapOfSource = true;
        }else{
            throw new RuntimeException("ResCommonUtil.match()方法入参listA的数据类型只能继承自DataContainer或Map");
        }
        if(destList.get(0) instanceof DataContainer){
            isMapOfDest = false;
        }else if(destList.get(0) instanceof Map){
            isMapOfDest = true;
        }else{
            throw new RuntimeException("ResCommonUtil.match()方法入参listA的数据类型只能继承自DataContainer或Map");
        }
        sort(sourceList, field);
        sort(destList, field);
        int i = 0, j = 0;
        while (i < sourceList.size() && j < destList.size()) {
            String valueInSource = String.valueOf(isMapOfSource? ((Map)sourceList.get(i)).get(field) : ((DataContainer)sourceList.get(i)).get(field));
            String valueInDest = String.valueOf(isMapOfDest? ((Map)destList.get(j)).get(field) : ((DataContainer)destList.get(j)).get(field));
            if (valueInSource.equals(valueInDest)) {
                operate.accept(sourceList.get(i), destList.get(j));
                i++;j++;
            } else if (valueInSource.compareTo(valueInDest) > 0) {
                j++;
            } else if (valueInSource.compareTo(valueInDest) < 0) {
                i++;
            }
        }
    }
    public static <T,K extends DataContainer> void match(T[] sourceList, K[] destList, String field, LambdaExceptionUtil.BiConsumerWithExceptions<T, K, Exception> operate) throws Exception {
        match(Arrays.asList(sourceList), Arrays.asList(destList), field, operate);
    }

    /**
     * 批量设值某些属性（从map中拷过来）
     * 注意：需要确认bo中含有待设值的属性，否则会报错
     *
     * @param datas  待设值的bo列表
     * @param params {属性：值}
     */
    public static void batchSetField(DataContainerInterface[] datas, Map<String, Object> params) {
        for (DataContainerInterface data : datas) {
            params.forEach(data::set);
        }
    }

    /**
     * 批量设值某些属性（从bo中拷过来）
     *
     * @param datas  待设值的bo列表
     * @param source 含有待拷贝属性的bo
     */
    public static void batchSetField(DataContainerInterface[] datas, DataContainerInterface... source) throws Exception {
        for (DataContainerInterface data : datas) {
            DataContainerInterface temp = new DataContainer();
            for (DataContainerInterface bo : source) {
                DataContainerFactory.copyNoClearData(bo, temp);
            }
            DataContainerFactory.copyNoClearData(temp, data);
        }
    }

    /**
     * 批量设值某些属性（从bo中拷过来）
     * 位置靠后的数据会覆盖靠前的数据
     *
     * @param datas  待设值的bo列表
     * @param source 含有待拷贝属性的bo
     */
    public static void batchSetField(List<? extends DataContainerInterface> datas, DataContainerInterface... source) throws Exception {
        for (DataContainerInterface data : datas) {
            DataContainerInterface temp = new DataContainer();
            for (DataContainerInterface bo : source) {
                DataContainerFactory.copyNoClearData(bo, temp);
            }
            DataContainerFactory.copyNoClearData(temp, data);
        }
    }

    /**
     * 拷贝源bo的属性到目标bo
     * 如果目标为DataContainer则全部属性拷贝过去
     * 如果目标为具体的bo，则只会拷贝bo含有的属性
     *
     * @param dest   目标bo
     * @param source 源bo，可传多个，按顺序拷贝到目标bo中
     */
    public static void copy(DataStructInterface dest, DataStructInterface... source) {
        try {
            for (DataStructInterface sourceBo : source) {
                DataContainerFactory.copyNoClearData(sourceBo, dest);
            }
        } catch (AIException e) {
            throw new RuntimeException("bo属性拷贝异常！",e);
        }
    }

    /**
     * 将DataContainer数组的某个属性的值取出来组成数组返回
     *
     * @param datas     DataContainer数组
     * @param fieldName 属性名称
     * @return
     */
    public static String[] getFieldValues(DataContainerInterface[] datas, String fieldName) {
        return Arrays.stream(datas).map(s -> s.getAsString(fieldName)).toArray(String[]::new);
    }

    public static <T extends DataContainer> String[] getFieldValues(List<T> bos, String fieldName) {
        return bos.stream().map(s -> s.getAsString(fieldName)).toArray(String[]::new);
    }


    /**
     * 通过字符串生成bo数组(支持Query.in()或Query.between()生成的字符串)
     *
     * @param sourceString 1.数据字符串 以","分隔；2.Query.in()生成的字符串；3.Query.between()生成的字符串
     * @param fieldName    属性名
     * @param clazz        class对象
     * @param <T>          bo类型
     * @return
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainerInterface> T[] getBoArrayFromString(String sourceString, String fieldName, Class<T> clazz) throws IllegalAccessException, InstantiationException {
        if (sourceString.startsWith(Query.IN)) {
            // 如果是Query.in()字符串，去除前缀"in:"
            sourceString = sourceString.replace(Query.IN, "");
        } else if (sourceString.startsWith(Query.BETWEEN)) {
            // 如果是Query.between()字符串，根据起始、结束数字获得完整数字列表
            String start = sourceString.substring(sourceString.lastIndexOf(":") + 1, sourceString.indexOf(","));
            String end = sourceString.substring(sourceString.indexOf(",") + 1);
            sourceString = getStringsFromStartAndEnd(start, end);
        }
        String[] valueArray = sourceString.split(",");
        T[] boList = (T[]) Array.newInstance(clazz, valueArray.length);
        for (int i = 0; i < valueArray.length; i++) {
            boList[i] = clazz.newInstance();
            boList[i].set(fieldName, valueArray[i]);
        }
        return boList;
    }

    /**
     * 根据起始、结束字符串拼装完整字符串，以“,”分隔
     * 注意，只能用于数字型字符串，且起始字符串和结束字符串的长度须相等
     * 例：
     * （"0000","0005"）->"0000,0001,0002,0003,0004,0005"
     *
     * @param start 字符串起始，如"0000"
     * @param end   字符串结束，如"0010"
     * @return
     */
    public static String getStringsFromStartAndEnd(String start, String end) {
        int lengthOfNumber = start.length();
        long startNum = Long.valueOf(start);
        long endNum = Long.valueOf(end);
        StringBuilder strings = new StringBuilder();
        for (long i = startNum; i <= endNum; i++) {
            String number = StringUtils.leftPad(String.valueOf(i), lengthOfNumber, "0");
            strings.append(number).append(",");
        }
        return strings.deleteCharAt(strings.length() - 1).toString();
    }



    /**
     * Map转DataContainer，
     * 并且指定一个bo的class作为过滤，Map中不是bo的属性的参数将不会设值到DC中
     *
     * 注：
     * 1.和Convert.toDc()方法的区别是，此方法可以指定某个BO作为属性筛选
     * 2.不需要过滤时请使用PartTool.toDc(Map)
     * 3.此方法不会对入参的Map的元素修改或删除（可能会新增）
     *
     * @param input   入参map
     * @param clazz 作为过滤模板的bo类
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static DataContainer toDcByFilter(Map input, Class<? extends DataContainer> clazz) throws Exception {
        Map<String, Object> newMap = new HashMap<>(input);
        newMap.forEach((key, value) -> {
            // M代表主表，使用"M."兼容连表查询，此处判断时需要先进行转换
            input.put(key.startsWith("M.") ? key.replace("M.", "") : key, input.get(key));
        });
        // 过滤掉BO中没有的属性
        List<String> keys = Arrays.asList(clazz.newInstance().getPropertyNames());
        newMap.entrySet().removeIf(item -> !keys.contains(item.getKey()));

        return Convert.toDc(newMap);
    }

    /**
     * 创建List，批量生成新数据并以源数据设值
     * 常见拷表/搬表业务，两个表的字段相同时
     *
     * @param source 源数据List
     * @param clazz  生成List的类型
     * @param <T>
     * @return
     * @throws Exception
     * @author luozhan
     */
    public static <T extends DataContainer> List<T> copyBoList(List<? extends DataContainer> source, Class<T> clazz) throws Exception {
        List<T> result = new ArrayList<>(source.size());
        for (DataContainer data : source) {
            T obj = clazz.newInstance();
            DataContainerFactory.copyNoClearData(data, obj);
            result.add(obj);
        }
        return result;
    }

    /**
     * 创建Array，批量生成新数据并以源数据设值
     * 常见拷表/搬表业务，两个表的字段相同时
     *
     * @param source 源数组
     * @param clazz  生成List的类型
     * @param <T>
     * @return
     * @throws Exception
     * @author luozhan
     */
    @SuppressWarnings("unchecked")
    public static <T extends DataContainer, K extends DataContainer> T[] copyBoArray(K[] source, Class<T> clazz) throws Exception {
        List<T> result = new ArrayList<>(source.length);
        for (DataContainer data : source) {
            T obj = clazz.newInstance();
            DataContainerFactory.copyNoClearData(data, obj);
            result.add(obj);
        }
        return result.toArray((T[]) Array.newInstance(clazz, source.length));
    }

    /**
     * 批量清除主键属性的值
     * 当集合中的对象已经含有主键信息，但主键信息需要清除时使用
     *
     * @param <T>
     * @param data bo集合（必须是具体bo的集合，不能是DataContainer集合）
     * @return
     */
    public static <T extends DataContainer> List<T> batchClearId(List<T> data) {
        if (data == null || data.size() == 0) {
            return data;
        }
        String[] keys = data.get(0).getKeyPropertyNames();
        if (keys.length != 1) {
            throw new RuntimeException(data.get(0).getClass().getSimpleName()+(keys.length==0?"未配置主键，不能清除":"的主键为联合主键，不能批量清除"));
        }
        String key = keys[0];
        for (T bo : data) {
            bo.set(key, "");
        }
        return data;
    }

    /**
     * {@link #batchClearId(List)}
     */
    public static <T extends DataContainer> List<T> batchClearId(T[] data) {
        return batchClearId(Arrays.asList(data));
    }

    /**
     * 获取非自定义sql的方式且带分页查询时的总条数
     * 注意：
     * 获取getBy()方式查询的数据总条数勿使用该方法，请用dao.count()！
     * 此方法用来获取含有分页条件的自定义sql查询（如连表查询）的总条数
     * 如果获取不到将取入参数据集的大小
     *
     * @param unionQueryResult 自定义sql查询的结果集
     * @return 总数目
     */
    public static int getTotal(DataContainer[] unionQueryResult) throws Exception{
        if (unionQueryResult.length == 0) {
            return 0;
        }
        Integer total = (Integer)SessionManager.getUser().get(Constants.ReturnCode.TOTAL);

        return total == null? unionQueryResult.length:  total;
    }


    /**
     * List去重
     * 示例：
     * list = [{id:1,age:3}{id:2}{id:1,age:3}];
     * distinct(list, "id"); // [{id:1,age:3}{id:2}]
     *
     * @param list 源数据
     * @param key  判断两个map重复的属性
     * @return 去重后的list
     * @author luozhan
     */
    public static List<Map> distinct(List<Map> list, String key) {
        return list.stream().collect(
                collectingAndThen(
                        toCollection(() -> new TreeSet<>(comparing(o -> (String) o.get(key)))), ArrayList::new)
        );
    }


    /**
     * 获取超大数据集（超过30万）
     * 用于突破框架底层查询数据库当返回数据超过30w时报错的限制
     *
     * @param function
     * @return
     * @throws Exception
     * @author luozhan
     */
    public static <T> List<T> getOversizeData(LambdaExceptionUtil.FunctionWithExceptions<Pagination, T[], Exception> function) throws Exception {
        int size = 100000;
        List<T> result = new ArrayList<>(size);
        int pageNum = 1;
        while (true) {
            T[] data = function.apply(new Pagination(size, pageNum++));
            Collections.addAll(result, data);
            if (data.length < size) {
                break;
            }
        }
        return result;
    }




    /**
     * 按照List<Map<String,Object>>里面map的某个value重新封装成多个不同的list, 原始数据类型List<Map
     * <String,Object>>, 转换后数据类型Map<String,List<Map<String,Object>>>
     *
     * @param list
     * @param oneMapKey
     * @return     
     */
    public static Map<String, Object> subGroupList(List<Map<String, Object>> list, String oneMapKey) {
        Map<String, Object> resultMap = new HashMap<String, Object>();
        Set<Object> setTmp = new HashSet<Object>();
        for (Map<String, Object> tmp : list) {
            setTmp.add(tmp.get(oneMapKey));
        }
        Iterator<Object> it = setTmp.iterator();
        while (it.hasNext()) {
            String oneSetTmpStr = (String) it.next();
            List<Map<String, Object>> oneSetTmpList = new ArrayList<Map<String, Object>>();
            for (Map<String, Object> tmp : list) {
                String oneMapValueStr = (String) tmp.get(oneMapKey);
                if (oneMapValueStr.equals(oneSetTmpStr)) {
                    oneSetTmpList.add(tmp);
                }
            }
            resultMap.put(oneSetTmpStr, oneSetTmpList);
        }
        return resultMap;
    }

}
