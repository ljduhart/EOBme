package app.eob.me.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import app.eob.me.ui.components.EobSplashLogo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun EobSplashScreen(modifier: Modifier = Modifier, onSplashComplete: () -> Unit) {
    val splashAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Main) {
            delay(4_000)
            splashAlpha.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
            )
            onSplashComplete()
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        EobSplashLogo(
            modifier = Modifier
                .fillMaxWidth(0.76f)
                .aspectRatio(1f)
                .graphicsLayer { alpha = splashAlpha.value }
        )
    }
}
