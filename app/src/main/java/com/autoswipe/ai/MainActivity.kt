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
                    requestOverlayPermission = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:$packageName")
                        )
                        startActivity(intent)
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
    requestOverlayPermission: () -> Unit,
    startOverlay: () -> Unit,
    stopOverlay: () -> Unit
) {
    var permissionGranted by remember { mutableStateOf(hasOverlayPermission()) }
    var running by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Page) {
        Column(
            modifier = Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.BubbleChart, null, tint = Blue, modifier = Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("AutoSwipe AI", fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Navy)
                    Text("Your floating scroll assistant", color = Color.Gray, fontSize = 13.sp)
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Navy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Scroll less. Watch hands-free.", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "Control your video feed with a small floating button while using supported apps.",
                        color = Color(0xFFD5E1F2), fontSize = 14.sp
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        Text("Floating controls", color = Color.White)
                    }
                }
            }

            Text("SETUP", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Display over other apps", fontWeight = FontWeight.SemiBold, color = Navy)
                    Text(
                        if (permissionGranted) "Permission granted" else "Allow AutoSwipe AI to show its floating button.",
                        color = if (permissionGranted) Color(0xFF16834A) else Color.Gray,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = {
                            if (!hasOverlayPermission()) requestOverlayPermission()
                            else permissionGranted = true
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (permissionGranted) "Permission enabled" else "Grant overlay permission")
                    }
                }
            }

            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("2. Floating button", fontWeight = FontWeight.SemiBold, color = Navy)
                    Text("Start the floating control, then open TikTok, Reels or Shorts.", color = Color.Gray, fontSize = 13.sp)
                    Button(
                        onClick = {
                            if (hasOverlayPermission()) {
                                startOverlay()
                                running = true
                                permissionGranted = true
                            } else requestOverlayPermission()
                        },
                        enabled = !running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Show floating button") }
                    OutlinedButton(
                        onClick = {
                            stopOverlay()
                            running = false
                        },
                        enabled = running,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Remove floating button") }
                }
            }

            Spacer(Modifier.weight(1f))
            Text(
                "Part 1 preview: floating interface only. Automatic swiping will be added in Part 2.",
                color = Color.Gray, fontSize = 12.sp
            )
        }
    }
}
