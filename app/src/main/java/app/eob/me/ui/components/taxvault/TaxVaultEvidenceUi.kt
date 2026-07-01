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
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
private val EobTableHeaderBlue = Color(0xFFB3E5FC)
private val AddReceiptCyan = Color(0xFF00E5FF)
private val AddReceiptGlow = Color(0x9900E5FF)
private val IconInk = Color.Black

private val MiniatureCardWidth = 108.dp
private val MiniatureCardHeight = 148.dp
private val AddReceiptButtonWidth = 96.dp
private val AddReceiptButtonHeight = 108.dp

@Composable
fun VaultAddReceiptButton(
    language: AppLanguage,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(width = AddReceiptButtonWidth, height = AddReceiptButtonHeight)
            .vaultAddReceiptGlow()
            .clip(RoundedCornerShape(20.dp))
            .background(AddReceiptCyan)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .height(46.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                VaultAddReceiptDocumentCameraIcon(
                    modifier = Modifier.size(width = 40.dp, height = 44.dp)
                )
            }
            Text(
                text = EobStrings.t(language, "taxVaultAddReceipt"),
                color = IconInk,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        VaultSparkleAccent(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-5).dp, y = 5.dp)
        )
    }
}

@Composable
private fun VaultAddReceiptDocumentCameraIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(width = size.minDimension * 0.055f)
        val docWidth = size.width * 0.62f
        val docHeight = size.height * 0.78f
        val docLeft = size.width * 0.12f
        val docTop = size.height * 0.06f
        val cornerFold = docWidth * 0.22f

        val docPath = Path().apply {
            moveTo(docLeft, docTop)
            lineTo(docLeft + docWidth - cornerFold, docTop)
            lineTo(docLeft + docWidth, docTop + cornerFold)
            lineTo(docLeft + docWidth, docTop + docHeight)
            lineTo(docLeft, docTop + docHeight)
            close()
        }
        drawPath(docPath, color = Color.Transparent)
        drawPath(docPath, color = IconInk, style = stroke)

        val foldPath = Path().apply {
            moveTo(docLeft + docWidth - cornerFold, docTop)
            lineTo(docLeft + docWidth - cornerFold, docTop + cornerFold)
            lineTo(docLeft + docWidth, docTop + cornerFold)
        }
        drawPath(foldPath, color = IconInk, style = stroke)

        val lineStartX = docLeft + docWidth * 0.14f
        val lineEndX = docLeft + docWidth * 0.82f
        val lineYs = listOf(0.34f, 0.48f, 0.62f)
        lineYs.forEach { fraction ->
            val y = docTop + docHeight * fraction
            drawLine(
                color = IconInk,
                start = Offset(lineStartX, y),
                end = Offset(lineEndX, y),
                strokeWidth = stroke.width
            )
        }

        val cameraWidth = size.width * 0.42f
        val cameraHeight = size.height * 0.34f
        val cameraLeft = docLeft + docWidth - cameraWidth * 0.72f
        val cameraTop = docTop + docHeight - cameraHeight * 0.55f
        drawRoundRect(
            color = IconInk,
            topLeft = Offset(cameraLeft, cameraTop),
            size = Size(cameraWidth, cameraHeight),
            cornerRadius = CornerRadius(cameraHeight * 0.18f, cameraHeight * 0.18f),
            style = stroke
        )
        drawCircle(
            color = IconInk,
            radius = cameraHeight * 0.22f,
            center = Offset(cameraLeft + cameraWidth * 0.5f, cameraTop + cameraHeight * 0.52f),
            style = stroke
        )
        drawCircle(
            color = IconInk,
            radius = cameraHeight * 0.07f,
            center = Offset(cameraLeft + cameraWidth * 0.78f, cameraTop + cameraHeight * 0.24f),
            style = stroke
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
            .size(width = MiniatureCardWidth, height = MiniatureCardHeight)
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
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(EobHeaderBlue),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thumbnail.providerName.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 7.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MiniatureDetailLine(text = thumbnail.serviceDate)
            MiniatureDetailLine(text = thumbnail.amountDisplay)
            repeat(2) {
                MiniatureDetailLine(text = "........................")
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(EobTableHeaderBlue)
            )
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
        }
    }
}

@Composable
private fun MiniatureReceiptPolaroidBody(thumbnail: VaultEvidenceThumbnail) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(EobHeaderBlue),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Favorite,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(8.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = thumbnail.providerName.uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 7.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            MiniatureReceiptLine(text = thumbnail.serviceDate)
            repeat(3) {
                MiniatureReceiptLine(text = "ITEM ..............")
            }
            Spacer(modifier = Modifier.weight(1f))
            MiniatureReceiptLine(text = "TOTAL PAID", bold = true)
            Text(
                text = thumbnail.amountDisplay,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 7.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center
            )
        }
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
    Canvas(modifier = modifier.size(16.dp)) {
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
            18.dp.toPx(),
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
        20.dp.toPx(),
        20.dp.toPx(),
        paint
    )
}
