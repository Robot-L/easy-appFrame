package com.robot.easyframe.util;

import com.ai.appframe2.bo.DataContainer;
import com.ai.appframe2.common.DataStructInterface;
import com.ai.appframe2.complex.cache.CacheFactory;
import com.ai.common.bo.BOBsStaticDataBean;
import com.ai.common.util.StaticDataUtil;
import com.ai.secframe.orgmodel.bo.BOSecOrganizeBean;
import com.ai.secframe.orgmodel.bo.QBOSecOrgStaffOperBean;
import com.robot.easyframe.annotation.Translate;
import com.robot.easyframe.annotation.Dictionary;
import com.robot.easyframe.core.dao.BaseDao;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.robot.easyframe.util.LambdaExceptionUtil.wrapFunction;


/**
 * 翻译工具类
 * 配合注解{@link Translate}和{@link Dictionary}使用
 *
 * @author luozhan
 * @date 2019-03
 */
public class TranslateUtil {
    private static final String TRANSLATE_MAP = "TRANSLATE_MAP";
    private static Log log = LogFactory.getLog(TranslateUtil.class);

    /**
     * 翻译并转换成List（只用于单表查询）
     *
     * @param origins 待翻译数据
     * @param <T>     具体的bo类型
     * @return List
     */
    @SafeVarargs
    public static <T extends DataContainer> List<Map> parse(T... origins) {
        translate(origins);
        return beans2List(origins);
    }

    /**
     * 翻译并转换成List（适用于连表查询）
     * 因为传入的是DataContainer，所以需要指定待翻译bo的class
     *
     * @param origins DataContainer类型数据
     * @param classes 待翻译的class列表
     * @return List
     */
    @SafeVarargs
    public static List<Map> parse(DataContainer[] origins, Class<? extends DataContainer>... classes) {
        translate(origins, classes);
        return beans2List(origins);
    }

    /**
     * 执行翻译
     *
     *
     * @param origins 原始bo数组
     * @see this.getTranslateName()
     */
    @SafeVarargs
    private static void translate(DataContainer[] origins, Class<? extends DataContainer>... clazz) {
        if (origins.length == 0) {
            return;
        }
        // 待翻译属性名列表
        List<String> fieldNameList = new ArrayList<>();
        // 字典数据源列表
        List<DataStructInterface[]> dictList = new ArrayList<>();
        // 字典code列表
        List<String> dictCodeList = new ArrayList<>();
        // 字典name列表
        List<String> dictNameList = new ArrayList<>();
        Class<? extends DataContainer>[] classes = clazz.length != 0 ? clazz : new Class[]{origins[0].getClass()};
        preHandle(classes, fieldNameList, dictList, dictCodeList, dictNameList);
        translate(origins, fieldNameList, dictList, dictCodeList, dictNameList);
    }

    private static void preHandle(Class<? extends DataContainer>[] classes, List<String> fieldNameList, List<DataStructInterface[]> dictList,
                                  List<String> dictCodeList, List<String> dictNameList) {
        // 获取bo中需要翻译的属性
        List<Field> translateFieldList = Arrays.stream(classes)
                .map(Class::getDeclaredFields)
                .flatMap(Stream::of)
                .filter(field -> field.isAnnotationPresent(Translate.class))
                .collect(Collectors.toList());

        if (translateFieldList.size() == 0) {
            log.warn(String.format("找不到需要翻译的属性，请检查Bean:%s的配置，如果需要翻译，请在Bean中使用@Translate注解，如果只要转成List不需要翻译，可使用PartTool.toList()", Arrays.toString(classes)));
        }
        for (Field field : translateFieldList) {
            // 1.保存要翻译的属性名
            String fieldName = getRealFieldName(field.getName());
            if (fieldNameList.contains(fieldName)) {
                // 不同的bo可能有相同的属性，跳过
                continue;
            }
            fieldNameList.add(fieldName);

            // 2.保存每个属性翻译的字典、字典code、字典name
            // 获取属性上的翻译配置信息
            Translate translateConfig = field.getAnnotation(Translate.class);
            // 配置的字典数据源class
            Class<?> srcClass = translateConfig.src();
            // 获取字典数据源配置信息
            Dictionary dictionaryConfig = srcClass.getAnnotation(Dictionary.class);
            // 翻译字典源
            DataStructInterface[] dict = null;
            // 字典值属性，字典名称属性
            String code = null, name = null;
            // 根据翻译类型准备数据
            try {
                switch (translateConfig.type()) {
                    case STATIC:
                        // 静态字典的key
                        String[] staticKeys = translateConfig.staticKey();
                        // 合并多个字典
                        dict = Stream.of(staticKeys).flatMap(wrapFunction(v -> Stream.of(StaticDataUtil.getStaticData(v)))).toArray(DataStructInterface[]::new);
                        code = BOBsStaticDataBean.S_CodeValue;
                        name = BOBsStaticDataBean.S_CodeName;
                        break;
                    case CACHE:
                        dict = (DataStructInterface[]) CacheFactory.get(srcClass, srcClass);
                        code = StringUtils.isEmpty(translateConfig.dicKey()) ? dictionaryConfig.key() : translateConfig.dicKey();
                        name = StringUtils.isEmpty(translateConfig.dicValue()) ? dictionaryConfig.value() : translateConfig.dicValue();
                        break;
                    case DAO:
                        dict = ((BaseDao) ServiceUtil.get(srcClass)).getAll();
                        code = StringUtils.isEmpty(translateConfig.dicKey()) ? dictionaryConfig.key() : translateConfig.dicKey();
                        name = StringUtils.isEmpty(translateConfig.dicValue()) ? dictionaryConfig.value() : translateConfig.dicValue();
                        break;
                    case OP:
                        //todo: 根据项目改成实际的缓存类
                        // dict = (QBOSecOrgStaffOperBean[]) CacheFactory.get(opCacheClass, opCacheClass);
                        code = QBOSecOrgStaffOperBean.S_StaffId;
                        name = QBOSecOrgStaffOperBean.S_StaffName;
                        break;
                    case ORG:
                        //todo: 根据项目改成实际的缓存类
                        // dict = (BOSecOrganizeBean[]) CacheFactory.get(orgCacheClass, orgCacheClass);
                        code = BOSecOrganizeBean.S_OrganizeId;
                        name = BOSecOrganizeBean.S_OrganizeName;
                        break;
                    default:
                }
            } catch (Exception e) {
                log.error("字典数据源获取失败：" + e);
                dict = new DataContainer[0];
            }
            dictList.add(dict);
            dictCodeList.add(code);
            dictNameList.add(name);
        }
    }

