package com.bpdlh.pencatatankeuangan.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class Connectivity(context: Context) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val _online = MutableStateFlow(manager.isOnline())
    val online: StateFlow<Boolean> = _online

    init {
        manager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                _online.value = capabilities.isOnline()
            }

            override fun onLost(network: Network) {
                _online.value = false
            }
        })
    }
}

private fun ConnectivityManager.isOnline(): Boolean = getNetworkCapabilities(activeNetwork)?.isOnline() == true

private fun NetworkCapabilities.isOnline(): Boolean =
    hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
