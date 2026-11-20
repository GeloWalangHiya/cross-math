package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.engine.CellKind
import com.example.engine.Difficulty
import com.example.engine.MathOp
import com.example.ui.components.CrossMathBoard
import com.example.ui.components.CrossMathControlDeck
import com.example.ui.components.RulesAndTwistsSheet
import com.example.ui.components.VictoryDialog
import com.example.ui.components.formatDuration
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCrossMathColors

@Composable
fun CrossMathScreen(
    viewModel: CrossMathViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val puzzle = uiState.puzzle
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val selectedCell = puzzle?.let { p ->
                    uiState.selectedCoord?.let { p.cellAt(it) }
                }
                when (event.key) {
                    Key.Zero, Key.NumPad0 -> { viewModel.inputDigit(0); true }
                    Key.One, Key.NumPad1 -> { viewModel.inputDigit(1); true }
                    Key.Two, Key.NumPad2 -> { viewModel.inputDigit(2); true }
                    Key.Three, Key.NumPad3 -> { viewModel.inputDigit(3); true }
                    Key.Four, Key.NumPad4 -> { viewModel.inputDigit(4); true }
                    Key.Five, Key.NumPad5 -> { viewModel.inputDigit(5); true }
                    Key.Six, Key.NumPad6 -> { viewModel.inputDigit(6); true }
                    Key.Seven, Key.NumPad7 -> { viewModel.inputDigit(7); true }
                    Key.Eight, Key.NumPad8 -> {
                        if (event.isShiftPressed && selectedCell?.kind == CellKind.OPERATOR) {
                            viewModel.inputOperator(MathOp.MUL)
                        } else {
                            viewModel.inputDigit(8)
                        }
                        true
                    }
                    Key.Nine, Key.NumPad9 -> { viewModel.inputDigit(9); true }
                    Key.Plus, Key.NumPadAdd -> { viewModel.inputOperator(MathOp.ADD); true }
                    Key.Equals -> {
                        if (event.isShiftPressed) {
                            viewModel.inputOperator(MathOp.ADD)
                            true
                        } else false
                    }
                    Key.Minus, Key.NumPadSubtract -> {
                        if (selectedCell?.kind == CellKind.OPERATOR) {
                            viewModel.inputOperator(MathOp.SUB)
                        } else if (uiState.preferences.allowNegatives) {
                            viewModel.toggleSign()
                        }
                        true
                    }
                    Key.Multiply, Key.NumPadMultiply -> { viewModel.inputOperator(MathOp.MUL); true }
                    Key.Slash, Key.NumPadDivide -> { viewModel.inputOperator(MathOp.DIV); true }
                    Key.Backspace, Key.Delete -> { viewModel.backspace(); true }
                    Key.Tab, Key.Enter, Key.NumPadEnter -> { viewModel.selectNextEmptyCell(); true }
                    Key.DirectionUp -> { viewModel.moveSelection(-1, 0); true }
                    Key.DirectionDown -> { viewModel.moveSelection(1, 0); true }
                    Key.DirectionLeft -> { viewModel.moveSelection(0, -1); true }
                    Key.DirectionRight -> { viewModel.moveSelection(0, 1); true }
                    else -> false
                }
            },
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (puzzle == null) return@Scaffold

        val selectedCell = uiState.selectedCoord?.let { puzzle.cellAt(it) }
        val isDailyCompletedToday = uiState.preferences.completedDailyDate == uiState.todayDateKey &&
            uiState.todayDateKey.isNotEmpty()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
        ) {
            val isWideScreen = maxWidth >= 720.dp

            if (isWideScreen) {
                // Canonical 2-pane studio layout for tablets & wide displays
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1.1f)
                            .widthIn(max = 520.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StudioHeader(
                            isMuted = uiState.preferences.isMuted,
                            isDailyActive = puzzle.isDailyChallenge,
                            isDailyCompletedToday = isDailyCompletedToday,
                            onDailyClick = viewModel::startDailyChallenge,
                            onRulesClick = { viewModel.setRulesSheetVisible(true) },
                            onToggleMute = viewModel::toggleMute
                        )
                        CrossMathBoard(
                            puzzle = puzzle,
                            selectedCoord = uiState.selectedCoord,
                            isSwapMode = uiState.isSwapMode,
                            swapFirstCoord = uiState.swapFirstCoord,
                            onCellClick = viewModel::onCellClicked,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(0.9f)
                            .widthIn(max = 440.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DifficultyRow(
                            selectedDifficulty = puzzle.difficulty,
                            isDailyActive = puzzle.isDailyChallenge,
                            onSelectDifficulty = viewModel::selectDifficulty
                        )
                        HudStatusStrip(
                            elapsedSeconds = uiState.elapsedSeconds,
                            moves = uiState.moves,
                            validCount = puzzle.validEquationCount,
                            totalCount = puzzle.totalEquationCount,
                            comboStreak = uiState.comboStreak,
                            evalModeTag = puzzle.evaluationMode.shortTag
                        )
                        FeedbackBannerSlot(message = uiState.feedbackBanner)
                        CrossMathControlDeck(
                            selectedKind = selectedCell?.kind,
                            allowNegatives = puzzle.allowNegatives,
                            canUndo = uiState.canUndo,
                            isSwapMode = uiState.isSwapMode,
                            isSolved = puzzle.isSolved,
                            onDigitClick = viewModel::inputDigit,
                            onOperatorClick = viewModel::inputOperator,
                            onToggleSign = viewModel::toggleSign,
                            onBackspace = viewModel::backspace,
                            onNextCell = viewModel::selectNextEmptyCell,
                            onUndo = viewModel::undo,
                            onHint = viewModel::useHint,
                            onSwapToggle = viewModel::toggleSwapMode,
                            onClearAll = viewModel::clearAllEntries,
                            onNewPuzzle = {
                                viewModel.startNewPuzzle(
                                    difficulty = puzzle.difficulty,
                                    isDailyChallenge = puzzle.isDailyChallenge
                                )
                            }
                        )
                    }
                }
            } else {
                // Clean mobile portrait stack
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StudioHeader(
                        isMuted = uiState.preferences.isMuted,
                        isDailyActive = puzzle.isDailyChallenge,
                        isDailyCompletedToday = isDailyCompletedToday,
                        onDailyClick = viewModel::startDailyChallenge,
                        onRulesClick = { viewModel.setRulesSheetVisible(true) },
                        onToggleMute = viewModel::toggleMute
                    )

                    DifficultyRow(
                        selectedDifficulty = puzzle.difficulty,
                        isDailyActive = puzzle.isDailyChallenge,
                        onSelectDifficulty = viewModel::selectDifficulty
                    )

                    HudStatusStrip(
                        elapsedSeconds = uiState.elapsedSeconds,
                        moves = uiState.moves,
                        validCount = puzzle.validEquationCount,
                        totalCount = puzzle.totalEquationCount,
                        comboStreak = uiState.comboStreak,
                        evalModeTag = puzzle.evaluationMode.shortTag
                    )

                    FeedbackBannerSlot(message = uiState.feedbackBanner)

                    CrossMathBoard(
                        puzzle = puzzle,
                        selectedCoord = uiState.selectedCoord,
                        isSwapMode = uiState.isSwapMode,
                        swapFirstCoord = uiState.swapFirstCoord,
                        onCellClick = viewModel::onCellClicked,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    CrossMathControlDeck(
                        selectedKind = selectedCell?.kind,
                        allowNegatives = puzzle.allowNegatives,
                        canUndo = uiState.canUndo,
                        isSwapMode = uiState.isSwapMode,
                        isSolved = puzzle.isSolved,
                        onDigitClick = viewModel::inputDigit,
                        onOperatorClick = viewModel::inputOperator,
                        onToggleSign = viewModel::toggleSign,
                        onBackspace = viewModel::backspace,
                        onNextCell = viewModel::selectNextEmptyCell,
                        onUndo = viewModel::undo,
                        onHint = viewModel::useHint,
                        onSwapToggle = viewModel::toggleSwapMode,
                        onClearAll = viewModel::clearAllEntries,
                        onNewPuzzle = {
                            viewModel.startNewPuzzle(
                                difficulty = puzzle.difficulty,
                                isDailyChallenge = puzzle.isDailyChallenge
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (uiState.showWinDialog) {
            VictoryDialog(
                puzzle = puzzle,
                elapsedSeconds = uiState.elapsedSeconds,
                moves = uiState.moves,
                hintsUsed = uiState.hintsUsed,
                maxComboStreak = uiState.maxComboStreak,
                onPlayAgain = {
                    viewModel.startNewPuzzle(
                        difficulty = puzzle.difficulty,
                        isDailyChallenge = false
                    )
                },
                onNextDifficulty = {
                    val next = when (puzzle.difficulty) {
                        Difficulty.EASY -> Difficulty.MEDIUM
                        Difficulty.MEDIUM -> Difficulty.HARD
                        Difficulty.HARD -> Difficulty.HARD
                    }
                    viewModel.selectDifficulty(next)
                },
                onDismiss = viewModel::dismissWinDialog
            )
        }

        if (uiState.showRulesSheet) {
            RulesAndTwistsSheet(
                evaluationMode = uiState.preferences.evaluationMode,
                allowNegatives = uiState.preferences.allowNegatives,
                themeMode = uiState.preferences.themeMode,
                totalSolved = uiState.preferences.totalSolved,
                onSelectEvaluationMode = viewModel::setEvaluationMode,
                onToggleAllowNegatives = viewModel::setAllowNegatives,
                onSelectThemeMode = viewModel::setThemeMode,
                onDismiss = { viewModel.setRulesSheetVisible(false) }
            )
        }
    }
}

@Composable
private fun StudioHeader(
    isMuted: Boolean,
    isDailyActive: Boolean,
    isDailyCompletedToday: Boolean,
    onDailyClick: () -> Unit,
    onRulesClick: () -> Unit,
    onToggleMute: () -> Unit
) {
    val semantic = LocalCrossMathColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Cross Math",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (isDailyActive) "Daily Challenge" else "Arithmetic Crossword",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isDailyActive,
                onClick = onDailyClick,
                label = {
                    Text(
                        text = if (isDailyCompletedToday) "Daily ✓" else "Daily",
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isDailyCompletedToday) Icons.Filled.CheckCircle else Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (isDailyCompletedToday) semantic.valid else MaterialTheme.colorScheme.primary
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("daily_challenge_button")
            )

            IconButton(
                onClick = onToggleMute,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("mute_toggle_button")
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute sound effects" else "Mute sound effects",
                    tint = if (isMuted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }

            IconButton(
                onClick = onRulesClick,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("rules_settings_button")
            ) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Rules, twists, and theme settings",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun DifficultyRow(
    selectedDifficulty: Difficulty,
    isDailyActive: Boolean,
    onSelectDifficulty: (Difficulty) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Difficulty.entries.forEach { diff ->
            val selected = !isDailyActive && selectedDifficulty == diff
            Surface(
                onClick = { onSelectDifficulty(diff) },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("difficulty_${diff.name}"),
                shape = RoundedCornerShape(14.dp),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                },
                border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = diff.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HudStatusStrip(
    elapsedSeconds: Int,
    moves: Int,
    validCount: Int,
    totalCount: Int,
    comboStreak: Int,
    evalModeTag: String
) {
    val semantic = LocalCrossMathColors.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 500.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.gridLine.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = "Timer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = formatDuration(elapsedSeconds),
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("timer_text")
                )
            }

            Text(
                text = "Moves $moves",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("moves_text")
            )

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (validCount == totalCount) semantic.validContainer else MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = "$validCount/$totalCount eq",
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (validCount == totalCount) semantic.onValidContainer else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }

            if (comboStreak >= 2) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = semantic.streakContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.LocalFireDepartment,
                            contentDescription = "Combo streak",
                            tint = semantic.streak,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${comboStreak}×",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            color = semantic.onStreakContainer
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = evalModeTag,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackBannerSlot(message: String?) {
    val semantic = LocalCrossMathColors.current
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = semantic.streakContainer,
            modifier = Modifier.widthIn(max = 500.dp)
        ) {
            Text(
                text = message.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = semantic.onStreakContainer,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }
    }
}
