package app.eob.me.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.DocumentScanPipelineState
import app.eob.me.data.EobStrings
import app.eob.me.ui.theme.EobCyberAccent
import app.eob.me.ui.theme.EobCyberGlow

private val ProcessingBackdrop = Color(0xCC05070D)

@Composable
fun DocumentProcessingOverlay(
    language: AppLanguage,
    state: DocumentScanPipelineState,
    modifier: Modifier = Modifier
) {
    if (state !is DocumentScanPipelineState.OcrPreCheck &&
        state !is DocumentScanPipelineState.UploadingAndProcessing
    ) {
        return
    }

    val statusText = when (state) {
        DocumentScanPipelineState.OcrPreCheck ->
            EobStrings.t(language, "documentScanOcrPreCheck")
        DocumentScanPipelineState.UploadingAndProcessing ->
            EobStrings.t(language, "documentScanHybridProcessing")
        else -> EobStrings.t(language, "documentScanProcessing")
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ProcessingBackdrop),
        contentAlignment = Alignment.Center
    ) {
        DocumentLaserScanEffect(modifier = Modifier.fillMaxSize())
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 24.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = EobStrings.t(language, "documentScanProcessingTitle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.82f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun DocumentLaserScanEffect(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "documentProcessingLaser")
    val sweepProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "documentProcessingLaserProgress"
    )

    Canvas(modifier = modifier) {
        val laserY = size.height * sweepProgress
        val laserHalfHeight = size.height * 0.04f
        val gradient = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                EobCyberGlow.copy(alpha = 0.35f),
                EobCyberAccent.copy(alpha = 0.95f),
                EobCyberGlow.copy(alpha = 0.35f),
                Color.Transparent
            ),
            startY = laserY - laserHalfHeight,
            endY = laserY + laserHalfHeight
        )
        drawRect(
            brush = gradient,
            topLeft = Offset(0f, laserY - laserHalfHeight),
            size = androidx.compose.ui.geometry.Size(size.width, laserHalfHeight * 2f)
        )
        drawLine(
            color = EobCyberAccent,
            start = Offset(0f, laserY),
            end = Offset(size.width, laserY),
            strokeWidth = 2.dp.toPx()
        )
    }
}
