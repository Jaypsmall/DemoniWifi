package com.jaylizapp.demoniwifi.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DemonicColorScheme = darkColorScheme(
    primary = DemonicRed,
    secondary = BloodRed,
    tertiary = PentagramGold,
    background = EvilBlack,
    surface = HellGray,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = EvilBlack,
    onBackground = DemonicRed,
    onSurface = BloodRed
)

private val LightColorScheme = lightColorScheme(
    primary = DemonicRed,
    secondary = HellGray,
    tertiary = PentagramGold,
    background = ShinySilver, // Plata metálico tipo DemoniTalk
    surface = Color.White,    // Blanco alma
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AshGrey,
    onSurface = SoulWhite
)

@Composable
fun DemoniWifiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DemonicColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
