package com.setons.trackrep

import com.setons.trackrep.navigation.CoachNavKey
import com.setons.trackrep.navigation.HistoryNavKey
import com.setons.trackrep.navigation.HomeNavKey
import com.setons.trackrep.navigation.ProfileNavKey
import com.setons.trackrep.navigation.TrackAiNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ThemeAndNavigationTest {

    @Test
    fun testNavigationKeysExist() {
        val keys = listOf(HomeNavKey, CoachNavKey, TrackAiNavKey, HistoryNavKey, ProfileNavKey)
        assertEquals(5, keys.size)
        keys.forEach { key ->
            assertNotNull(key)
        }
    }
}
