package com.aicallshield.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aicallshield.R
import com.aicallshield.ui.theme.*

/**
 * Personal Details screen — full screen for name & gender input.
 * Matches Equal AI design.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalDetailsScreen(
    initialName: String = "",
    initialGender: String = "Male",
    onBack: () -> Unit,
    onConfirm: (name: String, gender: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedGender by remember { mutableStateOf(initialGender) }

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
                text = "Personal Details",
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
                .padding(top = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Heading
            Text(
                text = "What should we call you",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Gray900,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enter your first name and\nselect your gender",
                style = MaterialTheme.typography.bodyMedium,
                color = Gray600,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Name input field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = {
                    Text(
                        text = "Your Name",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        color = Gray400
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Gray400,
                    unfocusedBorderColor = Gray200,
                    cursorColor = Green600
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Gender selection
            GenderOption(
                label = "Male",
                avatarRes = R.drawable.ic_avatar_male,
                isSelected = selectedGender == "Male",
                onClick = { selectedGender = "Male" }
            )

            Spacer(modifier = Modifier.height(12.dp))

            GenderOption(
                label = "Female",
                avatarRes = R.drawable.ic_avatar_female,
                isSelected = selectedGender == "Female",
                onClick = { selectedGender = "Female" }
            )
        }

        // ── Confirm Button ───────────────────────────────────────
        Button(
            onClick = {
                onConfirm(name, selectedGender)
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Green600),
            enabled = name.isNotBlank()
        ) {
            Text(
                text = "Confirm",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GenderOption(
    label: String,
    avatarRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) MintLight else Color.White
    val borderColor = if (isSelected) Green400 else Gray200

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = Green600,
                unselectedColor = Gray400
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Gray900,
            modifier = Modifier.weight(1f)
        )
        // Avatar illustration
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = avatarRes),
                contentDescription = label,
                modifier = Modifier.size(44.dp)
            )
        }
    }
}
