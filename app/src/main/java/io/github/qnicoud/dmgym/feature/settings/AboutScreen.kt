package io.github.qnicoud.dmgym.feature.settings

import android.content.Context
import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.R
import io.github.qnicoud.dmgym.ui.icons.DmIcons
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

object AboutTags {
    const val VERSION = "about_version"
    const val LICENSES = "about_licenses"
}

/** Version lue dans le paquet installé ; chaîne vide si le système ne la fournit pas. */
@Composable
fun AboutRoute(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version = remember(context) { context.versionName() }
    AboutScreen(version, onBack, modifier)
}

private fun Context.versionName(): String =
    runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull().orEmpty()

/** À propos et licences (S12) : version, polices embarquées (OFL) et bip de fin (CC0). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    version: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title), Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(DmIcons.Back, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.about_version, version),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag(AboutTags.VERSION),
            )
            Text(stringResource(R.string.about_intro), style = MaterialTheme.typography.bodyLarge)
            Text(
                text = stringResource(R.string.about_licenses_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Column(
                Modifier.testTag(AboutTags.LICENSES),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                listOf(
                    R.string.about_font_alegreya,
                    R.string.about_font_atkinson,
                    R.string.about_ofl_note,
                    R.string.about_beep,
                ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
            }
        }
    }
}

@Preview(name = "Clair", showBackground = true)
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun AboutScreenPreview() {
    DmGymTheme { AboutScreen(version = "0.1.0", onBack = {}) }
}
