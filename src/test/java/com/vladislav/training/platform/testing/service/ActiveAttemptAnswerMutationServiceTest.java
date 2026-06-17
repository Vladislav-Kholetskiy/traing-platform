package com.vladislav.training.platform.testing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.vladislav.training.platform.common.exception.ConflictException;
import com.vladislav.training.platform.common.model.AttemptMode;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.AnswerOptionRole;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.QuestionType;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.testing.domain.TestAttempt;
import com.vladislav.training.platform.testing.domain.TestAttemptStatus;
import com.vladislav.training.platform.testing.domain.UserAnswer;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.TestAttemptRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import com.vladislav.training.platform.testing.repository.UserAnswerRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
/**
 * Проверяет поведение сервиса {@code ActiveAttemptAnswerMutation}.
 * Сценарии сосредоточены на прикладной логике.
 */
@ExtendWith(MockitoExtension.class)
class ActiveAttemptAnswerMutationServiceTest {

    private static final Long ACTOR_USER_ID = 301L;
    private static final Instant CREATED_AT = Instant.parse("2026-04-21T09:00:00Z");
    private static final Instant MUTATED_AT = Instant.parse("2026-04-21T09:10:00Z");

    @Mock
    private TestAttemptRepository testAttemptRepository;
    @Mock
    private TestQuestionRepository testQuestionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private AnswerOptionRepository answerOptionRepository;
    @Mock
    private UserAnswerRepository userAnswerRepository;
    @Mock
    private UserAnswerItemRepository userAnswerItemRepository;

    private ActiveAttemptAnswerMutationService service;

    @BeforeEach
    void setUp() {
        service = new ActiveAttemptAnswerMutationService(
            testAttemptRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository
        );
    }

