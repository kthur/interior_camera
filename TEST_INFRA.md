# Test Infrastructure (TEST_INFRA.md)

## 1. Test Philosophy
FitCheck AR uses a dual-layered testing strategy combining **Local Unit Tests** (fast, deterministic verification of business logic, ViewModels, and data layers) and **Instrumented UI/E2E Tests** (verification of user interface, Jetpack Compose layouts, AR screen interactions, and device-level integrations such as permissions and ARCore availability checks).

*   **Unit Testing (Logic Tier)**: Located under `app/src/test`. Focuses on `DataRepository` behavior, `MainScreenViewModel` state transitions, and business logic. Dependencies (like `SharedPreferences` or android contexts) are replaced with fakes or mocks (e.g. `FakeDataRepository` or Mockito mocks) to maintain speed and determinism.
*   **Instrumented UI Testing (Presentation & Integration Tier)**: Located under `app/src/androidTest`. Uses Jetpack Compose's testing framework (`ComposeContentTestRule` / `createAndroidComposeRule`) to perform clicks, text inputs, and verify UI layout. For AR elements where physical camera input and 3D rendering cannot be easily verified on standard emulators, we verify:
    1.  UI Overlays (e.g. sliders, buttons, informational cards, guide coachmarks).
    2.  The configuration and existence of Compose wrappers around `ARSceneView` / `ModelNode`.
    3.  Proper state transitions and callbacks when gesture events or button clicks are triggered.
*   **Hermeticity**: Tests are self-contained and run without external network or physical environment dependencies. Fakes/mocks are utilized for data/storage operations and hardware components.
*   **Decoupled State Design**: By segregating the AR rendering engine (`ARSceneView`) from its interactive overlay layout (containing sliders, buttons, and coachmarks), we can test the UI controls in isolation without crashing due to native library dependencies (Filament/OpenGL) in non-AR supported environments.

---

## 2. Feature Inventory (R1 - R5)
The application features map to specific test targets and validation strategies:

| ID | Feature Description | Testing Target | Test Type | Verification Strategy |
|---|---|---|---|---|
| **R1** | **Opacity Slider Control** | `ArScreen` UI Overlay & Renderer | Instrumented UI & Unit Test | Verify that sliding the opacity slider changes the selected model's alpha/opacity state, updates the progress semantics, and sets the `ModelNode` alpha correctly. |
| **R2** | **3D Rotation Controls** | `ArScreen` UI Overlay & Gestures | Instrumented UI & Unit Test | Verify that "Rotate Left" / "Rotate Right" buttons increment/decrement the model's Y-axis rotation by fixed steps (wrapping at 360°/0°), and verify multi-touch rotation gestures update rotation state. |
| **R3** | **Plane Detection Guide UI** | `ArScreen` UI Overlay | Instrumented UI Test | Verify that the coachmark "바닥(평면)을 비추고 점들이 나타나면 터치하여 배치하세요." displays when no plane is tracked, hides when plane tracking is stable, and reappears when tracking is lost. |
| **R4** | **Multi-Anchor Placement** | `ArScreen` Scene & Overlay | Instrumented UI & Unit Test | Verify that tapping multiple plane locations places multiple anchors simultaneously, that tapped nodes are selected/highlighted, and that individual deletion ("지우기") and "지우기" (Clear All) remove nodes correctly. |
| **R5** | **Custom Preset Storage** | `MainScreen`, `DataRepository` & DB | Unit Test & UI Test | Verify that entering custom dimensions (Width, Height, Depth, Name) and clicking "Save" persists presets locally (via SharedPreferences JSON serialization), displays them in the presets list, and validates input bounds. |

---

## 3. Test Architecture

### 3.1 Directory Layout
The testing code is segregated into standard Android test directories matching the production codebase structure:

```
app/src/
├── main/                   # Production code
├── test/                   # Local Unit Tests (JVM)
│   └── java/com/example/interiorcamera/
│       ├── data/
│       │   └── DefaultDataRepositoryTest.kt    # Unit tests for JSON/SharedPreferences repository
│       └── ui/main/
│           └── MainScreenViewModelTest.kt      # ViewModel state transition tests (using FakeMyModelRepository)
└── androidTest/            # Instrumented UI/E2E Tests (Android device/emulator)
    └── java/com/example/interiorcamera/
        ├── test/
        │   └── ComposeTestHelpers.kt           # Custom matchers and wait helpers (e.g. assertSliderValue)
        └── ui/main/
            └── MainScreenTest.kt               # MainScreen UI & save preset callback tests
```

