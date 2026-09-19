package com.setons.trackrep.review

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.setons.trackrep.pose.SkeletonOverlay
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold

@Composable
fun MotionSticksCanvas(
    poses: List<TimestampedPose>,
    currentPositionMs: Long,
    isFrontCamera: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Find closest recorded pose to current timeline position
    val currentPose = remember(poses, currentPositionMs) {
        if (poses.isEmpty()) null
        else {
            poses.minByOrNull { Math.abs(it.timestampMs - currentPositionMs) }?.pose
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentPose != null) {
            SkeletonOverlay(
                pose = currentPose,
                isFrontCamera = isFrontCamera,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No motion telemetry captured for this timestamp",
                    color = Color.White.copy(alpha = 0.6f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Privacy Mode Badge
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
        ) {
            Text(
                text = "🦴 MOTION STICKS ONLY (PRIVACY MODE)",
                style = MaterialTheme.typography.labelSmall,
                color = DarkSecondaryGold,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
        }
    }
}
