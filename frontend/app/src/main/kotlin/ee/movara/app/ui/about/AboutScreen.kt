
package ee.movara.app.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ee.movara.app.R
import ee.movara.app.ui.theme.*


//About screen containing the project information,team members and third-party attributions.

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaraBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        // Screen header with back button.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    painter = painterResource(
                        R.drawable.ic_ui_arrow_back
                    ),
                    contentDescription = "Back",
                    tint = MovaraText
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = "About",
                style = MaterialTheme.typography.titleLarge,
                color = MovaraText
            )
        }

        Spacer(Modifier.height(24.dp))

        // App info
        Text(
            text = "MOVARA",
            style = MaterialTheme.typography.displayLarge,
            color = MovaraGold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Discover your next favorite movie.",
            style = MaterialTheme.typography.bodyLarge,
            color = MovaraText
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "Movara helps you discover movies, " +
                    "find recommendations and organize " +
                    "your personal movie lists.",
            style = MaterialTheme.typography.bodyMedium,
            color = MovaraTextSecondary
        )

        SectionDivider()

        // TMDB attribution required by the API
        SectionTitle("MOVIE DATA")

        Spacer(Modifier.height(12.dp))

        AsyncImage(
            model = "https://thumb.wikimedia.org/" +
                    "wikipedia/commons/thumb/8/89/" +
                    "Tmdb.new.logo.svg/" +
                    "330px-Tmdb.new.logo.svg.png",
            contentDescription = "The Movie Database logo",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(144.dp)
                .height(104.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = "This product uses the TMDB API " +
                    "but is not endorsed or certified by TMDB.",
            style = MaterialTheme.typography.bodyLarge,
            color = MovaraText
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Movie information and artwork " +
                    "are provided by The Movie Database.",
            style = MaterialTheme.typography.bodyMedium,
            color = MovaraTextSecondary
        )

        CreditLink(
            label = "The Movie Database",
            url = "https://www.themoviedb.org"
        )

        SectionDivider()

        // Project team
        SectionTitle("TEAM MEMBERS")

        Spacer(Modifier.height(16.dp))

        listOf(
            "Marten Tiisler",
            "Kristo Kohtse",
            "Gregor Uustalu",
            "Sander Zirel",
            "Anna Liisa Nurm"
        ).forEach { member ->
            Text(
                text = member,
                style = MaterialTheme.typography.bodyLarge,
                color = MovaraText,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        SectionDivider()

        // hird-party resources
        SectionTitle("LICENSES & CREDITS")

        Spacer(Modifier.height(16.dp))

        CreditItem(
            title = "Barlow Condensed & Inter",
            description = "Fonts — SIL Open Font License 1.1"
        )

        CreditLink(
            label = "View font license",
            url = "https://openfontlicense.org/" +
                    "open-font-license-official-text/"
        )

        Spacer(Modifier.height(16.dp))

        CreditItem(
            title = "Google Material Symbols",
            description = "Icons — Apache License 2.0"
        )

        CreditLink(
            label = "View icon license",
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        Spacer(Modifier.height(16.dp))

        CreditItem(
            title = "Unsplash",
            description = "Genre imagery — Unsplash License"
        )

        CreditLink(
            label = "View Unsplash license",
            url = "https://unsplash.com/license"
        )

        Spacer(Modifier.height(16.dp))

        CreditItem(
            title = "TMDB Logo",
            description = "Logo artwork by Travis Bell"
        )

        CreditLink(
            label = "Logo source and license",
            url = "https://commons.wikimedia.org/" +
                    "wiki/File:Tmdb.new.logo.svg"
        )

        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        color = MovaraGold
    )
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(28.dp))

    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MovaraBorder)
    )

    Spacer(Modifier.height(28.dp))
}
@Composable
private fun CreditItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MovaraText
        )

        Spacer(Modifier.height(4.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MovaraTextSecondary
        )
    }
}

@Composable
private fun CreditLink(
    label: String,
    url: String
) {
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable {
                uriHandler.openUri(url)
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MovaraGold
        )
    }
}


 //Preview of the About screen.

@Preview(
    showBackground = true,
    widthDp = 412,
    heightDp = 917
)
@Composable
private fun AboutScreenPreview() {
    MovaraTheme {
        AboutScreen(onBack = {})
    }
}
