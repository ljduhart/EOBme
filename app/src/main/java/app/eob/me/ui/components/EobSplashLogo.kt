package app.eob.me.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun EobSplashLogo(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cornerRadius = size.width * 0.21f
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2498EA), Color(0xFF0E45BE)),
                    start = Offset(size.width * 0.2f, 0f),
                    end = Offset(size.width * 0.75f, size.height)
                ),
                cornerRadius = CornerRadius(cornerRadius, cornerRadius)
            )

            val shadowWave = Path().apply {
                moveTo(0f, size.height * 0.70f)
                cubicTo(size.width * 0.20f, size.height * 0.58f, size.width * 0.38f, size.height * 0.62f, size.width * 0.55f, size.height * 0.66f)
                cubicTo(size.width * 0.72f, size.height * 0.70f, size.width * 0.89f, size.height * 0.66f, size.width, size.height * 0.45f)
                lineTo(size.width, size.height * 0.58f)
                cubicTo(size.width * 0.86f, size.height * 0.82f, size.width * 0.68f, size.height * 0.80f, size.width * 0.50f, size.height * 0.75f)
                cubicTo(size.width * 0.31f, size.height * 0.70f, size.width * 0.17f, size.height * 0.70f, 0f, size.height * 0.82f)
                close()
            }
            drawPath(
                path = shadowWave,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF2A91F1), Color(0xFF58BDF6)),
                    start = Offset(0f, size.height * 0.82f),
                    end = Offset(size.width, size.height * 0.54f)
                )
            )

            val lightWave = Path().apply {
                moveTo(0f, size.height * 0.60f)
                cubicTo(size.width * 0.20f, size.height * 0.49f, size.width * 0.35f, size.height * 0.55f, size.width * 0.53f, size.height * 0.57f)
                cubicTo(size.width * 0.73f, size.height * 0.60f, size.width * 0.88f, size.height * 0.56f, size.width, size.height * 0.40f)
                lineTo(size.width, size.height * 0.47f)
                cubicTo(size.width * 0.88f, size.height * 0.70f, size.width * 0.68f, size.height * 0.71f, size.width * 0.50f, size.height * 0.67f)
                cubicTo(size.width * 0.30f, size.height * 0.62f, size.width * 0.16f, size.height * 0.60f, 0f, size.height * 0.70f)
                close()
            }
            drawPath(
                path = lightWave,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF69B8F7), Color(0xFFD7F2FF)),
                    start = Offset(0f, size.height * 0.63f),
                    end = Offset(size.width, size.height * 0.52f)
                )
            )
        }

        Row(
            modifier = Modifier.offset(y = (-14).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "EOB",
                color = Color.White,
                fontSize = 54.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-2).sp
            )
            Text(
                modifier = Modifier.offset(x = (-2).dp, y = 4.dp),
                text = "me",
                color = Color(0xFF7DD4FF),
                fontSize = 52.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-3).sp
            )
        }
    }
}

fun eobAppBackgroundGradient(): Brush {
    return Brush.verticalGradient(
        colors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFEAF6FF),
            Color(0xFFD6ECFF)
        )
    )
}
