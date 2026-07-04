# FitCheck AR UI/UX Testing Analysis (R1, R2, R3)

This document contains a comprehensive testing analysis and design for features **R1 (Opacity Slider Control)**, **R2 (3D Rotation Controls)**, and **R3 (Plane Detection Guide UI)** in the FitCheck AR Android application.

---

## 1. Testing Methodologies for R1, R2, and R3

### R1: Opacity Slider Control
* **Target Behavior**: The overlay includes a slider (`0.0f` to `1.0f`) that controls the alpha/opacity of the selected 3D model node.
* **Testing Opacity Changes**:
  * **Compose State Verification**: The slider value is bound to a state variable (e.g., `modelOpacity`). Changes to this state can be tested by finding the slider via its `testTag` (e.g., `OpacitySlider`) and asserting its `ProgressBarRangeInfo`.
  * **Simulating Slider Dragging**:
    ```kotlin
    composeTestRule.onNodeWithTag("OpacitySlider").performSemanticsAction(SemanticsActions.SetProgress) { progress ->
        // Inject values like 0.5f
    }
    ```
  * **Model Node State Propagation**: Verify that changes in the slider value invoke the model node updates (e.g., `modelNode.alpha = newAlpha` or updating Filament materials). In tests, we can assert that the callback `onOpacityChanged` was called with the expected value or mock the 3D node representation.

### R2: 3D Rotation Controls (Gestures & Buttons)
* **Target Behavior**: Users can rotate the selected 3D model node by clicking "Rotate Left" or "Rotate Right" overlay buttons (rotating by fixed increments such as 15° or 45°) or using a two-finger rotation gesture on the model.
* **Testing Button-Based Rotation**:
  * **Button Interaction**: Find the rotation buttons by their `testTag` (e.g., `RotateLeftButton`, `RotateRightButton`) and trigger `performClick()`.
  * **State Verification**: Verify that the internal state representation of the model's Y-axis rotation (e.g., `rotationY`) changes by precisely the configured step (e.g., -15° or +15°).
* **Testing Gesture-Based Rotation**:
  * **Gesture Interaction**: Multi-touch gestures (like two-finger rotation) are typically harder to test reliably on native OpenGL/AR views in standard instrumented Compose UI tests.
  * **Decoupled Gesture Testing**: The gesture listener (e.g., `rememberOnGestureListener` or custom gesture modifiers) should map raw gestures into a state update (e.g., `onRotate(deltaAngle)`). We can test this by invoking the gesture listener callbacks directly in the test setup or using Compose's `TouchInjectionScope` to perform rotation gestures on a mock view area.

### R3: Plane Detection Guide UI
* **Target Behavior**: A coachmark showing visual guidance (e.g., "천천히 카메라를 움직여 주변 바닥(평면)을 비추어 주세요.") is displayed when no plane is detected. Once a plane is detected and tracking is stable, the coachmark is hidden. If tracking is lost, the coachmark reappears.
* **Testing Plane Detection Guide**:
  * **Initial State**: Verify that when `ArScreen` loads and no plane has been tracked, the guide UI exists and is visible.
  * **Simulating Plane Detection**: Update the underlying ARCore state to simulate plane tracking. In Compose UI tests, we can inject a mock frame with detected planes or update the state variable `isPlaneDetected` to `true`.
  * **Visibility Assertion**:
    * If `isPlaneDetected` is `false`, the node with tag `PlaneDetectionGuide` must be displayed.
    * If `isPlaneDetected` is `true`, the node with tag `PlaneDetectionGuide` must be hidden (`assertDoesNotExist()` or `assertIsNotDisplayed()`).

---

## 2. Interaction of Compose UI Tests with Sceneview AR

### The Problem: Native ARCore & OpenGL Dependencies
`ARSceneView` relies on:
1. Native ARCore engine (requires `com.google.ar.core` packages).
2. Physical camera and GPU/OpenGL rendering.
3. Stable tracking session.

If an instrumented test runs on a standard emulator or a CI environment:
- ARCore session initialization fails (unsupported device/no camera).
- In `ArScreen.kt`, `onSessionFailed` is called, which displays a `Toast` and calls `onBack()`, exiting the screen.
- This prevents standard Compose UI tests from verifying any overlay components in `ArScreen`.

### Solutions for Testability

#### Solution A: Component Separation (Stateless Overlay UI)
The most robust pattern is to separate the 3D scene from the control overlays. We extract the overlay UI into a stateless Composable:
```kotlin
@Composable
fun ArOverlay(
    widthCm: Float,
    heightCm: Float,
    depthCm: Float,
    opacity: Float,
    onOpacityChange: (Float) -> Unit,
    rotationAngle: Float,
    onRotateLeft: () -> Unit,
    onRotateRight: () -> Unit,
    isPlaneDetected: Boolean,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Contains Slider, Buttons, Coachmarks, Info Card
}
```
* **Why it works**: `ArOverlay` has no dependencies on ARCore, Sceneview, or Filament.
* **Testing approach**: We can write 100% reliable UI tests for `ArOverlay` in JVM (using Robolectric + Compose test rule) or on any standard emulator. We test slider movements, button clicks, and plane guide visibility using standard Compose Test Rules.

