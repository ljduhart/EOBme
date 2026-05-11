package com.eobme.app.ui.screens.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.eobme.app.R

private data class SupportSection(val title: String, val content: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreen(onBack: () -> Unit) {
    val sections = listOf(
        SupportSection(
            title = "How to Upload an EOB",
            content = "Tap the + button on the home screen. You can take a photo with your camera for the best results, or select an existing image from your photo library. EOBme will automatically scan and extract key details from your EOB document."
        ),
        SupportSection(
            title = "Understanding Analysis Results",
            content = "After uploading, EOBme displays a detailed breakdown including: Insurance Name, Provider Name, Billed Amount, Insurance Paid Amount, Contractual Adjustment, Copay, Deductible, and Coinsurance. The CPT codes found on the EOB are also listed with descriptions."
        ),
        SupportSection(
            title = "CPT Billing Tracker",
            content = "The CPT Tracking tab keeps a running count of all CPT codes billed per year. You can filter by category: Office Visits (OVs), Labs, Hospital, DME, and Injections. Each code shows how many times it was billed (e.g., 99215 (5x))."
        ),
        SupportSection(
            title = "Generating Appeal Letters",
            content = "From any Analysis Results page, tap 'Generate Appeal Letter'. If your profile information and the EOB provider details are complete, the letter is auto-filled. You can edit the letter using the edit button in the top right corner."
        ),
        SupportSection(
            title = "Insurance Card",
            content = "Your insurance card information appears at the top of the home screen. If you've entered your Subscriber ID in your profile, it will display there. You can update your insurance details in the Profile section."
        ),
        SupportSection(
            title = "Your Profile",
            content = "Tap the profile icon on the home screen to view and edit your profile. Your profile includes your name, email, city, state, subscriber ID, and language preference. Tap the edit icon to make changes, then tap save when done."
        ),
        SupportSection(
            title = "Language Settings",
            content = "EOBme supports English, Spanish, French, and Chinese. You can change your language at any time from your Profile settings. The language you choose at initial setup is used throughout the app."
        ),
        SupportSection(
            title = "News Feed",
            content = "The News tab on the home screen shows the latest insurance industry news from major carriers and regulatory agencies. Stay informed about policy changes, new benefits, and important updates that may affect your coverage."
        ),
        SupportSection(
            title = "Security & Privacy",
            content = "EOBme stores all your data locally on your device. No information is sent to external servers. The app will automatically log you out after 3 minutes of inactivity to protect your sensitive health information."
        ),
        SupportSection(
            title = "EOBme Features Summary",
            content = "• OCR scanning of EOB documents (camera & library)\n• Automatic insurance company recognition\n• Detailed financial analysis of each EOB\n• CPT code tracking by year and category\n• ICD-10 code recognition\n• Auto-generated appeal letters with editing\n• Insurance card / Subscriber ID display\n• Insurance industry news feed\n• Multi-language support (EN, ES, FR, ZH)\n• User profile with edit capability\n• EOB history ordered by date of service\n• Auto-logout after 3 minutes of inactivity"
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.support)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = stringResource(R.string.support_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.support_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(sections) { section ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = section.content,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
