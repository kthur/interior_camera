# Handoff Report — Explorer 1

## 1. Observation
From a thorough review of the codebase, the following files and configurations were observed:

1. **`PROJECT.md` Interface Contracts**:
   ```kotlin
   data class PresetItem(val name: String, val width: Float, val height: Float, val depth: Float, val modelName: String = "cube.glb")

   interface DataRepository {
       val data: Flow<List<PresetItem>>
       suspend fun savePreset(preset: PresetItem)
   }

   sealed interface MainScreenUiState {
       object Loading : MainScreenUiState
       data class Error(val throwable: Throwable) : MainScreenUiState
       data class Success(val presets: List<PresetItem>) : MainScreenUiState
   }
   ```
2. **Current `DataRepository.kt` (lines 6-12)**:
   ```kotlin
   interface DataRepository {
     val data: Flow<List<String>>
   }

   class DefaultDataRepository : DataRepository {
     override val data: Flow<List<String>> = flow { emit(listOf("Android")) }
   }
   ```
3. **Current `MainScreenViewModel.kt` (lines 13-27)**:
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
4. **Current `MainScreen.kt`**:
   - `PresetItem` is defined locally (lines 26-32) and default presets are defined as a hardcoded list `PRESETS` (lines 34-40).
   - Filter chips display default presets (lines 113-124).
   - Form fields exist for `widthStr`, `heightStr`, and `depthStr` (lines 140-177).
   - There is no field for `name` and no button or logic to save the custom inputs to a list.
   - The screen does not receive or observe `MainScreenViewModel` or collect `uiState`.
5. **`app/build.gradle.kts` (lines 46-87)**:
   - Contains dependencies for Core Android, Compose, Jetpack Lifecycle, Navigation 3, and SceneView AR.
   - Does **NOT** contain Room Database or Preferences DataStore libraries.
6. **`app/src/main/java/com/example/interiorcamera/Navigation.kt` (lines 23-25)**:
   - Instantiates `MainScreen` without injecting or passing any ViewModel:
     ```kotlin
     entry<Main> {
       MainScreen(onItemClick = { navKey -> backStack.add(navKey) }, modifier = Modifier.safeDrawingPadding().padding(16.dp))
     }
     ```

---

## 2. Logic Chain
- **Step 1**: The user requires custom presets containing name, width, height, and depth to be saved locally (R5) and displayed on the Main Screen alongside default presets.
- **Step 2**: The current `DataRepository.kt` and `MainScreenViewModel.kt` are skeleton files that operate on `List<String>` rather than the specified `PresetItem` structure.
- **Step 3**: Looking at `app/build.gradle.kts`, there are no local databases (Room) or preferences (DataStore) libraries declared.
- **Step 4**: To implement local persistence without modifying Gradle dependencies, Android’s native `SharedPreferences` is the ideal solution.
- **Step 5**: To serialize and deserialize `PresetItem` lists within `SharedPreferences`, standard `org.json.JSONArray` and `org.json.JSONObject` (which are built into the Android SDK) can be used to convert the list of custom presets into a JSON string.
- **Step 6**: Because `SharedPreferences` requires an Android `Context`, the `DefaultDataRepository` constructor must accept a `context: Context`.
- **Step 7**: To instantiate `MainScreenViewModel` with `DefaultDataRepository(context)` without passing the context down manually through navigation, we can use Compose's viewmodel-aware CreationExtras in a custom `ViewModelProvider.Factory` inside `MainScreenViewModel`.
- **Step 8**: `MainScreen.kt` must be updated to accept the ViewModel, collect its `uiState`, add a Name input field and a "Save to My List" button, and display saved presets under a new section (e.g. `"나의 리스트"`).

---

## 3. Caveats
- **Dependencies**: The recommendation avoids adding Room or DataStore to prevent build configuration changes. If Room or DataStore is preferred, the build configuration must be modified by the worker.
- **Model Colocation**: Currently, `PresetItem` is defined in `MainScreen.kt`. It must be moved to `data/` package or a separate file to prevent compilation errors and imports circularity.
- **Visual Presentation**: While "alongside default presets" is required, custom presets are best displayed under their own sub-header (e.g., "나의 리스트") so the user can easily distinguish between pre-bundled and user-saved items.

