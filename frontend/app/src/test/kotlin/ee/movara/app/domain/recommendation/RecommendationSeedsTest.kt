package ee.movara.app.domain.recommendation

import ee.movara.app.domain.model.Movie
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class RecommendationSeedsTest {
  private val today = LocalDate.of(2026, 11, 9)
  private fun movies(count: Int) = (1..count).map { Movie(it, "Movie $it", null, 2020) }

  @Test fun historiesOfAtMostTenUseEveryLikeWithoutDuplicates() {
    for (count in listOf(0, 1, 3, 5, 7, 10)) {
      val likes = movies(count)
      val selected = RecommendationSeeds.selectLikes(likes + likes, today)
      assertEquals(likes.map { it.id }.toSet(), selected.map { it.id }.toSet())
      assertEquals(count, selected.size)
      assertEquals(likes.take(5), selected.take(5))
    }
  }

  @Test fun largeHistoriesKeepFiveRecentAndRotateFiveOlderWithAStableDailyChoice() {
    val likes = movies(30)
    val selected = RecommendationSeeds.selectLikes(likes, today)
    val tomorrow = RecommendationSeeds.selectLikes(likes, today.plusDays(1))
    assertEquals(likes.take(5), selected.take(5))
    assertEquals(likes.take(5), tomorrow.take(5))
    assertEquals(10, selected.size)
    assertEquals(10, selected.map { it.id }.toSet().size)
    assertEquals(selected, RecommendationSeeds.selectLikes(likes, today))
    assertNotEquals(selected.drop(5), tomorrow.drop(5))
    // Reordering the older rows does not change the day's selection.
    assertEquals(selected, RecommendationSeeds.selectLikes(likes.take(5) + likes.drop(5).reversed(), today))
  }

  @Test fun rotationEventuallyIncludesEveryOlderLikeEvenWhenTheCountIsNotDivisibleByFive() {
    for (count in listOf(11, 12, 20, 30, 32)) {
      val likes = movies(count)
      val selectedIds = (0L until count.toLong()).flatMap { day ->
        RecommendationSeeds.selectLikes(likes, today.plusDays(day)).map { it.id }
      }.toSet()
      assertEquals(likes.map { it.id }.toSet(), selectedIds)
    }
  }

  @Test fun removingAnOlderLikeExcludesItFromEveryFollowingSelection() {
    val likes = movies(30).filterNot { it.id == 20 }
    for (day in 0L..10L) {
      assertFalse(RecommendationSeeds.selectLikes(likes, today.plusDays(day)).any { it.id == 20 })
    }
  }
}
