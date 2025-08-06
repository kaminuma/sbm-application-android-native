package com.sbm.application.presentation.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sbm.application.presentation.theme.CuteDesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyPolicyScreen(
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "プライバシーポリシー",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CuteDesignSystem.Colors.Primary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "戻る",
                            tint = CuteDesignSystem.Colors.Primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CuteDesignSystem.Colors.Background
                )
            )
        },
        containerColor = CuteDesignSystem.Colors.Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = CuteDesignSystem.Colors.Surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = CuteDesignSystem.Shapes.Large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        "最終更新日: 2025年8月5日",
                        style = MaterialTheme.typography.bodySmall,
                        color = CuteDesignSystem.Colors.OnSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    PrivacySection(
                        title = "1. はじめに",
                        content = "SBM Application（以下「本アプリ」）は、ユーザーの生活記録とスケジュール管理を支援するサービスです。本プライバシーポリシーは、本アプリがユーザーの個人情報をどのように収集、使用、保護するかについて説明します。"
                    )
                    
                    PrivacySection(
                        title = "2. 収集する情報",
                        content = """本アプリでは以下の情報を収集します：
                        
• アカウント情報（ユーザー名、パスワード）
• アクティビティ記録（タイトル、詳細、時間、カテゴリ）
• 気分記録（気分レベル、メモ、日時）
• アプリの使用状況データ（クラッシュレポート、パフォーマンスデータ）"""
                    )
                    
                    PrivacySection(
                        title = "3. 情報の使用目的",
                        content = """収集した情報は以下の目的で使用します：
                        
• サービスの提供・運営
• ユーザーサポートの提供  
• アプリの改善・最適化
• セキュリティの確保"""
                    )
                    
                    PrivacySection(
                        title = "4. 情報の保護",
                        content = """本アプリは以下のセキュリティ対策を実施しています：
                        
• データの暗号化保存
• HTTPS通信による安全なデータ転送
• 適切なアクセス制御
• 定期的なセキュリティ監査"""
                    )
                    
                    PrivacySection(
                        title = "5. 第三者への提供",
                        content = "本アプリは、法的要請がある場合を除き、ユーザーの同意なしに個人情報を第三者に提供することはありません。"
                    )
                    
                    PrivacySection(
                        title = "6. データの削除",
                        content = "ユーザーはアカウント設定からいつでも自分の個人データの削除を要求できます。削除要求から30日以内にデータを完全に削除いたします。"
                    )
                    
                    PrivacySection(
                        title = "7. お問い合わせ",
                        content = "本プライバシーポリシーに関するご質問やご要望は、アプリ内のお問い合わせ機能またはサポートメールまでご連絡ください。"
                    )
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CuteDesignSystem.Colors.PrimaryLight.copy(alpha = 0.1f)
                        ),
                        shape = CuteDesignSystem.Shapes.Medium
                    ) {
                        Text(
                            "本プライバシーポリシーは予告なく変更される場合があります。重要な変更がある場合は、アプリ内で通知いたします。",
                            style = MaterialTheme.typography.bodySmall,
                            color = CuteDesignSystem.Colors.Primary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    content: String
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = CuteDesignSystem.Colors.Primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = CuteDesignSystem.Colors.OnSurface,
            lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.4
        )
        
        Spacer(modifier = Modifier.height(12.dp))
    }
}