package com.robot.easyframe.annotation;

/**
 * 字典类型枚举
 *
 * @author luozhan
 * @date 2019-03
 */
public enum DicType {
    /**
     * 通过静态表翻译
     */
    STATIC,
    /**
     * 通过缓存翻译
     */
    CACHE,
    /**
     * 通过DAO实时查询翻译
     */
    DAO,
    /* 自定义翻译 */
    /**
     * 员工翻译
     */
    OP,
    /**
     * 组织翻译
     */
    ORG


}
