package com.vladislav.training.platform.testing.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import com.vladislav.training.platform.testing.repository.UserAnswerItemRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaUserAnswerItemRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaUserAnswerItemRepositoryAdapter implements UserAnswerItemRepository {

    private final SpringDataUserAnswerItemJpaRepository repository;
    private final TestingPersistenceMapper mapper;

    public JpaUserAnswerItemRepositoryAdapter(
        SpringDataUserAnswerItemJpaRepository repository,
        TestingPersistenceMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public UserAnswerItem findUserAnswerItemById(Long userAnswerItemId) {
        return repository.findById(userAnswerItemId)
            .map(mapper::toDomain)
            .orElseThrow(() -> new NotFoundException("User answer item not found: " + userAnswerItemId));
    }

    @Override
    public List<UserAnswerItem> findUserAnswerItemsByUserAnswerId(Long userAnswerId) {
        return mapper.toUserAnswerItems(repository.findAllByUserAnswerIdOrderByIdAsc(userAnswerId));
    }

    @Override
    @Transactional
    public UserAnswerItem saveUserAnswerItem(UserAnswerItem userAnswerItem) {
        try {
            return mapper.toDomain(repository.save(mapper.toEntity(userAnswerItem)));
        } catch (DataIntegrityViolationException exception) {
            throw new PersistenceConstraintViolationException("Failed to persist user_answer_item", exception);
        }
    }

    @Override
    @Transactional
    public void deleteUserAnswerItem(Long userAnswerItemId) {
        repository.deleteById(userAnswerItemId);
    }

    @Override
    @Transactional
    public void deleteUserAnswerItemsByUserAnswerId(Long userAnswerId) {
        repository.deleteAllByUserAnswerId(userAnswerId);
    }
}
