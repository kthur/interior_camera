# FitCheck AR Technical Analysis & E2E Test Suite Design

## 1. Custom Preset Storage (R5) Analysis

### Current Implementation Assessment
- **`data/DataRepository.kt`**: Exposes a simple `Flow<List<String>>` emitting `listOf("Android")`. This is a stub implementation.
- **`ui/main/MainScreenViewModel.kt`**: Maps the flow of string lists to `MainScreenUiState.Success` and displays it.
- **`ui/main/MainScreen.kt`**: Contains a hardcoded list of predefined `PRESETS` (`PresetItem` objects representing refrigerator, washer, etc.) and allows users to input dimensions directly. However, it does not support creating, saving, or retrieving custom user-defined presets from storage.

### Proposed Architecture for R5
To implement custom preset storage, the following updates are required:
1. **Model Updates**: Ensure `PresetItem` has `isCustom: Boolean = false` or separate custom presets in the database.
2. **Storage Selection**: **Room Database** is recommended over Preferences DataStore because custom presets are structured entities. 
   - Define a `PresetEntity` database entity:
     ```kotlin
     @Entity(tableName = "custom_presets")
     data class PresetEntity(
         @PrimaryKey(autoGenerate = true) val id: Int = 0,
         val name: String,
         val width: Float,
         val height: Float,
         val depth: Float,
         val modelName: String
     )
     ```
   - Define a `PresetDao` for query/insert operations:
     ```kotlin
     @Dao
     interface PresetDao {
         @Query("SELECT * FROM custom_presets")
         fun getAllPresetsFlow(): Flow<List<PresetEntity>>
         
         @Insert(onConflict = OnConflictStrategy.REPLACE)
         suspend fun insertPreset(preset: PresetEntity)
         
         @Delete
         suspend fun deletePreset(preset: PresetEntity)
     }
     ```
3. **Repository Expansion**: Implement `DefaultDataRepository` to fetch from `PresetDao` and map database entities to the domain `PresetItem` model.
4. **MainScreen Integration**:
   - Add a "Save Preset" (프리셋으로 저장) button near the manual input fields. Tapping it opens a dialog/field to specify a custom name and triggers `viewModel.savePreset(name, width, height, depth)`.
   - The presets list on `MainScreen` will display default presets alongside user-saved custom presets (e.g., in a separate "My Presets" section).

### Storage/Database Testing Strategy
- **Unit/Integration Testing**:
  - Test the Database and DAO using an **in-memory database** via `Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()`.
  - Verify that insertion, deletion, and flow emission occur correctly and asynchronously.
- **E2E/UI Testing**:
  - Use `ComposeTestRule` or `AndroidComposeTestRule` to test the user interface flow: inputting values, clicking "Save Preset", verifying that a chip/item immediately appears in the list, and verifying that the database receives the correct record.
  - To test persistence across restarts, write an instrumented test that uses `ActivityScenarioRule` to launch the app, save a custom preset, recreate the activity using `scenario.recreate()`, and verify the preset is still present and selectable.

---

## 2. Multi-Anchor Placement & Model Interaction (R4) Analysis

### Current Implementation Assessment
- **`ui/ar/ArScreen.kt`**:
  - Utilizes `ARSceneView` and `rememberOnGestureListener` from Sceneview AR.
  - Currently maintains a single `anchor` state variable (`var anchor by remember { mutableStateOf<Frame?>(null) }`).
  - Only one `AnchorNode` is rendered under the `ARSceneView` block. Tapping a new plane location replaces the old anchor:
    ```kotlin
    onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
      if (node == null) {
        val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
        val newAnchor = hitResults?.firstOrNull()?.createAnchor()
        if (newAnchor != null) {
          anchor = newAnchor // Replaces the single anchor
        }
      }
      true
    }
    ```
  - The "지우기" (Delete) button simply clears the single anchor state (`anchor = null`).
  - There is no mechanism to select a node, scale it, or rotate it.

### Proposed Architecture for R4
1. **Multi-Anchor Placement**:
   Change the single `anchor` state to a list of active placed node data objects:
   ```kotlin
   data class PlacedNode(
       val id: String,
       val anchor: Anchor,
       val widthCm: Float,
       val heightCm: Float,
       val depthCm: Float,
       val modelName: String,
       var rotationY: Float = 0f,
       var scaleMultiplier: Float = 1f
   )
   
   var placedNodes by remember { mutableStateOf<List<PlacedNode>>(emptyList()) }
   ```
   When tapping a plane (where `node == null`), append the new anchor to `placedNodes`.
