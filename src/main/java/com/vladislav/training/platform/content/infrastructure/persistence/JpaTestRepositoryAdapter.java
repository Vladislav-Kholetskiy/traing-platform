package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Test;
import com.vladislav.training.platform.content.domain.TestType;
import com.vladislav.training.platform.content.repository.TestRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaTestRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaTestRepositoryAdapter implements TestRepository {

    private final SpringDataTestJpaRepository repository;
    private final ContentMapper mapper;

    public JpaTestRepositoryAdapter(SpringDataTestJpaRepository repository, ContentMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public Test findTestById(Long testId) {
        return repository.findById(testId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Test not found: " + testId));
    }

    @Override
    @Transactional
    public Test lockTestById(Long testId) {
        return repository.findByIdForUpdate(testId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("Test not found: " + testId));
    }

    @Override
    public List<Test> findTestsByTopicId(Long topicId) {
        return mapper.toTests(repository.findAllByTopicIdOrderBySortOrderAscIdAsc(topicId));
    }

    @Override
    public List<Test> findTestsByTopicIdAndStatus(Long topicId, ContentStatus status) {
        return mapper.toTests(repository.findAllByTopicIdAndStatusOrderBySortOrderAscIdAsc(topicId, status));
    }

    @Override
    public List<Test> findTestsByTopicIdAndStatusAndType(Long topicId, ContentStatus status, TestType testType) {
        return mapper.toTests(repository.findAllByTopicIdAndStatusAndTestTypeOrderBySortOrderAscIdAsc(topicId, status, testType));
    }

    @Override
    public List<Test> findEligibleFinalControlTestsByTopicId(Long topicId) {
        return mapper.toTests(repository.findAllByTopicIdAndStatusAndTestTypeOrderBySortOrderAscIdAsc(
            topicId,
            ContentStatus.PUBLISHED,
            TestType.CONTROL
        ));
    }

    @Override
    public Optional<Test> findActiveFinalTestByTopicId(Long topicId) {
        return repository.findByTopicIdAndActiveFinalForTopicTrue(topicId).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public Optional<Test> findActiveFinalTestByTopicIdForUpdate(Long topicId) {
        return repository.findActiveFinalByTopicIdForUpdate(topicId).map(mapper::toDomain);
    }

    @Override
    public boolean existsNonArchivedByTopicId(Long topicId) {
        return repository.existsByTopicIdAndStatusNot(topicId, ContentStatus.ARCHIVED);
    }

    @Override
    @Transactional
    public Test saveTest(Test test) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(test)));
        } catch (DataIntegrityViolationException ex) {
            throw new PersistenceConstraintViolationException("Failed to persist test", ex);
        }
    }
}
