package com.wu.cloud.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 角色校验注解：标注在 Controller 方法或类上，校验当前登录用户是否具备指定角色
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresRoles {

    /**
     * 角色标识集合
     */
    String[] value();

    /**
     * 多个角色之间的匹配逻辑，默认满足其一即可
     */
    Logical logical() default Logical.OR;
}
