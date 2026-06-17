package com.vladislav.training.platform.application.policy;

/**
 * Класс {@code CapabilityOperationCodes}.
 */
public final class CapabilityOperationCodes {

    public static final String USERORG_USER_CREATE = CapabilityOperationCode.USERORG_USER_CREATE.code();
    public static final String USERORG_USER_UPDATE = CapabilityOperationCode.USERORG_USER_UPDATE.code();
    public static final String USERORG_USER_DEACTIVATE = CapabilityOperationCode.USERORG_USER_DEACTIVATE.code();
    public static final String USERORG_USER_ROLE_ASSIGN = CapabilityOperationCode.USERORG_USER_ROLE_ASSIGN.code();
    public static final String USERORG_USER_ROLE_CLOSE = CapabilityOperationCode.USERORG_USER_ROLE_CLOSE.code();
    public static final String USERORG_USER_ORGANIZATION_ASSIGN = CapabilityOperationCode.USERORG_USER_ORGANIZATION_ASSIGN.code();
    public static final String USERORG_USER_ORGANIZATION_CLOSE = CapabilityOperationCode.USERORG_USER_ORGANIZATION_CLOSE.code();
    public static final String USERORG_USER_PRIMARY_HOME_REPLACE = CapabilityOperationCode.USERORG_USER_PRIMARY_HOME_REPLACE.code();

    public static final String USERORG_ORGANIZATIONAL_UNIT_TYPE_CREATE = CapabilityOperationCode.USERORG_ORGANIZATIONAL_UNIT_TYPE_CREATE.code();
    public static final String USERORG_ORGANIZATIONAL_UNIT_TYPE_UPDATE = CapabilityOperationCode.USERORG_ORGANIZATIONAL_UNIT_TYPE_UPDATE.code();
    public static final String USERORG_ORGANIZATIONAL_UNIT_CREATE = CapabilityOperationCode.USERORG_ORGANIZATIONAL_UNIT_CREATE.code();
    public static final String USERORG_ORGANIZATIONAL_UNIT_UPDATE = CapabilityOperationCode.USERORG_ORGANIZATIONAL_UNIT_UPDATE.code();
    public static final String USERORG_ORGANIZATIONAL_UNIT_MOVE = CapabilityOperationCode.USERORG_ORGANIZATIONAL_UNIT_MOVE.code();
    public static final String USERORG_ORGANIZATIONAL_UNIT_ARCHIVE = CapabilityOperationCode.USERORG_ORGANIZATIONAL_UNIT_ARCHIVE.code();

    public static final String ACCESS_USER_ACCESS_AREA_ASSIGN = CapabilityOperationCode.ACCESS_USER_ACCESS_AREA_ASSIGN.code();
    public static final String ACCESS_USER_ACCESS_AREA_CLOSE = CapabilityOperationCode.ACCESS_USER_ACCESS_AREA_CLOSE.code();
    public static final String ACCESS_MANAGEMENT_RELATION_ASSIGN = CapabilityOperationCode.ACCESS_MANAGEMENT_RELATION_ASSIGN.code();
    public static final String ACCESS_MANAGEMENT_RELATION_CLOSE = CapabilityOperationCode.ACCESS_MANAGEMENT_RELATION_CLOSE.code();
    public static final String ACCESS_TEMPORARY_ROLE_ASSIGN = CapabilityOperationCode.ACCESS_TEMPORARY_ROLE_ASSIGN.code();
    public static final String ACCESS_TEMPORARY_ROLE_CLOSE = CapabilityOperationCode.ACCESS_TEMPORARY_ROLE_CLOSE.code();
    public static final String ACCESS_TEMPORARY_ACCESS_ASSIGN = CapabilityOperationCode.ACCESS_TEMPORARY_ACCESS_ASSIGN.code();
    public static final String ACCESS_TEMPORARY_ACCESS_CLOSE = CapabilityOperationCode.ACCESS_TEMPORARY_ACCESS_CLOSE.code();
    public static final String ACCESS_TEMPORARY_MANAGEMENT_ASSIGN = CapabilityOperationCode.ACCESS_TEMPORARY_MANAGEMENT_ASSIGN.code();
    public static final String ACCESS_TEMPORARY_MANAGEMENT_CLOSE = CapabilityOperationCode.ACCESS_TEMPORARY_MANAGEMENT_CLOSE.code();

