
package ee.movara.app.data.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface NetworkMonitor {
    val isOnline: Flow<Boolean>
}

class AndroidNetworkMonitor(
    context: Context
) : NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(
            ConnectivityManager::class.java
        )

    private fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork
            ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(network)
                ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    override val isOnline: Flow<Boolean> = callbackFlow {
        val callback =
            object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) {
                    trySend(isConnected())
                }

                override fun onLost(network: Network) {
                    trySend(isConnected())
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    capabilities: NetworkCapabilities
                ) {
                    trySend(isConnected())
                }
            }

        trySend(isConnected())

        connectivityManager.registerDefaultNetworkCallback(
            callback
        )

        awaitClose {
            connectivityManager.unregisterNetworkCallback(
                callback
            )
        }
    }.distinctUntilChanged()
}
