package com.sbm.application.presentation.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sbm.application.data.network.NetworkError

/**
 * ネットワークエラーを表示するコンポーネント
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkErrorCard(
    error: NetworkError,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, color, title) = when (error) {
        is NetworkError.NoConnection -> Triple(
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.error,
            "ネットワーク接続エラー"
        )
        is NetworkError.Timeout -> Triple(
            Icons.Filled.Refresh,
            MaterialTheme.colorScheme.tertiary,
            "タイムアウトエラー"
        )
        is NetworkError.ServerError -> Triple(
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.error,
            "サーバーエラー"
        )
        is NetworkError.AuthError -> Triple(
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.error,
            "認証エラー"
        )
        is NetworkError.ClientError -> Triple(
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.error,
            "リクエストエラー"
        )
        is NetworkError.Unknown -> Triple(
            Icons.Filled.Warning,
            MaterialTheme.colorScheme.error,
            "エラー"
        )
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error.errorMessage,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // 認証エラーでない場合のみリトライボタンを表示
            if (error !is NetworkError.AuthError) {
                FilledTonalButton(
                    onClick = onRetry,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = color.copy(alpha = 0.2f),
                        contentColor = color
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("再試行")
                }
            }
        }
    }
}

/**
 * ネットワーク状態を表示するインジケーター
 */
@Composable
fun NetworkStatusIndicator(
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    if (!isConnected) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "オフライン",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