    @Test
    void writePathCreatesRootAndItemsAndPromotesStartedAttemptIntoInProgress() {
        TestAttempt startedAttempt = attempt(101L, TestAttemptStatus.STARTED);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.MULTIPLE_CHOICE,
            choiceOption(7001L, 501L),
            choiceOption(7002L, 501L)
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(startedAttempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(null);
        when(userAnswerRepository.saveUserAnswer(any(UserAnswer.class))).thenAnswer(invocation -> {
            UserAnswer answer = invocation.getArgument(0, UserAnswer.class);
            return new UserAnswer(9001L, answer.testAttemptId(), answer.questionId(), answer.createdAt(), answer.updatedAt());
        });
        when(userAnswerItemRepository.saveUserAnswerItem(any(UserAnswerItem.class))).thenAnswer(invocation -> {
            UserAnswerItem item = invocation.getArgument(0, UserAnswerItem.class);
            return new UserAnswerItem(
                9100L,
                item.userAnswerId(),
                item.answerOptionId(),
                item.leftAnswerOptionId(),
                item.rightAnswerOptionId(),
                item.userOrderPosition(),
                item.createdAt(),
                item.updatedAt()
            );
        });
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt updatedAttempt = service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, null)
            ),
            MUTATED_AT
        );

        assertThat(updatedAttempt.status()).isEqualTo(TestAttemptStatus.IN_PROGRESS);
        assertThat(updatedAttempt.lastActivityAt()).isEqualTo(MUTATED_AT);
        assertThat(updatedAttempt.completedAt()).isNull();
        verify(userAnswerRepository).saveUserAnswer(new UserAnswer(null, 101L, 501L, MUTATED_AT, MUTATED_AT));
        verify(userAnswerItemRepository).deleteUserAnswerItemsByUserAnswerId(9001L);
        verify(userAnswerItemRepository).saveUserAnswerItem(new UserAnswerItem(null, 9001L, 7001L, null, null, null, MUTATED_AT, MUTATED_AT));
        verify(userAnswerItemRepository).saveUserAnswerItem(new UserAnswerItem(null, 9001L, 7002L, null, null, null, MUTATED_AT, MUTATED_AT));
        verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void replacePathReusesExistingRootDeletesPreviousItemsAndKeepsOnlyCurrentSet() {
        TestAttempt inProgressAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        UserAnswer existingAnswer = new UserAnswer(9002L, 101L, 501L, CREATED_AT, CREATED_AT.plusSeconds(30));
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.MATCHING,
            matchingLeftOption(8101L, 501L, "A"),
            matchingRightOption(8102L, 501L, "A")
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(inProgressAttempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(existingAnswer);
        when(userAnswerRepository.saveUserAnswer(any(UserAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userAnswerItemRepository.saveUserAnswerItem(any(UserAnswerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt updatedAttempt = service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 8101L, 8102L, null)),
            MUTATED_AT
        );

        assertThat(updatedAttempt.status()).isEqualTo(TestAttemptStatus.IN_PROGRESS);
        verify(userAnswerRepository).saveUserAnswer(new UserAnswer(9002L, 101L, 501L, CREATED_AT, MUTATED_AT));
        verify(userAnswerItemRepository).deleteUserAnswerItemsByUserAnswerId(9002L);
        verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9002L, null, 8101L, 8102L, null, MUTATED_AT, MUTATED_AT)
        );
        verify(testAttemptRepository, never()).saveTestAttempt(attempt(101L, TestAttemptStatus.IN_PROGRESS));
    }

    @Test
    void choiceNormalizationPersistsDeterministicCanonicalSetForUniqueChoiceItems() {
        TestAttempt startedAttempt = attempt(101L, TestAttemptStatus.STARTED);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.MULTIPLE_CHOICE,
            choiceOption(7001L, 501L),
            choiceOption(7002L, 501L)
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(startedAttempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(null);
        when(userAnswerRepository.saveUserAnswer(any(UserAnswer.class))).thenReturn(
            new UserAnswer(9005L, 101L, 501L, MUTATED_AT, MUTATED_AT)
        );
        when(userAnswerItemRepository.saveUserAnswerItem(any(UserAnswerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)
            ),
            MUTATED_AT
        );

        InOrder inOrder = inOrder(userAnswerItemRepository);
        inOrder.verify(userAnswerItemRepository).deleteUserAnswerItemsByUserAnswerId(9005L);
        inOrder.verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9005L, 7001L, null, null, null, MUTATED_AT, MUTATED_AT)
        );
        inOrder.verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9005L, 7002L, null, null, null, MUTATED_AT, MUTATED_AT)
        );
        verify(userAnswerItemRepository, times(2)).saveUserAnswerItem(any(UserAnswerItem.class));
    }

    @Test
    void rejectsDuplicateChoiceAnswerItemsWithoutAnyAnswerWriteOrSilentDeduplication() {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void sourceGuardForbidsSilentChoiceMergeFallbackInNormalizeChoiceItems() throws IOException {
        String source = Files.readString(Path.of(
            "src",
            "main",
            "java",
            "com",
            "vladislav",
            "training",
            "platform",
            "testing",
            "service",
            "ActiveAttemptAnswerMutationService.java"
        ));

        assertThat(source).contains("private List<ActiveAttemptAnswerItemMutation> normalizeChoiceItems");
        assertThat(source).doesNotContain("(left, right) -> left");
        assertThat(source).doesNotContain("(left, right) -> right");
    }

    @Test
    void rejectsDuplicateMatchingExactPairsWithoutAnyAnswerWriteOrSilentDeduplication() {
        TestAttempt inProgressAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.MATCHING,
            matchingLeftOption(7101L, 501L, "A"),
            matchingRightOption(7201L, 501L, "A"),
            matchingLeftOption(7102L, 501L, "B"),
            matchingRightOption(7202L, 501L, "B")
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(inProgressAttempt);

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 7101L, 7201L, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 7101L, 7201L, null)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("duplicate");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void rejectsDuplicateMatchingRightSideOwnershipWithoutAnyAnswerWrite() {
        TestAttempt inProgressAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.MATCHING,
            matchingLeftOption(7101L, 501L, "A"),
            matchingRightOption(7201L, 501L, "A"),
            matchingLeftOption(7102L, 501L, "B"),
            matchingRightOption(7202L, 501L, "B")
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(inProgressAttempt);

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 7101L, 7201L, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 7102L, 7201L, null)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("unique right-side ownership");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void rejectsDuplicateMatchingLeftWithoutAnyAnswerWriteOrSilentDeduplication() {
        TestAttempt inProgressAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.MATCHING,
            matchingLeftOption(7101L, 501L, "A"),
            matchingRightOption(7201L, 501L, "A"),
            matchingLeftOption(7102L, 501L, "B"),
            matchingRightOption(7202L, 501L, "B")
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(inProgressAttempt);

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 7101L, 7201L, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 7101L, 7202L, null)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("unique left-side ownership");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void orderingNormalizationPersistsDeterministicOrderByPosition() {
        TestAttempt inProgressAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.ORDERING,
            orderingOption(7001L, 501L, 0),
            orderingOption(7002L, 501L, 1),
            orderingOption(7003L, 501L, 2)
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(inProgressAttempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(null);
        when(userAnswerRepository.saveUserAnswer(any(UserAnswer.class))).thenReturn(
            new UserAnswer(9007L, 101L, 501L, MUTATED_AT, MUTATED_AT)
        );
        when(userAnswerItemRepository.saveUserAnswerItem(any(UserAnswerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7003L, null, null, 3),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, 1),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, 2)
            ),
            MUTATED_AT
        );

        InOrder inOrder = inOrder(userAnswerItemRepository);
        inOrder.verify(userAnswerItemRepository).deleteUserAnswerItemsByUserAnswerId(9007L);
        inOrder.verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9007L, 7001L, null, null, 1, MUTATED_AT, MUTATED_AT)
        );
        inOrder.verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9007L, 7002L, null, null, 2, MUTATED_AT, MUTATED_AT)
        );
        inOrder.verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9007L, 7003L, null, null, 3, MUTATED_AT, MUTATED_AT)
        );
    }

    @Test
    void orderingNormalizationRejectsDuplicateAnswerOptionIds() {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, 1),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, 2)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("unique answerOptionId");
    }

    @Test
    void rejectsDuplicateOrderingAnswerOptionWithoutAnyAnswerWriteOrSilentDeduplication() {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7301L, null, null, 1),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7301L, null, null, 2)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("unique answerOptionId");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void orderingNormalizationRejectsDuplicatePositions() {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, 1),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, 1)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("unique userOrderPosition");
    }

    @Test
    void rejectsDuplicateOrderingUserPositionWithoutAnyAnswerWriteOrSilentDeduplication() {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7301L, null, null, 1),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7302L, null, null, 1)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("unique userOrderPosition");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    @Test
    void mixedShapeChoiceAndMatchingIsRejected() {
        assertRejectsMixedShape(List.of(
            new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null),
            new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 8101L, 8201L, null)
        ));
    }

    @Test
    void mixedShapeChoiceAndOrderingIsRejected() {
        assertRejectsMixedShape(List.of(
            new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null),
            new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, 1)
        ));
    }

    @Test
    void mixedShapeMatchingAndOrderingIsRejected() {
        assertRejectsMixedShape(List.of(
            new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(null, 8101L, 8201L, null),
            new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, 1)
        ));
    }

    @Test
    void emptyItemCollectionIsRejectedAndRequiresExplicitClearPath() {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(ACTOR_USER_ID, 101L, 501L, List.of(), MUTATED_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("clearAnswer");
    }

    @Test
    void clearPathDeletesCurrentItemsWithoutCreatingResultOrTerminalizationEffects() {
        TestAttempt inProgressAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        UserAnswer existingAnswer = new UserAnswer(9003L, 101L, 501L, CREATED_AT, CREATED_AT.plusSeconds(15));
        stubOwnedQuestionIdentity(401L, 501L, QuestionType.SINGLE_CHOICE);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(inProgressAttempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(existingAnswer);
        when(userAnswerRepository.saveUserAnswer(any(UserAnswer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt updatedAttempt = service.clearAnswer(ACTOR_USER_ID, 101L, 501L, MUTATED_AT);

        assertThat(updatedAttempt.status()).isEqualTo(TestAttemptStatus.IN_PROGRESS);
        assertThat(updatedAttempt.lastActivityAt()).isEqualTo(MUTATED_AT);
        verify(userAnswerItemRepository).deleteUserAnswerItemsByUserAnswerId(9003L);
        verify(userAnswerRepository).saveUserAnswer(new UserAnswer(9003L, 101L, 501L, CREATED_AT, MUTATED_AT));
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
    }

    @Test
    void clearPathWithoutExistingAnswerStillUpdatesActiveAttemptWithoutImplicitRootCreation() {
        TestAttempt startedAttempt = attempt(101L, TestAttemptStatus.STARTED);
        stubOwnedQuestionIdentity(401L, 501L, QuestionType.SINGLE_CHOICE);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(startedAttempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(null);
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TestAttempt updatedAttempt = service.clearAnswer(ACTOR_USER_ID, 101L, 501L, MUTATED_AT);

        assertThat(updatedAttempt.status()).isEqualTo(TestAttemptStatus.IN_PROGRESS);
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
    }

    @Test
    void rejectsMutationForCompletedExpiredAndAbandonedAttempts() {
        assertRejectsMutationFor(TestAttemptStatus.COMPLETED);
        assertRejectsMutationFor(TestAttemptStatus.EXPIRED);
        assertRejectsMutationFor(TestAttemptStatus.ABANDONED);
    }

    @Test
    void ownerValidationRunsBeforeAnswerWritesAndKeepsWritePhaseLocal() {
        TestAttempt attempt = attempt(101L, TestAttemptStatus.STARTED);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.SINGLE_CHOICE,
            choiceOption(7001L, 501L)
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(attempt);
        when(userAnswerRepository.findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L)).thenReturn(null);
        when(userAnswerRepository.saveUserAnswer(any(UserAnswer.class))).thenReturn(
            new UserAnswer(9004L, 101L, 501L, MUTATED_AT, MUTATED_AT)
        );
        when(userAnswerItemRepository.saveUserAnswerItem(any(UserAnswerItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(testAttemptRepository.saveTestAttempt(any(TestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)),
            MUTATED_AT
        );

        InOrder inOrder = inOrder(
            testAttemptRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository
        );
        inOrder.verify(testAttemptRepository).findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID);
        inOrder.verify(testQuestionRepository).findTestQuestionsByTestId(401L);
        inOrder.verify(questionRepository).findQuestionById(501L);
        inOrder.verify(answerOptionRepository).findAnswerOptionsByQuestionId(501L);
        inOrder.verify(userAnswerRepository).findUserAnswerByTestAttemptIdAndQuestionId(101L, 501L);
        inOrder.verify(userAnswerRepository).saveUserAnswer(new UserAnswer(null, 101L, 501L, MUTATED_AT, MUTATED_AT));
        inOrder.verify(userAnswerItemRepository).deleteUserAnswerItemsByUserAnswerId(9004L);
        inOrder.verify(userAnswerItemRepository).saveUserAnswerItem(
            new UserAnswerItem(null, 9004L, 7001L, null, null, null, MUTATED_AT, MUTATED_AT)
        );
        inOrder.verify(testAttemptRepository).saveTestAttempt(any(TestAttempt.class));
        verifyNoMoreInteractions(
            testAttemptRepository,
            testQuestionRepository,
            questionRepository,
            answerOptionRepository,
            userAnswerRepository,
            userAnswerItemRepository
        );
    }

    @Test
    void rejectsQuestionThatDoesNotBelongToAttemptTestWithoutAnyAnswerWrite() {
        TestAttempt activeAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(testQuestionRepository.findTestQuestionsByTestId(401L)).thenReturn(List.of(testQuestion(401L, 999L)));

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("does not belong to attempt test");

        assertRejectProducedNoWrites();
        verify(questionRepository, never()).findQuestionById(any());
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(any());
    }

    @Test
    void rejectsForeignQuestionWithoutAnyAnswerWrite() {
        TestAttempt activeAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(testQuestionRepository.findTestQuestionsByTestId(401L)).thenReturn(List.of(testQuestion(401L, 501L)));

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            999L,
            List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("does not belong to attempt test");

        assertRejectProducedNoWrites();
        verify(questionRepository, never()).findQuestionById(999L);
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(any());
    }

    @Test
    void rejectsOptionThatDoesNotBelongToQuestionWithoutPartialWrite() {
        TestAttempt activeAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        stubOwnedQuestion(
            401L,
            501L,
            QuestionType.SINGLE_CHOICE,
            choiceOption(7001L, 501L)
        );
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(activeAttempt);

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7999L, null, null, null)),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("does not belong to question");

        assertRejectProducedNoWrites();
    }

    @Test
    void rejectsPayloadShapeThatDoesNotMatchQuestionTypeWithoutPartialWrite() {
        TestAttempt activeAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        stubOwnedQuestionIdentity(401L, 501L, QuestionType.SINGLE_CHOICE);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(activeAttempt);

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null),
                new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7002L, null, null, null)
            ),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("exactly one choice item");

        assertRejectProducedNoWrites();
    }

    @Test
    void clearPathRejectsQuestionOutsideAttemptTestWithoutTouchingAnswerState() {
        TestAttempt activeAttempt = attempt(101L, TestAttemptStatus.IN_PROGRESS);
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(activeAttempt);
        when(testQuestionRepository.findTestQuestionsByTestId(401L)).thenReturn(List.of(testQuestion(401L, 999L)));

        assertThatThrownBy(() -> service.clearAnswer(ACTOR_USER_ID, 101L, 501L, MUTATED_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("does not belong to attempt test");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        assertRejectProducedNoWrites();
        verify(questionRepository, never()).findQuestionById(any());
    }

    private void assertRejectsMutationFor(TestAttemptStatus status) {
        when(testAttemptRepository.findAndLockTestAttemptByIdAndUserId(101L, ACTOR_USER_ID)).thenReturn(attempt(101L, status));

        assertThatThrownBy(() -> service.saveOrReplaceAnswer(
            ACTOR_USER_ID,
            101L,
            501L,
            List.of(new ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation(7001L, null, null, null)),
            MUTATED_AT
        ))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("active attempts");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).findTestAttemptById(any());
        verify(testQuestionRepository, never()).findTestQuestionsByTestId(any());
        verify(questionRepository, never()).findQuestionById(any());
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(any());
    }

    private void assertRejectsMixedShape(List<ActiveAttemptAnswerMutationService.ActiveAttemptAnswerItemMutation> items) {
        assertThatThrownBy(() -> service.saveOrReplaceAnswer(ACTOR_USER_ID, 101L, 501L, items, MUTATED_AT))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("mixed item shapes");

        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testQuestionRepository, never()).findTestQuestionsByTestId(any());
        verify(questionRepository, never()).findQuestionById(any());
        verify(answerOptionRepository, never()).findAnswerOptionsByQuestionId(any());
    }

    private void assertRejectProducedNoWrites() {
        verify(userAnswerRepository, never()).findUserAnswerByTestAttemptIdAndQuestionId(any(), any());
        verify(userAnswerRepository, never()).saveUserAnswer(any(UserAnswer.class));
        verify(userAnswerItemRepository, never()).deleteUserAnswerItemsByUserAnswerId(any());
        verify(userAnswerItemRepository, never()).saveUserAnswerItem(any(UserAnswerItem.class));
        verify(testAttemptRepository, never()).saveTestAttempt(any(TestAttempt.class));
    }

    private void stubOwnedQuestion(
        Long testId,
        Long questionId,
        QuestionType questionType,
        AnswerOption... answerOptions
    ) {
        stubOwnedQuestionIdentity(testId, questionId, questionType);
        when(answerOptionRepository.findAnswerOptionsByQuestionId(questionId)).thenReturn(List.of(answerOptions));
    }

    private void stubOwnedQuestionIdentity(Long testId, Long questionId, QuestionType questionType) {
        when(testQuestionRepository.findTestQuestionsByTestId(testId)).thenReturn(List.of(testQuestion(testId, questionId)));
        when(questionRepository.findQuestionById(questionId)).thenReturn(question(questionId, questionType));
    }

    private TestQuestion testQuestion(Long testId, Long questionId) {
        return new TestQuestion(601L, testId, questionId, 0, java.math.BigDecimal.ONE, CREATED_AT, CREATED_AT);
    }

    private Question question(Long questionId, QuestionType questionType) {
        return new Question(questionId, 201L, "Body", questionType, ContentStatus.PUBLISHED, 0, CREATED_AT, CREATED_AT);
    }

    private AnswerOption choiceOption(Long answerOptionId, Long questionId) {
        return new AnswerOption(
            answerOptionId,
            questionId,
            "Choice " + answerOptionId,
            AnswerOptionRole.CHOICE_OPTION,
            Boolean.FALSE,
            0,
            null,
            null,
            CREATED_AT,
            CREATED_AT
        );
    }

    private AnswerOption matchingLeftOption(Long answerOptionId, Long questionId, String pairingKey) {
        return new AnswerOption(
            answerOptionId,
            questionId,
            "Left " + answerOptionId,
            AnswerOptionRole.MATCH_LEFT,
            null,
            0,
            pairingKey,
            null,
            CREATED_AT,
            CREATED_AT
        );
    }

    private AnswerOption matchingRightOption(Long answerOptionId, Long questionId, String pairingKey) {
        return new AnswerOption(
            answerOptionId,
            questionId,
            "Right " + answerOptionId,
            AnswerOptionRole.MATCH_RIGHT,
            null,
            0,
            pairingKey,
            null,
            CREATED_AT,
            CREATED_AT
        );
    }

    private AnswerOption orderingOption(Long answerOptionId, Long questionId, Integer canonicalOrderPosition) {
        return new AnswerOption(
            answerOptionId,
            questionId,
            "Order " + answerOptionId,
            AnswerOptionRole.ORDER_ITEM,
            null,
            0,
            null,
            canonicalOrderPosition,
            CREATED_AT,
            CREATED_AT
        );
    }

    private TestAttempt attempt(Long id, TestAttemptStatus status) {
        return new TestAttempt(
            id,
            301L,
            401L,
            501L,
            AttemptMode.ASSIGNED,
            status,
            CREATED_AT,
            null,
            null,
            null,
            CREATED_AT.plusSeconds(30),
            CREATED_AT,
            CREATED_AT.plusSeconds(30)
        );
    }
}
