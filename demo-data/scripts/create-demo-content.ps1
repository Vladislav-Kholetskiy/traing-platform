param(
    [switch]$PreflightOnly
)

$ErrorActionPreference = 'Stop'

Set-StrictMode -Version Latest
Add-Type -AssemblyName System.Net.Http

# Настройки запуска
$BaseUrl = "http://localhost:8080"
$AuthMode = "DemoHeader" # DemoHeader or Bearer
$DemoActorId = "1"
$Token = "<PASTE_TOKEN_HERE>"
$DumpRequests = $true
$LogDir = "tmp/demo-content-run-logs"
$AllowNonEmptyContentBaseline = $false
$StopAfterSection = "" # Courses | Topics | FirstMaterial | Materials | Questions | Tests | Links | PublishCourses | PublishTopics | PublishMaterials | PublishQuestions | PublishTests | FinalControls
$StopBeforeSection = ""
$StopAfterOperation = ""
$Headers = @{
    "Content-Type" = "application/json; charset=utf-8"
}

if ($AuthMode -eq "DemoHeader") {
    if ([string]::IsNullOrWhiteSpace($DemoActorId)) {
        throw "Demo actor id is not configured."
    }
    $Headers["X-Demo-Actor-Id"] = $DemoActorId
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
$OptionIds = @{}
$TestIds = @{}
$script:RequestSequence = 0
$script:FirstMaterialCreated = $false

$ScriptRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$ScenarioRoot = Resolve-Path (Join-Path $ScriptRoot "..\scenarios")
$ContentMatrixPath = Join-Path $ScenarioRoot "content-matrix.md"
$QuestionBankPath = Join-Path $ScenarioRoot "question-bank.md"
$MaterialsSourcePath = Join-Path $ScenarioRoot "materials-source.md"
$AcceptancePath = Join-Path $ScenarioRoot "acceptance-checkpoints.md"

# Замечание по текущему состоянию:
# - в рабочем контуре нет отдельных бизнес-кодов для course/topic/material/question/test;
# - коды из сценария используются только как локальные ключи связывания;
# - значение scoringPolicyCode "DEFAULT" подтверждено текущим кодом и тестами,
#   но если серверная часть его отклоняет, запуск нужно остановить и перепроверить ожидания.

# VERIFY_AGAINST_RUNTIME before live run:
# Runtime test composition supports only same-topic questions inside one test.
# TEST-OPO-SELF-GENERAL is modeled in planning artifacts as a broad self refresher.
# To keep the helper executable under current constraints, it is anchored to TOPIC-OPO-SELF-RULES.
$TestTopicOverrides = @{
    "TEST-OPO-SELF-GENERAL" = "TOPIC-OPO-SELF-RULES"
}

$CourseDescriptions = @{
    "COURSE-OPS-SAFETY" = "Курс знакомит операторов с основными требованиями промышленной безопасности на НПЗ: производственными рисками, безопасным поведением на установке, обязанностями персонала и первичными действиями при признаках нештатной ситуации."
    "COURSE-PERMIT-SAFETY" = "Курс объясняет порядок оформления наряда-допуска, подготовки рабочего места, распределения ролей и контроля рисков перед началом работ. Он помогает безопасно организовать работы на производственном объекте и снизить вероятность инцидентов."
    "COURSE-OPO-SELF-REFRESHER" = "Курс для самостоятельного повторения ключевых требований промышленной безопасности на опасном производственном объекте. Он помогает освежить знания о правилах безопасной работы, типовых нарушениях и действиях при угрозе инцидента."
}

$TopicDescriptions = @{
    "TOPIC-OPS-INTRO" = "Introductory industrial safety topic for entry and baseline safe behavior on site."
    "TOPIC-OPS-UNIT-SAFE" = "Safe work on process unit, operating discipline, routes, PPE and local hazards."
    "TOPIC-OPS-INCIDENT" = "Early signs of abnormal situation and first-response actions."
    "TOPIC-PERMIT-PREP" = "Permit preparation sequence, approvals and readiness conditions."
    "TOPIC-PERMIT-RISK" = "Pre-job risk control, hazard identification and field verification."
    "TOPIC-PERMIT-ROLES" = "Responsibilities of work supervisor and responsible performer."
    "TOPIC-OPO-SELF-RULES" = "Self-study topic with baseline industrial safety requirements on OPO."
    "TOPIC-OPO-SELF-INCIDENTS" = "Self-study topic with typical violations and worker actions under accident threat."
}

$ThresholdByTestCode = @{
    "TEST-OPS-INTRO-FINAL" = 80
    "TEST-OPS-INTRO-SELF" = 70
    "TEST-OPS-UNIT-FINAL" = 80
    "TEST-OPS-INCIDENT-FINAL" = 80
    "TEST-OPS-INCIDENT-SELF" = 70
    "TEST-PERMIT-PREP-FINAL" = 80
    "TEST-PERMIT-PREP-TRAIN" = 70
    "TEST-PERMIT-RISK-FINAL" = 80
    "TEST-PERMIT-ROLES-FINAL" = 80
    "TEST-PERMIT-ROLES-TRAIN" = 70
    "TEST-OPO-SELF-RULES-SELF" = 70
    "TEST-OPO-SELF-INCIDENTS-SELF" = 70
    "TEST-OPO-SELF-GENERAL" = 70
}

# Helper functions
function Write-Step {
    param(
        [Parameter(Mandatory = $true)][string]$Message
    )

    Write-Host ""
    Write-Host ("[{0}] {1}" -f (Get-Date -Format "yyyy-MM-dd HH:mm:ss"), $Message) -ForegroundColor Cyan
}

function Convert-ToJsonBody {
    param(
        [Parameter()][AllowNull()]$Body,
        [Parameter()][string]$OperationLabel = "unspecified-operation"
    )

    if ($null -eq $Body) {
        return ""
    }

    $normalizedBody = Normalize-PayloadObject -Value $Body
    $json = $normalizedBody | ConvertTo-Json -Depth 20

    if ($json.Contains([string][char]0x00AB) -or $json.Contains([string][char]0x00BB)) {
        throw "Runtime JSON still contains typographic angle quotes for operation '$OperationLabel'. Normalization did not apply."
    }

    return $json
}

function Get-SafeOperationFileLabel {
    param(
        [Parameter(Mandatory = $true)][string]$OperationLabel
    )

    $safe = $OperationLabel -replace '[^A-Za-z0-9._-]+', '-'
    return $safe.Trim('-')
}

function Ensure-DebugLogDir {
    if (-not (Test-Path -LiteralPath $LogDir)) {
        New-Item -ItemType Directory -Path $LogDir -Force | Out-Null
    }
}

function Write-RequestDebugLog {
    param(
        [Parameter(Mandatory = $true)][string]$Method,
        [Parameter(Mandatory = $true)][string]$Uri,
        [Parameter(Mandatory = $true)][string]$OperationLabel,
        [Parameter()][string]$RequestJson = "",
        [Parameter()][AllowNull()]$ResponseObject = $null,
        [Parameter()][AllowNull()]$ResponseHeaders = $null,
        [Parameter()][string]$ErrorText = ""
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
    } | ConvertTo-Json -Depth 20
    Set-Content -Path (Join-Path $LogDir "$prefix.request.json") -Value $requestPayload -Encoding UTF8

    if ($null -ne $ResponseObject -or $null -ne $ResponseHeaders) {
        $responsePayload = [ordered]@{
            operationLabel = $OperationLabel
            headers = $ResponseHeaders
            body = $ResponseObject
        } | ConvertTo-Json -Depth 20
        Set-Content -Path (Join-Path $LogDir "$prefix.response.json") -Value $responsePayload -Encoding UTF8
    }

    if (-not [string]::IsNullOrWhiteSpace($ErrorText)) {
        Set-Content -Path (Join-Path $LogDir "$prefix.error.txt") -Value $ErrorText -Encoding UTF8
    }
}

function Get-HttpErrorDetails {
    param(
        [Parameter(Mandatory = $true)]$Exception
    )

    $statusCode = $null
    $headers = $null
    $bodyText = ""
    $response = $Exception.Exception.Response

    if ($null -ne $response) {
        try {
            if ($response.PSObject.Properties.Name -contains 'StatusCode') {
                $statusCode = [int]$response.StatusCode
            }
        } catch {}

        try {
            if ($response.PSObject.Properties.Name -contains 'Headers') {
                $headers = @{}
                foreach ($headerName in $response.Headers.AllKeys) {
                    $headers[$headerName] = $response.Headers[$headerName]
                }
            }
        } catch {}

        try {
            $stream = $response.GetResponseStream()
            if ($null -ne $stream) {
                $reader = New-Object System.IO.StreamReader($stream)
                $bodyText = $reader.ReadToEnd()
                $reader.Close()
            }
        } catch {}
    }

    return [pscustomobject]@{
        StatusCode = $statusCode
        Headers = $headers
        Body = $bodyText
    }
}

function Stop-IfRequestedBeforeSection {
    param(
        [Parameter(Mandatory = $true)][string]$SectionName
    )

    if ($StopBeforeSection -eq $SectionName) {
        throw "Controlled stop requested before section '$SectionName'."
    }
}

function Stop-IfRequestedAfterSection {
    param(
        [Parameter(Mandatory = $true)][string]$SectionName
    )

    if ($StopAfterSection -eq $SectionName) {
        throw "Controlled stop requested after section '$SectionName'."
    }
}

function Stop-IfRequestedAfterOperation {
    param(
        [Parameter(Mandatory = $true)][string]$OperationLabel
    )

    if (-not [string]::IsNullOrWhiteSpace($StopAfterOperation) -and $StopAfterOperation -eq $OperationLabel) {
        throw "Controlled stop requested after operation '$OperationLabel'."
    }
}

function Require-Id {
    param(
        [Parameter(Mandatory = $true)]$Response,
        [Parameter(Mandatory = $true)][string]$Context
    )

    if ($null -eq $Response) {
        throw "[$Context] Response is null."
    }

    if ($null -eq $Response.id) {
        throw "[$Context] Response does not contain id. Response: $($Response | ConvertTo-Json -Depth 10)"
    }

    return [int64]$Response.id
}

function Invoke-JsonPost {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter()][AllowNull()]$Body = $null,
        [Parameter(Mandatory = $true)][string]$Context
    )

    $uri = "{0}{1}" -f $BaseUrl.TrimEnd('/'), $Path
    Write-Host ("POST {0}" -f $uri) -ForegroundColor DarkGray
    $json = Convert-ToJsonBody -Body $Body -OperationLabel $Context
    $httpClient = [System.Net.Http.HttpClient]::new()

    try {
        foreach ($headerKey in $Headers.Keys) {
            if ($headerKey -ieq 'Content-Type') {
                continue
            }
            [void]$httpClient.DefaultRequestHeaders.TryAddWithoutValidation($headerKey, [string]$Headers[$headerKey])
        }

        if ($null -eq $Body) {
            $httpResponse = $httpClient.PostAsync($uri, $null).GetAwaiter().GetResult()
        } else {
            $content = [System.Net.Http.StringContent]::new($json, [System.Text.Encoding]::UTF8, 'application/json')
            $httpResponse = $httpClient.PostAsync($uri, $content).GetAwaiter().GetResult()
        }

        $responseText = $httpResponse.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        $responseHeaders = @{}
        foreach ($header in $httpResponse.Headers) {
            $responseHeaders[$header.Key] = ($header.Value -join ',')
        }
        foreach ($header in $httpResponse.Content.Headers) {
            $responseHeaders[$header.Key] = ($header.Value -join ',')
        }

        if (-not $httpResponse.IsSuccessStatusCode) {
            $errorText = @(
                "Operation: $Context"
                "Method: POST"
                "Url: $uri"
                "StatusCode: $([int]$httpResponse.StatusCode)"
                "RequestJson:"
                $json
                "ResponseHeaders:"
                (($responseHeaders | ConvertTo-Json -Depth 10))
                "ResponseBody:"
                $responseText
            ) -join "`n"

            Write-RequestDebugLog -Method "POST" -Uri $uri -OperationLabel $Context -RequestJson $json -ResponseHeaders $responseHeaders -ErrorText $errorText
            Write-Host $errorText -ForegroundColor Red
            throw "POST request failed for '$Context' with status $([int]$httpResponse.StatusCode)."
        }

        $response = if ([string]::IsNullOrWhiteSpace($responseText)) { $null } else { $responseText | ConvertFrom-Json }

        Write-RequestDebugLog -Method "POST" -Uri $uri -OperationLabel $Context -RequestJson $json -ResponseObject $response -ResponseHeaders $responseHeaders
        return $response
    } catch {
        $httpError = Get-HttpErrorDetails -Exception $_
        $errorText = @(
            "Operation: $Context"
            "Method: POST"
            "Url: $uri"
            "StatusCode: $($httpError.StatusCode)"
            "RequestJson:"
            $json
            "ResponseHeaders:"
            (($httpError.Headers | ConvertTo-Json -Depth 10))
            "ResponseBody:"
            $httpError.Body
            "Exception:"
            ($_ | Out-String).Trim()
        ) -join "`n"

        Write-RequestDebugLog -Method "POST" -Uri $uri -OperationLabel $Context -RequestJson $json -ResponseHeaders $httpError.Headers -ErrorText $errorText
        Write-Host $errorText -ForegroundColor Red
        throw
    } finally {
        $httpClient.Dispose()
    }
}

