# FitCheck AR — 4-Tier Test Plan & Feasibility Analysis

## 1. Executive Summary
This document defines a comprehensive 4-tier test plan consisting of 60 test cases designed to verify the 5 key requirements (R1-R5) of the FitCheck AR application. It also provides a technical feasibility analysis for simulating UI and spatial interactions (slider movements, button clicks, gestures, text input, database state) within both JVM local unit tests and Instrumented testing frameworks.

---

## 2. Requirements Mapping (R1 - R5)
The improvements span two main screens: **MainScreen** (preset selection and configuration) and **ArScreen** (Sceneview AR rendering and interactive overlays).
- **R1: Opacity Slider Control**: Dynamic alpha adjustment of placed 3D models using a slider in the AR screen overlay.
- **R2: 3D Rotation Controls**: Two-finger rotation gesture on 3D models and "Rotate Left"/"Rotate Right" overlay buttons for precise rotation increments (15 or 45 degrees).
- **R3: Plane Detection Guide UI**: Coachmark/message instructing the user to "slowly move the camera" until a surface is detected. Hidden when plane tracking is stable.
- **R4: Multi-Anchor Placement**: Multiple models rendered simultaneously. Support selecting a node, scaling/rotating/deleting the selected node, and clearing all nodes.
- **R5: Custom Preset Storage**: Local storage (database or preferences) for custom dimensions (width, height, depth, name). Custom presets appear on the Main Screen alongside default ones.

---

## 3. Comprehensive 4-Tier Test Plan (60 Cases)

### Tier 1: Feature Coverage — Happy Path (25 Cases)
*Ensures each of the five requirements works correctly under normal, expected usage (5 cases per requirement).*

| Test ID | Req | Component / Screen | Test Description | Expected Result |
|---|---|---|---|---|
| **T1.1** | R1 | `ArScreen` Overlay | View opacity slider in AR screen overlay after placing a model node. | Slider is visible and enabled. |
| **T1.2** | R1 | `ArScreen` / Render | Slide opacity to 50% scale. | Model node rendering alpha reduces to 0.5f (semi-transparent). |
| **T1.3** | R1 | `ArScreen` / Render | Slide opacity to 100% scale. | Model node rendering alpha returns to 1.0f (fully opaque). |
| **T1.4** | R1 | `ArScreen` / Render | Slide opacity to 10% scale. | Model node rendering alpha reduces to 0.1f (barely visible). |
| **T1.5** | R1 | `ArScreen` Overlay | Switch selection between Model A and Model B in multi-anchor mode. | Opacity slider state updates to match the selected model's alpha. |
| **T1.6** | R2 | `ArScreen` Overlay | Tap the "Rotate Left" button with a model selected. | Selected model node rotates counter-clockwise by 15° (or 45°). |
| **T1.7** | R2 | `ArScreen` Overlay | Tap the "Rotate Right" button with a model selected. | Selected model node rotates clockwise by 15° (or 45°). |
| **T1.8** | R2 | `ArScreen` / Gesture | Perform clockwise two-finger rotation gesture on a selected model. | Model node rotates dynamically clockwise matching gesture angle. |
| **T1.9** | R2 | `ArScreen` / Gesture | Perform counter-clockwise two-finger rotation gesture on a selected model. | Model node rotates dynamically counter-clockwise matching gesture angle. |
| **T1.10** | R2 | `ArScreen` Overlay | Deselect the currently active model node. | "Rotate Left" and "Rotate Right" buttons disappear or disable. |
| **T1.11** | R3 | `ArScreen` / Guide | Open AR camera view before any plane is detected. | Plane Detection Guide ("slowly move the camera") coachmark displays. |
| **T1.12** | R3 | `ArScreen` / Guide | Scan room without finding any surface. | Coachmark remains visible indefinitely. |
| **T1.13** | R3 | `ArScreen` / Guide | Detect a horizontal plane (plane tracking state becomes stable). | Coachmark is automatically dismissed / hidden. |
| **T1.14** | R3 | `ArScreen` / Guide | Lose plane tracking (e.g. camera covered or bad lighting). | Coachmark reappears to guide the user. |
| **T1.15** | R3 | `ArScreen` / Render | Verify model visibility after coachmark is dismissed. | Placed models remain visible and interactable. |
| **T1.16** | R4 | `ArScreen` / Scene | Tap a detected plane to place the first model anchor. | First model is rendered at hit position and selected. |
| **T1.17** | R4 | `ArScreen` / Scene | Tap a different location on the detected plane. | Second model is placed; first model remains in the scene. |
| **T1.18** | R4 | `ArScreen` / Scene | Tap on an unselected placed model node. | Node becomes highlighted/selected; prior selection is cleared. |
| **T1.19** | R4 | `ArScreen` Overlay | Tap "Delete" button when a specific node is selected. | The selected node is removed; other nodes remain untouched. |
| **T1.20** | R4 | `ArScreen` Overlay | Tap "Clear All" button with multiple nodes in scene. | All nodes are removed, leaving an empty AR scene. |
| **T1.21** | R5 | `MainScreen` / Form | Enter valid custom preset details (name: "New Fridge", 90x180x70) and click "Save". | Preset is saved to storage and input fields are validated. |
| **T1.22** | R5 | `MainScreen` / List | View the Main Screen presets list. | Saved custom preset appears under the "Custom Favorites" section. |
| **T1.23** | R5 | `MainScreen` / Form | Tap the newly saved custom preset chip. | Input fields are automatically auto-populated with the saved dimensions. |
| **T1.24** | R5 | `MainScreen` / Nav | Select a custom preset and tap "AR 카메라로 확인하기". | App navigates to AR Screen passing correct custom dimensions. |
| **T1.25** | R5 | `MainScreen` / Repo | Restart the application and view the presets list. | Saved custom presets persist and load successfully. |

