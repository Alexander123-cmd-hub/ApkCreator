package de.roboticmind.apkcreator.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.roboticmind.apkcreator.R
import de.roboticmind.apkcreator.core.data.BuildProfile
import de.roboticmind.apkcreator.core.data.BuildProfileRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuildProfileScreen(modifier: Modifier = Modifier) {
    val profiles = remember { BuildProfileRepository().profiles() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            items(profiles, key = BuildProfile::id) { profile ->
                BuildProfileCard(profile)
            }
        }
    }
}

@Composable
private fun BuildProfileCard(profile: BuildProfile) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = profile.name, style = MaterialTheme.typography.headlineSmall)
            Text(text = profile.applicationId, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(
                    R.string.profile_details,
                    profile.versionName,
                    profile.minSdk,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = stringResource(
                    if (profile.signed) R.string.profile_signed else R.string.profile_unsigned,
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
