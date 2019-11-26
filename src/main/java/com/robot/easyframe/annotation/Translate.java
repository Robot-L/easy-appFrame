package com.robot.easyframe.annotation;

import java.lang.annotation.*;

/**
 * 【字典翻译注解】标识在需要翻译的字段上
 *
 * @author luozhan
 * @date 2019-03
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Translate {
    /**
     * 翻译类型（静态字典翻译、缓存翻译、DAO翻译）
     */
    DicType type();

    /**
     * 翻译的数据源<br/>
     * 1.缓存翻译传入缓存类的class<br/>
     * 2.DAO翻译传入DAO的class<br/>
     * 3.静态翻译不需要传<br/>
     */
    Class<?> src() default Object.class;

    /**
     * 静态字典表中的key（即静态字典表的CODE_TYPE）
     * 静态翻译时必传
     * 支持多个key，传数组
     */
    String[] staticKey() default "";

    /**
     * 指定字典key，不指定默认取Dictionary的配置
     */
    String dicKey() default "";

    /**
     * 指定字典value，不指定默认取Dictionary的配置
     */
    String dicValue() default "";
}
