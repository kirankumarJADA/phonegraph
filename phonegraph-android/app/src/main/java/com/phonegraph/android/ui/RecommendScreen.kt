package com.phonegraph.android.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.phonegraph.android.viewmodel.RecommendUiState

/**
 * The single screen described in the proposal: a question field, an Ask
 * button, and a result area that shows a loading indicator while waiting,
 * an error message if something goes wrong, or the recommendation once
 * it arrives. Deliberately minimal — this exists to demonstrate PhoneGraph
 * has a working mobile client end-to-end, not to be a polished product UI.
 *
 * State-hoisted: this Composable owns only the text field's own input
 * value; everything about the actual request/response lifecycle comes
 * from the ViewModel via uiState, and is reported back via onAskClick.
 * That separation is what makes this testable without a real ViewModel.
 */
@Composable
fun RecommendScreen(
    uiState: RecommendUiState,
    onAskClick: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = "PhoneGraph",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ask about phones, backed by a knowledge-graph + LLM pipeline.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Ask a question") },
            placeholder = { Text("e.g. best phone under \$500") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = { onAskClick(query) },
            enabled = uiState !is RecommendUiState.Loading,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Ask")
        }

        Spacer(modifier = Modifier.height(24.dp))

        when (uiState) {
            is RecommendUiState.Idle -> {
                // Nothing to show yet.
            }

            is RecommendUiState.Loading -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Asking PhoneGraph…")
                }
            }

            is RecommendUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = uiState.message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }

            is RecommendUiState.Success -> {
                val response = uiState.response

                if (response.hallucinationDetected) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = "⚠ Possible unverified claim in this answer.",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Text(
                    text = response.answer,
                    style = MaterialTheme.typography.bodyLarge
                )

                if (response.candidatePhones.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Candidates considered:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = response.candidatePhones.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
