package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AppRepository
import com.example.data.ValidationResult
import com.example.service.FloatingOverlayService
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainAppScreen(repository: AppRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var activationCodeInput by remember { mutableStateOf("") }
    var isActivated by remember { mutableStateOf(false) }
    var activationMessage by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    // Channel subscription flow state
    var showSubscribeDialog by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableStateOf(0) }
    var isSubscribedClicked by remember { mutableStateOf(false) }
    var canVerifySubscription by remember { mutableStateOf(false) }
    var generatedCodeResult by remember { mutableStateOf<String?>(null) }

    // Overlay permissions state
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    LaunchedEffect(Unit) {
        val savedRes = repository.checkSavedActivation()
        if (savedRes is ValidationResult.Success) {
            isActivated = true
            isError = false
            activationMessage = savedRes.message
        }
        // Periodic check for overlay permission
        while (true) {
            hasOverlayPermission = Settings.canDrawOverlays(context)
            delay(1000)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A0C10),
                        Color(0xFF121620),
                        Color(0xFF0A0C10)
                    )
                )
            )
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // App Title Header with Glowing Effect
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF121620),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFF0055)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "𝑨𝑴𝑰𝑵𝑶 𝑨𝑰𝑴𝑩𝑶𝑻 𝑭𝑭",
                            color = Color(0xFFFF0055),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "واجهة التحكم ونظام التفعيل المحلي",
                            color = Color(0xFF00F0FF),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Main Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .border(1.dp, Color(0xFF2A3447), RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF121620)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!isActivated) {
                        // ACTIVATION FORM
                        Text(
                            text = "أدخل كود التفعيل",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = activationCodeInput,
                            onValueChange = { activationCodeInput = it },
                            label = { Text("كود التفعيل", color = Color(0xFF8A99AD)) },
                            placeholder = { Text("أدخل الكود هنا...", color = Color.Gray) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFFF0055),
                                unfocusedBorderColor = Color(0xFF2A3447),
                                focusedLabelColor = Color(0xFFFF0055),
                                cursorColor = Color(0xFFFF0055),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Activation Button
                        Button(
                            onClick = {
                                scope.launch {
                                    val result = repository.validateCode(activationCodeInput)
                                    when (result) {
                                        is ValidationResult.Success -> {
                                            isActivated = true
                                            isError = false
                                            activationMessage = result.message
                                        }
                                        is ValidationResult.Error -> {
                                            isError = true
                                            activationMessage = result.message
                                        }
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "تشغيل",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Message Feedback
                        activationMessage?.let { msg ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = msg,
                                color = if (isError) Color(0xFFFF0055) else Color(0xFF00FF66),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Divider(color = Color(0xFF1E2433))

                        Spacer(modifier = Modifier.height(16.dp))

                        // Get Activation Code Button
                        OutlinedButton(
                            onClick = {
                                showSubscribeDialog = true
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF00F0FF)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF00F0FF)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "الحصول على كود التفعيل",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        // ACTIVATED CONTROL PANEL & DEVICE INFO
                        val batteryLevel = remember { getBatteryPercentage(context) }
                        val deviceModel = remember { getDeviceModelName() }

                        Text(
                            text = "تم التفعيل بنجاح",
                            color = Color(0xFF00FF66),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = activationMessage ?: "مرحبًا بك في القائمة العائمة",
                            color = Color(0xFF8A99AD),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Device Info Card (Phone Model & Battery Level)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1E2433),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3447)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Phone Model Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نوع الهاتف:",
                                        color = Color(0xFF8A99AD),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = deviceModel,
                                        color = Color(0xFF00F0FF),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Divider(color = Color(0xFF2A3447).copy(alpha = 0.5f))

                                // Battery Percentage Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "نسبة الشحن:",
                                        color = Color(0xFF8A99AD),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "$batteryLevel%",
                                        color = if (batteryLevel > 20) Color(0xFF00FF66) else Color(0xFFFF0055),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        if (!hasOverlayPermission) {
                            // Permission Request Section
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFF0055).copy(alpha = 0.15f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF0055)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "الظهور فوق التطبيقات الأخرى",
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "يرجى منح صلاحية الظهور فوق التطبيقات لتشغيل قائمة الغش العائمة.",
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            val intent = Intent(
                                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055))
                                    ) {
                                        Text("منح الصلاحية الآن", color = Color.White)
                                    }
                                }
                            }
                        } else {
                            // Launch Cheat Menu Button
                            Button(
                                onClick = {
                                    val intent = Intent(context, FloatingOverlayService::class.java)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                    Toast.makeText(context, "تم تشغيل قائمة الغش العائمة بنجاح", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0055)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "تشغيل قائمة الغش",
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Developer Section Footer
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF121620),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E2433)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "المطور",
                            color = Color(0xFF8A99AD),
                            fontSize = 12.sp
                        )
                        Text(
                            text = "تواصل مع المطور",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Telegram Contact Button
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/amin_Jaddi"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0088CC)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Telegram",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // CHANNEL SUBSCRIPTION DIALOG
        if (showSubscribeDialog) {
            AlertDialog(
                onDismissRequest = { showSubscribeDialog = false },
                containerColor = Color(0xFF121620),
                titleContentColor = Color.White,
                textContentColor = Color.White,
                title = {
                    Text(
                        text = "الحصول على كود التفعيل",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "اشترك في قناة AMINO TV FF للحصول على كود التفعيل",
                            color = Color.LightGray,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // YouTube Channel Button
                        Button(
                            onClick = {
                                isSubscribedClicked = true
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/@amino_tv_ff")
                                )
                                context.startActivity(intent)

                                // Start 10 seconds countdown
                                scope.launch {
                                    countdownSeconds = 10
                                    canVerifySubscription = false
                                    while (countdownSeconds > 0) {
                                        delay(1000)
                                        countdownSeconds--
                                    }
                                    canVerifySubscription = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الاشتراك في القناة",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Countdown / Verification state
                        if (isSubscribedClicked) {
                            Spacer(modifier = Modifier.height(16.dp))

                            if (countdownSeconds > 0) {
                                Text(
                                    text = "يرجى الانتظار: $countdownSeconds ثانية",
                                    color = Color(0xFF00F0FF),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else if (canVerifySubscription) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val code = repository.generateNewCode()
                                            generatedCodeResult = code
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF66)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "تحقق من الاشتراك",
                                        color = Color.Black,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Display Generated Code result
                        generatedCodeResult?.let { code ->
                            Spacer(modifier = Modifier.height(16.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E2433),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF00F0FF)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "كود التفعيل الخاص بك (صالح 12 ساعة):",
                                        color = Color.LightGray,
                                        fontSize = 12.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = code,
                                        color = Color(0xFF00F0FF),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Activation Code", code)
                                            clipboard.setPrimaryClip(clip)
                                            activationCodeInput = code
                                            Toast.makeText(context, "تم نسخ الكود بنجاح", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A3447)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("نسخ الكود", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showSubscribeDialog = false
                        isSubscribedClicked = false
                        canVerifySubscription = false
                        countdownSeconds = 0
                        generatedCodeResult = null
                    }) {
                        Text("إغلاق", color = Color.LightGray)
                    }
                }
            )
        }
    }
}

private fun getBatteryPercentage(context: Context): Int {
    return try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
        val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        if (level in 0..100) {
            level
        } else {
            val iFilter = android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, iFilter)
            val rawLevel = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (rawLevel >= 0 && scale > 0) {
                (rawLevel * 100 / scale)
            } else 85
        }
    } catch (e: Exception) {
        85
    }
}

private fun getDeviceModelName(): String {
    val manufacturer = Build.MANUFACTURER.orEmpty().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    val model = Build.MODEL.orEmpty()
    return if (model.startsWith(manufacturer, ignoreCase = true)) {
        model
    } else {
        "$manufacturer $model"
    }.ifEmpty { "Android Device" }
}

