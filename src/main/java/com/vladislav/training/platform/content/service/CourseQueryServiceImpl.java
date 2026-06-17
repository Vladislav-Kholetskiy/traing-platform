package com.vladislav.training.platform.content.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import com.vladislav.training.platform.content.domain.ContentStatus;
import com.vladislav.training.platform.content.domain.Course;
import com.vladislav.training.platform.content.repository.CourseRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация сервиса чтения {@code CourseQueryServiceImpl}.
 */
@Service
@Transactional(readOnly = true)
public class CourseQueryServiceImpl implements CourseQueryService {

    private final CourseRepository courseRepository;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    public CourseQueryServiceImpl(
        CourseRepository courseRepository,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.courseRepository = courseRepository;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
    }

    @Override
    public Course findCourseById(Long courseId) {
        ensureReadAllowed(AccessReadArea.CONTENT_AUTHORING, AccessReadType.DETAIL, "course");
        return courseRepository.findCourseById(courseId);
    }

    @Override
    public List<Course> findAllCourses() {
        ensureReadAllowed(AccessReadArea.CONTENT_AUTHORING, AccessReadType.LIST, "course");
        return courseRepository.findAllCourses();
    }

    @Override
    public List<Course> findCoursesByStatus(ContentStatus status) {
        ensureReadAllowed(AccessReadArea.CONTENT_AUTHORING, AccessReadType.LIST, "course");
        return courseRepository.findCoursesByStatus(status);
    }

    private void ensureReadAllowed(AccessReadArea contour, AccessReadType readType, String family) {
        if (!accessSpecificationPolicy.canRead(contextResolver.resolve(contour, readType, family))) {
            throw new PolicyViolationException("ACTOR_NOT_AUTHORIZED", "Actor is not authorized to read content data");
        }
    }
}
