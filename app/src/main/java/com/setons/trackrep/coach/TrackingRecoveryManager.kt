package com.setons.trackrep.coach

import com.setons.trackrep.pose.TrackedPose

/**
 * State of body tracking during an active workout set.
 */
sealed class TrackingState {
    object Tracking : TrackingState()
    data class Lost(val message: String, val lostDurationMs: Long) : TrackingState()
    data class Recovering(val countdownSeconds: Int) : TrackingState()
}

/**
 * Guards against camera occlusion, out-of-frame movement, or momentary tracking drops.
 * Pauses workout metrics when tracking is sustained lost (>1500ms) and guides the athlete
 * with a 3-second visual and audible countdown upon returning to frame.
 */
class TrackingRecoveryManager(
    private val onTrackingLost: () -> Unit = {},
    private val onTrackingRecovered: () -> Unit = {}
) {
    private var trackingLossStartTime: Long = 0L
    private var recoveryStartTime: Long = 0L
    private val LOSS_THRESHOLD_MS = 1500L
    private val RECOVERY_COUNTDOWN_TOTAL_MS = 3000L

    var currentState: TrackingState = TrackingState.Tracking
        private set

    /**
     * Inspects tracking validity from TrackedPose.
     */
    fun processFrame(pose: TrackedPose?): TrackingState {
        val isVisible = pose != null && pose.isTrackingValid
        return processFrame(isVisible)
    }

    /**
     * Core processing loop taking visibility directly (allowing pure JVM unit testing).
     */
    fun processFrame(isVisible: Boolean): TrackingState {
        val now = System.currentTimeMillis()

        when (val state = currentState) {
            is TrackingState.Tracking -> {
                if (!isVisible) {
                    if (trackingLossStartTime == 0L) {
                        trackingLossStartTime = now
                    } else if (now - trackingLossStartTime >= LOSS_THRESHOLD_MS) {
                        currentState = TrackingState.Lost(
                            message = "Athlete out of frame • Step back into camera view",
                            lostDurationMs = now - trackingLossStartTime
                        )
                        onTrackingLost()
                    }
                } else {
                    trackingLossStartTime = 0L
                }
            }
            is TrackingState.Lost -> {
                if (isVisible) {
                    // Body re-acquired, begin 3-second recovery countdown
                    recoveryStartTime = now
                    currentState = TrackingState.Recovering(countdownSeconds = 3)
                } else {
                    currentState = TrackingState.Lost(
                        message = "Athlete out of frame • Step back into camera view",
                        lostDurationMs = now - trackingLossStartTime
                    )
                }
            }
            is TrackingState.Recovering -> {
                if (!isVisible) {
                    // Lost again during countdown
                    trackingLossStartTime = now
                    recoveryStartTime = 0L
                    currentState = TrackingState.Lost(
                        message = "Tracking lost again • Align body with camera",
                        lostDurationMs = 0L
                    )
                    onTrackingLost()
                } else {
                    val elapsedRecovery = now - recoveryStartTime
                    val remainingSeconds = (((RECOVERY_COUNTDOWN_TOTAL_MS - elapsedRecovery) / 1000L) + 1).toInt()
                    if (remainingSeconds <= 0) {
                        currentState = TrackingState.Tracking
                        trackingLossStartTime = 0L
                        recoveryStartTime = 0L
                        onTrackingRecovered()
                    } else {
                        currentState = TrackingState.Recovering(countdownSeconds = remainingSeconds.coerceIn(1, 3))
                    }
                }
            }
        }

        return currentState
    }

    fun reset() {
        currentState = TrackingState.Tracking
        trackingLossStartTime = 0L
        recoveryStartTime = 0L
    }
}
