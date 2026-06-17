package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.content.domain.ContentStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Контракт репозитория {@code SpringDataCourseJpaRepository}.
 */
public interface SpringDataCourseJpaRepository extends JpaRepository<CourseEntity, Long> {
    List<CourseEntity> findAllByOrderByIdAsc();
    List<CourseEntity> findAllByStatusOrderByIdAsc(ContentStatus status);
}
