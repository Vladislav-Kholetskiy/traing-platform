package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequest;
import com.vladislav.training.platform.application.policy.CapabilityAdmissionRequestFactory;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelApplyRowResult;
import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlan;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlanOutcomeCode;
import com.vladislav.training.platform.integration.personnel.model.PersonnelPlannedMutation;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRow;
import com.vladislav.training.platform.integration.personnel.model.PersonnelRowIssue;
import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;
import com.vladislav.training.platform.integration.personnel.support.PersonnelWorkbookLimits;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
/**
 * Контракт сервиса {@code PersonnelApplyService}.
 */

@Service
public class PersonnelApplyService {

    private final ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider;
    private final PersonnelWorkbookParser personnelWorkbookParser;
    private final PersonnelRowInterpreter personnelRowInterpreter;
    private final PersonnelChangePlanner personnelChangePlanner;
    private final PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor;
    private final PersonnelImportAdmissionService personnelImportAdmissionService;
    private final CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory;

    @Autowired
    public PersonnelApplyService(
        ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider,
        PersonnelImportAdmissionService personnelImportAdmissionService,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory,
        PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor
    ) {
        this(
            currentStateReaderProvider,
            new ApachePoiPersonnelWorkbookParser(new PersonnelWorkbookLimits(1000)),
            new PersonnelRowInterpreter(new PersonnelPositionMappingService()),
            new PersonnelChangePlanner(),
            personnelOwnerMutationExecutor,
            personnelImportAdmissionService,
            capabilityAdmissionRequestFactory
        );
    }

    public PersonnelApplyService(
        ObjectProvider<PersonnelCurrentStateReader> currentStateReaderProvider,
        PersonnelWorkbookParser personnelWorkbookParser,
        PersonnelRowInterpreter personnelRowInterpreter,
        PersonnelChangePlanner personnelChangePlanner,
        PersonnelOwnerMutationExecutor personnelOwnerMutationExecutor,
        PersonnelImportAdmissionService personnelImportAdmissionService,
        CapabilityAdmissionRequestFactory capabilityAdmissionRequestFactory
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
        this.personnelChangePlanner = Objects.requireNonNull(
            personnelChangePlanner,
            "personnelChangePlanner must not be null"
        );
        this.personnelOwnerMutationExecutor = Objects.requireNonNull(
            personnelOwnerMutationExecutor,
            "personnelOwnerMutationExecutor must not be null"
        );
        this.personnelImportAdmissionService = Objects.requireNonNull(
            personnelImportAdmissionService,
            "personnelImportAdmissionService must not be null"
        );
        this.capabilityAdmissionRequestFactory = Objects.requireNonNull(
            capabilityAdmissionRequestFactory,
            "capabilityAdmissionRequestFactory must not be null"
        );
    }

    public PersonnelApplyResult apply(byte[] workbookBytes) {
        CapabilityAdmissionRequest admissionRequest = capabilityAdmissionRequestFactory.createPersonnelExcelApply();
        personnelImportAdmissionService.checkApplyAdmission(admissionRequest);

        PersonnelWorkbookParseResult parseResult = personnelWorkbookParser.parse(workbookBytes);
        if (parseResult.hasFatalIssues()) {
            return new PersonnelApplyResult(parseIssueResults(parseResult.issues()));
        }

        PersonnelCurrentStateReader currentStateReader = currentStateReaderProvider.getIfAvailable();
        if (currentStateReader == null) {
            throw new IllegalStateException("Personnel apply current-state reader is not configured");
        }

        Map<Integer, List<String>> rowIssues = indexNonFatalIssues(parseResult.issues());
        List<PersonnelApplyRowResult> rows = new ArrayList<>();
        for (PersonnelRow row : parseResult.rows()) {
            List<String> messages = rowIssues.get(row.rowNumber());
            if (messages != null && !messages.isEmpty()) {
                rows.add(failed(
                    row.rowNumber(),
                    row.employeeNumber(),
                    com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.FAIL_CLOSED.name(),
                    null,
                    messages
                ));
                continue;
            }
            rows.add(applyRow(row, currentStateReader));
        }
        return new PersonnelApplyResult(rows);
    }

    private PersonnelApplyRowResult applyRow(PersonnelRow row, PersonnelCurrentStateReader currentStateReader) {
        try {
            PersonnelBusinessIntent intent = personnelRowInterpreter.interpret(row);
            PersonnelPlan plan = personnelChangePlanner.plan(intent, currentStateReader.resolveIdentity(intent));
            if (plan.outcomeCode() == PersonnelPlanOutcomeCode.NO_CHANGE) {
                return new PersonnelApplyRowResult(
                    row.rowNumber(),
                    row.employeeNumber(),
                    "NO_CHANGE",
                    plan.decision().name(),
                    plan.targetUserStatus(),
                    List.of(),
                    List.of(),
                    null
                );
            }
            if (plan.outcomeCode() != PersonnelPlanOutcomeCode.PLANNED_CHANGES
                && plan.outcomeCode() != PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED) {
                return failed(
                    row.rowNumber(),
                    row.employeeNumber(),
                    plan.decision().name(),
                    plan.targetUserStatus(),
                    plan.issues().isEmpty() ? List.of(plan.outcomeCode().name()) : plan.issues()
                );
            }

            Long appliedUserId = personnelOwnerMutationExecutor.execute(intent, plan);
            return new PersonnelApplyRowResult(
                row.rowNumber(),
                row.employeeNumber(),
                "SUCCESS",
                plan.decision().name(),
                plan.targetUserStatus(),
                List.of(),
                plan.plannedMutations().stream()
                    .map(PersonnelPlannedMutation::mutationType)
                    .map(Enum::name)
                    .toList(),
                plan.outcomeCode() == PersonnelPlanOutcomeCode.CREATE_USER_REQUIRED ? appliedUserId : null
            );
        } catch (RuntimeException exception) {
            return failed(
                row.rowNumber(),
                row.employeeNumber(),
                com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.FAIL_CLOSED.name(),
                null,
                List.of(safeMessage(exception))
            );
        }
    }

    private List<PersonnelApplyRowResult> parseIssueResults(List<PersonnelRowIssue> issues) {
        return issues.stream()
            .map(issue -> new PersonnelApplyRowResult(
                issue.rowNumber(),
                null,
                "FAILED",
                com.vladislav.training.platform.integration.personnel.model.PersonnelRowDecision.FAIL_CLOSED.name(),
                null,
                List.of(issue.message()),
                List.of(),
                null
            ))
            .toList();
    }

    private Map<Integer, List<String>> indexNonFatalIssues(List<PersonnelRowIssue> issues) {
        return issues.stream()
            .filter(issue -> issue.rowNumber() != null)
            .collect(Collectors.toMap(
                PersonnelRowIssue::rowNumber,
                issue -> new ArrayList<>(List.of(issue.message())),
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                LinkedHashMap::new
            ));
    }

    private PersonnelApplyRowResult failed(
        Integer rowNumber,
        String employeeNumber,
        String decision,
        String targetUserStatus,
        List<String> issues
    ) {
        return new PersonnelApplyRowResult(
            rowNumber,
            employeeNumber,
            "FAILED",
            decision,
            targetUserStatus,
            issues,
            List.of(),
            null
        );
    }

    private String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }
}
