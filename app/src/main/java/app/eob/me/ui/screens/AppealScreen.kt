package app.eob.me.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobRecord
import app.eob.me.data.EobStrings
import app.eob.me.data.UserProfile

private val BrandBlue = Color(0xFF2498EA)

@Composable
fun AppealScreen(
    language: AppLanguage,
    profile: UserProfile,
    selectedRecord: EobRecord?,
    appealLetter: String,
    appealLetterEditingEnabled: Boolean,
    onRegenerate: () -> Unit,
    onEditLetter: (String) -> Unit,
    onEnableEditing: () -> Unit,
    onSaveLetter: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = EobStrings.t(language, "appealGeneratorTitle"),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        if (selectedRecord == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "📋", fontSize = 48.sp)
                    Text(
                        text = EobStrings.t(language, "appealSelectClaimHint"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = "📄", fontSize = 20.sp)
                    Column {
                        Text(
                            text = EobStrings.tf(language, "appealingProvider", selectedRecord.providerName),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = EobStrings.tf(language, "appealServiceDate", selectedRecord.serviceDate),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                OutlinedTextField(
                    value = appealLetter,
                    onValueChange = onEditLetter,
                    readOnly = !appealLetterEditingEnabled,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    textStyle = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Serif,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.Black
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlue,
                        unfocusedBorderColor = Color.LightGray,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    placeholder = { Text(EobStrings.t(language, "appealDraftPlaceholder")) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRegenerate,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text("🔄 ${EobStrings.t(language, "appealRegenerate")}")
                }

                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(
                            ClipData.newPlainText(EobStrings.t(language, "appealLetter"), appealLetter)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = appealLetter.isNotBlank()
                ) {
                    Text("📋 ${EobStrings.t(language, "appealCopy")}")
                }

                IconButton(
                    onClick = { /* Simulated share/export */ },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Text("📤")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onEnableEditing,
                    modifier = Modifier.weight(1f),
                    enabled = !appealLetterEditingEnabled && appealLetter.isNotBlank()
                ) {
                    Text(EobStrings.t(language, "appealEditLetter"))
                }
                Button(
                    onClick = onSaveLetter,
                    modifier = Modifier.weight(1f),
                    enabled = appealLetterEditingEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue)
                ) {
                    Text(EobStrings.t(language, "appealSaveLetter"))
                }
            }
        }
    }
}