---

### Tier 2: Boundary, Corner, & Negative Cases (25 Cases)
*Tests application stability and correctness under extreme inputs, empty lists, limits, and system interruptions (5 cases per requirement).*

| Test ID | Req | Component / Screen | Test Description | Expected Result |
|---|---|---|---|---|
| **T2.1** | R1 | `ArScreen` / Render | Slide opacity to exactly 0.0f (minimum bound). | Model node becomes fully transparent (invisible) but remains in scene. |
| **T2.2** | R1 | `ArScreen` / Render | Slide opacity to exactly 1.0f (maximum bound). | Model node is rendered fully opaque with no rendering artifacts. |
| **T2.3** | R1 | `ArScreen` / Render | Rapidly toggle opacity slider between 0.0 and 1.0. | Renderer handles rapid alpha changes without memory leak or crash. |
| **T2.4** | R1 | `ArScreen` Overlay | Open AR Screen before any model is placed. | Opacity slider is hidden or disabled to prevent premature inputs. |
| **T2.5** | R1 | `ArScreen` / Render | Tap "Clear All" and place a new model. | Slider resets to default opacity (1.0f) for the newly placed model. |
| **T2.6** | R2 | `ArScreen` / Render | Tap "Rotate Right" repeatedly to rotate beyond 360 degrees. | Rotation y-angle wraps around modulo 360° correctly. |
| **T2.7** | R2 | `ArScreen` / Gesture | Perform two-finger rotation gesture with extremely small delta (micro-rotation). | Rotation is ignored below a minimum angle threshold (no jittering). |
| **T2.8** | R2 | `ArScreen` / Gesture | Perform two-finger rotation with extremely fast swipe velocity. | Rotation completes smoothly, clamping or decelerating correctly. |
| **T2.9** | R2 | `ArScreen` Overlay | Attempt to rotate when multiple models are placed but *none* is selected. | Buttons are disabled and touch gestures on empty space do not rotate. |
| **T2.10** | R2 | `ArScreen` / Render | Rotate a model node that is partially clipped by the screen/camera boundary. | Model rotates correctly in 3D space without rendering crashes. |
| **T2.11** | R3 | `ArScreen` / Lifecyle | Background and foreground the app during active scanning. | Scanning resumes; coachmark updates based on current tracking state. |
| **T2.12** | R3 | `ArScreen` / Guide | Scan in a dark room (unsupported tracking mode). | ARCore reports tracking failure; warning displays. |
| **T2.13** | R3 | `ArScreen` Overlay | Tap the "Back" button while the scanning coachmark is visible. | App navigates back to MainScreen immediately (coachmark doesn't block). |
| **T2.14** | R3 | `ArScreen` / Guide | Plane is detected, guide is hidden, user deletes all models. | Guide remains hidden (plane tracking is still active/stable). |
| **T2.15** | R3 | `ArScreen` / Guide | Rapid plane detection (under 100ms on startup). | Coachmark animates out smoothly without flickering UI states. |
| **T2.16** | R4 | `ArScreen` / Scene | Place more than 10 models in the AR scene (stress test). | System maintains performance; warning or caps are handled if defined. |
| **T2.17** | R4 | `ArScreen` / Scene | Place two models at the exact same location (overlapping anchors). | Both models render; tap prioritizes closest or most recently placed. |
| **T2.18** | R4 | `ArScreen` / Scene | Tap on empty background when a model is selected. | Selected model is deselected; highlight is removed. |
| **T2.19** | R4 | `ArScreen` / Scene | Tap to place a new model node immediately after clicking "Clear All". | Node places successfully; no lingering references to cleared anchors. |
| **T2.20** | R4 | `ArScreen` Overlay | Click "Delete" button when no node is selected. | Click is ignored; button is disabled or hidden. |
| **T2.21** | R5 | `MainScreen` / Form | Click "Save" on preset form with an empty name field. | Save is disabled or shows validation error: "Name cannot be empty". |
| **T2.22** | R5 | `MainScreen` / Form | Save a preset with zero or negative dimensions (e.g. Width: -5.0). | Validation error: "Dimensions must be greater than zero". |
| **T2.23** | R5 | `MainScreen` / Form | Save a preset with extremely large dimensions (e.g. 9999 cm). | Input is handled; verify layout does not break with large numbers. |
| **T2.24** | R5 | `MainScreen` / Form | Save a custom preset with a name that already exists. | App displays prompt to overwrite or appends a suffix (e.g., "(1)"). |
| **T2.25** | R5 | `MainScreen` / Repo | Force a database/write error (simulate full disk / failed transaction). | App displays friendly error message instead of crashing. |

---

### Tier 3: Cross-Feature Interaction Cases (5 Cases)
*Verifies correct behavior when multiple distinct features interact or compete for resources.*

1. **T3.1: Multi-Anchor Selection + Opacity Control (R4 + R1)**
   - *Scenario*: User places Model A and Model B. Select Model A. Move the opacity slider to 30%. Select Model B. Verify Model B’s opacity remains at 100% and the opacity slider UI updates to reflect Model B's opacity (100%). Change Model B's opacity to 70%. Switch back to Model A and verify the opacity slider returns to 30% and Model A remains at 30% alpha.
   - *Expected Result*: Opacity settings are tracked per-node. Changing the slider only impacts the currently active model node.
2. **T3.2: Multi-Anchor Selection + Rotation Controls (R4 + R2)**
   - *Scenario*: User places Model A and Model B. Select Model A. Click "Rotate Right" twice (rotating Model A by 30°). Tap Model B to select it. Click "Rotate Left" once.
   - *Expected Result*: Model A remains rotated by 30°. Model B rotates independently by -15°. The active rotation controls target only the selected node.
3. **T3.3: Custom Preset Input + AR Overlay Display (R5 + R1/R3)**
   - *Scenario*: User saves a custom preset ("My Wardrobe", 120 x 200 x 60 cm), selects it, and launches the AR view. Guide UI appears while scanning. Once plane is detected, the user taps to place the model.
   - *Expected Result*: The rendered model has dimensions scaling exactly to 1.2m x 2.0m x 0.6m. The UI overlay displays the correct text: "My Wardrobe: 가로: 120.0cm x 세로: 200.0cm x 깊이: 60.0cm". The opacity slider adjusts the custom model's opacity correctly.
4. **T3.4: Plane Detection Guide + Multi-Anchor Placement (R3 + R4)**
   - *Scenario*: Launch AR view. Guide UI is active. Scan room to find a plane (Guide UI hides). Place Model A. Tap another area on the plane to place Model B. Delete Model A.
   - *Expected Result*: The Guide UI remains hidden throughout. Deleting one model when another model and stable plane tracking remain does not trigger the Guide UI to reappear.
5. **T3.5: Multi-Anchor Selection + Rotation Gestures + Opacity (R4 + R2 + R1)**
   - *Scenario*: Place Model A. Select Model A, set its opacity to 50%, and perform a two-finger rotation gesture to rotate it by 90°. Place Model B and select it. Adjust Model B's rotation using the "Rotate Right" button.
   - *Expected Result*: Model A retains both its custom opacity (50%) and custom rotation (90°). Model B can be manipulated independently without altering Model A's state.

---

### Tier 4: Real-World User Scenarios (5 Cases)
*High-level, end-to-end workflows representing typical user interactions with the app.*

1. **T4.1: Entire Custom Furniture Planning Workflow (R5 → R3 → R4 → R1)**
   - *Workflow*:
     1. User opens FitCheck AR.
     2. Enters custom dimensions for a new refrigerator (92cm x 180cm x 75cm), names it "My Fridge", and taps "Save".
     3. The preset appears under "Custom Favorites". The user taps it and clicks "AR 카메라로 확인하기".
     4. User accepts the camera permission. AR Screen opens showing the scanning coachmark.
     5. User slowly pans the camera. A plane is detected and the coachmark disappears.
     6. User taps the floor to place the refrigerator.
     7. To check if the refrigerator will block light, the user adjusts the opacity slider to 50% to visualize overlap.
   - *Expected Result*: Entire flow completes successfully with correct navigation, storage, scanning transitions, and alpha changes.
2. **T4.2: Fitting Multiple Appliances in a Kitchen Layout (R4 + R2 + R1 + R5)**
   - *Workflow*:
     1. User wants to arrange both a custom dishwasher (60x85x60cm) and a custom refrigerator (90x180x80cm) side-by-side.
     2. They select the dishwasher preset, launch AR, detect a plane, and place it.
     3. They use the "Rotate Left" button to align the dishwasher with the kitchen counter.
     4. They select the refrigerator model, place it next to the dishwasher, and use a two-finger gesture to rotate it.
     5. They adjust the opacity of both models to check cabinet/door clearance behind them.
     6. They select the dishwasher and tap "Delete" to test a different configuration.
   - *Expected Result*: Multiple nodes are rendered and manipulated independently without interfering with each other's parameters.
3. **T4.3: Visualizing Large Wardrobe fit in a tight Bedroom Corner (R1 + R2 + R3)**
   - *Workflow*:
     1. User selects default "옷장 (자작)" preset (100x210x60cm) and launches AR.
     2. Coachmark guides them to scan the bedroom floor.
     3. Once the plane is found, they place the wardrobe.
     4. Because space is tight, the model spawns facing the wrong direction. The user taps "Rotate Right" multiple times (in increments of 45°) to align the wardrobe back against the wall.
     5. User drags the opacity slider down to see if the wardrobe blocks any wall outlets behind it.
   - *Expected Result*: Smooth rotation and opacity adjustments allow precision planning in confined spatial environments.
4. **T4.4: Storage Persistence and Management Lifecycle (R5)**
   - *Workflow*:
     1. User adds 5 different custom presets of varying sizes and names.
     2. Verifies all 5 appear in the Main Screen list.
     3. Closes the app completely (kills the background process).
     4. Relaunches the app and verifies all 5 custom presets are still loaded.
     5. Selects the 3rd preset, modifies the depth from 50cm to 60cm, and saves again.
     6. Verifies the preset list reflects the updated depth.
     7. Deletes a preset and verifies it is removed from the UI and storage.
   - *Expected Result*: Local database or SharedPreferences correctly serializes, deserializes, updates, and deletes presets across lifecycle events.
5. **T4.5: Handling AR Tracking Interruptions and Recovery during placement (R3 + R4)**
   - *Workflow*:
     1. User places two furniture models in the living room.
     2. User accidentally covers the camera lens or enters a dark area, causing the ARCore tracking state to fail.
     3. The Plane Detection Guide coachmark reappears: "slowly move the camera".
     4. The user returns to a well-lit area and pans the camera.
     5. Tracking recovers: the coachmark is hidden, and the two previously placed models reappear in their correct spatial locations.
     6. The user continues by placing a third model.
   - *Expected Result*: App recovers gracefully from tracking loss; previously placed nodes restore their absolute positions once tracking resumes.

---

## 4. Interaction Simulation Feasibility Analysis

Below is an analysis of how to simulate each interaction type within either **Local JVM Unit Tests** (Robolectric / Unit Tests) or **Instrumented UI Tests** (Compose UI Test / Espresso).

### A. Slider Movements (Opacity Slider — R1)
* **Instrumented UI Tests (High Feasibility)**:
  * In Compose, we can locate the slider using `onNodeWithTag` or `onNode(hasProgressBarRangeInfo(...))`.
  * The most reliable way to simulate movements is using Compose UI Semantics:
    ```kotlin
    composeTestRule.onNodeWithTag("OpacitySlider")
        .performSemanticsAction(SemanticsActions.SetProgress) { it(0.5f) }
    ```
  * Alternatively, we can use touch gestures:
    ```kotlin
    composeTestRule.onNodeWithTag("OpacitySlider").performTouchInput { swipeRight() }
    ```
* **Local JVM Tests (Moderate Feasibility)**:
  * Since Sceneview (relying on native Filament/OpenGL libraries) fails to load on a standard JVM headless environment, rendering UI layouts that contain `ARSceneView` will throw `java.lang.UnsatisfiedLinkError`.
  * However, if the opacity state is separated into a `ViewModel`, we can unit test the logic directly:
    ```kotlin
    viewModel.onOpacityChanged(0.5f)
    assertEquals(0.5f, viewModel.opacity.value)
    ```

### B. Button Clicks (Rotate, Clear, Delete, Save — R2, R4, R5)
* **Instrumented UI Tests (High Feasibility)**:
  * Standard Compose click simulations work perfectly:
    ```kotlin
    composeTestRule.onNodeWithText("Rotate Left").performClick()
    ```
* **Local JVM Tests (High Feasibility)**:
  * Click handlers that update a state model or trigger database actions can be tested by invoking the ViewModel functions directly (e.g. `viewModel.rotateSelectedNode(15f)` or `viewModel.savePreset(...)`).

### C. Gestures (Two-Finger Rotation & Tap-to-Place — R2, R4)
* **Instrumented UI Tests (Moderate to Low Feasibility)**:
  * **Two-finger rotation**: Compose's `TouchInjectionScope` provides `multiPointerInput` to simulate multi-touch. However, Sceneview registers gestures via a custom Android `View.OnTouchListener` (`rememberOnGestureListener` wrapping standard Android `MotionEvent` handling). To inject this in a Compose UI Test, we must access the underlying `AndroidView` wrapping the `ARSceneView` and dispatch raw `MotionEvent` sequences containing multiple pointer IDs and coordinate deltas. This is highly complex and prone to timing/sensitivity issues.
  * **Tap-to-Place**: Simulating a single tap on `ARSceneView` is easy via `performClick()`. However, the placement logic relies on `frame.hitTest(x, y)`. In an emulator or test environment without an active camera feed and physical planes, `hitTest` returns no results. To simulate placement, we must mock/stub the `Frame` and `HitResult` instances of ARCore, or mock the Sceneview engine to return mock hit results.
* **Local JVM Tests (Low Feasibility)**:
  * Cannot simulate touch inputs or ARCore plane hits. The math (e.g., quaternion multiplication for rotation) should be isolated in utility classes and unit tested separately.

### D. Form Text Inputs (Custom Preset Storage — R5)
* **Instrumented UI Tests (High Feasibility)**:
  * Locate input fields and replace text:
    ```kotlin
    composeTestRule.onNodeWithText("가로 너비 (Width)").performTextInput("95.0")
    ```
* **Local JVM Tests (High Feasibility)**:
  * Simply set values on the ViewModel or test the validation logic functions directly.

### E. Database / Storage State Changes (Custom Preset Storage — R5)
* **Instrumented UI Tests (High Feasibility)**:
  * Run tests on a physical device or emulator using an in-memory Android Room database or isolated SharedPreferences.
* **Local JVM Tests (High Feasibility)**:
  * Use a fake or mock `DataRepository` implementation (e.g. `FakeDataRepository`) to verify database write/read logic and state flow propagation.

---

## 5. Architectural Recommendations for Testability
To achieve the 60-test case target without getting blocked by native AR Core/Sceneview dependencies:
1. **Decouple UI state from AR Rendering**: Model all AR screen state (selected node ID, node scale, node rotation, opacity value, plane tracking state) in a ViewModel or State Holder.
2. **Unit Test State Logic in JVM**: Write JVM tests targeting the ViewModel to verify that gestures and clicks update the state correctly (covers ~50% of the cases).
3. **Mock Sceneview in UI Tests**: Wrap `ARSceneView` in a testable abstraction or use a Mockito/fake framework to stub `Frame` updates and `HitResult`s, allowing Compose UI tests to run without native crashes.
