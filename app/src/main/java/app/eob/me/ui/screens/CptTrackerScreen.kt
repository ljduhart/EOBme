package app.eob.me.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Biotech
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.MedicalInformation
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Vaccines
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.eob.me.data.AppLanguage
import app.eob.me.data.CptCategory
import app.eob.me.data.CptCodeEntry
import app.eob.me.data.EobStrings

/** Office visits (99213, 99214, 99385, …) */
private val CptOfficeVisitBlue = Color(0xFF03A9F4)

/** Laboratory panels and urinalysis (81004, 80035, …) */
private val CptLabGreen = Color(0xFF43A047)

/** Hospital / inpatient / imaging (99221, 99222, 99223, …) */
private val CptHospitalRed = Color(0xFFE53935)

/** Durable medical equipment */
private val CptDmeBlack = Color(0xFF212121)

/** Injection HCPCS (J3420, J0081, J0013, …) */
private val CptInjectionYellow = Color(0xFFFFC107)

/** Dental, therapy, and uncategorized codes */
private val CptOtherPurple = Color(0xFF8E24AA)

private val CptFlashcardPaper = Color.White
private val CptFlashcardBackText = Color(0xFF1A1A1A)
private val CptCategoryTabShape = RoundedCornerShape(12.dp)
private val CptFlashcardShape = RoundedCornerShape(16.dp)

@Composable
fun CptTrackerScreen(
    language: AppLanguage,
    entries: List<CptCodeEntry>,
    selectedCategory: CptCategory,
    onCategorySelected: (CptCategory) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = EobStrings.t(language, "cptTrackingTitle"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = EobStrings.t(language, "cptTrackingSubtitle"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        CptCategoryTabs(
            language = language,
            selectedCategory = selectedCategory,
            onCategorySelected = onCategorySelected
        )

        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(bottom = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                CptEducationalBanner(language = language)
            }
        } else {
            LazyVerticalGrid(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                columns = GridCells.Adaptive(minSize = 150.dp),
                contentPadding = PaddingValues(bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(entries, key = { "${selectedCategory.name}|${it.code}" }) { entry ->
                    FlashcardItem(
                        language = language,
                        entry = entry,
                        category = selectedCategory
                    )
                }
            }
        }
    }
}

@Composable
fun CptCategoryTabs(
    language: AppLanguage,
    selectedCategory: CptCategory,
    onCategorySelected: (CptCategory) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(CptCategory.entries) { category ->
            val isSelected = selectedCategory == category
            val categoryColor = categoryThemeColor(category)

            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) categoryColor else categoryColor.copy(alpha = 0.82f),
                animationSpec = tween(300),
                label = "TabBackgroundAnimation"
            )
            val contentColor = categoryContentColor(category)

            Surface(
                shape = CptCategoryTabShape,
                color = backgroundColor,
                shadowElevation = if (isSelected) 6.dp else 2.dp,
                modifier = Modifier
                    .clip(CptCategoryTabShape)
                    .clickable { onCategorySelected(category) }
            ) {
                Text(
                    text = EobStrings.cptCategoryLabel(language, category),
                    color = contentColor,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CptEducationalBanner(language: AppLanguage) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = EobStrings.t(language, "cptEducationalBannerTitle"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = EobStrings.t(language, "cptEducationalBannerBody"),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun FlashcardItem(
    language: AppLanguage,
    entry: CptCodeEntry,
    category: CptCategory,
    modifier: Modifier = Modifier
) {
    var flipped by remember(entry.code, category) { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "flashcard_rotation"
    )
    val density = LocalDensity.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(5f / 6f)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density.density
            }
            .clickable { flipped = !flipped },
        shape = CptFlashcardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (rotation <= 90f) {
                FlashcardFront(
                    entry = entry,
                    category = category
                )
            } else {
                FlashcardBack(
                    language = language,
                    entry = entry,
                    category = category,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}

@Composable
private fun FlashcardFront(
    entry: CptCodeEntry,
    category: CptCategory
) {
    val frontColor = categoryThemeColor(category)
    val contentColor = categoryContentColor(category)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(frontColor)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = entry.code,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Icon(
                imageVector = categoryIcon(category),
                contentDescription = entry.category,
                tint = contentColor.copy(alpha = 0.92f),
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = entry.shortName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = contentColor.copy(alpha = 0.95f),
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun FlashcardBack(
    language: AppLanguage,
    entry: CptCodeEntry,
    category: CptCategory,
    modifier: Modifier = Modifier
) {
    val borderColor = categoryThemeColor(category)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CptFlashcardPaper)
            .border(width = 4.dp, color = borderColor, shape = CptFlashcardShape)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = EobStrings.t(language, "cptFlashcardOfficialDefinition"),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = CptFlashcardBackText
                )
                Text(
                    text = entry.definition,
                    style = MaterialTheme.typography.bodySmall,
                    color = CptFlashcardBackText.copy(alpha = 0.72f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                if (entry.serviceDates.isNotBlank()) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = EobStrings.t(language, "cptFlashcardDosLabel"),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CptFlashcardBackText.copy(alpha = 0.82f)
                        )
                        Text(
                            text = entry.serviceDates,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = CptFlashcardBackText.copy(alpha = 0.82f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = EobStrings.t(language, "cptFlashcardBilledTitle"),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = CptFlashcardBackText.copy(alpha = 0.82f),
                        textAlign = TextAlign.End
                    )
                    Text(
                        text = entry.totalBilled,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = CptFlashcardBackText,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

internal fun categoryThemeColor(category: CptCategory): Color {
    return when (category) {
        CptCategory.OfficeVisit -> CptOfficeVisitBlue
        CptCategory.Lab -> CptLabGreen
        CptCategory.Hospital -> CptHospitalRed
        CptCategory.Dme -> CptDmeBlack
        CptCategory.Injection -> CptInjectionYellow
        CptCategory.Other -> CptOtherPurple
    }
}

internal fun categoryContentColor(category: CptCategory): Color {
    return when (category) {
        CptCategory.Injection -> CptFlashcardBackText
        else -> Color.White
    }
}

private fun categoryIcon(category: CptCategory): ImageVector {
    return when (category) {
        CptCategory.OfficeVisit -> Icons.Rounded.MedicalServices
        CptCategory.Lab -> Icons.Rounded.Biotech
        CptCategory.Hospital -> Icons.Rounded.LocalHospital
        CptCategory.Dme -> Icons.Rounded.MedicalInformation
        CptCategory.Injection -> Icons.Rounded.Vaccines
        CptCategory.Other -> Icons.Rounded.Info
    }
}
