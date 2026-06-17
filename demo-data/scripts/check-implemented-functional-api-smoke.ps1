param(
    [string]$BaseUrl = "http://127.0.0.1:8080",
    [string]$DatabaseName = "training_platform_demo_fullcycle",
    [string]$PostgresContainer = "training_platform_postgres",
    [string]$PostgresUser = "postgres"
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$allowedDatabaseName = "training_platform_demo_fullcycle"

if ($DatabaseName -ne $allowedDatabaseName -or $DatabaseName -notmatch 'demo') {
    throw "Refusing to use non-allowlisted database '$DatabaseName'. Allowed database: '$allowedDatabaseName'."
}

$script:FailureCount = 0

function Invoke-PsqlScalar {
    param([Parameter(Mandatory = $true)][string]$Sql)
    $result = docker exec $PostgresContainer psql -U $PostgresUser -d $DatabaseName -v ON_ERROR_STOP=1 -P pager=off -At -c $Sql
    return ($result | Select-Object -First 1).ToString().Trim()
}

function Invoke-Smoke {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$ActorId,
        [int[]]$ExpectedStatuses = @(200),
        [AllowNull()]$Body = $null
    )

    $headers = @{ "X-Demo-Actor-Id" = $ActorId }
    if ($null -ne $Body) {
        $headers["Content-Type"] = "application/json"
    }

    try {
        $response = if ($null -eq $Body) {
            Invoke-RestMethod -Method $Method -Uri ($BaseUrl.TrimEnd('/') + $Path) -Headers $headers -ErrorAction Stop
        } else {
            Invoke-RestMethod -Method $Method -Uri ($BaseUrl.TrimEnd('/') + $Path) -Headers $headers -Body $Body -ErrorAction Stop
        }

        if ($ExpectedStatuses -contains 200) {
            Write-Host ("PASS  {0}: {1} {2} -> 200" -f $Label, $Method, $Path) -ForegroundColor Green
        } else {
            $script:FailureCount++
            Write-Host ("FAIL  {0}: expected one of [{1}], got 200" -f $Label, ($ExpectedStatuses -join ', ')) -ForegroundColor Red
        }
        return $response
    } catch {
        $status = $null
        if ($_.Exception.Response -and $_.Exception.Response.PSObject.Properties.Name -contains 'StatusCode') {
            $status = [int]$_.Exception.Response.StatusCode
        }

        if ($null -ne $status -and $ExpectedStatuses -contains $status) {
            Write-Host ("PASS  {0}: {1} {2} -> {3}" -f $Label, $Method, $Path, $status) -ForegroundColor Green
            return $null
        }

        $script:FailureCount++
        $statusLabel = if ($null -ne $status) { $status } else { 'ERROR' }
        Write-Host ("FAIL  {0}: {1} {2} -> {3}" -f $Label, $Method, $Path, $statusLabel) -ForegroundColor Red
        Write-Host (($_ | Out-String).Trim()) -ForegroundColor DarkRed
        return $null
    }
}

function Get-AssignedContext {
    param([Parameter(Mandatory = $true)][int]$ActorId)

    $assignmentId = Invoke-PsqlScalar -Sql "select id from assignment where user_id = $ActorId order by id limit 1;"
    if ([string]::IsNullOrWhiteSpace($assignmentId)) {
        throw "No assignment found for actor $ActorId in $DatabaseName."
    }

    $assignmentTestId = Invoke-PsqlScalar -Sql "select at.id from assignment_test at join assignment a on a.id = at.assignment_id where a.user_id = $ActorId order by at.id limit 1;"
    if ([string]::IsNullOrWhiteSpace($assignmentTestId)) {
        throw "No assignment_test found for actor $ActorId in $DatabaseName."
    }

    return [pscustomobject]@{
        AssignmentId = $assignmentId
        AssignmentTestId = $assignmentTestId
    }
}

function Assert-PsqlGreaterThan {
    param(
        [Parameter(Mandatory = $true)][string]$Label,
        [Parameter(Mandatory = $true)][string]$Sql,
        [Parameter(Mandatory = $true)][int]$Threshold
    )

    $actual = [int](Invoke-PsqlScalar -Sql $Sql)
    if ($actual -gt $Threshold) {
        Write-Host ("PASS  {0}: {1} > {2}" -f $Label, $actual, $Threshold) -ForegroundColor Green
    } else {
        $script:FailureCount++
        Write-Host ("FAIL  {0}: expected > {1}, got {2}" -f $Label, $Threshold, $actual) -ForegroundColor Red
    }
}

$expertAssignedTopicId = Invoke-PsqlScalar -Sql @"
select tp.id
from topic tp
join test t on t.topic_id = tp.id
where t.is_active_final_for_topic = true
order by tp.id
limit 1;
"@

$operator1 = Get-AssignedContext -ActorId 13
$operator2 = Get-AssignedContext -ActorId 21

Write-Host "Implemented functional API smoke" -ForegroundColor Cyan

