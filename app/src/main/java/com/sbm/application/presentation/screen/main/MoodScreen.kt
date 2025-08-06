package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sbm.application.presentation.theme.CuteDesignSystem
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.domain.model.MoodRecord
import com.sbm.application.presentation.viewmodel.MoodViewModel
import com.sbm.application.presentation.components.MoodSelector
import com.sbm.application.presentation.components.MoodOptions
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodScreen(
    moodViewModel: MoodViewModel = hiltViewModel()
) {
    val viewModel = moodViewModel
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.loadMoodRecords()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (uiState.isLoading && uiState.moodRecords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    items(uiState.moodRecords) { moodRecord ->
                        MoodRecordItem(
                            moodRecord = moodRecord,
                            onDelete = {
                                viewModel.deleteMoodRecord(moodRecord.date)
                            }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
            
            uiState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
        
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(64.dp),
            containerColor = Color(0xFF64B5F6), // 優しい青（ムード用）
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "気分記録を追加",
                modifier = Modifier.size(28.dp)
            )
        }
    }
    
    if (showAddDialog) {
        AddMoodDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { date, mood, note ->
                viewModel.createMoodRecord(date, mood, note)
                showAddDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodRecordItem(
    moodRecord: MoodRecord,
    onDelete: () -> Unit
) {
    val moodOption = MoodOptions.moodScale.find { it.value == moodRecord.mood }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFCFA)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp, 
            moodOption?.color?.copy(alpha = 0.3f) ?: Color(0xFFE8F5E8)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 日付を強調表示
                Text(
                    text = moodRecord.date,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // 気分表示を簡素化
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = moodOption?.emoji ?: "😐",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = moodOption?.label ?: "普通",
                        style = MaterialTheme.typography.titleMedium,
                        color = moodOption?.color ?: MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                if (moodRecord.note?.isNotBlank() == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = moodRecord.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMoodDialog(
    initialDate: String? = null,
    existingMoodRecord: MoodRecord? = null,
    onDismiss: () -> Unit,
    onAdd: (String, Int, String?) -> Unit
) {
    val isEditMode = existingMoodRecord != null
    
    var date by remember { 
        mutableStateOf(
            existingMoodRecord?.date 
                ?: initialDate 
                ?: getCurrentDate()
        ) 
    }
    var mood by remember { mutableStateOf(existingMoodRecord?.mood ?: 3) }
    var note by remember { mutableStateOf(existingMoodRecord?.note ?: "") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // プラットフォームの幅制限を無効化
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f) // 横幅を95%に確実に設定
                .fillMaxHeight(0.80f), // 縦幅を80%に設定
            shape = CuteDesignSystem.Shapes.Large,
            color = CuteDesignSystem.Colors.Background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // タイトル
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 20.dp)
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = CuteDesignSystem.Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                    Text(
                        if (isEditMode) "😊 気分を更新" else "😊 気分を記録",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CuteDesignSystem.Colors.Primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // コンテンツエリア
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(CuteDesignSystem.Spacing.LG),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        // 日付表示（選択機能なし）
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CuteDesignSystem.Colors.AccentBlue.copy(alpha = 0.1f)
                            ),
                            shape = CuteDesignSystem.Shapes.Medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(CuteDesignSystem.Spacing.MD)
                            ) {
                                Text(
                                    text = "📅 記録日付",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CuteDesignSystem.Colors.Accent,
                                    modifier = Modifier.padding(bottom = CuteDesignSystem.Spacing.SM)
                                )
                                
                                // 日付を表示のみ（クリック不可）
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = CuteDesignSystem.Colors.Surface
                                    ),
                                    shape = CuteDesignSystem.Shapes.Medium,
                                    border = BorderStroke(1.dp, CuteDesignSystem.Colors.Accent.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = "日付",
                                            modifier = Modifier.size(20.dp),
                                            tint = CuteDesignSystem.Colors.Accent
                                        )
                                        Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                                        Text(
                                            text = try {
                                                val localDate = java.time.LocalDate.parse(date)
                                                "${localDate.year}年${localDate.monthValue}月${localDate.dayOfMonth}日 (${
                                                    localDate.dayOfWeek.getDisplayName(
                                                        java.time.format.TextStyle.SHORT,
                                                        java.util.Locale.JAPANESE
                                                    )
                                                })"
                                            } catch (e: Exception) {
                                                "日付が設定されていません"
                                            },
                                            fontWeight = FontWeight.Medium,
                                            color = CuteDesignSystem.Colors.Accent,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                
                                // カレンダーからの選択を促すヒント
                                Text(
                                    text = "※ カレンダータブから日付を選択して記録してください",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = CuteDesignSystem.Colors.OnSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                    
                    item {
                        // 気分選択
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CuteDesignSystem.Colors.PrimaryLight.copy(alpha = 0.1f)
                            ),
                            shape = CuteDesignSystem.Shapes.Medium
                        ) {
                            Column(
                                modifier = Modifier.padding(CuteDesignSystem.Spacing.MD)
                            ) {
                                Text(
                                    text = "😊 気分選択",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CuteDesignSystem.Colors.Primary,
                                    modifier = Modifier.padding(bottom = CuteDesignSystem.Spacing.SM)
                                )
                                
                                MoodSelector(
                                    selectedMood = mood,
                                    onMoodSelected = { mood = it }
                                )
                            }
                        }
                    }
                    
                    item {
                        // メモ入力
                        OutlinedTextField(
                            value = note,
                            onValueChange = { note = it },
                            label = { Text("メモ (任意)") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 100.dp),
                            maxLines = 4,
                            shape = CuteDesignSystem.Shapes.Medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CuteDesignSystem.Colors.Secondary,
                                focusedLabelColor = CuteDesignSystem.Colors.Secondary,
                                cursorColor = CuteDesignSystem.Colors.Secondary
                            )
                        )
                    }
                }
                
                // ボタンエリア
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = CuteDesignSystem.Colors.Secondary
                        ),
                        shape = CuteDesignSystem.Shapes.Medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.XS))
                        Text(
                            "キャンセル",
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = {
                            if (date.isNotBlank()) {
                                onAdd(date, mood, note.takeIf { it.isNotBlank() })
                            }
                        },
                        enabled = date.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CuteDesignSystem.Colors.Primary,
                            contentColor = CuteDesignSystem.Colors.OnPrimary
                        ),
                        shape = CuteDesignSystem.Shapes.Medium
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.XS))
                        Text(
                            if (isEditMode) "更新" else "保存",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}



private fun getCurrentDate(): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(Date())
}