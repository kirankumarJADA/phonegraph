package com.phonegraph.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary = PhoneGraphPurple,
    secondary = PhoneGraphPurpleGrey,
    tertiary = PhoneGraphPink
)

private val LightColors = lightColorScheme(
    primary = PhoneGraphPurple,
    secondary = PhoneGraphPurpleGrey,
    tertiary = PhoneGraphPink
)

@Composable
fun PhoneGraphTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
