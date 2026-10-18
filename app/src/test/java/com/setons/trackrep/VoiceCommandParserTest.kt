package com.setons.trackrep

import com.setons.trackrep.coach.VoiceCommandParser
import com.setons.trackrep.coach.VoiceCommandParser.VoiceIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {

    @Test
    fun testStopMusicDoesNotTriggerStopWorkout() {
        // Direct bug report verification: "stop music" should parse as StopMusic, NEVER StopWorkout
        val parsed = VoiceCommandParser.parseSingle("stop music")
        assertTrue("Expected StopMusic but was ${parsed.intent}", parsed.intent is VoiceIntent.StopMusic)
        assertEquals("Music stopped.", parsed.feedbackSpeech)

        val parsedThe = VoiceCommandParser.parseSingle("stop the music")
        assertTrue("Expected StopMusic but was ${parsedThe.intent}", parsedThe.intent is VoiceIntent.StopMusic)

        val parsedTurnOff = VoiceCommandParser.parseSingle("turn off music")
        assertTrue("Expected StopMusic but was ${parsedTurnOff.intent}", parsedTurnOff.intent is VoiceIntent.StopMusic)

        val parsedKill = VoiceCommandParser.parseSingle("kill the music")
        assertTrue("Expected StopMusic but was ${parsedKill.intent}", parsedKill.intent is VoiceIntent.StopMusic)
    }

    @Test
    fun testPauseMusicParsesCorrectly() {
        val parsed = VoiceCommandParser.parseSingle("pause music")
        assertTrue("Expected PauseMusic but was ${parsed.intent}", parsed.intent is VoiceIntent.PauseMusic)
        assertEquals("Music paused.", parsed.feedbackSpeech)

        val parsedThe = VoiceCommandParser.parseSingle("pause the music")
        assertTrue("Expected PauseMusic but was ${parsedThe.intent}", parsedThe.intent is VoiceIntent.PauseMusic)

        val parsedSong = VoiceCommandParser.parseSingle("pause song")
        assertTrue("Expected PauseMusic but was ${parsedSong.intent}", parsedSong.intent is VoiceIntent.PauseMusic)
    }

    @Test
    fun testResumeAndTurnOnMusicParsesCorrectly() {
        val parsedResume = VoiceCommandParser.parseSingle("resume music")
        assertTrue("Expected ResumeMusic but was ${parsedResume.intent}", parsedResume.intent is VoiceIntent.ResumeMusic)

        val parsedTurnOn = VoiceCommandParser.parseSingle("turn on music")
        assertTrue("Expected ResumeMusic but was ${parsedTurnOn.intent}", parsedTurnOn.intent is VoiceIntent.ResumeMusic)

        val parsedUnpause = VoiceCommandParser.parseSingle("unpause music")
        assertTrue("Expected ResumeMusic but was ${parsedUnpause.intent}", parsedUnpause.intent is VoiceIntent.ResumeMusic)
    }

    @Test
    fun testMusicVolumeControls() {
        val parsedUp = VoiceCommandParser.parseSingle("volume up")
        assertTrue(parsedUp.intent is VoiceIntent.AdjustVolume)
        assertTrue((parsedUp.intent as VoiceIntent.AdjustVolume).isUp)

        val parsedDown = VoiceCommandParser.parseSingle("turn volume down")
        assertTrue(parsedDown.intent is VoiceIntent.AdjustVolume)
        assertFalse((parsedDown.intent as VoiceIntent.AdjustVolume).isUp)
    }

    @Test
    fun testMusicSkipControls() {
        val parsed = VoiceCommandParser.parseSingle("skip this song")
        assertTrue(parsed.intent is VoiceIntent.SkipMusic)

        val parsedNext = VoiceCommandParser.parseSingle("next track")
        assertTrue(parsedNext.intent is VoiceIntent.SkipMusic)
    }

    @Test
    fun testPlayMusicControls() {
        val parsedGeneric = VoiceCommandParser.parseSingle("play some music")
        assertTrue(parsedGeneric.intent is VoiceIntent.PlayMusic)

        val parsedSong = VoiceCommandParser.parseSingle("play Worry on YouTube Music")
        assertTrue(parsedSong.intent is VoiceIntent.PlayMusic)
        val playIntent = parsedSong.intent as VoiceIntent.PlayMusic
        assertEquals("worry", playIntent.query?.lowercase())
    }

    @Test
    fun testWorkoutStopCommandsOnlyTriggerStopWorkout() {
        val parsedStopWorkout = VoiceCommandParser.parseSingle("stop workout")
        assertTrue("Expected StopWorkout but was ${parsedStopWorkout.intent}", parsedStopWorkout.intent is VoiceIntent.StopWorkout)
        assertEquals("Stopping set. Great effort!", parsedStopWorkout.feedbackSpeech)

        val parsedStopSet = VoiceCommandParser.parseSingle("stop set")
        assertTrue("Expected StopWorkout but was ${parsedStopSet.intent}", parsedStopSet.intent is VoiceIntent.StopWorkout)

        val parsedDone = VoiceCommandParser.parseSingle("i'm done")
        assertTrue("Expected StopWorkout but was ${parsedDone.intent}", parsedDone.intent is VoiceIntent.StopWorkout)

        val parsedBareStop = VoiceCommandParser.parseSingle("stop")
        assertTrue("Expected StopWorkout but was ${parsedBareStop.intent}", parsedBareStop.intent is VoiceIntent.StopWorkout)
    }

    @Test
    fun testWorkoutStartCommands() {
        val parsedStartWorkout = VoiceCommandParser.parseSingle("start workout")
        assertTrue(parsedStartWorkout.intent is VoiceIntent.StartWorkout)

        val parsedPushups = VoiceCommandParser.parseSingle("start push-ups")
        assertTrue(parsedPushups.intent is VoiceIntent.StartWorkout)
        val startIntent = parsedPushups.intent as VoiceIntent.StartWorkout
        assertEquals("push_up_standard", startIntent.targetExerciseId)
    }
}
