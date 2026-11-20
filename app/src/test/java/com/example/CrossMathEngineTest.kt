package com.example

import com.example.engine.CellKind
import com.example.engine.CrossMathEngine
import com.example.engine.Difficulty
import com.example.engine.EquationStatus
import com.example.engine.EvaluationMode
import com.example.engine.MathOp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrossMathEngineTest {

    @Test
    fun pemdasPrecedenceEvaluatesMultiplicationAndDivisionFirst() {
        // 2 + 3 * 4 = 14 under PEMDAS, 20 under Left-to-Right
        val nums = listOf(2, 3, 4)
        val ops = listOf(MathOp.ADD, MathOp.MUL)
        assertEquals(14, CrossMathEngine.evaluateExpression(nums, ops, EvaluationMode.PEMDAS))
        assertEquals(20, CrossMathEngine.evaluateExpression(nums, ops, EvaluationMode.LEFT_TO_RIGHT))

        // 10 - 8 / 2 = 6 under PEMDAS, 1 under Left-to-Right
        assertEquals(6, CrossMathEngine.evaluateExpression(listOf(10, 8, 2), listOf(MathOp.SUB, MathOp.DIV), EvaluationMode.PEMDAS))
        assertEquals(1, CrossMathEngine.evaluateExpression(listOf(10, 8, 2), listOf(MathOp.SUB, MathOp.DIV), EvaluationMode.LEFT_TO_RIGHT))
    }

    @Test
    fun nonExactOrZeroDivisionReturnsNull() {
        assertNull(CrossMathEngine.evaluateExpression(listOf(7, 2), listOf(MathOp.DIV), EvaluationMode.PEMDAS))
        assertNull(CrossMathEngine.evaluateExpression(listOf(8, 0), listOf(MathOp.DIV), EvaluationMode.PEMDAS))
        assertNull(CrossMathEngine.evaluateExpression(listOf(2, 7, 2), listOf(MathOp.ADD, MathOp.DIV), EvaluationMode.PEMDAS))
    }

    @Test
    fun generatedPuzzlesAcrossAllDifficultiesHaveValidSolutionsAndAreSolvable() {
        for (difficulty in Difficulty.entries) {
            for (seedOffset in 1L..5L) {
                val puzzle = CrossMathEngine.generatePuzzle(
                    difficulty = difficulty,
                    evaluationMode = EvaluationMode.PEMDAS,
                    allowNegatives = (seedOffset % 2L == 0L),
                    seed = 10_000L + seedOffset
                )

                assertEquals(difficulty.gridSize, puzzle.gridSize)
                assertFalse(puzzle.isSolved)

                val editable = puzzle.cells.flatten().filter { it.isEditable }
                assertTrue("Expected editable cells for $difficulty", editable.isNotEmpty())

                if (difficulty == Difficulty.HARD) {
                    assertTrue(
                        "Hard difficulty should include missing operators",
                        editable.any { it.kind == CellKind.OPERATOR }
                    )
                }

                // Fill every editable cell with its stored solution and assert full puzzle win
                val solvedCells = puzzle.cells.map { row ->
                    row.map { cell ->
                        when {
                            !cell.isEditable -> cell
                            cell.kind == CellKind.NUMBER -> cell.copy(userNumberInput = cell.solutionNumber.toString())
                            cell.kind == CellKind.OPERATOR -> cell.copy(userOpInput = cell.solutionOp)
                            else -> cell
                        }
                    }
                }
                val statuses = CrossMathEngine.evaluateAllEquations(
                    solvedCells,
                    puzzle.equations,
                    puzzle.evaluationMode
                )
                assertTrue(
                    "All equations should evaluate VALID when solved for $difficulty",
                    statuses.values.all { it == EquationStatus.VALID }
                )
                val solvedPuzzle = puzzle.copy(cells = solvedCells, equationStatuses = statuses)
                assertTrue(solvedPuzzle.isSolved)
            }
        }
    }

    @Test
    fun dailyChallengeSeedProducesDeterministicPuzzle() {
        val p1 = CrossMathEngine.generatePuzzle(
            difficulty = Difficulty.MEDIUM,
            evaluationMode = EvaluationMode.PEMDAS,
            seed = 20261120L,
            isDailyChallenge = true,
            dailyDateLabel = "Nov 20"
        )
        val p2 = CrossMathEngine.generatePuzzle(
            difficulty = Difficulty.MEDIUM,
            evaluationMode = EvaluationMode.PEMDAS,
            seed = 20261120L,
            isDailyChallenge = true,
            dailyDateLabel = "Nov 20"
        )
        assertNotNull(p1)
        assertEquals(p1.cells, p2.cells)
    }
}
