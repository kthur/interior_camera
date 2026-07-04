# Scope: E2E Testing Track

## Objective
Design and implement a comprehensive, requirement-driven, opaque-box E2E test suite for the 5 key UI/UX improvements in the FitCheck AR Android application.
Upon completion, publish `TEST_READY.md` at project root.

## Architecture & Code Layout
- Target tests should be placed in:
  - `app/src/androidTest/java/com/example/interiorcamera/` (Instrumented tests for UI integration)
  - `app/src/test/java/com/example/interiorcamera/` (Local unit/JVM tests for behavior logic)

---

## Test Case Requirements (4-Tier Approach)
Enumerate every feature and design tests covering:
- **Tier 1 - Feature Coverage (>=5 per feature)**: Happy-path tests for each of the 5 features.
- **Tier 2 - Boundary & Corner Cases (>=5 per feature)**: Boundary conditions, limit values, empty inputs, negative values.
- **Tier 3 - Cross-Feature Combinations (pairwise)**: Test feature interactions (e.g., rotation buttons + gestures, opacity + multi-anchor).
- **Tier 4 - Real-World Application Scenarios**: High-level user scenarios naturally combining features.

Given 5 features (R1-R5), the suite should include at least:
- Tier 1: 25 test cases
- Tier 2: 25 test cases
- Tier 3: 5 test cases
- Tier 4: 5 test cases
- **Total Minimum: 60 test cases**

---

## Deliverables
1. `d:\project\interior_camera\TEST_INFRA.md` at project root detailing features, methodology, test runner commands, and layout.
2. Complete test suite code in `app/src/androidTest` and/or `app/src/test`.
3. `d:\project\interior_camera\TEST_READY.md` at project root summarizing count and passing status of the E2E test cases.
