
package ee.movara.app.ui.about

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ee.movara.app.R
import ee.movara.app.ui.layout.LocalScreenLayout
import ee.movara.app.ui.theme.*

//Displays information about Movara, team members, TMDB attribution and third-party licenses.

@Composable
fun AboutScreen(
    onBack: () -> Unit
) {
    val screenMargin = LocalScreenLayout.current.screenMargin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MovaraBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = screenMargin)
    ) {
        // Screen header with back navigation.
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
                    contentDescription = stringResource(
                        R.string.about_back
                    ),
                    tint = MovaraText
                )
            }

            Spacer(Modifier.width(8.dp))

            Text(
                text = stringResource(R.string.about_title),
                style = MaterialTheme.typography.titleLarge,
                color = MovaraText
            )
        }

        Spacer(Modifier.height(24.dp))

        // Application info
        Text(
            text = stringResource(R.string.about_app_name),
            style = MaterialTheme.typography.displayLarge,
            color = MovaraGold
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = MovaraText
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.about_description),
            style = MaterialTheme.typography.bodyMedium,
            color = MovaraTextSecondary
        )

        SectionDivider()

        // TMDB attribution required by the API terms
        SectionTitle(
            stringResource(R.string.about_movie_data)
        )

        Spacer(Modifier.height(12.dp))

        // The official TMDB logo is stored locally.
        Image(
            painter = painterResource(R.drawable.tmdb_logo),
            contentDescription = stringResource(
                R.string.about_tmdb_logo_description
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(144.dp)
                .height(104.dp)
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.about_tmdb_notice),
            style = MaterialTheme.typography.bodyLarge,
            color = MovaraText
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.about_tmdb_source),
            style = MaterialTheme.typography.bodyMedium,
            color = MovaraTextSecondary
        )

        CreditLink(
            label = stringResource(R.string.about_tmdb_link),
            url = "https://www.themoviedb.org"
        )

        SectionDivider()

        // Team.
        SectionTitle(
            stringResource(R.string.about_team_title)
        )

        Spacer(Modifier.height(16.dp))

        val members = listOf(
            R.string.about_team_marten,
            R.string.about_team_kristo,
            R.string.about_team_gregor,
            R.string.about_team_sander,
            R.string.about_team_anna
        )

        members.forEach { member ->
            Text(
                text = stringResource(member),
                style = MaterialTheme.typography.bodyLarge,
                color = MovaraText,
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }

        SectionDivider()

        // Third-party resources and licenses
        SectionTitle(
            stringResource(R.string.about_licenses_title)
        )

        Spacer(Modifier.height(16.dp))

        // Font license
        CreditItem(
            title = stringResource(R.string.about_fonts_title),
            description = stringResource(
                R.string.about_fonts_description
            )
        )

        CreditLink(
            label = stringResource(R.string.about_fonts_link),
            url = "https://openfontlicense.org/open-font-license-official-text/"
        )

        Spacer(Modifier.height(16.dp))

        // Icon license
        CreditItem(
            title = stringResource(R.string.about_icons_title),
            description = stringResource(
                R.string.about_icons_description
            )
        )

        CreditLink(
            label = stringResource(R.string.about_icons_link),
            url = "https://www.apache.org/licenses/LICENSE-2.0"
        )

        Spacer(Modifier.height(16.dp))

        // Genre image license
        CreditItem(
            title = stringResource(R.string.about_unsplash_title),
            description = stringResource(
                R.string.about_unsplash_description
            )
        )

        CreditLink(
            label = stringResource(R.string.about_unsplash_link),
            url = "https://unsplash.com/license"
        )

        Spacer(Modifier.height(16.dp))

        // Official TMDB logo info
        CreditItem(
            title = stringResource(R.string.about_logo_title),
            description = stringResource(
                R.string.about_logo_description
            )
        )

        CreditLink(
            label = stringResource(R.string.about_logo_link),
            url = "https://www.themoviedb.org/about/logos-attribution"
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
