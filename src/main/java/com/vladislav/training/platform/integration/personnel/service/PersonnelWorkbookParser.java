package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelWorkbookParseResult;

/**
 * Интерфейс {@code PersonnelWorkbookParser}.
 */
public interface PersonnelWorkbookParser {

    PersonnelWorkbookParseResult parse(byte[] workbookBytes);
}
