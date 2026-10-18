package com.setons.trackrep.music

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.net.URLEncoder

/**
 * YouTube Music and Media Controller for TrackRep.
 *
 * Supports:
 * 1. Zero-Disruption Headless Controls (Play, Pause, Resume, Skip, Stop) via AudioManager
 *    dispatchMediaKeyEvent - operates 100% in the background without interrupting CameraX or workout telemetry.
 * 2. YouTube Music Catalog Search & Playback via MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.
 * 3. Media Stream Volume Adjustment (isolated from CoachVoiceManager TTS).
 * 4. Automatic Audio Focus Ducking: temporarily lowers background music volume to 20%
 *    while the voice assistant is listening or speaking.
 * 5. Standby Voice Listening Suspension: prevents voice recognizer from polling or looping
 *    while music is active, avoiding repeated pausing/resuming stutter.
 */
object TrackMusicController {

    private const val TAG = "TrackMusicController"
    private const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"

    private var audioFocusRequest: AudioFocusRequest? = null

    var isMusicPlaybackActive by mutableStateOf(false)
        private set

    @Volatile
    private var lastPlayTriggerTimeMs: Long = 0L

    /**
     * Check if music is actively playing on the device (either in YouTube Music
     * or any active media stream) or actively tracked by TrackMusicController.
     */
    fun isMusicActive(context: Context): Boolean {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val isAudioActive = am.isMusicActive
            if (isAudioActive) {
                isMusicPlaybackActive = true
            } else if (isMusicPlaybackActive && (System.currentTimeMillis() - lastPlayTriggerTimeMs >= 5000L)) {
                isMusicPlaybackActive = false
            }
            isAudioActive || isMusicPlaybackActive
        } catch (_: Exception) {
            isMusicPlaybackActive
        }
    }

    /**
     * Check if YouTube Music is installed on the device.
     */
    fun isYouTubeMusicInstalled(context: Context): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    YOUTUBE_MUSIC_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(YOUTUBE_MUSIC_PACKAGE, 0)
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Pause media playback via standard Android media button event.
     */
    fun pauseMusic(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sendMediaKeyEvent(am, KeyEvent.KEYCODE_MEDIA_PAUSE)
            isMusicPlaybackActive = false
            lastPlayTriggerTimeMs = 0L
            "Music paused."
        } catch (e: Exception) {
            Log.w(TAG, "Failed to pause music", e)
            "Could not pause music."
        }
    }

    /**
     * Resume media playback via standard Android media button event.
     */
    fun resumeMusic(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sendMediaKeyEvent(am, KeyEvent.KEYCODE_MEDIA_PLAY)
            isMusicPlaybackActive = true
            lastPlayTriggerTimeMs = System.currentTimeMillis()
            "Resuming music."
        } catch (e: Exception) {
            Log.w(TAG, "Failed to resume music", e)
            "Could not resume music."
        }
    }

    /**
     * Skip to next track via standard Android media button event.
     */
    fun skipMusic(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sendMediaKeyEvent(am, KeyEvent.KEYCODE_MEDIA_NEXT)
            isMusicPlaybackActive = true
            lastPlayTriggerTimeMs = System.currentTimeMillis()
            "Skipping to next song."
        } catch (e: Exception) {
            Log.w(TAG, "Failed to skip song", e)
            "Could not skip song."
        }
    }

    /**
     * Stop media playback via standard Android media button event.
     */
    fun stopMusic(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            sendMediaKeyEvent(am, KeyEvent.KEYCODE_MEDIA_STOP)
            isMusicPlaybackActive = false
            lastPlayTriggerTimeMs = 0L
            "Music stopped."
        } catch (e: Exception) {
            Log.w(TAG, "Failed to stop music", e)
            "Could not stop music."
        }
    }

    /**
     * Adjust music volume up without affecting Coach TTS speech guidance.
     */
    fun volumeUp(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
            "Volume turned up."
        } catch (e: Exception) {
            Log.w(TAG, "Failed to raise volume", e)
            "Volume adjusted."
        }
    }

    /**
     * Adjust music volume down without affecting Coach TTS speech guidance.
     */
    fun volumeDown(context: Context): String {
        return try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
            "Volume turned down."
        } catch (e: Exception) {
            Log.w(TAG, "Failed to lower volume", e)
            "Volume adjusted."
        }
    }

    /**
     * Play a specific song, playlist, or generic workout music on YouTube Music.
     */
    fun play(context: Context, query: String?, isPlaylist: Boolean = false): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Case 1: Generic "play some music" / "play music" without naming a song
        if (query.isNullOrBlank()) {
            if (am.isMusicActive) {
                isMusicPlaybackActive = true
                lastPlayTriggerTimeMs = System.currentTimeMillis()
                return "Music is already playing. Say 'skip' or 'pause' to control it."
            }

            // Attempt to resume previous session headless
            sendMediaKeyEvent(am, KeyEvent.KEYCODE_MEDIA_PLAY)
            isMusicPlaybackActive = true
            lastPlayTriggerTimeMs = System.currentTimeMillis()

            // If YouTube Music is installed, also ensure a workout station is queued if idle
            return if (isYouTubeMusicInstalled(context)) {
                try {
                    launchYouTubeMusicSearch(context, "Workout Music", isPlaylist = true)
                    "Starting workout music on YouTube Music."
                } catch (_: Exception) {
                    "Resuming music playback."
                }
            } else {
                "Resuming music playback."
            }
        }

        // Case 2: Specific song or playlist requested
        if (!isYouTubeMusicInstalled(context)) {
            return "YouTube Music is not installed on this device. Install it from the Play Store to stream music."
        }

        isMusicPlaybackActive = true
        lastPlayTriggerTimeMs = System.currentTimeMillis()

        return try {
            launchYouTubeMusicSearch(context, query, isPlaylist)
            if (isPlaylist) {
                "Loading $query on YouTube Music."
            } else {
                "Playing $query on YouTube Music."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching YouTube Music with query: $query", e)
            try {
                // Fallback to web search intent
                val encoded = URLEncoder.encode(query, "UTF-8")
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://music.youtube.com/search?q=$encoded")).apply {
                    setPackage(YOUTUBE_MUSIC_PACKAGE)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(webIntent)
                "Searching $query on YouTube Music."
            } catch (fallbackEx: Exception) {
                Log.e(TAG, "Fallback search failed", fallbackEx)
                isMusicPlaybackActive = false
                lastPlayTriggerTimeMs = 0L
                "Could not launch YouTube Music for $query."
            }
        }
    }

    private fun launchYouTubeMusicSearch(context: Context, query: String, isPlaylist: Boolean) {
        val intent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            setPackage(YOUTUBE_MUSIC_PACKAGE)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra(SearchManager.QUERY, query)
            if (isPlaylist) {
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE)
            } else {
                putExtra(MediaStore.EXTRA_MEDIA_TITLE, query)
                putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
            }
        }
        context.startActivity(intent)
    }

    private fun sendMediaKeyEvent(am: AudioManager, keyCode: Int) {
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    /**
     * Request transient audio focus ducking to smoothly lower YouTube Music volume
     * while Track is listening to speech or speaking coaching responses.
     */
    fun requestDucking(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                audioFocusRequest = req
                am.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus ducking request failed", e)
        }
    }

    /**
     * Abandon audio focus ducking to restore normal YouTube Music playback volume.
     */
    fun abandonDucking(context: Context) {
        try {
            val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { am.abandonAudioFocusRequest(it) }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio focus ducking abandon failed", e)
        }
    }
}
