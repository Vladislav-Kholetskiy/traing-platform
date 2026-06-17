package com.vladislav.training.platform.testing.repository;

import com.vladislav.training.platform.testing.domain.UserAnswerItem;
import java.util.List;

/**
 * Контракт репозитория {@code UserAnswerItemRepository}.
 */
public interface UserAnswerItemRepository {

    UserAnswerItem findUserAnswerItemById(Long userAnswerItemId);

    List<UserAnswerItem> findUserAnswerItemsByUserAnswerId(Long userAnswerId);

    UserAnswerItem saveUserAnswerItem(UserAnswerItem userAnswerItem);

    void deleteUserAnswerItem(Long userAnswerItemId);

    void deleteUserAnswerItemsByUserAnswerId(Long userAnswerId);
}
