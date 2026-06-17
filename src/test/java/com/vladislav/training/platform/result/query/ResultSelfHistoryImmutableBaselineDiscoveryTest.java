package com.vladislav.training.platform.result.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.vladislav.training.platform.result.infrastructure.persistence.ResultAnswerOptionSnapshotEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.ResultQuestionSnapshotEntity;
import com.vladislav.training.platform.result.infrastructure.persistence.SpringDataResultJpaRepository;
import com.vladislav.training.platform.result.domain.Result;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
/**
 * Проверяет поведение {@code ResultSelfHistoryImmutableBaselineDiscovery}.
 * Тесты сосредоточены на сценариях, которые важны для работы приложения.
 */
class ResultSelfHistoryImmutableBaselineDiscoveryTest {

    private static final Set<String> ACCEPTABLE_IMMUTABLE_ACTOR_ANCHOR_NAMES = Set.of(
        "userIdSnapshot",
        "actorUserIdSnapshot",
        "ownerUserIdSnapshot",
        "subjectUserIdSnapshot"
    );

    @Test
    void immutableResultBaselineMustExposeMaterializedActorOrUserAnchorForActorOwnedHistory() {
        assertThat(fieldNames(ResultEntity.class))
            .contains("id", "testAttemptId", "attemptMode", "completedAt", "createdAt")
            .anyMatch(ACCEPTABLE_IMMUTABLE_ACTOR_ANCHOR_NAMES::contains);
        assertThat(fieldNames(Result.class))
            
            .anyMatch(ACCEPTABLE_IMMUTABLE_ACTOR_ANCHOR_NAMES::contains);
        assertThat(fieldNames(ResultQuestionSnapshotEntity.class))
            .contains("id", "resultId", "questionOriginalId")
            .doesNotContain("userId", "actorUserId", "ownerUserId", "subjectUserId");
        assertThat(fieldNames(ResultAnswerOptionSnapshotEntity.class))
            .contains("id", "resultQuestionSnapshotId", "answerOptionOriginalId")
            .doesNotContain("userId", "actorUserId", "ownerUserId", "subjectUserId");
    }

    @Test
    void immutableResultBaselineDoesNotExposeMaterializedTestIdentityAnchorForSelfHistoryRead() {
        assertThat(fieldNames(ResultEntity.class))
            .doesNotContain("testId");
        assertThat(fieldNames(Result.class))
            .doesNotContain("testId");
        assertThat(fieldNames(ResultQuestionSnapshotEntity.class))
            .doesNotContain("testId");
        assertThat(fieldNames(ResultAnswerOptionSnapshotEntity.class))
            .doesNotContain("testId");
    }

    @Test
    void resultSideRepositoriesMustExposeOwnerScopedHistoricalReadAnchorWithoutMandatoryAttemptModeFilter() {
        Set<Method> repositoryMethods = Arrays.stream(SpringDataResultJpaRepository.class.getDeclaredMethods())
            .collect(Collectors.toUnmodifiableSet());
        Set<Method> immutableAnchorCandidates = repositoryMethods.stream()
            .filter(method -> {
                String name = method.getName().toLowerCase();
                return name.contains("userid")
                    || name.contains("actoruserid")
                    || name.contains("owneruserid")
                    || name.contains("subjectuserid");
            })
            .collect(Collectors.toUnmodifiableSet());

        assertThat(immutableAnchorCandidates)
            
            .isNotEmpty();
        assertThat(immutableAnchorCandidates)
            .allSatisfy(method -> assertThat(Arrays.stream(method.getParameterTypes()).map(Class::getSimpleName).toList())
                
                .doesNotContain("AttemptMode"));
    }

    private Set<String> fieldNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredFields())
            .map(Field::getName)
            .collect(Collectors.toUnmodifiableSet());
    }

    private Set<String> methodNames(Class<?> type) {
        return Arrays.stream(type.getDeclaredMethods())
            .map(Method::getName)
            .collect(Collectors.toUnmodifiableSet());
    }
}
