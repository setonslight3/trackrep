package com.setons.trackrep.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

data class ThemePresetColor(
    val name: String,
    val color: Color,
    val hexCode: String
)

/**
 * Manages persistent theme styling, mode selection, and custom accent colors.
 */
class ThemeManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "trackrep_theme_prefs"
        private const val KEY_THEME_MODE = "theme_mode" // "DARK", "LIGHT", "SYSTEM"
        private const val KEY_DARK_ACCENT = "dark_accent_color"
        private const val KEY_LIGHT_ACCENT = "light_accent_color"

        val DEFAULT_DARK_ACCENT = Color(0xFFD4AF37) // Luxury Olympic Gold
        val DEFAULT_LIGHT_ACCENT = Color(0xFFD32F2F) // Athletic Crimson Red

        val RAINBOW_PRESETS = listOf(
            ThemePresetColor("Crimson Red", Color(0xFFE53935), "#E53935"),
            ThemePresetColor("Sunset Orange", Color(0xFFFF6D00), "#FF6D00"),
            ThemePresetColor("Olympic Gold", Color(0xFFD4AF37), "#D4AF37"),
            ThemePresetColor("Volt Lime", Color(0xFFAEEA00), "#AEEA00"),
            ThemePresetColor("Cyber Emerald", Color(0xFF00E676), "#00E676"),
            ThemePresetColor("Aqua Cyan", Color(0xFF00E5FF), "#00E5FF"),
            ThemePresetColor("Electric Blue", Color(0xFF2979FF), "#2979FF"),
            ThemePresetColor("Vivid Violet", Color(0xFF7C4DFF), "#7C4DFF"),
            ThemePresetColor("Neon Magenta", Color(0xFFF50057), "#F50057"),
            ThemePresetColor("Titanium Silver", Color(0xFF9E9E9E), "#9E9E9E")
        )

        @Volatile
        private var instance: ThemeManager? = null

        fun getInstance(context: Context): ThemeManager {
            return instance ?: synchronized(this) {
                instance ?: ThemeManager(context.applicationContext).also { instance = it }
            }
        }

        // Color Conversion Utilities
        fun colorToHex(color: Color): String {
            val r = (color.red * 255).roundToInt().coerceIn(0, 255)
            val g = (color.green * 255).roundToInt().coerceIn(0, 255)
            val b = (color.blue * 255).roundToInt().coerceIn(0, 255)
            return String.format("#%02X%02X%02X", r, g, b)
        }

        fun hexToColor(hex: String): Color? {
            return try {
                val cleaned = hex.trim().removePrefix("#")
                if (cleaned.length == 6) {
                    val r = cleaned.substring(0, 2).toInt(16)
                    val g = cleaned.substring(2, 4).toInt(16)
                    val b = cleaned.substring(4, 6).toInt(16)
                    Color(r, g, b)
                } else if (cleaned.length == 8) {
                    val a = cleaned.substring(0, 2).toInt(16)
                    val r = cleaned.substring(2, 4).toInt(16)
                    val g = cleaned.substring(4, 6).toInt(16)
                    val b = cleaned.substring(6, 8).toInt(16)
                    Color(r, g, b, a)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }

        fun rgbToColor(r: Int, g: Int, b: Int): Color {
            return Color(r.coerceIn(0, 255), g.coerceIn(0, 255), b.coerceIn(0, 255))
        }

        fun colorToHsv(color: Color): FloatArray {
            val r = color.red
            val g = color.green
            val b = color.blue

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val delta = max - min

            var h = 0f
            if (delta != 0f) {
                h = when (max) {
                    r -> 60f * (((g - b) / delta) % 6f)
                    g -> 60f * (((b - r) / delta) + 2f)
                    else -> 60f * (((r - g) / delta) + 4f)
                }
                if (h < 0f) h += 360f
            }

            val s = if (max == 0f) 0f else delta / max
            val v = max

            return floatArrayOf(h, s, v)
        }

        fun hsvToColor(hue: Float, saturation: Float, value: Float): Color {
            val h = (hue % 360f + 360f) % 360f
            val s = saturation.coerceIn(0f, 1f)
            val v = value.coerceIn(0f, 1f)

            val c = v * s
            val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
            val m = v - c

            val (rPrime, gPrime, bPrime) = when {
                h < 60f -> Triple(c, x, 0f)
                h < 120f -> Triple(x, c, 0f)
                h < 180f -> Triple(0f, c, x)
                h < 240f -> Triple(0f, x, c)
                h < 300f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }

            return Color(
                red = (rPrime + m).coerceIn(0f, 1f),
                green = (gPrime + m).coerceIn(0f, 1f),
                blue = (bPrime + m).coerceIn(0f, 1f),
                alpha = 1f
            )
        }
    }

    private var _themeMode by mutableStateOf(prefs.getString(KEY_THEME_MODE, "SYSTEM") ?: "SYSTEM")
    val themeMode: String get() = _themeMode

    private var _darkAccent by mutableStateOf(
        Color(prefs.getInt(KEY_DARK_ACCENT, DEFAULT_DARK_ACCENT.toArgb()))
    )
    val darkAccent: Color get() = _darkAccent

    private var _lightAccent by mutableStateOf(
        Color(prefs.getInt(KEY_LIGHT_ACCENT, DEFAULT_LIGHT_ACCENT.toArgb()))
    )
    val lightAccent: Color get() = _lightAccent

    fun isDark(systemInDarkTheme: Boolean): Boolean {
        return when (_themeMode) {
            "DARK" -> true
            "LIGHT" -> false
            else -> systemInDarkTheme
        }
    }

    fun setThemeMode(mode: String) {
        _themeMode = mode
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
    }

    fun setDarkAccentColor(color: Color) {
        _darkAccent = color
        prefs.edit().putInt(KEY_DARK_ACCENT, color.toArgb()).apply()
    }

    fun setLightAccentColor(color: Color) {
        _lightAccent = color
        prefs.edit().putInt(KEY_LIGHT_ACCENT, color.toArgb()).apply()
    }

    fun setBothAccents(color: Color) {
        _darkAccent = color
        _lightAccent = color
        prefs.edit()
            .putInt(KEY_DARK_ACCENT, color.toArgb())
            .putInt(KEY_LIGHT_ACCENT, color.toArgb())
            .apply()
    }

    fun resetToDefaults() {
        _darkAccent = DEFAULT_DARK_ACCENT
        _lightAccent = DEFAULT_LIGHT_ACCENT
        _themeMode = "SYSTEM"
        prefs.edit()
            .putInt(KEY_DARK_ACCENT, DEFAULT_DARK_ACCENT.toArgb())
            .putInt(KEY_LIGHT_ACCENT, DEFAULT_LIGHT_ACCENT.toArgb())
            .putString(KEY_THEME_MODE, "SYSTEM")
            .apply()
    }
}
