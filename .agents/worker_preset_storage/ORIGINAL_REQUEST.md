## 2026-07-04T13:43:02+09:00
Implement Milestone 1: Custom Preset Storage ("My List") & Main Screen (R5) as described in the PROJECT.md and SCOPE.md.
Follow the synthesized strategy:
1. Define the `PresetItem` model class under the `com.example.interiorcamera.data` package.
2. Refactor `DataRepository` and `DefaultDataRepository` (app/src/main/java/com/example/interiorcamera/data/DataRepository.kt) to use `List<PresetItem>` instead of `List<String>`. Use Android's `SharedPreferences` along with `org.json.JSONArray` and `org.json.JSONObject` to serialize/deserialize custom presets as a JSON string. The constructor of `DefaultDataRepository` should accept `context: Context`.
3. Update `MainScreenViewModel` and `MainScreenUiState` (app/src/main/java/com/example/interiorcamera/ui/main/MainScreenViewModel.kt) to use `PresetItem`. Create a custom `ViewModelProvider.Factory` in `MainScreenViewModel.Companion` using `CreationExtras` to obtain the Android Application context and instantiate the repository.
4. Update `MainScreen` (app/src/main/java/com/example/interiorcamera/ui/main/MainScreen.kt):
   - Inject the ViewModel and collect its UI state using lifecycle-aware Compose state collection.
   - Remove the local definition of `PresetItem`.
   - Add a product name text field and a button to save custom inputs ("Save to My List") to the form.
   - Display saved presets under a new section "나의 리스트".
   - Select custom presets to populate size text fields.
5. Fix compile/logic errors in the test files:
   - app/src/test/java/com/example/interiorcamera/ui/main/MainScreenViewModelTest.kt
   - app/src/androidTest/java/com/example/interiorcamera/ui/main/MainScreenTest.kt
6. Run gradle builds and unit tests via gradlew to verify everything builds and passes.
