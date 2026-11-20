package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEffectsPlayer
import com.example.data.PreferencesRepository
import com.example.data.UserPreferences
import com.example.engine.CellCoord
import com.example.engine.CellKind
import com.example.engine.CrossMathEngine
import com.example.engine.CrossMathPuzzle
import com.example.engine.Difficulty
import com.example.engine.EquationStatus
import com.example.engine.EvaluationMode
import com.example.engine.GridCell
import com.example.engine.MathOp
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class UndoSnapshot(
    val cells: List<List<GridCell>>,
    val selectedCoord: CellCoord?
)

data class CrossMathUiState(
    val puzzle: CrossMathPuzzle? = null,
    val selectedCoord: CellCoord? = null,
    val moves: Int = 0,
    val elapsedSeconds: Int = 0,
    val hintsUsed: Int = 0,
    val comboStreak: Int = 0,
    val maxComboStreak: Int = 0,
    val canUndo: Boolean = false,
    val isSwapMode: Boolean = false,
    val swapFirstCoord: CellCoord? = null,
    val showWinDialog: Boolean = false,
    val showRulesSheet: Boolean = false,
    val feedbackBanner: String? = null,
    val preferences: UserPreferences = UserPreferences(),
    val todayDateKey: String = ""
)

class CrossMathViewModel(application: Application) : AndroidViewModel(application) {

    private val prefsRepo = PreferencesRepository(application.applicationContext)
    val soundPlayer = SoundEffectsPlayer()

    private val _uiState = MutableStateFlow(CrossMathUiState())
    val uiState: StateFlow<CrossMathUiState> = _uiState.asStateFlow()

    private val undoStack = ArrayDeque<UndoSnapshot>()
    private var timerJob: Job? = null
    private var bannerJob: Job? = null
    private var hasRecordedWinForCurrentPuzzle = false

    init {
        val today = LocalDate.now()
        val todayKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        _uiState.update { it.copy(todayDateKey = todayKey) }

        viewModelScope.launch {
            val initialPrefs = prefsRepo.preferencesFlow.first()
            soundPlayer.isMuted = initialPrefs.isMuted
            _uiState.update { it.copy(preferences = initialPrefs) }
            startNewPuzzle(
                difficulty = initialPrefs.difficulty,
                evaluationMode = initialPrefs.evaluationMode,
                allowNegatives = initialPrefs.allowNegatives,
                isDailyChallenge = false
            )

            prefsRepo.preferencesFlow.collect { prefs ->
                soundPlayer.isMuted = prefs.isMuted
                _uiState.update { it.copy(preferences = prefs) }
            }
        }
    }

    fun startNewPuzzle(
        difficulty: Difficulty = _uiState.value.preferences.difficulty,
        evaluationMode: EvaluationMode = _uiState.value.preferences.evaluationMode,
        allowNegatives: Boolean = _uiState.value.preferences.allowNegatives,
        isDailyChallenge: Boolean = false
    ) {
        val today = LocalDate.now()
        val todayKey = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val prettyDate = today.format(DateTimeFormatter.ofPattern("MMM d"))
        val seed = if (isDailyChallenge) {
            // Deterministic daily seed shared across all sessions on the same calendar day
            today.toEpochDay() * 31_337L + evaluationMode.ordinal * 997L + (if (allowNegatives) 17L else 0L)
        } else {
            System.nanoTime()
        }

        val effectiveDifficulty = if (isDailyChallenge) Difficulty.MEDIUM else difficulty
        val puzzle = CrossMathEngine.generatePuzzle(
            difficulty = effectiveDifficulty,
            evaluationMode = evaluationMode,
            allowNegatives = allowNegatives,
            seed = seed,
            isDailyChallenge = isDailyChallenge,
            dailyDateLabel = if (isDailyChallenge) prettyDate else null
        )

        undoStack.clear()
        hasRecordedWinForCurrentPuzzle = false
        val firstEditable = puzzle.cells.flatten().firstOrNull { it.isEditable }?.coord

        _uiState.update {
            it.copy(
                puzzle = puzzle,
                selectedCoord = firstEditable,
                moves = 0,
                elapsedSeconds = 0,
                hintsUsed = 0,
                comboStreak = 0,
                maxComboStreak = 0,
                canUndo = false,
                isSwapMode = false,
                swapFirstCoord = null,
                showWinDialog = false,
                feedbackBanner = if (isDailyChallenge) "Daily Challenge • $prettyDate" else null,
                todayDateKey = todayKey
            )
        }
        startTimer()
    }

