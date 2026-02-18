package com.aicallshield.ui

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aicallshield.data.model.CallStatus
import com.aicallshield.ui.screens.*
import com.aicallshield.ui.theme.AICallShieldTheme
import com.aicallshield.viewmodel.CallHistoryViewModel
import com.aicallshield.viewmodel.CallScreeningViewModel
import com.aicallshield.viewmodel.SettingsViewModel
import com.aicallshield.viewmodel.UserViewModel

/**
 * Main Activity — hosts the navigation and bottom bar.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            AICallShieldTheme {
                MainApp()
            }
        }
    }
}

/**
 * Navigation routes.
 */
sealed class Screen(val route: String, val label: String) {
    object Home : Screen("home", "Home")
    object History : Screen("history", "History")
    object Screening : Screen("screening/{callerNumber}", "Screening") {
        fun createRoute(number: String) = "screening/$number"
    }
    object CallDetail : Screen("call_detail/{callId}", "Detail") {
        fun createRoute(callId: String) = "call_detail/$callId"
    }
    object Settings : Screen("settings", "Settings")
    object PersonalDetails : Screen("personal_details", "Personal Details")
    object AssistantVoice : Screen("assistant_voice", "Assistant Voice")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
    val userViewModel: UserViewModel = viewModel()
    val userName by userViewModel.userName.collectAsState()
    val userGender by userViewModel.userGender.collectAsState()

    Scaffold(
        topBar = {
            // HomeScreen has its own custom top bar, so skip the default one
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    userName = userName,
                    onStartDemo = { number ->
                        navController.navigate(Screen.Screening.createRoute(number))
                    },
                    onViewHistory = {
                        navController.navigate(Screen.History.route)
                    },
                    onOpenSettings = {
                        navController.navigate(Screen.Settings.route) { popUpTo(Screen.Home.route) }
                    }
                )
            }

            composable(Screen.History.route) {
                val historyViewModel: CallHistoryViewModel = viewModel()
                val calls by historyViewModel.calls.collectAsState()
                val searchQuery by historyViewModel.searchQuery.collectAsState()
                val isLoading by historyViewModel.isLoading.collectAsState()

                CallHistoryScreen(
                    calls = calls,
                    searchQuery = searchQuery,
                    onSearchQueryChange = historyViewModel::onSearchQueryChange,
                    onCallClick = { call ->
                        historyViewModel.selectCall(call)
                        navController.navigate(Screen.CallDetail.createRoute(call.id))
                    },
                    onDeleteCall = historyViewModel::deleteCall,
                    isLoading = isLoading
                )
            }

            composable(Screen.Screening.route) { backStackEntry ->
                val callerNumber = backStackEntry.arguments?.getString("callerNumber") ?: "Unknown"
                val screeningViewModel: CallScreeningViewModel = viewModel()

                LaunchedEffect(callerNumber) {
                    screeningViewModel.startScreening(callerNumber)
                }

                val callStatus by screeningViewModel.callStatus.collectAsState()
                val messages by screeningViewModel.messages.collectAsState()
                val spamScore by screeningViewModel.currentSpamScore.collectAsState()
                val riskLevel by screeningViewModel.currentRiskLevel.collectAsState()
                val isProcessing by screeningViewModel.isAIProcessing.collectAsState()

                CallScreeningScreen(
                    callerNumber = callerNumber,
                    callStatus = callStatus,
                    messages = messages,
                    currentSpamScore = spamScore,
                    currentRiskLevel = riskLevel,
                    isAIProcessing = isProcessing,
                    onJoinCall = screeningViewModel::joinCall,
                    onBlockCaller = screeningViewModel::blockCaller,
                    onEndCall = {
                        screeningViewModel.endCall()
                        navController.popBackStack()
                    },
                    onSendMessage = screeningViewModel::sendMessage
                )
            }

            composable(Screen.CallDetail.route) { backStackEntry ->
                val historyViewModel: CallHistoryViewModel = viewModel()
                val selectedCall by historyViewModel.selectedCall.collectAsState()

                selectedCall?.let { call ->
                    CallDetailScreen(
                        call = call,
                        onBack = { navController.popBackStack() },
                        onBlockCaller = { historyViewModel.blockCaller(call) }
                    )
                }
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    userName = userName,
                    onBack = { navController.popBackStack() },
                    onOpenPersonalDetails = {
                        navController.navigate(Screen.PersonalDetails.route)
                    },
                    onOpenAssistantVoice = {
                        navController.navigate(Screen.AssistantVoice.route)
                    }
                )
            }

            composable(Screen.PersonalDetails.route) {
                PersonalDetailsScreen(
                    initialName = userName,
                    initialGender = userGender,
                    onBack = { navController.popBackStack() },
                    onConfirm = { name, gender ->
                        userViewModel.saveProfile(name, gender)
                    }
                )
            }

            composable(Screen.AssistantVoice.route) {
                val settingsViewModel: SettingsViewModel = viewModel()
                val selectedVoice by settingsViewModel.selectedVoice.collectAsState()

                AssistantVoiceScreen(
                    userName = userName,
                    initialVoice = selectedVoice,
                    onBack = { navController.popBackStack() },
                    onConfirm = { voice ->
                        settingsViewModel.updateVoice(voice)
                    }
                )
            }
        }
    }
}
