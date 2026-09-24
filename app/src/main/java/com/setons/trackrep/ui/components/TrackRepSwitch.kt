package com.setons.trackrep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.setons.trackrep.theme.DarkPrimaryGold

/**
 * Luxury TrackRep high-contrast switch.
 * Ensures the toggle dot is ALWAYS crisp, sharp, and clearly visible against the track:
 * - Checked: Rich metallic gold track capsule with a solid, high-contrast dark thumb dot.
 * - Unchecked: Deep charcoal track capsule with a clean white/light gray thumb dot.
 */
@Composable
fun TrackRepSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color(0xFF0C0A04),
            checkedTrackColor = DarkPrimaryGold,
            checkedBorderColor = DarkPrimaryGold,
            uncheckedThumbColor = Color(0xFFCCCCCC),
            uncheckedTrackColor = Color(0xFF262626),
            uncheckedBorderColor = Color(0xFF555555)
        ),
        thumbContent = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (checked) Color(0xFF0C0A04) else Color.White,
                        shape = CircleShape
                    )
            )
        }
    )
}
