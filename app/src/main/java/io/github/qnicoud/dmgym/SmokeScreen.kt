package io.github.qnicoud.dmgym

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.qnicoud.dmgym.ui.theme.DmGymTheme

@Composable
fun SmokeScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = stringResource(R.string.smoke_title), style = MaterialTheme.typography.displayMedium)
            Text(text = stringResource(R.string.smoke_subtitle), style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Preview(name = "Clair")
@Preview(name = "Sombre", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SmokeScreenPreview() {
    DmGymTheme { SmokeScreen() }
}
