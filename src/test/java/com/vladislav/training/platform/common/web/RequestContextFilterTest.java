package com.vladislav.training.platform.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.context.CurrentRequestContext;
import com.vladislav.training.platform.common.context.RequestContextHolder;
import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.time.UtcClock;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
/**
 * Проверяет поведение {@code RequestContextFilter}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class RequestContextFilterTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-03-30T08:00:00Z");

    @AfterEach
    void tearDown() {
        RequestContextHolder.clear();
    }

    @Test
    void incomingHeadersPopulateRequestContextAndResponseHeaders() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/context")
                .header(RequestContextFilter.CORRELATION_ID_HEADER, "corr-123")
                .header(RequestContextFilter.REQUEST_ID_HEADER, "req-456"))
            .andExpect(status().isOk())
            .andExpect(header().string(RequestContextFilter.CORRELATION_ID_HEADER, "corr-123"))
            .andExpect(header().string(RequestContextFilter.REQUEST_ID_HEADER, "req-456"))
            .andExpect(jsonPath("$.correlationId").value("corr-123"))
            .andExpect(jsonPath("$.requestId").value("req-456"));

        assertThat(RequestContextHolder.getCurrent()).isEmpty();
    }

    @Test
    void missingHeadersGenerateRequestAndCorrelationIdsAndClearThreadLocal() throws Exception {
        MockMvc mockMvc = mockMvc();

        MvcResult result = mockMvc.perform(get("/context"))
            .andExpect(status().isOk())
            .andExpect(header().exists(RequestContextFilter.CORRELATION_ID_HEADER))
            .andExpect(header().exists(RequestContextFilter.REQUEST_ID_HEADER))
            .andReturn();

        String correlationId = result.getResponse().getHeader(RequestContextFilter.CORRELATION_ID_HEADER);
        String requestId = result.getResponse().getHeader(RequestContextFilter.REQUEST_ID_HEADER);
        String body = result.getResponse().getContentAsString();

        assertThat(correlationId).isNotBlank();
        assertThat(requestId).isEqualTo(correlationId);
        assertThat(body).contains(correlationId);
        assertThat(body).contains(requestId);
        assertThat(RequestContextHolder.getCurrent()).isEmpty();
    }

    @Test
    void exceptionResponseUsesRequestContextCorrelationId() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/fail").header(RequestContextFilter.CORRELATION_ID_HEADER, "corr-42"))
            .andExpect(status().isNotFound())
            .andExpect(header().string(RequestContextFilter.CORRELATION_ID_HEADER, "corr-42"))
            .andExpect(header().string(RequestContextFilter.REQUEST_ID_HEADER, "corr-42"))
            .andExpect(jsonPath("$.correlationId").value("corr-42"));

        assertThat(RequestContextHolder.getCurrent()).isEmpty();
    }

    private MockMvc mockMvc() {
        UtcClock utcClock = () -> FIXED_INSTANT;
        return MockMvcBuilders.standaloneSetup(new RequestContextEchoController())
            .addFilters(new RequestContextFilter())
            .setControllerAdvice(new GlobalExceptionHandler(utcClock))
            .build();
    }

    @RestController
    static class RequestContextEchoController {

        @GetMapping("/context")
        Map<String, String> currentContext() {
            CurrentRequestContext context = RequestContextHolder.getRequired();
            return Map.of(
                "correlationId", context.correlationId(),
                "requestId", context.requestId()
            );
        }

        @GetMapping("/fail")
        void fail() {
            throw new NotFoundException("missing");
        }
    }
}
