package com.example.interiorcamera.ui.main

import com.example.interiorcamera.data.DataRepository
import com.example.interiorcamera.data.PresetItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun uiState_initiallyLoading() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)
    val state = viewModel.uiState.first()
    assert(state is MainScreenUiState.Loading || state is MainScreenUiState.Success)
  }

  @Test
  fun uiState_onItemSaved_isDisplayed() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)

    // Save a custom preset
    val customItem = PresetItem("My custom desk", 120.0f, 75.0f, 60.0f)
    viewModel.savePreset(customItem)
    runCurrent()

    // Wait until Success is received and verify it contains the custom item
    val successState = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
    assertEquals(1, successState.presets.size)
    assertEquals(customItem, successState.presets[0])
  }

  @Test
  fun uiState_saveInvalidPreset_doesNotPersist() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)

    val invalidItem = PresetItem("", -5f, 0f, 10f)
    viewModel.savePreset(invalidItem)
    runCurrent()

    val successState = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
    assertTrue(successState.presets.isEmpty())
  }

  @Test
  fun uiState_loadErrorPropagation() = runTest {
    val errorRepository = object : DataRepository {
      override val data: Flow<List<PresetItem>> = kotlinx.coroutines.flow.flow {
        throw RuntimeException("Load failed")
      }
      override suspend fun savePreset(preset: PresetItem) {}
    }
    val viewModel = MainScreenViewModel(errorRepository)
    val state = viewModel.uiState.first { it is MainScreenUiState.Error } as MainScreenUiState.Error
    assertEquals("Load failed", state.throwable.message)
  }

  @Test
  fun uiState_initialUiStateIsSuccessWithEmptyListWhenRepoIsEmpty() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)
    val successState = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
    assertTrue(successState.presets.isEmpty())
  }

  @Test
  fun uiState_saveMultiplePresetsPropagated() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)
    val item1 = PresetItem("Desk", 120f, 75f, 60f)
    val item2 = PresetItem("Chair", 50f, 90f, 50f)
    viewModel.savePreset(item1)
    viewModel.savePreset(item2)
    runCurrent()
    val successState = viewModel.uiState.first { it is MainScreenUiState.Success && it.presets.size == 2 } as MainScreenUiState.Success
    assertEquals(2, successState.presets.size)
    assertEquals(item1, successState.presets[0])
    assertEquals(item2, successState.presets[1])
  }

  @Test
  fun uiState_saveDuplicatePresetHandled() = runTest {
    val repository = FakeMyModelRepository()
    val viewModel = MainScreenViewModel(repository)
    val item = PresetItem("Desk", 120f, 75f, 60f)
    viewModel.savePreset(item)
    viewModel.savePreset(item)
    runCurrent()
    val successState = viewModel.uiState.first { it is MainScreenUiState.Success && it.presets.size == 2 } as MainScreenUiState.Success
    assertEquals(2, successState.presets.size)
    assertEquals(item, successState.presets[0])
    assertEquals(item, successState.presets[1])
  }

  @Test
  fun uiState_loadPresetsTriggeredOnInit() = runTest {
    val repository = FakeMyModelRepository()
    val initialItem = PresetItem("Initial Desk", 100f, 70f, 60f)
    repository.savePreset(initialItem)
    val viewModel = MainScreenViewModel(repository)
    val successState = viewModel.uiState.first { it is MainScreenUiState.Success } as MainScreenUiState.Success
    assertEquals(1, successState.presets.size)
    assertEquals(initialItem, successState.presets[0])
  }
}

private class FakeMyModelRepository : DataRepository {
  private val _data = MutableStateFlow<List<PresetItem>>(emptyList())
  override val data: Flow<List<PresetItem>> = _data.asStateFlow()

  override suspend fun savePreset(preset: PresetItem) {
    if (preset.name.isEmpty() || preset.width <= 0f) {
      throw IllegalArgumentException("Invalid preset")
    }
    _data.value = _data.value + preset
  }
}


