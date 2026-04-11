package com.mall.modules.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestSupport {

	public record AuthSession(Long userId, String token) {
	}

	private AuthTestSupport() {
	}

	public static AuthSession registerAndLoginDefaultUser(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
		return registerAndLoginUser(mockMvc, objectMapper, "alice", "Alice", "pass123456");
	}

	public static AuthSession registerAndLoginUser(
		MockMvc mockMvc,
		ObjectMapper objectMapper,
		String username,
		String nickname,
		String password
	) throws Exception {
		Long userId = registerUser(mockMvc, objectMapper, username, nickname, password);
		String token = loginUser(mockMvc, objectMapper, username, password);
		return new AuthSession(userId, token);
	}

	public static AuthSession registerAndLoginAdmin(
		MockMvc mockMvc,
		ObjectMapper objectMapper,
		JdbcTemplate jdbcTemplate,
		String username,
		String nickname,
		String password
	) throws Exception {
		Long userId = registerUser(mockMvc, objectMapper, username, nickname, password);
		jdbcTemplate.update("update users set role = 'ADMIN' where id = ?", userId);
		String token = loginUser(mockMvc, objectMapper, username, password);
		return new AuthSession(userId, token);
	}

	private static Long registerUser(
		MockMvc mockMvc,
		ObjectMapper objectMapper,
		String username,
		String nickname,
		String password
	) throws Exception {
		MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", username,
					"nickname", nickname,
					"password", password
				))))
			.andExpect(status().isCreated())
			.andReturn();

		JsonNode registerRoot = objectMapper.readTree(registerResult.getResponse().getContentAsString());
		return registerRoot.path("data").path("id").asLong();
	}

	private static String loginUser(
		MockMvc mockMvc,
		ObjectMapper objectMapper,
		String username,
		String password
	) throws Exception {
		MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
				.contentType(APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of(
					"username", username,
					"password", password
				))))
			.andExpect(status().isOk())
			.andReturn();

		JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
		return root.path("data").path("token").asText();
	}
}
