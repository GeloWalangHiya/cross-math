package com.example.engine

import kotlin.math.abs
import kotlin.random.Random

enum class MathOp(val symbol: String, val inputChar: Char) {
    ADD("+", '+'),
    SUB("−", '-'),
    MUL("×", '*'),
    DIV("÷", '/');

    companion object {
        fun fromChar(c: Char): MathOp? = when (c) {
            '+' -> ADD
            '-', '−' -> SUB
            '*', '×', 'x', 'X' -> MUL
            '/', '÷' -> DIV
            else -> null
        }
    }
}

enum class EvaluationMode(val label: String, val shortTag: String, val description: String) {
    PEMDAS(
        label = "PEMDAS / BODMAS",
        shortTag = "PEMDAS",
        description = "Multiplication & division evaluate before addition & subtraction."
    ),
    LEFT_TO_RIGHT(
        label = "Left-to-Right",
        shortTag = "L → R",
        description = "Arcade twist: equations evaluate strictly from left to right / top to bottom."
    )
}

enum class Difficulty(
    val label: String,
    val gridSize: Int,
    val maxOperand: Int,
    val targetNumberBlanks: Int,
    val targetOperatorBlanks: Int,
    val summary: String
) {
    EASY(
        label = "Easy",
        gridSize = 5,
        maxOperand = 12,
        targetNumberBlanks = 4,
        targetOperatorBlanks = 0,
        summary = "5×5 grid • 2-term equations"
    ),
    MEDIUM(
        label = "Medium",
        gridSize = 7,
        maxOperand = 15,
        targetNumberBlanks = 8,
        targetOperatorBlanks = 0,
        summary = "7×7 grid • 3-term PEMDAS"
    ),
    HARD(
        label = "Hard",
        gridSize = 7,
        maxOperand = 20,
        targetNumberBlanks = 9,
        targetOperatorBlanks = 3,
        summary = "7×7 grid • Missing numbers & operators"
    )
}

enum class CellKind {
    VOID,
    NUMBER,
    OPERATOR,
    EQUALS
}

enum class EquationStatus {
    NEUTRAL,
    VALID,
    INVALID
}

data class CellCoord(val row: Int, val col: Int)

data class GridCell(
    val row: Int,
    val col: Int,
    val kind: CellKind,
    val solutionNumber: Int? = null,
    val solutionOp: MathOp? = null,
    val isFixed: Boolean = true,
    val userNumberInput: String = "",
    val userOpInput: MathOp? = null,
    val isHinted: Boolean = false
) {
    val coord: CellCoord get() = CellCoord(row, col)
    val isEditable: Boolean get() = !isFixed && (kind == CellKind.NUMBER || kind == CellKind.OPERATOR)
    val isFilled: Boolean
        get() = when (kind) {
            CellKind.NUMBER -> isFixed || userNumberInput.toIntOrNull() != null
            CellKind.OPERATOR -> isFixed || userOpInput != null
            CellKind.EQUALS -> true
            CellKind.VOID -> true
        }

    fun effectiveNumber(): Int? = if (kind != CellKind.NUMBER) null else {
        if (isFixed) solutionNumber else userNumberInput.toIntOrNull()
    }

    fun effectiveOp(): MathOp? = if (kind != CellKind.OPERATOR) null else {
        if (isFixed) solutionOp else userOpInput
    }
}

data class Equation(
    val id: String,
    val isHorizontal: Boolean,
    val operandCoords: List<CellCoord>,
    val operatorCoords: List<CellCoord>,
    val equalsCoord: CellCoord,
    val resultCoord: CellCoord
) {
    val allVariableCoords: List<CellCoord>
        get() = operandCoords + operatorCoords + resultCoord
}

