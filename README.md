# Wu Admin Cloud 🚀

基于 **Spring Cloud 2025 + Spring Boot 3.5 + Nacos + Sa-Token** 的企业级微服务后台管理系统，配套 **Vue 3 PC 端（wu-admin-web）** 与 **uni-app 移动端（wu-admin-uniapp，H5 / 微信小程序）**。

工程包含 **11 个独立可运行微服务/应用**（1 个 Gateway 网关 + 1 个 Auth 认证中心 + 1 个 BFF 聚合层 + 8 个域微服务）与 **3 个公共基础设施组件库**，统一经 `wu-cloud-gateway` 路由分发，覆盖 RBAC 权限、工单审批、企业 IM、AI 智能助手、文件存储、支付/订单、系统监控、代码生成等场景。

---

## 目录

- [🌟 功能特性](#-功能特性)
- [🛠️ 技术栈](#️-技术栈)
- [🏗️ 系统架构](#️-系统架构)
- [🧩 微服务与应用清单](#-微服务与应用清单)
- [🚀 快速开始](#-快速开始)
- [📁 目录结构](#-目录结构)
- [⚙️ 配置说明](#️-配置说明)
- [💡 开发指南](#-开发指南)
- [🧪 测试与 CI](#-测试与-ci)
- [📦 构建与部署](#-构建与部署)

---

## 🌟 功能特性

### ⚡ 微服务架构亮点

- **🧩 11 个独立 JVM 应用**：1 网关 + 1 认证中心 + 1 BFF + 8 业务微服务，可独立部署与扩缩容
- **🚪 统一网关入口**：`wu-cloud-gateway` (:8080) 按 Path 前缀分发，统一鉴权、Header 清洗、接口安全
- **📡 Nacos 服务注册发现**：注册至命名空间 `dev` / 分组 `WU_CLOUD`，网关通过 `lb://` 负载均衡
- **🔐 Sa-Token 共享会话**：Gateway / Auth / 业务服务共用同一 Redis（db=0）存 Token，网关鉴权后注入 `X-User-*` Header
- **🔗 OpenFeign 跨服务调用**：契约集中在 `wu-cloud-api`；内部接口前缀为 `/inner/**`，Feign 直连下游（不经网关）；外部经网关访问 `/inner/**` 会被拦截返回 403
- **🗄️ 按域拆分 SQL**：库名统一 `wu-admin`；8 个带 JDBC 的业务服务各有脚本（`01`～`08`）；gateway / auth / BFF 无业务库
- **🔄 渐进式一致性**：无 MQ 阶段采用「Feign 同步 + 本地事务 + Quartz / Outbox 补偿」

### 🛡️ 安全体系

- **🛡️ 网关统一鉴权**：Sa-Token 在 Gateway 校验登录态，清洗伪造的 `X-User-*` Header
- **🚫 内部接口保护**：网关 `InnerApiGuardFilter` 拦截入站 `/inner/**`；服务间 Feign 走 `lb://` 直连，不经网关
- **🔑 全链路接口安全**（DB `security` 组动态开关）：HMAC-SM3 请求签名、SM4 请求/响应加解密、时间戳窗口、Nonce 防重放；会话密钥由 `/api/auth/session-sign-init` 按 `X-Client-Id` 下发到 Redis
- **🔐 敏感字段加密**：AI 模型 API Key 等入库前 SM4 加密，回显掩码
- **🌐 CORS 凭证**：网关配置凭证模式 + Origin 白名单

### 💼 业务能力

- **👥 五维组织 + 数据权限**：用户 / 角色 / 菜单 / 部门 / 岗位，数据范围动态拼接
- **📋 工单与审批流**：请假 / 采购 / 报销 / 用印 / 合同 / 通用 + 注册审核
- **💬 企业 IM**：WebSocket 私聊 / 群聊、@提醒、撤回、免打扰、在线状态、正在输入
- **🤖 AI 助手**：SSE 流式对话（DeepSeek / OpenAI / Qwen / Kimi）、模型配置、对话日志、知识库
- **📁 文件存储**：本地存储适配，上传 / 下载 / 预览 / 回收站
- **💳 支付与订单**：微信 Native / 支付宝当面付、回调验签、订单与支付事件解耦

---

## 🛠️ 技术栈

- **后端（微服务）**：Spring Boot **3.5.13** · Spring Cloud **2025.0.2** · Spring Cloud Alibaba **2025.0.0.0** · Nacos 2.3.x · Spring Cloud Gateway · OpenFeign · Sa-Token **1.39.0** · MyBatis Plus 3.5.9 · Druid 1.2.24 · Redis + Redisson 3.41.0 · Hutool 5.8.42 · BouncyCastle 1.78.1 (国密) · Velocity 2.4.1 (代码生成) · Sentinel
- **前端（PC）**：Vue 3.4 + TypeScript 5.3 + Pinia + Element Plus + Vite 5 + ECharts
- **移动端**：uni-app (Vue 3) + uv-ui + luch-request + TypeScript
- **运行时环境**：JDK **17** · MySQL **8.0** · Redis **7.x** · Nacos 2.3.x · Node.js 18+ · Maven 3.6+

---

## 🏗️ 系统架构

```text
┌────────────────┐            ┌──────────────────────┐
│  PC / H5 / 小程序│  HTTPS    │       Nginx           │
│ wu-admin-web    │ ────────►  │ 反向代理 / 静态资源    │
│ wu-admin-uniapp │            │ /api → 8080, /ws → WS │
└────────────────┘            └──────────┬───────────┘
                                         │ /api, /ws
                                         ▼
                          ┌──────────────────────────┐
                          │   wu-cloud-gateway :8080  │
                          │  路由 + Sa-Token 鉴权      │
                          │  ApiSecurity(签名/SM4)    │
                          │  /inner/** → 403          │
                          │  X-User-* Header 透传     │
                          └──────────┬───────────────┘
                                     │ lb:// (Nacos)
    ┌────────────┬─────────────┬─────┴──────┬────────────┬─────────────┐
    ▼            ▼             ▼            ▼            ▼             ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ auth:9200│ │system:9201│ │ticket:9202│ │message:9203│ │ ai:9204  │ │ pay:9205 │
│  (Redis) │ │ wu-admin │ │ wu-admin │ │ wu-admin │ │ wu-admin │ │ wu-admin │
└──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘
    ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
    │order:9206│ │ file:9207│ │infra:9300│ │admin:9100│
    │ wu-admin │ │ wu-admin │ │ wu-admin │ │ (BFF聚合) │
    └──────────┘ └──────────┘ └──────────┘ └──────────┘
                                 │
                      ┌──────────┴───────────┐
                      │   MySQL 8.0 + Redis  │
                      │ 库名 wu-admin / db=0 │
                      └──────────────────────┘
```

| 维度 | 说明 |
|------|------|
| **🔀 网关路由顺序** | 兼容重写（monitor/AI 等）→ `/api/auth` → `/api/admin` → `/api/system/dashboard` → ticket/approval/notice → AI → `/api/system/**` → file-group → `/api/infra/**` → message / pay / order / file 等 |
| **📡 跨服务调用** | OpenFeign（契约在 `wu-cloud-api`），路径约定 `/inner/{域}/**`；强一致走 Feign + 本地事务，弱一致走补偿任务 |
| **🔑 鉴权链** | Gateway SaReactorFilter 校验 → `AuthGlobalFilter` 注入 `X-User-*` → 业务侧 `UserContextInterceptor` 还原上下文；部分接口仍可用 `StpUtil`（共享 Redis Token） |
| **🔌 WebSocket** | `/ws/**`、`/api/ws/**` 路由至 `wu-service-message`，网关 Upgrade 透传；多实例经 Redis Pub/Sub 广播 |

---

## 🧩 微服务与应用清单

### 🖥️ 核心应用与服务 (11 个)

| 模块名称 | 类型 | 端口 | 数据库 | 核心职责 |
|---------|------|------|--------|----------|
| `wu-cloud-gateway` | 🚪 网关入口 | 8080 | -（需 Redis） | 统一 API 入口、路由分发、Sa-Token 鉴权、接口安全、Header 清洗 |
| `wu-cloud-auth` | 🔑 认证中心 | 9200 | Redis | 登录/登出/验证码/注册/找回密码/session-sign-init（Feign 调 system） |
| `wu-admin` | 📊 BFF 聚合 | 9100 | -（需 Redis） | 仪表盘等跨服务聚合 |
| `wu-service-system` | ⚙️ 业务服务 | 9201 | `wu-admin` | 用户/角色/菜单/部门/岗位/字典/配置/登录日志/统计 |
| `wu-service-ticket` | 📋 业务服务 | 9202 | `wu-admin` | 工单流转、审批流引擎、站内通知 |
| `wu-service-message` | 💬 业务服务 | 9203 | `wu-admin` | WebSocket IM、私聊/群聊/@提醒/撤回/在线状态 |
| `wu-service-ai` | 🤖 业务服务 | 9204 | `wu-admin` | SSE 流式对话、模型配置、对话日志、知识库 |
| `wu-service-pay` | 💳 业务服务 | 9205 | `wu-admin` | 微信/支付宝接入、回调验签、支付状态 |
| `wu-service-order` | 📦 业务服务 | 9206 | `wu-admin` | 下单/支付结果关联/退款（与 pay 事件解耦） |
| `wu-service-file` | 📁 业务服务 | 9207 | `wu-admin` | 文件上传/下载/预览/回收站、本地存储适配 |
| `wu-service-infra` | 🛠️ 业务服务 | 9300 | `wu-admin` | 访问日志/操作日志/代码生成/定时任务/监控/导出 |

### 📦 基础组件与库 (3 个)

| 模块名称 | 类型 | 说明 |
|---------|------|------|
| `wu-cloud-common` | 🧰 公共底层包 | `R<T>`、`PageResult`、全局异常、`UserContextHolder`、国密工具、`DynamicConfigHelper` |
| `wu-cloud-api` | 🤝 Feign 契约包 | OpenFeign 远程接口契约与共享 DTO/VO |
| `wu-cloud-infra` | 🏗️ Starter 组件库 | `wu-cloud-starter-redis` / `monitor` / `codegen` |

---

## 🚀 快速开始

### ⚙️ 环境要求

| 软件 | 版本 | 说明 |
|------|------|------|
| JDK | 17 | 后端开发 |
| Maven | 3.6+ | 后端构建 |
| MySQL | 8.0 | 库名 **`wu-admin`** |
| Redis | 7.x | **database 0**（gateway / auth / 业务服务必须共用，否则 Token 不互认） |
| Nacos | 2.3.x | `:8848`，命名空间 `dev`，分组 `WU_CLOUD` |
| Node.js | 18+ | 前端 & uni-app |

### 1. 🗄️ 初始化数据库

库名统一 **`wu-admin`**。推荐用脚本一键导入（共享库模式）：

```powershell
# Windows（默认账号 root/root，可用参数覆盖）
.\scripts\init-databases.ps1
.\scripts\init-databases.ps1 -UseBaseline

# Linux/macOS
./scripts/init-databases.sh
./scripts/init-databases.sh --baseline
```

也可手动导入：

```bash
# 方式 A：按服务拆分脚本
mysql -u root -p < sql/01_wu_system.sql   # ← wu-service-system
mysql -u root -p < sql/02_wu_file.sql     # ← wu-service-file
mysql -u root -p < sql/03_wu_ticket.sql   # ← wu-service-ticket
mysql -u root -p < sql/04_wu_ai.sql       # ← wu-service-ai
mysql -u root -p < sql/05_wu_message.sql  # ← wu-service-message
mysql -u root -p < sql/06_wu_infra.sql    # ← wu-service-infra
mysql -u root -p < sql/07_wu_pay.sql      # ← wu-service-pay
mysql -u root -p < sql/08_wu_order.sql    # ← wu-service-order

# 方式 B：单体全量基线（本地最快）
mysql -u root -p < sql/admin_platform.sql
```

> gateway / auth / BFF 不建业务库。多机「一服务一库」部署时，可将各脚本部署到不同 MySQL 实例并改 JDBC；本地默认仍是**同一库 `wu-admin`**。

### 2. 📡 启动基础设施 (Nacos & Redis)

1. **Nacos**：启动在 `8848` 端口，在控制台创建命名空间 `dev`
2. **Redis**：启动在 `6379` 端口，使用 db `0`

### 3. ☕ 编译与启动微服务

```powershell
# 1) 全量编译（改 common/api 后也必须 install）
mvn clean install -DskipTests

# 2) 推荐：用 .vscode/launch.json 一键启动
#    「一键启动后端全量微服务 (10大服务)」
#    或按 01 网关 → 02 auth → 03 system … 逐个启动

# 3) 也可命令行启动（示例）
mvn -pl wu-cloud-gateway spring-boot:run -DskipTests
mvn -pl wu-cloud-auth spring-boot:run -DskipTests
mvn -pl wu-cloud-service/wu-service-system spring-boot:run -DskipTests
# … ticket / message / ai / file / infra / pay / order …
mvn -pl wu-admin spring-boot:run -DskipTests
```

建议启动顺序：**Nacos / Redis → gateway(01) → auth(02) → system(03) → 其余业务 → BFF(02B)**。

### 4. 💻 启动前端

```powershell
# PC 端（http://localhost:3000，/api 代理到网关 :8080）
cd wu-admin-web
npm install
npm run dev

# 移动端 H5（http://localhost:5174，/api 代理到网关 :8080）
cd wu-admin-uniapp
npm install
npm run dev:h5
```

### 🔐 默认登录凭证

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin123` | 超级管理员 |
| `zhangsan` | `admin123` | 普通用户（与脚本中 bcrypt 一致） |

---

## 📁 目录结构

```text
wu-admin-cloud/                       # 工程根目录
├── pom.xml                          # 父 POM
├── .vscode/launch.json              # 本地一键启动配置
├── .github/workflows/ci.yml         # CI 门禁
├── wu-cloud-common/                 # 公共底层基础库
├── wu-cloud-api/                    # 跨服务 Feign 契约与共享 DTO
├── wu-cloud-infra/                  # 基础能力 Starter
│   ├── wu-cloud-starter-redis/      # Redis & Redisson
│   ├── wu-cloud-starter-monitor/    # 监控指标
│   └── wu-cloud-starter-codegen/    # 代码生成器引擎
├── wu-cloud-gateway/                # [8080] 🚪 网关中心
├── wu-cloud-auth/                   # [9200] 🔑 认证中心
├── wu-admin/                        # [9100] 📊 BFF 聚合服务
├── wu-cloud-service/                # 业务微服务集合
│   ├── wu-service-system/          # [9201] ⚙️ 系统 → sql/01
│   ├── wu-service-file/            # [9207] 📁 文件 → sql/02
│   ├── wu-service-ticket/          # [9202] 📋 工单 → sql/03
│   ├── wu-service-ai/              # [9204] 🤖 AI → sql/04
│   ├── wu-service-message/         # [9203] 💬 消息IM → sql/05
│   ├── wu-service-infra/           # [9300] 🛠️ 基础设施 → sql/06
│   ├── wu-service-pay/             # [9205] 💳 支付 → sql/07
│   └── wu-service-order/           # [9206] 📦 订单 → sql/08
├── wu-admin-web/                    # PC 前端 (Vue 3 + Vite)
├── wu-admin-uniapp/                 # 移动端 (uni-app)
└── sql/                             # 库名 wu-admin；01～08 + admin_platform.sql
```

---

## ⚙️ 配置说明

### 📋 配置清单（P2）

| 配置项 | 存放位置 | 说明 |
|--------|----------|------|
| 端口、`spring.application.name`、Nacos 地址 | 各服务本地 `application.yml` | L0，可提交 |
| Redis、Sa-Token 公共段 | Nacos `wu-cloud-common.yaml`（Group=`WU_CLOUD`）+ 本地兜底 | L1；样例见 `docs/nacos/` |
| 服务私有开关 / 文件路径等 | Nacos `{app}.yaml`（可选）+ 本地 | L2 |
| site / login / security / ai | DB `sys_config_group` | L3，动态业务 |
| JDBC 密码、支付/AI 密钥 | 环境变量或加密配置；**勿明文进 Git/Nacos 公共库** | — |
| Gateway 路由表 | 本地 `wu-cloud-gateway/.../application.yml` | 暂不外置，防误改 |

发布公共配置：`.\scripts\nacos-publish-common.ps1`（Nacos **3.x** 控制台默认 `:8849`，脚本会登录后写入；客户端仍连 `:8848` 经 gRPC 拉配置）或见 `docs/nacos/README.md`。

### 📡 Nacos / 注册与配置中心

各服务统一：

```yaml
spring.cloud.nacos.discovery:
  server-addr: 127.0.0.1:8848
  namespace: dev
  group: WU_CLOUD
  ip: 127.0.0.1   # 本地避免注册到错误网卡 / 代理假 IP

spring.config.import:
  - optional:nacos:wu-cloud-common.yaml?group=WU_CLOUD&refreshEnabled=true
  - optional:nacos:${spring.application.name}.yaml?group=WU_CLOUD&refreshEnabled=true
```

`optional:`：未发布 DataId 或 Config 短暂不可用时仍可启动（依赖本地 YAML 兜底）。`import-check.enabled: false` 避免强依赖私有 DataId。

### 🔴 Redis（必须一致）

gateway、auth、BFF、业务服务均指向同一 Redis `127.0.0.1:6379`、`database: 0`，否则登录态无法跨服务识别。优先以 Nacos 公共配置为准，本地 YAML 为降级。

### 🔑 Sa-Token

各服务 `sa-token` 配置需一致（`token-name: Authorization`、timeout、share 等）。网关 `is-read-header: true`，从 Header 读 Token（网关特有项仍在本地）。

### 🎛️ 动态配置

业务配置存在表 `sys_config_group`（如 `security` / `login` / `site` / `ai`）。  
有 JDBC 的服务通过 `DynamicConfigHelper` 直读；auth / gateway 通过 Feign 或 WebClient 拉取 system 内部接口。

接口安全开关示例（`security` 组）：

| 字段 | 含义 |
|------|------|
| `sm4EncryptEnabled` | SM4 请求/响应加密 |
| `sm2SignEnabled` | HMAC-SM3 签名（历史命名） |
| `timestampEnabled` / `nonceEnabled` | 时间戳 / 防重放 |

网关 `gateway.api-security.enabled` 默认为 `false`，是否强制验签/解密由 DB `security` 组动态决定；若请求已带 `X-Encrypted: 1`，网关会解密后再转发下游。

### 📁 文件本地路径

file 服务上传根目录默认本模块 `data/file-upload`；可用环境变量 `FILE_LOCAL_BASE_DIR` 覆盖（`.vscode/launch.json` 与 `scripts/start-services.ps1` 均指向该目录）。

---

## 💡 开发指南

### 📦 改公共模块后必装

修改 `wu-cloud-common` 或 `wu-cloud-api` 后，先执行：

```powershell
mvn install -pl wu-cloud-common,wu-cloud-api -DskipTests
```

再重启依赖它们的服务（launch 中的 `00-安装公共模块`）。

### 🖥️ 本地调试入口

| 端 | 地址 | API |
|----|------|-----|
| 网关 | http://localhost:8080 | 直接访问 `/api/**` |
| PC | http://localhost:3000 | Vite 代理 `/api` → 8080 |
| H5 | http://localhost:5174 | Vite 代理 `/api` → 8080 |

### 🔗 内部接口约定

- 对外：`/api/{域}/**`（经网关）
- 对内：`/inner/{域}/**`（仅 Feign / 服务间；网关入站 403）
- 契约：`wu-cloud-api` 中 `RemoteXxxService`

### ❓ 常见问题

| 现象 | 排查 |
|------|------|
| 全站 401 | gateway 与 auth 是否同一 Redis；Token Header 名是否为 `Authorization` |
| 聊天发送 500「系统繁忙」、日志 JsonParseException（hex 密文） | 网关未解密 SM4 body；确认 gateway 已启动且 `ApiSecurityFilter` 生效，前端已完成 `session-sign-init` |
| Feign 503 / No servers | 目标服务未注册 Nacos；检查 namespace/group/ip |
| 改了 DB 安全开关不生效 | auth/gateway 配置缓存约 60s，或重启对应服务 |

---

## 🧪 测试与 CI

设计采用 Mockito 单测 + GitHub Actions CI 门禁。

```powershell
# 后端单测（无需 MySQL/Redis/Nacos）
mvn -B test

# PC 前端
cd wu-admin-web
npm ci
npm run lint
npm run typecheck
npm run test

# 移动端 uni-app
cd ../wu-admin-uniapp
npm ci
npm run type-check
npm run test

# 本地冒烟（需已启动基础设施与微服务）
.\scripts\smoke\smoke-test.ps1
```

CI：`.github/workflows/ci.yml`，在 `main` 的 push/PR 上跑 Backend `mvn test` + PC Frontend typecheck/test + Uni-app type-check/test。

---

## 📦 构建与部署

### ☕ 打包

```powershell
mvn clean package -DskipTests
```

各可运行模块产物在对应 `target/*.jar`（gateway / auth / admin / 各 `wu-service-*`）。

### ▶️ 运行示例

```powershell
java -jar wu-cloud-gateway/target/wu-cloud-gateway-1.0.0.jar
java -jar wu-cloud-auth/target/wu-cloud-auth-1.0.0.jar
# …
```

生产建议：

1. 先起 Nacos、Redis、MySQL
2. 再起 gateway、auth、system，最后其余服务与 BFF
3. 前端 `npm run build` 后由 Nginx 托管静态资源，`/api`、`/ws` 反代到网关 `:8080`
4. 按环境覆盖 JDBC / Redis / Nacos 地址；勿将含密钥的配置提交仓库

### 💻 前端构建

```powershell
cd wu-admin-web && npm run build
cd wu-admin-uniapp && npm run build:h5   # 或对应小程序构建命令
```

---

## 📜 许可证

本项目仅供学习与内部使用。
