package app.eob.me.ui.components.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import app.eob.me.R
import app.eob.me.ui.theme.EobCyberBackground
import app.eob.me.ui.theme.EobHomeGradientBase
import app.eob.me.ui.theme.EobHomeGradientDeep
import app.eob.me.ui.theme.EobHomeGradientMid
import app.eob.me.ui.theme.EobHomeGradientSurface
import app.eob.me.ui.theme.EobHomeLightGradientBase
import app.eob.me.ui.theme.EobHomeLightGradientMid
import app.eob.me.ui.theme.EobHomeLightGradientTop

private val DarkHomeScrimGradient = Brush.verticalGradient(
    colors = listOf(
        EobHomeGradientDeep.copy(alpha = 0.88f),
        EobHomeGradientBase.copy(alpha = 0.62f),
        EobHomeGradientSurface.copy(alpha = 0.48f),
        EobHomeGradientMid.copy(alpha = 0.54f),
        EobHomeGradientBase.copy(alpha = 0.66f),
        EobHomeGradientDeep.copy(alpha = 0.84f)
    )
)

private val LightHomeScrimGradient = Brush.verticalGradient(
    colors = listOf(
        EobHomeLightGradientTop.copy(alpha = 0.78f),
        EobHomeLightGradientMid.copy(alpha = 0.58f),
        EobHomeLightGradientBase.copy(alpha = 0.42f),
        EobHomeLightGradientMid.copy(alpha = 0.52f),
        EobHomeLightGradientTop.copy(alpha = 0.72f)
    )
)

/**
 * Full-bleed home hub backdrop: medical provider portrait with mode-aware scrims so
 * bento tiles, care-team cards, and header text stay legible in light and dark themes.
 */
@Composable
fun HomeProviderBackground(
    darkModeEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(R.drawable.home_medical_provider_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        if (darkModeEnabled) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(EobCyberBackground.copy(alpha = 0.74f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DarkHomeScrimGradient)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.70f))
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightHomeScrimGradient)
            )
        }
    }
}
