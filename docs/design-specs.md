# Movara – Design system and developer specifications

This file collects the Movara design system and the specifications the Build team needs to implement the app in Kotlin, Jetpack Compose and Material 3. All sizes are in dp and all text sizes in sp.

---

## 1. Colour palette

Movara has a dark theme with gold as the main colour.

| Colour | Hex | Material 3 role | Used for |
|---|---|---|---|
| Near-black | `#07090F` | background, surface | Screen background |
| Off-white | `#F0EEE8` | onBackground, onSurface | Main text |
| Light grey | `#A8A4B0` | onSurfaceVariant | Years, descriptions, inactive navigation |
| Dark navy | `#0E1120` | surfaceContainer | Cards, search field, tabs |
| Gold | `#F0B429` | primary | Selected items, main buttons, ratings |
| Red | `#E63946` | custom | Liked |
| Green | `#3A8F5F` | custom | Watched |

Supporting colours:

| Colour | Hex | Used for |
|---|---|---|
| Border | `#1C1F30` | Borders of cards, fields, tabs and chips |
| Secondary surface | `#161929` | Inactive action buttons, image placeholders, snackbar |

### Contrast (WCAG 2.1 level AA)

WCAG AA requires at least 4.5:1 for normal text and 3:1 for large text, buttons and icons.

| Text on background | Contrast | Result |
|---|---|---|
| Off-white on near-black | 17.2:1 | Pass |
| Light grey on near-black | 8.2:1 | Pass |
| Gold on near-black | 10.7:1 | Pass |
| Near-black text on gold button | 10.7:1 | Pass |
| Near-black text on red button | 4.8:1 | Pass |
| Near-black text on green button | 5.0:1 | Pass |

The gold, red and green buttons always use dark text (`#07090F`).

---

## 2. Typography scale

Two Google Fonts (SIL Open Font License): **Barlow Condensed** for titles (weights 600, 700, 800) and **Inter** for all other text (weights 400, 500, 600). The smallest text in the app is 12 sp.

| Style | Font | Size / line height | Weight | Used for |
|---|---|---|---|---|
| Display large | Barlow Condensed | 48 / 48 sp (60 sp on larger screens) | 800 | Movie title on Movie Details, Spotlight title |
| Display medium | Barlow Condensed | 36 / 40 sp | 800 | Screen titles |
| Headline | Barlow Condensed | 24 / 24 sp | 700 | Recommendation rail titles |
| Title large | Barlow Condensed | 20 / 28 sp | 700 | Section titles (Synopsis, Cast), genre tiles |
| Title medium | Barlow Condensed | 18 / 22 sp | 700 | My Movies row titles, "Movara" brand |
| Title small | Barlow Condensed | 14 / 18 sp | 600 | Movie card titles |
| Body | Inter | 14 / 20 sp | 400 | Normal text |
| Body relaxed | Inter | 14 / 23 sp | 400 | Movie description |
| Label large | Inter | 14 / 20 sp | 500 | Action buttons |
| Label | Inter | 12 / 16 sp, uppercase | 600 | Tabs, pills, badges, navigation labels |

---

## 3. Spacing, shapes and sizes

**Spacing scale (multiples of 4 dp):** 4, 8, 12, 16, 20, 24, 40 dp.

| Use | Value |
|---|---|
| Screen margins | 20 dp on phones, 24 dp on larger screens |
| Gap between movie cards | 12 dp (rails, genre tiles), 16 dp (movie grids) |
| Gap between chips / action buttons | 8 dp / 12 dp |
| Gap between sections on Home | 40 dp |

**Corner radius:** 8 dp (posters, tabs), 12 dp (cards, genre tiles, rows), 16 dp (search field), 24 dp (Spotlight), full (pills, round buttons).

**Borders:** 1 dp, `#1C1F30`. **Shadows:** none.

| Element | Size |
|---|---|
| Bottom navigation bar | 80 dp high |
| Navigation rail (larger screens) | 80 dp wide |
| Buttons, tabs and list rows | at least 48 dp high |
| Touch area of every clickable element | at least 48 × 48 dp |
| Search field | 52 dp high |
| Spotlight card on Home | 62 % of the screen height |
| Movie cards in recommendation rails | 144 × 208 dp |
| Genre tiles | 112 dp high |
| Posters in My Movies | 64 × 96 dp |

---

## 4. Icon set

Material Symbols (Apache License 2.0), added in Android Studio with **Vector Asset** as vector drawables (XML), so one file works on every screen density.

| Icon | Material Symbol | Size |
|---|---|---|
| Search | `search` | 20 dp |
| Clear | `close` | 20 dp |
| Back | `arrow_back` | 20 dp |
| Rating | `star` (filled) | 12–16 dp |
| Like | `favorite` (outline / filled) | 16–20 dp |
| Watch Later | `bookmark` (outline / filled) | 16–20 dp |
| Mark Watched | `visibility` | 16 dp |
| Done | `check` | 12–24 dp |
| Home tab | `home` | 24 dp |
| Genres tab | `style` | 24 dp |
| My Movies tab | `person` | 24 dp |
| About | `info` | 24 dp |
| Missing image | `movie` | 32 dp |
| No search results | `sentiment_dissatisfied` | 32 dp |

---

## 5. Component library

