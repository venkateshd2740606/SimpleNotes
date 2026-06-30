package com.simplenotes.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.simplenotes.R
import com.simplenotes.domain.model.SimpleNotesGame
import com.simplenotes.engine.SimpleNotesPalette

@Composable
fun SimpleNotesBoard(
    game: SimpleNotesGame,
    reducedMotion: Boolean,
    onTubeClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val boardDescription = stringResource(R.string.color_sort)
    val capacity = game.level.tubeCapacity
    val tubeWidth = 52.dp
    val segmentHeight = 28.dp
    val tubeHeight = segmentHeight * capacity + 16.dp

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .semantics { contentDescription = boardDescription },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.Bottom
        ) {
            game.tubes.forEachIndexed { index, tube ->
                val selected = game.selectedTubeId == index
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable { onTubeClick(index) }
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(tubeWidth)
                            .height(tubeHeight)
                            .clip(RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                            .border(
                                width = if (selected) 3.dp else 2.dp,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline
                                },
                                shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                            )
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            repeat(capacity) { slot ->
                                val slotIndex = capacity - 1 - slot
                                val colorIndex = tube.getOrNull(slotIndex)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(segmentHeight)
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (colorIndex != null) {
                                                SimpleNotesPalette.colorForIndex(colorIndex)
                                            } else {
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.2f)
                                            }
                                        )
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
fun GameStatChip(label: String, value: String, modifier: Modifier = Modifier) {
    Text(
        text = "$label: $value",
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
    )
}
