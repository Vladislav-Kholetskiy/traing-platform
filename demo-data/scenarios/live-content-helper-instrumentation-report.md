# Live Content Helper Instrumentation Report

## Why instrumentation was needed

Последний controlled content run остановился после создания `3` courses и `8` topics на первом `POST /api/v1/expert/content/materials`.

Ключевая проблема была не только в самом `400 Bad Request`, а в том, что helper не выводил достаточную диагностику:

- не сохранял serialized request body;
- не печатал exact response body;
- не сохранял response headers;
- не давал удобного operation-level trail по planning code.

При этом isolated manual repro к тому же endpoint прошёл успешно. Значит, для следующего clean rerun нужно сначала сделать helper диагностически прозрачным, а уже потом повторять content creation на fresh baseline.

## What changed in the script

В [create-demo-content.ps1](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1:1) добавлены:

- configurable diagnostics switches:
  - `$DumpRequests = $true`
  - `$LogDir = "tmp/demo-content-run-logs"`
- manual baseline safety switch:
  - `$AllowNonEmptyContentBaseline = $false`
- controlled stop switches:
  - `$StopAfterSection`
  - `$StopBeforeSection`
  - `$StopAfterOperation`
- JSON/debug helpers:
  - `Convert-ToJsonBody`
  - `Get-SafeOperationFileLabel`
  - `Ensure-DebugLogDir`
  - `Write-RequestDebugLog`
  - `Get-HttpErrorDetails`
- stop-control helpers:
  - `Stop-IfRequestedBeforeSection`
  - `Stop-IfRequestedAfterSection`
  - `Stop-IfRequestedAfterOperation`

## Debug artifacts that will be created

При следующем live run helper будет писать request/response diagnostics в `tmp/demo-content-run-logs`.

Ожидаемый формат файлов:

- `0001-create-course-COURSE-OPS-SAFETY.request.json`
- `0001-create-course-COURSE-OPS-SAFETY.response.json`
- при ошибке:
  - `0001-create-course-COURSE-OPS-SAFETY.error.txt`

Для каждого JSON API call фиксируются:

- HTTP method;
- URL;
- operation label;
- serialized request body;
- response body на успешном вызове;
- response headers и response body на ошибке, если они доступны.

## How to isolate the first failing material call

Для controlled rerun после reset рекомендуется использовать stop switches:

- `$StopAfterSection = "Courses"`
- `$StopAfterSection = "Topics"`
- `$StopAfterSection = "FirstMaterial"`

Практический порядок:

1. Reset/fresh demo DB.
2. Org bootstrap.
3. Personnel bootstrap.
4. Run helper with:
   - `$StopAfterSection = "Topics"` to confirm stable pre-material baseline.
5. Reset again if needed, then rerun with:
   - `$StopAfterSection = "FirstMaterial"`
6. If the first material call fails, inspect:
   - `tmp/demo-content-run-logs/*.request.json`
   - `tmp/demo-content-run-logs/*.response.json`
   - `tmp/demo-content-run-logs/*.error.txt`

## Why the current partial DB must not be continued

Текущее состояние после blocked run уже не является clean content baseline:

- `course = 3`
- `topic = 8`
- `material = 2`
- `question = 0`
- `test = 0`

Это ломает безопасный смысл helper-а:

- runtime IDs уже частично заняты;
- следующий прогон не даст чистого causal trail;
- repeated calls могут замаскировать исходный material blocker;
- request/response logs следующего прогона будут смешаны с partial state.

Продолжать поверх этой DB нельзя.

## Recommended reset strategy

Рекомендуемая стратегия перед следующим live rerun:

1. Поднять fresh demo DB или вернуть clean snapshot до content stage.
2. Перезапустить backend на этой clean DB.
3. Повторно выполнить:
   - org structure bootstrap;
   - personnel baseline/bootstrap.
4. Убедиться, что content baseline снова пуст:
   - `course = 0`
   - `topic = 0`
   - `material = 0`
   - `question = 0`
   - `test = 0`
5. Запустить helper с instrumentation и stop switches.
6. Только после получения точного failing request/response решать, требуется ли payload fix, helper-path fix или runtime contract investigation.
