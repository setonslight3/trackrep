package com.setons.trackrep

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            enableEdgeToEdge()
            setContent {
                TrackRepApp()
            }
        } catch (t: Throwable) {
            android.util.Log.e("TrackRep", "Error in MainActivity onCreate", t)
        }
    }
}
