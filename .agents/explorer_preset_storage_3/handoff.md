# Handoff Report — Explorer 3 (Milestone 1: Custom Preset Storage)

## 1. Observation

During my read-only investigation of the `interior_camera` codebase, I made the following direct observations:

*   **`DataRepository.kt` (Lines 6–12)** is currently implemented as a simple string flow stub:
    ```kotlin
    interface DataRepository {
      val data: Flow<List<String>>
    }

    class DefaultDataRepository : DataRepository {
      override val data: Flow<List<String>> = flow { emit(listOf("Android")) }
    }
    ```
    This conflicts with the `PresetItem` model interface contract defined in `PROJECT.md` (Lines 41–50):
    ```kotlin
    data class PresetItem(val name: String, val width: Float, val height: Float, val depth: Float, val modelName: String = "cube.glb")

    interface DataRepository {
        val data: Flow<List<PresetItem>>
        suspend fun savePreset(preset: PresetItem)
    }
    ```

*   **`MainScreenViewModel.kt` (Lines 13–27)** manages UI states utilizing the old `List<String>` data format:
    ```kotlin
    class MainScreenViewModel(dataRepository: DataRepository) : ViewModel() {
      val uiState: StateFlow<MainScreenUiState> =
        dataRepository.data
          .map<List<String>, MainScreenUiState>(::Success)
          .catch { emit(MainScreenUiState.Error(it)) }
          .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)
    }

    sealed interface MainScreenUiState {
      object Loading : MainScreenUiState
      data class Error(val throwable: Throwable) : MainScreenUiState
      data class Success(val data: List<String>) : MainScreenUiState
    }
    ```

*   **`MainScreen.kt` (Lines 42–46)** currently has no integration with the ViewModel or Repository, relying purely on a static hardcoded `PRESETS` list and local composable states:
    ```kotlin
    @Composable
    fun MainScreen(
      onItemClick: (NavKey) -> Unit,
      modifier: Modifier = Modifier
    )
    ```

*   **`MainScreenTest.kt` (Lines 15–24)** is a broken template stub that does not compile due to a signature mismatch and asserts on nonexistent nodes:
    ```kotlin
    @Before
    fun setup() {
      composeTestRule.setContent { MainScreen(FAKE_DATA) } // Mismatch: FAKE_DATA is List<String>
    }

    @Test
    fun firstItem_exists() {
      FAKE_DATA.forEach { composeTestRule.onNodeWithText("Hello $it!").assertExists() } // "Hello Sample1!" does not exist
    }
    ```
    The compilation failure of instrumented tests was confirmed by running `.\gradlew compileDebugAndroidTestSources --no-daemon` which returned `exit code: 1` (Task logs: `task-81.log`).

---

## 2. Logic Chain

