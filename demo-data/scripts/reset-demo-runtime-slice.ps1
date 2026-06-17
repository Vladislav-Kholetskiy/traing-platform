param(
    [string]$DatabaseName = "training_platform_demo_fullcycle",
    [string]$PostgresContainer = "training_platform_postgres",
    [string]$PostgresUser = "postgres"
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$allowedDatabaseName = "training_platform_demo_fullcycle"

if ([string]::IsNullOrWhiteSpace($DatabaseName)) {
    throw "DatabaseName must not be blank."
}

if ($DatabaseName -ne $allowedDatabaseName -or $DatabaseName -notmatch 'demo') {
    throw "Refusing to reset runtime slice for non-allowlisted database '$DatabaseName'. Allowed database: '$allowedDatabaseName'."
}

function Invoke-PsqlText {
    param(
        [Parameter(Mandatory = $true)][string]$Sql
    )

    $tempFile = [System.IO.Path]::GetTempFileName()
    try {
        Set-Content -LiteralPath $tempFile -Value $Sql -Encoding UTF8
        $stdin = Get-Content -Raw -LiteralPath $tempFile
        return $stdin | docker exec -i $PostgresContainer psql -U $PostgresUser -d $DatabaseName -v ON_ERROR_STOP=1 -P pager=off -At -F '|'
    } finally {
        Remove-Item -LiteralPath $tempFile -Force -ErrorAction SilentlyContinue
    }
}

function Write-CountSnapshot {
    param(
        [Parameter(Mandatory = $true)][string]$Label
    )

    $sql = @"
select 'analytics_campaign_aggregate', count(*) from analytics_campaign_aggregate
union all select 'analytics_department_topic_aggregate', count(*) from analytics_department_topic_aggregate
union all select 'analytics_question_aggregate', count(*) from analytics_question_aggregate
union all select 'analytics_user_topic_aggregate', count(*) from analytics_user_topic_aggregate
union all select 'result_answer_option_snapshot', count(*) from result_answer_option_snapshot
union all select 'result_question_snapshot', count(*) from result_question_snapshot
union all select 'result', count(*) from result
union all select 'user_answer_item', count(*) from user_answer_item
union all select 'user_answer', count(*) from user_answer
union all select 'test_attempt', count(*) from test_attempt
union all select 'assignment_test', count(*) from assignment_test
union all select 'assignment', count(*) from assignment
union all select 'assignment_campaign_recipient_snapshot', count(*) from assignment_campaign_recipient_snapshot
union all select 'assignment_campaign_course', count(*) from assignment_campaign_course
union all select 'assignment_campaign', count(*) from assignment_campaign
union all select 'test_question', count(*) from test_question
union all select 'answer_option', count(*) from answer_option
union all select 'test', count(*) from test
union all select 'question', count(*) from question
union all select 'material', count(*) from material
union all select 'topic', count(*) from topic
union all select 'course', count(*) from course
order by 1;
"@

    Write-Host ""
    Write-Host "${Label}:" -ForegroundColor Cyan
    foreach ($line in (Invoke-PsqlText -Sql $sql)) {
        $parts = $line -split '\|', 2
        if ($parts.Count -eq 2) {
            Write-Host ("  {0} = {1}" -f $parts[0], $parts[1]) -ForegroundColor DarkCyan
        }
    }
}

Write-Host "Resetting demo-generated runtime slice in '$DatabaseName' only." -ForegroundColor Yellow
Write-CountSnapshot -Label "Counts before cleanup"

$cleanupSql = @"
begin;
set local session_replication_role = replica;

delete from analytics_question_aggregate;
delete from analytics_department_topic_aggregate;
delete from analytics_user_topic_aggregate;
delete from analytics_campaign_aggregate;
delete from result_answer_option_snapshot;
delete from result_question_snapshot;
delete from result;
delete from user_answer_item;
delete from user_answer;
delete from test_attempt;
delete from assignment_test;
delete from assignment;
delete from assignment_campaign_recipient_snapshot;
delete from assignment_campaign_course;
delete from assignment_campaign;
delete from test_question;
delete from answer_option;
delete from test;
delete from question;
delete from material;
delete from topic;
delete from course;
commit;
"@

[void](Invoke-PsqlText -Sql $cleanupSql)
Write-CountSnapshot -Label "Counts after cleanup"
