package com.example.kotlinlibraryapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.kotlinlibraryapp.ui.navigation.LibraryApp
import com.example.kotlinlibraryapp.ui.theme.KotlinLibraryAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KotlinLibraryAppTheme {
                // LibraryApp obtains the Activity-scoped LibraryViewModel via viewModel(),
                // so state survives rotation and other configuration changes.
                LibraryApp()
            }
        }
    }
}
