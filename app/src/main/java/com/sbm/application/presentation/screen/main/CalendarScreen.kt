package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.RoundedCornerShape
import java.time.format.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.domain.model.Activity
import com.sbm.application.domain.model.MoodRecord
import com.sbm.application.presentation.viewmodel.ActivityViewModel
import com.sbm.application.presentation.viewmodel.MoodViewModel
import com.sbm.application.presentation.components.MoodOptions
import com.sbm.application.presentation.components.CategoryColors
import java.time.DayOfWeek
import java.util.Locale
import io.github.boguszpawlowski.composecalendar.SelectableCalendar
import io.github.boguszpawlowski.composecalendar.day.DayState
import io.github.boguszpawlowski.composecalendar.rememberSelectableCalendarState
import io.github.boguszpawlowski.composecalendar.selection.DynamicSelectionState
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    activityViewModel: ActivityViewModel,
    moodViewModel: MoodViewModel
) {
    val viewModel = activityViewModel
    val uiState by viewModel.uiState.collectAsState()
    val moodUiState by moodViewModel.uiState.collectAsState()
    
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var activityToEdit by remember { mutableStateOf<Activity?>(null) }
    var showMoodDialog by remember { mutableStateOf(false) }
    var selectedDateString by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))) }
    var selectedStartHour by remember { mutableStateOf(9) } // デフォルト開始時間
    var selectedEndHour by remember { mutableStateOf(10) } // デフォルト終了時間
    var selectedMoodDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedMoodRecord by remember { mutableStateOf<MoodRecord?>(null) }
    
    // Pull-to-Refresh状態
    val pullToRefreshState = rememberPullToRefreshState()
    
    // 表示モード（月表示/週表示）
    var isWeeklyView by remember { mutableStateOf(true) }
    var currentWeekStart by remember { mutableStateOf(LocalDate.now().with(DayOfWeek.MONDAY)) }
    
    val calendarState = rememberSelectableCalendarState()
    val today = remember { LocalDate.now() }
    
    LaunchedEffect(Unit) {
        viewModel.loadActivities()
        moodViewModel.loadMoodRecords()
        calendarState.selectionState.onDateSelected(today)
    }
    
    // Pull-to-Refreshのトリガー
    LaunchedEffect(pullToRefreshState.isRefreshing) {
        if (pullToRefreshState.isRefreshing) {
            viewModel.refreshActivities()
            moodViewModel.refreshMoodRecords()
        }
    }
    
    // リフレッシュ状態の監視（moodViewModelにrefreshMoodRecordsがない場合は一旦コメントアウト）
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing && pullToRefreshState.isRefreshing) {
            pullToRefreshState.endRefresh()
        }
    }
    
    // 選択された日付の変更を監視
    LaunchedEffect(calendarState.selectionState.selection) {
        val selectedDate = calendarState.selectionState.selection.firstOrNull()
        selectedDateString = selectedDate?.let { 
            it.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } ?: ""
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(pullToRefreshState.nestedScrollConnection)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp) // 緑・青テーマに合わせてゆったりと
        ) {
            // 表示切替タブ
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = !isWeeklyView,
                    onClick = { isWeeklyView = false },
                    label = { Text("月表示") },
                    modifier = Modifier.padding(end = 10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF66BB6A), // 優しい緑
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF1F8F6),
                        labelColor = Color(0xFF2E7D32)
                    )
                )
                FilterChip(
                    selected = isWeeklyView,
                    onClick = { isWeeklyView = true },
                    label = { Text("週表示") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF64B5F6), // 優しい青
                        selectedLabelColor = Color.White,
                        containerColor = Color(0xFFF0F8FF),
                        labelColor = Color(0xFF1565C0)
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (isWeeklyView) {
                // 週ナビゲーション
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { currentWeekStart = currentWeekStart.minusWeeks(1) }
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, "前の週")
                    }
                    
                    Text(
                        text = "${currentWeekStart.format(DateTimeFormatter.ofPattern("M月d日"))} - ${currentWeekStart.plusDays(6).format(DateTimeFormatter.ofPattern("M月d日"))}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = { currentWeekStart = currentWeekStart.plusWeeks(1) }
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, "次の週")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 週表示カレンダー
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAFCFA) // 極薄い緑がかったホワイト
                    )
                ) {
                    WeeklyCalendarView(
                        weekStart = currentWeekStart,
                        activities = uiState.activities,
                        moods = moodUiState.moodRecords,
                        onMoodClick = { date, mood ->
                            selectedMoodDate = date
                            selectedMoodRecord = mood
                            showMoodDialog = true
                        },
                        onActivityClick = { activity ->
                            activityToEdit = activity
                            showEditDialog = true
                        },
                        onTimeSlotClick = { date, hour ->
                            selectedDateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            selectedStartHour = hour
                            selectedEndHour = hour + 1
                            showAddDialog = true
                        },
                        onDateHeaderClick = { date ->
                            selectedDateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            // 月表示モードに切り替えて該当日を表示
                            isWeeklyView = false
                        },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            } else {
                // 月表示カレンダー
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFAFCFA) // 極薄い緑がかったホワイト
                    )
                ) {
                    SelectableCalendar(
                        calendarState = calendarState,
                        modifier = Modifier.padding(16.dp),
                        dayContent = { dayState ->
                            CalendarDay(
                                dayState = dayState,
                                activities = uiState.activities,
                                onClick = { 
                                    calendarState.selectionState.onDateSelected(dayState.date)
                                }
                            )
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 選択された日付のアクティビティ表示
                if (selectedDateString.isNotEmpty()) {
                    val dayActivities = uiState.activities.filter { it.date == selectedDateString }
                    
                    Text(
                        text = "${selectedDateString}のアクティビティ",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (dayActivities.isEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "この日にはアクティビティがありません",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        LazyColumn {
                            items(dayActivities) { activity ->
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
                        }
                    }
                }
            }
            
            // エラー表示
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(8.dp))
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
        
        // フローティングアクションボタン（可愛い緑デザイン）
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .size(64.dp), // 少し大きくして存在感をアップ
            containerColor = Color(0xFF66BB6A), // 優しい緑
            contentColor = Color.White,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = 8.dp,
                pressedElevation = 12.dp
            )
        ) {
            Icon(
                Icons.Default.Add, 
                contentDescription = "イベントを追加",
                modifier = Modifier.size(28.dp) // アイコンも少し大きく
            )
        }
        
        // Pull-to-Refreshインジケーター
        PullToRefreshContainer(
            state = pullToRefreshState,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
    
    // 新規アクティビティ追加ダイアログ
    if (showAddDialog) {
        AddActivityDialog(
            initialDate = selectedDateString,
            initialStartHour = selectedStartHour,
            initialEndHour = selectedEndHour,
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
    
    // ムード追加/編集ダイアログ
    if (showMoodDialog && selectedMoodDate != null) {
        AddMoodDialog(
            initialDate = selectedMoodDate?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            existingMoodRecord = selectedMoodRecord,
            onDismiss = { 
                showMoodDialog = false
                selectedMoodDate = null
                selectedMoodRecord = null
            },
            onAdd = { date, mood, note ->
                if (selectedMoodRecord != null) {
                    // 編集
                    moodViewModel.updateMoodRecord(date, mood, note)
                } else {
                    // 新規追加
                    moodViewModel.createMoodRecord(date, mood, note)
                }

                showMoodDialog = false
                selectedMoodDate = null
                selectedMoodRecord = null
            }
        )
    }
}

@Composable
fun CalendarDay(
    dayState: DayState<DynamicSelectionState>,
    activities: List<Activity>,
    onClick: () -> Unit
) {
    val dateString = dayState.date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    val dayActivities = activities.filter { it.date == dateString }
    val hasActivities = dayActivities.isNotEmpty()
    
    // 主要なカテゴリの色を取得（最初のアクティビティのカテゴリを使用）
    val primaryCategoryColor = if (dayActivities.isNotEmpty()) {
        CategoryColors.getColorScheme(dayActivities.first().category)
    } else null
    
    val backgroundColor = when {
        dayState.selectionState.isDateSelected(dayState.date) -> MaterialTheme.colorScheme.primary
        hasActivities && primaryCategoryColor != null -> primaryCategoryColor.backgroundColor
        else -> Color.Transparent
    }
    
    val textColor = when {
        dayState.selectionState.isDateSelected(dayState.date) -> MaterialTheme.colorScheme.onPrimary
        hasActivities && primaryCategoryColor != null -> primaryCategoryColor.textColor
        dayState.isFromCurrentMonth -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }
    
    Box(
        modifier = Modifier
            .size(40.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dayState.date.dayOfMonth.toString(),
                        color = textColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                    // アクティビティ数インジケーター（複数ある場合）
                    if (hasActivities && dayActivities.size > 1 && !dayState.selectionState.isDateSelected(dayState.date)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            modifier = Modifier.padding(top = 1.dp)
                        ) {
                            repeat(minOf(dayActivities.size, 3)) { index ->
                                val categoryColor = CategoryColors.getColorScheme(dayActivities.getOrNull(index)?.category ?: "その他")
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .background(
                                            color = categoryColor.textColor,
                                            shape = CircleShape
                                        )
                                )
                            }
                        }
                    } else if (hasActivities && !dayState.selectionState.isDateSelected(dayState.date)) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .padding(top = 1.dp),
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = primaryCategoryColor?.textColor ?: MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxSize()
                            ) {}
                        }
                    }
                }
            }
        }
    }
}

