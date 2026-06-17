package com.vladislav.training.platform.integration.personnel.service;

import com.vladislav.training.platform.integration.personnel.model.PersonnelBusinessIntent;
import com.vladislav.training.platform.integration.personnel.model.PersonnelIdentityResolution;

/**
 * Читатель {@code PersonnelCurrentStateReader}.
 */
public interface PersonnelCurrentStateReader {

    PersonnelIdentityResolution resolveIdentity(PersonnelBusinessIntent intent);
}
