package ee.movara.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Developer reference screen for the Movara design system, required by issue #24.
 *
 * It renders every colour, text style, corner radius and spacing step so that the implementation
 * can be compared against docs/design-specs.md side by side. It is not part of the user-facing
 * navigation.
 */
@Composable
fun ThemePreviewScreen(modifier: Modifier = Modifier) {
  Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
    Column(
      modifier =
        Modifier.safeDrawingPadding()
          .verticalScroll(rememberScrollState())
          .padding(MovaraSpacing.screenMargin),
      verticalArrangement = Arrangement.spacedBy(MovaraSpacing.section),
    ) {
      ColourSection()
      TypographySection()
      ShapeSection()
      SpacingSection()
    }
  }
}

@Composable
private fun ColourSection() {
  Column(verticalArrangement = Arrangement.spacedBy(MovaraSpacing.medium)) {
    SectionTitle("Colours")
    ColourRow("Near-black", "#07090F", MovaraBackground)
    ColourRow("Off-white", "#F0EEE8", MovaraText)
    ColourRow("Light grey", "#A8A4B0", MovaraTextSecondary)
    ColourRow("Dark navy", "#0E1120", MovaraSurface)
    ColourRow("Gold", "#F0B429", MovaraGold)
    ColourRow("Red (liked)", "#E63946", MovaraLiked)
    ColourRow("Green (watched)", "#3A8F5F", MovaraWatched)
    ColourRow("Border", "#1C1F30", MovaraBorder)
    ColourRow("Secondary surface", "#161929", MovaraSurfaceSecondary)
  }
}

@Composable
private fun ColourRow(name: String, hex: String, colour: Color) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MovaraSpacing.large),
  ) {
    Box(
      modifier =
        Modifier.size(MovaraSpacing.minimumTouchTarget)
          .background(colour, MovaraShapes.small)
          .border(1.dp, MovaraBorder, MovaraShapes.small)
    )
    Column {
      Text(name, style = MaterialTheme.typography.bodyLarge)
      Text(
        hex,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun TypographySection() {
  Column(verticalArrangement = Arrangement.spacedBy(MovaraSpacing.large)) {
    SectionTitle("Typography")
    val typography = MaterialTheme.typography
    TypeRow("Display large", "48 / 48", typography.displayLarge)
    TypeRow("Display medium", "36 / 40", typography.displayMedium)
    TypeRow("Display small", "28 / 32", typography.displaySmall)
    TypeRow("Headline", "24 / 24", typography.headlineMedium)
    TypeRow("Title large", "20 / 28", typography.titleLarge)
    TypeRow("Title medium", "18 / 22", typography.titleMedium)
    TypeRow("Title small", "14 / 18", typography.titleSmall)
    TypeRow("Body", "14 / 20", typography.bodyLarge)
    TypeRow("Body relaxed", "14 / 23", MovaraTextStyles.bodyRelaxed)
    TypeRow("Label large", "14 / 20", typography.labelLarge)
    TypeRow("Label", "12 / 16", typography.labelMedium, uppercase = true)
    TypeRow("Body small", "12 / 16", typography.bodySmall)

    // The rows above are the styles docs/design-specs.md names. The four below are the
    // remaining Material 3 slots, which carry a spec style so that no component falls
    // back to the system font. They are shown so this screen accounts for all fifteen.
    SubSectionTitle("Material slots that reuse a spec style")
    TypeRow("Headline large  (= Headline)", "24 / 24", typography.headlineLarge)
    TypeRow("Headline small  (= Headline)", "24 / 24", typography.headlineSmall)
    TypeRow("Body medium  (= Body)", "14 / 20", typography.bodyMedium)
    TypeRow("Label small  (= Label)", "12 / 16", typography.labelSmall, uppercase = true)
  }
}

@Composable
private fun TypeRow(
  name: String,
  size: String,
  style: TextStyle,
  uppercase: Boolean = false,
) {
  Column(verticalArrangement = Arrangement.spacedBy(MovaraSpacing.tiny)) {
    Text(
      "$name  ·  $size sp",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    val sample = "Fight Club 1999"
    Text(if (uppercase) sample.uppercase() else sample, style = style)
  }
}

@Composable
private fun ShapeSection() {
  Column(verticalArrangement = Arrangement.spacedBy(MovaraSpacing.medium)) {
    SectionTitle("Corner radii")
    Row(horizontalArrangement = Arrangement.spacedBy(MovaraSpacing.medium)) {
      ShapeSwatch("8", MovaraShapes.small)
      ShapeSwatch("12", MovaraShapes.medium)
      ShapeSwatch("16", MovaraShapes.large)
      ShapeSwatch("24", MovaraShapes.extraLarge)
      ShapeSwatch("Pill", PillShape)
    }
  }
}

@Composable
private fun ShapeSwatch(label: String, shape: Shape) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(MovaraSpacing.tiny),
  ) {
    Box(
      modifier =
        Modifier.size(56.dp)
          .background(MovaraSurface, shape)
          .border(1.dp, MovaraBorder, shape)
    )
    Text(
      label,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SpacingSection() {
  Column(verticalArrangement = Arrangement.spacedBy(MovaraSpacing.small)) {
    SectionTitle("Spacing")
    SpacingRow("tiny", MovaraSpacing.tiny)
    SpacingRow("small", MovaraSpacing.small)
    SpacingRow("medium", MovaraSpacing.medium)
    SpacingRow("large", MovaraSpacing.large)
    SpacingRow("screenMargin", MovaraSpacing.screenMargin)
    SpacingRow("screenMarginExpanded", MovaraSpacing.screenMarginExpanded)
    SpacingRow("section", MovaraSpacing.section)
    SpacingRow("minimumTouchTarget", MovaraSpacing.minimumTouchTarget)
  }
}

@Composable
private fun SpacingRow(name: String, value: Dp) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(MovaraSpacing.medium),
  ) {
    Box(modifier = Modifier.width(value).height(MovaraSpacing.small).background(MovaraGold))
    Text(
      "$name  ·  ${value.value.toInt()} dp",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SectionTitle(title: String) {
  Text(
    title,
    style = MaterialTheme.typography.displayMedium,
    modifier = Modifier.fillMaxWidth(),
  )
}

@Composable
private fun SubSectionTitle(title: String) {
  Text(
    title,
    style = MaterialTheme.typography.titleLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier = Modifier.fillMaxWidth(),
  )
}

@Preview(showBackground = true, showSystemUi = true, name = "Movara design system")
@Composable
private fun ThemePreviewScreenPreview() {
  MovaraTheme { ThemePreviewScreen() }
}
