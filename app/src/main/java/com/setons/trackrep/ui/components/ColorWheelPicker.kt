package com.setons.trackrep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.theme.ThemeManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Interactive color picker combining a Color Wheel (Hue & Saturation),
 * Brightness Slider, RGB Numerical Sliders, and Hex code input.
 */
@Composable
fun ColorWheelPicker(
    selectedColor: Color,
    onColorChanged: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // Internal HSV state
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }

    // Internal RGB state
    var redInt by remember { mutableIntStateOf(212) }
    var greenInt by remember { mutableIntStateOf(175) }
    var blueInt by remember { mutableIntStateOf(55) }

    // Internal Hex text state
    var hexText by remember { mutableStateOf("D4AF37") }

    // Synchronize from selectedColor when externally updated
    LaunchedEffect(selectedColor) {
        val hsv = ThemeManager.colorToHsv(selectedColor)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]

        redInt = (selectedColor.red * 255).roundToInt().coerceIn(0, 255)
        greenInt = (selectedColor.green * 255).roundToInt().coerceIn(0, 255)
        blueInt = (selectedColor.blue * 255).roundToInt().coerceIn(0, 255)

        hexText = ThemeManager.colorToHex(selectedColor).removePrefix("#")
    }

    fun notifyHsvChanged(newH: Float, newS: Float, newV: Float) {
        hue = newH
        saturation = newS
        brightness = newV
        val c = ThemeManager.hsvToColor(newH, newS, newV)
        redInt = (c.red * 255).roundToInt().coerceIn(0, 255)
        greenInt = (c.green * 255).roundToInt().coerceIn(0, 255)
        blueInt = (c.blue * 255).roundToInt().coerceIn(0, 255)
        hexText = ThemeManager.colorToHex(c).removePrefix("#")
        onColorChanged(c)
    }

    fun notifyRgbChanged(r: Int, g: Int, b: Int) {
        redInt = r
        greenInt = g
        blueInt = b
        val c = ThemeManager.rgbToColor(r, g, b)
        val hsv = ThemeManager.colorToHsv(c)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hexText = ThemeManager.colorToHex(c).removePrefix("#")
        onColorChanged(c)
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Color Wheel Canvas
        Box(
            modifier = Modifier
                .size(220.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        fun updateFromOffset(offset: Offset, size: androidx.compose.ui.geometry.Size) {
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radius = min(centerX, centerY)
                            val dx = offset.x - centerX
                            val dy = offset.y - centerY
                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

                            val angleRad = atan2(dy.toDouble(), dx.toDouble())
                            var angleDeg = Math.toDegrees(angleRad).toFloat()
                            if (angleDeg < 0f) angleDeg += 360f

                            val newS = (dist / radius).coerceIn(0f, 1f)
                            notifyHsvChanged(angleDeg, newS, brightness.coerceAtLeast(0.1f))
                        }

                        detectTapGestures { offset ->
                            updateFromOffset(offset, size.let { androidx.compose.ui.geometry.Size(it.width.toFloat(), it.height.toFloat()) })
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val centerX = size.width / 2f
                            val centerY = size.height / 2f
                            val radius = min(centerX, centerY)
                            val dx = change.position.x - centerX
                            val dy = change.position.y - centerY
                            val dist = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

                            val angleRad = atan2(dy.toDouble(), dx.toDouble())
                            var angleDeg = Math.toDegrees(angleRad).toFloat()
                            if (angleDeg < 0f) angleDeg += 360f

                            val newS = (dist / radius).coerceIn(0f, 1f)
                            notifyHsvChanged(angleDeg, newS, brightness.coerceAtLeast(0.1f))
                        }
                    }
            ) {
                val radius = min(size.width, size.height) / 2f
                val center = Offset(size.width / 2f, size.height / 2f)

                // 1. Draw Sweep Gradient Hue Wheel
                val hueColors = listOf(
                    Color.Red,
                    Color.Yellow,
                    Color.Green,
                    Color.Cyan,
                    Color.Blue,
                    Color.Magenta,
                    Color.Red
                )
                drawCircle(
                    brush = Brush.sweepGradient(hueColors, center = center),
                    radius = radius,
                    center = center
                )

                // 2. Radial Desaturation Gradient (Center white to edge transparent)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color.White.copy(alpha = 0f)),
                        center = center,
                        radius = radius
                    ),
                    radius = radius,
                    center = center
                )

                // 3. Current Selection Pointer Ring
                val angleRad = Math.toRadians(hue.toDouble())
                val pointerDist = saturation * radius
                val pointerX = center.x + (pointerDist * cos(angleRad)).toFloat()
                val pointerY = center.y + (pointerDist * sin(angleRad)).toFloat()

                // Outer black indicator ring
                drawCircle(
                    color = Color.Black,
                    radius = 12.dp.toPx(),
                    center = Offset(pointerX, pointerY),
                    style = Stroke(width = 3.dp.toPx())
                )
                // Inner white indicator ring
                drawCircle(
                    color = Color.White,
                    radius = 9.dp.toPx(),
                    center = Offset(pointerX, pointerY),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Center filled current color
                drawCircle(
                    color = selectedColor,
                    radius = 7.dp.toPx(),
                    center = Offset(pointerX, pointerY)
                )
            }
        }

        // Brightness / Value Slider
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Brightness / Tone",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${(brightness * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = brightness,
                onValueChange = { newB ->
                    notifyHsvChanged(hue, saturation, newB)
                },
                valueRange = 0.15f..1f,
                colors = SliderDefaults.colors(
                    thumbColor = selectedColor,
                    activeTrackColor = selectedColor,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }

        // RGB Numerical Sliders
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "RGB Precision Tuning",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Red Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("R", fontWeight = FontWeight.Bold, color = Color(0xFFE53935), modifier = Modifier.width(16.dp))
                Slider(
                    value = redInt.toFloat(),
                    onValueChange = { notifyRgbChanged(it.roundToInt(), greenInt, blueInt) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFFE53935),
                        activeTrackColor = Color(0xFFE53935)
                    )
                )
                Text(
                    text = "$redInt",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(36.dp)
                )
            }

            // Green Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("G", fontWeight = FontWeight.Bold, color = Color(0xFF43A047), modifier = Modifier.width(16.dp))
                Slider(
                    value = greenInt.toFloat(),
                    onValueChange = { notifyRgbChanged(redInt, it.roundToInt(), blueInt) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF43A047),
                        activeTrackColor = Color(0xFF43A047)
                    )
                )
                Text(
                    text = "$greenInt",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(36.dp)
                )
            }

            // Blue Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("B", fontWeight = FontWeight.Bold, color = Color(0xFF1E88E5), modifier = Modifier.width(16.dp))
                Slider(
                    value = blueInt.toFloat(),
                    onValueChange = { notifyRgbChanged(redInt, greenInt, it.roundToInt()) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF1E88E5),
                        activeTrackColor = Color(0xFF1E88E5)
                    )
                )
                Text(
                    text = "$blueInt",
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.width(36.dp)
                )
            }
        }

        // HEX Code Input & Preview Box
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Live Color Swatch Box
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedColor)
                    .border(2.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            )

            // Hex Text Input
            OutlinedTextField(
                value = hexText,
                onValueChange = { input ->
                    val filtered = input.uppercase().filter { it in "0123456789ABCDEF" }.take(6)
                    hexText = filtered
                    if (filtered.length == 6) {
                        ThemeManager.hexToColor(filtered)?.let { newColor ->
                            onColorChanged(newColor)
                        }
                    }
                },
                label = { Text("HEX Code") },
                prefix = {
                    Text(
                        text = "#",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 16.sp
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.weight(1f)
            )
        }
    }
}
