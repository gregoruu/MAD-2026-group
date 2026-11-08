
package ee.movara.app.domain.model
object GenreCatalog {

    val genres = listOf(
        Genre(28, "Action"),
        Genre(18, "Drama"),
        Genre(878, "Sci-Fi"),
        Genre(53, "Thriller"),
        Genre(27, "Horror"),
        Genre(35, "Comedy"),
        Genre(10749, "Romance"),
        Genre(80, "Crime"),
        Genre(16, "Animation"),
        Genre(99, "Documentary")
    )

    fun nameForId(id: Int): String {
        return genres.firstOrNull { it.id == id }?.name
            ?: "Movies"
    }
}
