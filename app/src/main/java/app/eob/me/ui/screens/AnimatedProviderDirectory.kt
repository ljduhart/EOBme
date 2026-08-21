package app.eob.me.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Help
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlinx.coroutines.delay

private const val ProviderDirectoryVerticalScale = 0.85f
private const val ProviderExpandDurationMillis = 200
private val ProviderExpandAnimationSpec = tween<Float>(
    durationMillis = ProviderExpandDurationMillis,
    easing = FastOutSlowInEasing
)

private val ProviderListItemSpacing = (12 * ProviderDirectoryVerticalScale).dp
private val ProviderCardPadding = (16 * ProviderDirectoryVerticalScale).dp
private val ProviderAvatarSize = (48 * ProviderDirectoryVerticalScale).dp
private val ProviderHeaderSpacerWidth = (16 * ProviderDirectoryVerticalScale).dp
private val ProviderBadgeSpacerWidth = (8 * ProviderDirectoryVerticalScale).dp
private val ProviderNameSpacerHeight = (4 * ProviderDirectoryVerticalScale).dp
private val ProviderExpandedTopPadding = (16 * ProviderDirectoryVerticalScale).dp
private val ProviderExpandedSectionSpacer = (16 * ProviderDirectoryVerticalScale).dp
private val ProviderAssuranceIconSize = (16 * ProviderDirectoryVerticalScale).dp
private val ProviderExpandIconSize = (18 * ProviderDirectoryVerticalScale).dp
private val ProviderViewRecordsFontSize = (15 * ProviderDirectoryVerticalScale).sp

enum class NetworkStatus(
    val label: String,
    val containerColor: Color,
    val contentColor: Color,
    val borderColor: Color,
    val icon: ImageVector
) {
    IN_NETWORK(
        "In-Network",
        Color(0xFFEAF4FC),
        Color(0xFF0056B3),
        Color(0xFFB9D9F6),
        Icons.Rounded.CheckCircle
    ),
    OUT_OF_NETWORK(
        "Out-of-Network",
        Color(0xFFFFEBEE),
        Color(0xFFC62828),
        Color(0xFFEF9A9A),
        Icons.Rounded.Cancel
    ),
    PENDING(
        "Pending / Unknown",
        Color(0xFFFFF8E1),
        Color(0xFFF57F17),
        Color(0xFFFFE082),
        Icons.AutoMirrored.Rounded.Help
    )
}

data class PremiumProviderSummary(
    val id: String,
    val name: String,
    val eobCount: Int,
    val lastServiceDate: String,
    val totalBilled: Double,
    val totalPaid: Double,
    val totalResponsibility: Double,
    val networkStatus: NetworkStatus = NetworkStatus.PENDING
)

