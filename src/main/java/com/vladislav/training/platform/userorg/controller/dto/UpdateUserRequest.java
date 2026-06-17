package com.vladislav.training.platform.userorg.controller.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = false)
public final class UpdateUserRequest {

    @NotBlank
    private String lastName;

    @NotBlank
    private String firstName;

    private String middleName;

    private final Map<String, Object> unsupportedFields = new LinkedHashMap<>();

    public String lastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String firstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String middleName() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    @JsonAnySetter
    public void captureUnsupportedField(String fieldName, Object value) {
        unsupportedFields.put(fieldName, value);
    }

    @AssertTrue(message = "Update user request contains unsupported fields")
    public boolean hasOnlySupportedFields() {
        return unsupportedFields.isEmpty();
    }
}
