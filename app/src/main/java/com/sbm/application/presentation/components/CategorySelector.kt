package com.sbm.application.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background

data class ActivityCategory(
    val name: String,
    val emoji: String,
    val color: Color
)

object Categories {
    // WebUIと完全一致するカテゴリリスト（ScheduleView.vue line 458-472と同一順序・色設定）
    val predefinedCategories = listOf(
        ActivityCategory("運動", "🏃", Color(0xFF01579b)),    // WebUI: bg:#e0f7fa, text:#01579b
        ActivityCategory("仕事", "💼", Color(0xFF0d47a1)),    // WebUI: bg:#e3f2fd, text:#0d47a1
        ActivityCategory("学習", "📚", Color(0xFF4a148c)),    // WebUI: bg:#f3e5f5, text:#4a148c
        ActivityCategory("趣味", "🎨", Color(0xFFe65100)),    // WebUI: bg:#fff3e0, text:#e65100
        ActivityCategory("食事", "🍽️", Color(0xFFb71c1c)),   // WebUI: bg:#ffebee, text:#b71c1c
        ActivityCategory("睡眠", "😴", Color(0xFF1a237e)),    // WebUI: bg:#e8eaf6, text:#1a237e
        ActivityCategory("買い物", "🛒", Color(0xFF004d40)),   // WebUI: bg:#e0f2f1, text:#004d40
        ActivityCategory("娯楽", "🎮", Color(0xFF880e4f)),    // WebUI: bg:#fce4ec, text:#880e4f
        ActivityCategory("休憩", "☕", Color(0xFF33691e)),    // WebUI: bg:#f1f8e9, text:#33691e
        ActivityCategory("家事", "🧹", Color(0xFF3e2723)),    // WebUI: bg:#efebe9, text:#3e2723
        ActivityCategory("通院", "🏥", Color(0xFF263238)),    // WebUI: bg:#eceff1, text:#263238 (新規追加)
        ActivityCategory("散歩", "🚶", Color(0xFF827717)),    // WebUI: bg:#f9fbe7, text:#827717 (新規追加)
        ActivityCategory("その他", "📝", Color(0xFF212121))   // WebUI: bg:#f5f5f5, text:#212121
    )
}

/**
 * 緑・青ベースの可愛いカテゴリ色定義
 * 自然で落ち着いた印象を与えつつ、愛らしさを演出
 */
object CategoryColors {
    data class CategoryColorScheme(
        val backgroundColor: Color,
        val textColor: Color
    )
    
    // 緑・青ベースの可愛いカテゴリカラー
    private val categoryColorMap = mapOf(
        "運動" to CategoryColorScheme(
            backgroundColor = Color(0xFFE8F5E8), // 薄い緑
            textColor = Color(0xFF2E7D32)        // 濃い緑
        ),
        "仕事" to CategoryColorScheme(
            backgroundColor = Color(0xFFE3F2FD), // 薄い青
            textColor = Color(0xFF1565C0)        // 濃い青
        ),
        "学習" to CategoryColorScheme(
            backgroundColor = Color(0xFFE0F2F1), // 薄いティール
            textColor = Color(0xFF00695C)        // 濃いティール
        ),
        "趣味" to CategoryColorScheme(
            backgroundColor = Color(0xFFF1F8E9), // 薄いライムグリーン
            textColor = Color(0xFF558B2F)        // 濃いライムグリーン
        ),
        "食事" to CategoryColorScheme(
            backgroundColor = Color(0xFFE8F6F3), // 薄いエメラルド
            textColor = Color(0xFF00796B)        // 濃いエメラルド
        ),
        "睡眠" to CategoryColorScheme(
            backgroundColor = Color(0xFFE1F5FE), // 薄いライトブルー
            textColor = Color(0xFF0277BD)        // 濃いライトブルー
        ),
        "買い物" to CategoryColorScheme(
            backgroundColor = Color(0xFFE0F7FA), // 薄いシアン
            textColor = Color(0xFF00838F)        // 濃いシアン
        ),
        "娯楽" to CategoryColorScheme(
            backgroundColor = Color(0xFFE8EAF6), // 薄いインディゴ
            textColor = Color(0xFF3F51B5)        // 濃いインディゴ
        ),
        "休憩" to CategoryColorScheme(
            backgroundColor = Color(0xFFF3E5F5), // 薄いパープル（アクセント）
            textColor = Color(0xFF7B1FA2)        // 濃いパープル
        ),
        "家事" to CategoryColorScheme(
            backgroundColor = Color(0xFFEFEBE9), // 薄いブラウン（自然色）
            textColor = Color(0xFF5D4037)        // 濃いブラウン
        ),
        "通院" to CategoryColorScheme(
            backgroundColor = Color(0xFFF9FBE7), // 薄いイエローグリーン
            textColor = Color(0xFF689F38)        // 濃いイエローグリーン
        ),
        "散歩" to CategoryColorScheme(
            backgroundColor = Color(0xFFE4F7F7), // 薄いアクア
            textColor = Color(0xFF00897B)        // 濃いアクア
        ),
        "その他" to CategoryColorScheme(
            backgroundColor = Color(0xFFF5F5F5), // 薄いグレー
            textColor = Color(0xFF424242)        // 濃いグレー
        )
    )
    
    /**
     * カテゴリに対応する色を取得
     */
    fun getColorScheme(category: String): CategoryColorScheme {
        return categoryColorMap[category] ?: categoryColorMap["その他"]!!
    }
    
    /**
     * カテゴリの背景色を取得
     */
    fun getBackgroundColor(category: String): Color {
        return getColorScheme(category).backgroundColor
    }
    
    /**
     * カテゴリの文字色を取得
     */
    fun getTextColor(category: String): Color {
        return getColorScheme(category).textColor
    }
}

@Composable
fun CategorySelector(
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "カテゴリ",
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF2E7D32), // 緑ベースのテキスト色
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.height(220.dp)
        ) {
            items(Categories.predefinedCategories) { category ->
                CategoryCard(
                    category = category,
                    isSelected = selectedCategory == category.name,
                    onClick = { onCategorySelected(category.name) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryCard(
    category: ActivityCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val categoryColors = CategoryColors.getColorScheme(category.name)
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp), // 少し高くして余裕を持たせる
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                categoryColors.backgroundColor
            } else {
                Color(0xFFFAFCFA) // 極薄い緑がかったホワイト
            }
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 6.dp else 3.dp
        ),
        shape = RoundedCornerShape(14.dp), // より丸みを帯びた可愛い形
        border = if (isSelected) {
            BorderStroke(2.dp, categoryColors.textColor.copy(alpha = 0.5f))
        } else {
            BorderStroke(1.dp, Color(0xFFE8F5E8))
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // 絵文字を大きく表示
                Text(
                    text = category.emoji,
                    fontSize = 24.sp, // 大きくして可愛さアップ
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                // カテゴリ名
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) {
                        categoryColors.textColor
                    } else {
                        Color(0xFF5A5A5A)
                    },
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}