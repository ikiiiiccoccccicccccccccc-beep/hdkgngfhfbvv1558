package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.data.AppDatabase
import com.example.data.AppRepository
import com.example.data.CheatSetting
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

class FloatingOverlayService : LifecycleService(), SavedStateRegistryOwner {

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private lateinit var repository: AppRepository

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        repository = AppRepository(applicationContext, AppDatabase.getInstance(applicationContext))
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        startForegroundServiceNotification()
        showFloatingWindow()
    }

    private fun startForegroundServiceNotification() {
        val channelId = "AMINO_AIMBOT_OVERLAY_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "AMINO AIMBOT Overlay",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("AMINO AIMBOT FF")
            .setContentText("قائمة الغش تعمل في الخلفية")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()

        startForeground(1001, notification)
    }

    private fun showFloatingWindow() {
        val layoutParamsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutParamsType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 100
            y = 200
        }

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)

            setContent {
                MyApplicationTheme {
                    FloatingContent(
                        repository = repository,
                        onDrag = { dx, dy ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            windowManager.updateViewLayout(this, params)
                        },
                        onExitService = {
                            stopSelf()
                        }
                    )
                }
            }
        }

        overlayView = composeView
        windowManager.addView(composeView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let {
            windowManager.removeView(it)
        }
    }
}

@Composable
fun FloatingContent(
    repository: AppRepository,
    onDrag: (Float, Float) -> Unit,
    onExitService: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var cheatSetting by remember { mutableStateOf(CheatSetting()) }

    LaunchedEffect(Unit) {
        repository.cheatSettingsFlow.collect { setting ->
            if (setting != null) {
                cheatSetting = setting
            } else {
                repository.getCheatSetting()
            }
        }
    }

    fun updateSetting(updated: CheatSetting) {
        cheatSetting = updated
        scope.launch {
            repository.updateCheatSetting(updated)
        }
    }

    Box(
        modifier = Modifier.wrapContentSize()
    ) {
        if (!isExpanded) {
            // Floating Button Icon (Draggable)
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF0055),
                                Color(0xFF0A0C10)
                            )
                        )
                    )
                    .border(2.dp, Color(0xFF00F0FF), CircleShape)
                    .clickable { isExpanded = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        } else {
            // Floating Panel (Cheat Menu)
            Card(
                modifier = Modifier
                    .width(310.dp)
                    .wrapContentHeight()
                    .padding(8.dp)
                    .shadow(16.dp, RoundedCornerShape(16.dp))
                    .border(2.dp, Color(0xFFFF0055), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF121620)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header with Drag bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    onDrag(dragAmount.x, dragAmount.y)
                                }
                            }
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFFF0055), Color(0xFF00F0FF))
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "قائمة الغش",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                                .clickable { isExpanded = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "X",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Menu Status Text
                    Text(
                        text = if (cheatSetting.menuActive) "قائمة الغش تعمل" else "إيقاف قائمة الغش",
                        color = if (cheatSetting.menuActive) Color(0xFF00FF66) else Color(0xFFFF0055),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Options list
                    CheatOptionToggle(
                        title = "هيد شوت فقط 100%",
                        checked = cheatSetting.headshotOnly,
                        onCheckedChange = { updateSetting(cheatSetting.copy(headshotOnly = it)) }
                    )
                    CheatOptionToggle(
                        title = "كشف أماكن",
                        checked = cheatSetting.espLocations,
                        onCheckedChange = { updateSetting(cheatSetting.copy(espLocations = it)) }
                    )
                    CheatOptionToggle(
                        title = "اختراق الجدران",
                        checked = cheatSetting.wallHack,
                        onCheckedChange = { updateSetting(cheatSetting.copy(wallHack = it)) }
                    )
                    CheatOptionToggle(
                        title = "تجميد العدو",
                        checked = cheatSetting.freezeEnemy,
                        onCheckedChange = { updateSetting(cheatSetting.copy(freezeEnemy = it)) }
                    )
                    CheatOptionToggle(
                        title = "قتل تلقائي",
                        checked = cheatSetting.autoKill,
                        onCheckedChange = { updateSetting(cheatSetting.copy(autoKill = it)) }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Toggle Menu State Button
                    Button(
                        onClick = {
                            updateSetting(cheatSetting.copy(menuActive = !cheatSetting.menuActive))
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cheatSetting.menuActive) Color(0xFF1E2433) else Color(0xFFFF0055)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (cheatSetting.menuActive) "إيقاف قائمة الغش" else "تشغيل قائمة الغش",
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Exit App & Overlay Service Button
                    OutlinedButton(
                        onClick = {
                            onExitService()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF0055)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "خروج من التطبيق")
                    }
                }
            }
        }
    }
}

@Composable
fun CheatOptionToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Color(0xFF1E2433), shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF0055),
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF0A0C10)
            )
        )
    }
}