function Invoke-JsonGet {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$Context
    )

    $uri = "{0}{1}" -f $BaseUrl.TrimEnd('/'), $Path
    Write-Host ("GET {0}" -f $uri) -ForegroundColor DarkGray

    try {
        $response = Invoke-RestMethod -Method Get -Uri $uri -Headers $Headers -ErrorAction Stop
        Write-RequestDebugLog -Method "GET" -Uri $uri -OperationLabel $Context -ResponseObject $response
        return $response
    } catch {
        $httpError = Get-HttpErrorDetails -Exception $_
        $errorText = @(
            "Operation: $Context"
            "Method: GET"
            "Url: $uri"
            "StatusCode: $($httpError.StatusCode)"
            "ResponseHeaders:"
            (($httpError.Headers | ConvertTo-Json -Depth 10))
            "ResponseBody:"
            $httpError.Body
            "Exception:"
            ($_ | Out-String).Trim()
        ) -join "`n"

        Write-RequestDebugLog -Method "GET" -Uri $uri -OperationLabel $Context -ResponseHeaders $httpError.Headers -ErrorText $errorText
        Write-Host $errorText -ForegroundColor Red
        throw
    }
}

function Publish-Entity {
    param(
        [Parameter(Mandatory = $true)][ValidateSet("courses", "topics", "materials", "questions", "tests")][string]$EntityPath,
        [Parameter(Mandatory = $true)][int64]$Id,
        [Parameter(Mandatory = $true)][string]$PlanningCode
    )

    $response = Invoke-JsonPost -Path "/api/v1/expert/content/lifecycle/$EntityPath/$Id/publish" -Context "publish:$PlanningCode"
    $publishedId = Require-Id -Response $response -Context "publish:$PlanningCode"
    Write-Host ("Published {0} -> runtime id {1} with status {2}" -f $PlanningCode, $publishedId, $response.status) -ForegroundColor Green
    return $response
}

