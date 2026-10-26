# PetMarket Redis 缓存接入说明

> 本轮新增：用 **本地 Docker** 启动 Redis，并给三个高频读接口接上 **Cache-Aside 缓存**。
> 技术栈：Spring Boot 3.5.16 / Java 17 / Spring Data Redis（Lettuce 连接池）/ docker compose。

---

## 一、这轮做了什么

| 能力 | 实现 |
| --- | --- |
| 本地 Redis 一键启动 | 根目录 `docker-compose.yml`（`redis:7.4-alpine`，AOF 持久化 + 健康检查 + 密码认证） |
| 缓存接入 | 商品详情、前台分类列表、宠物个性化推荐三处读接口走 Redis |
| 缓存失效 | 写操作主动失效；跨表影响用前缀批量失效（SCAN，不用 `KEYS`） |
| 故障降级 | Redis 挂掉/超时自动回落数据库，接口照常可用；故障后 30 秒熔断窗口，避免每个请求都等超时 |
| 可观测 | Redis 故障只打一条 WARN；键名统一前缀，便于 `redis-cli` 排查 |

**刻意没做的**：分页列表缓存（查询条件组合多，键会爆炸）、会话存储、限流、Token 黑名单。
后续要做的话，`RedisCache` 这一层可以直接复用。

---

## 二、完整启动流程（MySQL + Redis + Java + 前端）

### 0. 先看依赖关系与启动顺序

| 依赖 | 必需性 | 说明 |
| --- | --- | --- |
| JDK 17+ | 必需 | 项目按 Java 17 编译；本机更高版本 JDK 也能运行 |
| MySQL 8.x | 必需 | 业务数据都在这里，库名 `pet_market` |
| Redis | **非必需** | 不启动应用也能起，缓存自动降级为直连数据库；但要用上本次功能就必须启动 |
| Docker Desktop | 启动 Redis 时需要 | Redis 由 `docker compose` 管理 |
| Node 18+ | 仅前端需要 | Vite 开发服务器，端口 5173 |

```
第 1 步：Docker Desktop（Docker 引擎就绪）
   │
   ├─ 第 2 步：MySQL 服务                    →  MySQL :3306 / 库 pet_market
   └─ 第 3 步：docker compose up -d          →  Redis :6379（healthy）
              │
              ▼
   第 5 步：.\mvnw.cmd spring-boot:run       →  后端  :8081
              │
              ▼
   第 7 步：cd frontend && npm run dev        →  前端  :5173（代理 /api 到 8081）
```

> MySQL 与 Redis 谁先起都行，**但都要在后端处理请求之前就绪**：MySQL 决定接口能不能查到数据，
> Redis 决定缓存能不能命中（未就绪时自动降级查库，接口不会因此报错）。

### 1. 核对环境（一次性）

```powershell
java -version          # 期望 17 及以上
docker --version       # 能输出版本说明 Docker CLI 可用
docker compose version # 期望 v2 及以上
```

Docker 命令报 `permission denied ... dockerDesktopLinuxEngine` 或 `cannot connect to the Docker daemon`
时，先启动 **Docker Desktop** 并等它进入 Running 状态。

### 2. 启动 MySQL，确认库表存在（必需）

默认连接参数（与 `application.yml` 默认值一致，可用环境变量覆盖）：
`127.0.0.1:3306` / 库 `pet_market` / 用户 `root` / 密码 `123456`。

```powershell
# 能连上 + 库存在
mysql -uroot -p123456 -h 127.0.0.1 -P 3306 -N -e "SELECT VERSION(); SHOW DATABASES LIKE 'pet_market';"

# 有数据（商品表行数 > 0）
mysql -uroot -p123456 -h 127.0.0.1 -P 3306 pet_market -N -e "SELECT COUNT(*) FROM product;"
```

> 本机 MySQL 由 phpStudy 提供；如果 `mysql` 不在 PATH 里，用它的完整路径执行，例如
> `E:\phpstudy_pro\Extensions\MySQL5.7.26\bin\mysql.exe`。

**首次建库**（全新环境才需要）：`pet_market.sql` 会建库、建表并写入初始数据。

```powershell
cmd /c "mysql -uroot -p123456 --default-character-set=utf8mb4 < src\main\resources\sql\pet_market.sql"
```

**老库升级**（可选，脚本幂等可重复执行）：

```powershell
mysql -uroot -p123456 --default-character-set=utf8mb4 pet_market -e "source src/main/resources/sql/V1.2_add_pet_categories_products.sql"
mysql -uroot -p123456 --default-character-set=utf8mb4 pet_market -e "source src/main/resources/sql/V1.3_add_pet_table.sql"
```