| Component | Compose component | Size | States |
|---|---|---|---|
| Bottom navigation bar | `NavigationBar` | 80 dp high, 3 items | Selected: gold filled icon, gold label, small gold dot. Unselected: light grey outline icon and label |
| Navigation rail | `NavigationRail` | 80 dp wide | Same as bottom navigation bar |
| Search field | `TextField` | 52 dp high, radius 16 | Empty: placeholder "Search movies by title". Focused: gold border. With text: clear button |
| Spotlight card | `Card` | 62 % of screen height, radius 24 | Pressed: ripple |
| Movie card | `Card` | Poster 2:3, radius 8–12 | Pressed: ripple. Missing poster: dark box with movie icon |
| Genre tile | `Card` | 112 dp high, radius 12 | Pressed: ripple |
| Tabs (My Movies) | `SegmentedButton` | 48 dp high | Selected: gold background, dark text. Unselected: light grey text. Shows the number of movies |
| Movie row (My Movies) | `Card` | Full width, poster 64 × 96 | Pressed: ripple |
| Action buttons (Like, Watch Later, Mark Watched) | `FilledTonalButton` | 48 dp high, pill | Off: dark navy, light grey text, outline icon. On: Like red, Watch Later gold, Watched green, dark text, filled icon |
| Icon buttons (search, back, info) | `IconButton` | 48 dp touch area | Pressed: ripple |
| Snackbar | `Snackbar` | Material 3 default | Shown after adding or removing a movie; "Undo" when a movie is removed |
| Loading placeholder | custom | Same size as the element | Grey shape that slowly pulses |

The app does not use dialogs. Confirmations are shown as snackbars, so the user is never interrupted.

---

## 6. Layouts for two screen sizes

Main target: **Compact (412 × 917 dp)**, phones in portrait. Also designed: **Medium (700 × 840 dp)**, foldable phones and small tablets.

| Element | Compact | Medium |
|---|---|---|
| Navigation | Bottom navigation bar | Navigation rail on the left |
| Screen margins | 20 dp | 24 dp |
| Search and genre movie grids | 3 columns | 5 columns |
| Genre tiles | 2 columns | 3 columns |
| My Movies list | 1 column | 2 columns |
| Spotlight card on Home | Tall card | Wide card with the poster on the left |
| Movie Details | One column | Poster on the left, details on the right |

---

## 7. Assets

- **Movie images:** loaded from TMDB (`https://image.tmdb.org/t/p/{size}{path}`). Posters on cards `w342`, Spotlight poster `w500`, Movie Details background `w780`.
- **Genre images:** one image per genre chosen by the team, from Unsplash (free to use), added to the app as WebP, about 800 × 500 px, with a dark overlay so the white genre name is easy to read.
- **Fonts:** Barlow Condensed and Inter, added to the app as font files.
- **App icon:** Android adaptive icon (108 × 108 dp), created with Android Studio's Image Asset tool, which makes the files for all screen densities.
- **TMDB logo:** shown on the About screen together with the sentence "This product uses the TMDB API but is not endorsed or certified by TMDB."

---

## 8. Interaction and animation notes

- **Home:** tapping the Spotlight card, "See Details" or a movie in a rail opens Movie Details. The heart and bookmark buttons add the movie to Liked or Watch Later right away. The search button opens Search.
- **Search:** results appear while the user types, after a 400 ms pause. The clear button empties the field.
- **Genres:** tapping a genre opens the movies of that genre. More movies load at the end of the list. "All Genres" or the Back button returns to the genre list.
- **Movie Details:** Like, Watch Later and Mark Watched can be turned on and off. Marking a movie as watched also removes it from Watch Later. Back returns to the previous screen at the same scroll position.
- **My Movies:** tabs switch between Liked, Watch Later and Watched. In Watch Later the user can mark a movie as watched or remove it. The lists are saved on the phone.
- **About:** opens from the info button on My Movies.
- **Feedback:** every change to a list shows a snackbar, for example "Added to Watch Later".
- **Loading / empty / error:** grey placeholder shapes while loading, a short message for empty lists, and a message with "Try again" when there is no internet. My Movies works offline.
- **Animations:** state colour changes take 200 ms, screens change with a short fade (Navigation Compose default), images fade in when loaded, and snackbars slide up and disappear after 4 seconds.

---

## 9. Data and content rules

- **Home:** Top Pick and up to five rails "Because you liked [movie]", based on the movies the user has liked (TMDB recommendations). Watched movies are not shown. With no liked movies, Home shows "Trending this week".
- **Search:** TMDB search by title, 20 results at a time, each with poster, title and year.
- **Genres:** the same 10 genres every time, each linked to a TMDB genre ID: Action 28, Drama 18, Sci-Fi 878, Thriller 53, Horror 27, Comedy 35, Romance 10749, Crime 80, Animation 16, Documentary 99.
- **Selected genre:** the most popular movies of the genre, 20 at a time.
- **Movie Details:** title, year, runtime, rating (one decimal, e.g. 7.6), genres, director, description and the first five actors. Missing description → "No description available". Missing runtime or cast → not shown.
- **My Movies:** stored on the phone (Room), most recently added first, with the number of movies on each tab.
- **Text length:** titles on cards and rows have at most two lines and end with "…". The full title is shown on Movie Details.

---

## 10. Accessibility

- Every clickable element has a touch area of at least 48 × 48 dp.
- All main colour pairs meet WCAG 2.1 AA (see the contrast table above).
- All text is in sp and follows the phone's font size setting; the smallest text is 12 sp. With a larger font, text wraps and rows grow instead of cutting text.
- Selected states are never shown only with colour: icons change from outline to filled and labels change, for example "Liked".