---

## 4. Conclusion & Recommended Implementation Strategy
To implement Milestone 1, we propose the following changes:

### Phase 4.1: Move and Refactor `PresetItem`
1. Define `PresetItem` in a dedicated file under `com.example.interiorcamera.data` package.
2. Update imports in `MainScreen.kt` and remove the duplicate local definition.

### Phase 4.2: Update `DataRepository.kt`
Update `DataRepository` interface and implement `DefaultDataRepository` using `SharedPreferences` & `org.json`:
```kotlin
package com.example.interiorcamera.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class PresetItem(
  val name: String,
  val width: Float,
  val height: Float,
  val depth: Float,
  val modelName: String = "cube.glb"
)

interface DataRepository {
  val data: Flow<List<PresetItem>>
  suspend fun savePreset(preset: PresetItem)
}

class DefaultDataRepository(private val context: Context) : DataRepository {
  private val sharedPreferences = context.getSharedPreferences("presets_prefs", Context.MODE_PRIVATE)
  private val PRESETS_KEY = "custom_presets"

  private val _presets = MutableStateFlow<List<PresetItem>>(emptyList())
  override val data: Flow<List<PresetItem>> = _presets.asStateFlow()

  init {
    loadPresets()
  }

  private fun loadPresets() {
    val jsonString = sharedPreferences.getString(PRESETS_KEY, "[]") ?: "[]"
    val list = mutableListOf<PresetItem>()
    try {
      val jsonArray = JSONArray(jsonString)
      for (i in 0 until jsonArray.length()) {
        val obj = jsonArray.getJSONObject(i)
        list.add(
          PresetItem(
            name = obj.getString("name"),
            width = obj.getDouble("width").toFloat(),
            height = obj.getDouble("height").toFloat(),
            depth = obj.getDouble("depth").toFloat(),
            modelName = obj.optString("modelName", "cube.glb")
          )
        )
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
    _presets.value = list
  }

  override suspend fun savePreset(preset: PresetItem) {
    val currentList = _presets.value.toMutableList()
    currentList.add(preset)

    val jsonArray = JSONArray()
    currentList.forEach { item ->
      val obj = JSONObject().apply {
        put("name", item.name)
        put("width", item.width.toDouble())
        put("height", item.height.toDouble())
        put("depth", item.depth.toDouble())
        put("modelName", item.modelName)
      }
      jsonArray.put(obj)
    }

    sharedPreferences.edit().putString(PRESETS_KEY, jsonArray.toString()).apply()
    _presets.value = currentList
  }
}
```

### Phase 4.3: Update `MainScreenViewModel.kt`
1. Redefine `MainScreenUiState` to hold `Success(val presets: List<PresetItem>)`.
2. Add `savePreset` and implement `Factory` using `CreationExtras`:
```kotlin
package com.example.interiorcamera.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.interiorcamera.data.DataRepository
import com.example.interiorcamera.data.DefaultDataRepository
import com.example.interiorcamera.data.PresetItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
  val uiState: StateFlow<MainScreenUiState> =
    dataRepository.data
      .map<List<PresetItem>, MainScreenUiState> { MainScreenUiState.Success(it) }
      .catch { emit(MainScreenUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun savePreset(name: String, width: Float, height: Float, depth: Float, modelName: String = "cube.glb") {
    viewModelScope.launch {
      dataRepository.savePreset(PresetItem(name, width, height, depth, modelName))
    }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
        val repository = DefaultDataRepository(application)
        return MainScreenViewModel(repository) as T
      }
    }
  }
}

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(val presets: List<PresetItem>) : MainScreenUiState
}
```