> PowerShell 不支持 `<` 输入重定向，所以第一条用 `cmd /c` 包一层，或者用 `source` 写法。

### 3. 启动 Redis（Docker Compose）

在项目根目录执行：

```powershell
docker compose up -d
docker compose ps
docker exec petmarket-redis redis-cli --no-auth-warning -a petmarket ping   # 期望 PONG
```

- 第一次会拉取 `redis:7.4-alpine` 镜像，之后秒起；
- `docker compose ps` 的 STATUS 应显示 `Up ... (healthy)`，没到 healthy 就再等几秒；
- 容器带 `restart: unless-stopped`：电脑重启、Docker Desktop 起来后一般会自己恢复，
  如果 `docker compose ps` 看不到容器，再执行一次 `docker compose up -d` 即可；
- 密码默认 `petmarket`；如要修改，`REDIS_PASSWORD` 必须**同时**给容器和应用设置（见下一步）。

### 4. 设置环境变量（可选）

不设置就使用默认值（本机 MySQL `root/123456`、Redis `localhost:6379/petmarket`），
所以**只在本机开发时这一步可以跳过**。需要覆盖时：

```powershell
$env:DB_URL="jdbc:mysql://localhost:3306/pet_market?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true"
$env:DB_USERNAME="root"
$env:DB_PASSWORD="123456"

$env:REDIS_HOST="localhost"
$env:REDIS_PORT="6379"
$env:REDIS_PASSWORD="petmarket"    # 必须与 Redis 服务端一致，否则缓存静默降级

$env:JWT_SECRET="请换成至少32字节的随机串"
```

IDEA 用户：Run/Debug Configurations → Environment variables 里填同样的键值。
完整变量清单见 [.env.example](.env.example) 与本文第三节。

### 5. 启动后端

在项目根目录执行（或 IDEA 里直接运行 `PetMarketApplication`）：

```powershell
.\mvnw.cmd spring-boot:run
# 本机装了 Maven 也可以：mvn spring-boot:run
```

看到下面两行就是启动成功：

```
Tomcat started on port 8081 (http)
Started PetMarketApplication in x.xxx seconds
```

要点：

- 启动阶段**不会**主动连 Redis（Lettuce 懒连接），所以 Redis 没起来也能启动成功；
- 但 MySQL 不可用时接口会报错（数据源是必需的），启动日志里若出现数据源连接异常要先解决第 2 步；
- 端口被占用（`Port 8081 was already in use`）多半是已有实例在跑，先停掉旧进程，
  或临时换端口：`$env:SERVER_PORT="8082"` 再启动。

### 6. 确认 Redis 真的连上了

**看"启动成功"不能证明 Redis 通了**——连接是第一次访问缓存接口时才建立的。按三步确认：

1）访问一个读接口（商品详情是公开接口，不需要登录）：

```powershell
curl.exe http://localhost:8081/api/product/1
```

2）看 Redis 里是否出现缓存键：

```powershell
docker exec petmarket-redis redis-cli --no-auth-warning -a petmarket --scan --pattern 'petmarket:*'
# 期望看到：petmarket:v1:product:detail:1
```

3）看缓存内容和剩余 TTL（应 ≤ 600 秒）：

```powershell
docker exec petmarket-redis redis-cli --no-auth-warning -a petmarket get petmarket:v1:product:detail:1
docker exec petmarket-redis redis-cli --no-auth-warning -a petmarket ttl petmarket:v1:product:detail:1
```

再打开前台分类列表、宠物推荐页，会分别生成 `petmarket:v1:category:active-list` 和
`petmarket:v1:pet:recommend:{petId}`。

**没出现键怎么办**：先看后端日志里有没有 `Redis 暂不可用…` 的 WARN。

- 有 → 应用连不上 Redis（密码/端口/容器没起），此时接口仍正常（自动降级查库），修好连接后 30 秒内自动恢复缓存；
- 没有 → 检查 `CACHE_ENABLED` 是否被设成了 `false`，或确认访问的确实是缓存过的接口。

### 7. 启动前端（可选）

```powershell
cd frontend
npm install     # 首次
npm run dev     # http://localhost:5173
```

`frontend/vite.config.js` 已把 `/api`、`/admin/*`、`/uploads` 代理到 `http://localhost:8081`，
所以要先起后端再起前端，否则页面接口全部报错。

### 8. 停止与清理

```powershell
# 停 Redis（缓存数据保留在命名卷 petmarket-redis-data 里）
docker compose down

# 停 Redis 并删除数据卷（彻底清空缓存）
docker compose down -v

# 只清缓存数据、容器继续运行
docker exec petmarket-redis redis-cli --no-auth-warning -a petmarket flushdb

# 后端：终端 Ctrl+C 或 IDEA 里 Stop
```

