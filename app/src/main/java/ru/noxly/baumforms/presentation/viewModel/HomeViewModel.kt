package ru.noxly.baumforms.presentation.viewModel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow("Hello from Home!")
    val uiState: StateFlow<String> = _uiState

    fun changeMessage(newMsg: String) {
        _uiState.value = newMsg
    }
}