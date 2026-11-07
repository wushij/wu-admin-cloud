---
name: mysql-design
description: 生产级 MySQL 工程方法论，涵盖数据建模、表与字段设计、约束与索引、SQL 编写与 Review、EXPLAIN 分析、并发与锁、Schema 迁移安全。用于新建业务表、修改表结构、索引设计、慢 SQL 优化、分页与批量查询、死锁排查、数据库层并发问题。纯 Java 业务逻辑请改用 backend-development。
---

# MySQL 高级数据库工程

<Purpose>
本 Skill 用于生产级数据库设计和优化。

目标：

Data Correct
+
Schema Clear
+
Constraint Safe
+
Index Reasonable
+
SQL Efficient
+
Migration Safe
+
Maintainable

核心原则：

**Business Semantics First**（业务语义优先）
**Access Pattern First**（访问模式优先）
**Constraint First**（约束优先）
**Existing Baseline First**（现有基线优先）
**Measure Before Optimize**（先度量再优化）
</Purpose>

<Use_When>
适用于以下数据库任务：

- 新建业务表
- 修改表结构
- 数据模型设计
- 字段设计
- 主键 / 唯一约束设计
- 索引设计
- SQL 编写 / Review
- 慢 SQL 优化
- EXPLAIN 分析
- 分页 / 批量查询
- 数据库锁 / Deadlock
- Schema Migration
- 数据库层并发问题
- N+1 Query 优化
- 重复查询优化
</Use_When>

<Do_Not_Use_When>
跳过完整流程的平凡修改：

- SQL typo
- 字段注释修改
- 简单测试数据修改
- 单纯 Java 业务逻辑（使用 backend-development）
- Frontend
- 部署

业务逻辑、状态机、幂等、分布式一致性优先使用：

`backend-development`
</Do_Not_Use_When>

<Why_This_Exists>
AI 生成的数据库设计常见问题：

- 不检查现有规范就创建表
- 基础字段不统一
- 主键策略混乱
- 状态字段随意设计
- 无约束保护
- 索引随意或缺失
- SQL 无性能意识
- TEXT/JSON 滥用
- 字段类型不合理
- 逻辑删除不一致
- Migration 无安全措施
- 不考虑并发

本 Skill 通过系统化方法论消除这些问题。
</Why_This_Exists>

<Workflow>
非平凡 MySQL 任务默认执行：

1. **Requirement Understanding** — 理解数据需求
2. **Existing Schema Audit** — 审计现有数据库规范
3. **Data Model** — 数据建模
4. **Table / Column Design** — 表和字段设计
5. **Constraint Design** — 约束设计
6. **Access Pattern Analysis** — 访问模式分析
7. **Index / SQL Design** — 索引和 SQL 设计
8. **Concurrency / Lock Review** — 并发和锁审查
9. **Migration Review** — 迁移安全审查
10. **MYSQL READY** — 所有标准满足

禁止：

Requirement
→ CREATE TABLE
→ Finish
</Workflow>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 核心工程规则                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Core_Engineering_Rules>

## 1. Existing Baseline First

**修改数据库前必须先审计现有规范。**

**至少检查：**
- Existing Tables / DDL
- Entity 类
- Mapper / Mapper XML
- Existing SQL
- Existing Index
- **Naming Convention（字段命名约定）**
- **ID Strategy（主键策略）**
- **Status Convention（状态字段约定）**
- **Logical Delete（逻辑删除字段）**
- **Audit Fields（审计字段）**
- **Time Fields（时间字段）**
- **Character Set / Collation**
- **Engine（InnoDB）**

**一个项目内可能存在多套并存的数据库规范。**

例如同一系统下的不同子系统各自维护一套基础字段、主键策略和逻辑删除约定，这是正常现象，不应强行统一。

**审计时必须逐一记录每个模块的实际约定：**
- 基础字段（是否有审计字段、时间字段、备注字段）
- 主键策略（自增 / 分布式 ID / UUID）
- 逻辑删除字段（字段名、类型、取值含义）
- 表名与字段命名风格

