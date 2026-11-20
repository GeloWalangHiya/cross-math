package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.CellCoord
import com.example.engine.CellKind
import com.example.engine.CrossMathPuzzle
import com.example.engine.EquationStatus
import com.example.engine.GridCell
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LocalCrossMathColors

@Composable
fun CrossMathBoard(
    puzzle: CrossMathPuzzle,
    selectedCoord: CellCoord?,
    isSwapMode: Boolean,
    swapFirstCoord: CellCoord?,
    onCellClick: (CellCoord) -> Unit,
    modifier: Modifier = Modifier
) {
    val semantic = LocalCrossMathColors.current

    // Determine which equations intersect the selected cell for subtle lane highlighting
    val activeEquations = remember(puzzle, selectedCoord) {
        if (selectedCoord == null) emptyList()
        else puzzle.equations.filter { selectedCoord in it.allVariableCoords }
    }
    val activeCoords = remember(activeEquations) {
        activeEquations.flatMap { it.allVariableCoords }.toSet()
    }

    // Precompute cell validation statuses from equations
    val cellStatusMap = remember(puzzle) {
        val map = mutableMapOf<CellCoord, EquationStatus>()
        for (r in 0 until puzzle.gridSize) {
            for (c in 0 until puzzle.gridSize) {
                val coord = CellCoord(r, c)
                val containing = puzzle.equations.filter {
                    coord in it.allVariableCoords || coord == it.equalsCoord
                }
                if (containing.isEmpty()) continue
                val statuses = containing.mapNotNull { puzzle.equationStatuses[it.id] }
                map[coord] = when {
                    statuses.any { it == EquationStatus.INVALID } -> EquationStatus.INVALID
                    statuses.isNotEmpty() && statuses.all { it == EquationStatus.VALID } -> EquationStatus.VALID
                    else -> EquationStatus.NEUTRAL
                }
            }
        }
        map
    }

    Surface(
        modifier = modifier.widthIn(max = 500.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, semantic.gridLine.copy(alpha = 0.7f))
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            // Subtle architectural dot/graph grid backdrop
            val gridLineColor = semantic.gridLine.copy(alpha = 0.25f)
            Canvas(modifier = Modifier.matchParentSize()) {
                val step = 20.dp.toPx()
                var x = step
                while (x < size.width) {
                    var y = step
                    while (y < size.height) {
                        drawCircle(
                            color = gridLineColor,
                            radius = 1.dp.toPx(),
                            center = Offset(x, y)
                        )
                        y += step
                    }
                    x += step
                }
            }

            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                val spacing: Dp = if (puzzle.gridSize == 5) 8.dp else 4.dp
                val totalSpacing = spacing * (puzzle.gridSize - 1)
                val cellSize = ((maxWidth - totalSpacing) / puzzle.gridSize).coerceIn(36.dp, 68.dp)

                Column(
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    for (r in 0 until puzzle.gridSize) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (c in 0 until puzzle.gridSize) {
                                val cell = puzzle.cells[r][c]
                                val coord = cell.coord
                                val status = cellStatusMap[coord] ?: EquationStatus.NEUTRAL
                                val isSelected = selectedCoord == coord
                                val isSwapSource = swapFirstCoord == coord
                                val isInActiveLane = coord in activeCoords

                                GridCellTile(
                                    cell = cell,
                                    gridSize = puzzle.gridSize,
                                    cellSize = cellSize,
                                    status = status,
                                    isSelected = isSelected,
                                    isSwapMode = isSwapMode,
                                    isSwapSource = isSwapSource,
                                    isInActiveLane = isInActiveLane,
                                    onClick = { onCellClick(coord) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GridCellTile(
    cell: GridCell,
    gridSize: Int,
    cellSize: Dp,
    status: EquationStatus,
    isSelected: Boolean,
    isSwapMode: Boolean,
    isSwapSource: Boolean,
    isInActiveLane: Boolean,
    onClick: () -> Unit
) {
    val semantic = LocalCrossMathColors.current

    if (cell.kind == CellKind.VOID) {
        Box(modifier = Modifier.size(cellSize))
        return
    }

    val scale by animateFloatAsState(
        targetValue = if (isSelected || isSwapSource) 1.06f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "cell_scale"
    )

    if (cell.kind == CellKind.EQUALS) {
        val eqBg by animateColorAsState(
            targetValue = when (status) {
                EquationStatus.VALID -> semantic.validContainer
                EquationStatus.INVALID -> semantic.invalidContainer
                EquationStatus.NEUTRAL -> Color.Transparent
            },
            label = "eq_bg"
        )
        val eqColor by animateColorAsState(
            targetValue = when (status) {
                EquationStatus.VALID -> semantic.valid
                EquationStatus.INVALID -> semantic.invalid
                EquationStatus.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            label = "eq_fg"
        )
        Box(
            modifier = Modifier
                .size(cellSize)
                .clip(CircleShape)
                .background(eqBg)
                .semantics {
                    contentDescription = when (status) {
                        EquationStatus.VALID -> "Equals sign, equation valid"
                        EquationStatus.INVALID -> "Equals sign, equation incorrect"
                        EquationStatus.NEUTRAL -> "Equals sign"
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (status == EquationStatus.VALID) "=" else "=",
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (gridSize == 5) 22.sp else 18.sp,
                color = eqColor
            )
        }
        return
    }

    val shape = if (cell.kind == CellKind.OPERATOR) {
        CircleShape
    } else {
        RoundedCornerShape(if (gridSize == 5) 14.dp else 10.dp)
    }

    val targetBg = when {
        isSwapSource -> semantic.swapSourceBg
        isSelected -> semantic.selectedTileBg
        cell.kind == CellKind.OPERATOR && cell.isFixed -> semantic.operatorTileBg
        cell.kind == CellKind.OPERATOR && !cell.isFixed -> {
            when (status) {
                EquationStatus.VALID -> semantic.validContainer
                EquationStatus.INVALID -> semantic.invalidContainer
                EquationStatus.NEUTRAL -> if (cell.isHinted) semantic.streakContainer else semantic.editableTileBg
            }
        }
        cell.isFixed -> when (status) {
            EquationStatus.VALID -> semantic.validContainer.copy(alpha = 0.55f)
            EquationStatus.INVALID -> semantic.invalidContainer.copy(alpha = 0.55f)
            EquationStatus.NEUTRAL -> if (isInActiveLane) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                semantic.fixedTileBg
            }
        }
        else -> when {
            status == EquationStatus.VALID -> semantic.validContainer
            status == EquationStatus.INVALID -> semantic.invalidContainer
            cell.isHinted -> semantic.streakContainer
            isInActiveLane -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            else -> semantic.editableTileBg
        }
    }

    val bgColor by animateColorAsState(targetValue = targetBg, label = "tile_bg")

    val borderColor by animateColorAsState(
        targetValue = when {
            isSwapSource -> semantic.streak
            isSelected -> MaterialTheme.colorScheme.primary
            status == EquationStatus.INVALID && cell.isEditable -> semantic.invalid
            status == EquationStatus.VALID && cell.isEditable -> semantic.valid
            cell.isHinted -> semantic.streak
            cell.isEditable && isSwapMode -> semantic.streak.copy(alpha = 0.65f)
            cell.isEditable -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
            else -> semantic.gridLine.copy(alpha = 0.45f)
        },
        label = "tile_border"
    )

    val borderWidth = if (isSelected || isSwapSource) 2.5.dp else if (cell.isEditable) 1.5.dp else 1.dp

    val displayText = when (cell.kind) {
        CellKind.NUMBER -> if (cell.isFixed) {
            cell.solutionNumber?.toString().orEmpty()
        } else {
            cell.userNumberInput
        }
        CellKind.OPERATOR -> if (cell.isFixed) {
            cell.solutionOp?.symbol.orEmpty()
        } else {
            cell.userOpInput?.symbol ?: "?"
        }
        else -> ""
    }

    val textColor = when {
        isSwapSource -> semantic.onStreakContainer
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        status == EquationStatus.VALID -> semantic.onValidContainer
        status == EquationStatus.INVALID -> semantic.onInvalidContainer
        cell.isHinted -> semantic.onStreakContainer
        cell.isEditable && displayText.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
        cell.kind == CellKind.OPERATOR && !cell.isFixed && cell.userOpInput == null ->
            MaterialTheme.colorScheme.primary
        cell.isEditable -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    val fontSize = when {
        cell.kind == CellKind.OPERATOR -> if (gridSize == 5) 22.sp else 17.sp
        displayText.length >= 3 -> if (gridSize == 5) 16.sp else 12.sp
        displayText.length == 2 -> if (gridSize == 5) 20.sp else 15.sp
        else -> if (gridSize == 5) 22.sp else 17.sp
    }

    val interactionSource = remember { MutableInteractionSource() }
    val cellDesc = buildString {
        append("Row ${cell.row + 1} Column ${cell.col + 1}, ")
        if (cell.isFixed) append("fixed ") else append("editable ")
        append(if (cell.kind == CellKind.NUMBER) "number " else "operator ")
        append(displayText.ifEmpty { "blank" })
    }

    Box(
        modifier = Modifier
            .size(cellSize)
            .scale(scale)
            .clip(shape)
            .background(bgColor)
            .border(borderWidth, borderColor, shape)
            .then(
                if (cell.isEditable) {
                    Modifier
                        .testTag("cell_${cell.row}_${cell.col}")
                        .clickable(
                            interactionSource = interactionSource,
                            indication = ripple(bounded = true),
                            onClick = onClick
                        )
                } else {
                    Modifier
                }
            )
            .semantics { contentDescription = cellDesc },
        contentAlignment = Alignment.Center
    ) {
        if (displayText.isNotEmpty()) {
            Text(
                text = displayText,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = if (cell.isEditable) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = fontSize,
                color = textColor,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        } else if (cell.isEditable) {
            // Subtle cursor dot in empty editable tile
            Box(
                modifier = Modifier
                    .size(if (isSelected) 7.dp else 4.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
            )
        }
    }
}
