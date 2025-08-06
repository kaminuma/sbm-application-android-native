package com.sbm.application.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 緑・青ベースの可愛いデザインシステム
 * 自然で落ち着いた印象を与えつつ、愛らしさを演出
 */
object CuteDesignSystem {
    
    // 🌿 メインカラーパレット（緑・青ベース）
    object Colors {
        // プライマリカラー（優しい緑系）
        val Primary = Color(0xFF66BB6A)           // 優しいミントグリーン
        val PrimaryVariant = Color(0xFF4CAF50)    // 少し濃い緑
        val PrimaryLight = Color(0xFFA5D6A7)      // 薄い緑
        
        // セカンダリカラー（優しい青系）
        val Secondary = Color(0xFF64B5F6)         // 優しいスカイブルー
        val SecondaryVariant = Color(0xFF42A5F5)  // 少し濃い青
        val SecondaryLight = Color(0xFF90CAF9)    // 薄い青
        
        // アクセントカラー（自然系）
        val Accent = Color(0xFF81C784)            // フレッシュグリーン
        val AccentBlue = Color(0xFF81D4FA)        // ライトブルー
        val AccentPink = Color(0xFFF8BBD9)       // 桃色系アクセント
        
        // 背景色（温かみのあるニュートラル）
        val Background = Color(0xFFF8FFF8)        // 極薄いミントホワイト
        val Surface = Color(0xFFFFFFFF)           // ピュアホワイト
        val SurfaceVariant = Color(0xFFF1F8F6)    // 薄いミントグレー
        val SurfaceTint = Color(0xFFF0F8FF)       // 薄いスカイブルー
        
        // テキストカラー
        val OnPrimary = Color(0xFFFFFFFF)
        val OnSecondary = Color(0xFFFFFFFF)
        val OnSurface = Color(0xFF2E2E2E)
        val OnSurfaceVariant = Color(0xFF5A5A5A)
        
        // エラー・警告色（優しく）
        val Error = Color(0xFFEF5350)
        val Warning = Color(0xFFFF9800)
        val Success = Primary
    }
    
    // 📏 スペーシングシステム
    object Spacing {
        val XXS = 2.dp   // 極小
        val XS = 4.dp    // 最小
        val SM = 8.dp    // 小
        val MD = 12.dp   // 中小
        val LG = 16.dp   // 中（従来の基準）
        val XL = 20.dp   // 大
        val XXL = 24.dp  // 最大
        val XXXL = 32.dp // 特大
        val Huge = 48.dp // 超特大
    }
    
    // 🔄 角丸システム
    object Shapes {
        val XSmall = RoundedCornerShape(4.dp)     // 極小要素
        val Small = RoundedCornerShape(8.dp)      // 小さいコンポーネント
        val Medium = RoundedCornerShape(12.dp)    // カード
        val Large = RoundedCornerShape(16.dp)     // ダイアログ
        val XLarge = RoundedCornerShape(20.dp)    // 大きなカード
        val XXLarge = RoundedCornerShape(24.dp)   // FAB
        val Round = RoundedCornerShape(50)        // 完全な丸
    }
    
    // ✨ エレベーション（影）システム
    object Elevations {
        val None = 0.dp
        val Small = 2.dp      // 小さな影
        val Medium = 4.dp     // 通常のカード
        val Large = 6.dp      // 重要なカード
        val XLarge = 8.dp     // ダイアログ
        val Float = 12.dp     // FAB
    }
    
    // 🎨 グラデーションパレット
    object Gradients {
        val PrimaryVertical = Brush.verticalGradient(
            colors = listOf(Colors.Primary, Colors.PrimaryVariant)
        )
        
        val SecondaryVertical = Brush.verticalGradient(
            colors = listOf(Colors.Secondary, Colors.SecondaryVariant)
        )
        
        val BackgroundSoft = Brush.verticalGradient(
            colors = listOf(Colors.Background, Colors.SurfaceVariant)
        )
        
        val NatureBlend = Brush.verticalGradient(
            colors = listOf(
                Colors.PrimaryLight.copy(alpha = 0.3f),
                Colors.SecondaryLight.copy(alpha = 0.3f)
            )
        )
        
        val SurfaceGlow = Brush.radialGradient(
            colors = listOf(
                Color.White,
                Colors.SurfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
    
    // 🎯 カテゴリカラー（緑・青ベース）
    object CategoryColors {
        data class CategoryColorScheme(
            val backgroundColor: Color,
            val textColor: Color
        )
        
        val categories = mapOf(
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
        
        fun getColorScheme(category: String): CategoryColorScheme {
            return categories[category] ?: categories["その他"]!!
        }
    }
    
    // 🎪 カードスタイルプリセット
    @Composable
    fun cuteCard(elevation: Dp = Elevations.Medium): CardElevation {
        return CardDefaults.cardElevation(defaultElevation = elevation)
    }
}