package ee.movara.app.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.decode.DataSource
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/** Drives the real composable, holding requests in Loading until the test releases them. */
@OptIn(coil3.annotation.DelicateCoilApi::class)
class MovieImageTest {
    @get:Rule val compose = createComposeRule()
    private val responses = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private lateinit var loader: ImageLoader
    private val requests = AtomicInteger()

    @Before fun setUp() {
        loader = ImageLoader.Builder(ApplicationProvider.getApplicationContext())
            .components {
                add(coil3.intercept.Interceptor { chain ->
                    requests.incrementAndGet()
                    val response = responses[chain.request.data]
                    if (response == null) {
                        chain.proceed()
                    } else if (response.await()) {
                        SuccessResult(ColorImage(Color.RED), chain.request, DataSource.NETWORK)
                    } else {
                        ErrorResult(null, chain.request, IOException("Image unavailable"))
                    }
                })
            }
            .build()
        SingletonImageLoader.setUnsafe(loader)
    }

    @After fun tearDown() {
        responses.values.forEach { it.cancel() }
        SingletonImageLoader.reset()
        loader.shutdown()
    }

    @Test fun nullAndBlankPaths_showFallbackWithoutRequests() {
        val path = mutableStateOf<String?>(null)
        compose.setContent { TmdbImage(path.value, Modifier.size(100.dp)) }
        listOf(null, "", " ", "\t\n").forEach { value ->
            compose.runOnIdle { path.value = value }
            compose.onNodeWithTag("movie_image_missing").assertIsDisplayed()
            compose.onNodeWithTag("movie_image_loading").assertDoesNotExist()
        }
        assertEquals("Missing paths must not start image requests", 0, requests.get())
    }

    @Test fun malformedUrl_showsFallback() {
        compose.setContent { TmdbImage("https://[invalid", Modifier.size(100.dp)) }
        awaitTag("movie_image_missing")
        compose.onNodeWithTag("movie_image_loading").assertDoesNotExist()
    }

    @Test fun loadingFailure_replacesPulseWithFallback() {
        val response = pending("broken")
        compose.setContent { MovieImage("broken", Modifier.size(100.dp)) }
        awaitTag("movie_image_loading")
        response.complete(false)
        awaitTag("movie_image_missing")
        compose.onNodeWithTag("movie_image_loading").assertDoesNotExist()
    }

    @Test fun changedModel_resetsErrorAndLoading_thenRemovesOverlayOnSuccess() {
        val failed = pending("first")
        val success = pending("second")
        val next = pending("third")
        val model = mutableStateOf<String?>("first")
        compose.setContent { MovieImage(model.value, Modifier.size(100.dp)) }
        failed.complete(false)
        awaitTag("movie_image_missing")

        compose.runOnIdle { model.value = "second" }
        awaitTag("movie_image_loading")
        compose.onNodeWithTag("movie_image_missing").assertDoesNotExist()
        success.complete(true)
        awaitNoOverlay()

        compose.runOnIdle { model.value = "third" }
        awaitTag("movie_image_loading")
        next.complete(false)
        awaitTag("movie_image_missing")
        compose.runOnIdle { model.value = null }
        compose.onNodeWithTag("movie_image_missing").assertIsDisplayed()
    }

    @Test fun oldRequestCompletion_doesNotReplaceNewModelsLoadingState() {
        val old = pending("old")
        val current = pending("current")
        val model = mutableStateOf("old")
        compose.setContent { MovieImage(model.value, Modifier.size(100.dp)) }
        awaitTag("movie_image_loading")
        compose.runOnIdle { model.value = "current" }
        old.complete(false)
        awaitTag("movie_image_loading")
        compose.onNodeWithTag("movie_image_missing").assertDoesNotExist()
        current.complete(true)
        awaitNoOverlay()
    }

    private fun pending(model: String) = CompletableDeferred<Boolean>().also { responses[model] = it }

    private fun awaitTag(tag: String) {
        compose.waitUntil(5_000) { compose.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun awaitNoOverlay() {
        compose.waitUntil(5_000) {
            listOf("movie_image_missing", "movie_image_loading").all {
                compose.onAllNodesWithTag(it).fetchSemanticsNodes().isEmpty()
            }
        }
    }
}