### 3.2 Running the Tests
To execute all local unit tests (under `app/src/test`):
```bash
./gradlew testDebugUnitTest
```

To compile instrumented Android tests (under `app/src/androidTest`):
```bash
./gradlew compileDebugAndroidTestSources
```

To run all instrumented tests (requires a running emulator or connected device):
```bash
./gradlew connectedAndroidTest
```

To run the baseline compilation verification task:
```bash
./gradlew clean compileDebugAndroidTestSources compileDebugUnitTestKotlin --no-daemon
```

### 3.3 Test Formatting & Naming Conventions
*   **Test Class Naming**: Target class name followed by `Test` (e.g. `MainScreenViewModelTest` for `MainScreenViewModel`).
*   **Test Function Naming**: Use descriptive camelCase or snake_case indicating: `[subject]_[action]_[expectedResult]` (e.g., `uiState_onItemSaved_isDisplayed` or `defaultPresets_areDisplayed`).
*   **Structure**: Follow the **Given-When-Then** AAA (Arrange-Act-Assert) pattern:
    ```kotlin
    @Test
    fun testName() {
        // 1. Arrange (Given)
        // 2. Act (When)
        // 3. Assert (Then)
    }
    ```

---

## 4. Comprehensive 4-Tier Test Plan (60 Cases)

### 4.1 Tier 1: Feature Coverage — Happy Path (25 Cases)
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

### 4.2 Tier 2: Boundary, Corner, & Negative Cases (25 Cases)
*Tests application stability and correctness under extreme inputs, empty lists, limits, and system interruptions.*

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
| **T2.11** | R3 | `ArScreen` / Lifecycle | Background and foreground the app during active scanning. | Scanning resumes; coachmark updates based on current tracking state. |
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

### 4.3 Tier 3: Cross-Feature Interaction Cases (5 Cases)
*Verifies correct behavior when multiple distinct features interact or compete for resources.*

*   **T3.1: Multi-Anchor Selection + Opacity Control (R4 + R1)**
    *   *Scenario*: User places Model A and Model B. Select Model A. Move the opacity slider to 30%. Select Model B. Verify Model B’s opacity remains at 100% and the opacity slider UI updates to reflect Model B's opacity (100%). Change Model B's opacity to 70%. Switch back to Model A and verify the opacity slider returns to 30% and Model A remains at 30% alpha.
    *   *Expected Result*: Opacity settings are tracked per-node. Changing the slider only impacts the currently active model node.
*   **T3.2: Multi-Anchor Selection + Rotation Controls (R4 + R2)**
    *   *Scenario*: User places Model A and Model B. Select Model A. Click "Rotate Right" twice (rotating Model A by 30°). Tap Model B to select it. Click "Rotate Left" once.
    *   *Expected Result*: Model A remains rotated by 30°. Model B rotates independently by -15°. The active rotation controls target only the selected node.
*   **T3.3: Custom Preset Input + AR Overlay Display (R5 + R1/R3)**
    *   *Scenario*: User saves a custom preset ("My Wardrobe", 120 x 200 x 60 cm), selects it, and launches the AR view. Guide UI appears while scanning. Once plane is detected, the user taps to place the model.
    *   *Expected Result*: The rendered model has dimensions scaling exactly to 1.2m x 2.0m x 0.6m. The UI overlay displays the correct text: "가로: 120.0cm x 세로: 200.0cm x 깊이: 60.0cm". The opacity slider adjusts the custom model's opacity correctly.
*   **T3.4: Plane Detection Guide + Multi-Anchor Placement (R3 + R4)**
    *   *Scenario*: Launch AR view. Guide UI is active. Scan room to find a plane (Guide UI hides). Place Model A. Tap another area on the plane to place Model B. Delete Model A.
    *   *Expected Result*: The Guide UI remains hidden throughout. Deleting one model when another model and stable plane tracking remain does not trigger the Guide UI to reappear.
*   **T3.5: Multi-Anchor Selection + Rotation Gestures + Opacity (R4 + R2 + R1)**
    *   *Scenario*: Place Model A. Select Model A, set its opacity to 50%, and perform a two-finger rotation gesture to rotate it by 90°. Place Model B and select it. Adjust Model B's rotation using the "Rotate Right" button.
    *   *Expected Result*: Model A retains both its custom opacity (50%) and custom rotation (90°). Model B can be manipulated independently without altering Model A's state.

### 4.4 Tier 4: Real-World User Scenarios (5 Cases)
*High-level, end-to-end workflows representing typical user interactions.*

