## 2026-07-04T09:00:21Z

You are the E2E Test Suite Developer. Your working directory path is d:\project\interior_camera\.agents\teamwork_preview_worker_m1_gen3\.

Please execute the following tasks:
1. Run `./gradlew clean compileDebugAndroidTestSources compileDebugUnitTestKotlin --no-daemon` to verify the baseline build compilation.
2. Review the existing test files:
   - `app/src/androidTest/java/com/example/interiorcamera/ui/main/MainScreenTest.kt`
   - `app/src/test/java/com/example/interiorcamera/data/DefaultDataRepositoryTest.kt`
   - `app/src/test/java/com/example/interiorcamera/ui/main/MainScreenViewModelTest.kt`
   - `app/src/androidTest/java/com/example/interiorcamera/test/ComposeTestHelpers.kt`
3. Design and implement a total of at least 60 test cases across 4 tiers:
   - **Tier 1 - Feature Coverage (>=5 per feature)**: Happy-path tests for each of the 5 features:
     - R1: Opacity Slider
     - R2: Multi-Anchor Placement
     - R3: Plane Detection Coachmark/Guide
     - R4: Rotation Controls / Gestures
     - R5: Custom Preset Storage
   - **Tier 2 - Boundary & Corner Cases (>=5 per feature)**: Edge cases, limit values, empty inputs, negative values, permission denial.
   - **Tier 3 - Cross-Feature Combinations (pairwise)**: Test feature interactions (e.g. rotation buttons + gestures, opacity + multi-anchor).
   - **Tier 4 - Real-World Application Scenarios**: High-level user scenarios naturally combining features (from Section 4 of `TEST_INFRA.md`).
   Note: The E2E tests are requirement-driven and opaque-box. The tests must compile, but they don't have to pass because the implementation track is currently developing these features. Use standard Compose UI testing rule (`createAndroidComposeRule<MainActivity>`) and fakes/mocks where appropriate. Use Semantics/Test Tags/Text to locate UI elements. If a UI element does not exist in the current layout yet, you can still test it by matching on its expected text or tag (e.g., `opacity_slider`, `rotate_left_button`, `rotate_right_button`, `"지우기"`, etc.). These will compile successfully.
4. Verify that all test cases compile cleanly using:
   `./gradlew compileDebugAndroidTestSources compileDebugUnitTestKotlin --no-daemon`
5. Report compilation results, test files created/modified, and handoff details.

MANDATORY INTEGRITY WARNING:
DO NOT CHEAT. All implementations must be genuine. DO NOT hardcode test results, create dummy/facade implementations, or circumvent the intended task. A Forensic Auditor will independently verify your work. Integrity violations WILL be detected and your work WILL be rejected.