/**
 * 週表示カレンダーコンポーネント
 */
@Composable
fun WeeklyCalendarView(
    weekStart: LocalDate,
    activities: List<Activity>,
    moods: List<MoodRecord>,
    onMoodClick: (LocalDate, MoodRecord?) -> Unit,
    onActivityClick: (Activity) -> Unit,
    onTimeSlotClick: (LocalDate, Int) -> Unit,
    onDateHeaderClick: (LocalDate) -> Unit, // 日付ヘッダークリック用コールバック
    modifier: Modifier = Modifier
) {
    // 30分間隔で6:00-23:30まで表示
    val timeSlots = mutableListOf<Pair<Int, Int>>()
    for (hour in 6..23) {
        timeSlots.add(Pair(hour, 0))  // xx:00
        timeSlots.add(Pair(hour, 30)) // xx:30
    }
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    
    // 日付別のデータを整理
    val activitiesByDate = activities.groupBy { it.date }
    val moodsByDate = moods.groupBy { it.date }
    
    LazyColumn(
        modifier = modifier.fillMaxSize()
    ) {
        // ヘッダー行（曜日と日付）
        item {
            Row(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 時間列のヘッダー
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "時間",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 各日のヘッダー
                weekDays.forEach { date ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                onDateHeaderClick(date)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = date.dayOfWeek.getDisplayName(
                                    TextStyle.SHORT,
                                    Locale.JAPANESE
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${date.monthValue}/${date.dayOfMonth}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
        
        // ムード行
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                // ムード列のラベル
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .height(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "気分",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // 各日のムード
                weekDays.forEach { date ->
                    val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val dayMood = moodsByDate[dateString]?.firstOrNull()
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clickable {
                                onMoodClick(date, dayMood)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (dayMood != null) {
                            val moodOption = MoodOptions.moodScale.find { it.value == dayMood.mood }
                            Text(
                                text = moodOption?.emoji ?: "😐",
                                style = MaterialTheme.typography.titleMedium
                            )
                        } else {
                            // 空の場合はプラスアイコン表示
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "ムード追加",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // 時間軸とアクティビティ（改良版：連続ブロック表示・重複対応）
        items(timeSlots) { timeSlot ->
            val (hour, minute) = timeSlot
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                // 時間ラベル
                Box(
                    modifier = Modifier
                        .width(60.dp)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d:%02d", hour, minute),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // 各日の時間スロット
                weekDays.forEach { date ->
                    val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val dayActivities = activitiesByDate[dateString] ?: emptyList()
                    
                    // この時間スロットに重複するすべてのアクティビティを取得
                    val currentSlotMinutes = hour * 60 + minute
                    val overlappingActivities = dayActivities.filter { activity ->
                        val startParts = activity.start.split(":")
                        val endParts = activity.end.split(":")
                        
                        val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: -1
                        val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
                        val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: -1
                        val endMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0
                        
                        val activityStartMinutes = startHour * 60 + startMinute
                        val activityEndMinutes = endHour * 60 + endMinute
                        
                        // 現在のスロットがアクティビティの時間範囲内にある場合
                        currentSlotMinutes >= activityStartMinutes && currentSlotMinutes < activityEndMinutes
                    }
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            )
                            .clickable {
                                if (overlappingActivities.isNotEmpty()) {
                                    onActivityClick(overlappingActivities.first())
                                } else {
                                    onTimeSlotClick(date, hour)
                                }
                            }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            overlappingActivities.isEmpty() -> {
                                // 空のスロット - 何も表示しない
                            }
                            overlappingActivities.size == 1 -> {
                                // 単一のアクティビティ
                                val activity = overlappingActivities.first()
                                val isStartSlot = isActivityStartSlot(activity, hour, minute)
                                
                                if (isStartSlot) {
                                    // 開始スロットの場合、従来のフルブロック表示
                                    ActivityBlock(
                                        activity = activity,
                                        showAsFullBlock = true
                                    )
                                } else {
                                    // 継続スロットの場合、統合された背景色表示
                                    UnifiedContinuationBlock(activity = activity)
                                }
                            }
                            else -> {
                                // 複数のアクティビティが重複（並列表示）
                                Row(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    overlappingActivities.forEachIndexed { index, activity ->
                                        val isStartSlot = isActivityStartSlot(activity, hour, minute)
                                        
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight()
                                                .padding(horizontal = 1.dp)
                                        ) {
                                            if (isStartSlot) {
                                                ActivityBlock(
                                                    activity = activity,
                                                    showAsFullBlock = false // 重複時は簡略表示
                                                )
                                            } else {
                                                UnifiedContinuationBlock(activity = activity)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * アクティビティが指定した時間スロットで開始するかどうかを判定
 */
private fun isActivityStartSlot(activity: Activity, hour: Int, minute: Int): Boolean {
    val startParts = activity.start.split(":")
    val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: -1
    val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
    return startHour == hour && startMinute == minute
}

/**
 * アクティビティの持続時間（30分単位のスロット数）を計算
 */
private fun calculateActivityDurationSlots(activity: Activity): Float {
    val startParts = activity.start.split(":")
    val endParts = activity.end.split(":")
    
    val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: 0
    val startMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
    val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 0
    val endMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0
    
    val startTimeMinutes = startHour * 60 + startMinute
    val endTimeMinutes = endHour * 60 + endMinute
    val durationMinutes = endTimeMinutes - startTimeMinutes
    
    return (durationMinutes / 30f).coerceAtLeast(1f)
}

/**
 * 改善されたアクティビティブロック表示コンポーネント
 * Web版風の統一感のある表示を実現
 */
@Composable
private fun ActivityBlock(
    activity: Activity,
    showAsFullBlock: Boolean,
    modifier: Modifier = Modifier
) {
    val categoryColors = CategoryColors.getColorScheme(activity.category)
    
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = categoryColors.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        shape = if (showAsFullBlock) RoundedCornerShape(8.dp) else RoundedCornerShape(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (showAsFullBlock) 6.dp else 4.dp),
            contentAlignment = if (showAsFullBlock) Alignment.TopCenter else Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = if (showAsFullBlock) Arrangement.Top else Arrangement.Center
            ) {
                // タイトル
                Text(
                    text = if (showAsFullBlock) {
                        activity.title
                    } else {
                        if (activity.title.length > 4) activity.title.take(4) + "..." else activity.title
                    },
                    style = if (showAsFullBlock) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelSmall,
                    color = categoryColors.textColor,
                    maxLines = if (showAsFullBlock) 2 else 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                if (showAsFullBlock) {
                    Spacer(modifier = Modifier.height(3.dp))
                    
                    // 時間情報
                    Text(
                        text = "${activity.date} ${activity.start}-${activity.end}",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColors.textColor.copy(alpha = 0.8f),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    // 内容（スペースに余裕がある場合）
                    if (activity.contents?.isNotBlank() == true && activity.contents.length <= 20) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = activity.contents,
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColors.textColor.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

/**
 * アクティビティ継続表示コンポーネント（より明確な継続表示）
 */
@Composable 
private fun ActivityContinuation(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val categoryColors = CategoryColors.getColorScheme(activity.category)
    
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = categoryColors.backgroundColor.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(2.dp),
            contentAlignment = Alignment.Center
        ) {
            // 継続を示す縦線
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(
                        color = categoryColors.textColor.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(1.5.dp)
                    )
            )
        }
    }
}

/**
 * 改善された継続表示コンポーネント（Web版スタイルの一体感を演出）
 * 縦線ではなく、統合された背景で一体感を表現
 */
@Composable
private fun UnifiedContinuationBlock(
    activity: Activity,
    modifier: Modifier = Modifier
) {
    val categoryColors = CategoryColors.getColorScheme(activity.category)
    
    Card(
        modifier = modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = categoryColors.backgroundColor.copy(alpha = 0.85f) // より強い一体感
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(0.dp) // 継続部分は角丸なしで統一感
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            // 左端に太いアクセントライン（Web版風の統一感演出）
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp)
                    .background(
                        color = categoryColors.textColor.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.CenterStart)
            )
            
            // 継続を示すテキスト（短時間のアクティビティでも表示）
            if (activity.title.length <= 8) {
                Text(
                    text = "●",
                    color = categoryColors.textColor.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
