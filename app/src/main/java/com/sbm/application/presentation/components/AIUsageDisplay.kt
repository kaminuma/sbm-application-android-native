package com.sbm.application.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sbm.application.config.ApiConfig
import com.sbm.application.domain.model.AIUsageInfo

/**
 * AI利用状況を表示するUIコンポーネント
 */
@Composable
fun AIUsageDisplay(
    usageInfo: AIUsageInfo?,
    modifier: Modifier = Modifier,
    showDetailed: Boolean = false
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                usageInfo?.isUnlimited == true -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                usageInfo?.needsWarning == true -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // タイトルとメイン情報
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI分析利用状況",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (usageInfo != null) {
                    StatusBadge(usageInfo)
                }
            }
            
            if (usageInfo != null) {
                // 使用状況テキスト
                Text(
                    text = usageInfo.usageDisplayText,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = when {
                        usageInfo.isUnlimited -> MaterialTheme.colorScheme.primary
                        usageInfo.needsWarning -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                // 制限が有効な場合はプログレスバーを表示
                if (!usageInfo.isUnlimited) {
                    LinearProgressIndicator(
                        progress = { usageInfo.progressRatio },
                        modifier = Modifier.fillMaxWidth(),
                        color = when {
                            usageInfo.needsWarning -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                    )
                }
                
                // 警告メッセージ
                if (usageInfo.needsWarning) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "警告",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "残り${usageInfo.dailyRemaining}回です。ご注意ください。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                
                // 詳細情報表示
                if (showDetailed) {
                    DetailedUsageInfo(usageInfo)
                }
                
                // デバッグ情報（デバッグビルドのみ）
                if (true && (usageInfo.debugMode || usageInfo.isDebugUser || !usageInfo.limitsEnabled)) {
                    DebugInfo(usageInfo)
                }
            } else {
                // ローディング状態
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = "利用状況を取得中...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(usageInfo: AIUsageInfo) {
    val (text, backgroundColor) = when {
        usageInfo.debugMode -> "DEBUG" to MaterialTheme.colorScheme.primary
        usageInfo.isDebugUser -> "DEV" to MaterialTheme.colorScheme.primary
        !usageInfo.limitsEnabled -> "UNLIMITED" to MaterialTheme.colorScheme.primary
        !usageInfo.canUseToday -> "LIMIT" to MaterialTheme.colorScheme.error
        usageInfo.needsWarning -> "LOW" to MaterialTheme.colorScheme.error
        else -> "OK" to MaterialTheme.colorScheme.primary
    }
    
    Box(
        modifier = Modifier
            .background(
                backgroundColor.copy(alpha = 0.2f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = backgroundColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailedUsageInfo(usageInfo: AIUsageInfo) {
    if (!usageInfo.isUnlimited) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Divider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "今日",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${usageInfo.dailyUsed} / ${usageInfo.dailyLimit}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "今月",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${usageInfo.monthlyUsed} / ${usageInfo.monthlyLimit}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
            
            if (usageInfo.nextResetDate.isNotEmpty()) {
                Text(
                    text = "リセット: ${usageInfo.nextResetDate}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun DebugInfo(usageInfo: AIUsageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3F51B5).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = "🔧 Debug Info",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF3F51B5)
            )
            
            Text(
                text = "Debug Mode: ${usageInfo.debugMode}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Text(
                text = "Debug User: ${usageInfo.isDebugUser}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Text(
                text = "Limits Enabled: ${usageInfo.limitsEnabled}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
            
            Text(
                text = "Can Use Today: ${usageInfo.canUseToday}",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
            )
        }
    }
}

/**
 * 簡易版の使用状況表示（ボタンの上などに表示）
 */
@Composable
fun AIUsageIndicator(
    usageInfo: AIUsageInfo?,
    modifier: Modifier = Modifier
) {
    if (usageInfo != null) {
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ステータスアイコン
            val (icon, color) = when {
                usageInfo.isUnlimited -> "🔧" to MaterialTheme.colorScheme.primary
                !usageInfo.canUseToday -> "🚫" to MaterialTheme.colorScheme.error
                usageInfo.needsWarning -> "⚠️" to MaterialTheme.colorScheme.error
                else -> "✓" to MaterialTheme.colorScheme.primary
            }
            
            Text(
                text = icon,
                fontSize = 12.sp
            )
            
            Text(
                text = if (usageInfo.isUnlimited) {
                    "無制限"
                } else {
                    "${usageInfo.dailyRemaining}回残り"
                },
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}