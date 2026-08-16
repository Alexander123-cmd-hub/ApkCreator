package de.roboticmind.apkcreator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.roboticmind.apkcreator.core.designsystem.ApkCreatorTheme
import de.roboticmind.apkcreator.ui.BuildProfileScreen

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            ApkCreatorTheme {
                BuildProfileScreen()
            }
        }
    }
}
