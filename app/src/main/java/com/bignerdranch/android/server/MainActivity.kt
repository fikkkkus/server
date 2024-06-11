package com.bignerdranch.android.server

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.bignerdranch.android.server.ui.theme.ServerTheme
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.application.install
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class GestureParams(var direction: Int, var distance: Int)

class MainActivity : ComponentActivity() {

    private fun generateRandomParams(): GestureParams {
        val direction = (0..1).random()
        val distance = (100..200).random()
        return GestureParams(direction, distance)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ServerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
        startWebSocketServer()
    }

    private fun startWebSocketServer() {
        embeddedServer(Netty, host = "192.168.0.102", port = 8080) {
            install(WebSockets)

            routing {
                webSocket("/ws") {
                    handleWebSocketConnection()
                }
            }
        }.start(wait = false)
    }

    private suspend fun DefaultWebSocketServerSession.handleWebSocketConnection() {
        try {
            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()

                val gestureParams = generateRandomParams()
                val gestureParamsJson = Json.encodeToString(gestureParams)

                delay(1000)
                outgoing.send(Frame.Text(gestureParamsJson))
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Error in WebSocket connection: ${e.localizedMessage}")
        } finally {
            Log.e("WebSocket", "WebSocket connection closed")
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    ServerTheme {
        Greeting("Android")
    }
}