#### Scenario 1: Entire Custom Furniture Planning Workflow (R5 → R3 → R4 → R1)
1.  **Given**: The user launches the application and is on the `MainScreen`. No custom presets are initially saved in local storage.
2.  **When**: The user enters "My Refrigerator" in the preset name field, "92" in width, "180" in height, "75" in depth, and clicks "Save to My List".
3.  **Then**: The form input fields validate the bounds, the preset is saved in `DefaultDataRepository` via SharedPreferences JSON serialization, and a new preset button named "My Refrigerator" appears under the "나의 리스트" header.
4.  **When**: The user clicks the "My Refrigerator" preset button and then clicks "AR 카메라로 확인하기".
5.  **Then**: Camera permission is requested. Upon granting, the app navigates to `ArScreen`, showing the plane detection coachmark guide ("바닥(평면)을 비추고 점들이 나타나면 터치하여 배치하세요.") because plane tracking is not yet stable.
6.  **When**: The user slowly pans the camera until a horizontal surface is detected and plane tracking is stable.
7.  **Then**: The plane detection guide is automatically dismissed and hidden.
8.  **When**: The user taps on the detected plane.
9.  **Then**: A 3D model node of the refrigerator (dimensions 0.92m x 1.80m x 0.75m) is placed in the scene, highlighted, and the opacity slider becomes enabled.
10. **When**: The user adjusts the opacity slider to 50% to check for physical cabinet overlap.
11. **Then**: The 3D model node's rendering alpha reduces to `0.5f`.

#### Scenario 2: Arranging Multiple Appliances in a Kitchen Layout (R4 + R2 + R1 + R5)
1.  **Given**: The user has saved custom presets for both a dishwasher (60x85x60cm) and a refrigerator (90x180x80cm).
2.  **When**: The user selects the dishwasher preset on the `MainScreen`, navigates to `ArScreen`, scans the kitchen floor, and taps to place it.
3.  **Then**: The dishwasher model is placed in the scene.
4.  **When**: The user taps "Rotate Left" or "Rotate Right" multiple times to align the dishwasher flush with the kitchen counter.
5.  **Then**: The dishwasher model rotates in 15-degree steps around the Y-axis.
6.  **When**: The user goes back, selects the refrigerator preset, launches AR again, scans, and places it next to the dishwasher.
7.  **Then**: Both the dishwasher and refrigerator models render simultaneously in the scene.
8.  **When**: The user taps the refrigerator node to select it, sets its opacity to 70%, and rotates it.
9.  **Then**: Only the refrigerator model's opacity and rotation states change, while the dishwasher remains unmodified.
10. **When**: The user selects the dishwasher node and taps the "지우기" (Delete) button.
11. **Then**: Only the dishwasher node is removed from the scene; the refrigerator node remains untouched.

#### Scenario 3: Visualizing Large Wardrobe fit in a tight Bedroom Corner (R1 + R2 + R3)
1.  **Given**: The user selects the default "옷장 (자작)" preset (100x210x60cm) and launches `ArScreen`.
2.  **When**: The user scans the bedroom floor; once a plane is tracked, they tap to place it.
3.  **Then**: The wardrobe model renders in the bedroom corner.
4.  **When**: The user taps "Rotate Right" repeatedly to rotate the model to face forward against the bedroom wall.
5.  **Then**: The model rotates correctly, and the rotation angle clamps/wraps modulo 360° cleanly.
6.  **When**: The user drags the opacity slider down to check if the wardrobe overlaps with wall sockets.
7.  **Then**: The wardrobe rendering transparency increases, letting the user verify outlet positions.

#### Scenario 4: Presets Persistence and Management Lifecycle (R5)
1.  **Given**: The user is on the `MainScreen` presets list.
2.  **When**: The user saves 5 custom presets of different sizes.
3.  **Then**: All 5 presets appear under the "나의 리스트" grid.
4.  **When**: The user terminates the application process and relaunches the app.
5.  **Then**: All 5 custom presets are loaded from SharedPreferences and are displayed on the main screen.
6.  **When**: The user selects the third preset, edits its width, and clicks "Save" again.
7.  **Then**: The preset is updated, and the new dimensions are immediately displayed.

#### Scenario 5: AR Tracking Interruption and Recovery (R3 + R4)
1.  **Given**: The user is on the `ArScreen` and has placed two furniture models in the living room.
2.  **When**: The user covers the camera lens, entering a tracking-lost state.
3.  **Then**: The plane detection coachmark reappears ("바닥(평면)을 비추고...").
4.  **When**: The user uncovers the lens and pans the camera over the floor.
5.  **Then**: Tracking state recovers, the guide is hidden, the two previously placed models restore their spatial coordinates, and the user can tap to place a third model.
