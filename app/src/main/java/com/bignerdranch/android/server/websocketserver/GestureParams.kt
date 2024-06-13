package com.bignerdranch.android.server.websocketserver

import kotlinx.serialization.Serializable

@Serializable
data class GestureParams(val requestId: Long, val direction: Int, val distance: Int)
