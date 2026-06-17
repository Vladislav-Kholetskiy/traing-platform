# Demo-Data Preparation Report

## Purpose

Prepare `demo-data` as an executable acceptance kit for repeatable verification of all currently implemented functionality on the Java/Spring backend project `training-platform`.

## Scope

- All contours except maintenance contour are treated as implemented.
- maintenance contour is treated as implemented only through Stage 0-2.
- maintenance contour post-Stage-2 analytics rebuild, aggregate population, and managerial historical analytics are `NOT_IMPLEMENTED_BY_SCOPE`.

## Changed Files

- `demo-data/imports/baseline/bootstrap-demo-import.sql`
- `demo-data/imports/baseline/personnel-demo-baseline.sql`
- `demo-data/scripts/create-demo-content.ps1`
- `demo-data/scripts/reset-demo-runtime-slice.ps1`
- `demo-data/scripts/verify-implemented-functional-state.ps1`
- `demo-data/scripts/check-implemented-functional-api-smoke.ps1`
- `demo-data/scenarios/implemented-functional-test-runbook.md`
- `demo-data/scenarios/implemented-functional-test-scope.md`
- `demo-data/scenarios/acceptance-checkpoints.md`
- `demo-data/scenarios/role-runbook.md`
- `demo-data/scenarios/demo-data-preparation-report.md`

## Prepared Scripts

### Reset script

- [reset-demo-runtime-slice.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/reset-demo-runtime-slice.ps1)
- Safe single-DB cleanup for allowlisted DB `training_platform_demo_fullcycle`
- Cleans only demo-generated runtime slice
- Preserves users, org units, org-unit types, user-role assignments, and user-organization assignments

### Content helper

- [create-demo-content.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/create-demo-content.ps1)
- Default mode is full run
- Default auth is `X-Demo-Actor-Id: 1`
- Final-boundary JSON normalization is active
- Local no-API mode `-PreflightOnly` was added for normalization/composition checks

### Read-only verification script

- [verify-implemented-functional-state.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/verify-implemented-functional-state.ps1)
- DB allowlist guard
- Read-only SQL verification only
- Reports `PASS`, `FAIL`, `WARN`

### API smoke script

- [check-implemented-functional-api-smoke.ps1](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scripts/check-implemented-functional-api-smoke.ps1)
- Semi-automated endpoint smoke using live runtime plus read-only SQL id discovery
- Covers main positive reads and fail-closed negative checks

## Prepared Scenario Artifacts

- versioned baseline SQL:
  - [bootstrap-demo-import.sql](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/baseline/bootstrap-demo-import.sql)
  - [personnel-demo-baseline.sql](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/imports/baseline/personnel-demo-baseline.sql)
- [implemented-functional-test-runbook.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/implemented-functional-test-runbook.md)
- [implemented-functional-test-scope.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/implemented-functional-test-scope.md)
- [acceptance-checkpoints.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/acceptance-checkpoints.md)
- [role-runbook.md](D:/Users/vladi/Desktop/Diplom/Java/training-platform/demo-data/scenarios/role-runbook.md)

## Expected Run Sequence

1. Start Docker and backend in `dev` profile on `training_platform_demo_fullcycle`
2. Restore baseline using versioned inputs from `demo-data/imports/baseline/` plus current org import
3. Run `reset-demo-runtime-slice.ps1`
4. Optionally run `create-demo-content.ps1 -PreflightOnly`
5. Run `create-demo-content.ps1`
6. Launch `3` assignment campaigns
7. Run `verify-implemented-functional-state.ps1`
8. Run `check-implemented-functional-api-smoke.ps1`
9. Complete manual role flows from `role-runbook.md`

## Expected Final Counts

- `organizational_unit_type = 6`
- `organizational_unit = 13`
- `app_user = 52`
- `user_organization_assignment = 51`
- `user_role_assignment = 103`
- `course = 3`
- `topic = 8`
- `material = 27`
- `question = 85`
- `answer_option = 340`
- `test = 13`
- `test_question = 91`
- `assignment_campaign = 3`
- `assignment = 24`
- `assignment_test = 72`
- `test_attempt = 4`
- `result = 4`
- `analytics_user_topic_aggregate = 0`
- `analytics_department_topic_aggregate = 0`

## Implemented Functionality Covered

- dev auth and admin profile
- org/personnel/user baseline
- content creation and publication
- tests, questions, answer options, test links
- active final controls
- assignment launch
- multi-employee assigned learning
- assigned attempt success and failure contours
- self-testing contour
- result recording and separation of self vs assigned
- manager current supervision
- negative access checks
- notification read surfaces

## Multi-Employee Assigned Testing Coverage

- `OPERATOR-1`:
  - sees own assignment
  - opens detail and learning context
  - starts assigned attempt
  - saves correct answers
  - submits passing result
- `OPERATOR-2`:
  - sees own assignment
  - starts separate assigned attempt
  - saves wrong/partially wrong answers
  - submits failed/lower-score result
- Verification expectations:
  - distinct `test_attempt` rows
  - distinct `result` rows
  - both results stay in `ASSIGNED` contour
  - self history does not contain assigned results
  - manager current supervision sees both employees in scope

## Notification Coverage

- `IMPLEMENTED_AND_PASSING`:
  - `GET /api/v1/admin/notifications`
  - `GET /api/v1/self/notifications`
- `NOT_RUNTIME_SURFACE`:
  - assignment/result/personnel notification emission
  - notification rule mutation runtime

Reasoning:
- notification read controllers are published and smoke-checkable
- current implemented-scope run on the demo DB leaves `notification = 0`
- no assignment/result/personnel runtime emitter wiring was found in the current contour
- empty notification tables therefore do not fail acceptance

## Scope Caveats

The following stay outside mandatory acceptance:

- analytics rebuild runtime
- analytics aggregate population runtime
- managerial historical analytics user-topic
- managerial historical analytics department-topic

These are explicitly `NOT_IMPLEMENTED_BY_SCOPE` because they belong to maintenance contour post-Stage-2.

## Safety Guards

- DB allowlist: only `training_platform_demo_fullcycle`
- no cleanup of org/personnel/users baseline tables
- no production DB cleanup path
- verification script is read-only
- helper preflight mode performs no API calls
- `tmp` is disposable-only and is not part of the mandatory acceptance source-of-truth path

## Parser / Check Results

- `create-demo-content.ps1`: `PARSER_OK`
- `reset-demo-runtime-slice.ps1`: `PARSER_OK`
- `verify-implemented-functional-state.ps1`: `PARSER_OK`
- `check-implemented-functional-api-smoke.ps1`: `PARSER_OK`
- local helper preflight:
  - normalization self-check passed
  - composition preflight passed
  - `-PreflightOnly` completed without API calls
- read-only verification:
  - finished with `PASS`
  - warnings only:
    - analytics aggregates remain empty because maintenance contour post-Stage-2 is `NOT_IMPLEMENTED_BY_SCOPE`
    - notification table remains empty because notification emission is not part of the current runtime contour
- API smoke:
  - finished with `PASS`
  - positive reads passed for admin, expert, assigned, self, managerial current supervision, and notification reads
  - negative fail-closed checks passed
- notification inventory check:
  - notification tables exist in schema
  - admin/self notification read controllers exist
  - assignment/result/personnel notification emission is not wired into the implemented acceptance contour

## Preparation Verdict

`DEMO_DATA_READY_WITH_SCOPE_CAVEATS`