**原则：**

新建表必须遵循所在模块的既有规范，而不是套用你熟悉的那一套。

**禁止：**
- 因为"更规范"而修改所在模块的既有字段约定
- 跨模块强推统一字段命名
- 在同一模块内混用多套 ID 策略
- 在同一模块内混用多套逻辑删除字段

**模块一致性 > 跨模块统一。**

## 2. 表基础字段（Mandatory）

**所有业务表必须具备当前所属模块规定的基础字段。**

**新建表必须包含所在模块约定的基础字段：**

```sql
CREATE TABLE xxx (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  -- 业务字段...
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP
                             ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  delete_flag   TINYINT      NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除',
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='xxx表';
```

**以上仅为示意，字段名与类型必须按所在模块的实际约定替换。**

**审计字段的使用场景：**

- 存在人工操作（后台审核、运营修改、管理员操作）
- 需要操作审计
- 需要区分操作主体

如果所在模块的其他表默认带有审计字段，沿用；如果默认没有，不要为单张表机械新增。

**重要区分：**

不要混淆 `user_id` 和 `create_by` / `update_by`：

- `user_id` — **业务归属**（这条数据属于谁）
- `create_by` — **操作主体**（谁创建的这条数据）
- `update_by` — **操作主体**（谁最后修改的这条数据）

**示例：**

```sql
-- 用户订单表
CREATE TABLE `order` (
  id            BIGINT NOT NULL,
  user_id       BIGINT NOT NULL COMMENT '订单所属用户（业务归属）',
  create_by     VARCHAR(64) COMMENT '创建人（操作主体，如客服代下单）',
  ...
);
```

在这个例子中：
- `user_id` = 1001（订单属于用户 1001）
- `create_by` = 'admin'（管理员代客户下单）

两者语义不同，不可混用。

## 3. 主键设计

**主键策略是所在模块的既有约定，先审计再动手。**

**常见策略：**

```sql
-- 自增
id BIGINT NOT NULL AUTO_INCREMENT

-- 分布式 ID（由应用层生成）
id BIGINT NOT NULL COMMENT '主键ID（分布式ID）'
```

**不要改变所在模块的主键策略。** 更换主键策略意味着数据迁移和上下游改造，属于独立的架构任务，不能在业务需求中顺手完成。

**通用原则：**
- 禁止使用业务字段作为主键
- 禁止使用 VARCHAR 主键
- 禁止复合主键（除非明确的关联表）
- 主键必须 NOT NULL
- 主键必须有 COMMENT

## 4. 字段设计

### 类型选择

**整数：**
- TINYINT：状态、标志（0-255）
- INT：一般整数
- BIGINT：ID、大数值、时间戳

**字符串：**
- VARCHAR(N)：变长字符串，N 根据业务合理设置
- CHAR(N)：固定长度（如 del_flag CHAR(1)）

**时间：**
- DATETIME：标准时间类型
- DATETIME(3)：需要毫秒精度时（如 IM 消息）

**禁止：**
- TEXT / LONGTEXT 存储结构化数据
- JSON 类型字段
- VARCHAR 存储 JSON 字符串

**Structured Business Data → Structured Schema**

### 字段约束

**必须明确：**
- NOT NULL 或 DEFAULT NULL
- DEFAULT 值
- COMMENT（必须有注释）

**示例：**
```sql
status        TINYINT      NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=正常',
nickname      VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
source_type   TINYINT      DEFAULT NULL COMMENT '来源：1=后台 2=注册 3=小程序'
```

### 命名约定

**遵循当前模块约定，先看同类表怎么命名：**

- 表名：单数还是复数？是否有统一前缀（如 `sys_`）？
- 字段：下划线命名
- 主键、创建时间、更新时间、删除标志等固定字段的命名

**新表命名必须与所在模块的既有表保持同一风格。**

**通用原则：**
- 小写
- 下划线分隔
- 语义清晰
- 避免保留字

## 5. 状态字段设计

