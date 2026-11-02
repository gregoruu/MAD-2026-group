package ee.movara.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ee.movara.app.R

// Typography scale from docs/design-specs.md section 2.
//
// Google Fonts ships Barlow Condensed as static weight files but Inter only as a variable font,
// so the Inter weights are instantiated from its wght axis. Both fonts are under the SIL Open
// Font License; the licence texts ship with the app in res/raw/ofl_barlow_condensed.txt and
// res/raw/ofl_inter.txt.

val BarlowCondensed =
  FontFamily(
    Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlow_condensed_bold, FontWeight.Bold),
    Font(R.font.barlow_condensed_extrabold, FontWeight.ExtraBold),
  )

private fun interFont(weight: FontWeight) =
  Font(
    R.font.inter_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
  )

val Inter =
  FontFamily(
    interFont(FontWeight.Normal),
    interFont(FontWeight.Medium),
    interFont(FontWeight.SemiBold),
  )

// The spec names ten styles; Material 3 has fifteen slots. Every slot is filled so that no
// component silently falls back to the system font.
//
// Four slots carry a spec style unchanged, because the spec names one headline, one body and
// one label where Material has three of each: headlineLarge and headlineSmall repeat the
// headline, bodyMedium repeats body, and labelSmall repeats label.
//
// Two slots are NOT in the spec at all and are interpolated from the styles around them,
// marked "not in the spec" below: displaySmall and bodySmall. The Design Lead should confirm
// those two before the components ticket (#31) starts using them.

val Typography =
  Typography(
    // Display large: movie title on Movie Details, Spotlight title.
    // The spec also asks for 60 sp on larger screens; that belongs to the responsive ticket (#45).
    displayLarge =
      TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 48.sp,
        lineHeight = 48.sp,
      ),
    // Display medium: screen titles.
    displayMedium =
      TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
      ),
    // Not in the spec: sits between display medium and headline.
    displaySmall =
      TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
      ),
    // Headline: recommendation rail titles. The spec names one headline, so all three Material
    // headline slots carry it.
    headlineLarge = headlineStyle(),
    headlineMedium = headlineStyle(),
    headlineSmall = headlineStyle(),
    // Title large: section titles (Synopsis, Cast), genre tiles.
    titleLarge =
      TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
      ),
    // Title medium: My Movies row titles, "Movara" brand.
    titleMedium =
      TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 22.sp,
      ),
    // Title small: movie card titles.
    titleSmall =
      TextStyle(
        fontFamily = BarlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
      ),
    // Body: normal text.
    bodyLarge = bodyStyle(),
    bodyMedium = bodyStyle(),
    // Not in the spec: the spec's smallest size is 12 sp, so body small uses it.
    bodySmall =
      TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
      ),
    // Label large: action buttons.
    labelLarge =
      TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
      ),
    // Label: tabs, pills, badges, navigation labels. The spec shows these uppercase; Compose has
    // no text transform, so callers uppercase the string themselves.
    labelMedium = labelStyle(),
    labelSmall = labelStyle(),
  )

private fun headlineStyle() =
  TextStyle(
    fontFamily = BarlowCondensed,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 24.sp,
  )

private fun bodyStyle() =
  TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  )

private fun labelStyle() =
  TextStyle(
    fontFamily = Inter,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
  )

/** Styles from the spec that Material 3 has no slot for. */
object MovaraTextStyles {

  /** Body relaxed: movie descriptions, where the extra leading helps longer text. */
  val bodyRelaxed =
    TextStyle(
      fontFamily = Inter,
      fontWeight = FontWeight.Normal,
      fontSize = 14.sp,
      lineHeight = 23.sp,
    )
}
