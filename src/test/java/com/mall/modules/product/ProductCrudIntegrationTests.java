package com.mall.modules.product;

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
 * 商品 CRUD 集成测试，直接验证接口、数据库和返回结构是否正常联动。
 */
class ProductCrudIntegrationTests extends IntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanProductsTableIfExists() {
		Integer productTableCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.tables
				where table_schema = 'public' and table_name = 'products'
				""",
			Integer.class
		);

		if (productTableCount != null && productTableCount > 0) {
			jdbcTemplate.execute("truncate table products restart identity");
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
	}

	@Test
	void createProductShouldRejectAnonymousRequest() throws Exception {
		mockMvc.perform(post("/api/v1/products")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateProductPayload())))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void createProductShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);

		mockMvc.perform(post("/api/v1/products")
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateProductPayload())))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void createProductShouldPersistAndReturnCreatedProductForAdmin() throws Exception {
		AuthTestSupport.AuthSession adminSession = registerAndLoginAdmin();

		mockMvc.perform(post("/api/v1/products")
				.header("Authorization", "Bearer " + adminSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateProductPayload())))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.productNo").isString())
			.andExpect(jsonPath("$.data.name").value("iPhone 16"))
			.andExpect(jsonPath("$.data.categoryName").value("手机"))
			.andExpect(jsonPath("$.data.price").value(6999.00))
			.andExpect(jsonPath("$.data.stock").value(88))
			.andExpect(jsonPath("$.data.status").value("DRAFT"))
			.andExpect(jsonPath("$.data.description").value("旗舰新机"))
			.andExpect(jsonPath("$.data.imageUrl").value("http://localhost:9000/mall-product-images/iphone16.png"));
	}

	@Test
	void getProductByIdShouldReturnExistingProduct() throws Exception {
		Long productId = createProductAndReturnId();

		mockMvc.perform(get("/api/v1/products/{id}", productId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(productId))
			.andExpect(jsonPath("$.data.productNo").isString())
			.andExpect(jsonPath("$.data.name").value("iPhone 16"))
			.andExpect(jsonPath("$.data.categoryName").value("手机"))
			.andExpect(jsonPath("$.data.price").value(6999.00))
			.andExpect(jsonPath("$.data.stock").value(88))
			.andExpect(jsonPath("$.data.status").value("DRAFT"))
			.andExpect(jsonPath("$.data.imageUrl").value("http://localhost:9000/mall-product-images/iphone16.png"));
	}

	@Test
	void listProductsShouldReturnCreatedProducts() throws Exception {
		createProductAndReturnId();
		createProductAndReturnId();

		mockMvc.perform(get("/api/v1/products"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	void updateProductShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession adminSession = registerAndLoginAdmin();
		AuthTestSupport.AuthSession userSession = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProductAndReturnId(adminSession.token());

		mockMvc.perform(put("/api/v1/products/{id}", productId)
				.header("Authorization", "Bearer " + userSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateProductPayload("ON_SALE"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void updateProductShouldPersistChangesForAdmin() throws Exception {
		AuthTestSupport.AuthSession adminSession = registerAndLoginAdmin();
		Long productId = createProductAndReturnId(adminSession.token());

		mockMvc.perform(put("/api/v1/products/{id}", productId)
				.header("Authorization", "Bearer " + adminSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateProductPayload("ON_SALE"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").value(productId))
			.andExpect(jsonPath("$.data.name").value("iPhone 16 Pro"))
			.andExpect(jsonPath("$.data.price").value(8999.00))
			.andExpect(jsonPath("$.data.stock").value(66))
			.andExpect(jsonPath("$.data.status").value("ON_SALE"))
			.andExpect(jsonPath("$.data.description").value("升级版旗舰新机"))
			.andExpect(jsonPath("$.data.imageUrl").value("http://localhost:9000/mall-product-images/iphone16-pro.png"));
	}

	@Test
	void deleteProductShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession adminSession = registerAndLoginAdmin();
		AuthTestSupport.AuthSession userSession = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);
		Long productId = createProductAndReturnId(adminSession.token());

		mockMvc.perform(delete("/api/v1/products/{id}", productId)
				.header("Authorization", "Bearer " + userSession.token()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void deleteProductShouldRemoveProductForAdmin() throws Exception {
		AuthTestSupport.AuthSession adminSession = registerAndLoginAdmin();
		Long productId = createProductAndReturnId(adminSession.token());

		mockMvc.perform(delete("/api/v1/products/{id}", productId)
				.header("Authorization", "Bearer " + adminSession.token()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true));

		mockMvc.perform(get("/api/v1/products/{id}", productId))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void updateProductShouldRejectInvalidStatusTransition() throws Exception {
		AuthTestSupport.AuthSession adminSession = registerAndLoginAdmin();
		Long productId = createProductAndReturnId(adminSession.token());

		mockMvc.perform(put("/api/v1/products/{id}", productId)
				.header("Authorization", "Bearer " + adminSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildUpdateProductPayload("OFF_SHELF"))))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("BAD_REQUEST"));
	}

	private AuthTestSupport.AuthSession registerAndLoginAdmin() throws Exception {
		String username = "ad" + System.nanoTime();
		return AuthTestSupport.registerAndLoginAdmin(
			mockMvc,
			objectMapper,
			jdbcTemplate,
			username,
			"管理员",
			"admin123456"
		);
	}

	private Long createProductAndReturnId() throws Exception {
		return createProductAndReturnId(registerAndLoginAdmin().token());
	}

	private Long createProductAndReturnId(String accessToken) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/products")
				.header("Authorization", "Bearer " + accessToken)
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(buildCreateProductPayload())))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		JsonNode productData = root.path("data");

		assertTrue(productData.path("id").isNumber());
		assertEquals("iPhone 16", productData.path("name").asText());
		return productData.path("id").asLong();
	}

	private Map<String, Object> buildCreateProductPayload() {
		return Map.of(
			"name", "iPhone 16",
			"categoryName", "手机",
			"price", new BigDecimal("6999.00"),
			"stock", 88,
			"description", "旗舰新机",
			"imageUrl", "http://localhost:9000/mall-product-images/iphone16.png"
		);
	}

	private Map<String, Object> buildUpdateProductPayload(String status) {
		return Map.of(
			"name", "iPhone 16 Pro",
			"categoryName", "手机",
			"price", new BigDecimal("8999.00"),
			"stock", 66,
			"status", status,
			"description", "升级版旗舰新机",
			"imageUrl", "http://localhost:9000/mall-product-images/iphone16-pro.png"
		);
	}
}
