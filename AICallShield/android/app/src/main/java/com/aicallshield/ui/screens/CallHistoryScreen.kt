package com.aicallshield.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.aicallshield.data.model.CallRecord
import com.aicallshield.data.model.CallStatus
import com.aicallshield.data.model.RiskLevel
import com.aicallshield.ui.components.RiskLevelChip
import com.aicallshield.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Call History screen showing past screened calls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryScreen(
    calls: List<CallRecord>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCallClick: (CallRecord) -> Unit,
    onDeleteCall: (CallRecord) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Search Bar ───────────────────────────────────────────────
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search calls, numbers, transcripts...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(28.dp),
            singleLine = true,
        )

        // ── Stats Summary ────────────────────────────────────────────
        if (calls.isNotEmpty()) {
            CallStatsSummary(calls = calls)
        }

        // ── Call List ────────────────────────────────────────────────
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (calls.isEmpty()) {
            EmptyCallHistory()
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calls, key = { it.id }) { call ->
                    CallHistoryItem(
                        call = call,
                        onClick = { onCallClick(call) },
                        onDelete = { onDeleteCall(call) }
                    )
                }
            }
        }
    }
}

/**
 * Summary stats card.
 */
@Composable
private fun CallStatsSummary(calls: List<CallRecord>) {
    val totalCalls = calls.size
    val spamCalls = calls.count { it.spamScore >= 0.65f }
    val blockedCalls = calls.count { it.isBlocked }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatChip(
            label = "Total",
            value = totalCalls.toString(),
            color = Blue500,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Spam",
            value = spamCalls.toString(),
            color = SpamHigh,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label = "Blocked",
            value = blockedCalls.toString(),
            color = Red500,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Individual call history item card.
 */
@Composable
private fun CallHistoryItem(
    call: CallRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusIcon = when (call.status) {
        CallStatus.SCREENING -> "🤖"
        CallStatus.USER_JOINED -> "👤"
        CallStatus.ENDED -> "✅"
        CallStatus.BLOCKED -> "🚫"
    }

    val spamColor = when {
        call.spamScore >= 0.8f -> SpamCritical
        call.spamScore >= 0.6f -> SpamHigh
        call.spamScore >= 0.35f -> SpamMedium
        else -> SpamLow
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Caller avatar with spam indicator
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(spamColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = statusIcon,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Call info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = call.callerName ?: call.callerNumber,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatDate(call.startTime),
                        style = MaterialTheme.typography.labelSmall,
                        color = Gray600
                    )
                }

                if (call.callerName != null) {
                    Text(
                        text = call.callerNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = Gray600
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Summary or last message preview
                Text(
                    text = call.aiSummary ?: call.transcript.lastOrNull()?.text ?: "No transcript",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Risk & Duration row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RiskLevelChip(riskLevel = call.riskLevel)

                    call.durationSeconds?.let { duration ->
                        Text(
                            text = "⏱ ${formatDuration(duration)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gray600
                        )
                    }

                    if (call.isBlocked) {
                        Text(
                            text = "🚫 Blocked",
                            style = MaterialTheme.typography.labelSmall,
                            color = Red500,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Gray400
                )
            }
        }
    }
}

/**
 * Empty state for call history.
 */
@Composable
private fun EmptyCallHistory() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🛡️", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No calls yet",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Screened calls will appear here",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600
            )
        }
    }
}

// ── Utility ──────────────────────────────────────────────────────────

private fun formatDate(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000 -> "Just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        diff < 604_800_000 -> SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatDuration(seconds: Int): String {
    val min = seconds / 60
    val sec = seconds % 60
    return if (min > 0) "${min}m ${sec}s" else "${sec}s"
}
