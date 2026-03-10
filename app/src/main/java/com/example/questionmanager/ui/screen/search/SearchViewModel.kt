package com.example.questionmanager.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.usecase.SearchQuestionsUseCase
import com.example.questionmanager.util.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchQuestionsUseCase: SearchQuestionsUseCase
) : ViewModel() {

    data class SearchUiState(
        val query: String = "",
        val results: List<Question> = emptyList(),
        val isSearching: Boolean = false,
        val hasSearched: Boolean = false
    )

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(query = query)

        searchJob?.cancel()

        if (query.isBlank()) {
            _uiState.value = _uiState.value.copy(
                results = emptyList(),
                isSearching = false,
                hasSearched = false
            )
            return
        }

        searchJob = viewModelScope.launch {
            delay(Constants.SEARCH_DEBOUNCE_MS)
            _uiState.value = _uiState.value.copy(isSearching = true)

            searchQuestionsUseCase(query)
                .catch {
                    _uiState.value = _uiState.value.copy(
                        isSearching = false,
                        hasSearched = true
                    )
                }
                .collect { results ->
                    _uiState.value = _uiState.value.copy(
                        results = results,
                        isSearching = false,
                        hasSearched = true
                    )
                }
        }
    }
}

