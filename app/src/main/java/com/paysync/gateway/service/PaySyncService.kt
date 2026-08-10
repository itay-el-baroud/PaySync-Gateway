package com.paysync.gateway.service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.paysync.gateway.MainActivity
import com.paysync.gateway.data.local.AppDataStore
import com.paysync.gateway.data.local.AppDatabase
import com.paysync.gateway.util.NetworkMonitor
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class PaySyncService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var retryJob: Job? = null
    companion object { const val CHANNEL_ID = "paysync_foreground"; const val NOTIF_ID = 1001 }
    override fun onCreate() { super.onCreate(); createChannel() }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) startForeground(NOTIF_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC) else startForeground(NOTIF_ID, notification)
        scope.launch { try { AppDataStore(this@PaySyncService).setServiceRunning(true) } catch (_: Exception) {} }
        startRetryLoop()
        return START_STICKY
    }
    private fun startRetryLoop() {
        retryJob?.cancel()
        retryJob = scope.launch {
            while (isActive) {
                try { if (NetworkMonitor.checkNow(this@PaySyncService)) retryPending() } catch (_: Exception) {}
                delay(30000)
            }
        }
    }
    private suspend fun retryPending() {
        try {
            val db = AppDatabase.getInstance(this)
            val ds = AppDataStore(this)
            val url = ds.webhookUrlFlow.first()
            if (url.isBlank()) return
            val pending = db.transactionDao().getPending()
            if (pending.isEmpty()) return
            val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
            for (item in pending) {
                try {
                    val json = JSONObject().apply {
                        put("sender", item.sender); put("message", item.rawMessage); put("amount", item.amount); put("phone", item.phone); put("timestamp", item.timestamp); put("retry", true)
                    }
                    val body = json.toString().toRequestBody("application/json".toMediaType())
                    val req = Request.Builder().url(url).post(body).build()
                    val resp = client.newCall(req).execute()
                    val txt = resp.body?.string() ?: ""
                    val status = if (resp.isSuccessful) "sent" else "failed"
                    db.transactionDao().updateStatus(item.id, status, txt.take(500))
                    delay(1000)
                } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }
    override fun onDestroy() {
        super.onDestroy()
        retryJob?.cancel()
        scope.cancel()
        CoroutineScope(Dispatchers.IO).launch { try { AppDataStore(this@PaySyncService).setServiceRunning(false) } catch (_: Exception) {} }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "PaySync Service", NotificationManager.IMPORTANCE_LOW).apply { description = "PaySync Gateway monitoring"; setShowBadge(false) }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }
    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java)
        val pending = PendingIntent.getActivity(this, 0, launchIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL_ID).setContentTitle("PaySync Gateway").setContentText("خدمة PaySync نشطة وتراقب التحويلات").setSmallIcon(android.R.drawable.stat_notify_sync).setOngoing(true).setContentIntent(pending).build()
    }
}