function Get-MarkdownTableRows {
    param(
        [Parameter(Mandatory = $true)][string]$Path,
        [Parameter(Mandatory = $true)][string]$SectionHeader
    )

    $lines = Get-Content -Path $Path -Encoding UTF8
    $sectionIndex = [Array]::IndexOf($lines, $SectionHeader)
    if ($sectionIndex -lt 0) {
        throw "Section '$SectionHeader' not found in $Path"
    }

    $headerLine = $null
    $separatorIndex = -1
    for ($i = $sectionIndex + 1; $i -lt $lines.Count; $i++) {
        if ($lines[$i] -match '^\|') {
            $headerLine = $lines[$i]
            $separatorIndex = $i + 1
            break
        }
    }

    if ($null -eq $headerLine -or $separatorIndex -ge $lines.Count -or $lines[$separatorIndex] -notmatch '^\|[-: ]+\|') {
        throw "Markdown table not found under section '$SectionHeader' in $Path"
    }

    $headers = ($headerLine.Trim('|').Split('|') | ForEach-Object { $_.Trim() })
    $rows = @()

    for ($i = $separatorIndex + 1; $i -lt $lines.Count; $i++) {
        $line = $lines[$i]
        if ($line -notmatch '^\|') {
            break
        }

        $cells = ($line.Trim('|').Split('|') | ForEach-Object { $_.Trim() })
        if ($cells.Count -ne $headers.Count) {
            continue
        }

        $row = [ordered]@{}
        for ($j = 0; $j -lt $headers.Count; $j++) {
            $row[$headers[$j]] = $cells[$j].Trim('`')
        }
        $rows += [pscustomobject]$row
    }

    return $rows
}

