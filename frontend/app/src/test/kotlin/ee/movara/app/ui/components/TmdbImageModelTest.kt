package ee.movara.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TmdbImageModelTest {
    @Test fun nullPath_hasNoModel() {
        assertNull(tmdbImageModel(null))
    }

    @Test fun blankPaths_haveNoModel() {
        listOf("", " ", "\t\n").forEach { assertNull(tmdbImageModel(it)) }
    }

    @Test fun relativePaths_areNormalizedAndTrimmed() {
        listOf("poster.jpg", "/poster.jpg", " /poster.jpg \n").forEach {
            assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", tmdbImageModel(it))
        }
    }

    @Test fun requestedSizes_areUsed() {
        listOf("w342", "w500", "w780").forEach {
            assertEquals("https://image.tmdb.org/t/p/$it/poster.jpg", tmdbImageModel("/poster.jpg", it))
        }
    }

    @Test fun absoluteUrls_arePreserved() {
        listOf("https://example.com/poster.jpg", "http://example.com/poster.jpg").forEach {
            assertEquals(it, tmdbImageModel(" $it ", "w780"))
        }
    }
}
