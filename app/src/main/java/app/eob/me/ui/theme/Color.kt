package app.eob.me.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ---- Cyber dark palette (EOBme brand) ----
val EobCyberBackground = Color(0xFF0B0E14)
val EobCyberBackgroundDeep = Color(0xFF060810)
val EobCyberBackgroundMid = Color(0xFF121820)
val EobCyberAccent = Color(0xFF00D1FF)
val EobCyberAccentBright = Color(0xFF00F2FF)
val EobCyberGlow = Color(0xFF00E5FF)
val EobCyberSuccess = Color(0xFF4CAF50)
val EobCyberError = Color(0xFFFF5252)
val EobCyberWarning = Color(0xFFD4AF37)
val EobCyberTextPrimary = Color(0xFFFFFFFF)
val EobCyberTextSecondary = Color(0xFFA0AABF)
val EobCyberGlassFill = Color(0x0DFFFFFF)
val EobCyberGlassBorder = Color(0x14FFFFFF)
val EobCyberSurface = Color(0xFF141A24)
val EobCyberSurfaceVariant = Color(0xFF1A2230)
val EobCyberNavBar = Color(0xFF0E1219)
val EobCyberOverlay = Color(0xB8000000)

// Semantic aliases used across components
val EobBrandBlue = EobCyberAccent
val EobBrandCyan = EobCyberAccentBright
val EobBrandGlow = EobCyberGlow

// Material theme token aliases (legacy names mapped to cyber palette)
val EobBlue80 = EobCyberAccentBright
val EobBlueGrey80 = EobCyberTextSecondary
val EobSky80 = EobCyberGlow
val EobNavy40 = EobCyberAccent
val EobBlue40 = EobCyberAccent
val EobSky40 = EobCyberGlow
val EobBackground = EobCyberBackground
val EobSurface = EobCyberSurface
val EobOutline = EobCyberGlassBorder

// Chart and dashboard semantics
val EobChartBlue = EobCyberAccent
val EobChartGreen = EobCyberSuccess
val EobChartPurple = Color(0xFF7E57C2)
val EobChartRed = EobCyberError
val EobChartOrange = Color(0xFFFF9800)
val EobChartTeal = Color(0xFF00897B)
val EobChartIndigo = Color(0xFF5C6BC0)
val EobBilledBlue = EobCyberAccent
val EobAdjustmentGreen = Color(0xFF10B981)
val EobPatientRed = EobCyberError

// Care team role accents
val EobCarePcpGreen = Color(0xFF43A047)
val EobCareDentistBlue = Color(0xFF1E88E5)
val EobCareSpecialistYellow = Color(0xFFF9A825)
val EobCareTherapistRed = EobCyberError

// Insurance card styling
val EobInsuranceGradientStart = Color(0xFF1A2838)
val EobInsuranceGradientMid = Color(0xFF121A24)
val EobInsuranceGradientEnd = Color(0xFF0B0E14)
val EobInsuranceNameAccent = EobCyberAccentBright.copy(alpha = 0.85f)
val EobInsuranceSecondaryText = EobCyberTextSecondary

// Gauge and tracker semantics
val EobGaugeLow = EobCyberSuccess
val EobGaugeMid = EobCareSpecialistYellow
val EobGaugeHigh = EobCyberError
val EobGaugeTrack = EobCyberSurfaceVariant
val EobLineTrajectory = EobCyberWarning
val EobParticleColor = EobCyberGlow.copy(alpha = 0.6f)

// Home hub gradient stops (layout unchanged; colors only)
val EobHomeGradientDeep = EobCyberBackgroundDeep
val EobHomeGradientBase = EobCyberBackground
val EobHomeGradientSurface = EobCyberSurface
val EobHomeGradientMid = EobCyberBackgroundMid

// Light hub palette (default on first launch)
val EobLightBackground = Color(0xFFFFFFFF)
val EobLightBackgroundSoft = Color(0xFFF5F7FA)
val EobLightSurface = Color(0xFFF7F9FC)
val EobLightSurfaceVariant = Color(0xFFEEF2F7)
val EobLightTextPrimary = Color(0xFF102033)
val EobLightTextSecondary = Color(0xFF5C6B7A)
val EobLightOutline = Color(0xFFB7C7D9)
val EobBentoCardSurface = Color(0xFFF0F4F8)
val EobSubscriptionFree = Color(0xFF8A8F98)
val EobSubscriptionSilver = Color(0xFF9EA3A8)
val EobSubscriptionGold = Color(0xFFD4AF37)

// Home hub gradient stops for light mode (layout unchanged; colors only)
val EobHomeLightGradientTop = EobLightBackground
val EobHomeLightGradientMid = EobLightBackgroundSoft
val EobHomeLightGradientBase = EobLightSurface

/** App-wide background behind hub screens in light mode. */
fun eobLightAppBackgroundGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        EobLightBackground,
        EobLightBackgroundSoft,
        EobLightBackground
    )
)

/** App-wide background behind hub screens (opening screens keep their own styling). */
fun eobCyberAppBackgroundGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        EobCyberBackgroundDeep,
        EobCyberBackground,
        EobCyberBackgroundMid,
        EobCyberBackground,
        EobCyberBackgroundDeep
    )
)

/** Frosted glass fill for care-team and bento surfaces. */
fun eobCyberGlassGradient(): Brush = Brush.verticalGradient(
    colors = listOf(
        EobCyberSurface.copy(alpha = 0.92f),
        EobCyberSurfaceVariant.copy(alpha = 0.88f),
        EobCyberBackgroundMid.copy(alpha = 0.85f)
    )
)
