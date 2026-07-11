package com.example.interiorcamera.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.interiorcamera.data.DataRepository
import com.example.interiorcamera.data.DefaultDataRepository
import com.example.interiorcamera.data.PresetItem
import com.example.interiorcamera.data.RoomPreset
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {

  val uiState: StateFlow<MainScreenUiState> =
    combine(dataRepository.data, dataRepository.roomPresets) { presets, roomPresets ->
      MainScreenUiState.Success(presets = presets, roomPresets = roomPresets) as MainScreenUiState
    }.catch { emit(MainScreenUiState.Error(it)) }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = MainScreenUiState.Loading
    )

  fun savePreset(preset: PresetItem) {
    viewModelScope.launch {
      try {
        dataRepository.savePreset(preset)
      } catch (_: Exception) {}
    }
  }

  fun saveRoomPreset(roomPreset: RoomPreset) {
    viewModelScope.launch {
      try {
        dataRepository.saveRoomPreset(roomPreset)
      } catch (_: Exception) {}
    }
  }

  fun deleteRoomPreset(presetId: String) {
    viewModelScope.launch {
      try {
        dataRepository.deleteRoomPreset(presetId)
      } catch (_: Exception) {}
    }
  }

  fun deletePreset(presetId: String) {
    viewModelScope.launch {
      try {
        dataRepository.deletePreset(presetId)
      } catch (_: Exception) {}
    }
  }

  fun togglePresetFavorite(presetId: String) {
    viewModelScope.launch {
      try {
        dataRepository.togglePresetFavorite(presetId)
      } catch (_: Exception) {}
    }
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val application = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
          ?: throw IllegalArgumentException("Application key missing")
        val repository = DefaultDataRepository(application.applicationContext)
        MainScreenViewModel(repository)
      }
    }
  }
}

sealed interface MainScreenUiState {
  object Loading : MainScreenUiState
  data class Error(val throwable: Throwable) : MainScreenUiState
  data class Success(
    val presets: List<PresetItem>,
    val roomPresets: List<RoomPreset> = emptyList()
  ) : MainScreenUiState
}


