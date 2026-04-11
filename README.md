# mall

Backend-only skeleton for practicing middleware in a mall project.

## Stack

- Java 17
- Spring Boot 3.5.13
- Maven
- Docker Compose for local middleware

## Current scope

This repository only contains a runnable backend skeleton.
Core business logic and middleware integration are intentionally left for practice.

## Reserved backend areas

- `com.mall.modules.product`
- `com.mall.modules.cart`
- `com.mall.modules.order`
- `com.mall.modules.inventory`
- `com.mall.modules.payment`
- `com.mall.modules.search`
- `com.mall.infrastructure.messaging.kafka`
- `com.mall.infrastructure.search`
- `com.mall.infrastructure.job`

## First practice path

1. Start local middleware with Docker Compose.
2. Run the Spring Boot application locally.
3. Implement order domain events and Kafka producer.
4. Implement Kafka consumers for inventory and order workflows.
5. Add PostgreSQL persistence after Kafka basics are clear.

## Useful commands

```bash
docker compose up -d
./mvnw spring-boot:run
./mvnw test
```

## Smoke check

- `GET /api/v1/system/ping`
- `GET /api/v1/system/overview`
