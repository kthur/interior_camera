# Plan - FitCheck AR UI/UX Implementation

This plan outlines the steps to coordinate the implementation of 5 key UI/UX improvements in the FitCheck AR application.

## Strategy & Iteration Loop
For each implementation milestone, we will follow the orchestrator loop:
1. **Explorer**: Spawn 3 explorers to analyze the requirements, review target files, and recommend a strategy.
2. **Worker**: Spawn a worker to implement the chosen strategy, run builds, and execute unit/local tests.
3. **Reviewers**: Spawn 2 reviewers independently to review correctness, API/layout compatibility, and verify tests pass.
4. **Challengers**: Spawn 2 challengers to dynamically check correctness.
5. **Auditor**: Spawn a forensic auditor to verify that the implementation is genuine and clean.
6. **Gate Check**: Pass if build/tests succeed, all reviewers approve, challengers verify, and the auditor reports CLEAN.

For Milestone 5:
- Wait for the E2E Testing Orchestrator to publish `TEST_READY.md`.
- Run E2E test suite.
- Generate and run adversarial checks.

## Milestones Details

### Milestone 1: Custom Preset Storage ("My List") & Main Screen (R5)
- **Objective**: Implement local storage of custom presets (width, height, depth, name). Add custom items to preset list and main form.
- **Target Files**: `DataRepository.kt`, `MainScreen.kt`, `MainScreenViewModel.kt`.

### Milestone 2: Basic AR Overlay & Plane Detection Guide UI (R1, R3)
- **Objective**: Opacity slider to adjust model alpha. Plane detection guide coachmark UI.
- **Target Files**: `ArScreen.kt`.

### Milestone 3: Multi-Anchor Placement & Model Interaction (R4)
- **Objective**: Support placing multiple models. Focus model highlight, select/deselect model, and deletion option.
- **Target Files**: `ArScreen.kt`.

### Milestone 4: 3D Rotation Controls & Gestures (R2)
- **Objective**: Precise rotation controls (slider or buttons) + standard twist gestures for rotation.
- **Target Files**: `ArScreen.kt`.

### Milestone 5: E2E Verification & Adversarial Hardening (Tier 5)
- **Objective**: Run E2E tests, find gaps, and add white-box tests to harden coverage.
- **Target Files**: Full codebase.
