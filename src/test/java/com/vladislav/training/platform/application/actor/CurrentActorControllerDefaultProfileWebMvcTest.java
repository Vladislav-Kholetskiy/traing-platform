package com.vladislav.training.platform.application.actor;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vladislav.training.platform.common.web.GlobalExceptionHandler;
import com.vladislav.training.platform.common.web.RequestContextFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = CurrentActorController.class)
@Import({
    CurrentActorReadService.class,
    InteractiveActorResolver.class,
    AuthenticatedActorAdapter.class,
    GlobalExceptionHandler.class,
    RequestContextFilter.class,
    CurrentActorControllerWebMvcTestConfig.class
})
/**
 * Проверяет поведение {@code CurrentActorControllerDefaultProfileWebMvc}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class CurrentActorControllerDefaultProfileWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void defaultProfileDoesNotAcceptDemoActorHeaderAsAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("X-Demo-Actor-Id", "101"))
            .andExpect(status().isUnauthorized());
    }
}
