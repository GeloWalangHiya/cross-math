package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.R
import com.example.engine.CrossMathPuzzle
import com.example.engine.Difficulty
import com.example.engine.EvaluationMode
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCrossMathColors
import com.example.ui.theme.ThemeMode

@Composable
fun VictoryDialog(
    puzzle: CrossMathPuzzle,
    elapsedSeconds: Int,
    moves: Int,
    hintsUsed: Int,
    maxComboStreak: Int,
    onPlayAgain: () -> Unit,
    onNextDifficulty: () -> Unit,
    onDismiss: () -> Unit
) {
    val semantic = LocalCrossMathColors.current
    val stars = when {
        hintsUsed == 0 && maxComboStreak >= 2 -> 3
        hintsUsed <= 1 -> 2
        else -> 1
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_victory_trophy),
                    contentDescription = "Puzzle solved celebration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 1..3) {
                        Icon(
                            imageVector = if (i <= stars) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (i <= stars) "Earned star" else "Empty star",
                            tint = if (i <= stars) semantic.streak else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (puzzle.isDailyChallenge) "Daily Challenge Complete!" else "Puzzle Solved!",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${puzzle.difficulty.label} • ${puzzle.evaluationMode.shortTag}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatBadge(
                        label = "Time",
                        value = formatDuration(elapsedSeconds),
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Moves",
                        value = moves.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Streak",
                        value = "${maxComboStreak}×",
                        modifier = Modifier.weight(1f)
                    )
                    StatBadge(
                        label = "Hints",
                        value = hintsUsed.toString(),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onPlayAgain,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("victory_new_puzzle_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Play Another Puzzle")
                }

                if (puzzle.difficulty != Difficulty.HARD && !puzzle.isDailyChallenge) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onNextDifficulty,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("victory_next_difficulty_button"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        val nextLabel = if (puzzle.difficulty == Difficulty.EASY) "Medium" else "Hard"
                        Text("Step Up to $nextLabel")
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("victory_inspect_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Inspect Board")
                }
            }
        }
    }
}

@Composable
private fun StatBadge(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesAndTwistsSheet(
    evaluationMode: EvaluationMode,
    allowNegatives: Boolean,
    themeMode: ThemeMode,
    totalSolved: Int,
    onSelectEvaluationMode: (EvaluationMode) -> Unit,
    onToggleAllowNegatives: (Boolean) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Rules & Game Twists",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "Puzzles solved: $totalSolved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // Order of Operations Mode
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Equation Evaluation Rule",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EvaluationMode.entries.forEach { mode ->
                        FilterChip(
                            selected = evaluationMode == mode,
                            onClick = { onSelectEvaluationMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("eval_mode_${mode.name}")
                        )
                    }
                }
                Text(
                    text = evaluationMode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Negative Numbers Twist
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Negative Numbers Twist",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Include negative integers and signed answers in newly generated puzzles.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Switch(
                    checked = allowNegatives,
                    onCheckedChange = onToggleAllowNegatives,
                    modifier = Modifier.testTag("negatives_switch")
                )
            }

            // Theme selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Studio Theme",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = themeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            label = { Text(mode.label) },
                            modifier = Modifier.testTag("theme_${mode.name}")
                        )
                    }
                }
            }

            HorizontalDivider()

            // How to Play
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "How to Play",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "• Tap any blank number or operator tile and enter a value using the keypad or keyboard.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Completed horizontal and vertical equations turn green when mathematically true and red when false.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Build Combo Streaks by solving consecutive equations accurately without errors or hints.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Use the Swap power-up to trade values between any two editable tiles of the same type.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "• Keyboard shortcuts: 0–9, +, -, *, /, Backspace, Tab/Enter (next cell), Arrow keys.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}
