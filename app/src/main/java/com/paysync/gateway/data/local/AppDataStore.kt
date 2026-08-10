package com.paysync.gateway.data.local
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "paysync_prefs")
class AppDataStore(private val context: Context) {
    companion object {
        val WEBHOOK_URL = stringPreferencesKey("webhook_url")
        val SERVICE_RUNNING = booleanPreferencesKey("service_running")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }
    val webhookUrlFlow: Flow<String> = context.dataStore.data.map { it[WEBHOOK_URL] ?: "" }
    val isServiceRunningFlow: Flow<Boolean> = context.dataStore.data.map { it[SERVICE_RUNNING] ?: false }
    val onboardingSeenFlow: Flow<Boolean> = context.dataStore.data.map { it[ONBOARDING_SEEN] ?: false }
    suspend fun saveWebhookUrl(url: String) { context.dataStore.edit { it[WEBHOOK_URL] = url } }
    suspend fun setServiceRunning(running: Boolean) { context.dataStore.edit { it[SERVICE_RUNNING] = running } }
    suspend fun setOnboardingSeen(seen: Boolean) { context.dataStore.edit { it[ONBOARDING_SEEN] = seen } }
}