2. **Node Selection**:
   Introduce a state variable for the selected node:
   ```kotlin
   var selectedNodeId by remember { mutableStateOf<String?>(null) }
   ```
   When a user taps an existing 3D model node in the scene (`node != null` inside `onSingleTapConfirmed`), set `selectedNodeId` to the matching node's ID. 
   When selected:
   - Render a visual highlight (e.g., bounding box or light halo).
   - Display a card containing the selected node's exact dimensions.
   - Show interaction UI controls (rotation slider, scale slider, and a "지우기" button dedicated to the selected node).
3. **Node Deletion**:
   When the delete button is tapped, filter out the selected node from `placedNodes`, call `anchor.detach()` and destroy its corresponding nodes.
4. **Node Scaling/Rotation**:
   - **Rotation**: Add a slider in the bottom overlay card. Sliding it adjusts the `rotationY` of the currently selected node. Update the model node's rotation matrix dynamically (e.g. `node.rotation = Rotation(0f, angle, 0f)`). Alternatively, support two-finger twist gestures on the screen.
   - **Scaling**: Add a scale slider (e.g., 0.5x to 2.0x) or support pinch-to-zoom gestures. Scaling updates the scale property of the `ModelNode` relative to its original dimensions: `Scale(w * scaleMultiplier, h * scaleMultiplier, d * scaleMultiplier)`.

### AR E2E Testing Strategy and Challenges
- **Core Challenges**:
  - ARCore requires camera initialization, physical plane detection (which relies on visual textures and movement), and sensor data.
  - Standard Android emulators without virtual scene support or physical devices on a CI/CD runner will fail to detect planes, preventing mock taps from creating anchors.
- **Resolution Strategy**:
  1. **Decoupled Business Logic (State Model) testing**: Extrapolate the list of placed nodes, selected node, scale, and rotation states into a state holder or ViewModel. Test the transition rules (e.g., adding a node, selecting, deleting, changing rotation values) using standard JVM unit tests or UI-less instrumented tests.
  2. **Simulating Plane Taps**: Mock the gesture listener callback (`onSingleTapConfirmed`) by supplying faked `MotionEvent` and `Node` parameters, or mock the Sceneview/ARCore interface so that hit test results return a mock `Anchor` without requiring camera frames.
  3. **Compose Controls Testing**: Test the UI controls in the overlay (sliders, delete buttons, detail text cards) by setting up a test screen where the AR view is stubbed/mocked, allowing the tests to verify Compose interaction flows (e.g., drag rotation slider -> verify State updates -> verify correct node rotation property updated).

---

## 3. Detailed E2E Test Cases for R4 & R5

Here are 5 Tier 1 (Happy Path) and 5 Tier 2 (Edge/Adversarial/Error) test cases designed for Multi-Anchor Placement and Custom Preset Storage.

### Tier 1 (Primary journeys and happy paths)

#### Test Case 1: [R5] Create and Display Custom Preset
- **Objective**: Verify the user can input custom dimensions, name the preset, save it, and see it display in the preset grid.
- **Prerequisites**: App is launched and starts on the Main Screen.
- **Steps**:
  1. Enter "Custom Bookshelf" in the Preset Name text field.
  2. Enter "80.0" in the Width field, "180.0" in the Height field, and "35.0" in the Depth field.
  3. Tap the "Save Preset" button.
  4. Verify that a chip or item labeled "Custom Bookshelf" appears in the presets section.
  5. Select another default preset (e.g., "양문형 냉장고"), then select the newly created "Custom Bookshelf" chip.
- **Expected Outcome**: The text fields are updated to exactly 80.0, 180.0, and 35.0 when "Custom Bookshelf" is selected.

#### Test Case 2: [R5] Custom Preset Storage Persistence
- **Objective**: Verify that custom presets are persisted in local storage and are loaded upon app relaunch.
- **Prerequisites**: Database/Preferences storage is empty.
- **Steps**:
  1. Launch the app and create a custom preset "Home Desk" (120 x 75 x 60).
  2. Verify it is saved and shown in the presets list.
  3. Close the app completely and terminate its process.
  4. Relaunch the application.
  5. Inspect the presets list on the Main Screen.
