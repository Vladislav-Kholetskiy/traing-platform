package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;
import com.vladislav.training.platform.integration.personnel.support.PersonnelWorkbookLimits;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * Класс {@code PersonnelWorkbookDryRunFacade}.
 */

@Service
public class PersonnelWorkbookDryRunFacade {

    private final ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider;
    private final PersonnelWorkbookParser personnelWorkbookParser;
    private final PersonnelRowInterpreter personnelRowInterpreter;

    @Autowired
    public PersonnelWorkbookDryRunFacade(ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider) {
        this(
            currentStateReaderProvider,
            new ApachePoiPersonnelWorkbookParser(new PersonnelWorkbookLimits(1000)),
            new PersonnelRowInterpreter(new PersonnelPositionMappingService())
        );
    }

    PersonnelWorkbookDryRunFacade(
        ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider,
        PersonnelWorkbookParser personnelWorkbookParser,
        PersonnelRowInterpreter personnelRowInterpreter
    ) {
        this.currentStateReaderProvider = Objects.requireNonNull(
            currentStateReaderProvider,
            "currentStateReaderProvider must not be null"
        );
        this.personnelWorkbookParser = Objects.requireNonNull(
            personnelWorkbookParser,
            "personnelWorkbookParser must not be null"
        );
        this.personnelRowInterpreter = Objects.requireNonNull(
            personnelRowInterpreter,
            "personnelRowInterpreter must not be null"
        );
    }

    public List<PersonnelPlan> dryRun(byte[] workbookBytes) {
        PersonnelWorkbookParseResult parseResult = personnelWorkbookParser.parse(workbookBytes);
        if (parseResult.hasFatalIssues()) {
            throw new IllegalArgumentException(parseResult.issues().getFirst().message());
        }
        if (!parseResult.issues().isEmpty()) {
            throw new IllegalArgumentException(parseResult.issues().getFirst().message());
        }

        PersonnelCurrentStateReader currentStateReader = currentStateReaderProvider.getIfAvailable();
        if (currentStateReader == null) {
            throw new IllegalStateException("Personnel dry-run current-state reader is not configured");
        }

        PersonnelDryRunService personnelDryRunService =
            new PersonnelDryRunService(currentStateReader, new PersonnelChangePlanner());
        List<com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent> intents =
            parseResult.rows().stream()
                .map(this::interpret)
                .toList();
        return personnelDryRunService.plan(intents);
    }

    private com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent interpret(PersonnelRow row) {
        return personnelRowInterpreter.interpret(row);
    }
}
