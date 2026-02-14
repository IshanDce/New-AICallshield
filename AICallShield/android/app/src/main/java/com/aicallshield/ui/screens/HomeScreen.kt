package com.aicallshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallshield.ui.theme.*

/**
 * Home screen — app overview, quick demo launcher, and feature showcase.
 */
@Composable
fun HomeScreen(
    onStartDemo: (String) -> Unit,
    onViewHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var demoNumber by remember { mutableStateOf("+1 (555) 123-4567") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ── Hero Banner ──────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Blue500, Blue700)
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "🛡️",
                        fontSize = 48.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AICallShield",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Intelligent AI Call Screening Assistant",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "AI screens your unknown calls, detects spam,\nand lets you take over anytime.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ── Demo Launcher ────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🧪 Demo Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Simulate an incoming call to test AI screening",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = demoNumber,
                    onValueChange = { demoNumber = it },
                    label = { Text("Caller Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = { onStartDemo(demoNumber) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Green500)
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Start AI Screening Demo",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Quick demo scenarios
                Text(
                    text = "Quick scenarios:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Gray600
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AssistChip(
                        onClick = { onStartDemo("+1 (555) 000-SPAM") },
                        label = { Text("🚨 Spam", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { onStartDemo("+1 (555) 000-SCAM") },
                        label = { Text("⚠ Scam", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    AssistChip(
                        onClick = { onStartDemo("+1 (555) 000-SAFE") },
                        label = { Text("✅ Safe", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Feature Cards ────────────────────────────────────────────
        Text(
            text = "Features",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )

        val features = listOf(
            Triple("🤖", "AI Auto Screening", "AI answers unknown calls and screens them for you"),
            Triple("📝", "Real-Time Transcription", "See what the caller is saying in real-time"),
            Triple("🛡️", "Spam Detection", "AI detects scam keywords and assigns risk scores"),
            Triple("👤", "Call Takeover", "Join the call anytime and continue the conversation"),
            Triple("📊", "AI Summaries", "Get post-call summaries and recommended actions"),
            Triple("💾", "Chat History", "All conversations stored with search functionality"),
        )

        features.forEach { (emoji, title, description) ->
            FeatureCard(emoji = emoji, title = title, description = description)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        // View History Button
        OutlinedButton(
            onClick = onViewHistory,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.History, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("View Call History")
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureCard(emoji: String, title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
            }
        }
    }
}