> `docker compose down` 只影响 Redis，不会动 MySQL 和后端进程。

---

## 三、配置项

### Redis 连接（`application.yml` → `spring.data.redis`）

| 配置 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `host` | `REDIS_HOST` | `localhost` | |
| `port` | `REDIS_PORT` | `6379` | 同时被 compose 用于宿主机端口映射 |
| `password` | `REDIS_PASSWORD` | `petmarket` | 必须与 Redis 服务端一致，否则缓存不可用（自动降级） |
| `database` | `REDIS_DATABASE` | `0` | |
| `timeout` | `REDIS_TIMEOUT` | `2s` | 命令超时，刻意设短以便快速失败 |
| `connect-timeout` | `REDIS_CONNECT_TIMEOUT` | `2s` | 连接超时 |
| `lettuce.pool.*` | `REDIS_POOL_*` | 16 / 8 / 0 / 1s | Lettuce 连接池（生产 profile 放大到 32 / 16 / 2） |

### 缓存策略（`petmarket.cache.*`）

| 配置 | 环境变量 | 默认值 | 说明 |
| --- | --- | --- | --- |
| `enabled` | `CACHE_ENABLED` | `true` | `false` 时完全不访问 Redis，接口等价直连数据库 |
| `product-detail-ttl` | `CACHE_TTL_PRODUCT_DETAIL` | `10m` | 商品详情 |
| `category-list-ttl` | `CACHE_TTL_CATEGORY_LIST` | `1h` | 前台分类列表 |
| `pet-recommend-ttl` | `CACHE_TTL_PET_RECOMMEND` | `10m` | 宠物推荐 |

> TTL 用 Spring 的 Duration 写法：`30s` / `10m` / `1h`。改动这些配置需要**重启后端**才生效。

---

## 四、缓存键与失效策略

键统一前缀 `petmarket:v1:`（`v1` 是结构版本号，缓存结构不兼容变更时改版本号即可整体作废旧键）。

| 键 | 内容 | TTL | 何时失效 |
| --- | --- | --- | --- |
| `petmarket:v1:product:detail:{productId}` | 商品详情 `ProductVO`（含分类名） | 10m | 商品增/改/删/上下架；分类增/改/删（分类名冗余在详情里） |
| `petmarket:v1:category:active-list` | 启用中的分类列表 | 1h | 分类增/改/删 |
| `petmarket:v1:pet:recommend:{petId}` | 该宠物的推荐商品列表 | 10m | 宠物档案改/删；商品增/改/删/上下架；分类增/改/删 |

设计要点：

- **按宠物维度缓存推荐**：推荐结果依赖宠物档案（物种/生日/过敏史），档案变更时能精确失效单个键；
- **商品/分类变更用前缀失效**：商品集合变化会影响所有宠物的推荐结果，`evictByPrefix` 用 SCAN 游标遍历删除，不会像 `KEYS` 那样阻塞 Redis；
- **归属校验在缓存之前**：`recommendProducts` 先校验宠物是否属于当前用户，再读缓存，避免越权命中他人缓存；
- **缓存不写空值**：查不到商品仍走原来的「商品不存在」异常，避免缓存穿透保护掩盖后续新增数据。

> 注意：商品详情缓存里包含 `stock`，下单/取消订单扣减库存后，详情页展示的库存最多滞后一个 TTL（10 分钟）。
> **库存校验、扣减全部走数据库，不受缓存影响**；如果对库存展示实时性要求高，把 `CACHE_TTL_PRODUCT_DETAIL` 调小或设置 `CACHE_ENABLED=false`。

---

## 五、故障降级是怎么做的

`cache/RedisCache.java` 是所有 Redis 访问的唯一入口，行为如下：

1. **读写异常不抛出**：读返回 `null`（调用方回落数据库），写/删只记一条 WARN；
2. **熔断窗口**：一旦发生连接类异常，30 秒内直接跳过 Redis，不再尝试连接，
   避免 Redis 故障时每个请求都白等一次 2 秒连接超时，把「缓存挂了」放大成「接口都变慢」；
3. **开关兜底**：`CACHE_ENABLED=false` 时所有缓存操作短路，等价于没用缓存，便于线上排查脏数据；
4. **启动不依赖 Redis**：Lettuce 是懒连接，Redis 没起来应用照常启动。

---

## 六、测试

```powershell
.\mvnw.cmd test
```

- `cache/RedisCacheTest`：单元测试，Mockito 模拟 `StringRedisTemplate`，覆盖命中/未命中、JSON 与 `LocalDate` 往返、
  异常降级、熔断、开关关闭、脏值清理 —— **不需要真实 Redis**；
