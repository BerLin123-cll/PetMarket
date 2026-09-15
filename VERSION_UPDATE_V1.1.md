# PetMarket 萌宠商城 —— 版本更新说明

> 本文档记录本轮（V1.1）的功能升级、改动清单、配置说明与验收结果。
> 编写时间：2026-09-10
> 适用代码基线：`com.pet:PetMarket:0.0.1-SNAPSHOT`（Spring Boot 3.5.16 / Java 17 / Vue 3 + Element Plus）

---

## 一、本轮版本总览

上一版已具备基础商城闭环（商品、购物车、地址、下单、模拟支付、后台管理）。
本轮按「**先补可上线能力，再扩展宠物特色功能**」的思路，完成了 **3 个 P0 上线阻塞项** + **2 项配套能力**。

| 优先级 | 升级方向 | 状态 | 核心交付 |
| --- | --- | --- | --- |
| P0 | 管理端权限校验 | ✅ 已完成 | 新增管理员角色守卫拦截器，普通用户 Token 访问 `/admin/**` 一律 403 |
| P0 | 密钥与部署配置外置 | ✅ 已完成 | 数据库/ JWT 密钥全部改为环境变量，新增 prod profile 并关闭 SQL debug 日志 |
| P0 | 库存与订单一致性 | ✅ 已完成 | 条件扣减（原子防超卖）+ 库存回补幂等 + 超时未支付自动取消释放库存 |
| P1 | 本地商品图片上传 | ✅ 已完成 | 后端上传接口 + 磁盘保存 + 静态访问，前端商品管理页接入上传组件 |
| — | Git 忽略规则 | ✅ 已完成 | 新增 `.gitignore` 并清理误入索引的 `node_modules` / `target` |

