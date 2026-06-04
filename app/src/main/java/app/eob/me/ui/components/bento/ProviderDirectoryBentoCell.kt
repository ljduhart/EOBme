package app.eob.me.ui.components.bento

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.eob.me.data.AppLanguage
import app.eob.me.data.ProviderAvatarPreview
import app.eob.me.navigation.HubBentoDestination

private val NeonCyan = Color(0xFF00E5FF)
private val NeonCyanDim = Color(0x6600E5FF)

@Composable
fun ProviderDirectoryBentoCell(
    language: AppLanguage,
    avatars: List<ProviderAvatarPreview>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isPressed by interactionSource.collectIsPressedAsState()
    val isActive = isHovered || isPressed

    val elevation by animateDpAsState(
        targetValue = if (isActive) 10.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "providerBentoLift"
    )
    val fanProgress by animateFloatAsState(
        targetValue = if (isActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "providerFanProgress"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1.35f)
            .hoverable(interactionSource = interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = HubBentoDestination.ProviderDirectory.title(language),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (avatars.isEmpty()) {
                    Text(
                        text = HubBentoDestination.ProviderDirectory.title(language),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                } else {
                    ProviderAvatarFan(
                        avatars = avatars,
                        fanProgress = fanProgress,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderAvatarFan(
    avatars: List<ProviderAvatarPreview>,
    fanProgress: Float,
    modifier: Modifier = Modifier
) {
    val displayAvatars = avatars.take(3)
    val fanOffsets = listOf(-28f, 0f, 28f)
    val fanRotations = listOf(-14f, 0f, 14f)
    val arcLift = listOf(10f, 0f, 10f)

    Box(modifier = modifier.height(72.dp), contentAlignment = Alignment.Center) {
        if (displayAvatars.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .offset(y = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                repeat(displayAvatars.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                            .border(
                                width = (1f * fanProgress).dp.coerceAtLeast(0.dp),
                                brush = Brush.horizontalGradient(
                                    listOf(Color.Transparent, NeonCyan, Color.Transparent)
                                ),
                                shape = RoundedCornerShape(50)
                            )
                            .height((1.5f * fanProgress).dp.coerceAtLeast(0.dp))
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.Bottom
        ) {
            displayAvatars.forEachIndexed { index, avatar ->
                val spread = fanOffsets.getOrElse(index) { 0f } * fanProgress
                val lift = arcLift.getOrElse(index) { 0f } * fanProgress
                val rotation = fanRotations.getOrElse(index) { 0f } * fanProgress

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            translationX = spread
                            translationY = -lift
                            rotationZ = rotation
                        }
                        .padding(horizontal = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        NeonCyan.copy(alpha = 0.35f * fanProgress + 0.1f),
                                        Color(0xFF1565A8)
                                    )
                                )
                            )
                            .border(
                                width = 1.5.dp,
                                color = NeonCyan.copy(alpha = 0.4f + 0.6f * fanProgress),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatar.initials,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 11.sp
                        )
                    }
                    if (fanProgress > 0.35f) {
                        Column(
                            modifier = Modifier
                                .padding(top = 3.dp)
                                .graphicsLayer { alpha = ((fanProgress - 0.35f) / 0.65f).coerceIn(0f, 1f) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = avatar.displayName.take(10),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 7.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = avatar.specialtyLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 6.sp,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = NeonCyan.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }
        }
    }
}