@Composable
fun AnimatedProviderDirectoryScreen(
    providers: List<PremiumProviderSummary>,
    onViewEobsClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Provider Directory",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Facilities and clinicians extracted from your synced EOB history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(ProviderListItemSpacing)
        ) {
            itemsIndexed(items = providers, key = { _, item -> item.id }) { index, provider ->
                val visibleState = remember(provider.id) { MutableTransitionState(false) }
                LaunchedEffect(provider.id) {
                    delay(index * 75L)
                    visibleState.targetState = true
                }

                AnimatedVisibility(
                    visibleState = visibleState,
                    enter = slideInVertically(
                        initialOffsetY = { 100 },
                        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(durationMillis = 500))
                ) {
                    ExpandableProviderCard(
                        provider = provider,
                        onViewEobsClicked = { onViewEobsClicked(provider.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableProviderCard(
    provider: PremiumProviderSummary,
    onViewEobsClicked: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val arrowRotationDegree by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = ProviderExpandAnimationSpec,
        label = "arrowRotation"
    )
    val cardElevation by animateDpAsState(
        targetValue = if (expanded) 8.dp else 2.dp,
        animationSpec = tween(durationMillis = ProviderExpandDurationMillis, easing = FastOutSlowInEasing),
        label = "cardElevation"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(ProviderCardPadding)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Box(
                    modifier = Modifier
                        .size(ProviderAvatarSize)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = provider.name.firstOrNull()?.toString()?.uppercase() ?: "",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(ProviderHeaderSpacerWidth))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = provider.name.uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Last service: ${provider.lastServiceDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(ProviderNameSpacerHeight))
                    AssuranceBadge(status = provider.networkStatus)
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.padding(start = ProviderBadgeSpacerWidth)
                ) {
                    Text(
                        text = "${provider.eobCount} EOBs",
                        modifier = Modifier.padding(
                            horizontal = (10 * ProviderDirectoryVerticalScale).dp,
                            vertical = (4 * ProviderDirectoryVerticalScale).dp
                        ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.width(ProviderBadgeSpacerWidth))

                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(ProviderExpandIconSize)
                        .rotate(arrowRotationDegree)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = tween(
                        durationMillis = ProviderExpandDurationMillis,
                        easing = FastOutSlowInEasing
                    ),
                    expandFrom = Alignment.Top
                ) + fadeIn(
                    animationSpec = tween(
                        durationMillis = ProviderExpandDurationMillis,
                        easing = FastOutSlowInEasing
                    )
                ),
                exit = shrinkVertically(
                    animationSpec = tween(
                        durationMillis = ProviderExpandDurationMillis,
                        easing = FastOutSlowInEasing
                    ),
                    shrinkTowards = Alignment.Top
                ) + fadeOut(
                    animationSpec = tween(
                        durationMillis = ProviderExpandDurationMillis,
                        easing = FastOutSlowInEasing
                    )
                )
            ) {
                Column(modifier = Modifier.padding(top = ProviderExpandedTopPadding)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        thickness = 1.dp
                    )
                    Spacer(modifier = Modifier.height(ProviderExpandedSectionSpacer))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FinancialStatBlock(
                            label = "Billed",
                            amount = "$${String.format(Locale.US, "%.2f", provider.totalBilled)}",
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FinancialStatBlock(
                            label = "Paid",
                            amount = "$${String.format(Locale.US, "%.2f", provider.totalPaid)}",
                            color = Color(0xFF4CAF50)
                        )
                        FinancialStatBlock(
                            label = "Responsibility",
                            amount = "$${String.format(Locale.US, "%.2f", provider.totalResponsibility)}",
                            color = if (provider.totalResponsibility > 0) {
                                Color(0xFFE53935)
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(ProviderExpandedSectionSpacer))

                    TextButton(
                        onClick = onViewEobsClicked,
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            text = "View Records",
                            fontWeight = FontWeight.Bold,
                            fontSize = ProviderViewRecordsFontSize
                        )
                        Spacer(modifier = Modifier.width((4 * ProviderDirectoryVerticalScale).dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = "View",
                            modifier = Modifier.size(ProviderExpandIconSize)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FinancialStatBlock(label: String, amount: String, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.titleMedium,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun AssuranceBadge(status: NetworkStatus) {
    Surface(
        shape = RoundedCornerShape(50),
        color = status.containerColor,
        border = BorderStroke(1.dp, status.borderColor),
        modifier = Modifier.padding(top = (6 * ProviderDirectoryVerticalScale).dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(
                start = (6 * ProviderDirectoryVerticalScale).dp,
                end = (10 * ProviderDirectoryVerticalScale).dp,
                top = (4 * ProviderDirectoryVerticalScale).dp,
                bottom = (4 * ProviderDirectoryVerticalScale).dp
            )
        ) {
            Icon(
                imageVector = status.icon,
                contentDescription = status.label,
                tint = status.contentColor,
                modifier = Modifier.size(ProviderAssuranceIconSize)
            )
            Spacer(modifier = Modifier.width((6 * ProviderDirectoryVerticalScale).dp))
            Text(
                text = status.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = (12 * ProviderDirectoryVerticalScale).sp
                ),
                color = status.contentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
