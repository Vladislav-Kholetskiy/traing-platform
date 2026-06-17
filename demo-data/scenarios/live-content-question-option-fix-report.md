# Live Content Question Option Fix Report

## Run metadata

- Date/time: `2026-05-13 Europe/Moscow`
- Branch: `codex/front`
- Scope: read-only backend contract/log inspection plus demo-helper payload fix only
- Production code: unchanged
- Runtime DB used for failure evidence: `training_platform_questions_rerun`
- Failure correlationId: `a036b3d3-6e11-4132-8f6b-2370d8d935ff`

## Failure recap

Controlled questions run failed on:
- operation: `create-option:Q-OPS-INTRO-011-A`
- endpoint: `POST /api/v1/expert/content/questions/11/answer-options`
- status: `400 Bad Request`

Failing request body:

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

Response body:

```json
{"timestamp":"2026-05-13T17:24:43.794656700Z","status":400,"error":"Bad Request","message":"Request body is invalid","correlationId":"a036b3d3-6e11-4132-8f6b-2370d8d935ff"}
```

## Contract inspection result

Inspected:
- [SaveAnswerOptionRequest.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/controller/dto/SaveAnswerOptionRequest.java:1)
- [QuestionController.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/controller/QuestionController.java:1)
- [QuestionCommandServiceImpl.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/service/QuestionCommandServiceImpl.java:1)
- [ContentCommandSupport.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/service/ContentCommandSupport.java:1)
- [GlobalExceptionHandler.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/common/web/GlobalExceptionHandler.java:1)
- [AnswerOptionRole.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/domain/AnswerOptionRole.java:1)
- [QuestionType.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/domain/QuestionType.java:1)
- [AnswerOption.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/content/domain/AnswerOption.java:1)
- [V100__full_schema_stack.sql](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/resources/db/migration/V100__full_schema_stack.sql:1)

Confirmed contract:
- `body`: required, `@NotBlank`
- `answerOptionRole`: required, `@NotNull`
- `displayOrder`: `@PositiveOrZero`
- `pairingKey`: optional
- `canonicalOrderPosition`: optional, `@PositiveOrZero` when present
- `isCorrect`: optional at DTO level, but required by service/domain for `CHOICE_OPTION`

Confirmed semantics:
- `displayOrder = 0` is allowed
- `isCorrect = true` for `CHOICE_OPTION` is allowed
- `canonicalOrderPosition = null` for `CHOICE_OPTION` is allowed
- `pairingKey = null` for `CHOICE_OPTION` is allowed

Schema findings:
- `answer_option.body` is `text not null`
- `display_order >= 0`
- `canonical_order_position is null or >= 0`
- no DB length limit was found for `body`

Conclusion from contract:
- helper shape for `CHOICE_OPTION` was valid
- no service/domain/schema rule explains this `400`

## Backend log evidence

Observed handler behavior:
- response message was exactly `Request body is invalid`
- in [GlobalExceptionHandler.java](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/src/main/java/com/vladislav/training/platform/common/web/GlobalExceptionHandler.java:1) this message is produced by `HttpMessageNotReadableException`

Log inspection result:
- searched current backend logs and local rotated logs for correlationId `a036b3d3-6e11-4132-8f6b-2370d8d935ff`
- no field-level `MethodArgumentNotValidException`, `ValidationException`, enum-binding message, or DB constraint detail was found
- no richer stack trace tied to that correlation id was emitted into the captured backend logs

Conclusion from logs:
- exact parser-level cause is not directly logged
- strongest confirmed fact is that failure happens before controller/service validation path returns a domain-specific message

## Successful-vs-failing request comparison

Compared:
- successful request: [0089-create-option-Q-OPS-INTRO-010-D.request.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0089-create-option-Q-OPS-INTRO-010-D.request.json:1)
- failing request: [0091-create-option-Q-OPS-INTRO-011-A.request.json](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/tmp/demo-content-run-logs/0091-create-option-Q-OPS-INTRO-011-A.request.json:1)

What stayed the same:
- same endpoint family
- same JSON property names
- same `answerOptionRole = "CHOICE_OPTION"`
- same `isCorrect = true`
- same `pairingKey = null`
- same `canonicalOrderPosition = null`
- both questions are `MULTIPLE_CHOICE`

What changed:
- `questionId`: `10` -> `11`
- `displayOrder`: `3` -> `0`
- `body` text

Why `displayOrder` is not the likely cause:
- `displayOrder = 0` is explicitly allowed by DTO/service/domain/schema
- many earlier successful options already used `displayOrder = 0`

Why question type is not the likely cause:
- both question `10` and question `11` are `MULTIPLE_CHOICE`
- `CHOICE_OPTION` is valid for `MULTIPLE_CHOICE`

Body comparison:
- successful body:
  - `Требование не обходить блокировки и не действовать вне допуска`
- failing body:
  - `Он говорит, что «и так всё понятно» без обращения к инструкции`

Important delta:
- failing body contains typographic angle quotes `« »`
- successful comparator does not

Additional source inspection:
- `question-bank.md` contains multiple payload texts with `« »`
- the very first such answer option in the helper run is exactly `Q-OPS-INTRO-011-A`

## Root cause verdict

`OPTION_BODY_SPECIAL_CHAR`

Confidence note:
- this is the strongest evidence-based payload root cause
- it is not backed by a field-level backend exception message
- but it is the only runtime-relevant delta left after contract comparison:
  - DTO/service/schema accept the other fields
  - a same-shape request without `« »` succeeds
  - the first request with `« »` fails via `HttpMessageNotReadableException` path

## Files changed

- [create-demo-content.ps1](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1:1)

## Exact payload/helper change

Added helper function:
- `Normalize-TextPayload`

Behavior:
- replaces `«` with `"`
- replaces `»` with `"`

Applied to:
- question create payload body
- answer option create payload body

Source markdown was intentionally not changed:
- [question-bank.md](/D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/question-bank.md:1) remains the authoring source of truth
- normalization is runtime-payload only

## Verification plan for next clean questions rerun

1. Create a fresh clean demo DB.
2. Restore the same demo baseline.
3. Keep helper in:
   - `AuthMode = "DemoHeader"`
   - `DumpRequests = $true`
   - `StopAfterSection = "Questions"`
4. Rerun helper from clean baseline.
5. Verify:
   - `course = 3`
   - `topic = 8`
   - `material = 27`
   - `question = 85`
   - `test = 0`
   - `answer_option = 340`
6. Recheck:
   - each question has at least `4` options
   - `SINGLE_CHOICE` has exactly `1` correct option
   - `MULTIPLE_CHOICE` has at least `2` correct options
