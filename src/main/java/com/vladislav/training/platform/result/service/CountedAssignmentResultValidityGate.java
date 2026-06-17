package com.vladislav.training.platform.result.service;

import com.vladislav.training.platform.result.domain.Result;
/**
 * Интерфейс {@code CountedAssignmentResultValidityGate}.
 */
public interface CountedAssignmentResultValidityGate {

    boolean allowsAssignmentCountedHandoff(Result materializedResult);
}
