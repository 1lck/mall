package com.mall.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.modules.auth.AuthTestSupport;
import com.mall.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminDashboardIntegrationTests extends IntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanTables() {
		jdbcTemplate.execute("truncate table orders restart identity cascade");
		jdbcTemplate.execute("truncate table products restart identity cascade");
		jdbcTemplate.execute("truncate table users restart identity cascade");
	}

	@Test
	void dashboardShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);

		mockMvc.perform(get("/api/v1/admin/dashboard")
				.header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void dashboardShouldReturnRealAggregatedDataForAdmin() throws Exception {
		AuthTestSupport.AuthSession adminSession = AuthTestSupport.registerAndLoginAdmin(
			mockMvc,
			objectMapper,
			jdbcTemplate,
			"admin01",
			"管理员",
			"admin123456"
		);

		mockMvc.perform(post("/api/v1/admin/users")
				.header("Authorization", "Bearer " + adminSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(java.util.Map.of(
					"username", "staff01",
					"nickname", "运营同学",
					"password", "staff123456",
					"role", "USER"
				))))
			.andExpect(status().isCreated());

		Instant now = Instant.now();
		Instant yesterday = now.minus(1, ChronoUnit.DAYS);
		Timestamp nowTimestamp = Timestamp.from(now);
		Timestamp yesterdayTimestamp = Timestamp.from(yesterday);

		jdbcTemplate.update(
			"""
				insert into products (product_no, name, category_name, price, stock, status, description, image_url, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
			"PRD-TODAY-001", "在售商品", "手机", 1999.00, 8, "ON_SALE", "desc", null, nowTimestamp, nowTimestamp
		);
		jdbcTemplate.update(
			"""
				insert into products (product_no, name, category_name, price, stock, status, description, image_url, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
				""",
			"PRD-OLD-001", "草稿商品", "数码", 299.00, 30, "DRAFT", "desc", null, yesterdayTimestamp, yesterdayTimestamp
		);
		jdbcTemplate.update(
			"""
				insert into orders (order_no, user_id, total_amount, status, remark, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?, ?)
				""",
			"ORD-TODAY-001", adminSession.userId(), 88.50, "PAID", "paid", nowTimestamp, nowTimestamp
		);
		jdbcTemplate.update(
			"""
				insert into orders (order_no, user_id, total_amount, status, remark, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?, ?)
				""",
			"ORD-TODAY-002", adminSession.userId(), 18.00, "CREATED", "created", nowTimestamp, nowTimestamp
		);
		jdbcTemplate.update(
			"""
				insert into orders (order_no, user_id, total_amount, status, remark, created_at, updated_at)
				values (?, ?, ?, ?, ?, ?, ?)
				""",
			"ORD-YESTERDAY-001", adminSession.userId(), 66.60, "PAID", "paid", yesterdayTimestamp, yesterdayTimestamp
		);

		mockMvc.perform(get("/api/v1/admin/dashboard")
				.header("Authorization", "Bearer " + adminSession.token()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.summaryCards.todayOrderCount").value(2))
			.andExpect(jsonPath("$.data.summaryCards.todaySalesAmount").value(88.50))
			.andExpect(jsonPath("$.data.summaryCards.yesterdaySalesAmount").value(66.60))
			.andExpect(jsonPath("$.data.productOverview.onSaleCount").value(1))
			.andExpect(jsonPath("$.data.productOverview.lowStockCount").value(1))
			.andExpect(jsonPath("$.data.productOverview.totalCount").value(2))
			.andExpect(jsonPath("$.data.userOverview.totalCount").value(2))
			.andExpect(jsonPath("$.data.orderStatistics.weekOrderCount").value(3))
			.andExpect(jsonPath("$.data.orderTrend.length()").isNumber());
	}
}
