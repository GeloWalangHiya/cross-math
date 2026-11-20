package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.CrossMathScreen
import com.example.ui.CrossMathViewModel
import com.example.ui.theme.CrossMathTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: CrossMathViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            CrossMathTheme(themeMode = uiState.preferences.themeMode) {
                CrossMathScreen(viewModel = viewModel)
            }
        }
    }
}
