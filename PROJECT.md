# Project: FitCheck AR UI/UX Improvements

## Architecture
FitCheck AR is an Android application using Jetpack Compose and Sceneview AR (ARCore-based) for spatial visualization.

- **Data Tier**: `DataRepository.kt` manages presets and favorite item dimensions (custom presets).
- **Presentation Tier**:
  - `MainScreen.kt` displays default presets and custom favorites, plus an entry form to add custom items.
  - `MainScreenViewModel.kt` retrieves and serves custom presets from the data layer.
  - `ArScreen.kt` manages 3D model loading, user gesture detection (placement, rotation), and rendering overlays (opacity, plane detection coachmark, rotation controls).
  - `Navigation.kt` handles navigation between `MainScreen` and `ArScreen`.

---

## Code Layout
- `app/src/main/java/com/example/interiorcamera/`
  - `MainActivity.kt` — Entry point activity
  - `Navigation.kt` — Compose navigation setup
  - `NavigationKeys.kt` — Nav arguments / destinations
  - `data/DataRepository.kt` — Data storage interface and implementation
  - `ui/main/MainScreen.kt` — Presets & custom form UI
  - `ui/main/MainScreenViewModel.kt` — Viewmodel for main screen presets
  - `ui/ar/ArScreen.kt` — AR scene and controls overlay

---

## Milestones
| # | Name | Scope | Dependencies | Status |
|---|------|-------|-------------|--------|
| 1 | E2E Testing Setup | Create testing infrastructure and test runner | None | IN_PROGRESS (Conv ID: 79864622-55d3-485a-bc6a-f3ea0f824282) |
| 2 | Custom Preset Storage | Storage and UI for user presets (R5) | None | IN_PROGRESS (Conv ID: 7f2cf0b0-f94f-4b49-8aee-b815bb80df02) |
| 3 | Basic AR Overlay | Opacity slider & Plane detection guide UI (R1, R3) | None | IN_PROGRESS (Conv ID: 7f2cf0b0-f94f-4b49-8aee-b815bb80df02) |
| 4 | AR Model Interaction | Multi-Anchor & Rotation controls/gestures (R2, R4) | M3 | IN_PROGRESS (Conv ID: 7f2cf0b0-f94f-4b49-8aee-b815bb80df02) |
| 5 | E2E & Adversarial Verification | Run E2E tests and white-box coverage hardening | M1, M2, M3, M4 | PLANNED |

---

## Interface Contracts

### MainScreen ↔ DataRepository / ViewModel
- `PresetItem` model:
  ```kotlin
  data class PresetItem(val name: String, val width: Float, val height: Float, val depth: Float, val modelName: String = "cube.glb")
  ```
- `DataRepository`:
  ```kotlin
  interface DataRepository {
      val data: Flow<List<PresetItem>>
      suspend fun savePreset(preset: PresetItem)
  }
  ```
- `MainScreenUiState`:
  ```kotlin
  sealed interface MainScreenUiState {
      object Loading : MainScreenUiState
      data class Error(val throwable: Throwable) : MainScreenUiState
      data class Success(val presets: List<PresetItem>) : MainScreenUiState
  }
  ```

### MainScreen ↔ ArScreen (Navigation)
- Navigated via `ArView` destination class:
  ```kotlin
  data class ArView(val widthCm: Float, val heightCm: Float, val depthCm: Float, val modelName: String) : NavKey
  ```
- Support passing multiple items or launching into the interactive AR view.
