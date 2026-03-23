package com.ykatchou.ylauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ALPHABET = listOf('#') + ('A'..'Z').toList()

@Composable
fun AlphabetSidebar(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isDragging by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableIntStateOf(-1) }
    var columnHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current

    Box(modifier = modifier) {
        // The bubble showing current letter (appears to the left when dragging)
        if (isDragging && selectedIndex in ALPHABET.indices) {
            val letterHeight = if (columnHeight > 0) columnHeight.toFloat() / ALPHABET.size else 0f
            val yOffset = (selectedIndex * letterHeight + letterHeight / 2 - with(density) { 20.dp.toPx() }).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(-with(density) { 52.dp.roundToPx() }, yOffset) }
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = ALPHABET[selectedIndex].toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        // The vertical letter strip
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(24.dp)
                .padding(vertical = 4.dp)
                .onSizeChanged { columnHeight = it.height }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val index = (offset.y / size.height * ALPHABET.size)
                            .toInt()
                            .coerceIn(0, ALPHABET.size - 1)
                        onLetterSelected(ALPHABET[index])
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            val index = (offset.y / size.height * ALPHABET.size)
                                .toInt()
                                .coerceIn(0, ALPHABET.size - 1)
                            selectedIndex = index
                            onLetterSelected(ALPHABET[index])
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val index = (change.position.y / size.height * ALPHABET.size)
                                .toInt()
                                .coerceIn(0, ALPHABET.size - 1)
                            if (index != selectedIndex) {
                                selectedIndex = index
                                onLetterSelected(ALPHABET[index])
                            }
                        },
                        onDragEnd = { isDragging = false },
                        onDragCancel = { isDragging = false },
                    )
                },
            verticalArrangement = Arrangement.SpaceEvenly,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ALPHABET.forEachIndexed { index, letter ->
                Text(
                    text = letter.toString(),
                    fontSize = 10.sp,
                    fontWeight = if (isDragging && index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDragging && index == selectedIndex)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
