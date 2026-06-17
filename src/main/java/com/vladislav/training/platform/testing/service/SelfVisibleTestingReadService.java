package com.vladislav.training.platform.testing.service;

import com.vladislav.training.platform.access.service.AccessPolicyQueryContext;
import com.vladislav.training.platform.access.service.AccessPolicyQueryContextResolver;
import com.vladislav.training.platform.access.service.AccessReadArea;
import com.vladislav.training.platform.access.service.AccessReadType;
import com.vladislav.training.platform.access.service.AccessSpecificationPolicy;
import com.vladislav.training.platform.common.exception.PolicyViolationException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Контракт сервиса {@code SelfVisibleTestingReadService}.
 */
@Service
@Transactional(readOnly = true)
public class SelfVisibleTestingReadService {

    private final SelfVisibleTestingProjectionReader selfVisibleTestingProjectionReader;
    private final AccessSpecificationPolicy accessSpecificationPolicy;
    private final AccessPolicyQueryContextResolver contextResolver;

    public SelfVisibleTestingReadService(
        SelfVisibleTestingProjectionReader selfVisibleTestingProjectionReader,
        AccessSpecificationPolicy accessSpecificationPolicy,
        AccessPolicyQueryContextResolver contextResolver
    ) {
        this.selfVisibleTestingProjectionReader = selfVisibleTestingProjectionReader;
        this.accessSpecificationPolicy = accessSpecificationPolicy;
        this.contextResolver = contextResolver;
    }

    public List<SelfVisibleTestCatalogEntryReadModel> findSelfVisibleTests() {
        ensurePolicyAllowed(AccessReadType.LIST);
        return selfVisibleTestingProjectionReader.findSelfVisibleTests();
    }

    public SelfVisibleTestReadModel findSelfVisibleTestById(Long testId) {
        ensurePolicyAllowed(AccessReadType.DETAIL);
        return selfVisibleTestingProjectionReader.findSelfVisibleTestById(testId);
    }

    public SelfVisibleTopicReadModel findSelfVisibleTopicById(Long topicId) {
        ensurePolicyAllowed(AccessReadType.DETAIL);
        return selfVisibleTestingProjectionReader.findSelfVisibleTopicById(topicId);
    }

    private void ensurePolicyAllowed(AccessReadType readType) {
        AccessPolicyQueryContext context = contextResolver.resolveActorSelfScope(
            AccessReadArea.SELF_VISIBLE_TESTING,
            readType,
            "self_visible_testing"
        );
        if (!accessSpecificationPolicy.canRead(context)) {
            throw new PolicyViolationException(
                "ACTOR_NOT_AUTHORIZED",
                "Actor is not authorized to read self-visible testing data"
            );
        }
    }
}