    public static final String CONTENT_DRAFT_CREATE = CapabilityOperationCode.CONTENT_DRAFT_CREATE.code();
    public static final String CONTENT_DRAFT_UPDATE = CapabilityOperationCode.CONTENT_DRAFT_UPDATE.code();
    public static final String CONTENT_PUBLISH = CapabilityOperationCode.CONTENT_PUBLISH.code();
    public static final String CONTENT_ARCHIVE = CapabilityOperationCode.CONTENT_ARCHIVE.code();
    public static final String CONTENT_FINAL_ASSIGN = CapabilityOperationCode.CONTENT_FINAL_ASSIGN.code();
    public static final String CONTENT_FINAL_REPLACE = CapabilityOperationCode.CONTENT_FINAL_REPLACE.code();
    public static final String CONTENT_FINAL_CLEAR = CapabilityOperationCode.CONTENT_FINAL_CLEAR.code();

    public static final String ASSIGNMENT_CAMPAIGN_LAUNCH = CapabilityOperationCode.ASSIGNMENT_CAMPAIGN_LAUNCH.code();
    public static final String ASSIGNMENT_CANCEL = CapabilityOperationCode.ASSIGNMENT_CANCEL.code();
    public static final String ASSIGNMENT_DEADLINE_EXTEND = CapabilityOperationCode.ASSIGNMENT_DEADLINE_EXTEND.code();
    public static final String ASSIGNMENT_REPLACE_WITH_NEW = CapabilityOperationCode.ASSIGNMENT_REPLACE_WITH_NEW.code();
    public static final String TESTING_ASSIGNED_ATTEMPT_START = CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_START.code();
    public static final String TESTING_SELF_ATTEMPT_START = CapabilityOperationCode.TESTING_SELF_ATTEMPT_START.code();
    public static final String TESTING_SELF_ATTEMPT_CONTINUE =
        CapabilityOperationCode.TESTING_SELF_ATTEMPT_CONTINUE.code();
    public static final String TESTING_ASSIGNED_ATTEMPT_SUBMIT =
        CapabilityOperationCode.TESTING_ASSIGNED_ATTEMPT_SUBMIT.code();
    public static final String TESTING_SELF_ATTEMPT_SUBMIT =
        CapabilityOperationCode.TESTING_SELF_ATTEMPT_SUBMIT.code();
    public static final String TESTING_SELF_ATTEMPT_ABANDON =
        CapabilityOperationCode.TESTING_SELF_ATTEMPT_ABANDON.code();
    public static final String TESTING_ASSIGNED_ANSWER_MUTATION =
        CapabilityOperationCode.TESTING_ASSIGNED_ANSWER_MUTATION.code();
    public static final String TESTING_SELF_ANSWER_MUTATION =
        CapabilityOperationCode.TESTING_SELF_ANSWER_MUTATION.code();
    public static final String NOTIFICATION_RULE_CREATE = "NOTIFICATION_RULE_CREATE";
    public static final String NOTIFICATION_RULE_UPDATE = "NOTIFICATION_RULE_UPDATE";
    public static final String NOTIFICATION_RULE_ENABLE = "NOTIFICATION_RULE_ENABLE";
    public static final String NOTIFICATION_RULE_DISABLE = "NOTIFICATION_RULE_DISABLE";
    public static final String PERSONNEL_EXCEL_DRY_RUN = CapabilityOperationCode.PERSONNEL_EXCEL_DRY_RUN.code();
    public static final String PERSONNEL_EXCEL_APPLY = CapabilityOperationCode.PERSONNEL_EXCEL_APPLY.code();
    public static final String IMPORT_JOB_LAUNCH = "IMPORT_JOB_LAUNCH";
    public static final String IMPORT_ITEM_REVIEW_APPLY = "IMPORT_ITEM_REVIEW_APPLY";
    public static final String IMPORT_ITEM_REVIEW_REJECT = "IMPORT_ITEM_REVIEW_REJECT";

    private CapabilityOperationCodes() {
    }
}
