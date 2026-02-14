package com.aicallshield.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aicallshield.ui.theme.*

/**
 * Settings screen for configuring AI screening behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    isAIScreeningEnabled: Boolean,
    onAIScreeningToggle: (Boolean) -> Unit,
    isAutoBlockEnabled: Boolean,
    onAutoBlockToggle: (Boolean) -> Unit,
    spamThreshold: Float,
    onSpamThresholdChange: (Float) -> Unit,
    selectedVoice: String,
    onVoiceChange: (String) -> Unit,
    serverStatus: String,
    onTestConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // ── AI Screening ─────────────────────────────────────────────
        SettingsSection(title = "🤖 AI Screening") {
            SettingsToggle(
                title = "Enable AI Screening",
                description = "Automatically screen unknown calls with AI",
                checked = isAIScreeningEnabled,
                onCheckedChange = onAIScreeningToggle
            )

            SettingsToggle(
                title = "Auto-Block Spam",
                description = "Automatically block calls above spam threshold",
                checked = isAutoBlockEnabled,
                onCheckedChange = onAutoBlockToggle
            )
        }

        // ── Spam Detection ───────────────────────────────────────────
        SettingsSection(title = "🛡️ Spam Detection") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Spam Threshold: ${(spamThreshold * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Calls above this score will be flagged as spam",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
                Spacer(modifier = Modifier.height(8.dp))
                Slider(
                    value = spamThreshold,
                    onValueChange = onSpamThresholdChange,
                    valueRange = 0.3f..0.9f,
                    steps = 6,
                    colors = SliderDefaults.colors(
                        thumbColor = Blue500,
                        activeTrackColor = Blue500
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Sensitive (30%)", style = MaterialTheme.typography.labelSmall, color = Gray600)
                    Text("Strict (90%)", style = MaterialTheme.typography.labelSmall, color = Gray600)
                }
            }
        }

        // ── Voice Settings ───────────────────────────────────────────
        SettingsSection(title = "🔊 AI Voice") {
            val voices = listOf("alloy", "echo", "fable", "onyx", "nova", "shimmer")
            voices.forEach { voice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedVoice == voice,
                        onClick = { onVoiceChange(voice) },
                        colors = RadioButtonDefaults.colors(selectedColor = Blue500)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = voice.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ── Server Connection ────────────────────────────────────────
        SettingsSection(title = "🌐 Server Connection") {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Server Status",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = serverStatus,
                            style = MaterialTheme.typography.bodySmall,
                            color = when (serverStatus) {
                                "Connected" -> Green500
                                "Disconnected" -> Red500
                                else -> Gray600
                            }
                        )
                    }
                    OutlinedButton(onClick = onTestConnection) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Test")
                    }
                }
            }
        }

        // ── About ────────────────────────────────────────────────────
        SettingsSection(title = "ℹ️ About") {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AICallShield v1.0.0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Intelligent AI Call Screening Assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "⚠ Due to Android system-level restrictions, this project implements " +
                            "a prototype AI call screening model using supported Android APIs and " +
                            "simulated routing where required.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Blue500,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Gray600
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = Blue500,
            )
        )
    }
}
