package com.vladislav.training.platform.audit.service;

import com.vladislav.training.platform.application.actor.TechnicalSystemActorAdapter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Разрешитель {@code DefaultSystemActorResolver}.
 */
@Service("canonicalSystemActorResolver")
@Transactional(readOnly = true)
public class DefaultSystemActorResolver implements SystemActorResolver {

    private final TechnicalSystemActorAdapter technicalSystemActorAdapter;

    public DefaultSystemActorResolver(TechnicalSystemActorAdapter technicalSystemActorAdapter) {
        this.technicalSystemActorAdapter = technicalSystemActorAdapter;
    }

    @Override
    public Long resolveSystemActorUserId() {
        return technicalSystemActorAdapter.resolveSystemActorUserId();
    }
}
