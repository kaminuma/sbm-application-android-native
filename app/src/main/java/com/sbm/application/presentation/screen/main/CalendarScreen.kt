package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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

@Composable
fun WeeklyCalendarView(
    weekStart: LocalDate,
    activities: List<Activity>,
    moods: List<MoodRecord>,
    onMoodClick: (LocalDate, MoodRecord?) -> Unit,
    onActivityClick: (Activity) -> Unit,
    onTimeSlotClick: (LocalDate, Int) -> Unit,
    onDateHeaderClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val startHour = 0
    val endHour = 24 // 24時まで（23:59分まで対応）
    val totalHours = endHour - startHour
    val minuteHeight = 1.dp // 1分の高さ
    val hourHeight = minuteHeight * 60 // 1時間 = 60dp
    val totalGridHeight = hourHeight * totalHours
    
    val weekDays = (0..6).map { weekStart.plusDays(it.toLong()) }
    val activitiesByDate = activities.groupBy { it.date }
    val moodsByDate = moods.groupBy { it.date }
    
    Column(modifier = modifier.fillMaxSize()) {
        // ヘッダー行（曜日と日付）
        Row(modifier = Modifier.fillMaxWidth()) {
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
            
            weekDays.forEach { date ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onDateHeaderClick(date) },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.JAPANESE),
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
        
        // ムード行
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
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
            
            weekDays.forEach { date ->
                val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val dayMood = moodsByDate[dateString]?.firstOrNull()
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable { onMoodClick(date, dayMood) },
                    contentAlignment = Alignment.Center
                ) {
                    if (dayMood != null) {
                        val moodOption = MoodOptions.moodScale.find { it.value == dayMood.mood }
                        Text(
                            text = moodOption?.emoji ?: "😐",
                            style = MaterialTheme.typography.titleMedium
                        )
                    } else {
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
        
        // 時間軸グリッド（絶対配置で連続ブロック表示・スクロール対応）
        val scrollState = rememberScrollState()
        
        // 初期表示時に6時の位置にスクロール
        LaunchedEffect(Unit) {
            val targetHour = 6
            val scrollPosition = (hourHeight * targetHour).value.toInt()
            scrollState.scrollTo(scrollPosition)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // 残りの空間を使用
                .verticalScroll(scrollState)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(totalGridHeight)
            ) {
            // 時間軸ラベル列
            Column(
                modifier = Modifier
                    .width(60.dp)
                    .fillMaxHeight()
            ) {
                for (hour in startHour until endHour) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(hourHeight)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            ),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(
                            text = String.format("%02d:00", hour),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            
            // 日付ごとのグリッドとアクティビティ
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 60.dp)
            ) {
                weekDays.forEach { date ->
                    val dateString = date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    val dayActivities = activitiesByDate[dateString] ?: emptyList()
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        // 背景グリッド（30分間隔で表示、クリック用）
                        Column(modifier = Modifier.fillMaxSize()) {
                            for (hour in startHour until endHour) {
                                // 00分スロット
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(minuteHeight * 30) // 30分間隔
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                        .clickable { onTimeSlotClick(date, hour) }
                                )
                                // 30分スロット
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(minuteHeight * 30) // 30分間隔
                                        .border(
                                            width = 0.5.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                        .clickable { onTimeSlotClick(date, hour) }
                                )
                            }
                        }
                        
                        // アクティビティブロック（絶対配置・重複対応）
                        val overlappingGroups = groupOverlappingActivities(dayActivities)
                        overlappingGroups.forEach { group ->
                            if (group.size == 1) {
                                // 単独アクティビティ
                                ContinuousActivityBlock(
                                    activity = group.first(),
                                    startHour = startHour,
                                    minuteHeight = minuteHeight,
                                    onActivityClick = onActivityClick,
                                    widthFraction = 1f,
                                    horizontalOffset = 0f
                                )
                            } else {
                                // 重複アクティビティ群
                                group.forEachIndexed { index, activity ->
                                    ContinuousActivityBlock(
                                        activity = activity,
                                        startHour = startHour,
                                        minuteHeight = minuteHeight,
                                        onActivityClick = onActivityClick,
                                        widthFraction = 1f / group.size,
                                        horizontalOffset = index * (1f / group.size)
                                    )
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
 * 重複するアクティビティをグループ化する関数
 * 時間が重なるアクティビティ同士を同じグループにまとめる
 */
private fun groupOverlappingActivities(activities: List<Activity>): List<List<Activity>> {
    if (activities.isEmpty()) return emptyList()
    
    val groups = mutableListOf<MutableList<Activity>>()
    
    activities.forEach { activity ->
        val activityStart = timeToMinutes(activity.start)
        val activityEnd = timeToMinutes(activity.end)
        
        // 既存のグループで重複するものがあるかチェック
        val overlappingGroup = groups.find { group ->
            group.any { existing ->
                val existingStart = timeToMinutes(existing.start)
                val existingEnd = timeToMinutes(existing.end)
                
                // 時間が重複している場合
                !(activityEnd <= existingStart || activityStart >= existingEnd)
            }
        }
        
        if (overlappingGroup != null) {
            // 既存のグループに追加
            overlappingGroup.add(activity)
        } else {
            // 新しいグループを作成
            groups.add(mutableListOf(activity))
        }
    }
    
    return groups
}

/**
 * 時間文字列を分に変換するヘルパー関数
 */
private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hour * 60 + minute
}

@Composable
private fun ContinuousActivityBlock(
    activity: Activity,
    startHour: Int,
    minuteHeight: Dp,
    onActivityClick: (Activity) -> Unit,
    widthFraction: Float = 1f,
    horizontalOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    // 時間を分析してオフセットと高さを計算（1分単位）
    val startParts = activity.start.split(":")
    val endParts = activity.end.split(":")
    
    val activityStartHour = startParts.getOrNull(0)?.toIntOrNull() ?: 0
    val activityStartMinute = startParts.getOrNull(1)?.toIntOrNull() ?: 0
    val activityEndHour = endParts.getOrNull(0)?.toIntOrNull() ?: 0
    val activityEndMinute = endParts.getOrNull(1)?.toIntOrNull() ?: 0
    
    // 開始時刻からの総分数を計算
    val startTotalMinutes = (activityStartHour - startHour) * 60 + activityStartMinute
    val endTotalMinutes = (activityEndHour - startHour) * 60 + activityEndMinute
    val durationMinutes = (endTotalMinutes - startTotalMinutes).coerceAtLeast(1)
    
    // 位置とサイズ計算（1分 = 1dp）
    val topOffset = minuteHeight * startTotalMinutes
    val blockHeight = minuteHeight * durationMinutes
    
    val categoryColors = CategoryColors.getColorScheme(activity.category)
    
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(blockHeight)
            .offset(y = topOffset)
    ) {
        val containerWidth = maxWidth
        val blockWidth = containerWidth * widthFraction
        val leftOffset = containerWidth * horizontalOffset
        
        Card(
            onClick = { onActivityClick(activity) },
            modifier = modifier
                .width(blockWidth)
                .fillMaxHeight()
                .offset(x = leftOffset)
                .padding(horizontal = 1.dp, vertical = 1.dp),
            colors = CardDefaults.cardColors(
                containerColor = categoryColors.backgroundColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.Top
            ) {
                // タイトル
                Text(
                    text = if (widthFraction < 0.5f && activity.title.length > 8) {
                        // 狭いスペースの場合は短縮
                        activity.title.take(8) + "..."
                    } else {
                        activity.title
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = categoryColors.textColor,
                    maxLines = if (widthFraction < 0.5f) 1 else 2,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = if (widthFraction < 0.5f) 9.sp else 11.sp
                )
                
                // 時間表示（十分なスペースがある場合）
                if (durationMinutes >= 30 && widthFraction >= 0.6f) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${activity.start}-${activity.end}",
                        style = MaterialTheme.typography.labelSmall,
                        color = categoryColors.textColor.copy(alpha = 0.8f),
                        fontSize = 8.sp
                    )
                }
                
                // 内容表示（十分なスペースがある場合）
                if (durationMinutes >= 60 && widthFraction >= 0.8f && !activity.contents.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = activity.contents,
                        style = MaterialTheme.typography.bodySmall,
                        color = categoryColors.textColor.copy(alpha = 0.7f),
                        fontSize = 7.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
