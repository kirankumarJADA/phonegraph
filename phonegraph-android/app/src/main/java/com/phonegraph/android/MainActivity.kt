package com.phonegraph.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.phonegraph.android.ui.RecommendScreen
import com.phonegraph.android.ui.theme.PhoneGraphTheme
import com.phonegraph.android.viewmodel.PhoneRecommendViewModel

/**
 * PhoneGraph Android Client — minimal demonstration app.
 *
 * Single Activity hosting a single Compose screen, per the proposal's
 * scope (single screen, TextField, Ask button, calls backend, displays
 * recommendation, loading indicator, error handling). This class only
 * wires the ViewModel to the screen; all real logic lives in
 * PhoneRecommendViewModel and PhoneGraphRepository.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneGraphTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val viewModel: PhoneRecommendViewModel = viewModel()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                    RecommendScreen(
                        uiState = uiState,
                        onAskClick = { query -> viewModel.askQuestion(query) }
                    )
                }
            }
        }
    }
}
