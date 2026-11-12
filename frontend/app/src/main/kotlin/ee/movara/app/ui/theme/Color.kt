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

import androidx.compose.ui.graphics.Color

// Palette from docs/design-specs.md.
val MovaraBackground = Color(0xFF07090F)
val MovaraText = Color(0xFFF0EEE8)
val MovaraTextSecondary = Color(0xFFA8A4B0)
val MovaraSurface = Color(0xFF0E1120)
val MovaraGold = Color(0xFFF0B429)
val MovaraLiked = Color(0xFFE63946)
val MovaraWatched = Color(0xFF3A8F5F)
val MovaraBorder = Color(0xFF1C1F30)
val MovaraSurfaceSecondary = Color(0xFF161929)

// Behind text drawn on an image. Strong enough for AA contrast even on a white image;
// ContrastTest checks the colour itself. Modifier.textScrim puts the text on it.
val MovaraScrim = MovaraBackground.copy(alpha = 0.85f)
