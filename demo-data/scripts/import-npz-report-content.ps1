param(
    [string]$DatasetPath = "demo-data/imports/npz-report-content.json",
    [string]$DemoActorId = "11",
    [switch]$AllowNonEmptyContentBaseline,
    [switch]$PreflightOnly
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# Настройки запуска
$BaseUrl = "http://localhost:8080"
$AuthMode = "DemoHeader" # DemoHeader or Bearer
$ConfiguredDemoActorId = $DemoActorId
$Token = "<PASTE_TOKEN_HERE>"
$DumpRequests = $true
$LogDir = "tmp/npz-report-import-logs"

$Headers = @{
    "Content-Type" = "application/json; charset=utf-8"
}

if ($AuthMode -eq "DemoHeader") {
    if ([string]::IsNullOrWhiteSpace($ConfiguredDemoActorId)) {
        throw "Demo actor id is not configured."
    }
    $Headers["X-Demo-Actor-Id"] = $ConfiguredDemoActorId
} elseif ($AuthMode -eq "Bearer") {
    if ($Token -eq "<PASTE_TOKEN_HERE>" -or [string]::IsNullOrWhiteSpace($Token)) {
        throw "Bearer token is not configured."
    }
    $Headers["Authorization"] = "Bearer $Token"
} else {
    throw "Unsupported AuthMode: $AuthMode"
}

$CourseIds = @{}
$TopicIds = @{}
$MaterialIds = @{}
$QuestionIds = @{}
$TestIds = @{}
$script:RequestSequence = 0

function Write-Step {
    param([Parameter(Mandatory = $true)][string]$Message)

    Write-Host ""
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message) -ForegroundColor Cyan
}

function Ensure-DebugLogDir {
    if (-not (Test-Path -LiteralPath $LogDir)) {
        New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
    }
}

function Get-SafeOperationFileLabel {
    param([Parameter(Mandatory = $true)][string]$OperationLabel)

    $safe = $OperationLabel -replace '[^A-Za-z0-9._-]+', '-'
    return $safe.Trim('-')
}

function Write-RequestDebugLog {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter(Mandatory = $true)][string]$OperationLabel,
        [Parameter()][AllowEmptyString()][string]$RequestJson = "",
        [Parameter()][AllowNull()]$ResponseObject = $null,
        [Parameter()][AllowNull()][string]$ErrorText = $null
    )

    if (-not $DumpRequests) {
        return
    }

    Ensure-DebugLogDir
    $script:RequestSequence++
    $prefix = "{0:D4}-{1}" -f $script:RequestSequence, (Get-SafeOperationFileLabel -OperationLabel $OperationLabel)

    $requestPayload = [ordered]@{
        method = $Method
        url = $Uri
        operationLabel = $OperationLabel
        headers = $Headers
        body = $RequestJson
    } | ConvertTo-Json -Depth 30
    Set-Content -Path (Join-Path $LogDir "$prefix.request.json") -Value $requestPayload -Encoding UTF8

    if ($null -ne $ResponseObject) {
        $responsePayload = [ordered]@{
            operationLabel = $OperationLabel
            body = $ResponseObject
        } | ConvertTo-Json -Depth 30
        Set-Content -Path (Join-Path $LogDir "$prefix.response.json") -Value $responsePayload -Encoding UTF8
    }

    if (-not [string]::IsNullOrWhiteSpace($ErrorText)) {
        Set-Content -Path (Join-Path $LogDir "$prefix.error.txt") -Value $ErrorText -Encoding UTF8
    }
}

function Convert-ToJsonBody {
    param([Parameter()][AllowNull()]$Body)

    if ($null -eq $Body) {
        return ""
    }

    return ($Body | ConvertTo-Json -Depth 30)
}

