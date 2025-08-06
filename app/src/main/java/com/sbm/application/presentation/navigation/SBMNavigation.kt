package com.sbm.application.presentation.navigation

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.sbm.application.data.remote.AuthInterceptor
import com.sbm.application.presentation.screen.auth.LoginScreen
import com.sbm.application.presentation.screen.auth.RegisterScreen
import com.sbm.application.presentation.screen.main.MainScreen
import com.sbm.application.presentation.viewmodel.AuthViewModel
import androidx.compose.ui.platform.LocalContext
import com.sbm.application.domain.usecase.AuthSessionManager
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import android.util.Log
import com.sbm.application.BuildConfig

@EntryPoint
@InstallIn(SingletonComponent::class)
interface AuthSessionManagerEntryPoint {
    fun authSessionManager(): AuthSessionManager
}

@Composable
fun SBMNavigation(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val authSessionManager = remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context,
            AuthSessionManagerEntryPoint::class.java
        )
        entryPoint.authSessionManager()
    }
    
    val authState by authViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    // デバッグ用ログ
    LaunchedEffect(authState.isAuthenticated) {
        if (BuildConfig.DEBUG) {
            Log.d("SBMNavigation", "Auth state changed: isAuthenticated = ${authState.isAuthenticated}")
            Log.d("SBMNavigation", "Current route: ${navController.currentDestination?.route}")
            Log.d("SBMNavigation", "User: ${authState.user}")
            Log.d("SBMNavigation", "Error: ${authState.error}")
        }
    }
    
    // 認証状態の変化に応じてナビゲーション処理（遅延を追加して確実に処理）
    LaunchedEffect(authState.isAuthenticated) {
        // 少し遅延を入れて、状態が確実に更新されるのを待つ
        kotlinx.coroutines.delay(100)
        
        val currentRoute = navController.currentDestination?.route
        if (BuildConfig.DEBUG) {
            Log.d("SBMNavigation", "Handling auth state change: authenticated=${authState.isAuthenticated}, currentRoute=$currentRoute")
        }
        
        when {
            authState.isAuthenticated && (currentRoute == Screen.Login.route || currentRoute == Screen.Register.route) -> {
                // 認証済みなのにログイン・登録画面にいる場合、メイン画面に遷移
                if (BuildConfig.DEBUG) {
                    Log.d("SBMNavigation", "Navigating to Main screen from $currentRoute")
                }
                try {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d("SBMNavigation", "Navigation to Main completed")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e("SBMNavigation", "Navigation error: ${e.message}")
                    }
                }
            }
            !authState.isAuthenticated && currentRoute == Screen.Main.route -> {
                // 未認証なのにメイン画面にいる場合、ログイン画面に遷移
                if (BuildConfig.DEBUG) {
                    Log.d("SBMNavigation", "Navigating to Login screen from $currentRoute")
                }
                try {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Main.route) { inclusive = true }
                        launchSingleTop = true
                    }
                    if (BuildConfig.DEBUG) {
                        Log.d("SBMNavigation", "Navigation to Login completed")
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e("SBMNavigation", "Navigation error: ${e.message}")
                    }
                }
            }
        }
    }
    
    // AuthInterceptorのコールバックを設定
    LaunchedEffect(Unit) {
        AuthInterceptor.onAuthError = { responseCode, message ->
            coroutineScope.launch {
                when (responseCode) {
                    401, 403 -> {
                        // セッション管理を通じてログアウト処理
                        authSessionManager.handleAuthenticationError(responseCode, message)
                        
                        // メイン画面にいる場合のみログイン画面に遷移
                        if (navController.currentDestination?.route == Screen.Main.route) {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // セッション期限切れの監視
    LaunchedEffect(Unit) {
        authSessionManager.sessionExpiredFlow.collect {
            // メイン画面にいる場合のみログイン画面に遷移
            if (navController.currentDestination?.route == Screen.Main.route) {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Main.route) { inclusive = true }
                }
            }
        }
    }
    
    // 認証エラーメッセージの表示
    LaunchedEffect(Unit) {
        authSessionManager.authErrorFlow.collect { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                withDismissAction = true
            )
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) Screen.Main.route else Screen.Login.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    // ナビゲーションは認証状態変化のLaunchedEffectで自動処理
                    if (BuildConfig.DEBUG) {
                        Log.d("SBMNavigation", "LoginScreen - Login success callback triggered")
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    // ナビゲーションは認証状態変化のLaunchedEffectで自動処理
                    if (BuildConfig.DEBUG) {
                        Log.d("SBMNavigation", "RegisterScreen - Register success callback triggered")
                    }
                },
                authViewModel = authViewModel
            )
        }
        
        composable(Screen.Main.route) {
            MainScreen(
                onLogout = {
                    authViewModel.logout()
                    // ログアウト処理の完了を待ってナビゲーション
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(50) // ログアウト状態の更新を確実にする
                        try {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Main.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("SBMNavigation", "Logout navigation error", e)
                        }
                    }
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object Main : Screen("main")
}