- **Expected Outcome**: The "Home Desk" chip remains visible in the presets list and is fully selectable with its original dimensions.

#### Test Case 3: [R4] Multi-Anchor Placement
- **Objective**: Verify that multiple models can be placed simultaneously in the AR view.
- **Prerequisites**: A custom preset or item dimensions are configured; user is navigated to `ArScreen` with camera permissions granted.
- **Steps**:
  1. Simulate plane detection (waiting for Coachmark to dismiss or state to indicate tracking).
  2. Tap on location A on the detected plane. Verify a `ModelNode` is instantiated.
  3. Tap on location B on the detected plane. Verify another `ModelNode` is instantiated.
- **Expected Outcome**: The active list of anchors/nodes contains exactly 2 elements, and both model nodes are actively rendered in the scene.

#### Test Case 4: [R4] Node Selection and Interaction Overlay
- **Objective**: Verify that tapping a placed model selects it and displays rotation/scale controls.
- **Prerequisites**: Two models (Node A and Node B) are placed in the AR space. No node is initially selected.
- **Steps**:
  1. Tap directly on the model Node A in the scene.
  2. Verify that Node A exhibits a selected state (highlighted) and the UI overlay containing the rotation slider, scale slider, and delete button is displayed.
  3. Tap directly on the model Node B in the scene.
- **Expected Outcome**: Node A's selection highlight disappears. Node B becomes highlighted, and the UI sliders update to reflect Node B's current scale and rotation settings.

#### Test Case 5: [R4] Selected Node Deletion
- **Objective**: Verify that deleting a selected node deletes only that node and preserves other placed nodes.
- **Prerequisites**: Two models (Node A and Node B) are placed. Node A is currently selected.
- **Steps**:
  1. Verify the "지우기" (Delete) button is visible in the UI.
  2. Tap the "지우기" button.
  3. Inspect the scene nodes and the placed nodes list.
- **Expected Outcome**: Node A is removed from the scene and detached. Node B remains placed in the scene. The selection state is set to null, and the transformation sliders disappear since no node is selected.

---

### Tier 2 (Boundary, edge cases, error handling, state survival)

#### Test Case 6: [R5] Validation of Invalid Preset Inputs
- **Objective**: Verify that invalid preset names (empty/whitespaces) and out-of-bound dimensions (zero, negative, or excessive sizes) are rejected and prevent saving.
- **Prerequisites**: App is on the Main Screen.
- **Steps**:
  1. Attempt to save a preset with an empty name. Verify "Save Preset" is disabled or shows an error.
  2. Input "Sofa" as the name, but enter "-50" for Width, "0" for Height, and "999999" (unrealistic size) for Depth.
  3. Attempt to click "Save Preset" or "AR 카메라로 확인하기".
- **Expected Outcome**: Both buttons are disabled, or input validation flags the fields as red/invalid. No preset is saved, and navigation to AR view is blocked.

#### Test Case 7: [R5] Local Storage Read/Write Failure Fallback
- **Objective**: Verify that the application does not crash and remains functional if the local database is corrupted or throws read/write errors.
- **Prerequisites**: Repository is mocked to throw `IOException` when reading database presets.
- **Steps**:
  1. Launch the application.
  2. Observe the main screen state.
  3. Try to save a new preset.
- **Expected Outcome**: The Main Screen handles the error state gracefully without crashing. Predefined default presets (hardcoded in code) are displayed as a fallback, ensuring the core app remains usable. An error toast or message is shown when attempting to save.

#### Test Case 8: [R4] AR Session Interruption (App Backgrounding)
- **Objective**: Verify that placed anchors and active selection states survive or recover gracefully when the app goes to the background and resumes.
- **Prerequisites**: Multiple nodes are placed in the AR scene, and Node B is currently selected.
- **Steps**:
  1. Trigger app backgrounding (e.g. simulate Home button press, incoming call).
  2. Wait 5 seconds.
  3. Resume the app back to the foreground.
- **Expected Outcome**: The AR scene resumes rendering, recovers plane tracking, restores the anchors of both Node A and Node B, and retains Node B as the selected node with the UI controls visible.

