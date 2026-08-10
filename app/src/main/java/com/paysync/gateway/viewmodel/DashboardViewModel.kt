package com.paysync.gateway.viewmodel
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.paysync.gateway.data.local.AppDataStore
import com.paysync.gateway.data.local.AppDatabase
import com.paysync.gateway.data.local.TransactionEntity
import com.paysync.gateway.service.PaySyncService
import com.paysync.gateway.util.NetworkMonitor
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
class DashboardViewModel(context: Context) : ViewModel() {
    private val appContext = context.applicationContext
    private val dataStore = AppDataStore(appContext)
    private val db = AppDatabase.getInstance(appContext)
    private val _webhookUrl = MutableStateFlow("")
    val webhookUrl: StateFlow<String> = _webhookUrl
    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning: StateFlow<Boolean> = _isServiceRunning
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message
    val logs: Flow<List<TransactionEntity>> = db.transactionDao().getAllFlow()
    init {
        viewModelScope.launch { dataStore.webhookUrlFlow.collect { _webhookUrl.value = it } }
        viewModelScope.launch { dataStore.isServiceRunningFlow.collect { _isServiceRunning.value = it } }
        viewModelScope.launch { _isOnline.value = NetworkMonitor.checkNow(appContext) }
    }
    fun onUrlChange(newUrl: String) { _webhookUrl.value = newUrl }
    fun saveUrl() {
        viewModelScope.launch {
            try {
                _isSaving.value = true
                val url = _webhookUrl.value.trim()
                if (url.isEmpty()) { _message.value = "ادخل رابط Webhook أولا"; return@launch }
                if (!url.startsWith("http://") && !url.startsWith("https://")) { _message.value = "الرابط يجب أن يبدأ بـ https://"; return@launch }
                dataStore.saveWebhookUrl(url)
                _message.value = "تم حفظ الرابط بنجاح"
            } catch (e: Exception) { _message.value = "خطأ أثناء الحفظ: ${e.message}" } finally { _isSaving.value = false }
        }
    }
    fun startService(context: Context) {
        viewModelScope.launch {
            try {
                if (_webhookUrl.value.isBlank()) { _message.value = "احفظ رابط Webhook أولا"; return@launch }
                val intent = Intent(context, PaySyncService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent) else context.startService(intent)
                dataStore.setServiceRunning(true)
                _isServiceRunning.value = true
                _message.value = "تم تشغيل الخدمة"
            } catch (e: Exception) { _message.value = "فشل تشغيل الخدمة: ${e.message}" }
        }
    }
    fun stopService(context: Context) {
        viewModelScope.launch {
            try {
                context.stopService(Intent(context, PaySyncService::class.java))
                dataStore.setServiceRunning(false)
                _isServiceRunning.value = false
                _message.value = "تم إيقاف الخدمة"
            } catch (e: Exception) { _message.value = "فشل الإيقاف: ${e.message}" }
        }
    }
    fun clearLogs() { viewModelScope.launch { try { db.transactionDao().clearAll(); _message.value = "تم مسح السجل" } catch (e: Exception) { _message.value = "فشل مسح السجل" } } }
    fun clearMessage() { _message.value = null }
    fun refreshNetwork() { _isOnline.value = NetworkMonitor.checkNow(appContext) }
}
