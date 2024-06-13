package com.bignerdranch.android.server.websocketserver

import kotlinx.serialization.Serializable

@Serializable
data class Request(val requestId: Long, val status: Int)

