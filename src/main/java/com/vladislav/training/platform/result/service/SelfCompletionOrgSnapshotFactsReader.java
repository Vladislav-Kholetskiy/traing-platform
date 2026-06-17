package com.vladislav.training.platform.result.service;

import java.util.Objects;
/**
 * Читатель {@code SelfCompletionOrgSnapshotFactsReader}.
 */
interface SelfCompletionOrgSnapshotFactsReader {

    SelfCompletionOrgSnapshotFacts readSelfCompletionOrgSnapshotFacts(Long testAttemptId);

    record SelfCompletionOrgSnapshotFacts(
        Long organizationalUnitIdSnapshot,
        String organizationalPathSnapshot
    ) {

        public SelfCompletionOrgSnapshotFacts {
            Objects.requireNonNull(organizationalUnitIdSnapshot, "organizationalUnitIdSnapshot must not be null");
            Objects.requireNonNull(organizationalPathSnapshot, "organizationalPathSnapshot must not be null");
        }
    }
}
