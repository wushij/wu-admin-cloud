package com.wu.cloud.common.security.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限校验注解：标注在 Controller 方法或类上，校验当前登录用户是否具备指定权限标识
 *
 * <p>示例：{@code @RequiresPermissions("system:user:create")}</p>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequiresPermissions {

    /**
     * 权限标识集合
     */
    String[] value();

    /**
     * 多个权限之间的匹配逻辑，默认满足其一即可
     */
    Logical logical() default Logical.OR;
}
