package com.vladislav.training.platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.DockerClientFactory;
/**
 * Собирает проверки вокруг {@code TrainingPlatformApplication}.
 * Эти сценарии помогают держать поведение в порядке.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@EnabledIf("dockerAvailable")
class TrainingPlatformApplicationTests {

    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Test
    void contextLoads() {
    }
}