**状态字段必须：**
- 使用 TINYINT
- 定义清晰语义
- COMMENT 中说明所有可能值
- 有合理 DEFAULT

**好的示例：**
```sql
status        TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用 1=正常 2=锁定',
order_status  TINYINT NOT NULL DEFAULT 0 COMMENT '订单状态：0=待支付 1=已支付 2=已发货 3=已完成 4=已取消',
audit_status  TINYINT NOT NULL DEFAULT 0 COMMENT '审核状态：0=待审核 1=审核通过 2=审核拒绝'
```

**避免：**
```sql
-- 反模式：无语义说明
status TINYINT,

-- 反模式：VARCHAR 存储状态
status VARCHAR(20) DEFAULT 'pending'
```

## 6. 逻辑删除

**逻辑删除字段的字段名、类型、取值含义由所在模块约定决定。**

常见形态：

```sql
-- 整型标记
delete_flag TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志：0=未删除 1=已删除'

-- 字符标记
del_flag CHAR(1) DEFAULT '0' COMMENT '删除标志'
```

**审计时确认该模块用的是哪一种，不要凭习惯选择。**

**所有业务表必须有逻辑删除字段。**

**查询时必须带上删除条件**，条件写法与该模块的字段定义一致。

**禁止物理删除业务数据。**

## 7. 唯一约束

**业务唯一性必须通过数据库约束保护。**

**示例：**
```sql
UNIQUE KEY uk_user_id (user_id),
UNIQUE KEY uk_token (token),
UNIQUE KEY uk_phone (phone),
UNIQUE KEY uk_email (email)
```

**复合唯一约束：**
```sql
UNIQUE KEY uk_user_conversation (user_id, conversation_id)
```

**原则：**
- 业务不变量必须有约束
- 不要仅依赖 Java 层校验
- 约束名称清晰（uk_ 前缀表示 unique key）

### 唯一约束与逻辑删除的冲突（必须处理）

逻辑删除的行仍然存在于表中，仍然占用唯一索引。因此**逻辑删除 + 无条件唯一索引会导致删除后无法再次创建同值记录**。

典型故障：用户注销手机号后，同一手机号再也无法注册；订单被逻辑删除后，同一业务单号无法重建。

**三种处理方式，按业务语义选择：**

```sql
-- 方案 A：把删除标记纳入唯一键（未删除行用同一值，已删除行必须互不相同）
UNIQUE KEY uk_phone (phone, delete_flag)
-- 注意：仅适用于 delete_flag 只有 0/1 两种取值、且删除后不再复用的场景。
-- 若同一 phone 可被删除多次，需改用 delete_time 或删除时的唯一序号参与唯一键。

-- 方案 B：唯一键改为「有效数据唯一」，用可空列表达
-- 未删除时 unique_active_key = phone，删除时置 NULL（MySQL 允许 NULL 重复）
UNIQUE KEY uk_phone (unique_active_key)
-- 该列由应用层写入，业务层必须保证与 phone 一致

-- 方案 C：不使用逻辑删除，改用状态字段 + 归档
-- 适用于「删除」本质是状态流转的场景（如订单取消、任务终止）
```

**必须明确回答：**

- 该业务字段是否允许在删除后被重新使用？
- 允许的话，唯一性由什么保证？
- 逻辑删除的行是否会与新建行冲突？

**禁止：** 不检查唯一索引与逻辑删除的关系就直接写 `UNIQUE KEY uk_xxx (xxx)`。

## 8. 外键

**默认不使用数据库外键。**

**原因：**
- 影响性能
- 增加迁移复杂度
- 分库分表困难

**通过以下方式保证引用完整性：**
- Java 业务层校验
- 逻辑删除而非物理删除
- 后台定时对账任务

## 9. 索引设计

### 索引原则

**必须建索引：**
- 主键（自动）
- 唯一约束（自动）
- WHERE 条件高频字段
- JOIN 条件字段
- ORDER BY 字段
- 外键关联字段

