/**
 * 零第三方框架依赖的核心公共层
 * <p>
 * 子包职责：
 * <ul>
 *   <li>{@code domain}   —— 核心类型：统一响应 {@code R}、分页 {@code PageResult/PageQuery}、ORM 基类 {@code BaseEntity}。</li>
 *   <li>{@code context}  —— 用户上下文 {@code UserContextHolder}/{@code UserContext}（ThreadLocal）。</li>
 *   <li>{@code feign}    —— Feign 透传拦截器 {@code FeignAuthInterceptor}。</li>
 *   <li>{@code constant} —— 跨服务常量（如 {@code HeaderConstants} 的 X-User-Id 等 Header 名）。</li>
 *   <li>{@code enums}    —— 公共枚举归属地。</li>
 * </ul>
 * 注意：{@code domain} 此处指“核心类型容器”，与业务微服务内部的 domain 包裹层（已禁用）含义不同。
 * </p>
 */
package com.wu.cloud.common.core;
