package com.setons.trackrep.screens.trackai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.setons.trackrep.ai.action.AdjustTargetAction
import com.setons.trackrep.ai.action.ModifyRoutineAction
import com.setons.trackrep.ai.action.ReplaceExerciseAction
import com.setons.trackrep.ai.action.RescheduleWorkoutAction
import com.setons.trackrep.ai.action.TrackAction
import com.setons.trackrep.ai.service.TrackAiService
import com.setons.trackrep.exercise.catalog.ExerciseCatalog
import com.setons.trackrep.screens.coach.CoachModeHolder
import com.setons.trackrep.theme.DarkPrimaryGold
import com.setons.trackrep.theme.DarkSecondaryGold
import com.setons.trackrep.theme.SuccessGreen
import kotlinx.coroutines.launch
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isFromUser: Boolean,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis(),
    val actions: List<TrackAction> = emptyList()
)

@Composable
fun TrackAiScreen(
    onNavigateToCoach: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var userPrompt by remember { mutableStateOf("") }
    var isThinking by remember { mutableStateOf(false) }
    var showApiKeyDialog by remember { mutableStateOf(false) }

    val messages = remember {
        mutableStateListOf(
            ChatMessage(
                isFromUser = false,
                text = "Hello! I'm Track, your adaptive AI fitness coach designed by Setons. I monitor your movement biomechanics, recovery windows, and progression targets.\n\nYou can ask me to swap exercises if you have joint discomfort, adjust targets, reschedule missed sessions, or explain form cues."
            )
        )
    }

    // Auto-scroll when new messages arrive
    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    fun submitQuery(promptText: String) {
        val query = promptText.trim()
        if (query.isBlank() || isThinking) return

        messages.add(ChatMessage(isFromUser = true, text = query))
        userPrompt = ""
        isThinking = true

        coroutineScope.launch {
            try {
                val response = TrackAiService.processMessage(context, query)
                messages.add(
                    ChatMessage(
                        isFromUser = false,
                        text = response.replyMessage,
                        actions = response.actions
                    )
                )
            } catch (e: Exception) {
                messages.add(
                    ChatMessage(
                        isFromUser = false,
                        text = "I encountered a minor processing error, but my safety layer has preserved your workout state."
                    )
                )
            } finally {
                isThinking = false
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(DarkPrimaryGold.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Track AI",
                        tint = DarkPrimaryGold,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column {
                    Text(
                        text = "Track AI Coach",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Powered by Gemini • Developed by Setons",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = DarkPrimaryGold
                    )
                }
            }

            IconButton(onClick = { showApiKeyDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "AI Settings",
                    tint = DarkPrimaryGold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick Suggestion Chips (Horizontal Scroller)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickChip("Wrist relief", "My wrists hurt. Can we swap standard push-ups?") { submitQuery(it) }
            QuickChip("Reschedule missed", "I missed yesterday's workout. Reschedule it without stacking.") { submitQuery(it) }
            QuickChip("+2 Push-up Reps", "Increase push-up target by 2 reps.") { submitQuery(it) }
            QuickChip("Fix hip sagging", "Why are my hips sagging in push-ups?") { submitQuery(it) }
            QuickChip("Phone setup", "How should I position my phone for tracking?") { submitQuery(it) }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Conversation Stream
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                if (msg.isFromUser) {
                    UserMessageBubble(text = msg.text)
                } else {
                    AiMessageCard(
                        message = msg,
                        onLaunchExerciseInCoach = { exerciseId ->
                            val ex = ExerciseCatalog.getById(exerciseId)
                            CoachModeHolder.pendingExerciseMode = ex?.framingMode
                            onNavigateToCoach()
                        }
                    )
                }
            }

            if (isThinking) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(8.dp)
                    ) {
                        CircularProgressIndicator(
                            color = DarkPrimaryGold,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Track is reasoning about your biomechanics...",
                            style = MaterialTheme.typography.bodySmall,
                            color = DarkPrimaryGold.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Box
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF181818),
            border = BorderStroke(1.dp, Color(0xFF333333)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = userPrompt,
                    onValueChange = { userPrompt = it },
                    placeholder = {
                        Text(
                            text = "Ask Track about workouts, form, or swaps...",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    maxLines = 3
                )

                IconButton(
                    onClick = { submitQuery(userPrompt) },
                    enabled = userPrompt.isNotBlank() && !isThinking,
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            if (userPrompt.isNotBlank()) DarkPrimaryGold else Color(0xFF2A2A2A),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (userPrompt.isNotBlank()) Color.Black else Color.Gray,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }

    // Gemini API Key Dialog
    if (showApiKeyDialog) {
        ApiKeyConfigDialog(
            currentKey = TrackAiService.getApiKey(context) ?: "",
            onSaveKey = { newKey ->
                TrackAiService.setApiKey(context, newKey)
                showApiKeyDialog = false
            },
            onDismiss = { showApiKeyDialog = false }
        )
    }
}

@Composable
private fun UserMessageBubble(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp),
            color = Color(0xFF2E2412),
            border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFF7E7C4),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun AiMessageCard(
    message: ChatMessage,
    onLaunchExerciseInCoach: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp),
            color = Color(0xFF161616),
            border = BorderStroke(1.dp, Color(0xFF2C2C2C)),
            modifier = Modifier.fillMaxWidth(0.92f)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Sender label
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(DarkPrimaryGold, CircleShape)
                    )
                    Text(
                        text = "TRACK AI",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimaryGold,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.92f),
                    lineHeight = 20.sp
                )

                // Render Action Confirmation Cards if actions were executed
                if (message.actions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        message.actions.forEach { action ->
                            ActionConfirmationCard(
                                action = action,
                                onLaunchExerciseInCoach = onLaunchExerciseInCoach
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionConfirmationCard(
    action: TrackAction,
    onLaunchExerciseInCoach: (String) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1F1F1F),
        border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.45f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = DarkPrimaryGold.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = DarkPrimaryGold,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "ACTION APPLIED",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkPrimaryGold
                        )
                    }
                }

                Text(
                    text = "Schema Verified",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when (action) {
                is ReplaceExerciseAction -> {
                    val oldEx = ExerciseCatalog.getById(action.oldExerciseId)
                    val newEx = ExerciseCatalog.getById(action.newExerciseId)
                    Text(
                        text = "Swapped '${oldEx?.name ?: action.oldExerciseId}' -> '${newEx?.name ?: action.newExerciseId}'",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Reason: ${action.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (newEx?.isVisionSupported == true) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onLaunchExerciseInCoach(newEx.id) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkPrimaryGold,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Launch in Coach", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is AdjustTargetAction -> {
                    val ex = ExerciseCatalog.getById(action.exerciseId)
                    Text(
                        text = "Adjusted Target for '${ex?.name ?: action.exerciseId}'",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = action.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                is RescheduleWorkoutAction -> {
                    Text(
                        text = "Workout Schedule Updated",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Shifted forward by ${action.daysOffset} day without volume stacking.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                is ModifyRoutineAction -> {
                    Text(
                        text = "Custom Routine Modified",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = action.summary,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }

                else -> {
                    Text(
                        text = action.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickChip(
    label: String,
    prompt: String,
    onClick: (String) -> Unit
) {
    Surface(
        onClick = { onClick(prompt) },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1E1E1E),
        border = BorderStroke(1.dp, Color(0xFF333333))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 11.sp,
            color = DarkPrimaryGold,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ApiKeyConfigDialog(
    currentKey: String,
    onSaveKey: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var keyText by remember { mutableStateOf(currentKey) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1C1C1C),
            border = BorderStroke(1.dp, DarkPrimaryGold.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, tint = DarkPrimaryGold)
                    Text(
                        text = "Gemini API Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Track AI can connect directly to Gemini 2.5 Flash. If left blank, Track operates seamlessly using its built-in on-device reasoning engine.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = keyText,
                    onValueChange = { keyText = it },
                    label = { Text("Gemini API Key (Optional)") },
                    placeholder = { Text("AIzaSy...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DarkPrimaryGold,
                        unfocusedBorderColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSaveKey(keyText) },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimaryGold, contentColor = Color.Black)
                    ) {
                        Text("Save Key", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
