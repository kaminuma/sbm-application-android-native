package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
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
    
    LaunchedEffect(selectedPeriod, startDate, endDate) {
        when (selectedPeriod) {
            "1週間" -> {
                val today = java.time.LocalDate.now()
                startDate = today.minusWeeks(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            "1ヶ月" -> {
                val today = java.time.LocalDate.now()
                startDate = today.minusMonths(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
            "3ヶ月" -> {
                val today = java.time.LocalDate.now()
                startDate = today.minusMonths(3).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                endDate = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            }
        }
        viewModel.loadAnalysisData(startDate, endDate)
    }
    
    LaunchedEffect(Unit) {
        // デフォルトで1週間のデータを読み込み
        viewModel.loadAnalysisData(startDate, endDate)
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
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column {
                // DateRangePicker
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f),
                    showModeToggle = false
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