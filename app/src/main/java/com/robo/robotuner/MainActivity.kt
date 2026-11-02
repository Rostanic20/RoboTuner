package com.robo.robotuner

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.robo.robotuner.presentation.tuner.TunerScreen
import com.robo.robotuner.ui.theme.DarkBackground
import com.robo.robotuner.ui.theme.RoboTunerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            RoboTunerTheme {
                Scaffold(modifier = Modifier.fillMaxSize(), containerColor = DarkBackground) { innerPadding ->
                    TunerScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}
