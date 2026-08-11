package com.wu.cloud.common.security.annotation;

/**
 * 权限/角色校验的组合逻辑
 */
public enum Logical {

    /** 满足其中之一即可 */
    OR,

    /** 必须全部满足 */
    AND
}
