package com.example.cameraswitch.Receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager

class LowBatteryReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
            context?.registerReceiver(null, ifilter)
        }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = (level / scale.toFloat()) * 100

        // Check if the battery is critically low (<= 5%)
        if (batteryPct < 5) {
            // Send a custom broadcast to AccessService
            sendBatteryLowBroadcast(context)
        }
    }

    private fun sendBatteryLowBroadcast(context: Context?) {
        val intent = Intent("com.example.app.ACTION_BATTERY_LOW") // Custom action
        intent.putExtra("battery_level", 5) // Additional data (optional)
        context?.sendBroadcast(intent) // Sending broadcast
    }
}
