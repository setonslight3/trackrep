package com.setons.trackrep.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * High-contrast responsive switch matching active theme (Crimson Red in Light Mode, Luxury Gold in Dark Mode).
 */
@Composable
fun TrackRepSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onPrimaryColor = MaterialTheme.colorScheme.onPrimary
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = onPrimaryColor,
            checkedTrackColor = primaryColor,
            checkedBorderColor = primaryColor,
            uncheckedThumbColor = outlineColor,
            uncheckedTrackColor = surfaceVariant,
            uncheckedBorderColor = outlineColor.copy(alpha = 0.4f)
        ),
        thumbContent = {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(
                        color = if (checked) onPrimaryColor else outlineColor,
                        shape = CircleShape
                    )
            )
        }
    )
}
