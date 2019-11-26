package com.robot.easyframe.annotation;

import java.lang.annotation.*;

/**
 * 接口入参校验配置
 *
 * 支持功能：
 * 1.标识入参Map中含有哪些属性、添加描述
 * 2.标识属性间的父子级关系
 * 3.校验属性必填、非空等
 * 4.支持校验多个属性不能同时为空的场景
 *
 * @author luozhan
 * @date 2019-05
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Checks.class)
public @interface Check {
    /**
     * Map中的key名
     */
    String key();

    /**
     * 值类型，如String.class
     */
    Class type();

    /**
     * 泛型
     * 只有type=List且泛型不为集合类的时候需要配置泛型，如List<String>/List<Integer>等
     */
    Class genericType() default Object.class;

    /**
     * key必须存在且值不能为null
     */
    boolean notNull() default false;

    /**
     * key必须存在且值不能为null、空字符串或空集合
     */
    boolean notEmpty() default false;

    /**
     * 指定多个key，这些key的值不能同时为空、空字符串或空集合
     */
    String[] notAllEmpty() default {};

    /**
     * 指定父级key，此时父级一般是List或Map类型
     */
    String parent() default "";

    /**
     * 描述
     */
    String desc() default "";
}
