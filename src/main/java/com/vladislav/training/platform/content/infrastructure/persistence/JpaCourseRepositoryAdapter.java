package com.vladislav.training.platform.content.infrastructure.persistence;

import com.vladislav.training.platform.common.exception.NotFoundException;
import com.vladislav.training.platform.common.exception.PersistenceConstraintViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Адаптер репозитория {@code JpaCourseRepositoryAdapter}.
 */
@Repository
@Transactional(readOnly = true)
public class JpaCourseRepositoryAdapter implements CourseRepository {
    private final SpringDataCourseJpaRepository repository; private final ContentMapper mapper;
    public JpaCourseRepositoryAdapter(SpringDataCourseJpaRepository repository, ContentMapper mapper){this.repository=repository;this.mapper=mapper;}
    @Override public Course findCourseById(Long courseId){ return repository.findById(courseId).map(mapper::toDomain).orElseThrow(() -> new NotFoundException("Course not found: "+courseId)); }
    @Override public List<Course> findAllCourses(){ return mapper.toCourses(repository.findAllByOrderByIdAsc()); }
    @Override public List<Course> findCoursesByStatus(ContentStatus status){ return mapper.toCourses(repository.findAllByStatusOrderByIdAsc(status)); }
    @Override @Transactional public Course saveCourse(Course course){ try { return mapper.toDomain(repository.save(mapper.toEntity(course))); } catch (DataIntegrityViolationException ex){ throw new PersistenceConstraintViolationException("Failed to persist course", ex); } }
}
