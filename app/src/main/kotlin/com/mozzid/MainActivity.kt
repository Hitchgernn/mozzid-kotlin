package com.mozzid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.mozzid.presentation.HomeScreen
import com.mozzid.presentation.theme.AppAccent
import com.mozzid.presentation.theme.MozzTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MozzTheme(accent = AppAccent.TEAL) {
                // Build the graph once, off the UI construction path.
                val boot by produceState<Bootstrap?>(initialValue = null) {
                    value = Bootstrap.create(applicationContext)
                }
                boot?.let { HomeScreen(it) }
            }
        }
    }
}
