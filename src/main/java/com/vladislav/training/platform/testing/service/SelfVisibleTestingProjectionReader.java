package com.vladislav.training.platform.testing.service;

import java.util.List;

/**
 * Читатель {@code SelfVisibleTestingProjectionReader}.
 */
public interface SelfVisibleTestingProjectionReader {

    List<SelfVisibleTestCatalogEntryReadModel> findSelfVisibleTests();

    SelfVisibleTestReadModel findSelfVisibleTestById(Long testId);

    SelfVisibleTopicReadModel findSelfVisibleTopicById(Long topicId);
}
