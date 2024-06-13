package com.bignerdranch.android.server.websocketserver

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.mutableStateOf
import com.bignerdranch.android.server.ServerPreferences
import com.bignerdranch.android.server.sqlite.DatabaseHelper
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Duration

@RequiresApi(Build.VERSION_CODES.O)
object WebSocketServer {

    private lateinit var applicationContext: Context
    private lateinit var dbHelper: DatabaseHelper
    val requestDetails = mutableStateOf("")

    private var callback: ServerCallback? = null

    fun initialize(context: Context, callback: ServerCallback) {
        applicationContext = context
        dbHelper = DatabaseHelper(applicationContext)
        this.callback = callback
    }

    private val serverScope = CoroutineScope(Dispatchers.Default)
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    fun start() {
        serverScope.launch {
            try {
                server = embeddedServer(
                    Netty,
                    host = ServerPreferences.getIp(applicationContext),
                    port = ServerPreferences.getPort(applicationContext).toInt()
                ) {
                    install(WebSockets)

                    routing {
                        webSocket("/ws") {
                            handleWebSocketConnection()
                        }
                    }
                }
                server?.start(true)
            } catch (e: Exception) {
                callback?.onServerError()
            }
        }
    }

    fun stop() {
        serverScope.launch {
            server?.stop(0, 0)
            server = null
        }
    }

    private suspend fun DefaultWebSocketServerSession.handleWebSocketConnection() {
        val ip = call.request.local.remoteAddress
        try {
            val port = ServerPreferences.getPort(applicationContext).toInt()

            withContext(Dispatchers.IO) {
                if (!dbHelper.isIpInDatabase(ip)) {
                    dbHelper.insertIpAddress(ip)
                }
            }

            val firstFrame = incoming.receive() as? Frame.Text
            firstFrame?.let {
                val gestureParams = generateRandomParams()
                val direction = gestureParams.first()
                val distance = gestureParams.last()
                val requestId = withContext(Dispatchers.IO) {
                    dbHelper.insertLinkData(ip, port, direction, distance, 0)
                }
                val gesture = GestureParams(requestId, direction, distance)
                val gestureParamsJson = Json.encodeToString(gesture)

                outgoing.send(Frame.Text(gestureParamsJson))
            }

            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                val request = Json.decodeFromString<Request>(receivedText)

                withContext(Dispatchers.IO) {
                    dbHelper.updateRequestStatus(request.requestId, request.status)
                }
                val details = withContext(Dispatchers.IO) {
                    dbHelper.getRequestDetailsAsString(request.requestId)
                }

                if (requestDetails.value == "") {
                    requestDetails.value = details
                } else {
                    requestDetails.value += "\n\n" + details
                }

                val gestureParams = generateRandomParams()
                val direction = gestureParams.first()
                val distance = gestureParams.last()
                val newRequestId = withContext(Dispatchers.IO) {
                    dbHelper.insertLinkData(ip, port, direction, distance, 0)
                }
                val gesture = GestureParams(newRequestId, direction, distance)
                val gestureParamsJson = Json.encodeToString(gesture)

                delay(2000)
                outgoing.send(Frame.Text(gestureParamsJson))
            }
        } catch (e: Exception) {
            Log.e("WebSocket", "Error in WebSocket connection: ${e.localizedMessage}")
        } finally {
            Log.e("WebSocket", "WebSocket connection closed")
            handleDisconnectedClient(ip)
        }
    }

    private suspend fun handleDisconnectedClient(clientIp: String) {
        withContext(Dispatchers.IO) {
            val pendingRequests = dbHelper.getPendingRequests(clientIp)

            pendingRequests.forEach { details ->
                requestDetails.value += "\n\n" + details
            }
        }
    }


    private fun generateRandomParams(): IntArray {
        val direction = (0..1).random()
        val distance = (300..350).random()
        return intArrayOf(direction, distance)
    }

    interface ServerCallback {
        fun onServerError()
    }
}
