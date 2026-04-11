package com.mall.logging;

import com.mall.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(OutputCaptureExtension.class)
class RequestLoggingIntegrationTests extends IntegrationTestSupport {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldWriteRequestCompletionLogForSuccessfulRequest(CapturedOutput output) throws Exception {
		mockMvc.perform(get("/api/v1/products"))
			.andExpect(status().isOk());

		assertThat(output.getOut())
			.contains("HTTP request completed")
			.contains("method=GET")
			.contains("path=/api/v1/products")
			.contains("status=200")
			.contains("traceId=");
	}
}