data class CrossMathPuzzle(
    val id: String,
    val difficulty: Difficulty,
    val evaluationMode: EvaluationMode,
    val allowNegatives: Boolean,
    val isDailyChallenge: Boolean,
    val dailyDateLabel: String?,
    val gridSize: Int,
    val cells: List<List<GridCell>>,
    val equations: List<Equation>,
    val equationStatuses: Map<String, EquationStatus>
) {
    val isSolved: Boolean
        get() = equationStatuses.isNotEmpty() &&
            equationStatuses.values.all { it == EquationStatus.VALID } &&
            cells.flatten().filter { it.isEditable }.all { it.isFilled }

    val validEquationCount: Int
        get() = equationStatuses.values.count { it == EquationStatus.VALID }

    val totalEquationCount: Int
        get() = equations.size

    fun cellAt(coord: CellCoord): GridCell = cells[coord.row][coord.col]
}

object CrossMathEngine {

    /**
     * Exact integer evaluation of [numbers] (size N) and [ops] (size N-1).
     * Returns null if division by zero or non-exact integer division occurs.
     */
    fun evaluateExpression(
        numbers: List<Int>,
        ops: List<MathOp>,
        mode: EvaluationMode
    ): Int? {
        if (numbers.isEmpty() || numbers.size != ops.size + 1) return null
        return when (mode) {
            EvaluationMode.LEFT_TO_RIGHT -> {
                var acc = numbers[0].toLong()
                for (i in ops.indices) {
                    val rhs = numbers[i + 1].toLong()
                    acc = when (ops[i]) {
                        MathOp.ADD -> acc + rhs
                        MathOp.SUB -> acc - rhs
                        MathOp.MUL -> acc * rhs
                        MathOp.DIV -> {
                            if (rhs == 0L || acc % rhs != 0L) return null
                            acc / rhs
                        }
                    }
                    if (abs(acc) > 100_000L) return null
                }
                acc.toInt()
            }

            EvaluationMode.PEMDAS -> {
                val reducedNums = mutableListOf(numbers[0].toLong())
                val reducedOps = mutableListOf<MathOp>()
                for (i in ops.indices) {
                    val op = ops[i]
                    val rhs = numbers[i + 1].toLong()
                    when (op) {
                        MathOp.MUL -> {
                            val prod = reducedNums.last() * rhs
                            if (abs(prod) > 100_000L) return null
                            reducedNums[reducedNums.lastIndex] = prod
                        }
                        MathOp.DIV -> {
                            val lhs = reducedNums.last()
                            if (rhs == 0L || lhs % rhs != 0L) return null
                            reducedNums[reducedNums.lastIndex] = lhs / rhs
                        }
                        MathOp.ADD, MathOp.SUB -> {
                            reducedOps.add(op)
                            reducedNums.add(rhs)
                        }
                    }
                }
                var acc = reducedNums[0]
                for (i in reducedOps.indices) {
                    val rhs = reducedNums[i + 1]
                    acc = when (reducedOps[i]) {
                        MathOp.ADD -> acc + rhs
                        MathOp.SUB -> acc - rhs
                        else -> acc
                    }
                    if (abs(acc) > 100_000L) return null
                }
                acc.toInt()
            }
        }
    }

    fun evaluateAllEquations(
        cells: List<List<GridCell>>,
        equations: List<Equation>,
        mode: EvaluationMode
    ): Map<String, EquationStatus> {
        val statuses = LinkedHashMap<String, EquationStatus>(equations.size)
        for (eq in equations) {
            val operands = eq.operandCoords.map { cells[it.row][it.col].effectiveNumber() }
            val operators = eq.operatorCoords.map { cells[it.row][it.col].effectiveOp() }
            val result = cells[eq.resultCoord.row][eq.resultCoord.col].effectiveNumber()

            if (operands.any { it == null } || operators.any { it == null } || result == null) {
                statuses[eq.id] = EquationStatus.NEUTRAL
                continue
            }

            val computed = evaluateExpression(
                numbers = operands.filterNotNull(),
                ops = operators.filterNotNull(),
                mode = mode
            )
            statuses[eq.id] = if (computed != null && computed == result) {
                EquationStatus.VALID
            } else {
                EquationStatus.INVALID
            }
        }
        return statuses
    }

