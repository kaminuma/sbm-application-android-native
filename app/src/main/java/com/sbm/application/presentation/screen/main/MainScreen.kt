package com.sbm.application.presentation.screen.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sbm.application.presentation.theme.CuteDesignSystem
import com.sbm.application.presentation.viewmodel.ActivityViewModel
import com.sbm.application.presentation.viewmodel.MoodViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    // ViewModelをMainScreenレベルで一元管理
    val activityViewModel: ActivityViewModel = hiltViewModel()
    val moodViewModel: MoodViewModel = hiltViewModel()
    
    Box {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        // 空のタイトルスペース
                    },
                    actions = {
                        // ログアウトボタンを右上に配置（アイコンのみ）
                        IconButton(
                            onClick = onLogout,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "ログアウト",
                                tint = CuteDesignSystem.Colors.Secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = CuteDesignSystem.Colors.Background,
                        titleContentColor = CuteDesignSystem.Colors.Primary
                    )
                )
            },
        bottomBar = {
            NavigationBar(
                containerColor = CuteDesignSystem.Colors.Surface,
                contentColor = CuteDesignSystem.Colors.Primary
            ) {
                MainTab.values().forEach { tab ->
                    NavigationBarItem(
                        icon = { 
                            Icon(
                                tab.icon, 
                                contentDescription = tab.title,
                                tint = if (currentRoute == tab.route) 
                                    CuteDesignSystem.Colors.Primary 
                                else 
                                    CuteDesignSystem.Colors.OnSurfaceVariant
                            ) 
                        },
                        label = { 
                            Text(
                                tab.title,
                                fontWeight = if (currentRoute == tab.route) FontWeight.Bold else FontWeight.Normal,
                                color = if (currentRoute == tab.route) 
                                    CuteDesignSystem.Colors.Primary 
                                else 
                                    CuteDesignSystem.Colors.OnSurfaceVariant
                            ) 
                        },
                        selected = currentRoute == tab.route,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CuteDesignSystem.Colors.Primary,
                            selectedTextColor = CuteDesignSystem.Colors.Primary,
                            indicatorColor = CuteDesignSystem.Colors.PrimaryLight.copy(alpha = 0.3f),
                            unselectedIconColor = CuteDesignSystem.Colors.OnSurfaceVariant,
                            unselectedTextColor = CuteDesignSystem.Colors.OnSurfaceVariant
                        ),
                        onClick = {
                            if (currentRoute != tab.route) {
                                navController.navigate(tab.route) {
                                    popUpTo(MainTab.Calendar.route) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    )
                }
            }
        },
        containerColor = CuteDesignSystem.Colors.Background
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainTab.Calendar.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(MainTab.Activities.route) {
                ActivityScreen(activityViewModel = activityViewModel)
            }
            composable(MainTab.Calendar.route) {
                CalendarScreen(activityViewModel = activityViewModel, moodViewModel = moodViewModel)
            }
            composable(MainTab.Mood.route) {
                MoodScreen(moodViewModel = moodViewModel)
            }
            composable(MainTab.Analysis.route) {
                AnalysisScreen()
            }
        }
    }
    
    // タイトルをTopAppBarの上にオーバーレイとして配置（完全に中央揃え）
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp), // TopAppBarの高さに合わせて調整
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = when (currentRoute) {
                MainTab.Activities.route -> "🎯 アクティビティ"
                MainTab.Calendar.route -> "📅 カレンダー"
                MainTab.Mood.route -> "😊 ムード"
                MainTab.Analysis.route -> "📊 分析"
                else -> "✨ SBM Application"
            },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = CuteDesignSystem.Colors.Primary,
            textAlign = TextAlign.Center
        )
    }
    }
}

enum class MainTab(
    val route: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Activities("activities", "アクティビティ", Icons.Default.List),
    Calendar("calendar", "カレンダー", Icons.Default.DateRange),
    Mood("mood", "ムード", Icons.Default.Favorite),
    Analysis("analysis", "分析", Icons.Default.Info)
}