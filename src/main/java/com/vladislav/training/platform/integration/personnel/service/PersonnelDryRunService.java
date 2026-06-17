package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import java.util.List;
import java.util.Objects;

/**
 * Контракт сервиса {@code PersonnelDryRunService}.
 */
public class PersonnelDryRunService {

    private final PersonnelCurrentStateReader currentStateReader;
    private final PersonnelChangePlanner changePlanner;

    public PersonnelDryRunService(
        PersonnelCurrentStateReader currentStateReader,
        PersonnelChangePlanner changePlanner
    ) {
        this.currentStateReader = Objects.requireNonNull(currentStateReader, "currentStateReader must not be null");
        this.changePlanner = Objects.requireNonNull(changePlanner, "changePlanner must not be null");
    }

    public List<PersonnelPlan> plan(List<PersonnelBusinessIntent> intents) {
        Objects.requireNonNull(intents, "intents must not be null");
        return intents.stream()
            .map(intent -> changePlanner.plan(intent, currentStateReader.resolveIdentity(intent)))
            .toList();
    }
}