**索引命名：**
```sql
PRIMARY KEY (id)                              -- 主键
UNIQUE KEY uk_token (token)                   -- 唯一索引
INDEX idx_user_id (user_id)                   -- 普通索引
INDEX idx_status_create_time (status, create_time)  -- 复合索引
```

### 复合索引

**遵循最左前缀原则：**

```sql
INDEX idx_status_create_time (status, create_time)
```

**可以使用：**
- `WHERE status = 1`
- `WHERE status = 1 AND create_time > xxx`

**不能使用：**
- `WHERE create_time > xxx`（跳过了最左列）

**字段顺序（核心口诀：等值在前、排序紧随、范围在后、高区分度优先）：**
1. **等值查询字段在前**：`WHERE a = ? AND b = ?` 中的等值过滤列放最左侧（其中区分度高的字段优先放前面）。
2. **排序/分组字段紧随等值列之后（排序末尾）**：若存在 `ORDER BY c` 或 `GROUP BY c`，将 `c` 放在所有等值列之后，如 `INDEX idx_a_b_c (a, b, c)`，直接利用索引天然有序性消除 `Using filesort` / `Using temporary`。
3. **范围查询字段放最后**：`>`、`<`、`BETWEEN`、`LIKE 'abc%'` 等范围条件列放在索引末尾（因为 B+ 树中一旦某列使用范围查询，其右侧后续列无法再用于索引等值定位或索引排序）。
4. **范围与排序共存时的取舍**：当同一查询既有范围条件 `b > ?` 又有排序分页 `ORDER BY c DESC LIMIT N` 时，索引 `(a, b, c)` 中的 `c` 无法用于排序。需按数据分布权衡：
   - 高频分页取 Top-N 且范围条件过滤性一般：优先建 `(a, c, b)`，先用索引排好序取前 N 条，再通过索引下推（ICP）过滤 `b`；
   - 范围条件极度精准（过滤后仅剩极少行）：优先建 `(a, b)`，在内存中快速完成少量行的排序。

### 索引覆盖

**如果查询只需要索引字段，可以避免回表：**

```sql
-- 查询只需要 user_id 和 status
INDEX idx_user_id_status (user_id, status)

SELECT user_id, status FROM xxx WHERE user_id = 1;  -- 索引覆盖（Extra: Using index）
```

### 索引注意事项与失效黑名单

**建索引避免：**
- 单表过多索引（一般建议不超过 5~6 个，过多影响写入性能与优化器选错索引）
- 区分度极低的字段单独建索引（如性别、删除标志单列；应作为复合索引的一部分）
- VARCHAR 长字段不加长度直接建全列索引（长文本应使用前缀索引 `INDEX idx_col (col(20))`）

**必须自查的索引失效场景（严禁出现）：**
- **对索引列做函数或四则运算**：如 `WHERE DATE(create_time) = '2024-01-01'`（应改写为范围 `create_time >= '2024-01-01 00:00:00' AND create_time < '2024-01-02 00:00:00'`）、`WHERE amount + 10 > 100`
- **隐式类型转换**：如 `phone` 为 `VARCHAR` 却传数字 `WHERE phone = 13800138000`（相当于对列做 `CAST`，导致全表扫描）
- **隐式字符集/排序规则转换**：两表 `JOIN` 时关联列的字符集（`utf8mb4` vs `utf8`）或 Collation（`utf8mb4_general_ci` vs `utf8mb4_0900_ai_ci`）不一致，导致被驱动表索引失效
- **左模糊或全模糊**：`LIKE '%xxx'` 或 `LIKE '%xxx%'` 无法使用普通 B+ 树索引前缀定位
- **OR 条件未全列覆盖索引**：`WHERE indexed_col = 1 OR unindexed_col = 2` 会导致整条查询退化为全表扫描（必要时拆分为 `UNION ALL`）
- **负向查询**：`!=`、`<>`、`NOT IN`、`NOT LIKE` 往往无法高效使用索引

## 10. SQL 性能

