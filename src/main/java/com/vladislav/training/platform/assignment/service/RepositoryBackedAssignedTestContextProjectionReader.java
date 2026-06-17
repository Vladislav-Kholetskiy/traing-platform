package com.vladislav.training.platform.assignment.service;

import com.vladislav.training.platform.assignment.domain.AssignmentTest;
import com.vladislav.training.platform.content.domain.AnswerOption;
import com.vladislav.training.platform.content.domain.Question;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestQuestion;
import com.vladislav.training.platform.content.repository.AnswerOptionRepository;
import com.vladislav.training.platform.content.repository.QuestionRepository;
import com.vladislav.training.platform.content.repository.TestQuestionRepository;
import com.vladislav.training.platform.content.repository.TestRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
class RepositoryBackedAssignedTestContextProjectionReader implements AssignedTestContextProjectionReader {

    private static final Comparator<TestQuestion> TEST_QUESTION_ORDER = Comparator
        .comparingInt(TestQuestion::displayOrder)
        .thenComparing(TestQuestion::id);
    private static final Comparator<AnswerOption> ANSWER_OPTION_ORDER = Comparator
        .comparingInt(AnswerOption::displayOrder)
        .thenComparing(AnswerOption::id);

    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;

    RepositoryBackedAssignedTestContextProjectionReader(
        TestRepository testRepository,
        TestQuestionRepository testQuestionRepository,
        QuestionRepository questionRepository,
        AnswerOptionRepository answerOptionRepository
    ) {
        this.testRepository = Objects.requireNonNull(testRepository, "testRepository must not be null");
        this.testQuestionRepository = Objects.requireNonNull(
            testQuestionRepository,
            "testQuestionRepository must not be null"
        );
        this.questionRepository = Objects.requireNonNull(questionRepository, "questionRepository must not be null");
        this.answerOptionRepository = Objects.requireNonNull(
            answerOptionRepository,
            "answerOptionRepository must not be null"
        );
    }

    @Override
    public AssignedTestContext readAssignedTestContext(AssignmentTest assignmentTest) {
        Objects.requireNonNull(assignmentTest, "assignmentTest must not be null");
        Test test = testRepository.findTestById(assignmentTest.testId());
        List<AssignedTestContext.AssignedTestQuestionContext> questions = testQuestionRepository
            .findTestQuestionsByTestId(assignmentTest.testId()).stream()
            .sorted(TEST_QUESTION_ORDER)
            .map(this::toQuestionContext)
            .toList();
        return new AssignedTestContext(
            assignmentTest.assignmentId(),
            assignmentTest.id(),
            test.id(),
            test.name(),
            questions
        );
    }

    private AssignedTestContext.AssignedTestQuestionContext toQuestionContext(TestQuestion testQuestion) {
        Question question = questionRepository.findQuestionById(testQuestion.questionId());
        List<AssignedTestContext.AssignedTestAnswerOptionContext> answerOptions = answerOptionRepository
            .findAnswerOptionsByQuestionId(question.id()).stream()
            .sorted(ANSWER_OPTION_ORDER)
            .map(answerOption -> new AssignedTestContext.AssignedTestAnswerOptionContext(
                answerOption.id(),
                answerOption.body(),
                answerOption.answerOptionRole(),
                answerOption.displayOrder()
            ))
            .toList();
        return new AssignedTestContext.AssignedTestQuestionContext(
            question.id(),
            question.body(),
            question.questionType(),
            testQuestion.displayOrder(),
            answerOptions
        );
    }
}
