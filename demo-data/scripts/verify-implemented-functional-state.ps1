param(
    [string]$DatabaseName = "training_platform_demo_fullcycle",
    [string]$PostgresContainer = "training_platform_postgres",
    [string]$PostgresUser = "postgres"
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$allowedDatabaseName = "training_platform_demo_fullcycle"

if ($DatabaseName -ne $allowedDatabaseName -or $DatabaseName -notmatch 'demo') {
    throw "Refusing to verify non-allowlisted database '$DatabaseName'. Allowed database: '$allowedDatabaseName'."
}

$script:FailureCount = 0
$script:WarnCount = 0

function Invoke-PsqlScalar {
    param(
        [Parameter(Mandatory = $true)][string]$Sql
    )

    $result = docker exec $PostgresContainer psql -U $PostgresUser -d $DatabaseName -v ON_ERROR_STOP=1 -P pager=off -At -c $Sql
    return ($result | Select-Object -First 1).ToString().Trim()
}

function Write-Pass {
    param([string]$Message)
    Write-Host ("PASS  {0}" -f $Message) -ForegroundColor Green
}

function Write-Fail {
    param([string]$Message)
    $script:FailureCount++
    Write-Host ("FAIL  {0}" -f $Message) -ForegroundColor Red
}

function Write-Warn {
    param([string]$Message)
    $script:WarnCount++
    Write-Host ("WARN  {0}" -f $Message) -ForegroundColor Yellow
}

function Assert-Equals {
    param(
        [string]$Label,
        [string]$Sql,
        [string]$Expected
    )

    $actual = Invoke-PsqlScalar -Sql $Sql
    if ($actual -eq $Expected) {
        Write-Pass ("{0}: expected {1}, got {2}" -f $Label, $Expected, $actual)
    } else {
        Write-Fail ("{0}: expected {1}, got {2}" -f $Label, $Expected, $actual)
    }
}

function Assert-GreaterThan {
    param(
        [string]$Label,
        [string]$Sql,
        [int]$Threshold
    )

    $actual = [int](Invoke-PsqlScalar -Sql $Sql)
    if ($actual -gt $Threshold) {
        Write-Pass ("{0}: {1} > {2}" -f $Label, $actual, $Threshold)
    } else {
        Write-Fail ("{0}: expected > {1}, got {2}" -f $Label, $Threshold, $actual)
    }
}

function Assert-Zero {
    param(
        [string]$Label,
        [string]$Sql
    )

    Assert-Equals -Label $Label -Sql $Sql -Expected "0"
}

Write-Host "Read-only verification for implemented functional demo state on '$DatabaseName'." -ForegroundColor Cyan

Write-Host ""
Write-Host "Counts" -ForegroundColor Cyan
Assert-Equals -Label "organizational_unit_type" -Sql "select count(*) from organizational_unit_type;" -Expected "6"
Assert-Equals -Label "organizational_unit" -Sql "select count(*) from organizational_unit;" -Expected "13"
Assert-Equals -Label "app_user" -Sql "select count(*) from app_user;" -Expected "52"
Assert-Equals -Label "user_organization_assignment" -Sql "select count(*) from user_organization_assignment;" -Expected "51"
Assert-Equals -Label "user_role_assignment" -Sql "select count(*) from user_role_assignment;" -Expected "103"
Assert-Equals -Label "course" -Sql "select count(*) from course;" -Expected "3"
Assert-Equals -Label "topic" -Sql "select count(*) from topic;" -Expected "8"
Assert-Equals -Label "material" -Sql "select count(*) from material;" -Expected "27"
Assert-Equals -Label "question" -Sql "select count(*) from question;" -Expected "85"
Assert-Equals -Label "answer_option" -Sql "select count(*) from answer_option;" -Expected "340"
Assert-Equals -Label "test" -Sql "select count(*) from test;" -Expected "13"
Assert-Equals -Label "test_question" -Sql "select count(*) from test_question;" -Expected "91"
Assert-Equals -Label "assignment_campaign" -Sql "select count(*) from assignment_campaign;" -Expected "3"
Assert-Equals -Label "assignment" -Sql "select count(*) from assignment;" -Expected "24"
Assert-Equals -Label "assignment_test" -Sql "select count(*) from assignment_test;" -Expected "72"
Assert-GreaterThan -Label "test_attempt" -Sql "select count(*) from test_attempt;" -Threshold 0
Assert-GreaterThan -Label "result" -Sql "select count(*) from result;" -Threshold 0

$analyticsUserTopic = [int](Invoke-PsqlScalar -Sql "select count(*) from analytics_user_topic_aggregate;")
$analyticsDepartmentTopic = [int](Invoke-PsqlScalar -Sql "select count(*) from analytics_department_topic_aggregate;")
if ($analyticsUserTopic -eq 0 -and $analyticsDepartmentTopic -eq 0) {
    Write-Warn "analytics aggregate tables are empty. maintenance contour post-Stage-2 analytics aggregate runtime is NOT_IMPLEMENTED_BY_SCOPE."
} else {
    Write-Pass ("analytics aggregates present: user-topic={0}, department-topic={1}" -f $analyticsUserTopic, $analyticsDepartmentTopic)
}

Write-Host ""
Write-Host "Content invariants" -ForegroundColor Cyan
Assert-Zero -Label "INFOSEC in course name" -Sql "select count(*) from course where upper(name) like '%INFOSEC%' or upper(coalesce(description,'')) like '%INFOSEC%';"
Assert-Zero -Label "INFOSEC in topic name" -Sql "select count(*) from topic where upper(name) like '%INFOSEC%' or upper(coalesce(description,'')) like '%INFOSEC%';"
Assert-Zero -Label "INFOSEC in material name/body" -Sql "select count(*) from material where upper(name) like '%INFOSEC%' or upper(coalesce(description,'')) like '%INFOSEC%';"
Assert-Zero -Label "INFOSEC in question body" -Sql "select count(*) from question where upper(body) like '%INFOSEC%';"
Assert-Zero -Label "INFOSEC in answer option body" -Sql "select count(*) from answer_option where upper(body) like '%INFOSEC%';"
Assert-Zero -Label "INFOSEC in test name/description" -Sql "select count(*) from test where upper(name) like '%INFOSEC%' or upper(coalesce(description,'')) like '%INFOSEC%';"

Assert-Equals -Label "allowed material types" -Sql "select string_agg(material_type, ',') from (select distinct material_type from material order by material_type) s;" -Expected "DOCX,PDF,TEXT,VIDEO"
Assert-Equals -Label "allowed question types" -Sql "select string_agg(question_type, ',') from (select distinct question_type from question order by question_type) s;" -Expected "MULTIPLE_CHOICE,SINGLE_CHOICE"
Assert-Zero -Label "questions with option count != 4" -Sql @"
select count(*) from (
  select q.id, count(ao.id) as option_count
  from question q
  left join answer_option ao on ao.question_id = q.id
  group by q.id
) x
where x.option_count <> 4;
"@
Assert-Zero -Label "single-choice questions with correct count != 1" -Sql @"
select count(*) from (
  select q.id, sum(case when ao.is_correct then 1 else 0 end) as correct_count
  from question q
  join answer_option ao on ao.question_id = q.id
  where q.question_type = 'SINGLE_CHOICE'
  group by q.id
) x
where x.correct_count <> 1;
"@
Assert-Zero -Label "multiple-choice questions with correct count < 2" -Sql @"
select count(*) from (
  select q.id, sum(case when ao.is_correct then 1 else 0 end) as correct_count
  from question q
  join answer_option ao on ao.question_id = q.id
  where q.question_type = 'MULTIPLE_CHOICE'
  group by q.id
) x
where x.correct_count < 2;
"@

Write-Host ""
Write-Host "Testing/content invariants" -ForegroundColor Cyan
Assert-Equals -Label "all tests use DEFAULT scoring policy" -Sql "select count(*) from test where scoring_policy_code = 'DEFAULT';" -Expected "13"
Assert-Equals -Label "all tests published" -Sql "select count(*) from test where status = 'PUBLISHED';" -Expected "13"
Assert-Equals -Label "active final control count" -Sql "select count(*) from test where is_active_final_for_topic = true;" -Expected "6"
Assert-Zero -Label "self topics marked active final" -Sql @"
select count(*)
from test t
join topic tp on tp.id = t.topic_id
join course c on c.id = tp.course_id
where t.is_active_final_for_topic = true
  and c.name = 'Самоподготовка по промышленной безопасности на ОПО';
"@

Write-Host ""
Write-Host "Assignment/result invariants" -ForegroundColor Cyan
Assert-Zero -Label "self results with non-null assignment linkage" -Sql "select count(*) from result where attempt_mode = 'SELF' and (assignment_id is not null or assignment_test_id is not null);"
Assert-Zero -Label "assigned results leaked into self learner actor 29" -Sql "select count(*) from result where attempt_mode = 'ASSIGNED' and user_id_snapshot = 29;"
Assert-Zero -Label "self attempts with assignment_test_id" -Sql "select count(*) from test_attempt where attempt_mode = 'SELF' and assignment_test_id is not null;"
Assert-Equals -Label "assigned results count" -Sql "select count(*) from result where attempt_mode = 'ASSIGNED';" -Expected "2"
Assert-Equals -Label "self results count" -Sql "select count(*) from result where attempt_mode = 'SELF';" -Expected "2"
Assert-Equals -Label "OPERATOR-1 distinct assigned attempts" -Sql "select count(distinct id) from test_attempt where user_id = 13 and attempt_mode = 'ASSIGNED';" -Expected "1"
Assert-Equals -Label "OPERATOR-2 distinct assigned attempts" -Sql "select count(distinct id) from test_attempt where user_id = 21 and attempt_mode = 'ASSIGNED';" -Expected "1"
Assert-Equals -Label "OPERATOR-1 distinct assigned results" -Sql "select count(distinct id) from result where user_id_snapshot = 13 and attempt_mode = 'ASSIGNED';" -Expected "1"
Assert-Equals -Label "OPERATOR-2 distinct assigned results" -Sql "select count(distinct id) from result where user_id_snapshot = 21 and attempt_mode = 'ASSIGNED';" -Expected "1"

Assert-GreaterThan -Label "notification rows present" -Sql "select count(*) from notification;" -Threshold 0

Write-Host ""
if ($script:FailureCount -gt 0) {
    Write-Host ("Verification finished with {0} FAIL and {1} WARN." -f $script:FailureCount, $script:WarnCount) -ForegroundColor Red
    exit 1
}

Write-Host ("Verification finished with PASS and {0} WARN." -f $script:WarnCount) -ForegroundColor Green
