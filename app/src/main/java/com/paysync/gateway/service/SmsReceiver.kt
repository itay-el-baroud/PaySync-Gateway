package com.paysync.gateway.service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.paysync.gateway.data.local.AppDatabase
import com.paysync.gateway.data.local.AppDataStore
import com.paysync.gateway.data.local.TransactionEntity
import com.paysync.gateway.util.NetworkMonitor
import com.paysync.gateway.util.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody ?: continue
                val sender = sms.displayOriginatingAddress ?: "unknown"
                handleMessage(context, body, sender)
            }
        } catch (e: Exception) { Log.e("SmsReceiver", "Error", e) }
    }
    private fun handleMessage(context: Context, body: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val dataStore = AppDataStore(context)
                val parsed = SmsParser.parse(body)
                val entity = TransactionEntity(rawMessage = body, sender = sender, amount = parsed.amount, phone = parsed.phone, timestamp = System.currentTimeMillis(), status = "pending")
                val id = db.transactionDao().insert(entity)
                val webhookUrl = dataStore.webhookUrlFlow.first()
                if (webhookUrl.isBlank()) { db.transactionDao().updateStatus(id, "failed", "No webhook URL"); return@launch }
                if (!NetworkMonitor.checkNow(context)) { return@launch }
                sendToWebhook(context, webhookUrl, entity, id)
            } catch (e: Exception) { Log.e("SmsReceiver", "handle error", e) }
        }
    }
    private suspend fun sendToWebhook(context: Context, url: String, entity: TransactionEntity, id: Long) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) return
            val client = OkHttpClient.Builder().callTimeout(15, TimeUnit.SECONDS).build()
            val json = JSONObject().apply {
                put("sender", entity.sender); put("message", entity.rawMessage); put("amount", entity.amount); put("phone", entity.phone); put("timestamp", entity.timestamp)
            }
            val body = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder().url(url).post(body).build()
            val response = client.newCall(request).execute()
            val respText = response.body?.string() ?: ""
            val status = if (response.isSuccessful) "sent" else "failed"
            AppDatabase.getInstance(context).transactionDao().updateStatus(id, status, respText.take(500))
        } catch (e: Exception) {
            try { AppDatabase.getInstance(context).transactionDao().updateStatus(id, "failed", e.message?.take(500)) } catch (_: Exception) {}
        }
    }
}