function Invoke-ApiJson {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("GET", "POST", "PATCH")][string]$Method,
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$OperationLabel,
        [Parameter()][AllowNull()]$Body = $null
    )

    $uri = "$BaseUrl$Path"
    $json = Convert-ToJsonBody -Body $Body

    try {
        $invokeParams = @{
            Uri = $uri
            Headers = $Headers
            Method = $Method
        }
        if ($Method -ne "GET") {
            $invokeParams["Body"] = [System.Text.Encoding]::UTF8.GetBytes($json)
            $invokeParams["ContentType"] = "application/json; charset=utf-8"
        }
        $response = Invoke-RestMethod @invokeParams
        Write-RequestDebugLog -Method $Method -Uri $uri -OperationLabel $OperationLabel -RequestJson $json -ResponseObject $response
        return $response
    } catch {
        $errorText = $_.Exception.Message
        if ($_.ErrorDetails -and $_.ErrorDetails.Message) {
            $errorText = $errorText + [Environment]::NewLine + $_.ErrorDetails.Message
        }
        Write-RequestDebugLog -Method $Method -Uri $uri -OperationLabel $OperationLabel -RequestJson $json -ErrorText $errorText
        throw
    }
}

function Invoke-ApiNoBody {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$OperationLabel
    )

    return Invoke-ApiJson -Method POST -Path $Path -OperationLabel $OperationLabel -Body ([pscustomobject]@{})
}

function Assert-EmptyContentBaseline {
    if ($AllowNonEmptyContentBaseline.IsPresent) {
        return
    }

    $courses = Invoke-ApiJson -Method GET -Path "/api/v1/expert/content/courses" -OperationLabel "preflight-list-courses"
    if ($null -ne $courses -and $courses.Count -gt 0) {
        throw "Content baseline is not empty. Set `$AllowNonEmptyContentBaseline = `$true only if you intentionally want to import into a non-empty content DB."
    }
}

function Resolve-DatasetPath {
    param([Parameter(Mandatory = $true)][string]$InputPath)

    if ([System.IO.Path]::IsPathRooted($InputPath)) {
        return $InputPath
    }

    return (Join-Path $PWD $InputPath)
}

$ResolvedDatasetPath = Resolve-DatasetPath -InputPath $DatasetPath
if (-not (Test-Path -LiteralPath $ResolvedDatasetPath)) {
    throw "Dataset file not found: $ResolvedDatasetPath"
}

$Dataset = Get-Content -LiteralPath $ResolvedDatasetPath -Encoding UTF8 -Raw | ConvertFrom-Json

Write-Step "Dataset loaded: $($Dataset.summary.courseCount) courses, $($Dataset.summary.topicCount) topics, $($Dataset.summary.materialCount) materials, $($Dataset.summary.testCount) tests, $($Dataset.summary.questionCount) questions."
Invoke-ApiJson -Method GET -Path "/api/v1/me" -OperationLabel "preflight-whoami" | Out-Null
Assert-EmptyContentBaseline

if ($PreflightOnly) {
    Write-Step "Preflight finished successfully. Backend is reachable, actor is valid, and content baseline is empty."
    return
}

Write-Step "Creating courses"
foreach ($course in $Dataset.courses) {
    $created = Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/courses" -OperationLabel "create-$($course.code)" -Body ([ordered]@{
        name = $course.name
        description = $course.description
        sortOrder = [int]$course.sortOrder
    })
    $CourseIds[$course.code] = [long]$created.id
}

Write-Step "Creating topics"
foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        $created = Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/topics" -OperationLabel "create-$($topic.code)" -Body ([ordered]@{
            courseId = $CourseIds[$course.code]
            name = $topic.name
            description = $topic.description
            sortOrder = [int]$topic.sortOrder
        })
        $TopicIds[$topic.code] = [long]$created.id
    }
}

Write-Step "Creating materials with body content"
foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        $material = $topic.material
        $created = Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/materials" -OperationLabel "create-$($material.code)" -Body ([ordered]@{
            topicId = $TopicIds[$topic.code]
            name = $material.name
            description = $material.description
            body = $material.body
            videoUrl = $null
            materialType = $material.materialType
            sortOrder = [int]$material.sortOrder
        })
        $MaterialIds[$material.code] = [long]$created.id
    }
}

