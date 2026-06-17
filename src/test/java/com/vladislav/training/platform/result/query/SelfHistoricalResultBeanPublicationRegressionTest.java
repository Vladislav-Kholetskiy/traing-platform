package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
/**
 * Проверяет, что {@code SelfHistoricalResultBeanPublication} не уходит от текущих договорённостей.
 * Сценарии сосредоточены на важных регрессионных рисках.
 */
class SelfHistoricalResultBeanPublicationRegressionTest {

    private static final List<Path> SELF_HISTORY_NON_BEAN_FILES = List.of(
        Path.of("src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryService.java"),
        Path.of("src/main/java/com/vladislav/training/platform/result/query/internal/SelfHistoricalResultReader.java")
    );

    @Test
    void selfHistoryContourPublishesOnlyPolicyAwareQueryServiceAndJpaReadSeamAsRuntimeBeans() throws IOException {
        Path seamFile = Path.of(
            "src/main/java/com/vladislav/training/platform/result/infrastructure/persistence/JpaSelfHistoricalResultReader.java"
        );
        assertThat(seamFile).exists();
        assertThat(read(seamFile))
            .contains("@Repository")
            .doesNotContain("@Service")
            .doesNotContain("@Component")
            .doesNotContain("@Configuration");

        Path serviceFile = Path.of(
            "src/main/java/com/vladislav/training/platform/result/query/SelfHistoricalResultQueryServiceImpl.java"
        );
        assertThat(serviceFile).exists();
        assertThat(read(serviceFile))
            .contains("@Service")
            .doesNotContain("@Component")
            .doesNotContain("@Repository")
            .doesNotContain("@Configuration");

        assertThat(SELF_HISTORY_NON_BEAN_FILES).allMatch(Files::exists);

        for (Path contourFile : SELF_HISTORY_NON_BEAN_FILES) {
            String source = read(contourFile);

            assertThat(source)
                
                .doesNotContain("@Service")
                .doesNotContain("@Component")
                .doesNotContain("@Repository")
                .doesNotContain("@Configuration");
        }

        try (Stream<Path> paths = Files.walk(Path.of("src/main/java/com/vladislav/training/platform"))) {
            List<Path> configFiles = paths
                .filter(path -> path.toString().endsWith(".java"))
                .filter(path -> normalized(path).contains("/config/") || normalized(path).contains("/configuration/")
                    || normalized(path).endsWith("Configuration.java")
                    || normalized(path).endsWith("AutoConfiguration.java"))
                .toList();

            for (Path configFile : configFiles) {
                String source = read(configFile);

                assertThat(source)
                    
                    .doesNotContain("SelfHistoricalResultQueryService")
                    .doesNotContain("SelfHistoricalResultQueryServiceImpl")
                    .doesNotContain("SelfHistoricalResultReader")
                    .doesNotContain("JpaSelfHistoricalResultReader");
            }
        }
    }

    private String normalized(Path path) {
        return path.toString().replace('\\', '/');
    }

    private String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read source file: " + path, exception);
        }
    }
}

