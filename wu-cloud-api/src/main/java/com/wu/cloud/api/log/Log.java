package com.wu.cloud.api.log;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 操作日志注解
 * <p>
 * 标注在 Controller 方法上，切面会拦截并记录操作日志（模块/业务类型/入参/耗时/操作人），
 * 通过 RemoteLogService 异步写入 system 服务的 {@code sys_oper_log}（与操作日志列表同源）。
 * </p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {

    /** 操作模块，例如 "用户管理" */
    String value() default "";

    /** 操作模块（同 value，语义化别名） */
    String title() default "";

    /** 业务类型：1新增 2修改 3删除 4查询 5登录 6登出 0其它 */
    BusinessType businessType() default BusinessType.OTHER;

    /** 操作人类别：0其它 1后台用户 2手机端用户 */
    OperatorType operatorType() default OperatorType.MANAGE;

    /** 是否保存请求参数 */
    boolean isSaveRequestData() default true;

    /** 是否保存响应结果 */
    boolean isSaveResponseData() default true;

    enum BusinessType {
        OTHER(0), INSERT(1), UPDATE(2), DELETE(3), QUERY(4), LOGIN(5), LOGOUT(6);
        private final int type;
        BusinessType(int type) { this.type = type; }
        public int getType() { return type; }
    }

    enum OperatorType {
        OTHER(0), MANAGE(1), MOBILE(2);
        private final int type;
        OperatorType(int type) { this.type = type; }
        public int getType() { return type; }
    }
}