### Phase 4.4: Update `MainScreen.kt`
1. Inject the ViewModel into `MainScreen` parameter:
   ```kotlin
   @Composable
   fun MainScreen(
     onItemClick: (NavKey) -> Unit,
     modifier: Modifier = Modifier,
     viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory)
   ) {
     val uiState by viewModel.uiState.collectAsStateWithLifecycle()
     var nameStr by remember { mutableStateOf("") }
     // Keep existing state for widthStr, heightStr, depthStr...
   ```
2. Display the saved presets below the default presets section:
   ```kotlin
   HorizontalDivider()
   Text(
     text = "나의 리스트 (저장된 규격)",
     style = MaterialTheme.typography.titleMedium,
     modifier = Modifier.align(Alignment.Start)
   )

   when (val state = uiState) {
     is MainScreenUiState.Loading -> {
       CircularProgressIndicator()
     }
     is MainScreenUiState.Error -> {
       Text("에러가 발생했습니다: ${state.throwable.localizedMessage}")
     }
     is MainScreenUiState.Success -> {
       if (state.presets.isEmpty()) {
         Text(
           text = "저장된 커스텀 프리셋이 없습니다.",
           style = MaterialTheme.typography.bodyMedium,
           color = MaterialTheme.colorScheme.onSurfaceVariant
         )
       } else {
         Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
           state.presets.forEach { preset ->
             Card(
               onClick = {
                 widthStr = preset.width.toString()
                 heightStr = preset.height.toString()
                 depthStr = preset.depth.toString()
                 selectedPresetIndex = -1
               },
               modifier = Modifier.fillMaxWidth()
             ) {
               Column(modifier = Modifier.padding(12.dp)) {
                 Text(text = preset.name, style = MaterialTheme.typography.titleSmall)
                 Text(
                   text = "가로 ${preset.width}cm x 세로 ${preset.height}cm x 깊이 ${preset.depth}cm",
                   style = MaterialTheme.typography.bodyMedium,
                   color = MaterialTheme.colorScheme.onSurfaceVariant
                 )
               }
             }
           }
         }
       }
     }
   }
   ```
3. Add a Custom Preset input field (for Name) and a "Save to My List" button in the form:
   ```kotlin
   // Inside form, before size inputs:
   OutlinedTextField(
     value = nameStr,
     onValueChange = { nameStr = it },
     label = { Text("제품 이름 (Name)") },
     modifier = Modifier.fillMaxWidth(),
     singleLine = true
   )

   // Below size inputs, before the main AR launch button:
   Button(
     onClick = {
       if (nameStr.isNotBlank() && isValid) {
         viewModel.savePreset(nameStr, width, height, depth)
         nameStr = "" // Reset name input
       }
     },
     enabled = nameStr.isNotBlank() && isValid,
     modifier = Modifier.fillMaxWidth()
   ) {
     Text("나의 리스트에 저장하기")
   }
   ```

---

## 5. Verification Method
1. **Executing Tests**:
   Run the local JVM unit tests:
   `gradlew.bat test`
   And Android instrumented tests on an emulator/device:
   `gradlew.bat connectedAndroidTest`
2. **Files to Inspect**:
   - `app/src/main/java/com/example/interiorcamera/data/DataRepository.kt` — Verify `PresetItem` model, `DataRepository` interface, and `DefaultDataRepository` class.
   - `app/src/main/java/com/example/interiorcamera/ui/main/MainScreenViewModel.kt` — Verify `MainScreenUiState` uses `PresetItem`, and `Factory` correctly constructs the repository using creation extras.
   - `app/src/main/java/com/example/interiorcamera/ui/main/MainScreen.kt` — Verify Composable signature, state collection, `nameStr` text field, save button, and custom presets list display.
3. **Validation Test Scenarios**:
   - Entering invalid data (e.g. width = 0, name = empty) should disable the "Save" button.
   - Saving a valid custom preset should immediately update the UI with the saved preset item in "나의 리스트".
   - Clicking a saved custom preset should populate the text fields (width, height, depth) and allow launching the AR screen with these dimensions.
   - Closing and reopening the app should persist the custom presets list via `SharedPreferences`.
