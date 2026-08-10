package com.paysync.gateway.util
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
object NetworkMonitor {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline
    fun checkNow(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = cm.activeNetwork ?: return false.also { _isOnline.value = false }
            val caps = cm.getNetworkCapabilities(network) ?: return false.also { _isOnline.value = false }
            val online = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) && (caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) || caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) || caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR))
            _isOnline.value = online
            online
        } catch (e: Exception) { false }
    }
}
