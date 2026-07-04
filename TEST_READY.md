# TEST_READY — FitCheck AR E2E Test Suite Summary

This document lists and categorizes the test suite for FitCheck AR. The test suite has been expanded to a total of 66 test cases (60 newly added/updated + 6 challenger baseline), covering unit tests, integration tests, and instrumented UI tests across 4 Tiers.

---

## 1. Summary of Test Files & Case Counts

| Test File | Type | Target | Test Cases |
| :--- | :--- | :--- | :--- |
| `DefaultDataRepositoryTest.kt` | Unit | SharedPreferences & JSON Storage | 10 |
| `MainScreenViewModelTest.kt` | Unit | Viewmodel & State Transitions | 8 |
| `ArScreenStateTest.kt` | Unit | AR Model Math & State Collection | 15 |
| `MainScreenTest.kt` | Instrumented | Main Presets & Custom Form UI | 12 |
| `ArScreenTest.kt` | Instrumented | AR Guide, Overlays & Control UI | 15 |
| `ChallengerPresetStorageTest.kt` | Unit | Edge-case storage robustness | 6 |
| **Total** | | | **66** |

---

## 2. Test Categorization by 4-Tier Plan

### Tier 1: Feature Coverage — Happy Path
Ensures each of the five requirements works correctly under normal, expected usage.

- **DefaultDataRepositoryTest**
  - `testSaveNormalPreset`: Saves a valid preset item and verifies it persists.
  - `testSavePresetMultipleItems`: Saves multiple valid items and verifies all exist.
- **MainScreenViewModelTest**
  - `uiState_initiallyLoading`: ViewModel initial state starts as Loading or Success.
  - `uiState_onItemSaved_isDisplayed`: Saved custom item is correctly displayed in the success list.
  - `uiState_initialUiStateIsSuccessWithEmptyListWhenRepoIsEmpty`: Success state with empty list when repo is empty.
  - `uiState_saveMultiplePresetsPropagated`: Saving multiple items propagates correctly to the UI.
  - `uiState_loadPresetsTriggeredOnInit`: Initial data is correctly loaded from repository on VM initialization.
- **ArScreenStateTest**
  - `testPlacedItemScaleCalculation`: Verifies proper scale conversions from cm to meters.
  - `testPlacedItemPositionCalculation`: Verifies vertical centering calculation for model placement.
  - `testAddPlacedItemToList`: Verifies adding placed item updates collection and selects it.
  - `testSelectPlacedItem`: Verifies selecting placed items updates selection ID.
- **MainScreenTest**
  - `defaultPresets_areDisplayed`: Default preset chips are rendered.
  - `customPresets_areDisplayed`: Custom preset chips are displayed.
  - `savePreset_invokesCallback`: Saving preset triggers save callback.
  - `clickPresetChip_populatesForm`: Clicking a preset chip fills input fields.
  - `clickPresetThenViewInAR_navigatesWithCorrectArgs`: Selecting preset and launching AR navigates with arguments.
  - `myListSection_displaysExpectedCount`: Renders correct custom list size.
- **ArScreenTest**
  - `testArScreenInitialLayout`: AR screen status header and guides are displayed initially.
  - `testArScreenGuidesVisibleWhenNoPlane`: Guide displays instructions while scanning.
  - `testClickBackButtonNavigatesBack`: Back button click triggers navigation callback.
  - `testArOverlayDisplaysDimensions`: Verify status text displays correct dimensions.
  - `testCircularProgressIndicatorShownInGuide`: Guide UI renders loading progress bar.

---

### Tier 2: Boundary, Corner, & Negative Cases
Tests stability and correctness under extreme inputs, empty lists, limits, and system interruptions.

