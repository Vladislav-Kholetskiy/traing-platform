package com.vladislav.training.platform.integration.personnel;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.integration.personnel.service.PersonnelCurrentStateReader;
import com.vladislav.training.platform.integration.personnel.service.PersonnelWorkbookDryRunFacade;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
/**
 * Проверяет договорённости вокруг {@code PersonnelDryRunFacadeWiring}.
 * Тест помогает сохранить предсказуемое поведение.
 */
class PersonnelDryRunFacadeWiringContractTest {

    @Test
    void facadeCanBeConstructedWithoutCurrentStateReaderBeanImplementation() {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();

        assertThatCode(() -> new PersonnelWorkbookDryRunFacade(
            beanFactory.getBeanProvider(PersonnelCurrentStateReader.class)
        )).doesNotThrowAnyException();
    }

    @Test
    void stageFiveClaimsApiPerimeterOnlyAndUsesOptionalReaderLookup() throws IOException {
        String source = Files.readString(Path.of(
            "src/main/java/com/vladislav/training/platform/integration/personnel/service/PersonnelWorkbookDryRunFacade.java"
        ));

        assertThat(source)
            .contains("ObjectProvider<PersonnelCurrentStateReader>")
            .contains("getIfAvailable()")
            .doesNotContain("@Autowired private PersonnelCurrentStateReader")
            .doesNotContain("findBy")
            .doesNotContain(".save(")
            .doesNotContain(".delete(")
            .doesNotContain(".update(");
    }
}
