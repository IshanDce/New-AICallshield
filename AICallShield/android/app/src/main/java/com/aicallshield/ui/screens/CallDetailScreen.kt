package com.aicallshield.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aicallshield.data.model.CallRecord
import com.aicallshield.ui.components.ChatBubble
import com.aicallshield.ui.components.RiskLevelChip
import com.aicallshield.ui.components.SpamScoreIndicator
import com.aicallshield.ui.theme.*

/**
 * Detailed view of a call record with full transcript, summary, and stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    call: CallRecord,
    onBack: () -> Unit,
    onBlockCaller: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // ── Top Bar ──────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = call.callerName ?: call.callerNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = call.callerNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (!call.isBlocked) {
                    IconButton(onClick = onBlockCaller) {
                        Icon(Icons.Default.Block, contentDescription = "Block", tint = Red500)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Spam Score ───────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Risk Assessment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        SpamScoreIndicator(score = call.spamScore)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RiskLevelChip(riskLevel = call.riskLevel)

                            if (call.scamKeywordsFound.isNotEmpty()) {
                                Text(
                                    text = "Keywords: ${call.scamKeywordsFound.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SpamHigh
                                )
                            }
                        }
                    }
                }
            }

            // ── AI Summary ───────────────────────────────────────────
            call.aiSummary?.let { summary ->
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Blue500,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "AI Summary",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // ── Call Info ────────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Call Details",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        CallInfoRow("Status", call.status.name)
                        call.sentiment?.let { CallInfoRow("Sentiment", it) }
                        call.durationSeconds?.let {
                            CallInfoRow("Duration", "${it / 60}m ${it % 60}s")
                        }
                        CallInfoRow("Messages", call.transcript.size.toString())
                        if (call.isBlocked) {
                            CallInfoRow("Blocked", "Yes 🚫")
                        }
                    }
                }
            }

            // ── Full Transcript ──────────────────────────────────────
            item {
                Text(
                    text = "📝 Full Transcript",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            items(call.transcript) { message ->
                ChatBubble(message = message)
            }

            if (call.transcript.isEmpty()) {
                item {
                    Text(
                        text = "No transcript available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Gray600,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun CallInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Gray600
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}
