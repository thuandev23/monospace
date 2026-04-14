package com.monospace.app.core.common

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.monospace.app.core.sync.SyncScheduler
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context,
    private val syncScheduler: SyncScheduler
) {
    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    /**
     * Flow phát `true` khi có mạng, `false` khi mất mạng.
     * Khi chuyển từ offline → online sẽ tự trigger sync.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        // Gửi trạng thái hiện tại ngay lập tức
        trySend(isCurrentlyOnline())

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(true)
            }

            override fun onLost(network: Network) {
                trySend(false)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                capabilities: NetworkCapabilities
            ) {
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                trySend(hasInternet)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }
        .distinctUntilChanged()
        .onEach { online ->
            // Khi có mạng trở lại → trigger sync ngay
            if (online) syncScheduler.scheduleSync()
        }

    fun isCurrentlyOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        // NET_CAPABILITY_VALIDATED có thể chưa set ngay trên một số thiết bị/emulator
        // nên chỉ cần có INTERNET là coi là online
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