function Parse-QuestionBank {
    param(
        [Parameter(Mandatory = $true)][string]$Path
    )

    $lines = Get-Content -Path $Path -Encoding UTF8
    $questions = @()
    $current = $null
    $state = "idle"
    $buffer = New-Object System.Collections.Generic.List[string]

    function Flush-BufferToCurrent {
        param($Question, [string]$TargetField, $BufferRef)
        if ($null -ne $Question -and $BufferRef.Count -gt 0) {
            $text = ($BufferRef | Where-Object { $_ -ne $null }) -join "`n"
            $Question[$TargetField] = $text.Trim()
            $BufferRef.Clear()
        }
    }

    foreach ($line in $lines) {
        if ($line -match '^###\s+(Q-[A-Z0-9-]+)$') {
            if ($null -ne $current) {
                Flush-BufferToCurrent -Question $current -TargetField "Explanation" -BufferRef $buffer
                $questions += [pscustomobject]$current
            }
            $current = [ordered]@{
                QuestionCode  = $Matches[1]
                TopicCode     = $null
                QuestionType  = $null
                Text          = $null
                Options       = New-Object System.Collections.Generic.List[object]
                Correct       = New-Object System.Collections.Generic.List[string]
                Explanation   = ""
                IntendedTests = New-Object System.Collections.Generic.List[string]
            }
            $state = "question"
            $buffer.Clear()
            continue
        }

        if ($null -eq $current) {
            continue
        }

        if ($line -match '^- Topic:\s+`([^`]+)`$') {
            Flush-BufferToCurrent -Question $current -TargetField "Explanation" -BufferRef $buffer
            $current.TopicCode = $Matches[1]
            $state = "question"
            continue
        }
        if ($line -match '^- Type:\s+`([^`]+)`$') {
            $current.QuestionType = $Matches[1]
            continue
        }
        if ($line -match '^- Text:\s+(.+)$') {
            $current.Text = $Matches[1].Trim()
            continue
        }
        if ($line -match '^- Options:$') {
            $state = "options"
            continue
        }
        if ($line -match '^- Correct:$') {
            $state = "correct"
            continue
        }
        if ($line -match '^- Explanation:\s*(.*)$') {
            $state = "explanation"
            $buffer.Clear()
            if (-not [string]::IsNullOrWhiteSpace($Matches[1])) {
                $buffer.Add($Matches[1].Trim())
            }
            continue
        }
        if ($line -match '^- Intended tests:$') {
            Flush-BufferToCurrent -Question $current -TargetField "Explanation" -BufferRef $buffer
            $state = "tests"
            continue
        }

        switch ($state) {
            "options" {
                if ($line -match '^\s*-\s+`([^`]+)`:\s+(.+)$') {
                    $current.Options.Add([pscustomobject]@{
                        OptionCode = $Matches[1]
                        Text       = $Matches[2].Trim()
                    })
                }
            }
            "correct" {
                if ($line -match '^\s*-\s+`([^`]+)`$') {
                    $current.Correct.Add($Matches[1])
                }
            }
            "tests" {
                if ($line -match '^\s*-\s+`([^`]+)`$') {
                    $current.IntendedTests.Add($Matches[1])
                }
            }
            "explanation" {
                if ($line -notmatch '^###\s+' -and $line.Trim().Length -gt 0) {
                    $buffer.Add($line.Trim())
                }
            }
        }
    }

    if ($null -ne $current) {
        Flush-BufferToCurrent -Question $current -TargetField "Explanation" -BufferRef $buffer
        $questions += [pscustomobject]$current
    }

    return $questions
}

function Parse-MaterialsSource {
    param(
        [Parameter(Mandatory = $true)][string]$Path
    )

    $lines = Get-Content -Path $Path -Encoding UTF8
    $materials = @()
    $current = $null
    $capturingDemoText = $false
    $demoBuffer = New-Object System.Collections.Generic.List[string]

    function Flush-Material {
        param($Material, $BufferRef, [ref]$TargetCollection)
        if ($null -ne $Material) {
            $Material.SourceText = (($BufferRef | Where-Object { $_ -ne $null }) -join "`n").Trim()
            $TargetCollection.Value += [pscustomobject]$Material
            $BufferRef.Clear()
        }
    }

    foreach ($line in $lines) {
        if ($line -match '^###\s+(MAT-[A-Z0-9-]+)$') {
            Flush-Material -Material $current -BufferRef $demoBuffer -TargetCollection ([ref]$materials)
            $current = [ordered]@{
                MaterialCode = $Matches[1]
                CourseCode   = $null
                TopicCode    = $null
                MaterialType = $null
                Name         = $null
                Description  = $null
                SortOrder    = $null
                Notes        = $null
                SourceText   = ""
            }
            $capturingDemoText = $false
            continue
        }

        if ($null -eq $current) {
            continue
        }

        if ($line -match '^- Material code:\s+`([^`]+)`$') { $current.MaterialCode = $Matches[1]; continue }
        if ($line -match '^- Course code:\s+`([^`]+)`$') { $current.CourseCode = $Matches[1]; continue }
        if ($line -match '^- Topic code:\s+`([^`]+)`$') { $current.TopicCode = $Matches[1]; continue }
        if ($line -match '^- Material type:\s+`([^`]+)`$') { $current.MaterialType = $Matches[1]; continue }
        if ($line -match '^- Name:\s+`(.+)`$') { $current.Name = $Matches[1]; continue }
        if ($line -match '^- Description:\s+`(.+)`$') { $current.Description = $Matches[1]; continue }
        if ($line -match '^- Sort order:\s+`([^`]+)`$') { $current.SortOrder = [int]$Matches[1]; continue }
        if ($line -match '^- Notes:\s+`(.+)`$') { $current.Notes = $Matches[1]; continue }
        if ($line -match '^- Demo content/source text:\s*$') {
            $capturingDemoText = $true
            $demoBuffer.Clear()
            continue
        }

        if ($capturingDemoText) {
            if ($line -match '^###\s+' -or $line -match '^##\s+') {
                Flush-Material -Material $current -BufferRef $demoBuffer -TargetCollection ([ref]$materials)
                $current = $null
                $capturingDemoText = $false
                continue
            }
            $demoBuffer.Add($line)
        }
    }

    Flush-Material -Material $current -BufferRef $demoBuffer -TargetCollection ([ref]$materials)
    return $materials
}

function Get-TestOwnerTopicCode {
    param(
        [Parameter(Mandatory = $true)][string]$TestCode,
        [Parameter(Mandatory = $true)][string]$TopicOrCourseCode
    )

    if ($TestTopicOverrides.ContainsKey($TestCode)) {
        return $TestTopicOverrides[$TestCode]
    }

    if ($TopicOrCourseCode -like 'TOPIC-*') {
        return $TopicOrCourseCode
    }

    throw "[$TestCode] Cannot determine owner topic from '$TopicOrCourseCode'. Add explicit override before live run."
}

function New-MaterialDescription {
    param(
        [Parameter(Mandatory = $true)]$Material
    )

    # Material create API stores metadata only. Full educational text remains
    # in materials-source.md and must not be posted as description payload.
    if (-not [string]::IsNullOrWhiteSpace($Material.Description)) {
        return $Material.Description.Trim()
    }

    return $null
}

function New-MaterialBody {
    param(
        [Parameter(Mandatory = $true)]$Material
    )

    if ([string]::IsNullOrWhiteSpace($Material.SourceText)) {
        return $null
    }

    $sourceText = Normalize-TextPayload -Value $Material.SourceText.Trim()

    if ($Material.MaterialType -eq "TEXT") {
        return $sourceText
    }

    return (Convert-MaterialSourceTextToRuntimeBody -Material $Material -SourceText $sourceText)
}

