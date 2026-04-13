package com.mall.modules.order;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.modules.auth.AuthTestSupport;
import com.mall.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 订单 CRUD 集成测试，直接验证 HTTP 接口、数据库和返回结构是否协同正常。
 */
class OrderCrudIntegrationTests extends IntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanTablesIfExists() {
		Integer orderTableCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.tables
				where table_schema = 'public' and table_name = 'orders'
				""",
			Integer.class
		);

		if (orderTableCount != null && orderTableCount > 0) {
			jdbcTemplate.execute("truncate table orders restart identity");
		}

		Integer userTableCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.tables
				where table_schema = 'public' and table_name = 'users'
				""",
			Integer.class
		);

		if (userTableCount != null && userTableCount > 0) {
			jdbcTemplate.execute("truncate table users restart identity cascade");
		}

		Integer productTableCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.tables
				where table_schema = 'public' and table_name = 'products'
				""",
			Integer.class
		);

		if (productTableCount != null && productTableCount > 0) {
			jdbcTemplate.execute("truncate table products restart identity cascade");
		}
	}

	@Test
	void createOrderShouldRequireAuthentication() throws Exception {
		mockMvc.perform(post("/api/v1/orders")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateOrderPayload(1L, 1))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void createOrderShouldPersistAndReturnCreatedOrder() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProduct("库存商品A", 10, new BigDecimal("99.95"));

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateOrderPayload(productId, 2))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.orderNo").isString())
			.andExpect(jsonPath("$.data.userId").value(session.userId()))
			.andExpect(jsonPath("$.data.totalAmount").value(199.90))
			.andExpect(jsonPath("$.data.productId").value(productId))
			.andExpect(jsonPath("$.data.quantity").value(2))
			.andExpect(jsonPath("$.data.status").value("CREATED"))
			.andExpect(jsonPath("$.data.remark").value("first order"));

		Integer remainingStock = jdbcTemplate.queryForObject(
			"select stock from products where id = ?",
			Integer.class,
			productId
		);
		assertEquals(8, remainingStock);
	}

	@Test
	void createOrderShouldRejectWhenStockIsInsufficient() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProduct("库存商品B", 1, new BigDecimal("99.95"));

		mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateOrderPayload(productId, 2))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));

		Integer remainingStock = jdbcTemplate.queryForObject(
			"select stock from products where id = ?",
			Integer.class,
			productId
		);
		assertEquals(1, remainingStock);
	}

	@Test
	void getOrderByIdShouldReturnExistingOrder() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProduct("库存商品C", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(session, productId);

		mockMvc.perform(get("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(orderId))
			.andExpect(jsonPath("$.data.orderNo").isString())
			.andExpect(jsonPath("$.data.userId").value(session.userId()))
			.andExpect(jsonPath("$.data.totalAmount").value(199.90))
			.andExpect(jsonPath("$.data.productId").value(productId))
			.andExpect(jsonPath("$.data.quantity").value(2))
			.andExpect(jsonPath("$.data.status").value("CREATED"));
	}

	@Test
	void getOrderByIdShouldRejectAccessingAnotherUsersOrder() throws Exception {
		AuthTestSupport.AuthSession ownerSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "alice", "Alice", "pass123456");
		AuthTestSupport.AuthSession anotherSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "bob", "Bob", "pass123456");
		Long productId = createProduct("库存商品D", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(ownerSession, productId);

		mockMvc.perform(get("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + anotherSession.token()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void listOrdersShouldOnlyReturnCurrentUsersOrders() throws Exception {
		AuthTestSupport.AuthSession aliceSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "alice", "Alice", "pass123456");
		AuthTestSupport.AuthSession bobSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "bob", "Bob", "pass123456");
		Long productId = createProduct("库存商品E", 20, new BigDecimal("99.95"));
		createOrderAndReturnId(aliceSession, productId);
		createOrderAndReturnId(bobSession, productId);

		mockMvc.perform(get("/api/v1/orders")
				.header("Authorization", "Bearer " + aliceSession.token()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(1))
			.andExpect(jsonPath("$.data[0].userId").value(aliceSession.userId()));
	}

	@Test
	void updateOrderShouldPersistChanges() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProduct("库存商品F", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(session, productId);

		mockMvc.perform(put("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateOrderPayload("PAID", "updated order"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(orderId))
			.andExpect(jsonPath("$.data.totalAmount").value(299.50))
			.andExpect(jsonPath("$.data.status").value("PAID"))
			.andExpect(jsonPath("$.data.remark").value("updated order"));
	}

	@Test
	void updateOrderShouldRejectAccessingAnotherUsersOrder() throws Exception {
		AuthTestSupport.AuthSession ownerSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "alice", "Alice", "pass123456");
		AuthTestSupport.AuthSession anotherSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "bob", "Bob", "pass123456");
		Long productId = createProduct("库存商品G", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(ownerSession, productId);

		mockMvc.perform(put("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + anotherSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateOrderPayload("PAID", "try update another order"))))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void deleteOrderShouldRemoveOrder() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProduct("库存商品H", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(session, productId);

		mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void deleteOrderShouldRejectAccessingAnotherUsersOrder() throws Exception {
		AuthTestSupport.AuthSession ownerSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "alice", "Alice", "pass123456");
		AuthTestSupport.AuthSession anotherSession =
			AuthTestSupport.registerAndLoginUser(mockMvc, objectMapper, "bob", "Bob", "pass123456");
		Long productId = createProduct("库存商品I", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(ownerSession, productId);

		mockMvc.perform(delete("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + anotherSession.token()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void updateOrderShouldRejectInvalidStatusTransition() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProduct("库存商品J", 10, new BigDecimal("99.95"));
		Long orderId = createOrderAndReturnId(session, productId);

		mockMvc.perform(put("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateOrderPayload("PAID", "paid order"))))
			.andExpect(status().isOk());

		mockMvc.perform(put("/api/v1/orders/{id}", orderId)
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateOrderPayload("CANCELLED", "try cancel paid order"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	private Long createOrderAndReturnId(AuthTestSupport.AuthSession session, Long productId) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/orders")
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateOrderPayload(productId, 2))))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		JsonNode orderData = root.path("data");

		assertTrue(orderData.path("id").isNumber());
		assertEquals(session.userId(), orderData.path("userId").asLong());
		assertEquals(productId.longValue(), orderData.path("productId").asLong());
		assertEquals(2, orderData.path("quantity").asInt());
		return orderData.path("id").asLong();
	}

	private Map<String, Object> buildCreateOrderPayload(Long productId, int quantity) {
		return Map.of(
			"productId", productId,
			"quantity", quantity,
			"remark", "first order"
		);
	}

	private Map<String, Object> buildUpdateOrderPayload(String status, String remark) {
		return Map.of(
			"totalAmount", new BigDecimal("299.50"),
			"status", status,
			"remark", remark
		);
	}

	private Long createProduct(String name, int stock, BigDecimal price) {
		return jdbcTemplate.queryForObject(
			"""
				insert into products (product_no, name, category_name, price, stock, status, description, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?, ?, now(), now())
				returning id
				""",
			Long.class,
			"PRD-" + System.nanoTime(),
			name,
			"测试分类",
			price,
			stock,
			"ON_SALE",
			"测试商品"
		);
	}
}
