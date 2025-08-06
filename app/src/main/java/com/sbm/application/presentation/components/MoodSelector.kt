package com.sbm.application.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class MoodOption(
    val value: Int,
    val emoji: String,
    val label: String,
    val color: Color
)

object MoodOptions {
    val moodScale = listOf(
        MoodOption(1, "😢", "とても悪い", Color(0xFFD32F2F)),    // WebUIに合わせて修正
        MoodOption(2, "😕", "悪い", Color(0xFFFF9800)),         // WebUIに合わせて修正
        MoodOption(3, "😐", "普通", Color(0xFF9E9E9E)),         // そのまま
        MoodOption(4, "🙂", "良い", Color(0xFF4CAF50)),         // WebUIに合わせて修正
        MoodOption(5, "😄", "とても良い", Color(0xFF00bfa5))    // WebUIに合わせて修正
    )
}

@Composable
fun MoodSelector(
    selectedMood: Int,
    onMoodSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "今日の気分はいかがですか？",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp), // 横のパディングを少し減らして幅を確保
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally) // スペースを調整
        ) {
            MoodOptions.moodScale.forEach { moodOption ->
                MoodCard(
                    moodOption = moodOption,
                    isSelected = selectedMood == moodOption.value,
                    onClick = { onMoodSelected(moodOption.value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodCard(
    moodOption: MoodOption,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(0.6f) // さらに縦長にしてテキスト領域を確保
            .selectable(
                selected = isSelected,
                onClick = onClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                moodOption.color.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.foundation.BorderStroke(
                    2.dp, 
                    moodOption.color
                ).brush
            )
        } else {
            CardDefaults.outlinedCardBorder()
        },
        elevation = if (isSelected) {
            CardDefaults.cardElevation(defaultElevation = 4.dp)
        } else {
            CardDefaults.cardElevation(defaultElevation = 1.dp)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp), // パディングを適度に調整
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly // 要素を等間隔で配置
        ) {
            Text(
                text = moodOption.emoji,
                style = MaterialTheme.typography.headlineSmall // サイズを少し小さく調整
            )
            Text(
                text = moodOption.label,
                style = MaterialTheme.typography.bodySmall, // サイズを調整
                textAlign = TextAlign.Center,
                maxLines = 2,
                minLines = 1, // 最低1行は確保
                color = if (isSelected) {
                    moodOption.color
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}