    fun generatePuzzle(
        difficulty: Difficulty,
        evaluationMode: EvaluationMode = EvaluationMode.PEMDAS,
        allowNegatives: Boolean = false,
        seed: Long = System.nanoTime(),
        isDailyChallenge: Boolean = false,
        dailyDateLabel: String? = null
    ): CrossMathPuzzle {
        val random = Random(seed)
        val gridSize = difficulty.gridSize

        // Attempt constructive generation with peeling uniqueness verification
        repeat(250) {
            val candidate = tryBuildPuzzle(
                difficulty = difficulty,
                evaluationMode = evaluationMode,
                allowNegatives = allowNegatives,
                random = random,
                isDailyChallenge = isDailyChallenge,
                dailyDateLabel = dailyDateLabel
            )
            if (candidate != null) {
                return candidate
            }
        }

        // Guaranteed deterministic fallback puzzle if random retries are exhausted
        return buildFallbackPuzzle(
            difficulty = difficulty,
            evaluationMode = evaluationMode,
            allowNegatives = allowNegatives,
            isDailyChallenge = isDailyChallenge,
            dailyDateLabel = dailyDateLabel
        )
    }

    private fun tryBuildPuzzle(
        difficulty: Difficulty,
        evaluationMode: EvaluationMode,
        allowNegatives: Boolean,
        random: Random,
        isDailyChallenge: Boolean,
        dailyDateLabel: String?
    ): CrossMathPuzzle? {
        val size = difficulty.gridSize
        val termCount = if (size == 5) 2 else 3
        val operandIndices = if (size == 5) listOf(0, 2) else listOf(0, 2, 4)
        val opIndices = if (size == 5) listOf(1) else listOf(1, 3)
        val equalsIdx = size - 2
        val resultIdx = size - 1

        // 1. Pick non-zero operands
        val operandGrid = mutableMapOf<CellCoord, Int>()
        for (r in operandIndices) {
            for (c in operandIndices) {
                operandGrid[CellCoord(r, c)] = pickOperand(difficulty.maxOperand, allowNegatives, random)
            }
        }

        val minResult = if (allowNegatives) -99 else 0
        val maxResult = if (difficulty == Difficulty.EASY) 60 else 150

        // 2. Pick operators and compute horizontal equations
        val opGrid = mutableMapOf<CellCoord, MathOp>()
        val resultGrid = mutableMapOf<CellCoord, Int>()
        val equations = mutableListOf<Equation>()

        for (r in operandIndices) {
            val nums = operandIndices.map { c -> operandGrid.getValue(CellCoord(r, c)) }
            val chosen = pickValidOperators(nums, evaluationMode, minResult, maxResult, difficulty, random)
                ?: return null
            val (ops, res) = chosen
            opIndices.forEachIndexed { idx, c -> opGrid[CellCoord(r, c)] = ops[idx] }
            resultGrid[CellCoord(r, resultIdx)] = res
            equations.add(
                Equation(
                    id = "R$r",
                    isHorizontal = true,
                    operandCoords = operandIndices.map { c -> CellCoord(r, c) },
                    operatorCoords = opIndices.map { c -> CellCoord(r, c) },
                    equalsCoord = CellCoord(r, equalsIdx),
                    resultCoord = CellCoord(r, resultIdx)
                )
            )
        }

        // 3. Pick operators and compute vertical equations
        for (c in operandIndices) {
            val nums = operandIndices.map { r -> operandGrid.getValue(CellCoord(r, c)) }
            val chosen = pickValidOperators(nums, evaluationMode, minResult, maxResult, difficulty, random)
                ?: return null
            val (ops, res) = chosen
            opIndices.forEachIndexed { idx, r -> opGrid[CellCoord(r, c)] = ops[idx] }
            resultGrid[CellCoord(resultIdx, c)] = res
            equations.add(
                Equation(
                    id = "C$c",
                    isHorizontal = false,
                    operandCoords = operandIndices.map { r -> CellCoord(r, c) },
                    operatorCoords = opIndices.map { r -> CellCoord(r, c) },
                    equalsCoord = CellCoord(equalsIdx, c),
                    resultCoord = CellCoord(resultIdx, c)
                )
            )
        }

        // Encourage PEMDAS mix on 3-term puzzles so order of operations actively matters
        if (termCount == 3) {
            val hasMixedPrecedence = equations.any { eq ->
                val ops = eq.operatorCoords.map { opGrid.getValue(it) }
                val hasHigh = ops.any { it == MathOp.MUL || it == MathOp.DIV }
                val hasLow = ops.any { it == MathOp.ADD || it == MathOp.SUB }
                hasHigh && hasLow
            }
            if (!hasMixedPrecedence) return null
        }

        // 4. Construct full solved grid
        val solvedCells = MutableList(size) { r ->
            MutableList(size) { c ->
                val coord = CellCoord(r, c)
                when {
                    operandGrid.containsKey(coord) -> GridCell(
                        row = r,
                        col = c,
                        kind = CellKind.NUMBER,
                        solutionNumber = operandGrid.getValue(coord),
                        isFixed = true
                    )
                    resultGrid.containsKey(coord) -> GridCell(
                        row = r,
                        col = c,
                        kind = CellKind.NUMBER,
                        solutionNumber = resultGrid.getValue(coord),
                        isFixed = true
                    )
                    opGrid.containsKey(coord) -> GridCell(
                        row = r,
                        col = c,
                        kind = CellKind.OPERATOR,
                        solutionOp = opGrid.getValue(coord),
                        isFixed = true
                    )
                    (r in operandIndices && c == equalsIdx) || (c in operandIndices && r == equalsIdx) ->
                        GridCell(row = r, col = c, kind = CellKind.EQUALS, isFixed = true)
                    else -> GridCell(row = r, col = c, kind = CellKind.VOID, isFixed = true)
                }
            }
        }

        // 5. Select blank cells while preserving 100% peeling uniqueness
        val numberCoords = (operandGrid.keys + resultGrid.keys).shuffled(random)
        val opCoords = opGrid.keys.shuffled(random)
        val blanks = mutableSetOf<CellCoord>()

        // First punch missing operators (on Hard mode)
        var opBlanksAdded = 0
        for (coord in opCoords) {
            if (opBlanksAdded >= difficulty.targetOperatorBlanks) break
            val candidateBlanks = blanks + coord
            if (isUniquelyPeelable(solvedCells, equations, candidateBlanks, evaluationMode)) {
                blanks.add(coord)
                opBlanksAdded++
            }
        }
        if (opBlanksAdded < difficulty.targetOperatorBlanks) return null

        // Next punch missing numbers
        var numBlanksAdded = 0
        for (coord in numberCoords) {
            if (numBlanksAdded >= difficulty.targetNumberBlanks) break
            val candidateBlanks = blanks + coord
            if (isUniquelyPeelable(solvedCells, equations, candidateBlanks, evaluationMode)) {
                blanks.add(coord)
                numBlanksAdded++
            }
        }
        if (numBlanksAdded < difficulty.targetNumberBlanks - 1) return null

        // Apply blanks
        val finalCells = solvedCells.map { row ->
            row.map { cell ->
                if (cell.coord in blanks) {
                    cell.copy(isFixed = false)
                } else {
                    cell
                }
            }
        }

        val statuses = evaluateAllEquations(finalCells, equations, evaluationMode)
        return CrossMathPuzzle(
            id = "puzzle_${difficulty.name}_${random.nextInt(100_000, 999_999)}",
            difficulty = difficulty,
            evaluationMode = evaluationMode,
            allowNegatives = allowNegatives,
            isDailyChallenge = isDailyChallenge,
            dailyDateLabel = dailyDateLabel,
            gridSize = size,
            cells = finalCells,
            equations = equations,
            equationStatuses = statuses
        )
    }

