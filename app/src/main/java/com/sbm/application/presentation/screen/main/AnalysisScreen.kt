package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.Divider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.presentation.theme.CuteDesignSystem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.sbm.application.presentation.viewmodel.AnalysisViewModel
import com.sbm.application.presentation.components.CategoryChart
import com.sbm.application.presentation.components.MoodTrendChart
import com.sbm.application.presentation.components.StatsOverview
import com.sbm.application.presentation.components.AIAnalysisSection
import com.sbm.application.presentation.components.AIUsageDisplay
import com.sbm.application.presentation.components.RateLimitDialog
import com.sbm.application.presentation.components.LowUsageWarningDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    onNavigateToAIConfig: () -> Unit = {},
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // LazyListStateを管理してスクロール位置を保持
    val listState = rememberLazyListState()
    
    // 期間選択の状態管理
    var selectedPeriod by remember { mutableStateOf("1週間") }
    var showDatePicker by remember { mutableStateOf(false) }
    var startDate by remember { 
        mutableStateOf(
            java.time.LocalDate.now().minusWeeks(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
    var endDate by remember { 
        mutableStateOf(
            java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
    }
    
    // 期間変更時の処理を最適化
    LaunchedEffect(selectedPeriod) {
        when (selectedPeriod) {
            "1週間" -> {
                val today = java.time.LocalDate.now()
                val newStartDate = today.minusWeeks(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val newEndDate = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                if (startDate != newStartDate || endDate != newEndDate) {
                    startDate = newStartDate
                    endDate = newEndDate
                    viewModel.setDateRange(startDate, endDate)
                }
            }
            "1ヶ月" -> {
                val today = java.time.LocalDate.now()
                val newStartDate = today.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val newEndDate = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                if (startDate != newStartDate || endDate != newEndDate) {
                    startDate = newStartDate
                    endDate = newEndDate
                    viewModel.setDateRange(startDate, endDate)
                }
            }
            "3ヶ月" -> {
                val today = java.time.LocalDate.now()
                val newStartDate = today.minusMonths(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val newEndDate = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                if (startDate != newStartDate || endDate != newEndDate) {
                    startDate = newStartDate
                    endDate = newEndDate
                    viewModel.setDateRange(startDate, endDate)
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        // デフォルトで1週間のデータを読み込み
        val today = java.time.LocalDate.now()
        val weekAgo = today.minusWeeks(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val todayStr = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        viewModel.setDateRange(weekAgo, todayStr)
        
        // 使用状況も初期ロード
        viewModel.loadUsageInfo()
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "データ読み込みエラー",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = uiState.error ?: "不明なエラー",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "データ分析",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // AI利用状況表示
                item {
                    AIUsageDisplay(
                        usageInfo = uiState.aiUsageInfo,
                        showDetailed = false
                    )
                }
                
                // AI分析セクション - 一番上に配置
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        AIAnalysisSection(
                            canGenerate = uiState.canGenerateAI,
                            canUseToday = uiState.aiUsageInfo?.canUseToday ?: true,
                            isLoading = uiState.isAiLoading,
                            insight = uiState.aiInsight,
                            error = uiState.aiError,
                            onGenerateClick = { viewModel.generateAIInsight() },
                            onConfigureClick = onNavigateToAIConfig,
                            modifier = Modifier.widthIn(max = 600.dp)
                        )
                    }
                }
                
                // 期間選択セクション
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "分析期間",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // 期間選択ボタン - 改善されたデザイン
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(CuteDesignSystem.Spacing.SM)
                            ) {
                                listOf("1週間", "1ヶ月", "3ヶ月", "カスタム").forEach { period ->
                                    val isSelected = selectedPeriod == period
                                    Card(
                                        onClick = { 
                                            if (period == "カスタム") {
                                                showDatePicker = true
                                            } else {
                                                // 期間変更時はスクロール位置を保持
                                                selectedPeriod = period
                                            }
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected) {
                                                CuteDesignSystem.Colors.Secondary
                                            } else {
                                                CuteDesignSystem.Colors.Surface
                                            }
                                        ),
                                        shape = RoundedCornerShape(CuteDesignSystem.Spacing.MD),
                                        elevation = if (isSelected) {
                                            CardDefaults.cardElevation(defaultElevation = 4.dp)
                                        } else {
                                            CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        },
                                        border = if (isSelected) {
                                            null
                                        } else {
                                            BorderStroke(1.dp, CuteDesignSystem.Colors.Secondary.copy(alpha = 0.3f))
                                        }
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = CuteDesignSystem.Spacing.MD),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = period,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) {
                                                    CuteDesignSystem.Colors.OnSecondary
                                                } else {
                                                    CuteDesignSystem.Colors.Secondary
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 現在の期間表示 - ユーザーフレンドリーに改善
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DateRange,
                                        contentDescription = "期間",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = formatDateRange(startDate, endDate),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
                
                item {
                    StatsOverview(
                        totalActivities = uiState.totalActivities,
                        averageMood = uiState.averageMood,
                        mostActiveCategory = uiState.mostActiveCategory
                    )
                }
                
                item {
                    CategoryChart(
                        categoryData = uiState.categoryData
                    )
                }
                
                item {
                    MoodTrendChart(
                        moodData = uiState.moodTrendData,
                        selectedPeriod = selectedPeriod,
                        startDate = if (selectedPeriod == "カスタム") startDate else null,
                        endDate = if (selectedPeriod == "カスタム") endDate else null
                    )
                }
                
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
    
    // カスタム期間選択ダイアログ
    if (showDatePicker) {
        CustomDateRangePickerDialog(
            startDate = startDate,
            endDate = endDate,
            onDismiss = { showDatePicker = false },
            onDateRangeSelected = { newStartDate, newEndDate ->
                startDate = newStartDate
                endDate = newEndDate
                selectedPeriod = "カスタム"
                showDatePicker = false
                viewModel.loadAnalysisData(startDate, endDate)
            }
        )
    }
    
    // 制限到達ダイアログ
    if (uiState.showRateLimitDialog) {
        RateLimitDialog(
            usageInfo = uiState.aiUsageInfo,
            onDismiss = { viewModel.dismissRateLimitDialog() }
        )
    }
    
    // 利用制限警告ダイアログ
    if (uiState.showLowUsageWarning) {
        uiState.aiUsageInfo?.let { usageInfo ->
            LowUsageWarningDialog(
                usageInfo = usageInfo,
                onDismiss = { viewModel.dismissLowUsageWarning() },
                onProceed = { viewModel.proceedWithLowUsage() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDateRangePickerDialog(
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onDateRangeSelected: (String, String) -> Unit
) {
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    val dateRangePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = runCatching {
            LocalDate.parse(startDate, dateFormatter)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull(),
        initialSelectedEndDateMillis = runCatching {
            LocalDate.parse(endDate, dateFormatter)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)  // 95%の幅で余裕を持たせる
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                // カスタムヘッダー
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "期間を選択",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // 選択された期間の表示
                        val startMillis = dateRangePickerState.selectedStartDateMillis
                        val endMillis = dateRangePickerState.selectedEndDateMillis
                        if (startMillis != null && endMillis != null) {
                            val displayDateFormatter = DateTimeFormatter.ofPattern("yyyy年M月d日")
                            val start = Instant.ofEpochMilli(startMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(displayDateFormatter)
                            val end = Instant.ofEpochMilli(endMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                                .format(displayDateFormatter)
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = start,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " 〜 ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = end,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Divider(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                // DateRangePicker
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    showModeToggle = false,
                    title = null,  // デフォルトタイトルを非表示
                    headline = null  // デフォルトヘッドラインも非表示
                )
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("キャンセル")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(
                        onClick = {
                            val startMillis = dateRangePickerState.selectedStartDateMillis
                            val endMillis = dateRangePickerState.selectedEndDateMillis
                            
                            if (startMillis != null && endMillis != null) {
                                val startFormatted = Instant.ofEpochMilli(startMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(dateFormatter)
                                val endFormatted = Instant.ofEpochMilli(endMillis)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                    .format(dateFormatter)
                                onDateRangeSelected(startFormatted, endFormatted)
                            }
                        },
                        enabled = dateRangePickerState.selectedStartDateMillis != null && 
                                 dateRangePickerState.selectedEndDateMillis != null
                    ) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

/**
 * 開始日と終了日を読みやすい形式でフォーマットする
 */
private fun formatDateRange(startDate: String, endDate: String): String {
    return try {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val displayFormatter = DateTimeFormatter.ofPattern("M/d")
        
        val start = LocalDate.parse(startDate, formatter)
        val end = LocalDate.parse(endDate, formatter)
        
        "${start.format(displayFormatter)} - ${end.format(displayFormatter)}"
    } catch (e: Exception) {
        "$startDate - $endDate"
    }
}