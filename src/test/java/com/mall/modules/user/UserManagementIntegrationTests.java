package com.mall.modules.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.modules.auth.AuthTestSupport;
import com.mall.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserManagementIntegrationTests extends IntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void cleanUsersTableIfExists() {
		Integer tableCount = jdbcTemplate.queryForObject(
			"""
				select count(*)
				from information_schema.tables
				where table_schema = 'public' and table_name = 'users'
				""",
			Integer.class
		);

		if (tableCount != null && tableCount > 0) {
			jdbcTemplate.execute("truncate table users restart identity cascade");
		}
	}

	@Test
	void listUsersShouldRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/admin/users"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void listUsersShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);

		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	@ExtendWith(OutputCaptureExtension.class)
	void listUsersShouldWriteForbiddenAccessLog(CapturedOutput output) throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);

		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + session.token()))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		org.assertj.core.api.Assertions.assertThat(output.getOut())
			.contains("Security access denied")
			.contains("path=/api/v1/admin/users")
			.contains("status=403");
	}

	@Test
	void createUserShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);

		mockMvc.perform(post("/api/v1/admin/users")
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "operator01",
					"nickname", "运营同学",
					"password", "operator123",
					"role", "ADMIN"
				))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void updateUserStatusShouldRequireAdminRole() throws Exception {
		AuthTestSupport.AuthSession session = AuthTestSupport.registerAndLoginDefaultUser(mockMvc, objectMapper);

		mockMvc.perform(patch("/api/v1/admin/users/{id}/status", session.userId())
				.header("Authorization", "Bearer " + session.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void createUserShouldPersistAndReturnCreatedUserForAdmin() throws Exception {
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
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "operator01",
					"nickname", "运营同学",
					"password", "operator123",
					"role", "ADMIN"
				))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.username").value("operator01"))
			.andExpect(jsonPath("$.data.nickname").value("运营同学"))
			.andExpect(jsonPath("$.data.role").value("ADMIN"))
			.andExpect(jsonPath("$.data.status").value("ACTIVE"));
	}

	@Test
	void listUsersShouldReturnCreatedUsersForAdmin() throws Exception {
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
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "staff01",
					"nickname", "客服同学",
					"password", "service123",
					"role", "USER"
				))))
			.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/admin/users")
				.header("Authorization", "Bearer " + adminSession.token()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.length()").value(2));
	}

	@Test
	void updateUserStatusShouldToggleToDisabledForAdmin() throws Exception {
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
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "staff01",
					"nickname", "客服同学",
					"password", "service123",
					"role", "USER"
				))))
			.andExpect(status().isCreated());

		mockMvc.perform(patch("/api/v1/admin/users/{id}/status", 2L)
				.header("Authorization", "Bearer " + adminSession.token())
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("status", "DISABLED"))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.data.id").value(2))
			.andExpect(jsonPath("$.data.status").value("DISABLED"));
	}
}
