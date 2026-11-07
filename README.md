<div align="center">

# 🛡️ Admin Platform · Wu-Admin-Cloud

### 企业级微服务后台 · 细粒度权限 · PC + 移动端一体

[![GitHub Repo](https://img.shields.io/badge/GitHub-wushij%2Fwu--admin-181717?style=flat-square&logo=github)](https://github.com/wushij/wu-admin)
[![Gitee Repo](https://img.shields.io/badge/Gitee-wusj17%2Fwu--admin-C71D23?style=flat-square&logo=gitee)](https://gitee.com/wusj17/wu-admin)
[![Online Demo](https://img.shields.io/badge/Demo-wushij.com-0078D4?style=flat-square&logo=googlechrome)](https://wushij.com)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.2-6DB33F?style=flat-square&logo=spring)](https://spring.io/projects/spring-cloud)
[![Spring Cloud Alibaba](https://img.shields.io/badge/Spring%20Cloud%20Alibaba-2025.0.0.0-FF6A00?style=flat-square&logo=alibabacloud)](https://sca.aliyun.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.16-6DB33F?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Nacos](https://img.shields.io/badge/Nacos-3.0.3-1890FF?style=flat-square)](https://nacos.io)
[![JDK](https://img.shields.io/badge/JDK-17-ED8B00?style=flat-square&logo=openjdk)](https://openjdk.org)
[![MyBatis Plus](https://img.shields.io/badge/MyBatis%20Plus-3.5.18-1B6B93?style=flat-square)](https://baomidou.com)
[![Sa-Token](https://img.shields.io/badge/Sa--Token-1.46.0-2E9E5B?style=flat-square)](https://sa-token.cc)
[![Vue 3](https://img.shields.io/badge/Vue-3.5.44-4FC08D?style=flat-square&logo=vuedotjs)](https://vuejs.org)
[![TypeScript](https://img.shields.io/badge/TypeScript-6.0.3-3178C6?style=flat-square&logo=typescript)](https://www.typescriptlang.org)
[![Vite](https://img.shields.io/badge/Vite-8.3.5-646CFF?style=flat-square&logo=vite)](https://vite.dev)
[![Element Plus](https://img.shields.io/badge/Element%20Plus-2.14.7-409EFF?style=flat-square)](https://element-plus.org)
[![uni-app](https://img.shields.io/badge/uni--app-3.0-2B9939?style=flat-square)](https://uniapp.dcloud.net.cn)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql)](https://www.mysql.com)
[![Redis](https://img.shields.io/badge/Redis-7.4-DC382D?style=flat-square&logo=redis)](https://redis.io)
[![Node.js](https://img.shields.io/badge/Node.js-24-5FA04E?style=flat-square&logo=nodedotjs)](https://nodejs.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=flat-square)](LICENSE)

<p align="center">
  <a href="https://github.com/wushij/wu-admin"><b>🔗 GitHub</b></a> &nbsp;•&nbsp;
  <a href="https://gitee.com/wusj17/wu-admin"><b>🔴 Gitee</b></a> &nbsp;•&nbsp;
  <a href="https://wushij.com"><b>🌐 在线演示</b></a> &nbsp;•&nbsp;
  <a href="https://app.wushij.com"><b>📱 移动端 H5</b></a> &nbsp;•&nbsp;
  <a href="#-微服务与应用清单"><b>🧩 微服务清单</b></a> &nbsp;•&nbsp;
  <a href="#-快速开始"><b>🚀 快速开始</b></a> &nbsp;•&nbsp;
  <a href="#-系统架构"><b>🏗 系统架构</b></a> &nbsp;•&nbsp;
  <a href="#-目录结构"><b>📁 目录结构</b></a>
</p>

</div>

---

**Admin Platform Cloud（Wu-Admin-Cloud）** 是一套基于 **Spring Cloud 2025 + Spring Cloud Alibaba 2025 + Spring Boot 3.5 + Nacos 3.x + Sa-Token** 的企业级微服务后台管理系统，配套 **Vue 3 PC 端（`wu-admin-web`）** 与 **uni-app 移动端（`wu-admin-uniapp`，H5 / 微信小程序）**。

工程包含 **11 个独立可运行微服务/应用**（1 个 Gateway 响应式网关 + 1 个 Auth 认证中心 + 1 个 BFF 聚合服务 + 8 个领域微服务）与 **3 个公共基础设施组件库**，遵循 **Database-per-Service（一服务一库，8 个物理隔离 Schema）** 与 **OpenFeign + Sentinel 契约化通信** 标准，覆盖组织（部门）五级数据隔离、角色权限、工单审批、企业 IM、AI 智能助手与知识库、系统监控大屏、多引擎文件存储、跨库代码生成、支付/订单/短信/邮件集成等场景。

> 🔗 **在线体验**：PC 端 <https://wushij.com> · 移动端 H5 <https://app.wushij.com> · 体验账号 `zhangsan` / `zhangsan123`

---

## 目录

- [功能特性](#-功能特性)
- [技术栈](#-技术栈)
- [系统架构](#-系统架构)
- [微服务与应用清单](#-微服务与应用清单)
- [快速开始](#-快速开始)
- [目录结构](#-目录结构)
- [配置说明](#-配置说明)
- [开发指南](#-开发指南)
- [测试与 CI](#-测试与-ci)
- [构建与部署](#-构建与部署)
- [源码仓库](#-源码仓库)
- [开源协议](#-开源协议)

---

## ✨ 功能特性

### 微服务架构特性

- **11 个独立 JVM 应用**：1 个网关（`wu-cloud-gateway`）+ 1 个认证中心（`wu-cloud-auth`）+ 1 个 BFF 聚合层（`wu-admin`）+ 8 个业务领域微服务，支持按域独立部署与弹性扩缩容
- **Database-per-Service 彻底分库**：8 个业务微服务各自拥有独立数据库 Schema（`wu_system` / `wu_file` / `wu_ticket` / `wu_ai` / `wu_message` / `wu_infra` / `wu_pay` / `wu_order`），零跨库 SQL 直连，严格保证领域数据边界
- **统一网关入口**：`wu-cloud-gateway` (`:8080`) 统一承载路由分发、Sa-Token 响应式鉴权、反爬风控、国密报文加解密、伪造 Header 清洗与内部接口隔离
- **Nacos 3.x 服务注册与配置中心**：统一注册至命名空间 `dev` / 分组 `WU_CLOUD`，网关与服务间通过 `lb://` 负载均衡调用
- **OpenFeign 契约化通信 + Sentinel 熔断**：跨服务远程调用契约集中收口于 `wu-cloud-api`，内部接口路径统一为 `/inner/**`，全量配备 `FallbackFactory` 降级兜底；网关 `InnerApiGuardFilter` 拦截外部对 `/inner/**` 的越权访问并返回 403
- **Sa-Token 分布式共享会话 + 上下文透传**：网关校验 Token 后将用户 ID、用户名、角色集、权限集、部门及数据范围注入 `X-User-*` 请求头，下游微服务通过 `UserContextInterceptor` 零 Redis IO 还原 `UserContextHolder`
- **分布式动态配置 + Redis Pub/Sub 热刷新**：`sys_config_group` 12 组系统配置由 `wu-service-system` 统一维护，更新时通过 Redis Pub/Sub 广播通道 `wu:cloud:config:refresh` 实时失效各微服务本地 Caffeine 缓存，全集群毫秒级生效
- **渐进式最终一致性**：采用「OpenFeign 同步调用 + 本地事务 + Outbox 本地消息表 / Quartz 定时任务补偿」保障跨服务业务最终一致

### 安全体系

- **网关统一鉴权 + 细粒度权限**：Gateway `SaReactorFilter` 统一拦截未登录请求，下游服务通过 `@RequirePermission` / `PermissionChecker` 校验菜单与按钮权限，未登录返回 401、无权限返回 403
- **BCrypt 密码哈希 + SM2 传输加密**：密码经 BCrypt 哈希落库；前端默认使用服务端下发的 SM2 公钥加密口令后提交（`sm2:` 前缀 C1C3C2 密文，可在安全配置关闭），`PasswordCipherService` 私钥解密后校验，私钥永不下发；明文提交保持兼容
- **httpOnly Cookie / Header 双模传 Token** + 全局安全响应头（`nosniff` / `X-XSS-Protection` / 禁缓存 / HTTPS 下 HSTS）
- **滑块行为验证码**：`wu-cloud-auth` 服务端渲染带缺口的背景图与拼图块，缺口 X 坐标仅存 Redis；校验拖动轨迹与耗时（事件数、耗时区间、终点一致性）后签发 2 分钟一次性票据；票据绑定业务场景与客户端 IP，同一 challenge 连续失败 3 次立即作废
- **登录锁定与防爆破**：账号级 + IP 级失败计数，阈值可配置，管理员可远程解锁；图形/滑块验证码错误不计入密码失败次数；短信与邮箱验证码按「场景 + 目标」记录失败次数，超限自动拒绝并叠加 IP 频率限制
- **客户端 IP 信任链**：`ClientIpUtils` 仅当请求来自可信代理（回环与内网网段）时才读取 `X-Real-IP` / `X-Forwarded-For` / `CF-Connecting-IP`，防止直连伪造代理头绕过风控
- **国密 API 安全防线**：网关 `ApiSecurityFilter` 全链路支持国密 SM4 接口传输加解密（**SM4-GCM 认证加密**，`IV[12] + Ciphertext + Tag[16]`，认证失败直接拒绝、不降级）与数字签名防篡改（HMAC-SM3 / SM2），整合 5 分钟滑动时间戳（`X-Timestamp`）、Redis 随机数防重放（`X-Nonce`）及 `Encrypt-then-Sign` 签名校验；会话密钥由 `/api/auth/session-sign-init` 协商下发
- **行为风控与安全审计**：网关 `AntiBotGateFilter` 全路径执行 IP 封禁与爬虫 UA 指纹（python-requests / curl / headless / selenium 等）拦截；`ApiSecurityFilter` 对时间戳缺失、重放、签名不匹配等行为累计风险分（Redis 1 小时滚动），默认 80 分自动临时封禁 1 小时、120 分封禁 24 小时；拦截明细经内部接口异步落库 `wu_system.sys_security_event`，提供今日拦截/爬虫/重放/封禁统计与一键解封面板
- **邮件服务与防垃圾投递**：支持 SMTP 多服务商（QQ/163/Gmail/自定义）、RFC 2046 规范 `multipart/alternative` 双格式（Plain Text + HTML 卡片），通过 `RemoteConfigLogService` 实时持久化发信日志与投递回执（`wu_infra.sys_email_log`）
- **分布式操作审计**：`@Log` 注解 + AOP 全接口记录（操作人/IP/归属地/参数/耗时），跨服务异步上报至 `wu-service-infra` 落库

### 组织与权限

- **组织（部门）五级数据隔离**：按角色配置五级数据范围（全部 / 自定义部门 / 本部门 / 本部门及以下 / 仅本人），通过 `DataScopeHelper` 与 `@DataScope` 在用户、组织、文件、工单、审批、日志等列表统一过滤，超级管理员自动放行
- **标准组织与岗位**：内置「总公司 / 研发部 / 运营部」三级部门与「总经理 / 技术总监 / 研发工程师」三岗位，开箱即用
- **五维组织架构**：用户 → 角色 → 菜单 → 部门 → 岗位，全关联管理
- **角色权限模型**：菜单按角色动态渲染侧栏，按钮级权限指令 `v-permission` / `v-role`
- **系统配置集群热更新**：12 组配置（下表），数据库持久化 + Redis Pub/Sub 跨服务广播 + 本地缓存失效，运行时即时生效

| 分组 | 名称 | 关键能力 |
|------|------|----------|
| `site` | 基础信息 | 平台名称/副标题/版权，登录页与工作台展示 |
| `session` | 会话配置 | `tokenExpireHours`（1～720），覆盖 Sa-Token 超时 |
| `file` | 文件与存储 | 本地 / MinIO / 腾讯云 COS / 阿里云 OSS 四引擎热切换、连通性测试、`maxSizeMb`、`allowedExtensions`（上限 500MB） |
| `rateLimit` | 接口限流 | 验证码/登录/注册每 IP 每分钟；短信/邮箱防刷（间隔/日上限）；AI 对话每用户每分钟 |
| `login` | 登录认证 | 验证码开关/类型、短信登录、邮箱登录、发码前滑块、记住我、重试锁定 |
| `register` | 注册认证 | 开放注册、验证码、默认角色、需审核、密码最小长度 |
| `thirdParty` | 第三方配置 | 微信/支付宝/GitHub/Google OAuth 密钥 |
| `payment` | 支付配置 | 微信 Native/支付宝当面付，支持通过 Feign 调 `wu-service-pay` 发起**测试订单**与回调验签 |
| `sms` | 短信配置 | 阿里云/腾讯云双通道（策略模式 `SmsSenderFactory`），模板管理、测试发送与发送日志回执 (`wu_infra.sys_sms_log`) |
| `email` | 邮件配置 | QQ/163/Gmail/自定义 SMTP，发件人名称、SSL/TLS/STARTTLS、测试发送与投递日志回执 (`wu_infra.sys_email_log`) |
| `security` | 安全配置 | 前端反调试（禁用 Devtools）、Sa-Token 多端控制（`isConcurrent`）、国密 SM4-GCM 接口加密、HMAC-SM3/SM2 签名、SM2 密码公钥、风控时间戳/Nonce 开关 |
| `ai` | AI 助手配置 | 助手开关、回答边界（聚焦本系统/开放问答）、全局项目知识、单用户每日 Token 配额与角色配额 |

- **字典管理**：字典类型 + 字典数据两级维护，Redis 缓存，前端封装 `DictSelect` / `DictTag` 全局组件

### 流程中心（`wu-service-ticket`）

- **工单系统**：创建 → 指派 → 处理 → 关闭全生命周期，支持优先级、截止时间、评论附件（经 `RemoteFileService` 跨服务上传）、全员通知
- **审批流引擎**：请假/采购/报销/用印/合同/通用 + `REGISTER` 注册审核，支持多节点顺序流转与会签/或签策略，通过/驳回实时通知
- **注册审核联动**：开启注册审核后，新用户注册自动通过 Feign 在 `wu-service-ticket` 创建注册审批单，超管审批通过后远程回调激活 `wu-service-system` 账号

### 消息中心 & 企业 IM（`wu-service-message`）

- **业务消息与系统通知**：工单/审批触达与管理员全员/按部门/指定用户定向广播，发送日志全程追踪
- **企业 IM**：私聊 + 群聊，文本/图片/文件/表情发送，历史分页（每页 50 条），用户信息经 `RemoteUserService` 批量装配
- **@ 提及与消息撤回**：输入 `@` 弹出成员列表，被 @ 用户 WebSocket 强提醒 `[有人@你]`；2 分钟内消息可撤回并实时同步
- **群管理与免打扰**：超管建群、群主转让、成员禁言/移除/解散、群公告置顶推送；免打扰模式下 **@ 我和群公告**始终提醒
- **分布式 WebSocket 广播**：网关 `/ws/**`、`/api/ws/**` Upgrade 透传至 `wu-service-message`，多实例间经 `WebSocketRedisBroadcaster`（Redis Pub/Sub）跨节点投递 `notice` / `chat` / `groupChat` / `groupAnnouncement` / `typing` / `presence` 事件

### AI wu 助手（`wu-service-ai`）

- **全局悬浮球助手**：PC 端右下角悬浮球（`AiWuFloatBtn` + `AiWuChatPanel`）与移动端 `AiWuAssistant` 组件，登录即可唤起流式对话
- **SSE 流式对话**：`POST /api/ai/chat/stream` 基于 `SseEmitter` 增量推送（事件 `delta` / `done` / `error`），专用线程池异步执行
- **多供应商协议分发**：DeepSeek / Kimi / GLM / Claude / 自定义；`AiProviderFactory` 支持 OpenAI 兼容协议 `/chat/completions` 与 Anthropic Messages 协议 `/v1/messages`
- **AI 模型配置**：可视化管理供应商、模型、温度与连通性测试；**API Key 使用国密 SM4-GCM 认证加密存储**（`sm4-gcm:` 前缀，历史 `sm4:` CBC 密文只读兼容）
- **AI 知识库（RAG-lite）**：独立库表 `wu_ai.sys_ai_knowledge`，支持知识 CRUD、分类/关键词、权重排序、JSON 批量导入导出、Markdown 双栏编辑与检索召回演练（ngram 全文索引优先 + LIKE 自动降级）
- **对话自动注入知识与上下文**：`PromptAssembler` 自动检索知识库 `<reference_data>` 参考块，并通过 `RemoteUserService` 装配当前用户角色与菜单上下文（Redis 10 分钟缓存）
- **AI 对话日志与配额防护**：异步落库 `wu_ai.sys_ai_chat_log`，支持提问脱敏（`AiSanitizerUtil`）及单用户/角色每日 Token 配额控制

### 系统监控与基础设施（`wu-service-infra`）

| 模块 | 后端实现 | 前端特性 |
|------|----------|----------|
| **服务监控** | Actuator + OSHI/JMX 采集 CPU/内存/JVM/磁盘 | ECharts 折线趋势图（sessionStorage 保留最近 20 个点，F5 不丢） |
| **缓存监控** | Redis INFO 统计 + SCAN 键巡检 | 内存/QPS/命中率/连接数四宫格图表，键详情查看与安全删除 |
| **API 访问统计** | AOP 采集 → Redis 队列 → 批量落库 `wu_infra` | 统计卡片（请求总数/成功/失败）+ 日志列表 + Excel/CSV 导出 |
| **在线用户** | Sa-Token 会话 + `RemoteOnlineUserService` | ip2region IP 归属地、浏览器/OS 解析，强制下线 |
| **定时任务** | Quartz 分布式调度 + `DistributedJobLock` | Cron 可视化配置、立即执行/暂停/恢复；内置跨服务清理与 Outbox 补偿任务 |

### 开发工具与跨库代码生成（`wu-service-infra`）

- **跨库代码生成器**：基于 `wu-cloud-starter-codegen` Velocity 引擎，部署于 `wu-service-infra`（表 `wu_infra.gen_table` / `gen_table_column`）；`InformationSchemaService` 支持跨 8 个微服务 Schema（`wu_system` ～ `wu_order`）检索与导入表结构，自动根据 `wu_<module>` 推导 `moduleName`，一键生成 Controller / Service / Mapper / Entity / Vue 页面完整代码，支持预览、ZIP 下载与自动创建菜单（经 `RemoteMenuService`）
- **Knife4j 接口文档**：Springdoc OpenAPI 3（`2.10.0`）+ Knife4j（`4.5.0`），网关聚合各微服务 `/v3/api-docs`
- **列表导出**：EasyExcel 实现 Excel/CSV 双格式导出（`scope=filtered|all`），覆盖用户、日志、工单、审批、API 访问与在线用户

### 多引擎文件存储与跨服务回收中心（`wu-service-file` & `wu-service-system`）

- **多引擎文件存储**：`StorageRoutingService` + `ObjectStorageClient` 支持本地磁盘 / MinIO / 腾讯云 COS / 阿里云 OSS 四引擎热切换、连通性测试与失败回滚；`wu_file.sys_file` 记录每个文件的 `storage_type` 与 `bucket_name`，读取支持跨引擎回退，删除按历史引擎精准路由
- **统一流式访问**：`StorageFileController` 提供 `/api/storage/files/{bucket}/**`（网关公开 GET），支持图片直出、音视频 Range 断点续传、危险类型强制下载与路径穿越防护
- **跨服务回收中心**：`wu-service-system` 的 `RecycleController` 通过 OpenFeign 聚合 `system`、`ticket`、`file`、`infra` 四个微服务的 `/inner/*/recycle/**` 接口，统一汇总 **14 类软删数据**（用户/角色/菜单/部门/岗位/字典类型/字典数据/工单/审批/系统通知/文件/代码生成/定时任务/调度日志），支持一键恢复与彻底物理删除

### 交易、支付与订单（`wu-service-pay` & `wu-service-order`）

- **支付中心（`wu-service-pay`）**：集成微信支付 APIv3（Native）与支付宝当面付 SDK，支持统一下单、回调验签、主动查单、退款申请/执行与测试支付单
- **订单与库存中心（`wu-service-order`）**：独立维护商品库存（`StockServiceImpl` 乐观锁扣减与归还）与交易订单状态机，支付成功后由 `wu-service-pay` 经 Feign / Outbox 驱动订单状态流转

---

## 🏗 技术栈

**后端（微服务集群）**
- **核心框架**：Spring Boot **3.5.16** · Spring Cloud **2025.0.2** · Spring Cloud Alibaba **2025.0.0.0**
- **服务治理与网关**：Nacos **3.x**（`nacos-client` **3.0.3**，注册中心 + 配置中心）· Spring Cloud Gateway（WebFlux 响应式网关）· OpenFeign · Sentinel **1.8.8**（熔断降级）
- **认证与安全**：Sa-Token **1.46.0**（Redis 共享会话 + 网关鉴权）· `spring-security-crypto`（BCrypt）· BouncyCastle **1.86**（国密 SM4-GCM / SM2 / HMAC-SM3）
- **数据持久与缓存**：MyBatis Plus **3.5.18**（直接注入 Mapper 规范 + `mybatis-plus-jsqlparser` 分页）· Druid **1.2.28** · Redis + Redisson **3.52.0**
- **对象存储与网络**：MinIO Java SDK **9.0.4**（S3 协议兼容阿里云 OSS / 腾讯云 COS）· OkHttp **5.5.1**
- **第三方 SDK**：微信支付 Java SDK **0.2.17** · 支付宝 SDK **4.40.1061** · 阿里云短信 **4.6.0** / 阿里云号码认证 **2.0.0** / 腾讯云短信 **3.2.18**
- **工具与组件**：Quartz · Knife4j **4.5.0** + Springdoc **2.10.0** · Velocity **2.4.1** · Hutool **5.8.48** · EasyExcel **4.0.3** · OSHI **6.6.5** · ZXing **3.5.4** · ip2region **3.3.7**

**PC 前端（`wu-admin-web`）**
- Vue **3.5.44** + Composition API + TypeScript **6.0.3**
- Pinia **4.1.0** · Vue Router **5.4.0** · Element Plus **2.14.7** · Vite **8.3.5**
- ECharts **6.1.0** · Three.js **0.187.0** · tsparticles **4.5.1**
- sm-crypto **0.5.8**（浏览器端 SM2/SM3/SM4-GCM）· markdown-it **15.0.3** + highlight.js **11.12.0**

**移动端（`wu-admin-uniapp`）**
- uni-app **3.0**（Vue 3.5）· uv-ui **1.1.20** · Pinia **4.1** · luch-request **4.0** · sm-crypto **0.5.8** · markdown-it **15** / highlight.js **11**

**运行时环境**
- JDK **17+** · MySQL **8.0** · Redis **7.4** · Nacos **3.x** · Node.js **24+** · Maven **3.8+**

**测试体系**
- Vitest **5.1**（前端 + 移动端）· JUnit 5 + Mockito（后端 19 个 Maven 模块全量单测覆盖）

---

## 🖥 系统架构

```text
┌────────────────┐            ┌──────────────────────┐
│  PC / H5 / 小程序│   HTTPS    │       Nginx           │
│ wu-admin-web    │ ────────►  │ 反向代理 / 静态资源    │
│ wu-admin-uniapp │            │ /api → 8080, /ws → WS │
└────────────────┘            └──────────┬───────────┘
                                         │ /api, /ws
                                         ▼
                          ┌──────────────────────────┐
                          │   wu-cloud-gateway :8080  │
                          │  AntiBotGate 反爬/IP封禁   │
                          │  ApiSecurity(SM4-GCM/签名)│
                          │  SaReactorFilter 统一鉴权  │
                          │  InnerApiGuard(/inner→403)│
                          │  AuthGlobalFilter(X-User) │
                          └──────────┬───────────────┘
                                     │ lb:// (Nacos 3.x)
    ┌────────────┬─────────────┬─────┴──────┬────────────┬─────────────┐
    ▼            ▼             ▼            ▼            ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ auth:9200│ │system:9201│ │ticket:9202│ │message:9203│ │ ai:9204  │ │ pay:9205 │
│  (Redis) │ │ wu_system│ │ wu_ticket│ │ wu_message│ │  wu_ai   │ │  wu_pay  │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
    ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
    │order:9206│ │ file:9207│ │infra:9300│ │admin:9100│
    │ wu_order │ │  wu_file │ │ wu_infra │ │ (BFF聚合) │
    └──────────┘ └──────────┘ └──────────┘ └──────────┘
                                 │
          ┌──────────────────────┼──────────────────────┐
          ▼                      ▼                      ▼
┌──────────────────┐   ┌──────────────────┐   ┌────────────────────────┐
│ MySQL 8.0 (8库)  │   │   Redis 7.x      │   │ 对象存储（可热切换）     │
│ wu_system ~ order│   │ db=0 会话/PubSub │   │ local / MinIO / OSS/COS │
└──────────────────┘   └──────────────────┘   └────────────────────────┘
```

| 维度 | 说明 |
|------|------|
| **网关过滤链** | `InnerApiGuardFilter`（拦截入站 `/inner/**`）→ `AntiBotGateFilter`（全路径 IP 封禁 / 爬虫 UA 拦截）→ `ApiSecurityFilter`（时间戳 / Nonce / HMAC-SM3 签名 / SM4-GCM 加解密 / 风险分封禁）→ `SaReactorFilter`（登录态校验）→ `AuthGlobalFilter`（清洗并注入 `X-User-*` 上下文 Header） |
| **跨服务通信** | OpenFeign 契约集中在 `wu-cloud-api`（`RemoteXxxService`），服务间走 `lb://` 直连 `/inner/{域}/**`，全量配置 Sentinel `FallbackFactory` 降级 |
| **数据隔离模型** | **物理层**：8 个业务服务连接各自独立的 `wu_*` 数据库 Schema；**业务层**：登录用户 → 角色 `data_scope` → `DataScopeHelper` 按可见部门集合过滤列表数据 |
| **动态配置同步** | `wu-service-system` 修改 `sys_config_group` 后发布 Redis Pub/Sub `wu:cloud:config:refresh` 消息，各服务 `DynamicConfigRefreshSubscriber` 监听并驱逐本地缓存 |
| **WebSocket 集群** | `/ws/**`、`/api/ws/**` 路由至 `wu-service-message`，多节点间通过 Redis Pub/Sub 通道广播私聊、群聊、在线状态与系统通知 |

---

## 🧩 微服务与应用清单

### 核心应用与微服务（11 个）

| 模块名称 | 类型 | 端口 | 独立数据库 Schema | 核心职责 |
|---------|------|------|-------------------|----------|
| `wu-cloud-gateway` | 🚪 响应式网关 | `8080` | —（Redis `db=0`） | 统一入口、路由分发、反爬风控、国密 SM4-GCM 加解密、Sa-Token 鉴权、Header 清洗 |
| `wu-cloud-auth` | 🔑 认证中心 | `9200` | —（Redis `db=0`） | 密码/短信登录、注册、找回密码、滑块行为验证码、SM2 密码解密、`session-sign-init`、个人中心 |
| `wu-admin` | 📊 BFF 聚合层 | `9100` | —（Redis `db=0`） | 工作台大盘等跨多域微服务的聚合视图编排 |
| `wu-service-system` | ⚙️ 系统核心服务 | `9201` | `wu_system` (`01_wu_system.sql`) | 用户/角色/菜单/部门/岗位/字典/12 组系统配置/安全风控事件/登录日志/跨服务回收中心汇总 |
| `wu-service-ticket` | 📋 工单审批服务 | `9202` | `wu_ticket` (`03_wu_ticket.sql`) | 工单全生命周期、多节点会签/或签审批流引擎、注册审核单、站内通知 |
| `wu-service-message` | 💬 消息通讯服务 | `9203` | `wu_message` (`05_wu_message.sql`) | 企业 IM 私聊/群聊、@提及、撤回、群公告、黑名单、WebSocket 实时推送与 Redis 广播 |
| `wu-service-ai` | 🤖 AI 智能服务 | `9204` | `wu_ai` (`04_wu_ai.sql`) | SSE 流式对话（OpenAI/Anthropic 双协议）、模型配置（SM4-GCM 加密）、RAG 知识库、对话审计 |
| `wu-service-pay` | 💳 支付服务 | `9205` | `wu_pay` (`07_wu_pay.sql`) | 微信 Native / 支付宝当面付、回调验签、退款执行、支付测试单 |
| `wu-service-order` | 📦 订单库存服务 | `9206` | `wu_order` (`08_wu_order.sql`) | 交易订单状态机、订单明细、商品库存乐观锁扣减与归还 |
| `wu-service-file` | 📁 文件存储服务 | `9207` | `wu_file` (`02_wu_file.sql`) | 本地 / MinIO / COS / OSS 四引擎热切换、`/api/storage/files/**` 流式访问、文件分组与回收站 |
| `wu-service-infra` | 🛠️ 基础设施服务 | `9300` | `wu_infra` (`06_wu_infra.sql`) | 跨库代码生成（`gen_table`）、操作日志/访问日志、短信/邮件日志、Quartz 定时任务、服务/缓存监控、在线用户、Outbox 补偿 |

### 公共基础组件与契约库（3 个）

| 模块名称 | 类型 | 说明 |
|---------|------|------|
| `wu-cloud-common` | 🧰 公共底层核心包 | `R<T>`、`PageResult`、`BusinessException`、`UserContextHolder`、`DataScopeHelper`、国密 SM2/SM4-GCM 工具、`DynamicConfigHelper`、双通道短信/SMTP 邮件发送器 |
| `wu-cloud-api` | 🤝 Feign 契约包 | 全量跨服务 `RemoteXxxService` OpenFeign 接口、DTO/VO 契约与 Sentinel `FallbackFactory` 降级实现 |
| `wu-cloud-infra` | 🏗️ Starter 组件库 | `wu-cloud-starter-redis`（Redis/Redisson）、`wu-cloud-starter-monitor`（Actuator/OSHI 监控指标）、`wu-cloud-starter-codegen`（Velocity 代码生成引擎） |

---

## 🚀 快速开始

### 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | **17+** | 后端编译与运行 |
| Maven | **3.8+** | 后端多模块构建 |
| MySQL | **8.0** | 默认创建 8 个微服务独立库（`wu_system` ～ `wu_order`） |
| Redis | **7.x** | **database 0**（gateway / auth / 业务服务必须共用同一库以共享 Token 与配置广播） |
| Nacos | **3.x** | 控制台 **`http://127.0.0.1:8849`**；微服务 `server-addr` **`127.0.0.1:8848`**；命名空间 `dev`，分组 `WU_CLOUD` |
| Node.js | **24+** | PC 前端（`wu-admin-web`）与移动端（`wu-admin-uniapp`） |

### 1. 初始化数据库（8 个独立微服务库）

推荐使用初始化脚本一键创建并导入 8 个微服务的独立 Schema（`wu_system`、`wu_file`、`wu_ticket`、`wu_ai`、`wu_message`、`wu_infra`、`wu_pay`、`wu_order`）：

```powershell
# Windows PowerShell（默认账号 root/root，可通过参数 -User / -Password 覆盖）
.\scripts\init-databases.ps1

# Linux / macOS
./scripts/init-databases.sh
```

也可使用 MySQL 客户端手动按序导入（每个脚本均自带 `CREATE DATABASE IF NOT EXISTS` 与 `USE`）：

```bash
mysql -u root -p < sql/01_wu_system.sql   # → wu_system  (wu-service-system)
mysql -u root -p < sql/02_wu_file.sql     # → wu_file    (wu-service-file)
mysql -u root -p < sql/03_wu_ticket.sql   # → wu_ticket  (wu-service-ticket)
mysql -u root -p < sql/04_wu_ai.sql       # → wu_ai      (wu-service-ai)
mysql -u root -p < sql/05_wu_message.sql  # → wu_message (wu-service-message)
mysql -u root -p < sql/06_wu_infra.sql    # → wu_infra   (wu-service-infra，含 gen_table 代码生成表)
mysql -u root -p < sql/07_wu_pay.sql      # → wu_pay     (wu-service-pay)
mysql -u root -p < sql/08_wu_order.sql    # → wu_order   (wu-service-order)
```

### 2. 启动基础设施（Nacos & Redis）

1. **Nacos 3.x**：启动后浏览器打开 **`http://127.0.0.1:8849`**，创建命名空间 ID 为 `dev`（各微服务 `application.yml` 中的 `server-addr` 保持 **`127.0.0.1:8848`**）；可选执行 `.\scripts\nacos-publish-common.ps1` 一键发布公共配置。
2. **Redis**：启动在 `127.0.0.1:6379`，使用 `database: 0`。

### 3. 编译与启动后端微服务

```powershell
# 1) 全量编译并安装公共契约包到本地仓库
mvn clean install -DskipTests

# 2) 方式 A（推荐）：使用 VS Code / Cursor 的 .vscode/launch.json
#    选择「🚀 一键启动后端全量微服务 (11个JVM服务)」

# 3) 方式 B：使用脚本一键后台启动全部微服务
.\scripts\start-services.ps1

# 4) 方式 C：命令行按模块启动
mvn -pl wu-cloud-gateway spring-boot:run -DskipTests
mvn -pl wu-cloud-auth spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-system spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-infra spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-file spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-ticket spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-message spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-ai spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-pay spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-order spring-boot:run -DskipTests
mvn -pl wu-admin spring-boot:run -DskipTests
```

> **推荐启动顺序**：`Nacos / Redis` → `gateway (8080)` → `system (9201)` → `auth (9200)` → `infra / file / ticket / message / ai / pay / order` → `wu-admin BFF (9100)`。

### 4. 启动 PC 前端（3000）

```powershell
cd wu-admin-web
npm install
npm run dev
```

访问：**http://localhost:3000**（Vite 自动将 `/api` 与 `/ws` 代理至网关 `http://localhost:8080`）。

### 5. 启动移动端 H5（5174，可选）

```powershell
cd wu-admin-uniapp
npm install
npm run dev:h5
```

访问：**http://localhost:5174**。

### 默认登录账号

| 用户名 | 密码 | 角色 |
|--------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | `admin123` | 普通用户 |

---

## 📁 目录结构

```text
wu-admin-cloud/                        # 工程根目录 (Maven Reactor 19 个模块)
├── pom.xml                            # 父 POM（统一管理 Spring Boot 3.5.16 / Cloud 2025.0.2 版本）
├── .vscode/launch.json                # VS Code 本地一键启动配置
├── .github/workflows/ci.yml           # GitHub Actions CI 流水线（mvn test + 前端/移动端单测）
├── wu-cloud-common/                   # 公共底层核心库（R<T>、异常、国密、DataScope、动态配置、短信/邮件）
├── wu-cloud-api/                      # 跨服务 OpenFeign 契约与 FallbackFactory 降级工厂
│   └── src/main/java/com/wu/cloud/api/
│       ├── system/                    # RemoteUserService / RemoteConfigService / RemoteSecurityEventService 等
│       ├── infra/                     # RemoteLogService / RemoteConfigLogService / RemoteOnlineUserService 等
│       ├── file/                      # RemoteFileService / RemoteFileRecycleService
│       ├── ticket/                    # RemoteTicketService / RemoteApprovalService / RemoteNoticeService
│       ├── pay/                       # RemotePayService
│       └── order/                     # RemoteOrderService
├── wu-cloud-infra/                    # 技术基础设施 Starter 聚合
│   ├── wu-cloud-starter-redis/        # RedisTemplate / Redisson 分布式锁与缓存组件
│   ├── wu-cloud-starter-monitor/      # Actuator / OSHI 硬件与 JVM 监控采集
│   └── wu-cloud-starter-codegen/      # Velocity 代码生成器核心模板与引擎
├── wu-cloud-gateway/                  # [8080] 🚪 响应式网关（路由、反爬、SM4-GCM 安全、Sa-Token 鉴权）
├── wu-cloud-auth/                     # [9200] 🔑 认证中心（登录/注册/滑块验证码/SM2 解密/个人中心）
├── wu-admin/                          # [9100] 📊 BFF 聚合服务（工作台跨服务数据聚合）
├── wu-cloud-service/                  # 8 大业务领域微服务
│   ├── wu-service-system/             # [9201] ⚙️ 系统核心服务 → 库 wu_system
│   ├── wu-service-file/               # [9207] 📁 多引擎文件服务 → 库 wu_file
│   ├── wu-service-ticket/             # [9202] 📋 工单与审批服务 → 库 wu_ticket
│   ├── wu-service-ai/                 # [9204] 🤖 AI 助手与知识库 → 库 wu_ai
│   ├── wu-service-message/            # [9203] 💬 企业 IM 与通知 → 库 wu_message
│   ├── wu-service-infra/              # [9300] 🛠️ 监控/任务/日志/代码生成 → 库 wu_infra
│   ├── wu-service-pay/                # [9205] 💳 微信/支付宝支付 → 库 wu_pay
│   └── wu-service-order/              # [9206] 📦 订单与库存管理 → 库 wu_order
├── wu-admin-web/                      # Vue 3.5 + TypeScript 6.0 + Vite 8 PC 前端
├── wu-admin-uniapp/                   # uni-app 3.0 移动端（H5 / 微信小程序）
├── sql/                               # 01～08 独立微服务建库脚本 + admin_platform.sql 基线
├── scripts/                           # 数据库初始化、Nacos 配置发布、服务启停与冒烟测试脚本
└── docs/                              # 微服务标准化改造指南、Nacos 配置样例与部署文档
```

### 后端微服务分层与编码规范

后端采用 **「微服务物理隔离 + 契约中心化 + 服务内四层内聚」** 的架构规范：

1. **严格的模块依赖方向**：
   - `wu-service-*` / `wu-cloud-auth` / `wu-admin` → `wu-cloud-api` → `wu-cloud-common`；
   - 各业务微服务之间**严禁直接互相引用 jar 包**，跨服务协作必须 100% 通过 `wu-cloud-api` 中的 `RemoteXxxService` Feign 接口完成。
2. **禁止跨库 SQL**：
   - 每个微服务仅允许访问自身所属的 `wu_*` 数据库 Schema，严禁在 Mapper XML 或 `JdbcTemplate` 中编写跨库表名（唯一例外为 `wu-service-infra` 代码生成器的 `InformationSchemaService` 元数据自省）。
3. **MyBatis-Plus 3.5.18 规范**：
   - 适配 MyBatis-Plus `3.5.18` 移除 `extension.service.ServiceImpl` 的演进，所有业务 `ServiceImpl` 统一直接注入领域 `Mapper` 承载数据访问，杜绝反射注入 `baseMapper`。
4. **内部接口隔离规范**：
   - 供 Feign 调用的内部 Controller 统一放置在各服务的 `api/` 包下，路径统一以 `/inner/{域}/**` 开头，由网关 `InnerApiGuardFilter` 阻断外部直接访问。

### PC 前端目录分层约定（`wu-admin-web/src/`）

```text
wu-admin-web/src/
├── core/                  # 基础设施层：全局唯一 Axios 客户端（request.ts），SM4-GCM 报文加解密、防重放与拦截器收口
├── api/                   # 接口定义层：按业务域组织，纯函数声明（URL、Method、入参与出参类型），不写 UI 逻辑
├── views/                 # 页面视图层：按路由编排（ai / system / monitor / message / dashboard / tool）
├── components/            # 全局通用组件（AiWu 悬浮助手、SliderCaptcha、DictSelect、3D 地球等）
├── composables/           # 组合式函数：跨页面状态与业务逻辑复用（useDict、useStreamingMarkdown 等）
├── utils/                 # 领域纯工具库：无 Vue 响应式依赖的纯函数（chat / security / tree / permission.ts）
├── directives/            # 自定义指令：v-permission / v-role（按钮级鉴权）
├── router/                # 路由配置与动态权限路由守卫
├── store/                 # Pinia 全局状态管理（user / message / site / tagsView / aiWu）
├── constants/             # 全局常量与字典枚举映射
├── styles/                # SCSS 全局样式系统与主题变量
└── types/                 # 全局 TypeScript 接口与类型定义
```

### 移动端目录分层约定（`wu-admin-uniapp/src/`）

```text
wu-admin-uniapp/src/
├── pages/ pages-sub/      # 路由级页面：主包（首页/工作台/消息/我的）+ 分包（system/monitor/msg/ai/log/mine）
├── components/            # 三层组件体系：base（纯 UI 原子） / layout（页面骨架） / business（领域业务组件）
├── composables/           # 组合式函数，按领域分目录，base/ 放跨域复用逻辑
├── core/                  # 唯一基础设施层：http.ts、ws.ts、storage.ts、auth.ts、cache.ts、api-base.ts
├── utils/                 # 纯函数，按 nav / tree / display / security 分组收口
├── api/                   # 只放接口定义（地址、方法、入参、返回类型）
├── types/                 # 数据结构统一收口
├── constants/             # 常量与 TabBar 配置
└── store/                 # Pinia 全局状态
```

---

## 🔧 配置说明

### 数据库设计规范

所有微服务的业务表遵循统一范式：

| 维度 | 规范 |
|------|------|
| 主键 | `BIGINT AUTO_INCREMENT PRIMARY KEY` |
| 组织隔离字段 | 用户、文件、文件分组等归属 `dept_id`，列表按角色 `data_scope` 自动过滤 |
| 审计字段 | `create_time` / `update_time` / `creator` / `updater`（MyBatis-Plus 自动填充） |
| 逻辑删除 | `deleted TINYINT DEFAULT 0`，MyBatis-Plus `logic-delete-value: 1` |
| 字符集 | `utf8mb4_unicode_ci`（完整支持 Emoji） |

### 多级配置体系（Nacos + 本地 YAML + 动态配置表）

| 层级 | 存放位置 | 内容说明 |
|------|----------|----------|
| **L0 启动引导** | 各微服务本地 `application.yml` | 服务端口、`spring.application.name`、Nacos 地址、所属 `wu_*` 数据库连接 |
| **L1 集群公共** | Nacos `wu-cloud-common.yaml`（Group=`WU_CLOUD`） | Redis 连接、Sa-Token 统一参数、Feign 超时与 Sentinel 公共配置（本地 YAML 保留兜底） |
| **L2 服务私有** | Nacos `${spring.application.name}.yaml`（可选） | 单服务特定参数与开关（通过 `optional:nacos:` 导入，未发布不影响启动） |
| **L3 业务动态** | `wu_system.sys_config_group` 表 | `site` / `login` / `register` / `security` / `file` / `ai` / `sms` / `email` / `payment` 等 12 组运行时热配置 |

### 核心网关路由与 API 前缀总览

| 外部访问前缀 | 目标微服务 | 说明 | 认证要求 |
|--------------|------------|------|:--:|
| `/api/auth/**` | `wu-cloud-auth` (`:9200`) | 登录/注册/验证码/滑块挑战/SM2 公钥/session-sign-init/个人中心 | 🌐 匿名/登录 |
| `/api/admin/**` `/api/dashboard/**` | `wu-admin` (`:9100`) / `wu-service-system` | 工作台统计与跨服务聚合大盘 | 🔒 登录 |
| `/api/system/**` | `wu-service-system` (`:9201`) | 用户/角色/菜单/部门/岗位/字典/12 组配置/安全风控/回收中心 | 🔒 登录+权限 |
| `/api/ticket/**` `/api/approval/**` `/api/notice/**` | `wu-service-ticket` (`:9202`) | 工单流转、审批流引擎、站内通知 | 🔒 登录+权限 |
| `/api/message/**` `/ws/**` `/api/ws/**` | `wu-service-message` (`:9203`) | 企业 IM 私聊/群聊、群管理、WebSocket 实时长连接 | 🔒 登录 |
| `/api/ai/**` `/api/system/ai-*` | `wu-service-ai` (`:9204`) | AI wu 助手 SSE 流式对话、模型配置、对话日志、RAG 知识库 | 🔒 登录+权限 |
| `/api/pay/**` | `wu-service-pay` (`:9205`) | 微信/支付宝下单、退款、支付回调（`/api/pay/notify/**` 白名单） | 🔒 登录/白名单 |
| `/api/order/**` `/api/stock/**` | `wu-service-order` (`:9206`) | 交易订单管理、商品库存扣减与归还 | 🔒 登录+权限 |
| `/api/file/**` `/api/file-group/**` `/api/storage/files/**` | `wu-service-file` (`:9207`) | 文件上传/管理/分组、四引擎对象存储统一流式直出（GET 公开） | 🔒 登录/公开 |
| `/api/infra/**` `/api/monitor/**` `/api/tool/gen/**` | `wu-service-infra` (`:9300`) | 服务/缓存/在线用户/API 访问监控、Quartz 定时任务、跨库代码生成 | 🔒 登录+权限 |
| `/inner/**` | 各微服务内部接口 | 仅供服务间 OpenFeign 直连调用，网关入站直接拦截返回 `403` | 🚫 网关禁止 |

---

## 💡 开发指南

### 修改公共模块后必装

修改 `wu-cloud-common`、`wu-cloud-api` 或 `wu-cloud-infra` 下属 Starter 后，请先安装到本地 Maven 仓库，再重启依赖它的微服务：

```powershell
mvn install -pl wu-cloud-common,wu-cloud-api,wu-cloud-infra/wu-cloud-starter-redis,wu-cloud-infra/wu-cloud-starter-monitor,wu-cloud-infra/wu-cloud-starter-codegen -DskipTests
```

### 常见问题排查

| 现象 | 根因与排查方法 |
|------|----------------|
| **全站接口返回 401** | 检查 `wu-cloud-gateway`、`wu-cloud-auth` 与各业务微服务是否连接同一 Redis（`127.0.0.1:6379`，`database: 0`），且 `sa-token.token-name` 均为 `Authorization` |
| **请求报 400/500 且提示密文/签名错误** | 检查前端是否通过 `:8080` 网关访问（直连业务服务端口会跳过网关 `ApiSecurityFilter` 解密）；确认已完成 `/api/auth/session-sign-init` 密钥协商 |
| **Feign 调用报 503 `No servers available`** | 在 Nacos 控制台（`http://127.0.0.1:8849`）检查 `dev` 命名空间、`WU_CLOUD` 分组下目标服务是否已注册在线 |
| **修改系统配置后其他服务未生效** | 确认 Redis Pub/Sub 正常连通（通道 `wu:cloud:config:refresh`），或等待本地 Caffeine 缓存 60s 过期 |

---

## 🧪 测试与 CI

### 运行单元测试

全量单元测试基于 **JUnit 5 + Mockito**（后端）与 **Vitest 5**（前端/移动端），无需启动外部 MySQL / Redis / Nacos 即可独立运行：

```powershell
# 1) 后端 19 个 Maven 模块全量单元测试
mvn test

# 2) 指定单个微服务模块运行测试
mvn test -pl wu-cloud-auth,wu-cloud-service/wu-service-pay,wu-cloud-service/wu-service-order

# 3) PC 前端类型检查、代码规范与单元测试
cd wu-admin-web
npm run typecheck
npm run lint
npm run test

# 4) 移动端类型检查与单元测试
cd ../wu-admin-uniapp
npm run type-check
npm run test

# 5) 全链路本地冒烟测试（需先启动微服务集群）
.\scripts\smoke\smoke-test.ps1
```

### 测试覆盖概要

| 层级 | PC 前端 & 移动端 | 后端微服务（19 个模块） |
|------|------------------|-------------------------|
| **核心与基础** | 登录态、消息推送、页签缓存、纯权限函数 | `R<T>` 响应、分页、全局异常处理、动态配置、分布式锁 |
| **安全与认证** | SM2/SM4-GCM 加解密、请求签名、滑块组件 | 登录/注册/找回密码、SM2 解密、验证码限频、网关风控与签名过滤、AI Key SM4-GCM 加密 |
| **业务微服务** | 菜单树、组织树、聊天格式化、Cron 预设 | 用户/角色/菜单/配置、工单流转与会签审批、IM 私聊/群聊/撤回 + Redis 广播、RAG 知识检索、微信/支付宝退款状态机、订单流转与乐观锁扣库存 |

### GitHub Actions CI

推送到 `main` / `master` 或创建 PR 时自动触发 `.github/workflows/ci.yml`：

| Job | 环境 | 执行步骤 |
|-----|------|----------|
| **Backend (Maven)** | Ubuntu, JDK 17 | `mvn -B test`（19 个模块全量编译与单测） |
| **PC Frontend** | Ubuntu, Node 24 | `npm ci` → `npm run typecheck` → `npm run test` |
| **Uni-app Mobile** | Ubuntu, Node 24 | `npm ci` → `npm run type-check` → `npm run test` |

---

## 📦 构建与部署

### 构建命令

```powershell
# 1) 后端全量打包（生成 11 个可执行 jar）
mvn clean package -DskipTests

# 2) PC 前端生产构建 → wu-admin-web/dist/
cd wu-admin-web && npm run build

# 3) 移动端 H5 生产构建 → wu-admin-uniapp/dist/build/h5/
cd wu-admin-uniapp && npm run build:h5

# 4) 微信小程序构建 → wu-admin-uniapp/dist/build/mp-weixin/
cd wu-admin-uniapp && npm run build:mp-weixin
```

### 生产部署建议

1. **基础设施先行**：先启动 MySQL 8.0（导入 `sql/01`～`08` 建库脚本）、Redis 7.x 与 Nacos 3.x，并在 Nacos 发布生产环境公共配置。
2. **分层启动服务**：优先启动 `wu-service-system`、`wu-cloud-auth`、`wu-cloud-gateway`，随后启动其余业务微服务与 `wu-admin` BFF。
3. **Nginx 统一反代**：PC 端与移动端静态产物由 Nginx 托管，所有 `/api/**` 与 `/ws/**` 流量统一反向代理至网关 `127.0.0.1:8080`，严禁将业务微服务端口（`9100` / `9200`～`9300`）直接暴露到公网。
4. **安全基线**：生产环境请务必通过环境变量或加密配置覆盖默认数据库密码、Redis 密码、Sa-Token 密钥及国密 SM2/SM4 密钥，并收紧网关 CORS 允许的域名白名单。

---

## 🔗 源码仓库

| 平台 | 仓库地址 |
|------|------|
| 🐙 GitHub | <https://github.com/wushij/wu-admin> |
| 🔴 Gitee | <https://gitee.com/wusj17/wu-admin> |

---

## 📜 开源协议

本项目采用 [Apache-2.0 License](LICENSE) 开源协议。

- 允许免费用于个人学习、学术研究以及商业项目二次开发；
- 衍生修改或二次分发请保留原项目的版权声明、许可条款与免责声明；
- 生产环境部署上线前请务必修改默认密码、会话密钥以及数据库/Redis/Nacos 等敏感配置。