    /**
     * Verifies that every blank cell in [blanks] can be uniquely deduced step-by-step
     * without guessing (peeling solver).
     */
    fun isUniquelyPeelable(
        solvedCells: List<List<GridCell>>,
        equations: List<Equation>,
        blanks: Set<CellCoord>,
        mode: EvaluationMode
    ): Boolean {
        val remaining = blanks.toMutableSet()
        while (remaining.isNotEmpty()) {
            var peeledAny = false
            for (eq in equations) {
                val eqBlanks = eq.allVariableCoords.filter { it in remaining }
                if (eqBlanks.size == 1) {
                    val target = eqBlanks.first()
                    if (isSingleUnknownUnique(solvedCells, eq, target, mode)) {
                        remaining.remove(target)
                        peeledAny = true
                    }
                }
            }
            if (!peeledAny) break
        }
        return remaining.isEmpty()
    }

    private fun isSingleUnknownUnique(
        solvedCells: List<List<GridCell>>,
        eq: Equation,
        target: CellCoord,
        mode: EvaluationMode
    ): Boolean {
        val cell = solvedCells[target.row][target.col]
        if (cell.kind == CellKind.NUMBER) {
            // Result cell is always uniquely determined by known operands & ops.
            // Any operand cell is uniquely determined whenever all operands are non-zero.
            return true
        }
        if (cell.kind == CellKind.OPERATOR) {
            val opIdx = eq.operatorCoords.indexOf(target)
            if (opIdx < 0) return false
            val nums = eq.operandCoords.mapNotNull { solvedCells[it.row][it.col].solutionNumber }
            val result = solvedCells[eq.resultCoord.row][eq.resultCoord.col].solutionNumber ?: return false
            val baseOps = eq.operatorCoords.mapNotNull { solvedCells[it.row][it.col].solutionOp }
            if (nums.size != eq.operandCoords.size || baseOps.size != eq.operatorCoords.size) return false

            var validOpCount = 0
            for (candidateOp in MathOp.entries) {
                val testOps = baseOps.toMutableList()
                testOps[opIdx] = candidateOp
                if (evaluateExpression(nums, testOps, mode) == result) {
                    validOpCount++
                }
            }
            return validOpCount == 1
        }
        return false
    }

