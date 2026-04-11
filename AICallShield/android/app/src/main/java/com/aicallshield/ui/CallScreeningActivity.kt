package com.aicallshield.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aicallshield.ui.screens.CallScreeningScreen
import com.aicallshield.ui.theme.AICallShieldTheme
import com.aicallshield.viewmodel.CallScreeningViewModel

/**
 * Activity launched when an incoming call triggers AI screening.
 *
 * This is shown as a full-screen overlay on the lock screen.
 * It displays the live call screening chat interface.
 */
class CallScreeningActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callerNumber = intent.getStringExtra("caller_number") ?: "Unknown"

        setContent {
            AICallShieldTheme {
                val viewModel: CallScreeningViewModel = viewModel()

                LaunchedEffect(callerNumber) {
                    viewModel.startScreening(callerNumber)
                }

                val callStatus by viewModel.callStatus.collectAsState()
                val messages by viewModel.messages.collectAsState()
                val spamScore by viewModel.currentSpamScore.collectAsState()
                val riskLevel by viewModel.currentRiskLevel.collectAsState()
                val isProcessing by viewModel.isAIProcessing.collectAsState()
                val isLocalMode by viewModel.isLocalMode.collectAsState()

                CallScreeningScreen(
                    callerNumber = callerNumber,
                    callStatus = callStatus,
                    messages = messages,
                    currentSpamScore = spamScore,
                    currentRiskLevel = riskLevel,
                    isAIProcessing = isProcessing,
                    isLocalMode = isLocalMode,
                    onJoinCall = viewModel::joinCall,
                    onBlockCaller = {
                        viewModel.blockCaller()
                        finish()
                    },
                    onEndCall = {
                        viewModel.endCall()
                        finish()
                    },
                    onSendMessage = viewModel::sendMessage
                )
            }
        }
    }
}
