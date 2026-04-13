package com.mall;

import com.mall.support.IntegrationTestSupport;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 应用级冒烟测试。
 *
 * <p>主要验证文档页面和数据库连接这些基础能力是否已经可用。</p>
 */
class MallApplicationTests extends IntegrationTestSupport {

	// 用 MockMvc 直接校验文档接口，不需要真的起浏览器。
	@Autowired
	private MockMvc mockMvc;

	// 数据库真正接通后，这里应该能拿到 Spring 创建好的数据源。
	@Autowired
	private DataSource dataSource;

	@Autowired
	private Environment environment;

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
	}

	@Test
	void testsShouldNotRunWithLocalProfile() {
		assertEquals(false, java.util.Arrays.asList(environment.getActiveProfiles()).contains("local"));
		assertEquals(true, java.util.Arrays.asList(environment.getActiveProfiles()).contains("test"));
	}

	@Test
	void apiDocsEndpointShouldBeAvailable() throws Exception {
		// /v3/api-docs 是文档页面依赖的 OpenAPI JSON 数据源。
		mockMvc.perform(get("/v3/api-docs"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"openapi\"")));
	}

	@Test
	void knife4jPageShouldBeAvailable() throws Exception {
		// Knife4j 默认的文档入口页就是 /doc.html。
		mockMvc.perform(get("/doc.html"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("Knife4j")));
	}

	@Test
	void protectedOrderApiShouldStillAllowFrontendOrigin() throws Exception {
		mockMvc.perform(
			get("/api/v1/orders")
				.header("Origin", "http://localhost:5173")
		)
			.andExpect(status().isUnauthorized())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
	}

	@Test
	void orderApiShouldHandleCorsPreflight() throws Exception {
		mockMvc.perform(
			options("/api/v1/orders")
				.header("Origin", "http://localhost:5173")
				.header("Access-Control-Request-Method", "POST")
				.header("Access-Control-Request-Headers", "content-type")
		)
			.andExpect(status().isOk())
			.andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
			.andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
	}

	@Test
	void postgresConnectionShouldBeAvailable() throws Exception {
		// 直接通过 JDBC 执行最简单的 SQL，验证应用已经连上 PostgreSQL。
		try (
			Connection connection = dataSource.getConnection();
			Statement statement = connection.createStatement();
			ResultSet resultSet = statement.executeQuery("select 1")
		) {
			resultSet.next();
			assertEquals(1, resultSet.getInt(1));
		}
	}

	@Test
	void persistenceLayerShouldExposeMybatisPlusInsteadOfJpa() {
		assertEquals(false, applicationContext.containsBean("entityManagerFactory"));
		assertEquals(1, applicationContext.getBeanNamesForType(SqlSessionFactory.class).length);
		assertEquals(true, applicationContext.containsBean("productMapper"));
	}

	@Test
	void backendModuleStructureShouldUseDtoVoAndNestedPersistencePackages() {
		assertEquals(false, Files.exists(Path.of("src/main/java/com/mall/modules/auth/api")));
		assertEquals(false, Files.exists(Path.of("src/main/java/com/mall/modules/order/api")));
		assertEquals(false, Files.exists(Path.of("src/main/java/com/mall/modules/product/api")));
		assertEquals(false, Files.exists(Path.of("src/main/java/com/mall/modules/user/api")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/order/persistence/entity")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/order/persistence/mapper")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/payment/persistence/entity")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/payment/persistence/mapper")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/product/persistence/entity")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/product/persistence/mapper")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/user/persistence/entity")));
		assertEquals(true, Files.exists(Path.of("src/main/java/com/mall/modules/user/persistence/mapper")));
	}

}
