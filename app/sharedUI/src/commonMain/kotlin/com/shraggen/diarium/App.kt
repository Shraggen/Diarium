package com.shraggen.diarium

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun App() {
    MaterialTheme {
        // 1. Initialize our Skeleton
        val kernel = remember {
            DiariumKernel(
                registeredTools = listOf(RecordInspectionTool()) // Injecting the Plugin!
            )
        }

        var outputLog by remember { mutableStateOf("Kernel Booted. Waiting for command...") }

        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Diarium Walking Skeleton",
                style = MaterialTheme.typography.headlineSmall
            )

            Text("Registered Tools: ${kernel.getAvailableTools().joinToString()}")

            // Simulate the LLM deciding to call a tool based on "I saw the queen in hive 4"
            Button(onClick = {
                outputLog = kernel.processSimulatedLlmCall(
                    toolName = "record_inspection",
                    arguments = mapOf("hive_id" to "4", "queen_seen" to "true")
                )
            }) {
                Text("Simulate LLM: 'I saw the queen in hive 4'")
            }

            // Simulate an invalid LLM hallucination
            Button(onClick = {
                outputLog = kernel.processSimulatedLlmCall(
                    toolName = "launch_nukes",
                    arguments = emptyMap()
                )
            }) {
                Text("Simulate LLM Hallucination")
            }

            // Output Console
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(16.dp)
            ) {
                Text(text = outputLog, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}