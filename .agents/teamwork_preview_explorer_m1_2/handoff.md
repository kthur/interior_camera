# Handoff Report: E2E Test Case Design for R4 & R5

## 1. Observation
We analyzed the following files in the codebase:
- **`app/src/main/java/com/example/interiorcamera/data/DataRepository.kt`**:
  - Contains a stub implementation of data flow:
    ```kotlin
    class DefaultDataRepository : DataRepository {
      override val data: Flow<List<String>> = flow { emit(listOf("Android")) }
    }
    ```
- **`app/src/main/java/com/example/interiorcamera/ui/main/MainScreenViewModel.kt`**:
  - Maps `dataRepository.data` of type `List<String>` to `MainScreenUiState`:
    ```kotlin
    class MainScreenViewModel(dataRepository: DataRepository) : ViewModel() {
      val uiState: StateFlow<MainScreenUiState> =
        dataRepository.data
          .map<List<String>, MainScreenUiState>(::Success)
          ...
    }
    ```
- **`app/src/main/java/com/example/interiorcamera/ui/ar/ArScreen.kt`**:
  - Standard single anchor state and simple tap gesture handler:
    ```kotlin
    var anchor by remember { mutableStateOf<Anchor?>(null) }
    ...
    onGestureListener = rememberOnGestureListener(
      onSingleTapConfirmed = { motionEvent: MotionEvent, node: Node? ->
        if (node == null) {
          val hitResults = frame?.hitTest(motionEvent.x, motionEvent.y)
          val newAnchor = hitResults?.firstOrNull()?.createAnchor()
          if (newAnchor != null) {
            anchor = newAnchor
          }
        }
        true
      }
    )
    ```
  - Single deletion button:
    ```kotlin
    if (anchor != null) {
      Button(onClick = { anchor = null }, ...) { Text("지우기") }
    }
    ```
- **`PROJECT.md`**:
  - Outlines the interface contracts for R5 (Custom Preset Storage) and references `PresetItem` domain models:
    ```kotlin
    interface DataRepository {
        val data: Flow<List<PresetItem>>
        suspend fun savePreset(preset: PresetItem)
    }
    ```

## 2. Logic Chain
1. The `DataRepository.kt` stub (Observation 1) lacks the CRUD capability required for Custom Preset Storage (R5), meaning a persistent layer like Room DB must be introduced.
2. The `ArScreen.kt` (Observation 3) restricts anchor placement to a single state slot (`var anchor` instead of a list of placed anchors) and has no node selection, individual deletion, or transform logic. Therefore, multi-anchor management (R4) requires transitioning to a list-based node state.
3. Because E2E tests for ARCore features on headless CI/CD systems or standard emulators run into hardware capture limitations (lack of physical plane data), a proper E2E testing framework must rely on a decoupled MVVM architecture (to test state transitions in JVM) and mock/stub UI components for Compose UI testing.
4. Hence, E2E test cases should target UI-side Compose rules (e.g., verifying sliders, chips, forms, and dialog states) while faking the underlying repository and ARCore engine.

## 3. Caveats
- Since this is a read-only investigation, the proposed test layout, Gradle configurations, and test code snippets are designs and drafts; they have not been run on actual hardware or compiled.
- We assumed Room is the storage of choice for R5. If the implementer decides to use Preferences DataStore, the DAO/Entity setup can be replaced by JSON-serialized String preferences, though the E2E UI tests remain identical.

## 4. Conclusion
We successfully analyzed the codebase and produced `analysis.md` outlining the current limitations, the recommended architectural updates to support R4 and R5, a suite of 10 comprehensive E2E test cases (5 Tier 1, 5 Tier 2), and a file structure layout. These artifacts provide a clear blueprint for the implementation team.

## 5. Verification Method
- **Locally Inspect Findings File**:
  - Open and verify the content of the analysis report at:
    `d:\project\interior_camera\.agents\teamwork_preview_explorer_m1_2\analysis.md`
- **Invalidation Conditions**:
  - If the Sceneview AR library version is upgraded to a version that completely alters `ARSceneView` gesture callbacks or state bindings, the faked/mocked AR test strategies might need adjustment.
