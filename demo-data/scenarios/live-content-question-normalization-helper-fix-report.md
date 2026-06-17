## Failure recap

Fresh Questions rerun повторно упал на `create-option:Q-OPS-INTRO-011-A` при `POST /api/v1/expert/content/questions/11/answer-options` с `400 Bad Request` и `Request body is invalid`.

Captured request body показал, что в runtime JSON всё ещё уходили типографские угловые кавычки `« »`:

```json
{
  "body": "Он говорит, что «и так всё понятно» без обращения к инструкции",
  "pairingKey": null,
  "isCorrect": true,
  "displayOrder": 0,
  "canonicalOrderPosition": null,
  "answerOptionRole": "CHOICE_OPTION"
}
```

## Why previous normalization was insufficient

Предыдущее исправление вызывало `Normalize-TextPayload` при сборке отдельных строковых полей, но не на последней границе перед JSON serialization.

Из-за этого не было жёсткой гарантии, что фактический объект, который сериализуется в runtime request body, уже полностью нормализован. Любой path, дошедший до `Convert-ToJsonBody` с ненормализованной вложенной строкой, мог снова отправить `« »` в API.

## What changed in helper

В `demo-data/scripts/create-demo-content.ps1` добавлены:

- `Normalize-PayloadObject` для рекурсивной нормализации строк внутри `hashtable`, `ordered dictionary`, коллекций и вложенных значений.
- Обновлённый `Convert-ToJsonBody`, который сначала вызывает `Normalize-PayloadObject`, а затем сериализует уже нормализованный объект.
- Runtime guard после сериализации, который аварийно останавливает выполнение, если итоговый JSON всё ещё содержит `«` или `»`.
- `Test-NormalizeTextPayload` и `Test-AnswerOptionPayloadNormalization` как локальные self-check функции без необходимости backend/API.

## Boundary where normalization is now applied

Нормализация теперь применяется на финальной boundary перед `ConvertTo-Json` внутри `Convert-ToJsonBody`.

Это означает:

- debug request log получает уже нормализованный JSON;
- фактический HTTP request body получает тот же самый нормализованный JSON;
- больше нет расхождения между `*.request.json` и телом запроса, отправленным через `Invoke-RestMethod`.

## Guard against « » in runtime JSON

После сериализации в `Convert-ToJsonBody` добавлен guard:

```powershell
if ($json.Contains([string][char]0x00AB) -or $json.Contains([string][char]0x00BB)) {
    throw "Runtime JSON still contains typographic angle quotes for operation '$OperationLabel'. Normalization did not apply."
}
```

Если нормализация снова не дойдёт до runtime JSON, helper остановится до любого API call.

## Local self-check result

Добавлены две локальные проверки:

- `Test-NormalizeTextPayload` подтверждает, что строка с `« »` преобразуется к строке с обычными `"`.
- `Test-AnswerOptionPayloadNormalization` строит sample payload для answer option и пропускает его через тот же `Convert-ToJsonBody`, что используется runtime path.

Ожидаемый результат self-check:

- входной payload содержит `« »`;
- serialized runtime JSON содержит обычные `\"`;
- serialized runtime JSON не содержит `«` или `»`.

## Parser check result

После правки должен проходить PowerShell parser check для `demo-data/scripts/create-demo-content.ps1`.

## Ready/not ready for next clean Questions rerun

Ready for next clean Questions rerun на стороне helper.

Ограничение: этот отчёт подтверждает локальную правку boundary, guard и self-check path без выполнения backend/API calls в текущем шаге.