- `cache/RedisCacheIntegrationTest`：集成测试，连真实 Redis 验证「首次读回填缓存 → 绕过 service 直接改库读不到新值
  → 走 service 写操作后缓存失效」；**Redis 没启动时自动跳过**，不会让无 Redis 环境失败；
- 其余测试默认 `petmarket.cache.enabled=false`（见 `src/test/resources/application.yml`），保持测试与 Redis 解耦。

当前结果：`Tests run: 59, Failures: 0, Errors: 0, Skipped: 0`。

---

## 七、启动 / 连接常见问题

| 现象 | 原因 | 处理 |
| --- | --- | --- |
| 后端启动报 `Communications link failure` 或 `Access denied for user` | MySQL 未启动，或账号密码不对 | 启动 MySQL；核对 `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` |
| 接口 500，日志 `Table 'pet_market.xxx' doesn't exist` | 库表没导入或导入了旧结构 | 执行 `pet_market.sql`（老库再跑 V1.2 / V1.3） |
| `Port 8081 was already in use` | 已有旧实例在跑（IDEA 里的旧进程常见） | 停掉旧进程，或临时 `$env:SERVER_PORT="8082"` |
| `docker compose` 报 `permission denied while trying to connect to the Docker API` | Docker Desktop 没启动 / 引擎未就绪 | 先启动 Docker Desktop，等状态 Running |
| 应用日志 `Redis 暂不可用（读取 ...）：NOAUTH Authentication required` | 应用没配密码，而 Redis 要求密码 | 设置 `REDIS_PASSWORD` 与容器一致（默认 `petmarket`）后重启 |
| `WRONGPASS invalid username-password pair` | 应用密码与 Redis 密码不一致 | 同上，两边一起改 |
| `Redis 暂不可用 ... Connection refused` | Redis 没启动、端口没映射或容器没 healthy | `docker compose up -d`；`docker compose ps` 看状态 |
| 缓存键一直不出现 | `CACHE_ENABLED=false`，或密码错导致静默降级 | 检查环境变量；看后端日志有没有 WARN |
| 改了商品/分类，页面仍是旧数据 | 缓存没失效，或看的是分页列表（列表不缓存） | 商品详情/分类列表应主动失效；确认走的是 service 写接口；必要时 `flushdb` |
| 改了代码或 Redis 配置，行为没变 | 旧进程没重启（配置不支持热更新） | 重启后端；必要时 `mvn clean` 后重新构建 |
| 导入 SQL 时中文乱码 | 客户端字符集不是 utf8mb4 | 加 `--default-character-set=utf8mb4` |

---

## 八、上线前注意

- [ ] `REDIS_HOST / REDIS_PORT / REDIS_PASSWORD` 指向内网 Redis，并注入**强密码**；
- [ ] 不要把 `docker-compose.yml` 直接当生产部署方案（它只是本地开发依赖）；
- [ ] 生产 Redis 建议关闭 `FLUSHALL` 等危险命令、设置 `maxmemory` 与淘汰策略、开启持久化或使用托管实例；
- [ ] 多实例部署时所有实例必须连同一个 Redis，否则缓存失效只能在单实例内生效（当前失效是「删除」而非广播，多实例下靠 TTL 兜底）；
- [ ] 如需临时关闭缓存排查问题：`CACHE_ENABLED=false` 重启即可，无需改代码。

---

## 九、文件清单

新增：

```
docker-compose.yml                                             本地 Redis（docker compose）
src/main/java/com/pet/petmarket/cache/CacheKeys.java           缓存键统一管理
src/main/java/com/pet/petmarket/cache/RedisCache.java          Redis 读写封装 + 故障降级/熔断
src/main/java/com/pet/petmarket/config/CacheProperties.java    缓存开关与 TTL 配置
src/test/java/com/pet/petmarket/cache/RedisCacheTest.java      缓存封装单元测试
src/test/java/com/pet/petmarket/cache/RedisCacheIntegrationTest.java  真实 Redis 集成测试
```

修改：

```
pom.xml                       增加 spring-boot-starter-data-redis + commons-pool2
src/main/resources/application.yml        spring.data.redis / petmarket.cache
src/main/resources/application-prod.yml   Redis 说明 + 生产连接池
src/test/resources/application.yml        测试默认关闭缓存
.env.example                  补充 REDIS_* / CACHE_* 变量
service/impl/CategoryServiceImpl.java     分类列表缓存 + 失效
service/impl/ProductServiceImpl.java      商品详情缓存 + 失效
service/impl/PetServiceImpl.java          宠物推荐缓存 + 失效
```
