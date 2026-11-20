package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CellKind
import com.example.engine.MathOp
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCrossMathColors

@Composable
fun CrossMathControlDeck(
    selectedKind: CellKind?,
    allowNegatives: Boolean,
    canUndo: Boolean,
    isSwapMode: Boolean,
    isSolved: Boolean,
    onDigitClick: (Int) -> Unit,
    onOperatorClick: (MathOp) -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onNextCell: () -> Unit,
    onUndo: () -> Unit,
    onHint: () -> Unit,
    onSwapToggle: () -> Unit,
    onClearAll: () -> Unit,
    onNewPuzzle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCrossMathColors.current

    Column(
        modifier = modifier.widthIn(max = 500.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Quick Action Row: Undo, Hint, Swap, Clear, New
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ToolActionButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                label = "Undo",
                enabled = canUndo && !isSolved,
                highlighted = false,
                testTag = "undo_button",
                onClick = onUndo,
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                icon = Icons.Outlined.Lightbulb,
                label = "Hint",
                enabled = !isSolved,
                highlighted = false,
                testTag = "hint_button",
                onClick = onHint,
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                icon = Icons.Outlined.SwapHoriz,
                label = "Swap",
                enabled = !isSolved,
                highlighted = isSwapMode,
                testTag = "swap_button",
                onClick = onSwapToggle,
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                icon = Icons.Outlined.DeleteSweep,
                label = "Clear",
                enabled = !isSolved,
                highlighted = false,
                testTag = "clear_button",
                onClick = onClearAll,
                modifier = Modifier.weight(1f)
            )
            ToolActionButton(
                icon = Icons.Outlined.Refresh,
                label = "New",
                enabled = true,
                highlighted = false,
                testTag = "new_puzzle_button",
                onClick = onNewPuzzle,
                modifier = Modifier.weight(1f)
            )
        }

        // Adaptive Keypad (Numbers vs Operators)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, semantic.gridLine.copy(alpha = 0.6f))
        ) {
            AnimatedContent(
                targetState = selectedKind == CellKind.OPERATOR,
                label = "keypad_mode"
            ) { isOperatorMode ->
                if (isOperatorMode) {
                    OperatorKeypad(
                        enabled = !isSolved,
                        onOperatorClick = onOperatorClick,
                        onBackspace = onBackspace,
                        onNextCell = onNextCell
                    )
                } else {
                    NumberKeypad(
                        enabled = !isSolved,
                        allowNegatives = allowNegatives,
                        onDigitClick = onDigitClick,
                        onToggleSign = onToggleSign,
                        onBackspace = onBackspace,
                        onNextCell = onNextCell
                    )
                }
            }
        }
    }
}

@Composable
private fun ToolActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    highlighted: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCrossMathColors.current
    val containerColor = when {
        highlighted -> semantic.streakContainer
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    }
    val contentColor = when {
        highlighted -> semantic.onStreakContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .heightIn(min = 48.dp)
            .testTag(testTag)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NumberKeypad(
    enabled: Boolean,
    allowNegatives: Boolean,
    onDigitClick: (Int) -> Unit,
    onToggleSign: () -> Unit,
    onBackspace: () -> Unit,
    onNextCell: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (d in 1..5) {
                DigitKey(
                    label = d.toString(),
                    enabled = enabled,
                    testTag = "key_$d",
                    onClick = { onDigitClick(d) },
                    modifier = Modifier.weight(1f)
                )
            }
            IconKey(
                icon = Icons.AutoMirrored.Filled.Backspace,
                label = "Backspace",
                enabled = enabled,
                testTag = "key_backspace",
                onClick = onBackspace,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (d in listOf(6, 7, 8, 9, 0)) {
                DigitKey(
                    label = d.toString(),
                    enabled = enabled,
                    testTag = "key_$d",
                    onClick = { onDigitClick(d) },
                    modifier = Modifier.weight(1f)
                )
            }
            IconKey(
                icon = Icons.AutoMirrored.Filled.ArrowForward,
                label = "Next cell",
                enabled = enabled,
                testTag = "key_next",
                onClick = onNextCell,
                modifier = Modifier.weight(1f)
            )
        }
        if (allowNegatives) {
            OutlinedButton(
                onClick = onToggleSign,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("key_sign"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "±  Toggle Negative Sign",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun OperatorKeypad(
    enabled: Boolean,
    onOperatorClick: (MathOp) -> Unit,
    onBackspace: () -> Unit,
    onNextCell: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (op in MathOp.entries) {
                DigitKey(
                    label = op.symbol,
                    enabled = enabled,
                    testTag = "key_op_${op.name}",
                    onClick = { onOperatorClick(op) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBackspace,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("key_op_clear"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Clear Op")
            }
            FilledTonalButton(
                onClick = onNextCell,
                enabled = enabled,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("key_op_next"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Next Blank")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun DigitKey(
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
            .testTag(testTag)
            .semantics { contentDescription = "Key $label" },
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
    }
}

@Composable
private fun IconKey(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .height(50.dp)
            .testTag(testTag)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp)
        )
    }
}
