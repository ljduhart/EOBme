package app.eob.me.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage

@Composable
fun LanguageScreen(modifier: Modifier = Modifier, onSelected: (AppLanguage) -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("EOBme", style = MaterialTheme.typography.displaySmall)
        Text("Select a language / Seleccione idioma / Choisissez la langue / 选择语言")
        Spacer(Modifier.height(24.dp))
        AppLanguage.entries.forEach { option ->
            Button(
                onClick = { onSelected(option) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Text(option.displayName)
            }
        }
    }
}
