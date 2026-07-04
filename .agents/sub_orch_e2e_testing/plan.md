# Plan: FitCheck AR E2E Testing Suite

## Objective
Design and implement a comprehensive E2E test suite consisting of at least 60 test cases covering:
- R1: Opacity Slider Control
- R2: 3D Rotation Controls (Gestures & Buttons)
- R3: Plane Detection Guide UI
- R4: Multi-Anchor Placement
- R5: Custom Preset Storage ("My List")

## Milestones

### Milestone 1: Test Suite Design & Infrastructure Setup
- **Goal**: Establish the testing setup, analyze the application dependencies and codebase structure to ensure test compilability/runnability, and define the test plan in `TEST_INFRA.md`.
- **Inputs**: Codebase files, `PROJECT.md`, `SCOPE.md`.
- **Outputs**: `TEST_INFRA.md` containing the feature inventory, test runner configuration, and setup description.
- **Verification**: Code compiles and the test infrastructure skeleton is verified.

### Milestone 2: Tier 1 Test Case Implementation (Feature Coverage)
- **Goal**: Implement 25 tests (5 per feature R1-R5) covering happy-path interactions.
- **Inputs**: `TEST_INFRA.md`, codebase.
- **Outputs**: Tier 1 test cases in the test directories.
- **Verification**: Compilation and test runs verify Tier 1 tests.

### Milestone 3: Tier 2 Test Case Implementation (Boundary & Corner Cases)
- **Goal**: Implement 25 tests (5 per feature R1-R5) covering bounds, limits, empty lists, extreme inputs, negative scenarios.
- **Inputs**: Tier 1 test code, codebase.
- **Outputs**: Tier 2 test cases in the test directories.
- **Verification**: Compilation and test runs verify Tier 2 tests.

### Milestone 4: Tier 3 & 4 Test Case Implementation (Cross-Feature & Real-World)
- **Goal**: Implement 5 Tier 3 (pairwise combinations) and 5 Tier 4 (real-world app workflows) tests.
- **Inputs**: Existing test code, codebase.
- **Outputs**: Tier 3 & Tier 4 test cases.
- **Verification**: Compilation and test runs verify all 60 test cases.

### Milestone 5: E2E Verification & TEST_READY.md Publication
- **Goal**: Execute the full test suite, verify compilation, pass rate, and publish `TEST_READY.md`.
- **Inputs**: All implemented tests.
- **Outputs**: `TEST_READY.md` summarizing final test outcomes.
- **Verification**: All 60+ tests compiled and verified by subagents.
