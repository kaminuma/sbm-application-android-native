package com.sbm.application.presentation.screen.auth

// import android.util.Log // Removed for production
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sbm.application.BuildConfig
import com.sbm.application.presentation.viewmodel.AuthViewModel
import com.sbm.application.presentation.theme.CuteDesignSystem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onRegisterSuccess: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    LaunchedEffect(uiState.registrationSuccess) {
        if (uiState.registrationSuccess) {
                        // TODO: ここでSnackbarやToastで「登録が完了しました」メッセージを表示
            authViewModel.clearRegistrationSuccess()
            onNavigateToLogin()
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
                    .padding(bottom = CuteDesignSystem.Spacing.XL),
                shape = CuteDesignSystem.Shapes.Large,
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.9f)
                ),
                elevation = CuteDesignSystem.cuteCard(CuteDesignSystem.Elevations.Medium)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(CuteDesignSystem.Spacing.XL),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // アプリアイコン風のデザイン
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        CuteDesignSystem.Colors.Secondary,
                                        CuteDesignSystem.Colors.Primary
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🌱",
                            fontSize = 28.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.MD))
                    
                    Text(
                        text = "新規アカウント作成",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = CuteDesignSystem.Colors.Secondary,
                        fontSize = 24.sp
                    )
                    
                    Text(
                        text = "SBM Applicationへようこそ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CuteDesignSystem.Colors.OnSurfaceVariant
                    )
                }
            }
            
            // 新規登録フォームカード
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
                        text = "アカウント情報入力",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = CuteDesignSystem.Colors.Secondary,
                        modifier = Modifier.padding(bottom = CuteDesignSystem.Spacing.LG)
                    )
                    
                    // ユーザー名入力欄
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
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
                    
                    // メールアドレス入力欄
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("メールアドレス") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "メールアドレス",
                                tint = CuteDesignSystem.Colors.Accent
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true, // 改行制限
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CuteDesignSystem.Colors.Accent,
                            focusedLabelColor = CuteDesignSystem.Colors.Accent,
                            cursorColor = CuteDesignSystem.Colors.Accent
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
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
                    
                    // パスワード確認入力欄
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("パスワード確認") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = "パスワード確認",
                                tint = CuteDesignSystem.Colors.Secondary
                            )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        singleLine = true,
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CuteDesignSystem.Colors.Secondary,
                            focusedLabelColor = CuteDesignSystem.Colors.Secondary,
                            cursorColor = CuteDesignSystem.Colors.Secondary
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !uiState.isLoading,
                        isError = password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword
                    )
                    
                    // パスワード確認エラー表示
                    if (password.isNotEmpty() && confirmPassword.isNotEmpty() && password != confirmPassword) {
                        Text(
                            text = "パスワードが一致しません",
                            color = CuteDesignSystem.Colors.Error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.XXL))
                    
                    // 新規登録ボタン
                    Button(
                        onClick = {
                                                        authViewModel.register(name, email, password, confirmPassword)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = CuteDesignSystem.Shapes.Medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = CuteDesignSystem.Colors.Secondary,
                            contentColor = CuteDesignSystem.Colors.OnSecondary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = CuteDesignSystem.Elevations.Small,
                            pressedElevation = CuteDesignSystem.Elevations.Medium
                        ),
                        enabled = !uiState.isLoading && 
                                 name.isNotBlank() && 
                                 email.isNotBlank() && 
                                 password.isNotBlank() &&
                                 confirmPassword.isNotBlank() &&
                                 password == confirmPassword
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CuteDesignSystem.Colors.OnSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "アカウントを作成",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(CuteDesignSystem.Spacing.LG))
                    
                    // ログインリンク
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = CuteDesignSystem.Colors.Primary
                        )
                    ) {
                        Text(
                            "すでにアカウントをお持ちの方はこちら",
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