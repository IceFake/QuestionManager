package com.example.questionmanager.ui.screen.search

import app.cash.turbine.test
import com.example.questionmanager.domain.model.Question
import com.example.questionmanager.domain.model.QuestionStatus
import com.example.questionmanager.domain.usecase.SearchQuestionsUseCase
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var searchUseCase: SearchQuestionsUseCase
    private lateinit var viewModel: SearchViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        searchUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty`() {
        viewModel = SearchViewModel(searchUseCase)
        val state = viewModel.uiState.value
        assertEquals("", state.query)
        assertTrue(state.results.isEmpty())
        assertFalse(state.isSearching)
        assertFalse(state.hasSearched)
    }

    @Test
    fun `blank query clears results`() = runTest {
        viewModel = SearchViewModel(searchUseCase)
        viewModel.onQueryChanged("   ")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.results.isEmpty())
        assertFalse(state.hasSearched)
    }

    @Test
    fun `search with query returns results after debounce`() = runTest {
        val questions = listOf(
            Question(id = 1, question = "MVVM是什么", status = QuestionStatus.COMPLETED)
        )
        every { searchUseCase("MVVM") } returns flowOf(questions)

        viewModel = SearchViewModel(searchUseCase)
        viewModel.onQueryChanged("MVVM")

        // Advance past debounce
        advanceTimeBy(400)
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("MVVM", state.query)
            assertEquals(1, state.results.size)
            assertTrue(state.hasSearched)
            cancelAndIgnoreRemainingEvents()
        }
    }
}

