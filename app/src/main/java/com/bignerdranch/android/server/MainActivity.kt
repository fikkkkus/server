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

class MainActivity : ComponentActivity() {

    fun generateRandomParams(): String {
        val param1 = (0..1).random() // Пример генерации рандомного числа от 1 до 10
        val param2 = (1..100).random() // Пример генерации рандомного числа от 1 до 100
        return "$param1.$param2" // Возвращаем два параметра, разделенные запятой
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ServerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }

        embeddedServer(Netty, host = "192.168.0.102", port = 8080) {
            install(WebSockets)

            routing {
                webSocket("/ws") {
                    // Ожидание готовности клиента
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        Log.e("Received", receivedText)

                        val responseText = generateRandomParams()
                        outgoing.send(Frame.Text(responseText))
                    }
                }
            }
        }.start(wait = false)
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