    private fun pickOperand(maxOperand: Int, allowNegatives: Boolean, random: Random): Int {
        val magnitude = random.nextInt(1, maxOperand + 1)
        return if (allowNegatives && random.nextFloat() < 0.25f) -magnitude else magnitude
    }

    private fun pickValidOperators(
        nums: List<Int>,
        mode: EvaluationMode,
        minResult: Int,
        maxResult: Int,
        difficulty: Difficulty,
        random: Random
    ): Pair<List<MathOp>, Int>? {
        val opCount = nums.size - 1
        val candidates = mutableListOf<Pair<List<MathOp>, Int>>()

        if (opCount == 1) {
            for (op in MathOp.entries) {
                if (difficulty == Difficulty.EASY && op == MathOp.DIV && nums[0] == nums[1]) continue
                val res = evaluateExpression(nums, listOf(op), mode)
                if (res != null && res in minResult..maxResult) {
                    candidates.add(listOf(op) to res)
                }
            }
        } else if (opCount == 2) {
            for (op1 in MathOp.entries) {
                for (op2 in MathOp.entries) {
                    val ops = listOf(op1, op2)
                    val res = evaluateExpression(nums, ops, mode)
                    if (res != null && res in minResult..maxResult) {
                        candidates.add(ops to res)
                    }
                }
            }
        }

        if (candidates.isEmpty()) return null

        // Weight toward mixed precedence & division/multiplication variety for richer puzzles
        val weighted = mutableListOf<Pair<List<MathOp>, Int>>()
        for (pair in candidates) {
            val ops = pair.first
            var weight = 1
            if (ops.any { it == MathOp.DIV }) weight += 3
            if (ops.any { it == MathOp.MUL }) weight += 2
            if (ops.any { it == MathOp.MUL || it == MathOp.DIV } &&
                ops.any { it == MathOp.ADD || it == MathOp.SUB }
            ) {
                weight += 4
            }
            repeat(weight) { weighted.add(pair) }
        }
        return weighted[random.nextInt(weighted.size)]
    }