1.  **Repository Refactoring**: To support R5 (Custom Preset Storage), we must refactor `DataRepository` and its implementation `DefaultDataRepository` to use `PresetItem` instead of `String`.
2.  **Persistence Strategy**: Looking at the Gradle build configuration (`app/build.gradle.kts` and `gradle/libs.versions.toml`), no Room or DataStore libraries are present. However, Kotlin Serialization is active. Therefore, the most lightweight, robust, and native way to persist custom presets is using Android `SharedPreferences` to store a JSON-serialized list string of `PresetItem`.
3.  **ViewModel State Mapping**: The `MainScreenViewModel` and `MainScreenUiState` must be updated to consume `PresetItem` objects. A `savePreset(name, width, height, depth)` method must be added to launch a coroutine on `viewModelScope` and invoke the repository.
4.  **UI Overhaul**:
    *   `MainScreen` needs to retrieve the `MainScreenViewModel` instance. Since `Navigation.kt` is outside our target file scope, we should retain `MainScreen`'s public signature (`fun MainScreen(onItemClick: (NavKey) -> Unit, modifier: Modifier)`) and instantiate the ViewModel internally using `viewModel()` with a custom `ViewModelProvider.Factory` that supplies `DefaultDataRepository(context.applicationContext)`.
    *   The UI must include an input field for the product `name` and a button to save it (calling the VM's `savePreset` method).
    *   The screen must load custom presets from the VM's `uiState` (when `Success`) and display them under a "My List (저장된 프리셋)" section (similar to the default presets) using `FilterChip` components.
    *   Clicking a custom preset should update the input dimension states and select it. Clicking "AR 카메라로 확인하기" should resolve the model name correctly (either default preset models like `refrigerator.glb` or custom preset models defaulting to `cube.glb`).
5.  **Broken Tests Remediation**: Because `MainScreenTest.kt` is a broken template stub, it must be rewritten to match the actual layout (asserting headers, default chips, input forms, and saved list interaction) and correct the constructor compilation error. `MainScreenViewModelTest.kt` must be updated to mock the new `DataRepository` interface and test the `savePreset` flow.

---

## 3. Caveats

*   **JVM Unit Testing**: `DefaultDataRepository` depends on Android `SharedPreferences`, which requires Android runtime context. In local JVM unit tests (e.g. `MainScreenViewModelTest.kt`), the database or repository must be mocked/faked (using `FakeMyModelRepository` as already structured).
*   **Transitive Serialization Runtime**: This design assumes `kotlinx.serialization.json.Json` is transitively pulled by Navigation 3. If compiling throws a missing dependency error, a fallback manual string parser (e.g. joining fields with `|`) can be implemented in `DefaultDataRepository` to remain 100% independent of external libraries.

---

## 4. Conclusion

Implementing Custom Preset Storage ("My List") and Main Screen (R5) requires refactoring the interfaces and models in the three target files (`DataRepository.kt`, `MainScreenViewModel.kt`, `MainScreen.kt`), implementing Android `SharedPreferences` JSON storage in `DefaultDataRepository`, and updating local/instrumented tests to resolve compilation issues and assert correct behaviors.

### Recommended Implementation Steps for the Worker:

1.  **In `DataRepository.kt`**:
    *   Define `@Serializable data class PresetItem(...)`.
    *   Update `DataRepository` to use `PresetItem` and add `suspend fun savePreset(preset: PresetItem)`.
    *   Implement `DefaultDataRepository(private val context: Context)` using `SharedPreferences` and `kotlinx.serialization.json.Json` to store the presets. Use a `MutableStateFlow` to emit changes reactively.

2.  **In `MainScreenViewModel.kt`**:
    *   Refactor `MainScreenUiState.Success` to wrap `presets: List<PresetItem>`.
    *   Expose `savePreset(name, width, height, depth)` executing `dataRepository.savePreset(...)` inside `viewModelScope.launch`.

3.  **In `MainScreen.kt`**:
    *   Retrieve the ViewModel internally using `viewModel(factory = RememberViewModelFactory { DefaultDataRepository(context.applicationContext) })`.
    *   Collect UI state with `collectAsStateWithLifecycle()`.
    *   Add a "Product Name" input field.
    *   Add a "My List에 저장하기" button that triggers `viewModel.savePreset(...)` and clears the name input.
    *   Render a "My List (저장된 프리셋)" section showing custom presets. Clicking one selects it and populates input dimensions.

4.  **In Tests**:
    *   Update `MainScreenViewModelTest.kt` to mock the new interface and add a test verifying that `savePreset` updates the StateFlow.
    *   Update `MainScreenTest.kt` to fix the compilation error by matching the signature and write test assertions checking for actual UI components (the header, preset chips, text fields, and buttons).

---

## 5. Verification Method

To verify the implementation once completed by the Worker:

1.  **Compile & Run Unit Tests**:
    ```powershell
    .\gradlew test --no-daemon
    ```
    This verifies that `MainScreenViewModelTest.kt` compiles and passes successfully.

2.  **Compile Android Test Sources (Instrumented Tests)**:
    ```powershell
    .\gradlew compileDebugAndroidTestSources --no-daemon
    ```
    This verifies that the compilation error in `MainScreenTest.kt` has been fully resolved and all instrumented tests compile cleanly.

3.  **Inspect Files**:
    *   Verify that `PresetItem` is declared in `com.example.interiorcamera.data` (or imported).
    *   Verify that `DefaultDataRepository` uses SharedPreferences for local persistence.
