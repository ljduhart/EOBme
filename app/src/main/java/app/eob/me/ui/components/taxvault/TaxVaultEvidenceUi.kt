package app.eob.me.ui.components.taxvault

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.EobStrings
import app.eob.me.data.VaultEvidenceThumbnail

private val PolaroidFrame = Color.White
private val EobHeaderBlue = Color(0xFF1565C0)
private val EobTableBlue = Color(0xFFE3F2FD)
private val ReceiptAccentRed = Color(0xFFD32F2F)
private val AddReceiptCyan = Color(0xFF00E5FF)
private val AddReceiptGlow = Color(0x9900E5FF)

@Composable
fun VaultAddReceiptButton(
    language: AppLanguage,
    mirrored: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sparkleAlignment = if (mirrored) Alignment.BottomEnd else Alignment.BottomStart
    Box(
        modifier = modifier
            .size(width = 112.dp, height = 124.dp)
            .vaultAddReceiptGlow()
            .clip(RoundedCornerShape(22.dp))
            .background(AddReceiptCyan)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .height(52.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(34.dp)
                )
                Icon(
                    imageVector = Icons.Outlined.PhotoCamera,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 10.dp, y = 4.dp)
                )
            }
            Text(
                text = EobStrings.t(language, "taxVaultAddReceipt"),
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        VaultSparkleAccent(
            modifier = Modifier
                .align(sparkleAlignment)
                .offset(
                    x = if (mirrored) 6.dp else (-6).dp,
                    y = 6.dp
                )
        )
    }
}

@Composable
fun MiniaturePolaroidEvidenceCard(
    thumbnail: VaultEvidenceThumbnail,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(118.dp)
            .graphicsLayer { rotationZ = thumbnail.rotationDegrees }
            .shadow(8.dp, RoundedCornerShape(4.dp))
            .clip(RoundedCornerShape(4.dp))
            .background(PolaroidFrame)
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 8.dp)
    ) {
        if (thumbnail.isReceipt) {
            MiniatureReceiptPolaroidBody(thumbnail = thumbnail)
        } else {
            MiniatureEobPolaroidBody(thumbnail = thumbnail)
        }
    }
}

@Composable
private fun MiniatureEobPolaroidBody(thumbnail: VaultEvidenceThumbnail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(EobHeaderBlue)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thumbnail.providerName.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MiniatureDetailLine(text = thumbnail.serviceDate)
            MiniatureDetailLine(text = thumbnail.amountDisplay)
            repeat(2) {
                MiniatureDetailLine(text = "........................")
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(2.dp))
                .background(EobTableBlue)
                .border(0.5.dp, Color(0xFF90CAF9), RoundedCornerShape(2.dp))
                .padding(horizontal = 4.dp, vertical = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            val lines = thumbnail.chargePreviewLines.take(3)
            if (lines.isEmpty()) {
                repeat(3) { rowIndex ->
                    MiniatureTableRow(
                        left = "----",
                        right = "$0.00",
                        bold = rowIndex == 2
                    )
                }
            } else {
                lines.forEachIndexed { index, line ->
                    MiniatureTableRow(
                        left = line.code,
                        right = line.amount,
                        bold = index == lines.lastIndex
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun MiniatureReceiptPolaroidBody(thumbnail: VaultEvidenceThumbnail) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Favorite,
                contentDescription = null,
                tint = ReceiptAccentRed,
                modifier = Modifier.size(10.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = thumbnail.providerName.uppercase(),
                color = ReceiptAccentRed,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MiniatureReceiptLine(text = thumbnail.providerName, bold = true)
            MiniatureReceiptLine(text = thumbnail.serviceDate)
            repeat(3) {
                MiniatureReceiptLine(text = "ITEM ..............")
            }
            MiniatureReceiptLine(text = "TOTAL PAID", bold = true)
            Text(
                text = thumbnail.amountDisplay,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun MiniatureDetailLine(text: String) {
    Text(
        text = text,
        color = Color(0xFF455A64),
        fontSize = 6.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        fontFamily = FontFamily.Monospace
    )
}

@Composable
private fun MiniatureTableRow(
    left: String,
    right: String,
    bold: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = left,
            color = Color(0xFF263238),
            fontSize = 6.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = right,
            color = Color(0xFF263238),
            fontSize = 6.sp,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun MiniatureReceiptLine(text: String, bold: Boolean = false) {
    Text(
        text = text,
        color = Color.Black,
        fontSize = 6.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun VaultSparkleAccent(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(18.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val path = Path().apply {
            moveTo(center.x, center.y - size.height * 0.42f)
            lineTo(center.x + size.width * 0.08f, center.y - size.height * 0.08f)
            lineTo(center.x + size.width * 0.42f, center.y)
            lineTo(center.x + size.width * 0.08f, center.y + size.height * 0.08f)
            lineTo(center.x, center.y + size.height * 0.42f)
            lineTo(center.x - size.width * 0.08f, center.y + size.height * 0.08f)
            lineTo(center.x - size.width * 0.42f, center.y)
            lineTo(center.x - size.width * 0.08f, center.y - size.height * 0.08f)
            close()
        }
        drawPath(path, Color.White.copy(alpha = 0.92f))
        drawPath(path, Color.White.copy(alpha = 0.35f), style = Stroke(width = 1.2f))
    }
}

private fun Modifier.vaultAddReceiptGlow(): Modifier = drawBehind {
    val paint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
        color = AddReceiptGlow.toArgb()
        setShadowLayer(
            22.dp.toPx(),
            0f,
            0f,
            AddReceiptGlow.toArgb()
        )
    }
    drawContext.canvas.nativeCanvas.drawRoundRect(
        0f,
        0f,
        size.width,
        size.height,
        22.dp.toPx(),
        22.dp.toPx(),
        paint
    )
}