#### Solution B: Abstracting ARSceneView via wrappers
We can wrap the 3D rendering part in a wrapper Composable:
```kotlin
@Composable
fun ArSceneContainer(
    modifier: Modifier = Modifier,
    isPlaneDetected: Boolean,
    onSessionUpdated: (Frame) -> Unit,
    content: @Composable () -> Unit
)
```
In our instrumentation tests, we can mock/override the container using dependency injection or composition locals to display a mock `Box` rather than a real `ARSceneView`. This allows full integration tests of `ArScreen` without crashing on non-ARCore devices.

---

## 3. Designed Test Cases (Tier 1 & Tier 2)

We present at least 5 Tier 1 (happy path) and 5 Tier 2 (boundary/corner) test cases for R1, R2, R3.

### Tier 1 - Happy Path (Feature Coverage)

| Test Case ID | Feature | Test Case Name | Objective | Setup & Steps | Expected Result |
|---|---|---|---|---|---|
| **TC-R1-01** | R1 | Opacity Slider Value Change | Verify that moving the opacity slider updates the model's opacity state. | Place 3D model -> Locate `OpacitySlider` -> Perform gesture to set value to `0.7f`. | The slider value reflects `0.7f` and the model node's alpha/opacity property changes to `0.7f`. |
| **TC-R2-01** | R2 | Rotate Left Button Increment | Verify that clicking "Rotate Left" decreases the model's rotation angle. | Place 3D model -> Click `RotateLeftButton` once. | The model node's Y-axis rotation angle decreases by exactly the step size (e.g., -15° or -45°). |
| **TC-R2-02** | R2 | Rotate Right Button Increment | Verify that clicking "Rotate Right" increases the model's rotation angle. | Place 3D model -> Click `RotateRightButton` once. | The model node's Y-axis rotation angle increases by exactly the step size (e.g., +15° or +45°). |
| **TC-R3-01** | R3 | Guide Displayed on Launch | Verify that the plane detection guide is visible when the screen is loaded. | Launch `ArScreen` -> Wait for rendering. | The `PlaneDetectionGuide` is displayed with the instructions card. |
| **TC-R3-02** | R3 | Guide Hides on Plane Detected | Verify that the guide disappears when a plane is successfully tracked. | Launch `ArScreen` -> Simulate plane detection (set `isPlaneDetected = true`). | The `PlaneDetectionGuide` is hidden from the UI. |

### Tier 2 - Boundary & Corner Cases

| Test Case ID | Feature | Test Case Name | Objective | Setup & Steps | Expected Result |
|---|---|---|---|---|---|
| **TC-R1-02** | R1 | Opacity Slider Min Boundary | Verify that opacity handles the absolute minimum limit (0.0f). | Place 3D model -> Move `OpacitySlider` to `0.0f`. | The model node alpha becomes `0.0f` (completely transparent/invisible) without errors. |
| **TC-R1-03** | R1 | Opacity Slider Max Boundary | Verify that opacity handles the absolute maximum limit (1.0f). | Place 3D model -> Move `OpacitySlider` to `1.0f`. | The model node alpha becomes `1.0f` (completely opaque) without errors. |
| **TC-R2-03** | R2 | Rotation Underflow Wrapping | Verify that rotating past 0° wraps to 345°/315°. | Place 3D model (rotation = 0°) -> Click `RotateLeftButton` (step 15°). | The model's rotation wraps cleanly to `345.0f` (or equivalent negative representation). |
| **TC-R2-04** | R2 | Rotation Overflow Wrapping | Verify that rotating past 360° wraps to 0°/15°. | Place 3D model (rotation = 345°) -> Click `RotateRightButton` (step 15°). | The model's rotation wraps cleanly to `0.0f` (or `360.0f` standard form). |
| **TC-R3-03** | R3 | Guide Reappears on Tracking Lost | Verify that the guide is shown again if plane tracking is lost. | Launch `ArScreen` -> Detect plane (guide hidden) -> Simulate tracking lost (set `isPlaneDetected = false`). | The `PlaneDetectionGuide` reappears immediately on the screen. |

---

## 4. Recommended Test Run Commands

Depending on the environment, use the following commands to execute tests:

* **Local JVM Tests (Robolectric / Unit Tests)**:
  Runs local unit tests including isolated UI tests (if using Robolectric).
  ```bash
  ./gradlew test
  ```
  Or for the app module specifically:
  ```bash
  ./gradlew :app:testDebugUnitTest
  ```

* **Instrumented UI Tests (Physical Device / Emulator)**:
  Runs Compose instrumented UI tests on an connected device/emulator.
  ```bash
  ./gradlew connectedAndroidTest
  ```
  Or for the app module specifically:
  ```bash
  ./gradlew :app:connectedDebugAndroidTest
  ```
