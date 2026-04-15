# mall

一个基于 Spring Boot 3 的商城后端练习项目。当前仓库已经不只是“骨架”，而是包含了认证、商品、订单、支付、管理后台、Outbox 和 Kafka 消息链路的一套可运行后端。

## 技术栈

- Java 17
- Spring Boot 3.5.13
- Spring Security + JWT
- MyBatis-Plus
- PostgreSQL + Flyway
- Kafka
- MinIO
- Maven
- Testcontainers

## 当前后端能力

- 认证与鉴权：注册、登录、当前用户信息、管理员角色控制
- 用户管理：管理员查看用户列表、创建用户、启停账号
- 商品管理：商品 CRUD、商品图片上传到 MinIO
- 订单管理：登录用户下单、查单、改单、删单
- 支付流程：创建支付记录、支付成功流转
- 消息链路：订单创建事件、支付成功事件、Kafka 消费者、Outbox 重试与人工重发
- 管理后台：首页统计看板、Outbox 观察页、Outbox 调试接口
- 系统诊断：健康检查、系统概览、OpenAPI/Knife4j 文档

## 主要模块

- `com.mall.modules.auth`
- `com.mall.modules.user`
- `com.mall.modules.product`
- `com.mall.modules.order`
- `com.mall.modules.payment`
- `com.mall.modules.outbox`
- `com.mall.infrastructure.messaging.kafka`
- `com.mall.system`

另外，`cart`、`inventory`、`search`、`infrastructure.search`、`infrastructure.job` 这些目录已经预留，适合继续扩展。

## 本地运行

先启动本地依赖：

```bash
docker compose up -d
```

再启动后端：

```bash
mvn spring-boot:run
```

项目默认使用 `local` profile，本地会连接这些服务：

- PostgreSQL: `127.0.0.1:5432`
- Kafka: `127.0.0.1:9094`
- Kafka UI: [http://127.0.0.1:8081](http://127.0.0.1:8081)
- MinIO API: [http://127.0.0.1:9000](http://127.0.0.1:9000)
- MinIO Console: [http://127.0.0.1:9001](http://127.0.0.1:9001)
- Redis: `127.0.0.1:6379`

## 常用入口

- 应用接口基地址：`http://127.0.0.1:8080`
- 存活探针：`GET /api/v1/system/ping`
- 系统概览：`GET /api/v1/system/overview`
- OpenAPI：`http://127.0.0.1:8080/swagger-ui.html`
- Knife4j：`http://127.0.0.1:8080/doc.html`

## 常用命令

```bash
docker compose up -d
mvn spring-boot:run
mvn test
mvn -q -DskipTests compile
```

## 测试说明

- 单元测试和集成测试都在 Maven 测试流程里。
- 集成测试依赖 Testcontainers 启动 PostgreSQL，所以运行 `mvn test` 之前需要本机可用 Docker。

## 后续扩展方向

- 收紧订单、支付、库存这些核心流程的权限和状态边界
- 给库存扣减补上并发保护，避免超卖
- 继续完善 `cart`、`inventory`、`search` 等业务模块
- 把消息链路和后台运维能力继续补全
