package com.example.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object ThemePresets {
    fun getPresetBrush(presetName: String?): Brush {
        val colors = when (presetName) {
            "preset_fitness" -> listOf(Color(0xFF3E0A0A), Color(0xFF1C0505), Color(0xFF121212))
            "preset_productivity" -> listOf(Color(0xFF0D1F2D), Color(0xFF1A3644), Color(0xFF121212))
            "preset_cooking" -> listOf(Color(0xFF301E06), Color(0xFF1A1201), Color(0xFF121212))
            "preset_spark" -> listOf(Color(0xFF3B0B3C), Color(0xFF1D0620), Color(0xFF121212))
            "preset_nature" -> listOf(Color(0xFF0B291B), Color(0xFF051C12), Color(0xFF121212))
            else -> listOf(Color(0xFF1E1E1E), Color(0xFF121212))
        }
        return Brush.verticalGradient(colors)
    }

    fun getPresetCardBrush(presetName: String?, themeColor: Color): Brush {
        return when (presetName) {
            "preset_fitness" -> Brush.verticalGradient(listOf(Color(0xFF8B0000), Color(0xFF1C0000)))
            "preset_productivity" -> Brush.verticalGradient(listOf(Color(0xFF0D243A), Color(0xFF030E1B)))
            "preset_cooking" -> Brush.verticalGradient(listOf(Color(0xFF533306), Color(0xFF281601)))
            "preset_spark" -> Brush.verticalGradient(listOf(Color(0xFF5D0B5E), Color(0xFF1F0320)))
            "preset_nature" -> Brush.verticalGradient(listOf(Color(0xFF0A3E1E), Color(0xFF01180A)))
            else -> Brush.verticalGradient(listOf(themeColor.copy(alpha = 0.85f), themeColor.copy(alpha = 0.4f)))
        }
    }
}
