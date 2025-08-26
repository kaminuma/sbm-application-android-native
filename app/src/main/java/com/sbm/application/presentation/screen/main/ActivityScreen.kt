package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*

import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.domain.model.Activity
import com.sbm.application.presentation.viewmodel.ActivityViewModel
import com.sbm.application.presentation.components.CategoryColors
import com.sbm.application.presentation.components.MobileTimePicker
import com.sbm.application.presentation.components.CategorySelector
import com.sbm.application.presentation.theme.CuteDesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    activityViewModel: ActivityViewModel = hiltViewModel()
) {
    val viewModel = activityViewModel
    val uiState by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    
    // Pull-to-Refresh状態
    val pullToRefreshState = rememberPullToRefreshState()
    
    LaunchedEffect(Unit) {
        viewModel.loadActivities()
    }
    
    // Pull-to-Refreshのトリガー
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refreshActivities()
        }
    }
    
    // リフレッシュ状態の監視
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            if (uiState.isLoading && uiState.activities.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn {
                    // 日付でグループ化し、各日付内で時間順にソート（日付は降順、時間は昇順）
                    val groupedAndSortedActivities = uiState.activities
                        .sortedWith(compareByDescending<Activity> { it.date }.thenBy { it.start })
                        .groupBy { it.date }
                    
                    groupedAndSortedActivities.forEach { (date, activitiesForDate) ->
                        // 日付ヘッダー
                        item {
                            Text(
                                text = try {
                                    val localDate = java.time.LocalDate.parse(date)
                                    "${localDate.monthValue}月${localDate.dayOfMonth}日 (${
                                        localDate.dayOfWeek.getDisplayName(
                                            java.time.format.TextStyle.SHORT,
                                            java.util.Locale.JAPANESE
                                        )
                                    })"
                                } catch (e: Exception) {
                                    date
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                            )
                        }
                        
                        // その日のアクティビティ一覧
                        items(activitiesForDate.sortedBy { it.start }) { activity ->
                            ActivityItem(
                                activity = activity,
                                onEdit = {
                                    activityToEdit = activity
                                    showEditDialog = true
                                },
                                onDelete = {
                                    viewModel.deleteActivity(activity.activityId)
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        // 日付グループ間のスペース
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
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
            containerColor = Color(0xFF66BB6A), // 優しい緑（アクティビティ用）
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "イベントを追加",
                modifier = Modifier.size(28.dp)
            )
        }
        
        // Pull-to-Refreshインジケーター
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
    
    if (showAddDialog) {
        AddActivityDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { title, contents, start, end, date, category, categorySub ->
                viewModel.createActivity(title, contents, start, end, date, category, categorySub)
                showAddDialog = false
            }
        )
    }
    
    // アクティビティ編集ダイアログ
    if (showEditDialog && activityToEdit != null) {
        AddActivityDialog(
            activityToEdit = activityToEdit,
            onDismiss = { 
                showEditDialog = false
                activityToEdit = null
            },
            onAdd = { _, _, _, _, _, _, _ -> }, // 編集モードでは使用しない
            onUpdate = { activityId, title, contents, start, end, date, category, categorySub ->
                viewModel.updateActivity(activityId, title, contents, start, end, date, category, categorySub)
                showEditDialog = false
                activityToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityItem(
    activity: Activity,
    onEdit: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    val categoryColors = CategoryColors.getColorScheme(activity.category)
    
    Card(
        onClick = { onEdit?.invoke() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFCFA) // 極薄い緑がかったホワイト
        ),
        border = BorderStroke(1.dp, categoryColors.backgroundColor.copy(alpha = 0.6f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .heightIn(min = 56.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左側：カテゴリカラーのアクセント
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = categoryColors.backgroundColor,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when(activity.category) {
                        "運動" -> "🏃"
                        "仕事" -> "💼"
                        "学習" -> "📚"
                        "趣味" -> "🎨"
                        "食事" -> "🍽️"
                        "睡眠" -> "😴"
                        "買い物" -> "🛒"
                        "娯楽" -> "🎮"
                        "休憩" -> "☕"
                        "家事" -> "🧹"
                        "通院" -> "🏥"
                        "散歩" -> "🚶"
                        else -> "📝"
                    },
                    fontSize = 20.sp
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryColors.textColor
                )
                if (activity.contents?.isNotBlank() == true) {
                    Text(
                        text = activity.contents,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF5A5A5A)
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = categoryColors.textColor,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF81C784)
                    )
                    Text(
                        text = "${activity.date} ${activity.start}-${activity.end}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF5A5A5A)
                    )
                }
            }
            
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = Color(0xFFFFEBEE),
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "削除",
                    tint = Color(0xFFEF5350), // 優しい赤色
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddActivityDialog(
    initialDate: String = "",
    initialStartHour: Int? = null,
    initialEndHour: Int? = null,
    activityToEdit: Activity? = null, // 編集用のアクティビティ
    onDismiss: () -> Unit,
    onAdd: (String, String?, String, String, String, String, String?) -> Unit,
    onUpdate: ((Long, String, String?, String, String, String, String, String?) -> Unit)? = null // 更新用コールバック
) {
    val isEditMode = activityToEdit != null
    
    var title by remember { mutableStateOf(activityToEdit?.title ?: "") }
    var contents by remember { mutableStateOf(activityToEdit?.contents ?: "") }
    var startTime by remember { 
        mutableStateOf(
            if (activityToEdit != null) {
                val timeParts = activityToEdit.start.split(":")
                java.time.LocalTime.of(
                    timeParts.getOrNull(0)?.toIntOrNull() ?: 9,
                    timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                )
            } else if (initialStartHour != null) {
                java.time.LocalTime.of(initialStartHour, 0)
            } else {
                java.time.LocalTime.of(9, 0)
            }
        )
    }
    var endTime by remember { 
        mutableStateOf(
            if (activityToEdit != null) {
                val timeParts = activityToEdit.end.split(":")
                java.time.LocalTime.of(
                    timeParts.getOrNull(0)?.toIntOrNull() ?: 10,
                    timeParts.getOrNull(1)?.toIntOrNull() ?: 0
                )
            } else if (initialEndHour != null) {
                java.time.LocalTime.of(initialEndHour, 0)
            } else if (initialStartHour != null) {
                java.time.LocalTime.of(initialStartHour + 1, 0)
            } else {
                java.time.LocalTime.of(10, 0)
            }
        )
    }
    var date by remember { 
        mutableStateOf(
            activityToEdit?.date ?: initialDate.ifEmpty { 
                java.time.LocalDate.now().toString() // 現在の日付を使用
            }
        ) 
    }
    var category by remember { mutableStateOf(activityToEdit?.category ?: "") }
    var categorySub by remember { mutableStateOf(activityToEdit?.categorySub ?: "") }
    
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm")
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // プラットフォームの幅制限を無効化
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f) // 横幅を95%に確実に設定
                .fillMaxHeight(0.90f), // 縦幅を90%に設定
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
                        imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        tint = CuteDesignSystem.Colors.Primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                    Text(
                        if (isEditMode) "✏️ イベントを更新" else "➕ 新規イベントを登録",
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
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("イベントタイトル") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 60.dp),
                            singleLine = false,
                            maxLines = 2,
                            shape = CuteDesignSystem.Shapes.Medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CuteDesignSystem.Colors.Primary,
                                focusedLabelColor = CuteDesignSystem.Colors.Primary,
                                cursorColor = CuteDesignSystem.Colors.Primary
                            )
                        )
                    }
                    
                    item {
                        OutlinedTextField(
                            value = contents,
                            onValueChange = { contents = it },
                            label = { Text("イベント詳細") },
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
                    
                    item {
                        var showDatePicker by remember { mutableStateOf(false) }
                        
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
                                    text = "📅 日付選択",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CuteDesignSystem.Colors.Accent,
                                    modifier = Modifier.padding(bottom = CuteDesignSystem.Spacing.SM)
                                )
                                
                                OutlinedButton(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = CuteDesignSystem.Colors.Accent,
                                        containerColor = CuteDesignSystem.Colors.Surface
                                    ),
                                    border = BorderStroke(1.dp, CuteDesignSystem.Colors.Accent),
                                    shape = CuteDesignSystem.Shapes.Medium
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DateRange,
                                        contentDescription = "日付選択",
                                        modifier = Modifier.size(20.dp)
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
                                            "日付を選択してください"
                                        },
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        
                        if (showDatePicker) {
                            // 簡単な日付入力のためのダイアログ
                            AlertDialog(
                                onDismissRequest = { showDatePicker = false },
                                title = { Text("日付を入力") },
                                text = {
                                    OutlinedTextField(
                                        value = date,
                                        onValueChange = { date = it },
                                        label = { Text("YYYY-MM-DD") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("OK")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showDatePicker = false }) {
                                        Text("キャンセル")
                                    }
                                }
                            )
                        }
                    }
                    
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = CuteDesignSystem.Colors.SurfaceVariant.copy(alpha = 0.3f)
                            ),
                            shape = CuteDesignSystem.Shapes.Medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(CuteDesignSystem.Spacing.MD)
                                    .heightIn(min = 80.dp), // 時間入力欄の最小高さを確保
                                horizontalArrangement = Arrangement.spacedBy(CuteDesignSystem.Spacing.MD)
                            ) {
                                MobileTimePicker(
                                    label = "開始時間",
                                    time = startTime,
                                    onTimeChanged = { startTime = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 70.dp) // 時間選択の高さを十分に確保
                                )
                                
                                MobileTimePicker(
                                    label = "終了時間", 
                                    time = endTime,
                                    onTimeChanged = { endTime = it },
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 70.dp) // 時間選択の高さを十分に確保
                                )
                            }
                        }
                    }
                    
                    item {
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
                                    text = "📋 カテゴリ選択",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = CuteDesignSystem.Colors.Primary,
                                    modifier = Modifier.padding(bottom = CuteDesignSystem.Spacing.SM)
                                )
                                CategorySelector(
                                    selectedCategory = category.takeIf { it.isNotBlank() },
                                    onCategorySelected = { newCategory -> 
                                        category = newCategory
                                        if (newCategory != "その他") {
                                            categorySub = ""
                                        }
                                    }
                                )
                            }
                        }
                    }
                    
                    item {
                        OutlinedTextField(
                            value = categorySub,
                            onValueChange = { categorySub = it },
                            label = { Text("サブカテゴリ (任意)") },
                            enabled = category == "その他",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp),
                            maxLines = 2,
                            shape = CuteDesignSystem.Shapes.Medium,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CuteDesignSystem.Colors.Accent,
                                focusedLabelColor = CuteDesignSystem.Colors.Accent,
                                cursorColor = CuteDesignSystem.Colors.Accent
                            )
                        )
                        if (category != "その他") {
                            Text(
                                text = "※ 「その他」カテゴリ選択時のみ入力可能",
                                style = MaterialTheme.typography.bodySmall,
                                color = CuteDesignSystem.Colors.OnSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
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
                            if (title.isNotBlank() && category.isNotBlank() && date.isNotBlank() && startTime.isBefore(endTime)) {
                                if (isEditMode && activityToEdit != null && onUpdate != null) {
                                    onUpdate(
                                        activityToEdit.activityId,
                                        title, 
                                        contents.takeIf { it.isNotBlank() }, 
                                        startTime.format(timeFormatter), 
                                        endTime.format(timeFormatter), 
                                        date, 
                                        category, 
                                        categorySub.takeIf { it.isNotBlank() }
                                    )
                                } else {
                                    onAdd(
                                        title, 
                                        contents.takeIf { it.isNotBlank() }, 
                                        startTime.format(timeFormatter), 
                                        endTime.format(timeFormatter), 
                                        date, 
                                        category, 
                                        categorySub.takeIf { it.isNotBlank() }
                                    )
                                }
                            }
                        },
                        enabled = title.isNotBlank() && category.isNotBlank() && date.isNotBlank() && startTime.isBefore(endTime),
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