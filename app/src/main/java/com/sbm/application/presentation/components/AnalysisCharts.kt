package com.sbm.application.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sbm.application.presentation.theme.CuteDesignSystem
import com.sbm.application.presentation.viewmodel.CategoryData
import com.sbm.application.presentation.viewmodel.MoodTrendData
import kotlin.math.max

@Composable
fun CategoryChart(
    categoryData: List<CategoryData>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CuteDesignSystem.Colors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(CuteDesignSystem.Spacing.LG)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "カテゴリ分析",
                    tint = CuteDesignSystem.Colors.Secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                Text(
                    text = "📊 アクティビティカテゴリ",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CuteDesignSystem.Colors.Secondary
                )
            }
            
            Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
            
            if (categoryData.isNotEmpty()) {
                val maxHours = categoryData.maxOfOrNull { it.totalHours } ?: 1f
                
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    drawCategoryBarChart(
                        categoryData = categoryData.take(5),
                        maxHours = maxHours,
                        primaryColor = CuteDesignSystem.Colors.Secondary,
                        surfaceColor = CuteDesignSystem.Colors.SurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.MD))
                
                // Category labels with improved styling
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = CuteDesignSystem.Colors.SurfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(CuteDesignSystem.Spacing.MD)
                    ) {
                        categoryData.take(5).forEach { category ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(
                                                CuteDesignSystem.Colors.Secondary.copy(alpha = 0.7f),
                                                CircleShape
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                                    Text(
                                        text = category.category,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = CuteDesignSystem.Colors.OnSurface,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Text(
                                    text = "${String.format("%.1f", category.totalHours)}時間",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = CuteDesignSystem.Colors.Secondary
                                )
                            }
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CuteDesignSystem.Colors.SurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "データなし",
                                tint = CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.SM))
                            Text(
                                text = "アクティビティデータがありません",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CuteDesignSystem.Colors.OnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "アクティビティを登録して分析を開始しましょう",
                                style = MaterialTheme.typography.bodySmall,
                                color = CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MoodTrendChart(
    moodData: List<MoodTrendData>,
    selectedPeriod: String = "1ヶ月",
    startDate: String? = null,
    endDate: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CuteDesignSystem.Colors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(CuteDesignSystem.Spacing.LG)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "気分トレンド",
                    tint = CuteDesignSystem.Colors.Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                Text(
                    text = if (selectedPeriod == "カスタム" && startDate != null && endDate != null) {
                        "😊 気分トレンド（${startDate} ~ ${endDate}）"
                    } else {
                        "😊 気分トレンド（${selectedPeriod}）"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CuteDesignSystem.Colors.Primary
                )
            }
            
            Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
            
            if (moodData.isNotEmpty()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    drawMoodLineChart(
                        moodData = moodData,
                        primaryColor = CuteDesignSystem.Colors.Primary,
                        surfaceColor = CuteDesignSystem.Colors.SurfaceVariant
                    )
                }
            } else {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = CuteDesignSystem.Colors.SurfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = "データなし",
                                tint = CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.SM))
                            Text(
                                text = "気分データがありません",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CuteDesignSystem.Colors.OnSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "ムードタブから記録を開始しましょう",
                                style = MaterialTheme.typography.bodySmall,
                                color = CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsOverview(
    totalActivities: Int,
    averageMood: Float,
    mostActiveCategory: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CuteDesignSystem.Colors.Surface
        )
    ) {
        Column(
            modifier = Modifier.padding(CuteDesignSystem.Spacing.LG)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "統計概要",
                    tint = CuteDesignSystem.Colors.Secondary, // 青系に統一
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(CuteDesignSystem.Spacing.SM))
                Text(
                    text = "📈 統計概要",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = CuteDesignSystem.Colors.Secondary // 青系に統一
                )
            }
            
            Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CuteDesignSystem.Spacing.MD)
            ) {
                StatItem(
                    title = "総アクティビティ数",
                    value = totalActivities.toString(),
                    icon = Icons.Default.List,
                    color = CuteDesignSystem.Colors.Primary,
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    title = "平均気分",
                    value = if (averageMood > 0) String.format("%.1f", averageMood) else "データなし",
                    icon = Icons.Default.Favorite,
                    color = CuteDesignSystem.Colors.AccentPink,
                    modifier = Modifier.weight(1f)
                )
                
                StatItem(
                    title = "最多カテゴリ",
                    value = mostActiveCategory.ifEmpty { "データなし" },
                    icon = Icons.Default.Star,
                    color = CuteDesignSystem.Colors.Secondary, // 青系に変更
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(110.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(CuteDesignSystem.Spacing.MD)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.XS))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = CuteDesignSystem.Colors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun DrawScope.drawCategoryBarChart(
    categoryData: List<CategoryData>,
    maxHours: Float,
    primaryColor: Color,
    surfaceColor: Color
) {
    if (categoryData.isEmpty()) return
    
    val barWidth = size.width / (categoryData.size * 1.5f)
    val chartHeight = size.height * 0.8f
    val bottomMargin = size.height * 0.1f
    
    categoryData.forEachIndexed { index, data ->
        val barHeight = (data.totalHours / maxHours) * chartHeight
        val x = (index + 0.5f) * (size.width / categoryData.size)
        
        drawRect(
            color = primaryColor,
            topLeft = Offset(x - barWidth / 2, size.height - bottomMargin - barHeight),
            size = Size(barWidth, barHeight)
        )
    }
}

private fun DrawScope.drawMoodLineChart(
    moodData: List<MoodTrendData>,
    primaryColor: Color,
    surfaceColor: Color
) {
    if (moodData.size < 2) return
    
    val path = Path()
    val chartHeight = size.height * 0.8f
    val bottomMargin = size.height * 0.1f
    val leftMargin = size.width * 0.05f
    val rightMargin = size.width * 0.05f
    val chartWidth = size.width - leftMargin - rightMargin
    
    val minMood = 1f
    val maxMood = 5f
    val moodRange = maxMood - minMood
    
    moodData.forEachIndexed { index, data ->
        val x = leftMargin + (index.toFloat() / (moodData.size - 1)) * chartWidth
        val y = size.height - bottomMargin - ((data.mood - minMood) / moodRange) * chartHeight
        
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
        
        // Draw point
        drawCircle(
            color = primaryColor,
            radius = 6f,
            center = Offset(x, y)
        )
    }
    
    drawPath(
        path = path,
        color = primaryColor,
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
    )
}