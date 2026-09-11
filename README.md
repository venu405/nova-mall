# NovaMall 积分商城系统

NovaMall 是一套前后端分离的积分商城全栈系统，覆盖用户、商品、购物车、订单与后台管理的完整业务闭环：前台提供首页门户、商品分类、商品搜索、购物车、订单结算、会员中心等功能；后台提供轮播图管理、商品管理、分类管理、订单管理、会员管理等能力。

## 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Spring Boot 2.7、MyBatis、MySQL、Redis（热点数据缓存）、Swagger（springfox）、Maven |
| 前端 | Vue 3、Element Plus、Vite、Vant（移动端 H5）、Pinia、Axios |

## 目录结构

```
nova-mall/
├── nova-mall-api/        # 后端服务（Spring Boot + MyBatis）
│   ├── src/main/java/com/novamall/api/   # Java 源码
│   ├── src/main/resources/mapper/        # MyBatis Mapper XML
│   └── src/main/resources/novamall_db_schema.sql  # 建库建表脚本
└── nova-mall-vue3-app/   # 前端应用（Vue 3 + Vite）
    ├── src/              # 前端源码
    └── novamall-server.js # dist 静态预览服务
```

## 快速启动

### 后端

1. 准备 MySQL 8，创建数据库 `novamall_db`，并执行 `nova-mall-api/src/main/resources/novamall_db_schema.sql` 初始化表结构与数据。
2. 按需修改 `nova-mall-api/src/main/resources/application.properties` 中的数据源账号密码。
3. （可选）准备 Redis 用于热点数据缓存；没有 Redis 时将 `novamall.cache.enabled` 置为 `false` 即可正常启动。
4. 启动服务：

```bash
cd nova-mall-api
mvn spring-boot:run
```

服务默认端口为 `28019`。

### 前端

```bash
cd nova-mall-vue3-app
npm install
npm run dev
```

构建生产产物使用 `npm run build`，产物输出至 `dist/`。

## 缓存设计

后端通过 Redis 对读多写少的热点数据做缓存，整体实现位于 `com.novamall.api.cache` 包，业务侧通过 `NovaMallCacheService` 接口访问，由 `novamall.cache.enabled` 配置项做条件装配（关闭时装配空实现，应用不依赖 Redis 也能启动）。

- **缓存点位**：商品详情（`novamall:goods:detail:{id}`）、首页轮播图（`novamall:index:carousel:{num}`）、首页配置商品（`novamall:index:config:{type}:{num}`）。
- **防穿透**：数据库查询为空时缓存空值标记（商品详情缓存空对象、列表缓存空 List）并设置 60 秒短 TTL，恶意请求不存在的商品 ID 会被缓存层直接拦截，不会反复打穿数据库。
- **防雪崩**：正常数据 TTL 为 30 分钟 ± 5 分钟随机抖动，避免大量缓存 key 在同一时刻集中失效。
- **缓存一致性**：后台管理端修改商品、上下架商品、增删改轮播图与首页配置时，同步删除对应缓存（单 key 删除或按前缀批量删除），保证管理端改完前台立即生效。
- **降级**：缓存读写异常时捕获并降级为直接查询数据库，缓存故障不影响主流程。

## 订单超时自动取消

待支付订单超过 `novamall.order.pay-timeout-minutes`（默认 30 分钟，可配置，演示时可调小）未支付时，由定时任务自动取消并回补库存，实现位于 `com.novamall.api.task.OrderTimeoutTask` 与 `NovaMallOrderServiceImpl.cancelOrderByTimeout`。

- **定时扫描**：基于 Spring `@Scheduled` 每分钟扫描一次，筛选创建时间早于超时阈值、状态为待支付的订单，单批最多处理 100 笔。
- **状态乐观条件**：取消 SQL 为 `UPDATE ... SET order_status=超时关闭 WHERE order_id=? AND order_status=待支付`，与用户手动取消、支付成功并发时只有一个能生效，影响行数为 0 则直接跳过。
- **原子回补**：库存回补使用 `stock_num = stock_num + #{num}` 的原子 SQL（`NovaMallGoodsMapper.addBackStockNum`），与下单时 `stock_num = stock_num - #{num} AND stock_num >= #{num}` 的乐观扣减对称。
- **事务与容错**：单笔订单的取消与回补在同一事务内，回补失败抛异常回滚；单批逐单 try-catch，单笔失败不影响其他订单，等待下一轮扫描重试。