- **DefaultDataRepositoryTest**
  - `testSavePresetWithInfinity_throwsException`: Checks that saving POSITIVE_INFINITY triggers JSONException.
  - `testSavePresetWithEmptyName`: Checks that saving an empty name preset does not crash repository.
  - `testSavePresetWithZeroDimensions`: Checks that saving zero dimension preset does not crash repository.
  - `testSavePresetWithNegativeDimensions`: Checks that saving negative dimension preset does not crash repository.
  - `testSavePresetWithHugeDimensions`: Checks that saving huge dimension preset does not crash repository.
  - `testLoadPresetsWhenEmpty`: Checks fallback list is empty when key is not present.
  - `testSaveDuplicatePresetName`: Verifies saving presets with the same name preserves both entries.
  - `testLoadCorruptedPresetsFallback`: Verifies repository falls back to empty list on corrupt JSON string.
- **MainScreenViewModelTest**
  - `uiState_saveInvalidPreset_doesNotPersist`: Verifies invalid presets are not propagated to success state.
  - `uiState_loadErrorPropagation`: Catches repository flow exceptions and propagates as Error state.
  - `uiState_saveDuplicatePresetHandled`: Duplicates are accepted and processed cleanly without errors.
- **ArScreenStateTest**
  - `testPlacedItemRotationWrapAround`: Y-axis rotation angles wrap modulo 360 degrees.
  - `testPlacedItemOpacityConstraints`: Opacity values clamp to [0.1, 1.0].
  - `testRemovePlacedItemFromList`: Deleting an item removes it and clears active selection.
  - `testClearAllPlacedItems`: Clearing all anchors works cleanly on list state.
  - `testDeselectBeforeDeletingDoesNotDeleteAny`: Safe deletion check when no active node is selected.
  - `testRotateDeselectedNodeDoesNothing`: Safe rotation check when no active node is selected.
  - `testModifyOpacityDeselectedNodeDoesNothing`: Safe opacity check when no active node is selected.
- **MainScreenTest**
  - `savePreset_emptyName_showsError`: Save is disabled when name is blank.
  - `savePreset_zeroDimensions_showsError`: Save is disabled when dimension is 0.
  - `savePreset_largeDimensions_succeeds`: Saves extremely large dimensions successfully.
  - `formValidation_clearsOnErrorResolved`: Clears disable-state on correction of input error.
  - `customPresetsHeader_hiddenWhenNoCustomPresets`: "나의 리스트" header is hidden if custom list is empty.
  - `defaultPresetClick_doesNotPopulateForm`: Asserts that default preset click populates form with valid data (doesn't clear).
- **ArScreenTest**
  - `testDeselectedStateHidesControls`: Selected controls card hides when no item is selected.
  - `testSelectedStateShowsControls`: Selected controls card shows when selectedItemId is populated.
  - `testNoCameraPermissionShowsPermissionError`: Camera missing permission UI is shown when permission is false.
  - `testOverlayDisappearsWhenPlaneDetected`: Guide UI disappears once horizontal plane is tracked.

---

### Tier 3: Cross-Feature Interaction Cases
Verifies correct behavior when multiple distinct features interact or compete for resources.

- **ArScreenStateTest**
  - `testOpacityUpdatedForSelectedItem`: Opacity slider updates alpha ONLY for the selected node.
  - `testRotationUpdatedForSelectedItem`: Rotation buttons update y-angle ONLY for the selected node.
  - `testMultipleAnchorsPlacedIndependently`: Confirms placing multiple anchors doesn't share/overwrite independent size/rotation/opacity states.
- **ArScreenTest**
  - `testSliderValueChangeTriggersCallback`: Opacity slider value changes trigger callback for the selected node.
  - `testRotateLeftButtonTriggersCallback`: Rotate left button triggers callback for the selected node.
  - `testRotateRightButtonTriggersCallback`: Rotate right button triggers callback for the selected node.

---

### Tier 4: Real-World User Scenarios
High-level workflows representing typical user interactions.

- **ArScreenStateTest**
  - `testDeselectItem`: Deselecting active item removes control overlays without deleting nodes.
- **ArScreenTest**
  - `testDeleteButtonTriggersCallback`: Deleting selected model works cleanly.
  - `testClearAllButtonTriggersCallback`: Clearing all models clears the scene completely.
  - `testDeselectedButtonClosesControlCard`: "선택 해제" button triggers deselect callback to close the card.
