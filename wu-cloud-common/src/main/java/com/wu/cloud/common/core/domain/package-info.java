/**
 * 核心类型层（非业务 domain 包裹层）
 * <p>
 * 仅放置跨服务通用的基础类型：
 * <ul>
 *   <li>{@code R}             —— 统一响应包装</li>
 *   <li>{@code PageResult}    —— 分页响应</li>
 *   <li>{@code PageQuery}     —— 分页请求基类</li>
 *   <li>{@code BaseEntity}    —— 通用 ORM 实体基类（业务实体继承）</li>
 * </ul>
 * 业务微服务内部的 {@code domain/} 包裹层已被规范禁用，请勿在此处新增业务实体。
 * </p>
 */
package com.wu.cloud.common.core.domain;
