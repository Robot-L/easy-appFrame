package com.robot.easyframe.annotation;

import com.robot.easyframe.core.cache.BaseCache;

import java.lang.annotation.*;

/**
 * 缓存标识注解
 * 标记在bo类名上
 *
 * @author luozhan
 * @date 2019-03
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {
    /**
     * bo对应的缓存类
     */
    Class<? extends BaseCache> value();

}
