package com.robot.easyframe.annotation;

import java.lang.annotation.*;

/**
 * 【字典翻译注解】标识在字典数据源上
 *
 * @author luozhan
 * @date 2019-03
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Dictionary {

    /**
     * 代表字典编码的属性
     */
    String key();

    /**
     * 代表字典值的属性
     */
    String value();
}