Invoke-Smoke -Label "health" -Method "GET" -Path "/actuator/health" -ActorId "1"
Invoke-Smoke -Label "admin me" -Method "GET" -Path "/api/v1/me" -ActorId "1"
Invoke-Smoke -Label "admin org tree" -Method "GET" -Path "/api/v1/admin/org-units/tree" -ActorId "1"
Invoke-Smoke -Label "admin users" -Method "GET" -Path "/api/v1/admin/users" -ActorId "1"
Invoke-Smoke -Label "admin notifications read" -Method "GET" -Path "/api/v1/admin/notifications" -ActorId "1"
Invoke-Smoke -Label "expert courses" -Method "GET" -Path "/api/v1/expert/content/courses" -ActorId "11"
Invoke-Smoke -Label "expert active final read" -Method "GET" -Path "/api/v1/expert/content/topics/$expertAssignedTopicId/active-final-test" -ActorId "11"

Invoke-Smoke -Label "operator-1 assigned list" -Method "GET" -Path "/api/v1/assigned-learning/assignments" -ActorId "13"
Invoke-Smoke -Label "operator-1 assignment detail" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator1.AssignmentId)" -ActorId "13"
Invoke-Smoke -Label "operator-1 learning context" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator1.AssignmentId)/learning-context" -ActorId "13"
Invoke-Smoke -Label "operator-1 test context" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator1.AssignmentId)/assignment-tests/$($operator1.AssignmentTestId)/test-context" -ActorId "13"

Invoke-Smoke -Label "operator-2 assigned list" -Method "GET" -Path "/api/v1/assigned-learning/assignments" -ActorId "21"
Invoke-Smoke -Label "operator-2 assignment detail" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator2.AssignmentId)" -ActorId "21"
Invoke-Smoke -Label "operator-2 learning context" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator2.AssignmentId)/learning-context" -ActorId "21"
Invoke-Smoke -Label "operator-2 test context" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator2.AssignmentId)/assignment-tests/$($operator2.AssignmentTestId)/test-context" -ActorId "21"

Invoke-Smoke -Label "self-testing catalog" -Method "GET" -Path "/api/v1/self-testing/tests" -ActorId "29"
Invoke-Smoke -Label "self result history" -Method "GET" -Path "/api/v1/self/results/history" -ActorId "29"
Invoke-Smoke -Label "self notifications read" -Method "GET" -Path "/api/v1/self/notifications" -ActorId "29"
Invoke-Smoke -Label "manager current supervision" -Method "GET" -Path "/api/v1/managerial/current-supervision" -ActorId "4"

$negativeCourseBody = '{"name":"Negative smoke forbidden create","description":"smoke","sortOrder":10}'
Invoke-Smoke -Label "negative operator expert mutation" -Method "POST" -Path "/api/v1/expert/content/courses" -ActorId "13" -ExpectedStatuses @(403) -Body $negativeCourseBody
Invoke-Smoke -Label "negative learner foreign assignment" -Method "GET" -Path "/api/v1/assigned-learning/assignments/$($operator1.AssignmentId)" -ActorId "29" -ExpectedStatuses @(404, 403)
Invoke-Smoke -Label "negative manager admin mutation" -Method "POST" -Path "/api/v1/admin/org-units/999999/archive" -ActorId "4" -ExpectedStatuses @(403)
Invoke-Smoke -Label "negative operator managerial read" -Method "GET" -Path "/api/v1/managerial/current-supervision" -ActorId "13" -ExpectedStatuses @(403)
Invoke-Smoke -Label "negative learner managerial read" -Method "GET" -Path "/api/v1/managerial/current-supervision" -ActorId "29" -ExpectedStatuses @(403)
Invoke-Smoke -Label "negative learner admin notifications" -Method "GET" -Path "/api/v1/admin/notifications" -ActorId "29" -ExpectedStatuses @(403)

$notificationAnchor = Invoke-PsqlScalar -Sql "select recipient_user_id || ':' || id from notification order by id limit 1;"
if ([string]::IsNullOrWhiteSpace($notificationAnchor)) {
    $script:FailureCount++
    Write-Host "FAIL  notifications lifecycle: expected at least one emitted notification row." -ForegroundColor Red
} else {
    $notificationParts = $notificationAnchor -split ':'
    $notificationActorId = $notificationParts[0]
    $notificationId = $notificationParts[1]

    Invoke-Smoke -Label "self notification detail" -Method "GET" -Path "/api/v1/self/notifications/$notificationId" -ActorId $notificationActorId
    Invoke-Smoke -Label "self notification mark read" -Method "POST" -Path "/api/v1/self/notifications/$notificationId/read" -ActorId $notificationActorId
    Assert-PsqlGreaterThan -Label "notification read_at persisted" -Sql "select count(*) from notification where id = $notificationId and read_at is not null;" -Threshold 0
    Invoke-Smoke -Label "self notifications read all" -Method "POST" -Path "/api/v1/self/notifications/read-all" -ActorId $notificationActorId
    Invoke-Smoke -Label "admin notifications dispatch pending" -Method "POST" -Path "/api/v1/admin/notifications/dispatch-pending?limit=100" -ActorId "1"
    Assert-PsqlGreaterThan -Label "in-app notifications dispatched" -Sql "select count(*) from notification where channel_code = 'IN_APP' and status = 'SENT';" -Threshold 0
}

if ($script:FailureCount -gt 0) {
    Write-Host ("API smoke finished with {0} FAIL." -f $script:FailureCount) -ForegroundColor Red
    exit 1
}

Write-Host "API smoke finished with PASS." -ForegroundColor Green
