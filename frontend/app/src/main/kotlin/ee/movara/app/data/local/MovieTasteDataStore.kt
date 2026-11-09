package ee.movara.app.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

/** One application-scoped store per file, shared by every taste-profile consumer. */
internal val Context.movieTasteDataStore by preferencesDataStore(name = "movie_taste")