Write-Step "Creating questions and answer options"
foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        foreach ($question in $topic.test.questions) {
            $createdQuestion = Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/questions" -OperationLabel "create-$($question.code)" -Body ([ordered]@{
                topicId = $TopicIds[$topic.code]
                body = $question.body
                questionType = $question.questionType
                sortOrder = [int]$question.sortOrder
            })
            $QuestionIds[$question.code] = [long]$createdQuestion.id

            foreach ($option in $question.options) {
                Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/questions/$($createdQuestion.id)/answer-options" -OperationLabel "create-$($option.code)" -Body ([ordered]@{
                    body = $option.body
                    answerOptionRole = $option.answerOptionRole
                    isCorrect = [bool]$option.isCorrect
                    displayOrder = [int]$option.displayOrder
                }) | Out-Null
            }
        }
    }
}

Write-Step "Creating tests and attaching questions"
foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        $test = $topic.test
        $createdTest = Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/tests" -OperationLabel "create-$($test.code)" -Body ([ordered]@{
            topicId = $TopicIds[$topic.code]
            name = $test.name
            description = $test.description
            testType = $test.testType
            thresholdPercent = [int]$test.thresholdPercent
            scoringPolicyCode = $test.scoringPolicyCode
            sortOrder = [int]$test.sortOrder
        })
        $TestIds[$test.code] = [long]$createdTest.id

        $displayOrder = 0
        foreach ($question in $test.questions) {
            Invoke-ApiJson -Method POST -Path "/api/v1/expert/content/tests/$($createdTest.id)/questions" -OperationLabel "attach-$($question.code)-to-$($test.code)" -Body ([ordered]@{
                questionId = $QuestionIds[$question.code]
                displayOrder = $displayOrder
                weight = 1
            }) | Out-Null
            $displayOrder++
        }
    }
}

Write-Step "Publishing content lifecycle"
foreach ($course in $Dataset.courses) {
    Invoke-ApiNoBody -Path "/api/v1/expert/content/lifecycle/courses/$($CourseIds[$course.code])/publish" -OperationLabel "publish-$($course.code)" | Out-Null
}

foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        Invoke-ApiNoBody -Path "/api/v1/expert/content/lifecycle/topics/$($TopicIds[$topic.code])/publish" -OperationLabel "publish-$($topic.code)" | Out-Null
    }
}

foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        Invoke-ApiNoBody -Path "/api/v1/expert/content/lifecycle/materials/$($MaterialIds[$topic.material.code])/publish" -OperationLabel "publish-$($topic.material.code)" | Out-Null
    }
}

foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        foreach ($question in $topic.test.questions) {
            Invoke-ApiNoBody -Path "/api/v1/expert/content/lifecycle/questions/$($QuestionIds[$question.code])/publish" -OperationLabel "publish-$($question.code)" | Out-Null
        }
    }
}

foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        Invoke-ApiNoBody -Path "/api/v1/expert/content/lifecycle/tests/$($TestIds[$topic.test.code])/publish" -OperationLabel "publish-$($topic.test.code)" | Out-Null
    }
}

Write-Step "Assigning active final tests"
foreach ($course in $Dataset.courses) {
    foreach ($topic in $course.topics) {
        if (-not $topic.test.assignAsActiveFinal) {
            continue
        }

        Invoke-ApiNoBody -Path "/api/v1/expert/content/topics/$($TopicIds[$topic.code])/active-final-tests/$($TestIds[$topic.test.code])/assign" -OperationLabel "assign-final-$($topic.code)" | Out-Null
    }
}

Write-Step "Import completed successfully."
Write-Host ""
Write-Host ("Courses created: {0}" -f $Dataset.summary.courseCount) -ForegroundColor Green
Write-Host ("Topics created: {0}" -f $Dataset.summary.topicCount) -ForegroundColor Green
Write-Host ("Materials created: {0}" -f $Dataset.summary.materialCount) -ForegroundColor Green
Write-Host ("Tests created: {0}" -f $Dataset.summary.testCount) -ForegroundColor Green
Write-Host ("Questions created: {0}" -f $Dataset.summary.questionCount) -ForegroundColor Green
