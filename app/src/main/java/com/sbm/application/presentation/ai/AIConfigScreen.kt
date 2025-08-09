package com.sbm.application.presentation.ai

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.domain.model.*
import com.sbm.application.presentation.viewmodel.AIConfigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIConfigScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AIConfigViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val config = uiState.config

    // エラーメッセージの表示
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            // TODO: Snackbarでエラー表示
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("✨ AI 分析設定") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "戻る")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefaults() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "デフォルトに戻す")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                ConfigSection(
                    title = "📅 分析期間",
                    description = "直近1週間のデータを分析します"
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "📊",
                                fontSize = 20.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = "直近1週間",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "過去7日間のデータを分析",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // TODO: 将来的に比較分析機能を追加予定
            // item {
            //     ConfigSection(
            //         title = "📊 比較オプション", 
            //         description = "他の期間と比較して分析しますか？"
            //     ) {
            //         ComparisonOptionSelector(...)
            //     }
            // }

            item {
                ConfigSection(
                    title = "🎯 分析の焦点",
                    description = "何を重点的に分析しますか？"
                ) {
                    AnalysisFocusSelector(
                        selected = config.analysisFocus,
                        onSelectionChanged = viewModel::updateAnalysisFocus
                    )
                }
            }

            item {
                ConfigSection(
                    title = "📝 詳細レベル",
                    description = "どの程度詳しく分析しますか？"
                ) {
                    DetailLevelSelector(
                        selected = config.detailLevel,
                        onSelectionChanged = viewModel::updateDetailLevel
                    )
                }
            }

            item {
                ConfigSection(
                    title = "💬 応答スタイル",
                    description = "どんな口調で分析結果を受け取りますか？"
                ) {
                    ResponseStyleSelector(
                        selected = config.responseStyle,
                        onSelectionChanged = viewModel::updateResponseStyle
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ConfigSection(
    title: String,
    description: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

// AnalysisPeriodSelector は削除（期間固定のため不要）

// TODO: 将来的に比較分析機能を追加時に実装予定
// @Composable
// private fun ComparisonOptionSelector(...) { ... }

@Composable
private fun AnalysisFocusSelector(
    selected: AnalysisFocus,
    onSelectionChanged: (AnalysisFocus) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        AnalysisFocus.entries.forEach { focus ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == focus,
                        onClick = { onSelectionChanged(focus) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == focus,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = focus.emoji,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = focus.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = focus.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailLevelSelector(
    selected: DetailLevel,
    onSelectionChanged: (DetailLevel) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        DetailLevel.entries.forEach { level ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == level,
                        onClick = { onSelectionChanged(level) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == level,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = level.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = level.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ResponseStyleSelector(
    selected: ResponseStyle,
    onSelectionChanged: (ResponseStyle) -> Unit
) {
    Column(modifier = Modifier.selectableGroup()) {
        ResponseStyle.entries.forEach { style ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selected == style,
                        onClick = { onSelectionChanged(style) },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == style,
                    onClick = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = style.emoji,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = style.displayName,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = style.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}