function Convert-MaterialSourceTextToRuntimeBody {
    param(
        [Parameter(Mandatory = $true)]$Material,
        [Parameter(Mandatory = $true)][string]$SourceText
    )

    $paragraphs = [System.Collections.Generic.List[string]]::new()
    $lines = $SourceText -split "\r?\n"

    foreach ($line in $lines) {
        $text = $line.Trim()
        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }

        $wasBullet = $false
        if ($text -match '^-+\s*(.+)$') {
            $text = $Matches[1].Trim()
            $wasBullet = $true
        }

        if ($wasBullet -and $text.Contains(':')) {
            $text = ($text -replace '^[^:]+:\s*', '').Trim()
        }

        if ([string]::IsNullOrWhiteSpace($text)) {
            continue
        }

        $paragraphs.Add($text)
    }

    if ($paragraphs.Count -eq 0) {
        return $SourceText
    }

    return ($paragraphs -join "`n`n")
}

function Normalize-TextPayload {
    param(
        [AllowNull()][string]$Value
    )

    if ($null -eq $Value) {
        return $null
    }

    return $Value.
        Replace([string][char]0x00AB, '"').
        Replace([string][char]0x00BB, '"')
}

function Normalize-PayloadObject {
    param(
        $Value
    )

    if ($null -eq $Value) {
        return $null
    }

    if ($Value -is [string]) {
        return Normalize-TextPayload -Value $Value
    }

    if ($Value -is [System.Collections.IDictionary]) {
        $normalized = [ordered]@{}
        foreach ($key in $Value.Keys) {
            $normalized[$key] = Normalize-PayloadObject -Value $Value[$key]
        }
        return $normalized
    }

    if ($Value -is [System.Collections.IEnumerable] -and -not ($Value -is [string])) {
        $items = @()
        foreach ($item in $Value) {
            $items += Normalize-PayloadObject -Value $item
        }
        return $items
    }

    return $Value
}

function Test-NormalizeTextPayload {
    $sample = "Он говорит, что {0}и так всё понятно{1} без обращения к инструкции" -f [string][char]0x00AB, [string][char]0x00BB
    $normalized = Normalize-TextPayload -Value $sample
    $expectedFragment = [string][char]0x0022 + "и так всё понятно" + [string][char]0x0022

    if ($normalized.Contains([string][char]0x00AB) -or $normalized.Contains([string][char]0x00BB)) {
        throw "Normalize-TextPayload self-check failed."
    }

    if (-not $normalized.Contains($expectedFragment)) {
        throw "Normalize-TextPayload self-check produced unexpected result: $normalized"
    }
}

function Test-AnswerOptionPayloadNormalization {
    $samplePayload = @{
        body = ("Он говорит, что {0}и так всё понятно{1} без обращения к инструкции" -f [string][char]0x00AB, [string][char]0x00BB)
        pairingKey = $null
        isCorrect = $true
        displayOrder = 0
        canonicalOrderPosition = $null
        answerOptionRole = "CHOICE_OPTION"
    }

    $json = Convert-ToJsonBody -Body $samplePayload -OperationLabel "self-check-option-normalization"
    $expectedFragment = [string][char]0x005C + [string][char]0x0022 + "и так всё понятно" + [string][char]0x005C + [string][char]0x0022

    if ($json.Contains([string][char]0x00AB) -or $json.Contains([string][char]0x00BB)) {
        throw "Answer option payload normalization self-check failed: runtime JSON still contains typographic angle quotes."
    }

    if (-not $json.Contains($expectedFragment)) {
        throw "Answer option payload normalization self-check produced unexpected result: $json"
    }
}

function New-TestQuestionSelectionPlan {
    param(
        [Parameter(Mandatory = $true)][object[]]$Tests,
        [Parameter(Mandatory = $true)][object[]]$Questions
    )

    $plan = [ordered]@{}
    $topicOverlapStats = [ordered]@{}

    foreach ($test in $Tests) {
        $testCode = $test.'Test code'
        $ownerTopicCode = Get-TestOwnerTopicCode -TestCode $testCode -TopicOrCourseCode $test.'Topic/course code'
        $plannedCount = [int]($test.'Questions in test'.Trim('`'))
        $topicQuestions = @(
            $Questions |
                Where-Object { $_.TopicCode -eq $ownerTopicCode } |
                Sort-Object QuestionCode
        )
        $explicitQuestions = @(
            $topicQuestions |
                Where-Object { $_.IntendedTests -contains $testCode }
        )

        $selected = New-Object System.Collections.Generic.List[object]
        $selectedCodes = New-Object 'System.Collections.Generic.HashSet[string]'

        foreach ($question in $explicitQuestions) {
            if ($selected.Count -ge $plannedCount) {
                break
            }
            if ($selectedCodes.Add($question.QuestionCode)) {
                $selected.Add($question)
            }
        }

        foreach ($question in $topicQuestions) {
            if ($selected.Count -ge $plannedCount) {
                break
            }
            if ($selectedCodes.Add($question.QuestionCode)) {
                $selected.Add($question)
            }
        }

        if ($selected.Count -lt $plannedCount) {
            throw "[$testCode] Topic bank preflight failed for $ownerTopicCode. Need $plannedCount questions, planned $($selected.Count)."
        }

        $explicitSelectedCount = 0
        foreach ($question in $selected) {
            if ($question.IntendedTests -contains $testCode) {
                $explicitSelectedCount++
            }
        }

        $plan[$testCode] = [pscustomobject]@{
            TestCode = $testCode
            OwnerTopicCode = $ownerTopicCode
            PlannedCount = $plannedCount
            ExplicitCount = $explicitQuestions.Count
            ExplicitSelectedCount = $explicitSelectedCount
            FallbackCount = $selected.Count - $explicitSelectedCount
            SelectedQuestions = @($selected.ToArray())
        }
    }

    foreach ($topicCode in ($plan.Values | Select-Object -ExpandProperty OwnerTopicCode | Sort-Object -Unique)) {
        $selectionCodes = @(
            foreach ($entry in $plan.Values | Where-Object { $_.OwnerTopicCode -eq $topicCode }) {
                foreach ($question in $entry.SelectedQuestions) {
                    $question.QuestionCode
                }
            }
        )
        $overlapGroups = @($selectionCodes | Group-Object | Where-Object { $_.Count -gt 1 })
        $topicOverlapStats[$topicCode] = [pscustomobject]@{
            TopicCode = $topicCode
            ReusedQuestionCount = $overlapGroups.Count
            ReusedSelectionCount = (($overlapGroups | ForEach-Object { $_.Count - 1 }) | Measure-Object -Sum).Sum
            ReusedQuestionCodes = @($overlapGroups | ForEach-Object { $_.Name })
        }
    }

    Write-Step "Local test composition preflight"
    foreach ($entry in $plan.Values) {
        Write-Host (
            "  {0}: topic={1}, required={2}, explicitSelected={3}, fallback={4}" -f
            $entry.TestCode,
            $entry.OwnerTopicCode,
            $entry.PlannedCount,
            $entry.ExplicitSelectedCount,
            $entry.FallbackCount
        ) -ForegroundColor DarkCyan
    }
    foreach ($stat in $topicOverlapStats.Values) {
        $codes = if ($stat.ReusedQuestionCodes.Count -gt 0) { $stat.ReusedQuestionCodes -join ", " } else { "<none>" }
        Write-Host (
            "  overlap {0}: reusedQuestions={1}, reusedSelections={2}, questionCodes={3}" -f
            $stat.TopicCode,
            $stat.ReusedQuestionCount,
            $stat.ReusedSelectionCount,
            $codes
        ) -ForegroundColor DarkYellow
    }

    return $plan
}

