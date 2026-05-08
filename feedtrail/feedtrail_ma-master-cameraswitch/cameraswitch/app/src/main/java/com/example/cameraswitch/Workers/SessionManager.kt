package com.example.cameraswitch.Workers

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class SessionManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "SessionPrefs"
        private const val SESSION_COUNTER_KEY = "sessionCounter"
        private const val LAST_SESSION_TIMESTAMP_KEY = "lastSessionTimestamp"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun incrementSessionCounter(): Int {
        val currentCounter = prefs.getInt(SESSION_COUNTER_KEY, 0)+1
        prefs.edit().putInt(SESSION_COUNTER_KEY, currentCounter).apply()
        prefs.edit().putLong(LAST_SESSION_TIMESTAMP_KEY, System.currentTimeMillis()).apply()
        Log.d("SessionManager", "Incremented session counter to $currentCounter")
        return currentCounter
    }

    fun getSessionCounter(): Int {
        return prefs.getInt(SESSION_COUNTER_KEY, 0)
    }

    fun getLastSessionTimestamp(): Long {
        return prefs.getLong(LAST_SESSION_TIMESTAMP_KEY, 0)
    }

    fun getBackCameraTriggerCount(): Int {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getInt("backCameraTriggerCount", 0)
    }

}
