package com.paysync.gateway.ui.screens
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.paysync.gateway.viewmodel.DashboardViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) { delay(2000); onFinished() }
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.Sync, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color.Black)
            Spacer(Modifier.height(16.dp))
            Text("PaySync Gateway", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Spacer(Modifier.height(24.dp))
            CircularProgressIndicator(color = Color.Black, strokeWidth = 2.dp, modifier = Modifier.size(32.dp))
        }
    }
}
@Composable
fun AuthScreen(onSuccess: () -> Unit) {
    val context = LocalContext.current
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var isAuthenticating by remember { mutableStateOf(false) }
    fun triggerAuth() {
        try {
            val activity = context as? FragmentActivity ?: return
            val executor = ContextCompat.getMainExecutor(context)
            val promptInfo = BiometricPrompt.PromptInfo.Builder().setTitle("الحماية الأمنية").setSubtitle("استخدم البصمة أو وجهك للدخول").setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL).build()
            val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) { isAuthenticating = false; onSuccess() }
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) { isAuthenticating = false; errorMsg = errString.toString() }
                override fun onAuthenticationFailed() { isAuthenticating = false; errorMsg = "فشل التحقق، حاول مرة أخرى" }
            })
            isAuthenticating = true
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) { isAuthenticating = false; errorMsg = "البصمة غير متاحة: ${e.message}" }
    }
    LaunchedEffect(Unit) { triggerAuth() }
    Box(modifier = Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Outlined.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Black)
            Spacer(Modifier.height(16.dp))
            Text("الحماية الأمنية", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
            Spacer(Modifier.height(8.dp))
            Text("استخدم البصمة أو رمز الهاتف للدخول", color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            if (isAuthenticating) CircularProgressIndicator(color = Color.Black)
            errorMsg?.let { Spacer(Modifier.height(12.dp)); Text(it, color = Color.Red, fontSize = 13.sp) }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { triggerAuth() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White)) { Text("إعادة المحاولة") }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSuccess) { Text("استخدام رمز الحماية") }
        }
    }
}
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize().background(Color.White).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onFinish) { Text("تخطي", color = Color.Black) } }
        HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { page ->
            Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (page) {
                    0 -> { Icon(Icons.Outlined.Link, contentDescription = null, modifier = Modifier.size(80.dp)); Spacer(Modifier.height(16.dp)); Text("إعداد رابط Webhook", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)); Text("ادخل رابط السيرفر الخاص بك واحفظه لضمان وصول البيانات", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)) }
                    1 -> { Icon(Icons.Outlined.PlayCircle, contentDescription = null, modifier = Modifier.size(80.dp)); Spacer(Modifier.height(16.dp)); Text("بدء المراقبة", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)); Text("زر التشغيل الأخضر يبدأ الخدمة الخلفية التي تراقب الرسائل حتى عند إغلاق الشاشة", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)) }
                    2 -> { Icon(Icons.Outlined.StopCircle, contentDescription = null, modifier = Modifier.size(80.dp)); Spacer(Modifier.height(16.dp)); Text("الإيقاف والمتابعة", fontWeight = FontWeight.Bold, fontSize = 18.sp); Spacer(Modifier.height(8.dp)); Text("زر الإيقاف الأحمر يوقف المراقبة وسجل العمليات يعرض كل المعاملات لحظيا", color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp)) }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row { repeat(3) { index -> Box(modifier = Modifier.padding(4.dp).size(if (pagerState.currentPage == index) 12.dp else 8.dp).clip(CircleShape).background(if (pagerState.currentPage == index) Color.Black else Color.LightGray)) } }
            Button(onClick = { if (pagerState.currentPage < 2) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } else onFinish() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Black)) { Text(if (pagerState.currentPage == 2) "بدء الاستخدام" else "التالي", color = Color.White) }
        }
    }
}
@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val url by viewModel.webhookUrl.collectAsState()
    val isRunning by viewModel.isServiceRunning.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val msg by viewModel.message.collectAsState()
    val logs by viewModel.logs.collectAsState(initial = emptyList())
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(initialValue = 0.6f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(800), repeatMode = RepeatMode.Reverse), label = "pulse")
    LaunchedEffect(Unit) { viewModel.refreshNetwork() }
    Scaffold(containerColor = Color.White, snackbarHost = { msg?.let { Snackbar(containerColor = Color.Black, contentColor = Color.White, modifier = Modifier.padding(8.dp)) { Text(it) } } }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).background(Color.White)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("حالة الشبكة:", fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.width(6.dp))
                Icon(imageVector = if (isOnline) Icons.Outlined.Wifi else Icons.Outlined.WifiOff, contentDescription = null, tint = if (isOnline) Color(0xFF2E7D32) else Color.Red, modifier = Modifier.size(16.dp))
                Spacer(Modifier.weight(1f))
                if (!isOnline) { TextButton(onClick = { viewModel.refreshNetwork() }) { Text("إعادة فحص", fontSize = 12.sp, color = Color.Black) } }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = url, onValueChange = { viewModel.onUrlChange(it) }, label = { Text("Webhook URL") }, leadingIcon = { Icon(Icons.Outlined.Link, contentDescription = null) }, trailingIcon = { if (isSaving) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp) else IconButton(onClick = { viewModel.saveUrl() }) { Icon(Icons.Outlined.Save, contentDescription = "حفظ") } }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(if (isRunning) Color.Green.copy(alpha = pulse) else Color.Red)); Spacer(Modifier.width(8.dp)); Text(if (isRunning) "الخدمة نشطة" else "الخدمة متوقفة", fontWeight = FontWeight.Bold, color = Color.Black) }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = { viewModel.startService(context) }, enabled = !isRunning, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32), disabledContainerColor = Color.LightGray), shape = CircleShape, modifier = Modifier.size(90.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.PlayArrow, contentDescription = null, tint = Color.White); Text("تشغيل", color = Color.White, fontSize = 12.sp) } }
                        Button(onClick = { viewModel.stopService(context) }, enabled = isRunning, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828), disabledContainerColor = Color.LightGray), shape = CircleShape, modifier = Modifier.size(90.dp)) { Column(horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Stop, contentDescription = null, tint = Color.White); Text("إيقاف", color = Color.White, fontSize = 12.sp) } }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("سجل العمليات الحي", fontWeight = FontWeight.Bold, fontSize = 14.sp); Spacer(Modifier.width(8.dp)); Text("(${logs.size})", fontSize = 12.sp, color = Color.Gray) }
                IconButton(onClick = { viewModel.clearLogs() }) { Icon(Icons.Outlined.Delete, contentDescription = "مسح", tint = Color.Red) }
            }
            Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = Color.White), border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray), shape = RoundedCornerShape(12.dp)) {
                if (logs.isEmpty()) { Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("لا توجد عمليات بعد", color = Color.Gray, fontSize = 13.sp) } }
                else { LazyColumn(modifier = Modifier.fillMaxSize().padding(8.dp)) { items(logs) { log -> Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = if (log.status == "sent") Color(0xFFE8F5E9) else if (log.status == "pending") Color(0xFFFFF8E1) else Color(0xFFFFEBEE))) { Column(modifier = Modifier.padding(8.dp)) { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(log.sender, fontSize = 11.sp, color = Color.Gray); Text(log.status, fontSize = 10.sp, color = when(log.status){ "sent" -> Color(0xFF2E7D32); "pending" -> Color(0xFFF9A825); else -> Color.Red }, fontWeight = FontWeight.Bold) }; Text(log.rawMessage, fontSize = 12.sp, color = Color.Black, maxLines = 3); Row { log.amount?.let { Text("المبلغ: $it  ", fontSize = 10.sp, color = Color.Gray) }; log.phone?.let { Text("الهاتف: $it", fontSize = 10.sp, color = Color.Gray) } } } } } } }
            }
        }
    }
    LaunchedEffect(msg) { if (msg != null) { delay(2500); viewModel.clearMessage() } }
}