# Load source artifacts
Write-Step "Loading source artifacts from markdown files"

Write-Step "Running local normalization self-checks"
Test-NormalizeTextPayload
Test-AnswerOptionPayloadNormalization
Write-Host "Normalization self-checks passed" -ForegroundColor Green

$CourseRows = Get-MarkdownTableRows -Path $ContentMatrixPath -SectionHeader "## Courses"
$TopicRows = Get-MarkdownTableRows -Path $ContentMatrixPath -SectionHeader "## Topics"
$TestRows = Get-MarkdownTableRows -Path $ContentMatrixPath -SectionHeader "## Tests"
$QuestionRows = Parse-QuestionBank -Path $QuestionBankPath
$MaterialRows = Parse-MaterialsSource -Path $MaterialsSourcePath

if ($CourseRows.Count -ne 3) { throw "Expected 3 courses, got $($CourseRows.Count)" }
if ($TopicRows.Count -ne 8) { throw "Expected 8 topics, got $($TopicRows.Count)" }
if ($MaterialRows.Count -ne 27) { throw "Expected 27 materials, got $($MaterialRows.Count)" }
if ($QuestionRows.Count -ne 85) { throw "Expected 85 questions, got $($QuestionRows.Count)" }
if ($TestRows.Count -ne 13) { throw "Expected 13 tests, got $($TestRows.Count)" }
$TestQuestionSelectionPlan = New-TestQuestionSelectionPlan -Tests $TestRows -Questions $QuestionRows

Write-Host ("Loaded {0} courses, {1} topics, {2} materials, {3} questions, {4} tests" -f $CourseRows.Count, $TopicRows.Count, $MaterialRows.Count, $QuestionRows.Count, $TestRows.Count) -ForegroundColor Green

if ($PreflightOnly) {
    Write-Step "Preflight-only mode"
    Write-Host "Local helper preflight completed without API calls." -ForegroundColor Green
    return
}

Write-Step "Preflight auth check"
$MeResponse = Invoke-JsonGet -Path "/api/v1/me" -Context "preflight:/api/v1/me"
if ($null -eq $MeResponse -or $null -eq $MeResponse.actorUserId) {
    throw "[preflight:/api/v1/me] Successful actor response with actorUserId is required before content creation."
}
Write-Host ("Authenticated actor {0} ({1}) via mode {2}" -f $MeResponse.username, $MeResponse.actorUserId, $AuthMode) -ForegroundColor Green

if (-not $AllowNonEmptyContentBaseline) {
    Write-Warning "This helper is intended for a clean content baseline. If course/topic/material/question/test data already exists, stop and reset or use an explicit override before rerun."
}

# Create courses
Stop-IfRequestedBeforeSection -SectionName "Courses"
Write-Step "Creating courses"

$courseSortOrder = 10
foreach ($course in $CourseRows) {
    $courseCode = $course.'Course code'
    $body = @{
        name = $course.'Course name'
        description = $CourseDescriptions[$courseCode]
        sortOrder = $courseSortOrder
    }

    $response = Invoke-JsonPost -Path "/api/v1/expert/content/courses" -Body $body -Context "create-course:$courseCode"
    $id = Require-Id -Response $response -Context "create-course:$courseCode"
    $CourseIds[$courseCode] = $id
    Write-Host ("Created course {0} -> {1}" -f $courseCode, $id) -ForegroundColor Green
    Stop-IfRequestedAfterOperation -OperationLabel "create-course:$courseCode"
    $courseSortOrder += 10
}
Stop-IfRequestedAfterSection -SectionName "Courses"

# Create topics
Stop-IfRequestedBeforeSection -SectionName "Topics"
Write-Step "Creating topics"

$topicsByCourse = $TopicRows | Group-Object 'Course code'
foreach ($topicGroup in $topicsByCourse) {
    $sortOrder = 10
    foreach ($topic in $topicGroup.Group) {
        $topicCode = $topic.'Topic code'
        $courseCode = $topic.'Course code'
        $courseId = $CourseIds[$courseCode]

        $body = @{
            courseId = $courseId
            name = $topic.'Topic name'
            description = $TopicDescriptions[$topicCode]
            sortOrder = $sortOrder
        }

        $response = Invoke-JsonPost -Path "/api/v1/expert/content/topics" -Body $body -Context "create-topic:$topicCode"
        $id = Require-Id -Response $response -Context "create-topic:$topicCode"
        $TopicIds[$topicCode] = $id
        Write-Host ("Created topic {0} -> {1}" -f $topicCode, $id) -ForegroundColor Green
        Stop-IfRequestedAfterOperation -OperationLabel "create-topic:$topicCode"
        $sortOrder += 10
    }
}
Stop-IfRequestedAfterSection -SectionName "Topics"