**验收结论（自行验收）**：后端 34 个自动化测试全部通过（`Tests run: 34, Failures: 0, Errors: 0`），
前端 `vite build` 构建成功，并在真实 HTTP 链路上完成权限矩阵与图片上传的端到端验证。详见 [第六节](#六验收记录)。

---

## 二、P0-1 管理端权限校验

### 问题
- `AuthInterceptor` 只解析 `isAdmin` 放进请求属性，**从不拒绝**普通用户；
- `SecurityConfig` 对所有请求 `permitAll()`；
- 结果：**任意一个有效的普通用户 JWT 即可调用 `/admin/**` 后台接口**，属于严重越权漏洞。

### 修复方案
新增 `AdminGuardInterceptor`（管理端角色守卫），在 `AuthInterceptor` 之后执行，四重校验：

1. 缺少 Token → **401**；
2. Token 签名非法 / 已过期 → **401**；
3. Token 中 `isAdmin` 声明不为 `true` → **403**（本次修复的核心）；
4. Token 对应的管理员账号在库中已不存在（被删除）→ **403**，旧 Token 立即失效。

拒绝时**同时设置 HTTP 状态码与统一 JSON 响应体**，前端 axios 才能正确走 401/403 分支。

### 顺带修复的两个隐藏缺陷
| 缺陷 | 现象 | 修复 |
| --- | --- | --- |
| `AuthInterceptor` 拒绝时只写响应体、不设状态码 | HTTP 状态仍是 **200**，前端 401 分支永不触发，用户看到的是"操作失败"而不是跳登录页 | 改为 `response.setStatus(401)` + 统一 JSON |
| `SecurityConfig` 曾写成 `/admin/**` → `authenticated()` | 本项目没有 Spring Security 认证过滤器，匿名请求永远无法满足该条件，**管理员请求也会被 401 拒掉** | 明确修为 `permitAll()`，角色校验统一交给拦截器，并在注释中写明原因 |

### 关键文件
- 新增 `src/main/java/com/pet/petmarket/config/AdminGuardInterceptor.java`
- 修改 `config/WebConfig.java`（按 order 注册两级拦截器：登录态 → 角色）
- 修改 `config/AuthInterceptor.java`（状态码 401 + 常量 `ATTR_USER_ID` / `ATTR_IS_ADMIN`）
- 修改 `config/SecurityConfig.java`（关闭表单登录/Basic，统一 401/403 JSON 出口）
- 修改 `utils/JwtUtils.java`（新增 `resolveToken(request)` 统一解析 `Authorization: Bearer xxx`）

---

## 三、P0-2 密钥与部署配置外置

### 问题
数据库账号密码、JWT 密钥硬编码在 `application.yml` 中；SQL 明细日志（`StdOutImpl` + `mapper: debug`）默认开启，发布即泄露 SQL 与参数。

### 修复方案
1. **全部敏感配置改为环境变量占位符**（保留开发默认值，保证开箱即跑）：

   | 配置项 | 环境变量 | 说明 |
   | --- | --- | --- |
   | `spring.datasource.url` | `DB_URL` | 生产必须指向真实库 |
   | `spring.datasource.username` | `DB_USERNAME` | |
   | `spring.datasource.password` | `DB_PASSWORD` | **必须在发布前替换** |
   | `jwt.secret` | `JWT_SECRET` | **必须在发布前轮换**，≥32 字节 |
   | `server.port` | `SERVER_PORT` | |
   | `petmarket.upload.dir` | `UPLOAD_DIR` | 本地上传目录 |

2. **SQL 明细日志默认关闭**：`log-impl` 挪到 `dev` profile，只有显式启用 `--spring.profiles.active=dev` 才打印 SQL。

   > dev profile 使用 `org.apache.ibatis.logging.slf4j.Slf4jImpl`（**不是** `StdOutImpl`）。
   > `StdOutImpl` 直接写 `System.out` 且不带字符编码，会把 utf8mb4 的 `TEXT` 列（如商品 `description`）
   > 显示成 `<<BLOB>>`，中文日志也容易乱码；`Slf4jImpl` 走 logback 的 UTF-8 编码器，SQL / 参数 / 结果都正常显示。
3. **新增生产 profile** `application-prod.yml`：
   - `log-impl: NoLoggingImpl` 彻底关闭 SQL 日志；
   - `com.pet.petmarket.mapper: warn`，日志级别收敛为 `info` / `warn`；
   - 错误页不输出 message / stacktrace，日志按 20MB 滚动、保留 30 天。
4. **新增启动配置校验** `StartupConfigValidator`，生产环境**快速失败**：
   - `JWT_SECRET` 未配置或不足 32 字节 → 阻断启动；
   - 生产环境仍使用代码内置默认 JWT 密钥 → 阻断启动；
   - 生产环境仍使用默认数据库密码 `123456` → 阻断启动。
5. **新增 `.env.example`**：列出全部环境变量、生成密钥的命令与三种操作系统/IDEA 的注入方式。

### 发布前必须做的事（重要）
- [ ] 轮换 `JWT_SECRET`（旧密钥签发过的 Token 会全部失效，这正是轮换的意义）；
- [ ] 修改数据库账号密码，并使用**最小权限账号**（不要用 root）；
- [ ] 启动参数加 `--spring.profiles.active=prod`；
- [ ] 确认 `.env`、`application-local.yml` 未被提交（`.gitignore` 已忽略）。

> 生成密钥示例：`openssl rand -base64 48`（Linux/macOS）或
> `[Convert]::ToBase64String((1..48 | ForEach-Object { Get-Random -Max 256 }))`（PowerShell）

---

## 四、P0-3 库存与订单一致性

### 问题
原实现是「先 `getById` 查库存 → Java 里判断 → `updateById` 写回」，并发下两个请求可能同时通过判断，造成**超卖**；且没有超时自动取消，未支付订单会一直占用库存。

### 修复方案
**1｜下单：条件扣减（原子操作，防超卖）**

```sql
-- ProductMapper#deductStock
UPDATE product SET stock = stock - #{quantity}, update_time = NOW()
WHERE id = #{productId} AND stock >= #{quantity}
```

把"判断库存是否充足"和"扣减库存"合并成**一条原子 SQL**，由数据库行锁保证并发安全：
返回 1 表示扣减成功，返回 0 表示库存不足 → 立即抛异常触发**整单事务回滚**，
不会出现"扣了 A 没扣 B"的中间态。

**2｜库存预占与支付截止时间**
订单新增字段 `pay_expire_time`（支付截止时间 = 下单时间 + `pay-timeout-minutes`，默认 30 分钟），
下单即预占库存，前端可用它展示支付倒计时。

**3｜取消 / 释放：状态机 CAS + 幂等回补**

```sql
-- OrdersMapper#updateStatusIfMatch
UPDATE orders SET order_status = #{toStatus}
WHERE id = #{orderId} AND order_status = #{fromStatus}
```

先 CAS 把订单从"待支付"流转为"已取消"，**只有抢到流转权的线程才回补库存**，
因此"用户手动取消"与"定时任务自动取消"并发执行时，库存只会被回补一次。

**4｜超时自动取消定时任务**
`OrderTimeoutTask`（`@Scheduled`，间隔可配，默认 60 秒）批量扫描超时未支付订单，
逐单调用 `releaseTimeoutOrder` 释放库存；单笔失败不影响其他订单，且任务内捕获异常避免调度被取消。

**5｜支付防重**
`markPaidIfUnpaid` 用 `WHERE order_status = 0 AND pay_status = 0` 条件更新，重复支付 / 重复回调只成功一次；
同时支付前校验 `pay_expire_time`，已过期订单不允许支付。

### 关键文件
- 新增 `mapper` 层的 `ProductMapper#deductStock / restoreStock`、`OrdersMapper#updateStatusIfMatch / markPaidIfUnpaid / selectTimeoutOrderIds`
- 新增 `config/OrderProperties.java`、`task/OrderTimeoutTask.java`
- 修改 `service/impl/OrdersServiceImpl.java`（下单/支付/取消/发货全部改为条件更新）
- 修改 `PetMarketApplication.java`（`@EnableScheduling`）
- 修改 `entity/Orders.java`（`payExpireTime`）、`vo/OrderVO.java`
- 修改 `resources/sql/pet_market.sql`（新增字段与索引，文件末尾附**老库升级脚本**）

### 可调参数
| 配置项 | 环境变量 | 默认值 |
| --- | --- | --- |
| `petmarket.order.pay-timeout-minutes` | `ORDER_PAY_TIMEOUT_MINUTES` | 30（分钟） |
| `petmarket.order.release-scan-interval-ms` | `ORDER_RELEASE_SCAN_INTERVAL_MS` | 60000（毫秒） |
| `petmarket.order.auto-release-enabled` | — | true |
| `petmarket.order.release-batch-size` | — | 200（单批上限） |

---

## 五、本地商品图片上传（P1）

需求背景：**本阶段不做服务器部署，图片保存在运行后端的这台电脑上**即可。

### 后端
- 上传接口：`POST /admin/upload/image`（`multipart/form-data`，字段 `file`，可选 `bizType`）
  受 `AdminGuardInterceptor` 保护，**只有管理员 Token 能上传**；
- 前台头像接口：`POST /api/upload/avatar`（需登录）；
- 文件按 `业务类型/日期/UUID.扩展名` 落盘，例如 `data/upload/product/20260910/ecd556….png`；
- 通过 `/uploads/**` 静态资源映射直接对外访问，无需额外服务器；
- 元信息写入 `upload_file` 表（原始名、存储名、大小、MIME、上传人），便于后续清理与迁移到 OSS。

### 安全设计（5 层校验）
| 层 | 措施 |
| --- | --- |
| 文件名 | **不使用客户端文件名拼路径**，服务端用 UUID 重建；写入前 `normalize()` 校验目标路径必须落在上传根目录内 |
| 扩展名 | 白名单 `.jpg/.jpeg/.png/.gif/.webp/.bmp` |
| MIME | 白名单 `image/*` |
| 文件头魔数 | 校验 JPEG/PNG/GIF/BMP/WEBP 真实字节头，**改扩展名的伪装文件会被拒绝并清理已落盘文件** |
| 大小 | 应用层 5MB + Spring multipart 5MB 双重限制 |

`bizType` 走白名单（`product/avatar/category/other`），非法值统一落到 `other`，防止目录穿越。

### 配置项
| 配置项 | 环境变量 | 默认值 |
| --- | --- | --- |
| `petmarket.upload.dir` | `UPLOAD_DIR` | `./data/upload` |
| `petmarket.upload.url-prefix` | `UPLOAD_URL_PREFIX` | `/uploads` |
| `petmarket.upload.max-size` | `UPLOAD_MAX_SIZE` | 5242880（5MB） |

### 前端
- 新增通用组件 `frontend/src/components/ImageUpload.vue`：
  点击上传 / 拖拽预览 / 进度遮罩 / 清除图片 / 也支持直接粘贴图片地址；
- 商品管理页「新增·编辑商品」的"图片URL"文本框 **替换为该上传组件**（`ProductManage.vue`）；
- `vite.config.js` 新增 `/admin/upload/` 与 `/uploads/` 代理，开发环境同样可直接预览；
- `request.js` 增强 403 处理：后台接口收到 403 时清理管理员登录态并跳回后台登录页（提示无权限）。

### 新增数据表
`upload_file`（见 `resources/sql/pet_market.sql` 第 9 节）。**老库需执行**：

```sql
ALTER TABLE `orders` ADD COLUMN `pay_expire_time` DATETIME DEFAULT NULL COMMENT '支付截止时间' AFTER `pay_time`;
ALTER TABLE `orders` ADD INDEX `idx_timeout` (`order_status`, `pay_status`, `create_time`);
-- 再执行 upload_file 建表语句（脚本内已提供，可整段复制）
```

---

## 六、验收记录

### 6.1 自动化测试（后端）

命令：`.\mvnw.cmd test`

```
Running com.pet.petmarket.config.AdminGuardInterceptorTest
Tests run: 7,  Failures: 0, Errors: 0, Skipped: 0
Running com.pet.petmarket.config.AdminSecurityIntegrationTest
Tests run: 8,  Failures: 0, Errors: 0, Skipped: 0
Running com.pet.petmarket.PetMarketApplicationTests
Tests run: 1,  Failures: 0, Errors: 0, Skipped: 0
Running com.pet.petmarket.service.impl.FileUploadServiceImplTest
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
Running com.pet.petmarket.service.OrderInventoryConsistencyTest
Tests run: 8,  Failures: 0, Errors: 0, Skipped: 0

Tests run: 34, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

测试全部使用 **H2 内存库**（MySQL 兼容模式）+ 测试专用建表脚本，**不依赖本机 MySQL**，可重复执行。

重点用例：

| 用例 | 断言 |
| --- | --- |
| 并发下单防超卖 | 8 个线程抢 **2 件**库存 → **恰好 2 单成功、6 单因库存不足失败、最终库存=0（不为负）** |
| 下单扣减 | 库存 2 → 下单 2 件 → 库存 0，订单待支付且带支付截止时间，购物车项清空 |
| 库存不足 | 整单失败，库存不变、无订单产生、购物车项保留 |
| 取消幂等 | 取消回补到 2；**重复取消不重复回补** |
| 超时释放 | 回补库存并置为已取消；再次扫描 0 单；重复释放返回 false，库存不重复增加 |
| 已支付保护 | 已支付订单不被超时任务释放，不允许重复支付 |
| 权限矩阵 | 管理员 200 / 普通用户 403 / 无 Token 401 / 伪造 Token 401 |

### 6.2 端到端实测（真实 HTTP，应用实际启动）

在 `MODE=MySQL` 的 H2 上真实启动应用后逐一验证：

| # | 请求 | 结果 |
| --- | --- | --- |
| 1 | `GET /admin/product/list`（无 Token） | **401** |
| 2 | `GET /admin/product/list`（伪造 Token） | **401** |
| 3 | `GET /admin/product/list`（**普通用户 Token**） | **403** ← 原漏洞已修复 |
| 4 | `POST /admin/product/create`（普通用户 Token） | **403** |
| 5 | `GET /api/order/list`（普通用户 Token） | 200 |
| 6 | `GET /api/order/list`（无 Token） | **401** |
| 7 | `GET /admin/product/list`（管理员 Token） | 200 |
| 8 | `GET /admin/dashboard/stats`（管理员 Token） | 200 |
| 9 | `POST /admin/upload/image`（管理员 + 真实 PNG） | 200，`url=/uploads/product/20260910/ecd556….png`，size=619 |
| 10 | `GET` 上一步返回的 url | 200，`Content-Type: image/png`，619 字节 |
| 11 | 检查磁盘 `data/upload/product/20260910/…png` | 文件存在 |
| 12 | `POST /admin/upload/image`（普通用户 Token） | **403** |
| 13 | `POST /admin/upload/image`（无 Token） | **401** |
| 14 | 上传改名为 `.png` 的文本文件 | 被文件头魔数校验拒绝：`文件内容不是有效的图片` |
| 15 | 用上传地址创建商品 → 前台商品列表 | 商品可见，`mainImage=/uploads/product/…png`，图片可正常访问 |

### 6.3 生产配置校验实测

| 场景 | 结果 |
| --- | --- |
| `--spring.profiles.active=prod` + 代码内置默认密钥 | **启动被阻断**，报错：`[配置检查-生产环境阻断] 生产环境仍在用代码内置的默认 JWT 密钥，请立即轮换…` |
| `prod` + 环境变量提供 `JWT_SECRET`(48位随机) 与 `DB_PASSWORD` | **正常启动**，Tomcat 监听指定端口，SQL 明细日志不再输出 |

### 6.4 前端构建

命令：`npm run build`（在 `frontend/` 目录）

```
✓ built in 30.26s
dist/assets/ProductManage-COAnXowq.js   8.73 kB
```
构建成功，无编译错误（仅有既存的 chunk 体积告警）。

### 6.5 Git 状态

- 新增 `.gitignore`；
- 已执行 `git rm -r --cached .` + `git add -A` 重建索引：**暂存文件从数万个降为 116 个项目文件**，
  `node_modules/`、`target/`、`*.class`、`.idea/` 等均不再纳入版本控制。

---

## 七、本轮文件改动清单

### 新增（后端）
```
src/main/java/com/pet/petmarket/config/AdminGuardInterceptor.java     管理端角色守卫拦截器
src/main/java/com/pet/petmarket/config/UploadProperties.java          上传配置（可环境变量覆盖）
src/main/java/com/pet/petmarket/config/OrderProperties.java           订单超时配置
src/main/java/com/pet/petmarket/config/MybatisMetaObjectHandler.java  createTime/updateTime 自动填充
src/main/java/com/pet/petmarket/config/StartupConfigValidator.java    生产环境配置快速失败校验
src/main/java/com/pet/petmarket/task/OrderTimeoutTask.java            超时订单自动取消定时任务
src/main/java/com/pet/petmarket/common/BusinessException.java         携带状态码的业务异常
src/main/java/com/pet/petmarket/entity/UploadFile.java                上传文件记录实体
src/main/java/com/pet/petmarket/mapper/UploadFileMapper.java
src/main/java/com/pet/petmarket/service/FileUploadService.java
src/main/java/com/pet/petmarket/service/impl/FileUploadServiceImpl.java
src/main/java/com/pet/petmarket/vo/UploadVO.java
src/main/java/com/pet/petmarket/controller/admin/AdminUploadController.java  后台图片上传接口
src/main/java/com/pet/petmarket/controller/api/UploadController.java         前台头像上传接口
src/main/resources/application-prod.yml                               生产 profile
.gitignore                                                             Git 忽略规则
.env.example                                                           环境变量示例
VERSION_UPDATE_V1.1.md                                                 本文档
```

### 新增（测试）
```
src/test/resources/application.yml                       测试配置（H2）
src/test/resources/sql/h2/schema-h2.sql                  H2 建表 + 测试数据
src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker
src/test/java/.../config/AdminGuardInterceptorTest.java          7 个用例
src/test/java/.../config/AdminSecurityIntegrationTest.java       8 个用例
src/test/java/.../service/OrderInventoryConsistencyTest.java     8 个用例（含并发防超卖）
src/test/java/.../service/impl/FileUploadServiceImplTest.java    10 个用例
```

### 新增（前端）
```
frontend/src/components/ImageUpload.vue   通用图片上传组件
```

### 修改
```
pom.xml                                              H2 测试依赖
src/main/java/.../PetMarketApplication.java          开启 @EnableScheduling
src/main/java/.../config/AuthInterceptor.java        401 状态码 + 常量 + 统一 Token 解析
src/main/java/.../config/SecurityConfig.java         关闭表单登录/Basic，统一 401/403 JSON
src/main/java/.../config/WebConfig.java              注册两级拦截器 + /uploads 静态映射
src/main/java/.../common/GlobalExceptionHandler.java 业务异常/上传异常/405/404 状态码
src/main/java/.../entity/Orders.java                 payExpireTime 字段
src/main/java/.../mapper/ProductMapper.java          条件扣减 / 回补库存
src/main/java/.../mapper/OrdersMapper.java           状态机 CAS / 防重支付 / 超时扫描
src/main/java/.../service/OrdersService.java         新增超时释放方法
src/main/java/.../service/impl/OrdersServiceImpl.java 下单/支付/取消/发货全面改造
src/main/java/.../utils/JwtUtils.java                resolveToken + getExpiration
src/main/resources/application.yml                   环境变量化 + SQL 日志移入 dev profile
src/main/resources/sql/pet_market.sql                pay_expire_time + upload_file + 升级脚本
frontend/src/api/admin.js                            uploadImage 接口
frontend/src/api/user.js                             uploadAvatar 接口
frontend/src/utils/request.js                        403 处理（后台无权限 → 回登录页）
frontend/src/views/admin/ProductManage.vue           接入 ImageUpload 组件
frontend/vite.config.js                              /admin/upload/ 与 /uploads/ 代理
```

---

## 八、下一步建议

按"可上线能力 → 宠物特色功能"的顺序，建议下一轮：

**上线前补齐**
1. 接口限流与防刷（登录、下单、上传三个入口）；
2. 统一日志脱敏（手机号/密码不入日志）与关键操作审计日志；
3. 下单接口幂等键（防用户连点重复下单）；
4. 图片上传增加**图片压缩/缩略图**，避免大图直传拖慢前台。

**宠物特色功能**
5. 宠物档案（品种、生日、体重）→ 按宠物体重/年龄**智能推荐**商品与用量；
6. 定期购（宠物粮/猫砂订阅制）与到货提醒；
7. 商品评价与"适合宠物类型"标签体系；
8. 疫苗接种 / 驱虫 / 体检提醒日历。

---

## 九、本地启动方式（实测）

### 命令
```powershell
# 1) 后端（项目根目录）—— 只需这一条
mvn spring-boot:run

# 想同时看到 SQL 明细日志，加 dev profile：
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"

# 2) 前端（另开一个终端）
cd frontend
npm run dev
```

| 入口 | 地址 |
| --- | --- |
| 前台商城 | http://localhost:5173/ |
| 后台管理 | http://localhost:5173/admin/login |
| 后端接口 | http://localhost:8081/api/product/list |

账号：管理员 `admin` / `admin123`；普通用户 `testuser` / `123456`

### 关于 `mvn` 与 `.\mvnw.cmd`
| | 使用的 Maven | 适用场景 |
| --- | --- | --- |
| `mvn`（系统已装 3.9.11，`E:\Deveplop\apache-maven-3.9.11`） | 系统版本 | **本机日常开发，推荐**，命令最短 |
| `.\mvnw.cmd`（wrapper） | 按 `maven-wrapper.properties` 拉取 3.9.16 | 换机器 / 团队协作 / CI，**不依赖本机是否装 Maven** |

两者都是 Maven 3.9.x，构建结果一致，用哪个都能正常启动。

### ⚠️ 常见坑：端口 8081 被占用
`mvn spring-boot:run` 会 fork 一个子 JVM 进程。**关掉终端窗口或 Ctrl+C 有时不会杀掉子进程**，
下次启动就会报 `Web server failed to start. Port 8081 was already in use.`。
清理方式（PowerShell）：

```powershell
# 查看谁占用了 8081
netstat -ano | Select-String ':8081.*LISTENING'
# 结束它（把 <PID> 换成上面查到的进程号）
Stop-Process -Id <PID> -Force
```

DevTools 已启用：不重启进程的前提下，**改 Java 代码后保存会自动重启**，只有改配置/换端口才需要手动重启。

---

## 十、V1.2 登录页重构（左右分屏 + 多角色 Tab）

### 目标
原登录页是「一张居中卡片 + 渐变背景」，视觉单薄，而且用户登录与管理员登录各写一套、样式不统一。
本轮按"大厂标准"重做为**左右分屏**：左屏讲品牌，右屏放表单，并把两个角色合并为 Tab 切换。

### 结构

```
┌──────────────────────────┬───────────────────────────┐
│  左屏 54%：品牌           │  右屏 46%：表单            │
│  · PetMarket 标识 + 爪印  │  · 用户登录 / 管理员登录 Tab │
│  · 主张：让每一次陪伴…     │  · 账号 + 密码（带左图标）   │
│  · 3 条卖点（图标+说明）   │  · 记住账号 + 忘记密码提示   │
│  · 爪印水印 + 柔和光斑     │  · 渐变主按钮（48px 高）     │
│  · 版权行                 │  · 演示账号一键填充 / 注册入口 │
└──────────────────────────┴───────────────────────────┘
```

### 多角色 Tab 的实现方式
关键设计：**一套 UI，两个路由。**

| 入口 URL | 默认 Tab | 调用接口 | 登录态 | 登录后跳转 |
| --- | --- | --- | --- | --- |
| `/login` | 用户登录 | `POST /api/user/login` | `userStore` | `/`（或 `?redirect=`） |
| `/admin/login` | 管理员登录 | `POST /admin/auth/login` | `adminStore` | `/admin` |

- 真正的实现在 `frontend/src/components/SplitLogin.vue`，`views/front/Login.vue` 与
  `views/admin/AdminLogin.vue` 退化为 3 行包装组件 —— 避免两份样式各写各的，改一处即可；
- Tab 切换用 `router.replace` 切换 URL，URL 与 Tab 状态**双向一致**（刷新、分享链接都正确）；
- 额外支持 `?role=admin` 深链，方便把"后台登录入口"当链接发出去；
- 切换角色时清空密码与错误提示、保留已输入的账号名，并清理另一角色的登录态，避免两套 Token 并存造成困惑。

### 体验与安全细节
| 项 | 做法 |
| --- | --- |
| 表单校验 | 账号必填且 ≥2 位；密码必填且 ≥6 位，失焦即时校验 |
| 登录频控 | 连续失败 4 次锁定 60 秒，按钮倒计时并禁用，抑制暴力尝试 |
| 错误提示 | 表单内联 banner（而非一闪而过的 toast）；连不上后端时提示"请确认后端服务（默认 8081 端口）已启动" |
| 开放重定向防护 | `?redirect=` 只接受站内路径（`/` 开头且非 `//`），否则回退默认首页 |
| 记住账号 | 只把账号名存 `localStorage`，**绝不保存密码** |
| 防重复提交 | 提交中禁用按钮 + loading 态 |
| 登录态隔离 | 用户登录成功后清理 adminStore，反之亦然 |

### 无障碍
- 键盘用户可用「跳到登录表单」skip-link 直达表单；
- 请求失败用 `role="alert"` 播报；所有输入框都有可见 `<label>`（不只是 placeholder）；
- 支持 `prefers-reduced-motion`，可关闭入场动画；
- 对比度按 WCAG 2.1 AA 标准校验（见下）。

### 布局与对比度实测（真实浏览器 + 真实像素）

用无头 Chrome 在真实渲染环境跑脚本度量，并对截图做像素采样复算对比度：

| 检查项 | 结果 |
| --- | --- |
| 横向溢出（1440 / 1920 / 900 / 430 四档） | 无 |
| 左栏宽度占比 | 54%（与设计一致） |
| 左栏内容左边缘一致性 | 4 个元素全部对齐在 x=64 ✓ |
| 右栏表单居中 | 左右留白 116/116、226/227、229/229 全部对称 ✓ |
| Tab 指示条对齐 | 与激活 Tab 逐像素对齐（x=882, w=83）✓ |
| 圆角/高度一致性 | 圆角统一 10px，输入框与按钮高度统一 48px ✓ |
| 窄屏（430px） | 左栏隐藏、显示紧凑品牌头、留白 46/46 ✓ |
| 垂直适配（900px 高） | 表单底部余量 68px，不被挤出视口 ✓ |

**对比度审计发现并修复的 4 处缺陷**（均为真实计算值，非估计）：

| 位置 | 修复前 | 修复后 |
| --- | --- | --- |
| 输入框 placeholder | ≈2.6:1 ✗ | **4.76:1 ✓** |
| 「忘记密码…」辅助提示 | 2.56:1 ✗ | **4.76:1 ✓** |
| 「演示账号」小标题 | 2.45:1 ✗ | **4.55:1 ✓** |
| 左下版权行 | 2.82:1 ✗ | **5.42:1 ✓** |

> 左屏深色渐变上的浅色文字（白色主标题、副标题、卖点说明等）经真实像素采样复算为
> **5.0:1 ~ 16.6:1**，全部达标，最初脚本报的"1.0:1"是工具无法解析 CSS 渐变背景导致的**误报**，
> 已用像素采样证伪。

### 涉及文件
```
新增  frontend/src/components/SplitLogin.vue      分屏登录页（含设计令牌与响应式，约 890 行）
改写  frontend/src/views/front/Login.vue          退化为 3 行包装组件
改写  frontend/src/views/admin/AdminLogin.vue     退化为 3 行包装组件
```

### 构建产物
```text
dist/assets/SplitLogin-BfheXmDN.css   10.51 kB │ gzip: 2.93 kB
dist/assets/SplitLogin-C3YRNWRT.js     9.46 kB │ gzip: 4.26 kB
dist/assets/Login-*.js                 0.20 kB   ← 路由壳
dist/assets/AdminLogin-*.js            0.21 kB   ← 路由壳
```

### 需要你人工确认的一点（诚实说明）
代码正确性、布局几何、对比度、响应式都已通过程序化方式验证，但**"美观度/质感"这类主观判断我无法自己完成**：
我当前使用的模型不支持图片输入，尝试调用的图片模型通道（`pi-ai` provider）在本部署中也未配置成功。
截图已生成在 `target/shots/`（`01-user-login.png`、`02-admin-login.png`、`03-narrow.png`、`04-large.png`），
建议你直接在 http://localhost:5173/login 与 http://localhost:5173/admin/login 肉眼过一遍，
配色、留白、文案有想调整的地方告诉我即可。

---

## 十一、V1.3 登录页视觉重做

### 为什么重做
V1.2 的登录页"能看但不好看"：彩色渐变按钮、模糊光斑、爪印水印、随视口缩放的超大标题，
整体偏营销页气质；而且 `el-tabs` 的角色切换在真实浏览器里**点了没反应**。

### 设计语言换血
| 维度 | V1.2 | V1.3 |
| --- | --- | --- |
| 主按钮 | 彩色渐变 + 投影 | **实色** `#2f5fe0`，hover 加深 |
| 左屏装饰 | 3 个模糊光斑 + 爪印 SVG 水印 | **点阵纹理** + 一层极淡同色系光晕 |
| 标题字号 | `clamp(30px, 3.1vw, 46px)` 随视口缩放 | 固定 **40px**，稳定可预期 |
| 字号阶梯 | 混用 12/12.5/13/13.5/14/14.5/15/15.5px | 收敛为 **12 / 13 / 14 / 15 / 24 / 40** |
| 角色切换 | `el-tabs`（第三方事件不可控） | **自绘分段控件**（原生 button + 滑动滑块） |
| 左屏信息 | 品牌主张 + 3 条带图标卖点 | 品牌主张 + **3 项数据指标** + 3 条纯文字要点 |
| 演示账号 | 药丸 chip | **等宽字体**的账号/密码明文按钮（点击填入） |

**产物对比**：CSS `10.51 kB → 7.35 kB`，JS `9.46 kB → 8.50 kB`，
截图 `490 kB → 118 kB`（-76%，侧面反映视觉复杂度大幅下降）。

### 同时修掉的两个真实 bug（都是端到端测试抓出来的）
| # | 现象 | 根因 | 修复 |
| --- | --- | --- | --- |
| 1 | 点「管理员登录」Tab **完全没反应**，URL 不同步、Tab 不高亮 | 依赖 `el-tabs` 的 `@tab-change` 事件，实际未按预期触发 | 弃用 `el-tabs`，改为自绘分段控件，切换逻辑自己控制，并显式 `router.replace` 同步 URL |
| 2 | 密码输错时提示 **"无法连接服务器"**（其实后端已正常返回「密码错误」） | `request.js` 业务失败时 reject 的是普通 `Error`（无 `response`），而错误提示函数只判断 `error.response`，把业务错误误判成网络故障 | 区分「网络层失败」（含 `ERR_NETWORK` / 超时 / 无 response）与「业务失败」（读 `error.message`），并新增用例守住 |

> bug 2 影响面比 bug 1 更大：**所有用户密码输错都会看到错误的错误信息**，会让人误以为服务挂了。

### 端到端验收（真实浏览器，CDP 驱动真实鼠标点击与键盘输入）
```
场景 1 用户登录 .......... 7/7  ✓  默认Tab、填充、错误提示、登录跳转、Token 持久化
场景 2 管理员登录 ........ 7/7  ✓  Tab切换+URL同步、文案切换、进后台、Token隔离
场景 3 窄屏 430px ........ 3/3  ✓  隐藏左屏、显示紧凑头部、无横向溢出
控制台 ................... 2/2  ✓  无 JS 异常、无 console 错误
────────────────────────────────
合计 19/19 通过
```
脚本保留在 `target/e2e-login.js`，可随时 `node target/e2e-login.js` 回归。

### 截图
`target/shots/`：`v13-01-user-1440.png`、`v13-02-admin-1440.png`、`v13-03-narrow-430.png`、`v13-04-large-1920.png`

---

*本文档由本轮开发过程同步整理，验收数据均来自实际执行结果。*