    fun selectDifficulty(difficulty: Difficulty) {
        viewModelScope.launch { prefsRepo.setDifficulty(difficulty) }
        val prefs = _uiState.value.preferences
        startNewPuzzle(
            difficulty = difficulty,
            evaluationMode = prefs.evaluationMode,
            allowNegatives = prefs.allowNegatives,
            isDailyChallenge = false
        )
    }

    fun startDailyChallenge() {
        val prefs = _uiState.value.preferences
        startNewPuzzle(
            difficulty = Difficulty.MEDIUM,
            evaluationMode = prefs.evaluationMode,
            allowNegatives = prefs.allowNegatives,
            isDailyChallenge = true
        )
    }

    fun setEvaluationMode(mode: EvaluationMode) {
        viewModelScope.launch { prefsRepo.setEvaluationMode(mode) }
        val current = _uiState.value.puzzle
        startNewPuzzle(
            difficulty = current?.difficulty ?: _uiState.value.preferences.difficulty,
            evaluationMode = mode,
            allowNegatives = _uiState.value.preferences.allowNegatives,
            isDailyChallenge = current?.isDailyChallenge ?: false
        )
    }

    fun setAllowNegatives(allow: Boolean) {
        viewModelScope.launch { prefsRepo.setAllowNegatives(allow) }
        val current = _uiState.value.puzzle
        startNewPuzzle(
            difficulty = current?.difficulty ?: _uiState.value.preferences.difficulty,
            evaluationMode = _uiState.value.preferences.evaluationMode,
            allowNegatives = allow,
            isDailyChallenge = current?.isDailyChallenge ?: false
        )
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun toggleMute() {
        val nextMuted = !_uiState.value.preferences.isMuted
        soundPlayer.isMuted = nextMuted
        viewModelScope.launch { prefsRepo.setMuted(nextMuted) }
        if (!nextMuted) soundPlayer.playKeyTap()
    }

    fun setRulesSheetVisible(visible: Boolean) {
        _uiState.update { it.copy(showRulesSheet = visible) }
    }

    fun dismissWinDialog() {
        _uiState.update { it.copy(showWinDialog = false) }
    }

    fun onCellClicked(coord: CellCoord) {
        val puzzle = _uiState.value.puzzle ?: return
        val cell = puzzle.cellAt(coord)
        if (!cell.isEditable) return

        if (_uiState.value.isSwapMode) {
            handleSwapSelection(coord, cell)
            return
        }

        soundPlayer.playKeyTap()
        _uiState.update { it.copy(selectedCoord = coord) }
    }

    private fun handleSwapSelection(coord: CellCoord, cell: GridCell) {
        val puzzle = _uiState.value.puzzle ?: return
        val first = _uiState.value.swapFirstCoord
        if (first == null) {
            soundPlayer.playKeyTap()
            _uiState.update {
                it.copy(
                    swapFirstCoord = coord,
                    selectedCoord = coord,
                    feedbackBanner = "Now tap a second ${if (cell.kind == CellKind.NUMBER) "number" else "operator"} tile to swap"
                )
            }
            return
        }

        if (first == coord) {
            // Cancel first selection
            _uiState.update {
                it.copy(
                    isSwapMode = false,
                    swapFirstCoord = null,
                    feedbackBanner = null
                )
            }
            return
        }

        val firstCell = puzzle.cellAt(first)
        if (firstCell.kind != cell.kind) {
            showTransientBanner("Swap requires two tiles of the same type")
            soundPlayer.playEquationError()
            return
        }

        pushUndoSnapshot(puzzle)
        val updatedCells = puzzle.cells.map { row ->
            row.map { c ->
                when (c.coord) {
                    first -> c.copy(
                        userNumberInput = cell.userNumberInput,
                        userOpInput = cell.userOpInput,
                        isHinted = false
                    )
                    coord -> c.copy(
                        userNumberInput = firstCell.userNumberInput,
                        userOpInput = firstCell.userOpInput,
                        isHinted = false
                    )
                    else -> c
                }
            }
        }
        applyCellUpdate(
            oldPuzzle = puzzle,
            newCells = updatedCells,
            newSelected = coord,
            incrementMoves = true,
            overrideSound = { soundPlayer.playHintOrSwap() }
        )
        _uiState.update { it.copy(isSwapMode = false, swapFirstCoord = null) }
        showTransientBanner("Tiles swapped!")
    }

    fun toggleSwapMode() {
        val current = _uiState.value
        val puzzle = current.puzzle ?: return
        if (puzzle.isSolved) return
        val entering = !current.isSwapMode
        soundPlayer.playKeyTap()
        _uiState.update {
            it.copy(
                isSwapMode = entering,
                swapFirstCoord = null,
                feedbackBanner = if (entering) "Swap Power-up: tap two editable tiles to swap them" else null
            )
        }
    }

    fun inputDigit(digit: Int) {
        val puzzle = _uiState.value.puzzle ?: return
        if (puzzle.isSolved) return
        val coord = _uiState.value.selectedCoord ?: return
        val cell = puzzle.cellAt(coord)
        if (!cell.isEditable || cell.kind != CellKind.NUMBER) return

        val currentText = cell.userNumberInput
        val nextText = when {
            currentText == "0" -> digit.toString()
            currentText == "-0" -> "-$digit"
            currentText.replace("-", "").length >= 3 -> currentText
            else -> currentText + digit.toString()
        }
        if (nextText == currentText) return

        pushUndoSnapshot(puzzle)
        val updatedCells = replaceCell(
            puzzle.cells,
            coord,
            cell.copy(userNumberInput = nextText, isHinted = false)
        )
        applyCellUpdate(puzzle, updatedCells, coord, incrementMoves = true)
    }

    fun toggleSign() {
        val puzzle = _uiState.value.puzzle ?: return
        if (puzzle.isSolved) return
        val coord = _uiState.value.selectedCoord ?: return
        val cell = puzzle.cellAt(coord)
        if (!cell.isEditable || cell.kind != CellKind.NUMBER) return

        val currentText = cell.userNumberInput
        val nextText = if (currentText.startsWith("-")) {
            currentText.removePrefix("-")
        } else {
            "-$currentText"
        }

        pushUndoSnapshot(puzzle)
        val updatedCells = replaceCell(
            puzzle.cells,
            coord,
            cell.copy(userNumberInput = nextText, isHinted = false)
        )
        applyCellUpdate(puzzle, updatedCells, coord, incrementMoves = false)
    }

    fun inputOperator(op: MathOp) {
        val puzzle = _uiState.value.puzzle ?: return
        if (puzzle.isSolved) return
        val coord = _uiState.value.selectedCoord ?: return
        val cell = puzzle.cellAt(coord)
        if (!cell.isEditable || cell.kind != CellKind.OPERATOR) return
        if (cell.userOpInput == op) return

        pushUndoSnapshot(puzzle)
        val updatedCells = replaceCell(
            puzzle.cells,
            coord,
            cell.copy(userOpInput = op, isHinted = false)
        )
        applyCellUpdate(puzzle, updatedCells, coord, incrementMoves = true)
    }

    fun backspace() {
        val puzzle = _uiState.value.puzzle ?: return
        if (puzzle.isSolved) return
        val coord = _uiState.value.selectedCoord ?: return
        val cell = puzzle.cellAt(coord)
        if (!cell.isEditable) return

        when (cell.kind) {
            CellKind.NUMBER -> {
                if (cell.userNumberInput.isEmpty()) return
                pushUndoSnapshot(puzzle)
                val nextText = cell.userNumberInput.dropLast(1)
                val updatedCells = replaceCell(
                    puzzle.cells,
                    coord,
                    cell.copy(userNumberInput = nextText, isHinted = false)
                )
                soundPlayer.playErase()
                applyCellUpdate(
                    oldPuzzle = puzzle,
                    newCells = updatedCells,
                    newSelected = coord,
                    incrementMoves = false,
                    overrideSound = { soundPlayer.playErase() }
                )
            }
            CellKind.OPERATOR -> {
                if (cell.userOpInput == null) return
                pushUndoSnapshot(puzzle)
                val updatedCells = replaceCell(
                    puzzle.cells,
                    coord,
                    cell.copy(userOpInput = null, isHinted = false)
                )
                applyCellUpdate(
                    oldPuzzle = puzzle,
                    newCells = updatedCells,
                    newSelected = coord,
                    incrementMoves = false,
                    overrideSound = { soundPlayer.playErase() }
                )
            }
            else -> Unit
        }
    }

    fun clearSelectedCell() {
        val puzzle = _uiState.value.puzzle ?: return
        if (puzzle.isSolved) return
        val coord = _uiState.value.selectedCoord ?: return
        val cell = puzzle.cellAt(coord)
        if (!cell.isEditable) return
        if (cell.userNumberInput.isEmpty() && cell.userOpInput == null) return

        pushUndoSnapshot(puzzle)
        val updatedCells = replaceCell(
            puzzle.cells,
            coord,
            cell.copy(userNumberInput = "", userOpInput = null, isHinted = false)
        )
        applyCellUpdate(
            oldPuzzle = puzzle,
            newCells = updatedCells,
            newSelected = coord,
            incrementMoves = false,
            overrideSound = { soundPlayer.playErase() }
        )
    }

    fun clearAllEntries() {
        val puzzle = _uiState.value.puzzle ?: return
        val hasAnyInput = puzzle.cells.flatten().any {
            it.isEditable && (it.userNumberInput.isNotEmpty() || it.userOpInput != null)
        }
        if (!hasAnyInput) return

        pushUndoSnapshot(puzzle)
        val clearedCells = puzzle.cells.map { row ->
            row.map { cell ->
                if (cell.isEditable) {
                    cell.copy(userNumberInput = "", userOpInput = null, isHinted = false)
                } else {
                    cell
                }
            }
        }
        applyCellUpdate(
            oldPuzzle = puzzle,
            newCells = clearedCells,
            newSelected = _uiState.value.selectedCoord,
            incrementMoves = false,
            overrideSound = { soundPlayer.playErase() }
        )
        showTransientBanner("Board cleared")
    }

    fun undo() {
        val puzzle = _uiState.value.puzzle ?: return
        if (undoStack.isEmpty()) return
        val snapshot = undoStack.removeLast()
        soundPlayer.playErase()
        val statuses = CrossMathEngine.evaluateAllEquations(
            snapshot.cells,
            puzzle.equations,
            puzzle.evaluationMode
        )
        val updatedPuzzle = puzzle.copy(cells = snapshot.cells, equationStatuses = statuses)
        _uiState.update {
            it.copy(
                puzzle = updatedPuzzle,
                selectedCoord = snapshot.selectedCoord ?: it.selectedCoord,
                canUndo = undoStack.isNotEmpty(),
                isSwapMode = false,
                swapFirstCoord = null
            )
        }
    }

    fun useHint() {
        val puzzle = _uiState.value.puzzle ?: return
        if (puzzle.isSolved) return

        val editableCells = puzzle.cells.flatten().filter { it.isEditable }
        val selected = _uiState.value.selectedCoord?.let { puzzle.cellAt(it) }?.takeIf { it.isEditable }

        val targetCell: GridCell? = when {
            selected != null && !isCellMatchingSolution(selected) -> selected
            else -> editableCells.firstOrNull { !it.isFilled }
                ?: editableCells.firstOrNull { !isCellMatchingSolution(it) }
        }

        if (targetCell == null) return

        pushUndoSnapshot(puzzle)
        val revealed = when (targetCell.kind) {
            CellKind.NUMBER -> targetCell.copy(
                userNumberInput = targetCell.solutionNumber?.toString().orEmpty(),
                isHinted = true
            )
            CellKind.OPERATOR -> targetCell.copy(
                userOpInput = targetCell.solutionOp,
                isHinted = true
            )
            else -> targetCell
        }

        val updatedCells = replaceCell(puzzle.cells, targetCell.coord, revealed)
        _uiState.update {
            it.copy(
                hintsUsed = it.hintsUsed + 1,
                comboStreak = 0,
                isSwapMode = false,
                swapFirstCoord = null
            )
        }
        applyCellUpdate(
            oldPuzzle = puzzle,
            newCells = updatedCells,
            newSelected = targetCell.coord,
            incrementMoves = true,
            overrideSound = { soundPlayer.playHintOrSwap() }
        )
        showTransientBanner("Hint revealed!")
    }

    fun selectNextEmptyCell() {
        val puzzle = _uiState.value.puzzle ?: return
        val editables = puzzle.cells.flatten().filter { it.isEditable }
        if (editables.isEmpty()) return

        val currentIdx = editables.indexOfFirst { it.coord == _uiState.value.selectedCoord }
        val ordered = if (currentIdx >= 0) {
            editables.drop(currentIdx + 1) + editables.take(currentIdx + 1)
        } else {
            editables
        }
        val next = ordered.firstOrNull { !it.isFilled } ?: ordered.first()
        soundPlayer.playKeyTap()
        _uiState.update { it.copy(selectedCoord = next.coord) }
    }

    fun moveSelection(dRow: Int, dCol: Int) {
        val puzzle = _uiState.value.puzzle ?: return
        val current = _uiState.value.selectedCoord ?: run {
            selectNextEmptyCell()
            return
        }
        var r = current.row + dRow
        var c = current.col + dCol
        while (r in 0 until puzzle.gridSize && c in 0 until puzzle.gridSize) {
            val candidate = puzzle.cells[r][c]
            if (candidate.isEditable) {
                soundPlayer.playKeyTap()
                _uiState.update { it.copy(selectedCoord = candidate.coord) }
                return
            }
            r += dRow
            c += dCol
        }
    }

    private fun isCellMatchingSolution(cell: GridCell): Boolean = when (cell.kind) {
        CellKind.NUMBER -> cell.effectiveNumber() == cell.solutionNumber
        CellKind.OPERATOR -> cell.effectiveOp() == cell.solutionOp
        else -> true
    }

    private fun pushUndoSnapshot(puzzle: CrossMathPuzzle) {
        if (undoStack.size >= 50) {
            undoStack.removeFirst()
        }
        undoStack.addLast(UndoSnapshot(puzzle.cells, _uiState.value.selectedCoord))
    }

    private fun replaceCell(
        cells: List<List<GridCell>>,
        coord: CellCoord,
        replacement: GridCell
    ): List<List<GridCell>> = cells.mapIndexed { r, row ->
        if (r != coord.row) row
        else row.mapIndexed { c, cell -> if (c == coord.col) replacement else cell }
    }

    private fun applyCellUpdate(
        oldPuzzle: CrossMathPuzzle,
        newCells: List<List<GridCell>>,
        newSelected: CellCoord?,
        incrementMoves: Boolean,
        overrideSound: (() -> Unit)? = null
    ) {
        val newStatuses = CrossMathEngine.evaluateAllEquations(
            newCells,
            oldPuzzle.equations,
            oldPuzzle.evaluationMode
        )
        val updatedPuzzle = oldPuzzle.copy(cells = newCells, equationStatuses = newStatuses)

        val newlyValidCount = newStatuses.count { (id, status) ->
            status == EquationStatus.VALID && oldPuzzle.equationStatuses[id] != EquationStatus.VALID
        }
        val newlyInvalidCount = newStatuses.count { (id, status) ->
            status == EquationStatus.INVALID && oldPuzzle.equationStatuses[id] != EquationStatus.INVALID
        }

        val currentStreak = _uiState.value.comboStreak
        val nextStreak = when {
            newlyInvalidCount > 0 -> 0
            newlyValidCount > 0 -> currentStreak + newlyValidCount
            else -> currentStreak
        }
        val nextMaxStreak = maxOf(_uiState.value.maxComboStreak, nextStreak)

        val isWin = updatedPuzzle.isSolved
        if (isWin) {
            timerJob?.cancel()
            soundPlayer.playVictory()
            if (!hasRecordedWinForCurrentPuzzle) {
                hasRecordedWinForCurrentPuzzle = true
                val dailyCompleted = if (updatedPuzzle.isDailyChallenge) _uiState.value.todayDateKey else null
                viewModelScope.launch { prefsRepo.recordPuzzleSolved(dailyCompleted) }
            }
        } else if (overrideSound != null) {
            overrideSound()
        } else if (newlyValidCount > 0) {
            soundPlayer.playEquationValid()
            if (nextStreak >= 2) {
                showTransientBanner("${nextStreak}× Combo Streak!")
            }
        } else if (newlyInvalidCount > 0) {
            soundPlayer.playEquationError()
        } else {
            soundPlayer.playKeyTap()
        }

        _uiState.update {
            it.copy(
                puzzle = updatedPuzzle,
                selectedCoord = newSelected,
                moves = if (incrementMoves) it.moves + 1 else it.moves,
                comboStreak = nextStreak,
                maxComboStreak = nextMaxStreak,
                canUndo = undoStack.isNotEmpty(),
                showWinDialog = isWin
            )
        }
    }

    private fun showTransientBanner(message: String) {
        bannerJob?.cancel()
        _uiState.update { it.copy(feedbackBanner = message) }
        bannerJob = viewModelScope.launch {
            delay(2400L)
            _uiState.update { state ->
                if (state.feedbackBanner == message) state.copy(feedbackBanner = null) else state
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                _uiState.update { state ->
                    val solved = state.puzzle?.isSolved ?: false
                    if (!solved && !state.showRulesSheet) {
                        state.copy(elapsedSeconds = state.elapsedSeconds + 1)
                    } else {
                        state
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        bannerJob?.cancel()
    }
}
