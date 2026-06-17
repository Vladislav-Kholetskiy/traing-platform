package com.vladislav.training.platform.analytics.service;

import com.vladislav.training.platform.common.exception.NotFoundException;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;

public class AnalyticsCampaignAggregateSourceReaderImpl implements AnalyticsCampaignAggregateSourceReader {

    private final JdbcTemplate jdbcTemplate;

    public AnalyticsCampaignAggregateSourceReaderImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    @Override
    public AnalyticsCampaignAggregateSourceFacts readCampaignAggregateSourceFacts(Long campaignId) {
        Objects.requireNonNull(campaignId, "campaignId must not be null");
        return jdbcTemplate.query(
            """
                select
                    c.id as campaign_id,
                    coalesce((
                        select count(*)
                        from assignment_campaign_recipient_snapshot rs
                        where rs.campaign_id = c.id
                    ), 0) as recipient_snapshot_count,
                    coalesce((
                        select count(*)
                        from assignment a
                        where a.campaign_id = c.id
                          and a.status <> 'CANCELLED'
                    ), 0) as non_cancelled_assignments_from_campaign_snapshot,
                    coalesce((
                        select count(*)
                        from assignment a
                        where a.campaign_id = c.id
                          and a.status = 'COMPLETED'
                    ), 0) as completed_assignments,
                    coalesce((
                        select count(*)
                        from assignment a
                        where a.campaign_id = c.id
                          and a.status = 'OVERDUE'
                    ), 0) as overdue_assignments,
                    coalesce((
                        select count(*)
                        from assignment a
                        where a.campaign_id = c.id
                          and a.status in ('ASSIGNED', 'OVERDUE', 'COMPLETED')
                    ), 0) as non_cancelled_active_pool,
                    coalesce((
                        select count(*)
                        from assignment a
                        where a.campaign_id = c.id
                          and a.status = 'CANCELLED'
                    ), 0) as cancelled_assignments
                from assignment_campaign c
                where c.id = ?
                """,
            resultSet -> {
                if (!resultSet.next()) {
                    throw new NotFoundException("Assignment campaign not found for analytics aggregate refresh: " + campaignId);
                }
                return new AnalyticsCampaignAggregateSourceFacts(
                    resultSet.getLong("campaign_id"),
                    resultSet.getInt("recipient_snapshot_count"),
                    resultSet.getInt("non_cancelled_assignments_from_campaign_snapshot"),
                    resultSet.getInt("completed_assignments"),
                    resultSet.getInt("overdue_assignments"),
                    resultSet.getInt("non_cancelled_active_pool"),
                    resultSet.getInt("cancelled_assignments")
                );
            },
            campaignId
        );
    }

    @Override
    public List<Long> readAllCampaignIds() {
        return jdbcTemplate.query(
            """
                select c.id
                from assignment_campaign c
                order by c.id asc
                """,
            (resultSet, rowNum) -> resultSet.getLong(1)
        );
    }
}
