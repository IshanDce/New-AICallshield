package com.aicallshield.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallshield.data.model.*
import com.aicallshield.ui.components.ChatBubble
import com.aicallshield.ui.components.SpamScoreIndicator
import com.aicallshield.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Real-time call screening screen.
 * Shows live chat, AI status, spam alerts, and user action buttons.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallScreeningScreen(
    callerNumber: String,
    callStatus: CallStatus,
    messages: List<ChatMessage>,
    currentSpamScore: Float,
    currentRiskLevel: RiskLevel,
    isAIProcessing: Boolean,
    isLocalMode: Boolean,
    onJoinCall: () -> Unit,
    onBlockCaller: () -> Unit,
    onEndCall: () -> Unit,
    onSendMessage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var userMessage by remember { mutableStateOf("") }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ── Top Bar ──────────────────────────────────────────────────
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = callerNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = when (callStatus) {
                            CallStatus.SCREENING -> "🤖 AI Screening in Progress"
                            CallStatus.USER_JOINED -> "👤 You joined the call"
                            CallStatus.ENDED -> "📞 Call Ended"
                            CallStatus.BLOCKED -> "🚫 Caller Blocked"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = when (callStatus) {
                            CallStatus.SCREENING -> Blue300
                            CallStatus.USER_JOINED -> Green500
                            CallStatus.ENDED -> Gray600
                            CallStatus.BLOCKED -> Red500
                        }
                    )
                    if (isLocalMode) {
                        Text(
                            text = "📱 Local protection mode",
                            style = MaterialTheme.typography.bodySmall,
                            color = Orange500
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            actions = {
                // End call button
                IconButton(
                    onClick = onEndCall,
                    enabled = callStatus != CallStatus.ENDED && callStatus != CallStatus.BLOCKED
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        tint = Red500
                    )
                }
            }
        )

        AnimatedVisibility(
            visible = isLocalMode,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Orange500.copy(alpha = 0.12f)
                )
            ) {
                Text(
                    text = "Cloud AI is unavailable. The app is still protecting you with on-device scam rules and local call history.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray900,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Spam Score Bar ───────────────────────────────────────────
        AnimatedVisibility(
            visible = currentSpamScore > 0.2f,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        currentSpamScore >= 0.8f -> SpamCritical.copy(alpha = 0.08f)
                        currentSpamScore >= 0.6f -> SpamHigh.copy(alpha = 0.08f)
                        else -> SpamMedium.copy(alpha = 0.08f)
                    }
                )
            ) {
                SpamScoreIndicator(
                    score = currentSpamScore,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // ── Chat Messages ────────────────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id.ifEmpty { it.hashCode().toString() } }) { message ->
                ChatBubble(message = message)
            }

            // AI typing indicator
            if (isAIProcessing && callStatus == CallStatus.SCREENING) {
                item {
                    AITypingIndicator()
                }
            }
        }

        // ── Action Buttons ───────────────────────────────────────────
        when (callStatus) {
            CallStatus.SCREENING -> {
                ScreeningActions(
                    onJoinCall = onJoinCall,
                    onBlockCaller = onBlockCaller,
                    onEndCall = onEndCall
                )
            }
            CallStatus.USER_JOINED -> {
                UserMessageInput(
                    value = userMessage,
                    onValueChange = { userMessage = it },
                    onSend = {
                        if (userMessage.isNotBlank()) {
                            onSendMessage(userMessage)
                            userMessage = ""
                        }
                    }
                )
            }
            else -> {
                // Call ended/blocked — show summary prompt
                CallEndedBar()
            }
        }
    }
}

/**
 * Action buttons during AI screening.
 */
@Composable
private fun ScreeningActions(
    onJoinCall: () -> Unit,
    onBlockCaller: () -> Unit,
    onEndCall: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Block
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(
                    onClick = onBlockCaller,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Red500.copy(alpha = 0.12f)
                    )
                ) {
                    Icon(Icons.Default.Block, contentDescription = "Block", tint = Red500)
                }
                Text("Block", style = MaterialTheme.typography.labelSmall)
            }

            // Join Call - Primary action
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FloatingActionButton(
                    onClick = onJoinCall,
                    containerColor = Green500,
                    modifier = Modifier.size(64.dp)
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = "Join Call",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Join Call",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Green500
                )
            }

            // End
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalIconButton(
                    onClick = onEndCall,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = Gray200
                    )
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "End", tint = Gray600)
                }
                Text("End", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

/**
 * Message input bar when user has joined the call.
 */
@Composable
private fun UserMessageInput(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Type a message...") },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Blue500,
                    unfocusedBorderColor = Gray400
                )
            )

            Spacer(modifier = Modifier.width(8.dp))

            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(48.dp),
                containerColor = Blue500
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

/**
 * Bar shown when call has ended.
 */
@Composable
private fun CallEndedBar() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Gray100
    ) {
        Text(
            text = "📞 Call ended — Check the summary in call history",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = Gray600
        )
    }
}

/**
 * AI typing/processing indicator.
 */
@Composable
private fun AITypingIndicator() {
    Row(
        modifier = Modifier
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "🤖",
            fontSize = 16.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(AIBubble)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Gray400)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI is thinking...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Gray600
                )
            }
        }
    }
}
