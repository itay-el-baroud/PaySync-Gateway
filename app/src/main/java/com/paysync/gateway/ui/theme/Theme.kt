package com.paysync.gateway.ui.theme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val LightColorScheme = lightColorScheme(primary = Color.Black, onPrimary = Color.White, primaryContainer = Color.Black, onPrimaryContainer = Color.White, secondary = Color(0xFF424242), onSecondary = Color.White, background = Color.White, onBackground = Color.Black, surface = Color.White, onSurface = Color.Black, error = Color(0xFFC62828))
@Composable
fun PaySyncTheme(content: @Composable () -> Unit) { MaterialTheme(colorScheme = LightColorScheme, content = content) }
