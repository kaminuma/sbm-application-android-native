package com.sbm.application.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ネットワーク接続状態を監視するクラス
 */
@Singleton
class NetworkMonitor @Inject constructor(
    private val context: Context
) {
    
    data class NetworkState(
        val isConnected: Boolean,
        val isWifi: Boolean,
        val isMobile: Boolean,
        val isMetered: Boolean = false
    )
    
    /**
     * ネットワーク状態の変更を監視するFlow
     */
    fun networkState(): Flow<NetworkState> = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                trySend(getCurrentNetworkState(connectivityManager))
            }
            
            override fun onLost(network: Network) {
                super.onLost(network)
                trySend(getCurrentNetworkState(connectivityManager))
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                trySend(getCurrentNetworkState(connectivityManager))
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        // 初期状態を送信
        trySend(getCurrentNetworkState(connectivityManager))
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    private fun getCurrentNetworkState(connectivityManager: ConnectivityManager): NetworkState {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        
        return if (capabilities != null) {
            NetworkState(
                isConnected = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET),
                isWifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI),
                isMobile = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR),
                isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
            )
        } else {
            NetworkState(
                isConnected = false,
                isWifi = false,
                isMobile = false,
                isMetered = false
            )
        }
    }
}
