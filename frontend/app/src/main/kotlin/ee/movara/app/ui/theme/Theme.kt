/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ee.movara.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MovaraColorScheme = darkColorScheme(
    primary = MovaraGold,
    onPrimary = MovaraBackground,
    primaryContainer = MovaraSurface,
    onPrimaryContainer = MovaraGold,
    secondary = MovaraTextSecondary,
    onSecondary = MovaraBackground,
    secondaryContainer = MovaraSurfaceSecondary,
    onSecondaryContainer = MovaraText,
    background = MovaraBackground,
    onBackground = MovaraText,
    surface = MovaraBackground,
    onSurface = MovaraText,
    surfaceVariant = MovaraSurface,
    onSurfaceVariant = MovaraTextSecondary,
    surfaceTint = MovaraGold,
    surfaceDim = MovaraBackground,
    surfaceBright = MovaraSurfaceSecondary,
    surfaceContainerLowest = MovaraBackground,
    surfaceContainerLow = MovaraSurface,
    surfaceContainer = MovaraSurface,
    surfaceContainerHigh = MovaraSurfaceSecondary,
    surfaceContainerHighest = MovaraSurfaceSecondary,
    outline = MovaraBorder,
    outlineVariant = MovaraBorder,
)

@Composable
fun MovaraTheme(content: @Composable () -> Unit) {
    // Movara always uses the specified dark palette, regardless of system theme.
    MaterialTheme(
        colorScheme = MovaraColorScheme,
        typography = Typography,
        content = content,
    )
}