    private fun buildFallbackPuzzle(
        difficulty: Difficulty,
        evaluationMode: EvaluationMode,
        allowNegatives: Boolean,
        isDailyChallenge: Boolean,
        dailyDateLabel: String?
    ): CrossMathPuzzle {
        val size = difficulty.gridSize
        val equations = mutableListOf<Equation>()
        val cells: List<List<GridCell>>

        if (size == 5) {
            // 4 + 6 = 10; 8 - 3 = 5; 4 * 8 = 32; 6 / 3 = 2
            val layout = listOf(
                listOf(
                    GridCell(0, 0, CellKind.NUMBER, solutionNumber = 4, isFixed = false),
                    GridCell(0, 1, CellKind.OPERATOR, solutionOp = MathOp.ADD, isFixed = true),
                    GridCell(0, 2, CellKind.NUMBER, solutionNumber = 6, isFixed = true),
                    GridCell(0, 3, CellKind.EQUALS, isFixed = true),
                    GridCell(0, 4, CellKind.NUMBER, solutionNumber = 10, isFixed = false)
                ),
                listOf(
                    GridCell(1, 0, CellKind.OPERATOR, solutionOp = MathOp.MUL, isFixed = true),
                    GridCell(1, 1, CellKind.VOID),
                    GridCell(1, 2, CellKind.OPERATOR, solutionOp = MathOp.DIV, isFixed = true),
                    GridCell(1, 3, CellKind.VOID),
                    GridCell(1, 4, CellKind.VOID)
                ),
                listOf(
                    GridCell(2, 0, CellKind.NUMBER, solutionNumber = 8, isFixed = false),
                    GridCell(2, 1, CellKind.OPERATOR, solutionOp = MathOp.SUB, isFixed = true),
                    GridCell(2, 2, CellKind.NUMBER, solutionNumber = 3, isFixed = true),
                    GridCell(2, 3, CellKind.EQUALS, isFixed = true),
                    GridCell(2, 4, CellKind.NUMBER, solutionNumber = 5, isFixed = true)
                ),
                listOf(
                    GridCell(3, 0, CellKind.EQUALS, isFixed = true),
                    GridCell(3, 1, CellKind.VOID),
                    GridCell(3, 2, CellKind.EQUALS, isFixed = true),
                    GridCell(3, 3, CellKind.VOID),
                    GridCell(3, 4, CellKind.VOID)
                ),
                listOf(
                    GridCell(4, 0, CellKind.NUMBER, solutionNumber = 32, isFixed = true),
                    GridCell(4, 1, CellKind.VOID),
                    GridCell(4, 2, CellKind.NUMBER, solutionNumber = 2, isFixed = false),
                    GridCell(4, 3, CellKind.VOID),
                    GridCell(4, 4, CellKind.VOID)
                )
            )
            cells = layout
            equations.addAll(
                listOf(
                    Equation("R0", true, listOf(CellCoord(0, 0), CellCoord(0, 2)), listOf(CellCoord(0, 1)), CellCoord(0, 3), CellCoord(0, 4)),
                    Equation("R2", true, listOf(CellCoord(2, 0), CellCoord(2, 2)), listOf(CellCoord(2, 1)), CellCoord(2, 3), CellCoord(2, 4)),
                    Equation("C0", false, listOf(CellCoord(0, 0), CellCoord(2, 0)), listOf(CellCoord(1, 0)), CellCoord(3, 0), CellCoord(4, 0)),
                    Equation("C2", false, listOf(CellCoord(0, 2), CellCoord(2, 2)), listOf(CellCoord(1, 2)), CellCoord(3, 2), CellCoord(4, 2))
                )
            )
        } else {
            // Clean 7x7 PEMDAS / L->R safe fallback
            val opFixed = difficulty != Difficulty.HARD
            val nums = arrayOf(
                intArrayOf(6, 3, 2),
                intArrayOf(2, 4, 5),
                intArrayOf(3, 1, 4)
            )
            val hOps = arrayOf(
                arrayOf(MathOp.ADD, MathOp.MUL),
                arrayOf(MathOp.MUL, MathOp.ADD),
                arrayOf(MathOp.SUB, MathOp.MUL)
            )
            val vOps = arrayOf(
                arrayOf(MathOp.DIV, MathOp.MUL),
                arrayOf(MathOp.ADD, MathOp.SUB),
                arrayOf(MathOp.ADD, MathOp.SUB)
            )
            val hRes = (0..2).map { r ->
                evaluateExpression(nums[r].toList(), hOps[r].toList(), evaluationMode) ?: 0
            }
            val vRes = (0..2).map { c ->
                evaluateExpression(listOf(nums[0][c], nums[1][c], nums[2][c]), vOps[c].toList(), evaluationMode) ?: 0
            }

            val blanks = setOf(
                CellCoord(0, 0), CellCoord(0, 4), CellCoord(2, 2), CellCoord(4, 0),
                CellCoord(4, 4), CellCoord(0, 6), CellCoord(6, 2), CellCoord(2, 6)
            )
            val opBlanks = if (opFixed) emptySet() else setOf(CellCoord(0, 1), CellCoord(3, 2))

            val grid = MutableList(7) { r ->
                MutableList(7) { c ->
                    val coord = CellCoord(r, c)
                    when {
                        r in listOf(0, 2, 4) && c in listOf(0, 2, 4) ->
                            GridCell(r, c, CellKind.NUMBER, solutionNumber = nums[r / 2][c / 2], isFixed = coord !in blanks)
                        r in listOf(0, 2, 4) && c in listOf(1, 3) ->
                            GridCell(r, c, CellKind.OPERATOR, solutionOp = hOps[r / 2][c / 2], isFixed = coord !in opBlanks)
                        r in listOf(1, 3) && c in listOf(0, 2, 4) ->
                            GridCell(r, c, CellKind.OPERATOR, solutionOp = vOps[c / 2][r / 2], isFixed = coord !in opBlanks)
                        r in listOf(0, 2, 4) && c == 5 ->
                            GridCell(r, c, CellKind.EQUALS)
                        c in listOf(0, 2, 4) && r == 5 ->
                            GridCell(r, c, CellKind.EQUALS)
                        r in listOf(0, 2, 4) && c == 6 ->
                            GridCell(r, c, CellKind.NUMBER, solutionNumber = hRes[r / 2], isFixed = coord !in blanks)
                        c in listOf(0, 2, 4) && r == 6 ->
                            GridCell(r, c, CellKind.NUMBER, solutionNumber = vRes[c / 2], isFixed = coord !in blanks)
                        else -> GridCell(r, c, CellKind.VOID)
                    }
                }
            }
            cells = grid
            for (r in listOf(0, 2, 4)) {
                equations.add(
                    Equation("R$r", true, listOf(CellCoord(r, 0), CellCoord(r, 2), CellCoord(r, 4)), listOf(CellCoord(r, 1), CellCoord(r, 3)), CellCoord(r, 5), CellCoord(r, 6))
                )
            }
            for (c in listOf(0, 2, 4)) {
                equations.add(
                    Equation("C$c", false, listOf(CellCoord(0, c), CellCoord(2, c), CellCoord(4, c)), listOf(CellCoord(1, c), CellCoord(3, c)), CellCoord(5, c), CellCoord(6, c))
                )
            }
        }

        val statuses = evaluateAllEquations(cells, equations, evaluationMode)
        return CrossMathPuzzle(
            id = "fallback_${difficulty.name}",
            difficulty = difficulty,
            evaluationMode = evaluationMode,
            allowNegatives = allowNegatives,
            isDailyChallenge = isDailyChallenge,
            dailyDateLabel = dailyDateLabel,
            gridSize = size,
            cells = cells,
            equations = equations,
            equationStatuses = statuses
        )
    }
}