## AI 商品助手

商品详情页内置 AI 助手（后端 `NovaMallAiAssistantService` + `service/ai/LlmClient`，前端 `ProductDetail.vue` 浮动面板），提供两个能力：

- **AI 商品简介**（`POST /api/v1/goods/{id}/ai-summary`）：取商品名称、简介、价格、分类与剥离 HTML 后的详情文本构造 Prompt，生成 3-5 条卖点；结果写入 Redis（`novamall:goods:ai-summary:{id}`，TTL 24 小时），商品编辑/上下架时主动失效。
- **AI 商品问答**（`POST /api/v1/goods/{id}/ai-chat`）：将商品信息作为上下文注入，System Prompt 强制 grounding 约束——只允许依据商品信息回答，信息不足时明确拒答并建议联系客服，禁止编造参数、价格、售后政策；问答不缓存。

**降级策略**：LLM 调用统一封装在 `LlmClient` 中，`novamall.ai.enabled=false`、未配置 api-key、调用超时或异常时——简介接口返回基于商品字段的模板化兜底内容，问答接口返回友好提示文案；任何情况下 AI 故障不影响商品详情页本身的访问。

**配置方式**（application.properties）：填入 `novamall.ai.api-key` 并将 `novamall.ai.enabled=true` 即启用，默认适配 OpenAI 兼容协议（默认 DeepSeek：`base-url=https://api.deepseek.com`、`model=deepseek-chat`），可通过 `novamall.ai.timeout-seconds` 控制超时。Token 成本控制：问题截断 500 字、详情文本截断 2000 字、低温度（0.3）输出。

## 自动化测试

后端核心链路配有接口级自动化测试（JUnit 5 + Spring Boot Test，`@SpringBootTest` 随机端口 + TestRestTemplate 走真实 HTTP 请求），位于 `nova-mall-api/src/test/java/com/novamall/api/`，共 5 个测试类 7 个用例：

- **UserApiTest**：注册 → 登录 → 带 token 访问鉴权接口；未带 token 访问受保护接口返回 416。
- **GoodsCacheApiTest**：商品详情首次查询回源写缓存、二次查询命中缓存；查询不存在的商品返回业务错误并写入 TTL ≤ 60 秒的空值缓存（防穿透断言）。
- **OrderFlowApiTest**：加购物车 → 创建地址 → 下单全链路，校验库存原子扣减数量与订单待支付状态。
- **OrderTimeoutTest**：下单后将创建时间改为 40 分钟前，触发定时任务扫描，校验订单超时关闭、库存回补、商品详情缓存清除。
- **ConcurrentOrderTest**：库存 5 件、8 个用户并发下单，校验恰好 5 单成功、库存扣到 0 不为负（防超卖）。

测试与生产完全隔离：使用独立数据库 `novamall_db_test`（由 `novamall_db_schema.sql` 初始化）与 Redis database 1，随机端口启动，不影响 28019 端口的运行实例。

```bash
# 首次运行前初始化测试库
mysql -h127.0.0.1 -uroot -proot -e "CREATE DATABASE IF NOT EXISTS novamall_db_test DEFAULT CHARSET utf8mb4;"
mysql -h127.0.0.1 -uroot -proot novamall_db_test < src/main/resources/novamall_db_schema.sql
# 运行测试（可重复执行，用例内自动重置库存并清理缓存 key）
mvn test
```

## License

本项目基于 [newbee-mall](https://github.com/newbee-ltd/newbee-mall) 二次开发，遵循其开源协议；各子项目目录中保留原 LICENSE 文件，版权归原作者所有。
