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

## 缓存设计

后端通过 Redis 对读多写少的热点数据做缓存，整体实现位于 `com.novamall.api.cache` 包，业务侧通过 `NovaMallCacheService` 接口访问，由 `novamall.cache.enabled` 配置项做条件装配（关闭时装配空实现，应用不依赖 Redis 也能启动）。

- **缓存点位**：商品详情（`novamall:goods:detail:{id}`）、首页轮播图（`novamall:index:carousel:{num}`）、首页配置商品（`novamall:index:config:{type}:{num}`）。
- **防穿透**：数据库查询为空时缓存空值标记（商品详情缓存空对象、列表缓存空 List）并设置 60 秒短 TTL，恶意请求不存在的商品 ID 会被缓存层直接拦截，不会反复打穿数据库。
- **防雪崩**：正常数据 TTL 为 30 分钟 ± 5 分钟随机抖动，避免大量缓存 key 在同一时刻集中失效。
- **缓存一致性**：后台管理端修改商品、上下架商品、增删改轮播图与首页配置时，同步删除对应缓存（单 key 删除或按前缀批量删除），保证管理端改完前台立即生效。
- **降级**：缓存读写异常时捕获并降级为直接查询数据库，缓存故障不影响主流程。

### 前端

```bash
cd nova-mall-vue3-app
npm install
npm run dev
```

构建生产产物使用 `npm run build`，产物输出至 `dist/`。

## License

本项目基于 [newbee-mall](https://github.com/newbee-ltd/newbee-mall) 二次开发，遵循其开源协议；各子项目目录中保留原 LICENSE 文件，版权归原作者所有。
