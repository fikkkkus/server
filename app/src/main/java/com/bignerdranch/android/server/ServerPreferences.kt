package com.bignerdranch.android.server

import android.content.Context
import android.preference.PreferenceManager


private const val PORT = "port"
private const val IP = "ip"

object ServerPreferences {

    fun getIp(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(IP, "")!!
    }

    fun getPort(context:Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PORT, "")!!
    }

    fun setIp(context: Context, ip: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(IP, ip)
            .apply()
    }

    fun setPort(context: Context, port: String) {
        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .putString(PORT, port)
            .apply()
    }

}