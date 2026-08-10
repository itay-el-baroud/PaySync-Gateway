package com.paysync.gateway
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.paysync.gateway.data.local.AppDataStore
import com.paysync.gateway.ui.screens.AuthScreen
import com.paysync.gateway.ui.screens.DashboardScreen
import com.paysync.gateway.ui.screens.OnboardingScreen
import com.paysync.gateway.ui.screens.SplashScreen
import com.paysync.gateway.ui.theme.PaySyncTheme
import com.paysync.gateway.viewmodel.DashboardViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
class MainActivity : FragmentActivity() {
    private val viewModel: DashboardViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(this@MainActivity) as T
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkPermissions()
        setContent {
            PaySyncTheme {
                var currentScreen by remember { mutableStateOf("splash") }
                var onboardingSeen by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    try {
                        val ds = AppDataStore(this@MainActivity)
                        onboardingSeen = ds.onboardingSeenFlow.first()
                    } catch (_: Exception) {}
                }
                when (currentScreen) {
                    "splash" -> SplashScreen { currentScreen = "auth" }
                    "auth" -> AuthScreen { currentScreen = if (onboardingSeen) "dashboard" else "onboarding" }
                    "onboarding" -> OnboardingScreen {
                        runBlocking { try { AppDataStore(this@MainActivity).setOnboardingSeen(true) } catch (_: Exception) {} }
                        currentScreen = "dashboard"
                    }
                    "dashboard" -> DashboardScreen(viewModel)
                }
            }
        }
    }
    private fun checkPermissions() {
        val perms = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.RECEIVE_SMS)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.READ_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (perms.isNotEmpty()) ActivityCompat.requestPermissions(this, perms.toTypedArray(), 101)
    }
}
