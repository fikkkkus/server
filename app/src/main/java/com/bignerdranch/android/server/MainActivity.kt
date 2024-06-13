package com.bignerdranch.android.server

import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bignerdranch.android.server.ui.theme.ServerTheme
import com.bignerdranch.android.server.websocketserver.WebSocketServer
import com.bignerdranch.android.server.websocketserver.WebSocketServer.ServerCallback
import com.google.common.net.InetAddresses

class MainActivity : ComponentActivity(), ServerCallback {

    private var isPlaying by mutableStateOf(false)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WebSocketServer.initialize(context = applicationContext, callback = this)

        setContent {
            setUI()
        }
    }
    override fun onServerError() {
        runOnUiThread {
            Toast.makeText(this, "Проверьте корректность IP и порта", Toast.LENGTH_SHORT).show()
            isPlaying = !isPlaying
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun setUI() {
        ServerTheme {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    AnimatedPlayStopButton(
                        modifier = Modifier.align(Alignment.Center),
                        onStart = { WebSocketServer.start() },
                        onStop = { WebSocketServer.stop() },
                    )
                    ControlButtons(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth(),
                        isPlaying = isPlaying
                    )
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun ControlButtons(modifier: Modifier = Modifier, isPlaying: Boolean) {
        var showDialog by remember { mutableStateOf(false) }

        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            ConfigButton(onClick = { showDialog = true }, enabled = !isPlaying, modifier = Modifier.weight(0.5f))
            Spacer(modifier = Modifier.width(8.dp))
            LogsButton(modifier = Modifier.weight(0.5f))
        }

        if (showDialog) {
            ConfigDialog(onDismiss = { showDialog = false })
        }
    }

    @Composable
    fun ConfigButton(onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
        Button(
            onClick = onClick, enabled = enabled, modifier = modifier
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Default.Settings),
                    contentDescription = "Config",
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Config", fontSize = 16.sp)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @Composable
    fun LogsButton(modifier: Modifier) {
        var showDialog by remember { mutableStateOf(false) }
        val configuration = LocalConfiguration.current
        val screenHeight = configuration.screenHeightDp.dp

        Button(
            onClick = { showDialog = true },
            modifier = modifier
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = rememberVectorPainter(image = Icons.Default.MailOutline),
                    contentDescription = "Logs",
                    modifier = Modifier.size(25.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Logs", fontSize = 16.sp)
            }
        }

        if (showDialog) {
            val scrollState = rememberScrollState()

            LaunchedEffect(showDialog) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }

            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Logs") },
                text = {
                    Column(
                        modifier = Modifier
                            .height(screenHeight / 2)
                            .verticalScroll(scrollState)
                    ) {
                        Text(
                            text = if (WebSocketServer.requestDetails.value == "")
                                "No logs" else WebSocketServer.requestDetails.value,
                            modifier = Modifier.padding(16.dp),
                            textAlign = TextAlign.Left
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showDialog = false }
                    ) {
                        Text("Close")
                    }
                }
            )
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun ConfigDialog(onDismiss: () -> Unit) {
        var ip by remember { mutableStateOf(TextFieldValue(ServerPreferences.getIp(applicationContext))) }
        var port by remember { mutableStateOf(TextFieldValue(ServerPreferences.getPort(applicationContext))) }
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Configuration") },
            text = {
                Column {
                    OutlinedTextField(
                        value = ip,
                        onValueChange = { ip = it },
                        label = { Text("IP Address") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it },
                        label = { Text("Port") },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF90EE90)
                    ),
                    onClick = {
                        val portValue = port.text.toIntOrNull()
                        if (portValue == null || portValue < 0) {
                            Toast.makeText(context, "Порт это обычно целое неотрицательное число", Toast.LENGTH_SHORT).show()
                        } else if (!InetAddresses.isInetAddress(ip.text)) {
                            Toast.makeText(context, "Некорректный IP адрес", Toast.LENGTH_SHORT).show()
                        } else {
                            ServerPreferences.setIp(applicationContext, ip.text)
                            ServerPreferences.setPort(applicationContext, port.text)
                            onDismiss()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xE2E91A1A)
                    ),
                    onClick = onDismiss
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    @Composable
    fun AnimatedPlayStopButton(modifier: Modifier, onStart: () -> Unit, onStop: () -> Unit) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp.dp
        val buttonSize = screenWidth / 2
        val triangleSize = buttonSize / 4

        val backgroundColor by animateColorAsState(
            targetValue = if (isPlaying) Color(0xE2E91A1A) else Color(0xFF90EE90),
            animationSpec = tween(durationMillis = 500), label = ""
        )

        Box(
            modifier = modifier
        ) {
            IconButton(
                onClick = {
                    if (!isPlaying) {
                        val ip = ServerPreferences.getIp(applicationContext)
                        val port = ServerPreferences.getPort(applicationContext)
                        if (ip.isEmpty() || port.isEmpty()) {
                            Toast.makeText(applicationContext, "Порт или IP не задан", Toast.LENGTH_SHORT).show()
                        } else {
                            isPlaying = !isPlaying
                            onStart()
                        }
                    } else {
                        onStop()
                        isPlaying = !isPlaying
                    }
                },
                modifier = Modifier
                    .size(buttonSize)
                    .clip(CircleShape)
                    .background(backgroundColor)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.size(triangleSize)) {
                        if (isPlaying) {
                            drawRect(
                                color = Color.White,
                                size = size,
                            )
                        } else {
                            drawTriangle(
                                Color.White,
                                size.maxDimension
                            )
                        }
                    }
                }
            }
        }
    }

    fun DrawScope.drawTriangle(color: Color, size: Float) {
        val path = Path().apply {
            val yOffset = size / 8
            moveTo(size / 2, 0f - yOffset)
            lineTo(size, size - yOffset)
            lineTo(0f, size - yOffset)
            close()
        }
        rotate(degrees = 90f, pivot = center) {
            drawPath(path, color)
        }
    }
}
