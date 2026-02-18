package com.aicallshield.ui.screens

import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallshield.R
import com.aicallshield.ui.theme.*
import java.util.Locale

// ═════════════════════════════════════════════════════════════════
// Voice data model
// ═════════════════════════════════════════════════════════════════

/**
 * Represents one selectable assistant voice.
 *
 * @param id   Unique key persisted in settings (maps to OpenAI voice id).
 * @param name Human-readable name shown under the avatar.
 * @param gender "male" or "female" — drives emoji & accent color.
 * @param description Short tagline shown beneath the name.
 */
data class AssistantVoice(
    val id: String,
    val name: String,
    val gender: String,
    val description: String,
    val avatarRes: Int,        // R.drawable.ic_avatar_male / female
    val pitch: Float,          // TTS pitch: < 1 = deeper, > 1 = higher
    val speechRate: Float      // TTS speed
)

/** The two built-in assistant voices. */
val availableVoices = listOf(
    AssistantVoice(
        id = "alloy",
        name = "James",
        gender = "male",
        description = "Calm & professional",
        avatarRes = R.drawable.ic_avatar_male,
        pitch = 0.85f,
        speechRate = 0.95f
    ),
    AssistantVoice(
        id = "nova",
        name = "Sophia",
        gender = "female",
        description = "Warm & friendly",
        avatarRes = R.drawable.ic_avatar_female,
        pitch = 1.3f,
        speechRate = 1.0f
    )
)

// ═════════════════════════════════════════════════════════════════
// Screen
// ═════════════════════════════════════════════════════════════════

/**
 * Assistant Voice selection screen — Equal AI inspired design.
 *
 * Layout:
 *  1. Back arrow + "Assistant Voice" title
 *  2. "Select Assistant Voice" heading + subtitle
 *  3. Two named voice avatars side by side (James / Sophia)
 *  4. Voice preview card with play button
 *  5. Green "Confirm" button
 */
@Composable
fun AssistantVoiceScreen(
    userName: String = "",
    initialVoice: String = "alloy",
    onBack: () -> Unit,
    onConfirm: (voiceId: String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedId by remember { mutableStateOf(initialVoice) }
    val displayName = userName.ifBlank { "User" }
    val currentVoice = availableVoices.firstOrNull { it.id == selectedId } ?: availableVoices[0]

    // ── Text-to-Speech engine ────────────────────────────────────
    val context = LocalContext.current
    var isSpeaking by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }

    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale.US
                ttsReady = true
            }
        }
        engine
    }

    // Listen for utterance completion to flip isSpeaking back to false
    LaunchedEffect(tts) {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { /* already set */ }
            override fun onDone(utteranceId: String?) { isSpeaking = false }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) { isSpeaking = false }
        })
    }

    // Clean up TTS when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            tts?.stop()
            tts?.shutdown()
        }
    }

    // Build the preview sentence from the selected voice
    val previewSentence = "Hi $displayName! This is ${currentVoice.name}, your AI Call Shield Assistant."

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ── Top Bar ──────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Gray900
                )
            }
            Text(
                text = "Assistant Voice",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Gray900
            )
        }

        // ── Content ──────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 24.dp)
                .padding(top = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading
            Text(
                text = "Select Assistant Voice",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Gray900,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "How would you like your assistant\nto sound?",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(36.dp))

            // ── Voice Avatars ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                availableVoices.forEachIndexed { index, voice ->
                    if (index > 0) {
                        // vertical divider between cards
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(120.dp)
                                .background(Gray200)
                        )
                    }
                    VoiceAvatar(
                        avatarRes = voice.avatarRes,
                        name = voice.name,
                        description = voice.description,
                        isSelected = selectedId == voice.id,
                        onClick = { selectedId = voice.id }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Voice Preview Card ───────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3EDF7)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Voice name badge
                    Text(
                        text = currentVoice.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Green600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Audio waveform indicator (visual placeholder)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val heights = listOf(8, 16, 24, 32, 24, 16, 8)
                        heights.forEach { h ->
                            Box(
                                modifier = Modifier
                                    .width(5.dp)
                                    .height(h.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Green600)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Preview text
                    Text(
                        text = "\"Hi $displayName! This is ${currentVoice.name},",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Gray900,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "your AICallShield Assistant.\"",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = Gray900,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Play / Stop button
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) Color(0xFFB71C1C) else Gray900)
                            .clickable {
                                if (isSpeaking) {
                                    tts?.stop()
                                    isSpeaking = false
                                } else if (ttsReady) {
                                    tts?.setPitch(currentVoice.pitch)
                                    tts?.setSpeechRate(currentVoice.speechRate)
                                    tts?.speak(
                                        previewSentence,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "voice_preview"
                                    )
                                    isSpeaking = true
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpeaking) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = if (isSpeaking) "Stop preview" else "Play preview",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // ── Confirm Button ───────────────────────────────────────
        Button(
            onClick = {
                onConfirm(selectedId)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600)
        ) {
            Text(
                text = "Confirm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════
// Sub-components
// ═════════════════════════════════════════════════════════════════

@Composable
private fun VoiceAvatar(
    avatarRes: Int,
    name: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            // Avatar illustration
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(3.dp, Green600, CircleShape)
                        else Modifier.border(2.dp, Gray200, CircleShape)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = avatarRes),
                    contentDescription = name,
                    modifier = Modifier.size(96.dp),
                    colorFilter = if (!isSelected)
                        ColorFilter.tint(Color.Gray.copy(alpha = 0.5f), androidx.compose.ui.graphics.BlendMode.SrcAtop)
                    else null
                )
            }

            // Green checkmark for selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .offset(y = 8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Green600),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Gray900 else Gray600
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = if (isSelected) Green600 else Gray400
        )
    }
}
