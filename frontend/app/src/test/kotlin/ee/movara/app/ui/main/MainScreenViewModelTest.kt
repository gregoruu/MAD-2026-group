package ee.movara.app.ui.main

import ee.movara.app.data.repository.DataRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import ee.movara.app.ui.state.UiState

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {
  private val dispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(dispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_initiallyLoading() = runTest {
    val viewModel = MainScreenViewModel(FakeDataRepository(flowOf(listOf("Movara"))))
    assertEquals(UiState.Loading, viewModel.uiState.value)
  }

  @Test
  fun uiState_emitsRepositoryDataWhileObserved() = runTest {
    val data = listOf("Movara")
    val viewModel = MainScreenViewModel(FakeDataRepository(flowOf(data)))
    backgroundScope.launch(dispatcher) { viewModel.uiState.collect() }
    assertEquals(UiState.Success(data), viewModel.uiState.value)
  }

  @Test
  fun uiState_emitsErrorWhenRepositoryFails() = runTest {
    val error = IllegalStateException("Unable to load data")
    val viewModel = MainScreenViewModel(FakeDataRepository(flow { throw error }))
    backgroundScope.launch(dispatcher) { viewModel.uiState.collect() }
    assertSame(error, (viewModel.uiState.value as UiState.Error).cause)
  }
}

private class FakeDataRepository(override val data: Flow<List<String>>) : DataRepository