**职责边界：** N+1、缺少分页、循环单条查询等问题由 `backend-development` 第 17 节负责识别。本节只负责识别之后的 SQL 层修复方式。

### 查询编写红线

- **禁止 `SELECT *`**：必须显式写出所需字段。避免拉取无用 `TEXT`/大字段撑爆网络带宽与 InnoDB Sort Buffer，同时为覆盖索引创造条件。
- **存在性判断用 `LIMIT 1` 替代 `COUNT(*)`**：判断某条件是否有记录时，必须写 `SELECT 1 FROM xxx WHERE ... LIMIT 1`，禁止 `SELECT COUNT(*) FROM xxx WHERE ...` 全量扫描统计。
- **小表驱动大表（`IN` vs `EXISTS`）**：子查询结果集小、外表大时用 `IN`；外表小、子查询表极大时用 `EXISTS`（或改写为 `INNER JOIN` 由优化器选择驱动表）。

### N+1 Query 的 SQL 层修复

```sql
-- 反模式：应用层循环单条查询
SELECT id, name, avatar FROM user WHERE id = ?;   -- 执行 N 次

-- 修复 1：改为 JOIN，一次取回
SELECT o.id, o.order_no, u.name, u.avatar
FROM `order` o
JOIN user u ON u.id = o.user_id
WHERE o.status = 1;

-- 修复 2：改为 IN 批量查询，两次固定查询
SELECT id, order_no, user_id FROM `order` WHERE status = 1;
SELECT id, name, avatar FROM user WHERE id IN (?, ?, ...);
```

选择依据：需要跨表过滤或排序时用 JOIN；只是补齐关联字段时用 IN 批量，避免 JOIN 放大结果集。

### 分页

**必须分页：**
```sql
SELECT id, name, create_time FROM user
WHERE status = 1 AND delete_flag = 0
ORDER BY create_time DESC
LIMIT 20 OFFSET 0;
```

**大表深度分页优化（按业务交互二选一）：**
```sql
-- 反模式：OFFSET 过大（扫描并回表丢弃前 100000 行，极慢）
SELECT id, name, create_time FROM user ORDER BY id LIMIT 20 OFFSET 100000;

-- 优化方案 1：游标分页（适用于下拉加载、无跳页场景）
SELECT id, name, create_time FROM user
WHERE status = 1 AND id < 上次最小ID
ORDER BY id DESC LIMIT 20;

-- 优化方案 2：延迟关联 Deferred Join（适用于后台管理表格支持任意跳页场景）
-- 先在覆盖索引中快速定位主键 ID，再与原表 INNER JOIN 回表取 20 行完整数据
SELECT u.id, u.name, u.phone, u.create_time
FROM user u
INNER JOIN (
    SELECT id FROM user
    WHERE status = 1 AND delete_flag = 0
    ORDER BY create_time DESC
    LIMIT 20 OFFSET 100000
) page_ids ON u.id = page_ids.id
ORDER BY u.create_time DESC;
```

### 批量操作

**批量 INSERT：**
```sql
INSERT INTO user (id, name, status) VALUES
(1, 'a', 1),
(2, 'b', 1),
(3, 'c', 1);
```

**批量 UPDATE（谨慎使用）：**
```sql
UPDATE user SET status = 1 WHERE id IN (1, 2, 3, ...前100个);
```

**避免：**
- 一次批量操作过多数据（建议单批 500~1000 条）
- 长事务中大批量操作（导致行锁持有过久与主从延迟）

### EXPLAIN

**慢查询与复杂 SQL 必须经过 EXPLAIN 分析：**
```sql
EXPLAIN SELECT id, name FROM user WHERE status = 1 AND create_time >= '2024-01-01 00:00:00';
```