#### Test Case 9: [R4] Clamping Scale and Rotation Limits
- **Objective**: Verify that rotation and scaling operations clamp to realistic boundaries (e.g. rotation within 0-360 degrees, scale between 0.2x and 3.0x).
- **Prerequisites**: A node is placed and selected.
- **Steps**:
  1. Use the scale slider or double-tap pinch gesture to decrease the scale towards zero. Verify it clamps at the minimum scale (e.g., 0.2x).
  2. Drag the scale slider to its maximum. Verify it clamps at the maximum scale (e.g., 3.0x).
  3. Drag the rotation slider past 360 degrees or below 0.
- **Expected Outcome**: The model's scale is never allowed to reach 0.0x or negative values. The rotation wraps around properly (normalized within [0, 360) range) and doesn't crash the scene view model matrix calculation.

#### Test Case 10: [R4/R5] End-to-End Flow: Save Preset -> Select -> Place -> Transform
- **Objective**: Verify the complete end-to-end user path from creating a custom preset to placing, rotating, and scaling it in the AR environment.
- **Prerequisites**: App is on Main Screen.
- **Steps**:
  1. Input a custom preset: "Slim Cabinet" (50.0 x 200.0 x 30.0) and save it.
  2. Select "Slim Cabinet" from the custom presets list.
  3. Click "AR 카메라로 확인하기".
  4. Perform plane hit tap in `ArScreen` to place the model.
  5. Select the model.
  6. Adjust rotation slider to 90 degrees and scale slider to 1.5x.
  7. Verify the final dimensions/properties.
- **Expected Outcome**: The placed model's physical scale matches the custom preset (0.5m x 2.0m x 0.3m) scaled by the 1.5x multiplier, resulting in physical bounding dimensions of 0.75m x 3.0m x 0.45m at 90 degrees rotation.

---

## 4. Test Suite Layout Recommendation

To organize unit, integration, and E2E UI tests, the following directory structure is recommended.

```
app/
├── src/
│   ├── androidTest/java/com/example/interiorcamera/
│   │   ├── data/
│   │   │   └── DataRepositoryTest.kt          # Integration tests for Room DB / Preferences persistence
│   │   ├── ui/
│   │   │   ├── main/
│   │   │   │   ├── MainScreenTest.kt          # Existing UI tests for MainScreen (expanded)
│   │   │   │   └── CustomPresetE2ETest.kt     # R5 E2E tests: saving, list display, input validation
│   │   │   └── ar/
│   │   │       ├── ArScreenInteractionTest.kt # R4 UI test: overlays, selection states, sliders, and delete button
│   │   │       └── ArFlowE2ETest.kt           # Complete E2E flow from Preset Selection to AR placement & adjustment
│   │   └── fakes/
│   │       └── FakeDataRepository.kt          # Fake implementation of DataRepository for UI testing
│   └── test/java/com/example/interiorcamera/
│       ├── ui/
│       │   ├── main/
│       │   │   └── MainScreenViewModelTest.kt # Unit tests for MainScreenViewModel state machine
│       │   └── ar/
│       │       └── ArScreenViewModelTest.kt   # Unit tests for multi-anchor management business logic
│       └── data/
│           └── ModelMappingTest.kt            # Unit tests for converting entities to domain PresetItems
```

### Proposed Build Config & Dependency Additions

To support storage testing and robust UI assertions, the following should be added to the project configuration:

1. **`gradle/libs.versions.toml`**:
   ```toml
   [versions]
   room = "2.6.1"
   mockk = "1.13.8"
   
   [libraries]
   androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
   androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
   androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
   androidx-room-testing = { module = "androidx.room:room-testing", version.ref = "room" }
   mockk-android = { module = "io.mockk:mockk-android", version.ref = "mockk" }
   mockk-agent = { module = "io.mockk:mockk", version.ref = "mockk" }
   ```

2. **`app/build.gradle.kts`**:
   ```kotlin
   dependencies {
       // Room dependencies (to implement R5)
       implementation(libs.androidx.room.runtime)
       implementation(libs.androidx.room.ktx)
       ksp(libs.androidx.room.compiler) // Or kapt depending on configuration
       
       // Room test dependencies (to verify DB interactions in androidTest)
       androidTestImplementation(libs.androidx.room.testing)
       
       // Mocking frameworks for isolating components in testing
       testImplementation(libs.mockk.agent)
       androidTestImplementation(libs.mockk.android)
   }
   ```
