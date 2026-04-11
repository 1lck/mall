package com.mall.modules.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mall.support.IntegrationTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 认证相关集成测试，先把注册、登录和当前用户查询这条主链路钉住。
 */
class AuthIntegrationTests extends IntegrationTestSupport {

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
	void registerShouldPersistAndReturnCreatedUser() throws Exception {
		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "alice",
					"nickname", "Alice",
					"password", "pass123456"
				))))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.id").isNumber())
			.andExpect(jsonPath("$.data.username").value("alice"))
			.andExpect(jsonPath("$.data.nickname").value("Alice"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	void loginShouldReturnTokenAndCurrentUserProfile() throws Exception {
		registerDefaultUser();

		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "alice",
					"password", "pass123456"
				))))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.token").isString())
			.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
			.andExpect(jsonPath("$.data.user.username").value("alice"))
			.andReturn();

		JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		String token = root.path("data").path("token").asText();
		assertTrue(!token.isBlank());

		mockMvc.perform(get("/api/v1/auth/me")
				.header("Authorization", "Bearer " + token))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.success").value(true))
			.andExpect(jsonPath("$.data.username").value("alice"))
			.andExpect(jsonPath("$.data.nickname").value("Alice"))
			.andExpect(jsonPath("$.data.role").value("USER"));
	}

	@Test
	void meShouldRejectAnonymousRequest() throws Exception {
		mockMvc.perform(get("/api/v1/auth/me"))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void loginShouldRejectDisabledUser() throws Exception {
		registerDefaultUser();
		jdbcTemplate.update("update users set status = 'DISABLED' where username = 'alice'");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "alice",
					"password", "pass123456"
				))))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.success").value(false))
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private void registerDefaultUser() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", "alice",
					"nickname", "Alice",
					"password", "pass123456"
				))))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
		assertEquals("alice", root.path("data").path("username").asText());
	}
}
