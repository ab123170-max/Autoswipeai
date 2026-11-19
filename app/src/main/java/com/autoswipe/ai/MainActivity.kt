package com.autoswipe.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Navy = Color(0xFF10233F)
private val Blue = Color(0xFF2878F0)
private val Page = Color(0xFFF3F6FB)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Blue,
                    background = Page,
                    surface = Color.White
                )
            ) {
                AutoSwipeHome(
                    hasOverlayPermission = { Settings.canDrawOverlays(this) },
                    hasAccessibilityPermission = {
                        val enabled = Settings.Secure.getString(
                            contentResolver,
                            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                        ).orEmpty()
                        enabled.contains(AutoSwipeAccessibilityService.COMPONENT_NAME)
                    },
                    requestOverlayPermission = {
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:$packageName")
                            )
                        )
                    },
                    openAccessibilitySettings = {
                        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    },
                    startOverlay = {
                        if (Settings.canDrawOverlays(this)) {
                            startService(Intent(this, FloatingOverlayService::class.java))
                        }
                    },
                    stopOverlay = {
                        stopService(Intent(this, FloatingOverlayService::class.java))
                    }
                )
            }
        }
    }
}

@Composable
private fun AutoSwipeHome(
    hasOverlayPermission: () -> Boolean,
    hasAccessibilityPermission: () -> Boolean,
    requestOverlayPermission: () -> Unit,
    openAccessibilitySettings: () -> Unit,
    startOverlay: () -> Unit,
    stopOverlay: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission()) }
    var accessibilityGranted by remember { mutableStateOf(hasAccessibilityPermission()) }
    var running by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Page) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BubbleChart, null, tint = Blue, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AutoSwipe AI", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Floating automatic video scroller", color = Color.Gray, fontSize = 13.sp)
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Hands-free scrolling", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Part 2 adds a user-controlled swipe engine for supported short-video apps.",
                        color = Color(0xFFD5E1F2), fontSize = 14.sp
                    )
                }
            }

            Text("REQUIRED SETUP", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. Display over other apps", fontWeight = FontWeight.SemiBold, color = Navy)
                    Text(
                        if (permissionGranted) "Permission granted" else "Required to show the floating control.",
                        color = if (permissionGranted) Color(0xFF16834A) else Color.Gray,
                        fontSize = 13.sp
                    )
                    Button(onClick = {
                        if (!hasOverlayPermission()) requestOverlayPermission()
                        permissionGranted = hasOverlayPermission()
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Grant / refresh overlay permission")
                    }
                }
            }

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Enable swipe accessibility", fontWeight = FontWeight.SemiBold, color = Navy)
                    Text(
                        if (accessibilityGranted) "Accessibility service enabled" else "Enable AutoSwipe AI in Android Accessibility settings.",
                        color = if (accessibilityGranted) Color(0xFF16834A) else Color.Gray,
                        fontSize = 13.sp
                    )
                    Button(onClick = openAccessibilitySettings, modifier = Modifier.fillMaxWidth()) {
                        Text("Open Accessibility settings")
                    }
                    OutlinedButton(
                        onClick = { accessibilityGranted = hasAccessibilityPermission() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("I enabled it — refresh status") }
                }
            }

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("3. Floating controls", fontWeight = FontWeight.SemiBold, color = Navy)
                    Text("Show the bubble, open a supported video feed, then tap Start.", color = Color.Gray, fontSize = 13.sp)
                    Button(
                        onClick = {
                            if (hasOverlayPermission()) {
                                startOverlay()
                                running = true
                            } else requestOverlayPermission()
                        },
                        enabled = !running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Show floating button") }
                    OutlinedButton(
                        onClick = { stopOverlay(); running = false },
                        enabled = running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove floating button") }
                }
            }

            Text(
                "Safety: swipes run only while a supported video app is in the foreground. Start and Pause are always user-controlled.",
                color = Color.Gray, fontSize = 11.sp
            )
        }
    }
}
