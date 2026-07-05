package com.example.interiorcamera.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.interiorcamera.data.DataRepository
import com.example.interiorcamera.data.DefaultDataRepository
import com.example.interiorcamera.data.PresetItem
import com.example.interiorcamera.ui.main.MainScreenUiState.Success
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainScreenViewModel(private val dataRepository: DataRepository) : ViewModel() {
  val uiState: StateFlow<MainScreenUiState> =
    dataRepository.data
      .map<List<PresetItem>, MainScreenUiState> { Success(it) }
      .catch { emit(MainScreenUiState.Error(it)) }
      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainScreenUiState.Loading)

  fun savePreset(preset: PresetItem) {
    viewModelScope.launch {
      try {
        dataRepository.savePreset(preset)
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

  data class Success(val presets: List<PresetItem>) : MainScreenUiState
}

