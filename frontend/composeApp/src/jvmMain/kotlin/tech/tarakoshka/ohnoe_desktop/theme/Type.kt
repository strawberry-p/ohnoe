package tech.tarakoshka.ohnoe_desktop.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import frontend.composeapp.generated.resources.Res
import frontend.composeapp.generated.resources.overpass
import frontend.composeapp.generated.resources.pirata
import org.jetbrains.compose.resources.Font


@Composable
fun bodyFontFamily() = FontFamily(
    Font(
        Res.font.overpass, FontWeight.Normal, FontStyle.Normal
    )
)

@Composable
fun displayFontFamily() = FontFamily(
    Font(Res.font.pirata, FontWeight.Normal, FontStyle.Normal)
)

// Default Material 3 typography values
val baseline = Typography()

@Composable
fun AppTypography() = Typography(
    displayLarge = baseline.displayLarge.copy(fontFamily = displayFontFamily()),
    displayMedium = baseline.displayMedium.copy(fontFamily = displayFontFamily()),
    displaySmall = baseline.displaySmall.copy(fontFamily = displayFontFamily()),
    headlineLarge = baseline.headlineLarge.copy(fontFamily = displayFontFamily()),
    headlineMedium = baseline.headlineMedium.copy(fontFamily = displayFontFamily()),
    headlineSmall = baseline.headlineSmall.copy(fontFamily = displayFontFamily()),
    titleLarge = baseline.titleLarge.copy(fontFamily = displayFontFamily()),
    titleMedium = baseline.titleMedium.copy(fontFamily = displayFontFamily()),
    titleSmall = baseline.titleSmall.copy(fontFamily = displayFontFamily()),
    bodyLarge = baseline.bodyLarge.copy(fontFamily = bodyFontFamily()),
    bodyMedium = baseline.bodyMedium.copy(fontFamily = bodyFontFamily()),
    bodySmall = baseline.bodySmall.copy(fontFamily = bodyFontFamily()),
    labelLarge = baseline.labelLarge.copy(fontFamily = bodyFontFamily()),
    labelMedium = baseline.labelMedium.copy(fontFamily = bodyFontFamily()),
    labelSmall = baseline.labelSmall.copy(fontFamily = bodyFontFamily()),
)

