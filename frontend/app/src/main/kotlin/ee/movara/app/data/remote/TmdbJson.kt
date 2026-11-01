package ee.movara.app.data.remote

import kotlinx.serialization.json.Json

/**
 * How TMDB responses are parsed.
 *
 * `ignoreUnknownKeys` is on because TMDB sends far more fields than the app reads and
 * adds new ones over time; without it every added field would break the app.
 *
 * `coerceInputValues` is on because TMDB sometimes sends an explicit null where the
 * data classes above declare a list. With coercion the field falls back to its default
 * empty list instead of failing the whole response.
 *
 * Neither setting rescues a field of the wrong type. `"id": "invalid"` where an integer
 * is declared fails the whole response, because the parser gives up before the mappers
 * get a chance to drop the bad entry. TMDB does not send responses like that; if one
 * ever arrives, the request fails and the screen shows its error state.
 *
 * The API service builds its converter from this instance and the model tests parse
 * with it, so what the tests prove is what the app does.
 */
val tmdbJson: Json = Json {
  ignoreUnknownKeys = true
  coerceInputValues = true
}