**关注核心指标与验收红线：**
- **type（访问类型）**：至少达到 `range` / `ref` / `eq_ref` / `const` 级别；**严禁大表出现 `ALL`（全表扫描）**，警惕 `index`（全索引扫描）。
- **key（实际命中索引）**：确认不能为 `NULL`，且符合预期的复合索引最左前缀。
- **key_len（索引使用字节数）**：验证复合索引到底用到了前几列（判断是否因范围条件或类型转换提前截断）。
- **rows（预估扫描行数）**：若扫描行数远大于返回行数，说明索引过滤性差，需调整复合索引列顺序。
- **Extra（附加执行信息）**：
  - 理想状态：`Using index`（覆盖索引）、`Using index condition`（索引下推 ICP）；
  - 必须优化：`Using filesort`（未能利用索引排序，需检查「等值在前、排序紧随」原则）、`Using temporary`（常见于未命中索引的 `GROUP BY` / `DISTINCT`）。

## 11. 并发与锁

### 乐观锁

**使用 Version 字段：**
```sql
version INT NOT NULL DEFAULT 0 COMMENT '版本号（乐观锁）'

-- 更新时
UPDATE user SET balance = balance - 100, version = version + 1
WHERE id = 1 AND version = 当前version;
```

### 悲观锁

**FOR UPDATE：**
```sql
SELECT * FROM user WHERE id = 1 FOR UPDATE;  -- 行锁

UPDATE user SET balance = balance - 100 WHERE id = 1;
```

**注意：**
- 必须在事务中
- 锁的粒度（行锁 vs 表锁）
- 避免长事务持有锁

### 死锁

**常见死锁场景：**
- 多个事务以不同顺序锁定资源
- 间隙锁

**避免：**
- 事务尽量小
- 按相同顺序访问资源
- 使用唯一索引（避免间隙锁）

## 12. Schema Migration

### Migration 安全原则

**必须：**
- 可回滚
- 分阶段执行
- 先加后删
- 避免锁表

**危险操作：**
- ALTER TABLE（大表会锁表）
- DROP COLUMN（不可回滚）
- CHANGE COLUMN（类型变更）

**安全实践：**

**新增字段：**
```sql
-- Phase 1: 新增字段（允许 NULL）
ALTER TABLE user ADD COLUMN new_field VARCHAR(64) DEFAULT NULL;

-- Phase 2: 数据迁移
UPDATE user SET new_field = xxx;

-- Phase 3: 改为 NOT NULL（如需要）
ALTER TABLE user MODIFY new_field VARCHAR(64) NOT NULL;
```

**删除字段：**
```sql
-- Phase 1: 停止使用
-- Phase 2: 等待观察期
-- Phase 3: 真正删除
ALTER TABLE user DROP COLUMN old_field;
```

**字段重命名：**
```sql
-- 不要用 RENAME
-- Phase 1: 新增字段
ALTER TABLE user ADD COLUMN new_name VARCHAR(64);

-- Phase 2: 数据迁移
UPDATE user SET new_name = old_name;

-- Phase 3: 切换代码
-- Phase 4: 删除旧字段
ALTER TABLE user DROP COLUMN old_name;
```

## 13. 字符集

**统一使用：**
```sql
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
```

**原则：**
- 统一使用 utf8mb4（支持 emoji）
- 不要使用 utf8（实际是 utf8mb3，不完整）

## 14. 引擎

**统一使用：**
```sql
ENGINE=InnoDB
```

**不要使用：**
- MyISAM（不支持事务）
- MEMORY（数据易丢失）

## 15. 表注释

**所有表必须有清晰注释：**
```sql
CREATE TABLE user (
  ...
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

## 16. 与 backend-development 协作

**mysql-design 负责：**
- Data Model / Table Design
- Column / Data Type
- Index / Constraint
- DDL
- EXPLAIN / Execution Plan
- SQL 深度优化
- Query Rewrite
- Index Strategy

**backend-development 负责识别（然后由 mysql-design 优化）：**
- N+1 Query
- 缺少分页
- 批量访问机会
- 重复查询
- 明显 SQL 性能风险

**协作场景：**

```
backend-development 识别问题（N+1/重复查询）
  ↓