    /**
     * 获取bo真实的属性名
     * S_ResType  ->  RES_TYPE
     *
     * @param fieldName 属性名
     * @return 真实属性名
     */
    private static String getRealFieldName(String fieldName) {
        fieldName = fieldName.substring(2);
        StringBuilder sb = new StringBuilder(fieldName);
        int position = 0;
        for (int i = 0; i < fieldName.length(); i++) {
            // 大写字母在前面加入下划线，排除第一位
            if (i != 0 && Character.isUpperCase(fieldName.charAt(i))) {
                sb.insert(i + position, "_");
                position++;
            }
        }
        return sb.toString().toUpperCase();
    }

    /**
     * 对字典进行匹配，匹配不到则显示翻译前的值
     *
     * @param origins       原始对象数组
     * @param fieldNameList 翻译属性列表
     * @param dictList      字典列表
     * @param dictCodeList  字典编码列表
     * @param dictNameList  字典名称列表
     */
    public static <T extends DataContainer> void translate(T[] origins, List<String> fieldNameList, List<DataStructInterface[]> dictList,
                                                           List<String> dictCodeList, List<String> dictNameList) {
        for (T origin : origins) {
            for (int i = 0; i < fieldNameList.size(); i++) {
                String originField = fieldNameList.get(i);
                Object originValue = origin.getProperties().get(originField);
                if (originValue == null) {
                    continue;
                }
                String translateResult;
                if (dictList.get(i) == null) {
                    // 字典数据源获取失败时显示原值
                    translateResult = originValue.toString();
                } else {
                    translateResult = translate(originValue.toString(), dictList.get(i), dictCodeList.get(i), dictNameList.get(i));
                }
                origin.setDiaplayAttr(TRANSLATE_MAP, getTranslateName(originField), translateResult);
            }
        }

    }

    /**
     * 单独属性翻译方法
     * 所有属性翻译完成后需要使用本类的beans2List()方法转换成List，否则翻译后的值会丢失
     *
     * @param origins   源bo数据
     * @param fieldName 翻译的属性名
     * @param dict      字典数据
     * @param dictCode  字典编码属性
     * @param dictName  字典值属性
     */
    public static <T extends DataContainer> void translate(T[] origins, String fieldName, DataStructInterface[] dict,
                                                           String dictCode, String dictName) {
        translate(
                origins,
                Collections.singletonList(fieldName),
                Collections.singletonList(dict),
                Collections.singletonList(dictCode),
                Collections.singletonList(dictName)
        );
    }

    /**
     * 翻译String，字典中没找到的话返回原始值
     *
     * @param originValue 原始值
     * @param dict        字典数据源
     * @param codeField   字典的code
     * @param nameField   字典的name
     * @return 翻译后的值
     */
    private static String translate(String originValue, DataStructInterface[] dict, String codeField, String nameField) {
        if (StringUtils.isEmpty(originValue)) {
            return originValue;
        }
        return Arrays.stream(dict).filter(bean -> String.valueOf(bean.get(codeField)).equals(originValue))
                .map(v -> String.valueOf(Optional.ofNullable(v.get(nameField)).orElse("")))
                .findFirst().orElse(originValue);
    }

    /**
     * 对象转Map(如果有翻译，翻译的属性和值也会放到Map中)
     *
     * @param bo bean
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> bean2Map(DataContainer bo) {
        HashMap<String, Object> map = bo.getProperties();
        Map<String, String> displayParams = bo.getDisplayAttrHashMap(TRANSLATE_MAP);
        if (displayParams != null) {
            map.putAll(displayParams);
        }
        map.replaceAll((k, v) -> Convert.toStr(v));
        return map;

    }

    /**
     * 对象数组转List
     *
     * @param bos 对象数组
     * @return List
     */
    public static List<Map> beans2List(DataContainer... bos) {
        List<Map> result = new ArrayList<>(bos.length);
        for (DataContainer bo : bos) {
            result.add(bean2Map(bo));
        }
        return result;
    }

    /**
     * 生成翻译后的属性名
     * 规则：属性名以CODE或ID结尾的替换成NAME，否则在结尾加"_NAME"
     * 如：RES_STATE -> RES_STATE_NAME
     * RES_TYPE_ID -> RES_TYPE_NAME
     * POOL_CODE -> POOL_NAME
     *
     * @param srcName 翻译前的属性名
     * @return
     */
    @SuppressWarnings("all")
    private static String getTranslateName(String srcName) {
        if (srcName.endsWith("CODE") || srcName.endsWith("ID")) {
            return srcName.replaceAll("(CODE|ID)$", "NAME");
        } else {
            return srcName + "_NAME";
        }
    }
}
