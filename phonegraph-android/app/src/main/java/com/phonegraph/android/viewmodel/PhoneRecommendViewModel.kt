package com.phonegraph.android.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.phonegraph.android.data.PhoneGraphRepository
import com.phonegraph.android.data.RecommendationResponse
import com.phonegraph.android.data.RecommendationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * UI state for the single recommend screen. A sealed interface so the
 * Composable can exhaustively `when` over every possible state instead
 * of juggling separate loading/error/data booleans.
 */
sealed interface RecommendUiState {
    data object Idle : RecommendUiState
    data object Loading : RecommendUiState
    data class Success(val response: RecommendationResponse) : RecommendUiState
    data class Error(val message: String) : RecommendUiState
}

class PhoneRecommendViewModel(
    private val repository: PhoneGraphRepository = PhoneGraphRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<RecommendUiState>(RecommendUiState.Idle)
    val uiState: StateFlow<RecommendUiState> = _uiState

    fun askQuestion(query: String) {
        if (query.isBlank()) {
            _uiState.value = RecommendUiState.Error("Please enter a question first.")
            return
        }

        _uiState.value = RecommendUiState.Loading

        viewModelScope.launch {
            when (val result = repository.getRecommendation(query)) {
                is RecommendationResult.Success ->
                    _uiState.value = RecommendUiState.Success(result.response)
                is RecommendationResult.Error ->
                    _uiState.value = RecommendUiState.Error(result.message)
            }
        }
    }
}