# Create materials
Stop-IfRequestedBeforeSection -SectionName "Materials"
Write-Step "Creating materials"

foreach ($material in $MaterialRows) {
    $materialCode = $material.MaterialCode
    $topicCode = $material.TopicCode
    $topicId = $TopicIds[$topicCode]

    $body = @{
        topicId = $topicId
        name = $material.Name
        description = (New-MaterialDescription -Material $material)
        body = (New-MaterialBody -Material $material)
        materialType = $material.MaterialType
        sortOrder = $material.SortOrder
    }

    $response = Invoke-JsonPost -Path "/api/v1/expert/content/materials" -Body $body -Context "create-material:$materialCode"
    $id = Require-Id -Response $response -Context "create-material:$materialCode"
    $MaterialIds[$materialCode] = $id
    Write-Host ("Created material {0} -> {1}" -f $materialCode, $id) -ForegroundColor Green

    if (-not $script:FirstMaterialCreated) {
        $script:FirstMaterialCreated = $true
        Stop-IfRequestedAfterSection -SectionName "FirstMaterial"
    }
    Stop-IfRequestedAfterOperation -OperationLabel "create-material:$materialCode"
}
Stop-IfRequestedAfterSection -SectionName "Materials"

# Create questions and answer options
Stop-IfRequestedBeforeSection -SectionName "Questions"
Write-Step "Creating questions and answer options"

$questionIndexByTopic = @{}
foreach ($question in $QuestionRows) {
    $topicCode = $question.TopicCode
    if (-not $questionIndexByTopic.ContainsKey($topicCode)) {
        $questionIndexByTopic[$topicCode] = 0
    }

    $questionCode = $question.QuestionCode
    $topicId = $TopicIds[$topicCode]
    $sortOrder = 10 + ($questionIndexByTopic[$topicCode] * 10)
    $questionIndexByTopic[$topicCode] = $questionIndexByTopic[$topicCode] + 1

    $questionBody = @{
        topicId = $topicId
        body = (Normalize-TextPayload -Value $question.Text)
        questionType = $question.QuestionType
        sortOrder = $sortOrder
    }

    $questionResponse = Invoke-JsonPost -Path "/api/v1/expert/content/questions" -Body $questionBody -Context "create-question:$questionCode"
    $questionId = Require-Id -Response $questionResponse -Context "create-question:$questionCode"
    $QuestionIds[$questionCode] = $questionId
    Write-Host ("Created question {0} -> {1}" -f $questionCode, $questionId) -ForegroundColor Green
    Stop-IfRequestedAfterOperation -OperationLabel "create-question:$questionCode"

    $displayOrder = 0
    foreach ($option in $question.Options) {
        $optionCode = $option.OptionCode
        $isCorrect = $question.Correct -contains $optionCode
        $optionBody = @{
            body = (Normalize-TextPayload -Value $option.Text)
            answerOptionRole = "CHOICE_OPTION"
            isCorrect = $isCorrect
            displayOrder = $displayOrder
            pairingKey = $null
            canonicalOrderPosition = $null
        }

        $optionResponse = Invoke-JsonPost -Path "/api/v1/expert/content/questions/$questionId/answer-options" -Body $optionBody -Context "create-option:$optionCode"
        $optionId = Require-Id -Response $optionResponse -Context "create-option:$optionCode"
        $OptionIds[$optionCode] = $optionId
        Write-Host ("  Added option {0} -> {1}" -f $optionCode, $optionId) -ForegroundColor DarkGreen
        Stop-IfRequestedAfterOperation -OperationLabel "create-option:$optionCode"
        $displayOrder++
    }
}
Stop-IfRequestedAfterSection -SectionName "Questions"

# Create tests
Stop-IfRequestedBeforeSection -SectionName "Tests"
Write-Step "Creating tests"

$testsByOwnerTopic = @{}
foreach ($test in $TestRows) {
    $testCode = $test.'Test code'
    $ownerTopicCode = Get-TestOwnerTopicCode -TestCode $testCode -TopicOrCourseCode $test.'Topic/course code'
    if (-not $testsByOwnerTopic.ContainsKey($ownerTopicCode)) {
        $testsByOwnerTopic[$ownerTopicCode] = New-Object System.Collections.Generic.List[object]
    }
    $testsByOwnerTopic[$ownerTopicCode].Add($test)
}

foreach ($ownerTopicCode in $testsByOwnerTopic.Keys) {
    $sortOrder = 10
    foreach ($test in $testsByOwnerTopic[$ownerTopicCode]) {
        $testCode = $test.'Test code'
        $topicId = $TopicIds[$ownerTopicCode]
        $threshold = $ThresholdByTestCode[$testCode]
        if ($null -eq $threshold) {
            throw "No threshold configured for $testCode"
        }

        $body = @{
            topicId = $topicId
            name = $test.'Test name'
            description = $test.'Role in scenario'
            testType = $test.'Test type'
            thresholdPercent = $threshold
            scoringPolicyCode = "DEFAULT"
            sortOrder = $sortOrder
        }

        $response = Invoke-JsonPost -Path "/api/v1/expert/content/tests" -Body $body -Context "create-test:$testCode"
        $id = Require-Id -Response $response -Context "create-test:$testCode"
        $TestIds[$testCode] = $id
        Write-Host ("Created test {0} -> {1}" -f $testCode, $id) -ForegroundColor Green
        Stop-IfRequestedAfterOperation -OperationLabel "create-test:$testCode"
        $sortOrder += 10
    }
}
Stop-IfRequestedAfterSection -SectionName "Tests"

# Link questions to tests
Stop-IfRequestedBeforeSection -SectionName "Links"
Write-Step "Linking questions to tests"

