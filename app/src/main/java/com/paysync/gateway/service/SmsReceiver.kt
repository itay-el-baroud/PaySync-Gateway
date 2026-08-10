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
import com.paysync.gateway.util.SenderValidator
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
    
    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        
        try {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody ?: continue
                // Critical: Use getOriginatingAddress() for security - official API
                val sender = sms.displayOriginatingAddress ?: sms.originatingAddress ?: "unknown"
                handleMessage(context, body, sender)
            }
        } catch (e: Exception) {
            Log.e(TAG, "onReceive error", e)
        }
    }

    private fun handleMessage(context: Context, body: String, sender: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ========== CRITICAL SECURITY FILTER ==========
                // 1. Validate Sender ID BEFORE any processing
                // Using getOriginatingAddress() value already obtained
                if (!SenderValidator.isAuthorized(sender)) {
                    Log.w(TAG, "REJECTED unauthorized sender: $sender | Body: ${body.take(50)}")
                    // Immediate ignore - as if message never existed
                    // No DB insert, no network call
                    return@launch
                }

                Log.i(TAG, "ACCEPTED authorized sender: $sender")

                // 2. Parse only after authorization
                val parsed = SmsParser.parse(body)

                // 3. Create entity with verified sender
                val db = AppDatabase.getInstance(context)
                val dataStore = AppDataStore(context)
                
                val entity = TransactionEntity(
                    rawMessage = body,
                    sender = SenderValidator.getCanonicalName(sender), // Store verified sender_name
                    amount = parsed.amount,
                    phone = parsed.phone,
                    timestamp = System.currentTimeMillis(),
                    status = "pending"
                )
                val id = db.transactionDao().insert(entity)

                // 4. Check webhook URL
                val webhookUrl = dataStore.webhookUrlFlow.first()
                if (webhookUrl.isBlank()) {
                    db.transactionDao().updateStatus(id, "failed", "No webhook URL configured")
                    return@launch
                }

                // 5. Check network
                if (!NetworkMonitor.checkNow(context)) {
                    Log.i(TAG, "Network offline, will retry later for id=$id")
                    return@launch
                }

                // 6. Send with NEW secure payload
                sendToWebhook(context, webhookUrl, entity, id)

            } catch (e: Exception) {
                Log.e(TAG, "handleMessage error", e)
            }
        }
    }

    private suspend fun sendToWebhook(context: Context, url: String, entity: TransactionEntity, id: Long) {
        try {
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                Log.e(TAG, "Invalid URL: $url")
                return
            }

            val client = OkHttpClient.Builder()
                .callTimeout(15, TimeUnit.SECONDS)
                .build()

            // ========== NEW SECURE PAYLOAD (as per spec) ==========
            val json = JSONObject().apply {
                put("sender_name", entity.sender) // Verified sender ID
                put("phone_number", entity.phone ?: "") // Extracted from message content
                put("amount", entity.amount ?: "")
                put("raw_message", entity.rawMessage)
                put("timestamp", entity.timestamp)
            }

            Log.i(TAG, "Sending secure payload: ${json.toString().take(200)}")

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Content-Type", "application/json")
                .addHeader("X-PaySync-Sender", entity.sender)
                .build()

            val response = client.newCall(request).execute()
            val respText = response.body?.string() ?: ""
            val status = if (response.isSuccessful) "sent" else "failed"
            
            Log.i(TAG, "Webhook response: ${response.code} - $status")
            
            AppDatabase.getInstance(context).transactionDao().updateStatus(id, status, respText.take(500))
            
        } catch (e: Exception) {
            Log.e(TAG, "sendToWebhook error", e)
            try {
                AppDatabase.getInstance(context).transactionDao().updateStatus(id, "failed", e.message?.take(500))
            } catch (_: Exception) {}
        }
    }
}
