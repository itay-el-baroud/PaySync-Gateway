package com.paysync.gateway.service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.paysync.gateway.data.local.AppDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED || action == "android.intent.action.PACKAGE_REPLACED") {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val ds = AppDataStore(context)
                    val wasRunning = ds.isServiceRunningFlow.first()
                    if (wasRunning) {
                        val serviceIntent = Intent(context, PaySyncService::class.java)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(serviceIntent) else context.startService(serviceIntent)
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
