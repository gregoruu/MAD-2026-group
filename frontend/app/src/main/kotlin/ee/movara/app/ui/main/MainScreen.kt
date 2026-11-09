package ee.movara.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ee.movara.app.R
import ee.movara.app.ui.theme.MovaraTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import ee.movara.app.ui.components.PulsingPlaceholder
import ee.movara.app.ui.state.UiState


@Composable
fun MainScreen(
  viewModel: MainScreenViewModel,
  modifier: Modifier = Modifier
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  when (val state = uiState) {
    UiState.Loading -> {
      Column(
        modifier = modifier
          .fillMaxSize()
          .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        repeat(3) {
          PulsingPlaceholder(
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
          )
        }
      }
    }

    is UiState.Success -> {
      MainScreen(
        data = state.data,
        modifier = modifier
      )
    }

    UiState.Empty -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text("No data available")
      }
    }

    is UiState.Error -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
      ) {
        Text("Error loading data: ${state.message}")
      }
    }
  }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainScreen(data: List<String>, modifier: Modifier = Modifier) {
  Scaffold(
    modifier = modifier,
    topBar = {
      CenterAlignedTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.primary
        )
      )
    }
  ) { innerPadding ->

    // App Content

    Column(
      modifier = Modifier.padding(innerPadding),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      data.forEach { Greeting(it) }
      Text(
        text = stringResource(R.string.welcome_description),
        modifier = Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.bodyLarge
      )

    }
  }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
  Text(
    text = stringResource(R.string.welcome_title, name),
    modifier = modifier.padding(16.dp, 16.dp),
    style = MaterialTheme.typography.bodyLarge
  )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MainScreenPreview() {
  MovaraTheme { MainScreen(listOf("Movara")) }
}

