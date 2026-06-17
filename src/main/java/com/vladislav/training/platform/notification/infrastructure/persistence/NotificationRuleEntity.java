package com.vladislav.training.platform.notification.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
/**
 * JPA-сущность {@code NotificationRuleEntity}.
 */

@Entity
@Table(name = "notification_rule")
public class NotificationRuleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rule_code", nullable = false)
    private String ruleCode;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "notification_type", nullable = false)
    private String notificationType;

    @Column(name = "channel_code", nullable = false)
    private String channelCode;

    @Column(name = "is_enabled", nullable = false)
    private boolean enabled;

    @Column(name = "days_before_deadline")
    private Integer daysBeforeDeadline;

    @Column(name = "repeat_interval_days")
    private Integer repeatIntervalDays;

    @Column(name = "trigger_mode")
    private String triggerMode;

    @Column(name = "recipient_scope_code")
    private String recipientScopeCode;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationRuleEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getChannelCode() {
        return channelCode;
    }

    public void setChannelCode(String channelCode) {
        this.channelCode = channelCode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getDaysBeforeDeadline() {
        return daysBeforeDeadline;
    }

    public void setDaysBeforeDeadline(Integer daysBeforeDeadline) {
        this.daysBeforeDeadline = daysBeforeDeadline;
    }

    public Integer getRepeatIntervalDays() {
        return repeatIntervalDays;
    }

    public void setRepeatIntervalDays(Integer repeatIntervalDays) {
        this.repeatIntervalDays = repeatIntervalDays;
    }

    public String getTriggerMode() {
        return triggerMode;
    }

    public void setTriggerMode(String triggerMode) {
        this.triggerMode = triggerMode;
    }

    public String getRecipientScopeCode() {
        return recipientScopeCode;
    }

    public void setRecipientScopeCode(String recipientScopeCode) {
        this.recipientScopeCode = recipientScopeCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
