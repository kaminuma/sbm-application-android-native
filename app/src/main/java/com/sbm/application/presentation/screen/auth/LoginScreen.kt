package com.sbm.application.presentation.screen.auth

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.config.ApiConfig
import com.sbm.application.R
import com.sbm.application.presentation.viewmodel.AuthViewModel
import com.sbm.application.presentation.theme.CuteDesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onLoginSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            // 少し遅延を入れて、ナビゲーション処理を確実にする
            kotlinx.coroutines.delay(200)
            onLoginSuccess()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        CuteDesignSystem.Colors.Background,
                        CuteDesignSystem.Colors.SurfaceVariant,
                        CuteDesignSystem.Colors.SurfaceTint
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(CuteDesignSystem.Spacing.XXL),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // アプリタイトル部分
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = CuteDesignSystem.Spacing.XXXL),
                shape = CuteDesignSystem.Shapes.Large,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CuteDesignSystem.cuteCard(CuteDesignSystem.Elevations.Medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CuteDesignSystem.Spacing.XXL),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // アプリアイコン風のデザイン
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CuteDesignSystem.Colors.Primary,
                                        CuteDesignSystem.Colors.Secondary
                                    )
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📱",
                            fontSize = 32.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
                    
                    Text(
                        text = "SBM Application",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = CuteDesignSystem.Colors.Primary,
                        fontSize = 28.sp
                    )
                    
                    Text(
                        text = "生活記録・スケジュール管理アプリ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CuteDesignSystem.Colors.OnSurfaceVariant,
                        modifier = Modifier.padding(top = CuteDesignSystem.Spacing.XS)
                    )
                }
            }
            
            // ログインフォームカード
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = CuteDesignSystem.Shapes.XLarge,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                ),
                elevation = CuteDesignSystem.cuteCard(CuteDesignSystem.Elevations.Large)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CuteDesignSystem.Spacing.XXL),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ログイン",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CuteDesignSystem.Colors.Primary,
                        modifier = Modifier.padding(bottom = CuteDesignSystem.Spacing.XL)
                    )
                    
                    // ユーザー名入力欄
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("ユーザー名") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = "ユーザー名",
                                tint = CuteDesignSystem.Colors.Primary
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true, // 改行制限
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CuteDesignSystem.Colors.Primary,
                            focusedLabelColor = CuteDesignSystem.Colors.Primary,
                            cursorColor = CuteDesignSystem.Colors.Primary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
                    
                    // パスワード入力欄
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("パスワード") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "パスワード",
                                tint = CuteDesignSystem.Colors.Secondary
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true, // 改行制限
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CuteDesignSystem.Colors.Secondary,
                            focusedLabelColor = CuteDesignSystem.Colors.Secondary,
                            cursorColor = CuteDesignSystem.Colors.Secondary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading
                    )
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.XXL))
                    
                    // ログインボタン
                    Button(
                        onClick = {
                            authViewModel.login(username, password)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CuteDesignSystem.Colors.Primary,
                            contentColor = CuteDesignSystem.Colors.OnPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = CuteDesignSystem.Elevations.Small,
                            pressedElevation = CuteDesignSystem.Elevations.Medium
                        ),
                        enabled = !uiState.isLoading && username.isNotBlank() && password.isNotBlank()
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CuteDesignSystem.Colors.OnPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "ログイン",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.MD))
                    
                    // 区切り線
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Text(
                            text = "または",
                            modifier = Modifier.padding(horizontal = CuteDesignSystem.Spacing.MD),
                            color = CuteDesignSystem.Colors.OnSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Divider(
                            modifier = Modifier.weight(1f),
                            color = CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.3f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.MD))
                    
                    // Googleログインボタン
                    val context = LocalContext.current
                    OutlinedButton(
                        onClick = {
                            // Chrome Custom Tabsでブラウザを開く
                            val customTabsIntent = CustomTabsIntent.Builder()
                                .setShowTitle(true)
                                .build()
                            
                            // User-Agentを設定
                            customTabsIntent.intent.putExtra(
                                "com.android.browser.headers",
                                Bundle().apply {
                                    putString("User-Agent", "SBMApp/1.0 Android")
                                }
                            )
                            
                            // モバイルアプリ識別のためのパラメータを追加
                            val loginUrl = "${ApiConfig.BACKEND_URL}/oauth2/authorization/google?mobile=true"
                            customTabsIntent.launchUrl(context, Uri.parse(loginUrl))
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            CuteDesignSystem.Colors.OnSurfaceVariant.copy(alpha = 0.3f)
                        ),
                        enabled = !uiState.isLoading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_google),
                                contentDescription = "Google",
                                modifier = Modifier.size(20.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Googleでログイン",
                                style = MaterialTheme.typography.bodyMedium,
                                color = CuteDesignSystem.Colors.OnSurface
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
                    
                    // 新規登録リンク
                    TextButton(
                        onClick = onNavigateToRegister,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = CuteDesignSystem.Colors.Secondary
                        )
                    ) {
                        Text(
                            "アカウントをお持ちでない方はこちら",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // エラー表示
            uiState.error?.let { error ->
                Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = CuteDesignSystem.Shapes.Medium,
                    colors = CardDefaults.cardColors(
                        containerColor = CuteDesignSystem.Colors.Error.copy(alpha = 0.1f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        CuteDesignSystem.Colors.Error.copy(alpha = 0.3f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(CuteDesignSystem.Spacing.LG),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️",
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = CuteDesignSystem.Spacing.SM)
                        )
                        Text(
                            text = error,
                            color = CuteDesignSystem.Colors.Error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}