mysql-design 深度优化（Index/EXPLAIN/Rewrite）
```

</Core_Engineering_Rules>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- MySQL Review                                                                 -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<MySQL_Review>

DDL / SQL 完成后重新 Review：

**检查维度：**
- Existing Baseline Respected（遵循现有规范）
- Base Fields Complete（基础字段完整）
- Primary Key Correct（主键正确）
- Logical Delete Correct（逻辑删除正确）
- Logical Delete / Unique Constraint Conflict Resolved（逻辑删除与唯一约束的冲突已处理）
- Constraint Sufficient（约束充分）
- Index Reasonable（索引合理）
- Type Appropriate（类型合理）
- Comment Clear（注释清晰）
- Naming Consistent（命名一致）
- Migration Safe（迁移安全）
- SQL Efficient（SQL 高效）
- No JSON Fields（无 JSON 字段）
- No TEXT for Structured Data（无 TEXT 存储结构化数据）

**问题分级：**

**P0（Critical）：**
- 破坏现有规范
- 缺少基础字段
- 主键错误
- 无逻辑删除
- 逻辑删除与唯一约束冲突未处理
- 关键业务无约束
- 明显 SQL 性能风险

**P1（Major）：**
- 索引缺失
- 类型不合理
- 命名不一致
- 注释缺失
- Migration 不安全

**P2（Minor）：**
- 索引可优化
- 注释可完善
- 命名可改进

**P0 / P1 必须修复。**

</MySQL_Review>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- READY 标准                                                                   -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<MYSQL_READY_Definition>

只有同时满足以下条件才能 MYSQL READY：

✅ **Existing Baseline Respected** — 遵循现有规范

✅ **Base Fields Complete** — 基础字段完整

✅ **Primary Key Correct** — 主键设计正确

✅ **Logical Delete Correct** — 逻辑删除正确

✅ **Unique Constraint Consistent with Logical Delete** — 唯一约束与逻辑删除的冲突已处理，删除后不会阻塞新记录

✅ **Constraint Sufficient** — 业务约束充分

✅ **Index Reasonable** — 索引合理

✅ **Type Appropriate** — 字段类型合理

✅ **Comment Clear** — 所有表和字段有清晰注释

✅ **Naming Consistent** — 命名遵循模块约定

✅ **Character Set Correct** — utf8mb4

✅ **Engine Correct** — InnoDB

✅ **No JSON Fields** — 无 JSON 类型字段

✅ **No TEXT for Structured Data** — 无 TEXT 存储结构化数据

✅ **SQL Reviewed** — SQL 经过性能审查

✅ **Migration Safe** — Schema 变更安全

✅ **No P0** — 无 P0 问题

✅ **No P1** — 无 P1 问题

</MYSQL_READY_Definition>

<!-- ═══════════════════════════════════════════════════════════════════════════ -->
<!-- 最终注释                                                                     -->
<!-- ═══════════════════════════════════════════════════════════════════════════ -->

<Final_Notes>

## 本 Skill 的定位

**mysql-design 负责：**

数据库设计和优化（Data Model & Query Design）

**不负责：**

业务逻辑（backend-development）
前端实现（senior-frontend）
UI 设计（ui-design）

## 适用边界

**本 Skill 覆盖：** 数据建模、表与字段设计、约束与索引、SQL 编写与优化、EXPLAIN 分析、并发与锁、Schema 迁移安全。

**本 Skill 不覆盖：** Java 业务逻辑（使用 `backend-development`）、前端实现（使用 `senior-frontend`）、视觉设计（使用 `ui-design`）。

**边界提醒：** 不擅自统一多个模块或数据库之间不一致的字段命名。一个系统可能连接多个数据库，或包含多套互不相同的表规范（模块 A → 数据库 A / 模块 B → 数据库 B）。两套规范共存是正常的，模块一致性优先。改造其中一套使其与另一套统一，属于独立的架构任务，必须有明确指令才能执行。

## 兼容性

本 Skill 核心方法论保持项目无关。

不绑定任何具体项目、框架版本或组织内部的命名约定。

</Final_Notes>
