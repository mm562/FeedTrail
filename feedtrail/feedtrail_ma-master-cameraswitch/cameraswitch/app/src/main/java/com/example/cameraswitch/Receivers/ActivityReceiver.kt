package com.example.cameraswitch.Receivers

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.cameraswitch.MainActivity

class ActivityReceiver : BroadcastReceiver() {
    private val TAG = "ActivityReceiver"

    // List of known social media apps
    private val socialMediaApps = arrayOf("com.facebook.katana", "com.twitter.android", "com.instagram.android")

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val recentTasks = activityManager.getRecentTasks(1, ActivityManager.RECENT_WITH_EXCLUDED)
            if (recentTasks.size > 0) {
                val topTask = recentTasks[0]
                val topActivity = topTask.baseActivity
                if (topActivity != null) {
                    val topPackageName = topActivity.packageName
                    if (isSocialMediaApp(topPackageName)) {
                        Log.d("Receiver", "Social media app opened: $topPackageName")
                        // Start your app
                        val intent = Intent(context, MainActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(intent)
                    }else{
                        Log.d("Receiver", "No Social media app opened")

                    }
                }else{
                    Log.d("Receiver", "No Activity")

                }
            }
        }
    }

    private fun isSocialMediaApp(packageName: String): Boolean {
        for (socialMediaApp in socialMediaApps) {
            if (packageName == socialMediaApp) {
                return true
            }
        }
        return false
    }
}