foreach ($test in $TestRows) {
    $testCode = $test.'Test code'
    $testId = $TestIds[$testCode]
    $selectionPlan = $TestQuestionSelectionPlan[$testCode]
    if ($null -eq $selectionPlan) {
        throw "[$testCode] Missing local test composition plan."
    }

    $selectedQuestions = @($selectionPlan.SelectedQuestions)
    $displayOrder = 0
    foreach ($question in $selectedQuestions) {
        $questionCode = $question.QuestionCode
        $questionId = $QuestionIds[$questionCode]
        if ($null -eq $questionId) {
            throw "[$testCode] Missing runtime id for $questionCode"
        }

        $body = @{
            questionId = $questionId
            displayOrder = $displayOrder
            weight = 1
        }

        $response = Invoke-JsonPost -Path "/api/v1/expert/content/tests/$testId/questions" -Body $body -Context "link-test-question:${testCode}:${questionCode}"
        [void](Require-Id -Response $response -Context "link-test-question:${testCode}:${questionCode}")
        Write-Host ("Linked {0} -> {1}" -f $testCode, $questionCode) -ForegroundColor DarkGreen
        Stop-IfRequestedAfterOperation -OperationLabel "link-test-question:${testCode}:${questionCode}"
        $displayOrder++
    }
}
Stop-IfRequestedAfterSection -SectionName "Links"

# Publish content entities
Stop-IfRequestedBeforeSection -SectionName "PublishCourses"
Write-Step "Publishing courses"
foreach ($courseCode in ($CourseIds.Keys | Sort-Object)) {
    Publish-Entity -EntityPath "courses" -Id $CourseIds[$courseCode] -PlanningCode $courseCode | Out-Null
    Stop-IfRequestedAfterOperation -OperationLabel "publish:$courseCode"
}
Stop-IfRequestedAfterSection -SectionName "PublishCourses"

Stop-IfRequestedBeforeSection -SectionName "PublishTopics"
Write-Step "Publishing topics"
foreach ($topicCode in ($TopicIds.Keys | Sort-Object)) {
    Publish-Entity -EntityPath "topics" -Id $TopicIds[$topicCode] -PlanningCode $topicCode | Out-Null
    Stop-IfRequestedAfterOperation -OperationLabel "publish:$topicCode"
}
Stop-IfRequestedAfterSection -SectionName "PublishTopics"

Stop-IfRequestedBeforeSection -SectionName "PublishMaterials"
Write-Step "Publishing materials"
foreach ($materialCode in ($MaterialIds.Keys | Sort-Object)) {
    Publish-Entity -EntityPath "materials" -Id $MaterialIds[$materialCode] -PlanningCode $materialCode | Out-Null
    Stop-IfRequestedAfterOperation -OperationLabel "publish:$materialCode"
}
Stop-IfRequestedAfterSection -SectionName "PublishMaterials"

Stop-IfRequestedBeforeSection -SectionName "PublishQuestions"
Write-Step "Publishing questions"
foreach ($questionCode in ($QuestionIds.Keys | Sort-Object)) {
    Publish-Entity -EntityPath "questions" -Id $QuestionIds[$questionCode] -PlanningCode $questionCode | Out-Null
    Stop-IfRequestedAfterOperation -OperationLabel "publish:$questionCode"
}
Stop-IfRequestedAfterSection -SectionName "PublishQuestions"

Stop-IfRequestedBeforeSection -SectionName "PublishTests"
Write-Step "Publishing tests"
foreach ($testCode in ($TestIds.Keys | Sort-Object)) {
    Publish-Entity -EntityPath "tests" -Id $TestIds[$testCode] -PlanningCode $testCode | Out-Null
    Stop-IfRequestedAfterOperation -OperationLabel "publish:$testCode"
}
Stop-IfRequestedAfterSection -SectionName "PublishTests"

# Assign active final control tests for assigned topics
Stop-IfRequestedBeforeSection -SectionName "FinalControls"
Write-Step "Assigning active final control tests for assigned topics"

$FinalControlMap = [ordered]@{
    "TOPIC-OPS-INTRO" = "TEST-OPS-INTRO-FINAL"
    "TOPIC-OPS-UNIT-SAFE" = "TEST-OPS-UNIT-FINAL"
    "TOPIC-OPS-INCIDENT" = "TEST-OPS-INCIDENT-FINAL"
    "TOPIC-PERMIT-PREP" = "TEST-PERMIT-PREP-FINAL"
    "TOPIC-PERMIT-RISK" = "TEST-PERMIT-RISK-FINAL"
    "TOPIC-PERMIT-ROLES" = "TEST-PERMIT-ROLES-FINAL"
}

foreach ($topicCode in $FinalControlMap.Keys) {
    $testCode = $FinalControlMap[$topicCode]
    $topicId = $TopicIds[$topicCode]
    $testId = $TestIds[$testCode]
    [void](Invoke-JsonPost -Path "/api/v1/expert/content/topics/$topicId/active-final-tests/$testId/assign" -Context "assign-final:${topicCode}:${testCode}")
    Write-Host ("Assigned active final test {0} -> {1}" -f $topicCode, $testCode) -ForegroundColor Green
    Stop-IfRequestedAfterOperation -OperationLabel "assign-final:${topicCode}:${testCode}"
}
Stop-IfRequestedAfterSection -SectionName "FinalControls"

# Print final mapping summary
Write-Step "Final mapping summary"

Write-Host "Courses:" -ForegroundColor Yellow
$CourseIds.GetEnumerator() | Sort-Object Name | ForEach-Object { Write-Host ("  {0} = {1}" -f $_.Key, $_.Value) }

Write-Host "Topics:" -ForegroundColor Yellow
$TopicIds.GetEnumerator() | Sort-Object Name | ForEach-Object { Write-Host ("  {0} = {1}" -f $_.Key, $_.Value) }

Write-Host "Materials: $($MaterialIds.Count)" -ForegroundColor Yellow
Write-Host "Questions: $($QuestionIds.Count)" -ForegroundColor Yellow
Write-Host "Options: $($OptionIds.Count)" -ForegroundColor Yellow
Write-Host "Tests: $($TestIds.Count)" -ForegroundColor Yellow

# Print verification reminder
Write-Step "Verification reminder"
Write-Host "Before using the created content for assignments or self-testing, verify:" -ForegroundColor Magenta
Write-Host ("  - content checkpoints in {0}" -f $AcceptancePath)
Write-Host "  - course count = 3"
Write-Host "  - topic count = 8"
Write-Host "  - material count = 27"
Write-Host "  - question count = 85"
Write-Host "  - test count = 13"
Write-Host "  - active final control exists for all assigned topics"
Write-Host "  - self tests remain published and are not active final controls"
