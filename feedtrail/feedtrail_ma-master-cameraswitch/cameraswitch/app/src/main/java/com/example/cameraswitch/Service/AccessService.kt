package com.example.cameraswitch.Service;

import android.Manifest
import com.example.cameraswitch.UtilsAndCons.Constants.CAPTURED_IMAGE_PATH_KEY
import android.accessibilityservice.AccessibilityService
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.usage.UsageStatsManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.util.Pair
import androidx.work.WorkManager
import com.example.cameraswitch.Capturing.BackCameraButton
import com.example.cameraswitch.Capturing.BuiltInBackCamera
import com.example.cameraswitch.Capturing.CameraViewLibrary
import com.example.cameraswitch.Capturing.CombineCameras
import com.example.cameraswitch.Capturing.HideCamera
import com.example.cameraswitch.Capturing.ResetScreenCapturePermission
import com.example.cameraswitch.Capturing.ScreenCaptureService
import com.example.cameraswitch.MainActivity
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.R
import com.example.cameraswitch.Receivers.GlobalBroadcastReceiver
import com.example.cameraswitch.UtilsAndCons.NotificationUtils
import com.example.cameraswitch.UtilsAndCons.PermissionsUtils
import com.example.cameraswitch.Workers.ScreenCaptureWorker
import com.example.cameraswitch.Workers.SessionManager
//import com.example.cameraswitch.Workers.WorkerManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.rvalerio.fgchecker.AppChecker
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Timer
import java.util.TimerTask

class AccessService : AccessibilityService() {
    private var lastUsedPackage: String? = null
    private lateinit var context: Context
    private var isRelevantContent = false
    private var isInVideoTab = false
    private var timer: Timer? = null
    private var startedInfScrollinf = ""
    private var stoppedInfScrollinf = ""
    private var lastTabRelevant: Boolean = false
    private var currentTabRelevant: Boolean = false
    private var cameraTriggeredForTikTok = false
    private val db = FirebaseFirestore.getInstance()
    private var ISTimer: Long = 6000
    private var isReturningFromCamera: Boolean = false
    private var returnedFromCamera: Boolean = false
    private var servicesStarted: Boolean = false
    private var timerExceeded = false
    private var currentSessionId: String = ""
    private var lastContentDescription: String = ""
    private var cameraTriggered = false
    private var coolingPhaseTimer: Timer? = null
    private var coolingPhaseSeconds: Long = 10800000 // make 3 hours in milliseconds
    private var relevantTab_Cooling: Boolean = false
    private var timerEndedWithoutCameraTrigger: Boolean = false
    private var isFirstRelevantTabEntry: Boolean = true
    private var hasEnteredNonRelevantTab: Boolean = false
    private var enetredrelevantfirst: Boolean = false


    private var exitedApp: Boolean = false
    private var triggerForExitCam: Boolean = false
    private val BCinstance = BackCameraButton()
    private var currentForegroundApp: String = ""
    private var lastForegroundApp: String? = null
    var allpicturesTaken = false
    var duration: Long = 0L
    private lateinit var sessionManager: SessionManager
    private val relevantApps = listOf(
        "com.facebook.katana",
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.google.android.youtube"
    )
    private var exitTimer: Timer? = null
    private var exitedForFourMinutes = false
    private var wasScreenOn: Boolean = true
    private var screenwasOffNowOn: Boolean = false
    private var hasExecuted: Boolean = false
    private var isSessionDataUploaded: Boolean = false
    private var isCriticalBatteryLow: Boolean = false
    private var endstudyReached: Boolean = false

    //  private var capturedImagePath: String? = null
    //   private val CAPTURED_IMAGE_PATH_KEY = "capturedImagePath"
    private lateinit var batteryLowReceiver: BroadcastReceiver
    private lateinit var endstudyReeceiver: BroadcastReceiver
    private lateinit var permissionUpdatedReceiver: BroadcastReceiver
    var updatingPermissions = false
    private var isTimerStarted = false
    private var uploadFirebase: Boolean = false
    private var sessionStarted = false

    companion object {

        var instance: AccessService? = null

        fun stopService() {
            instance?.stopSelf()
        }

        private var ScreenCapInt2: Intent? = null
        private var text: String = ""
        fun getMyServiceIntent2(): Intent? = ScreenCapInt2
        fun getCurrentDay(context: Context): Int {
            val prefs = context.getSharedPreferences("AppPrefs", MODE_PRIVATE)
            val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)
            if (firstLaunchTimestamp == 0L) {
                return 1 // Just in case it's not set
            }
            val currentTime = System.currentTimeMillis()
            val difference = currentTime - firstLaunchTimestamp

            //  the number of days passed since the first launch
            val days = (difference / (24 * 60 * 60 * 1000)).toInt()
            return days + 1 // Day count starts at 1
        }
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        Log.d("AccessService", "Service created")
        BCinstance.triggered = false
        setReturningFromCameraFlag(false)
        if (getReturningFromCameraFlag()) {
            // Check if the event is a window state change which could indicate returning to the app
            Log.d("AccessCamera", " if getreturningfromcameraflag ")
            //      if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            if (isAppInFocus(packageName)) {
                startServicesIfNeeded()
            }
            //     }

        }


        sessionManager = SessionManager(applicationContext)
        val intentFilter = IntentFilter("com.example.cameraswitch.UPLOAD_TRIGGER")
        //  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        registerReceiver(
            GlobalBroadcastReceiver(),
            intentFilter,
            Context.RECEIVER_NOT_EXPORTED
        )
        //  }else {

        //   }
        // Register to listen for the custom broadcast


    }

    fun checkBatteryStatus() {
        val batteryStatus: Intent? =
            IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
                context.registerReceiver(null, ifilter)
            }

        val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1

        val batteryPct = (level / scale.toFloat()) * 100 // Battery percentage

        // Check if the battery is critically low (e.g., less than or equal to 5%)
        if (batteryPct < 5) {
            isCriticalBatteryLow = true
            stopISTimer()
            recordSocialMediaUsage(lastUsedPackage ?: "unknown")
            exitTimer?.cancel()
            exitTimer = Timer()
            ScreenCapInt2 = ScreenCaptureService.getStopIntent(context)
            val intent = Intent(context, ScreenCaptureService::class.java)
            stopService(intent)
            val intent2 = Intent(context, CombineCameras::class.java)
            stopService(intent2)
        } else {

        }
    }

    private fun getReturningFromCameraFlag(): Boolean {
        val sharedPref = getSharedPreferences("CameraSwitchPrefs", Context.MODE_PRIVATE)
        isReturningFromCamera = sharedPref.getBoolean("returningFromCamera", false)
        Log.d("AccessCamera", "Returning from Camera flag value: $isReturningFromCamera")
        return isReturningFromCamera
    }

    private fun setReturningFromCameraFlag(value: Boolean) {
        val previousValue = getReturningFromCameraFlag()
        val sharedPref = getSharedPreferences("CameraSwitchPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putBoolean("returningFromCamera", value)
            apply()
        }
        Log.d(
            "AccessCamera",
            "Changed Returning from Camera flag from: $previousValue to: $value"
        )
    }

    private fun isAppInForeground(packageName: String): Boolean {
        val usageStatsManager =
            getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val timeNow = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            timeNow - 1000,
            timeNow
        )
        val foregroundApp = stats?.maxByOrNull { it.lastTimeUsed }?.packageName
        return packageName == foregroundApp
    }

    private fun getActiveApp() {
        val appChecker = AppChecker()


        val handler = android.os.Handler()
        val checkInterval = 1000L
        handler.postDelayed(object : Runnable {
            override fun run() {
                batteryLowReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context?, intent: Intent?) {
                        if (intent?.action == "com.example.app.ACTION_BATTERY_LOW") {
                            isCriticalBatteryLow = true

                        }
                    }
                }
                val powerManager =
                    getSystemService(Context.POWER_SERVICE) as PowerManager
                val isScreenOn =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                        powerManager.isInteractive // Use isInteractive for API 20+

                    } else {
                        @Suppress("DEPRECATION")
                        powerManager.isScreenOn // Deprecated but valid for older APIs
                    }
                if (!wasScreenOn && isScreenOn) {
                    Log.d("ScreenState", "Screen was OFF and is now ON")
                    screenwasOffNowOn = true
                    /*     val intent =
                            Intent("com.example.cameraswitch.UPLOAD_TRIGGER_CANCEL").apply {
                                    putExtra("cameraSource", "BACK")
                                    putExtra("cameraSource", "FRONT")
                                    putExtra("camerasource", "SCREENSHOT")
                            }
                    context.sendBroadcast(intent)*/
                }
                if ((!isScreenOn || isCriticalBatteryLow) && !hasExecuted) {
                    hasExecuted = true
                    //   if(screenisoff) {
                    Log.d("Exit", "in getActiveApp")
                    stopISTimer()
                    recordSocialMediaUsage(lastUsedPackage ?: "unknown")
                    exitTimer?.cancel()
                    exitTimer = Timer()
                    ScreenCapInt2 =
                        ScreenCaptureService.getStopIntent(context)
                    val intent = Intent(
                        context,
                        ScreenCaptureService::class.java
                    )
                    stopService(intent)
                    val intent2 =
                        Intent(context, CombineCameras::class.java)
                    stopService(intent2)
                    coolingPhaseTimer?.cancel()
                    exitTimer?.schedule(object : TimerTask() {
                        override fun run() {
                            exitedForFourMinutes = true
                            Log.d(
                                "AccessService",
                                "User exited for 4 minutes, preparing to upload."
                            )
                            //  uploadFirebase= true
                            triggerUploadIfImageCaptured()
                        }
                    }, 240 * 1000)
                    Log.d("ExitTimer", exitTimer.toString())
                    //  screenisoff = false

                } else if (isScreenOn) {

                    hasExecuted = false

                }
                // Update the previous state
                //    wasScreenOn = isScreenOn
                //  screenwasOffNowOn=false
                currentForegroundApp =
                    appChecker.getForegroundApp(applicationContext)
                if (currentForegroundApp != lastForegroundApp) {
                    Log.d(
                        "AppTest",
                        "New foreground app: $currentForegroundApp"
                    )
                    if (currentForegroundApp in relevantApps && (lastForegroundApp == null || lastForegroundApp == "com.android.launcher" || lastForegroundApp == "com.sec.android.app.launcher")) {
                        // Detected app reentry
                        Log.d(
                            "AppTest",
                            "App reentered currentTabRelevant: $currentTabRelevant"
                        )

                        //    if (currentTabRelevant) {
                        if (exitedApp) {
                            if (currentTabRelevant || currentForegroundApp == "com.zhiliaoapp.musically") {
                                Log.d(
                                    "AppTest",
                                    " staring backcamera"
                                )
                                exitedApp = false

                                triggerBackCamera()
                                setupCoolingPhaseTimer()
                            }
                        }
                        // }
                    } else if ((((currentForegroundApp == null || currentForegroundApp == "com.android.launcher" || currentForegroundApp == "com.sec.android.app.launcher") && (!currentTabRelevant || currentTabRelevant)) && lastForegroundApp in relevantApps)) {
                        Log.d("AppTest", "App has exit")
                        Log.d("Timer", "Timer stopping , in exit clause")
                        if (lastTabRelevant) {
                            stopISTimer()
                            /**edited**/
                            recordSocialMediaUsage(
                                lastUsedPackage ?: "unknown"
                            )
                        }
                        isSessionDataUploaded = false
                        exitTimer?.cancel() // Cancel any existing timer
                        exitTimer = Timer()
                        /* exitTimer?.schedule(object : TimerTask() {
                                override fun run() {
                                        exitedForFourMinutes = true
                                        Log.d("AccessService", "User exited for 4 minutes, preparing to upload.")
                                        //  uploadFirebase= true
                                        triggerUploadIfImageCaptured()
                                }
                        },  240 * 1000)
                        Log.d("ExitTimer", exitTimer.toString())*/

                        /**edited**/
                        //   ScreenCapInt2 = ScreenCaptureService.getStopIntent_SessionCounter(context)
                        ScreenCapInt2 =
                            ScreenCaptureService.getStopIntent(context)

                        //    ScreenCapInt2 = ScreenCaptureService.isStopCommand(this)
                        val intent = Intent(
                            context,
                            ScreenCaptureService::class.java
                        )
                        stopService(intent)

                        val intent2 =
                            Intent(context, CombineCameras::class.java)
                        stopService(intent2)
                        /*val workerManager = WorkerManager(context)
                        workerManager.stopWorker()
                        ScreenCaptureWorker.MediaProjectionManagerSingleton.stopProjection()*/

                        exitedApp = true
                        if (coolingPhaseTimer != null) {
                            Log.d("AppTest", "Timer ended")

                            coolingPhaseTimer?.cancel()
                            coolingPhaseTimer = null
                        }

                        exitTimer?.schedule(object : TimerTask() {
                            override fun run() {
                                exitedForFourMinutes = true
                                Log.d(
                                    "AccessService",
                                    "User exited for 4 minutes, preparing to upload."
                                )
                                //  uploadFirebase= true
                                triggerUploadIfImageCaptured()
                            }
                        }, 240 * 1000)
                        Log.d("ExitTimer", exitTimer.toString())


                    }
                    lastForegroundApp = currentForegroundApp
                }
                handler.postDelayed(this, checkInterval)
            }
        }, checkInterval)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        FirebaseFirestore.getInstance().firestoreSettings =
            FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build()
        Log.d(
            "AccessEvent",
            "Event: " + event.getPackageName() + ", Type: " + event.getEventType()
        );
        val packageName = event.packageName?.toString() ?: return
        val eventType = event.eventType
        val rootNode = rootInActiveWindow ?: return
        val contentDesc = extractActiveTabContentDescription(rootNode)
        val isActive = rootInActiveWindow?.packageName?.toString() == packageName
        //   Log.d("AccsessCamera", "Returning from Camera flag value: $isReturningFromCamera")
        //   rootNodeHasComments(rootNode)
        /* val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        val isScreenOn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
                powerManager.isInteractive // Use isInteractive for API 20+
        } else {
                @Suppress("DEPRECATION")
                powerManager.isScreenOn // Deprecated but valid for older APIs
        }

        if (!isScreenOn) {
                Log.d("AccessTest", "Screen is OFF or locked.")
              //  handleScreenLock()
              exitedApp=true
                exitTimer?.cancel()
                coolingPhaseTimer?.cancel()
              //  exitTimer?.cancel()

        }*/
        //   Log.d("AccessService", "Package: $packageName, Active Tab ContentDesc: $contentDesc , currentRelevantTab: $currentTabRelevant")
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            Log.d(
                "AccessTest",
                "Window state changed for $packageName, Active: $isActive"
            )

            if (!isAppInForeground(packageName)) {
                Log.d("AccessTest", "Window closed")
            }

        }

        //    Log.d("AccsessCamera","  ${getReturningFromCameraFlag()}")
        if (lastUsedPackage != packageName) {

            lastUsedPackage = packageName

        }
        text = event.text.joinToString(" ")

        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            eventType == AccessibilityEvent.CONTENT_CHANGE_TYPE_CONTENT_DESCRIPTION ||
            eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {

            when (packageName) {
                "com.facebook.katana", "com.facebook.android" -> handleFacebookTest(
                    contentDesc
                )

                "com.instagram.android" -> handleInstagramEvent(contentDesc)
                "com.zhiliaoapp.musically" -> handleTikTokEvent(
                    contentDesc,
                    eventType
                )

                "com.google.android.youtube" -> handleYouTubeEvent(
                    contentDesc,
                    eventType
                )

                else -> notRelevantTabDeactivation(contentDesc)
            }

            /*   if (!isAppInFocus(packageName)) {
                    // Cancel the cooling phase timer if the app is no longer in focus
                    coolingPhaseTimer?.cancel()
                    coolingPhaseTimer = null
                    Log.d("Timer", "App is not in focus, Cooling timer cancelled")
            }*/

        }

        getActiveApp()

        /*  if (event != null && shouldStartScreenCapture) {
                Log.d("Screenshot", "should Start")
                val sharedPreferences = getSharedPreferences("ScreenCapturePrefs", MODE_PRIVATE)
                val resultCode = sharedPreferences.getInt("resultCode", Activity.RESULT_CANCELED)
                val resultDataString = sharedPreferences.getString("resultData", null)
                val resultData = Intent.parseUri(resultDataString, 0)

                val captureIntent = Intent(this, ScreenCaptureService::class.java)
                captureIntent.putExtra("resultCode", resultCode)
                captureIntent.putExtra("resultData", resultData)


                startService(captureIntent)
        }*/
    }


    private fun isAppInFocus(packageName: String): Boolean {
        // Here, you could check if the current app matches the package name of your app
        return packageName in listOf(
            "com.facebook.katana",
            "com.instagram.android",
            "com.zhiliaoapp.musically",
            "com.google.android.youtube"
        )
    }

    private fun startServicesIfNeeded() {
        if (!servicesStarted && getReturningFromCameraFlag()) {
            Log.d("AccessCamera", "Starting hide camera and screencap")
            //   val hideCameraIntent = Intent(this, HideCamera::class.java)
            //    startService(hideCameraIntent)

            //     val captureIntent = Intent(this, ScreenCaptureService::class.java)
            //      startService(captureIntent)

            // Reset the flags
            //   setReturningFromCameraFlag(false)
            servicesStarted =
                true  // This should be reset elsewhere when services are done
            Log.d("AccessCamera", "Flags reset")
        }
    }

    private fun handleFacebookEvent(contentDesc: String) {
        Log.d("AccessServiceFb", "Handling Facebook event")
        when {
            "Video" in contentDesc -> relevantTabActivation(
                "com.facebook.katana",
                contentDesc
            )

            isInVideoTab -> relevantTabActivation("com.facebook.katana", contentDesc)
            !isAppInFocus("com.facebook.katana") -> notRelevantTabDeactivation("Facebook exited")
            else -> notRelevantTabDeactivation(contentDesc)
        }
    }

    private fun isAppJustOpened(): Boolean {
        return lastForegroundApp != "com.facebook.katana" && currentForegroundApp == "com.facebook.katana"
    }


    /*  private fun handleFacebookEvent2(contentDesc: String) {
        if (isAppJustOpened()) {
                Log.d("debugFB", "Facebook app just opened, skipping camera trigger.")
                lastTabRelevant = false
                return
        }
       if (lastTabRelevant != currentTabRelevant) {
               lastTabRelevant = currentTabRelevant
       }

        var currentTabRelevant = "Video" in contentDesc

        if (currentTabRelevant && !lastTabRelevant && lastContentDescription != contentDesc) {
                Log.d("debugFB", "Switching to relevant tab in Facebook")
                relevantTabActivation("com.facebook.katana", contentDesc)
        } else if (!currentTabRelevant && lastTabRelevant) {
                Log.d("debugFB", "Exiting relevant tab in Facebook")
                val rootNode = rootInActiveWindow
                if (rootNode != null && rootNodeHasComments(rootNode)) {
                        Log.d("AccessService", "Detected comments section, keeping tab relevant.")
                    //    currentTabRelevant =true
                       currentTabRelevant ==true
                        return // Prevent deactivation if in the comments section
                }
                stopISTimer()
                recordSocialMediaUsage("com.facebook.katana")

                Log.d("AccessService", "Deactivating tab, contentDesc: $contentDesc")
                /**edited**/

                ScreenCapInt2 = ScreenCaptureService.getStopIntent(context)
                val intent = Intent(context, ScreenCaptureService::class.java)
                stopService(intent)

                val intent2 = Intent(context, CombineCameras::class.java)
                stopService(intent2)
                /*val workerManager = WorkerManager(context)
                workerManager.stopWorker()
                ScreenCaptureWorker.MediaProjectionManagerSingleton.stopProjection()*/
                exitTimer?.cancel() // Cancel any existing timer
                exitTimer = Timer()
                exitTimer?.schedule(object : TimerTask() {
                        override fun run() {
                                exitedForFourMinutes = true
                                Log.d("AccessService", "User exited for 4 minutes, preparing to upload.")
                                //  uploadFirebase= true
                                triggerUploadIfImageCaptured()
                        }
                }, 240 * 1000)
               // notRelevantTabDeactivation(contentDesc)
        }

        lastContentDescription = contentDesc
      //  lastTabRelevant = currentTabRelevant
}*/
    @SuppressLint("SuspiciousIndentation")
    private fun handleFacebookEvent2(contentDesc: String) {

        // If the app is just opened, skip camera trigger and track non-relevant tab entry
        if (isAppJustOpened()) {
            Log.d("debugFB", "Facebook app just opened, skipping camera trigger.")


            // Reset flags when the app is opened for the first time or re-entered
            lastTabRelevant = false // Reset the flag when the app is first opened
            hasEnteredNonRelevantTab = false // Reset non-relevant tab entry flag
            isFirstRelevantTabEntry = true // Allow triggering camera again if needed
            enetredrelevantfirst = false

            return
        } else if ("Video" in contentDesc && !enetredrelevantfirst) {


            Log.d(
                "debugFB",
                "Opening directly on relevant tab (Video), triggering camera."
            )
            triggerBackCamera()  // Trigger the camera immediately
            //  isFirstRelevantTabEntry = false // Ensure camera is not triggered again
            enetredrelevantfirst = true

            return
        }
        // Determine if the current tab is relevant based on content description
        val currentTabRelevant = "Video" in contentDesc


        // If the user enters a non-relevant tab for the first time after reopening the app
        if (!lastTabRelevant && !currentTabRelevant) {
            Log.d(
                "debugFB",
                "Entered a non-relevant tab, waiting for switch to relevant tab."
            )
            hasEnteredNonRelevantTab = true
            Log.d("debugFB", "Exiting relevant tab in Facebook")
            if (!sessionStarted) {
                Log.d(
                    "AccessService",
                    "No session started, so no usage will be recorded."
                )
                return
            }
            stopISTimer() // Stop any active timers if needed
            // recordSocialMediaUsage("com.facebook.katana")
            isSessionDataUploaded = false
            enetredrelevantfirst = false

            // Stop services related to the camera
            ScreenCapInt2 = ScreenCaptureService.getStopIntent(context)
            val intent = Intent(context, ScreenCaptureService::class.java)
            stopService(intent)

            val intent2 = Intent(context, CombineCameras::class.java)
            stopService(intent2)

            sessionStarted = false
        }

        // If user switches from non-relevant to relevant tab
        if (hasEnteredNonRelevantTab && currentTabRelevant && isFirstRelevantTabEntry) {
            // This is the first time entering a relevant tab after a non-relevant tab
            Log.d(
                "debugFB",
                "Switching to relevant tab from non-relevant. Triggering camera."
            )
            //   relevantTabActivation("com.facebook.katana", contentDesc)
            // triggerBackCamera()  // Trigger the back camera only once

            setupCoolingPhaseTimer_otherApps()
            enetredrelevantfirst = true

            // After the first entry to a relevant tab, reset the flag
            isFirstRelevantTabEntry = false
            hasEnteredNonRelevantTab = false // Reset non-relevant tab entry flag
        }

        // If leaving a relevant tab and going to a non-relevant tab
        else if (!currentTabRelevant && lastTabRelevant) {
            Log.d("debugFB", "Exiting relevant tab in Facebook")
            stopISTimer() // Stop any active timers if needed
            recordSocialMediaUsage("com.facebook.katana")
            isSessionDataUploaded = false
            // Stop services related to the camera
            ScreenCapInt2 = ScreenCaptureService.getStopIntent(context)
            val intent = Intent(context, ScreenCaptureService::class.java)
            stopService(intent)

            val intent2 = Intent(context, CombineCameras::class.java)
            stopService(intent2)

            // Reset the exit timer and prepare for the next time
            exitTimer?.cancel()
            exitTimer = Timer()
            exitTimer?.schedule(object : TimerTask() {
                override fun run() {
                    exitedForFourMinutes = true
                    Log.d(
                        "AccessService",
                        "User exited for 4 minutes, preparing to upload."
                    )
                    triggerUploadIfImageCaptured()
                }
            }, 240 * 1000) // Wait for 4 minutes before triggering upload
        }

        // Update last content description and tab relevance status for the next event comparison
        lastContentDescription = contentDesc
        lastTabRelevant = currentTabRelevant
    }

    private fun handleFacebookTest(contentDesc: String) {

        Log.d("AccessServiceFB", "Handling FB event. Current tab: $contentDesc")

        // Determine if the current tab is one of the relevant tabs
        //    val currentTabRelevant = "Home" in contentDesc || "Reels" in contentDesc || "Comments" in contentDesc
        val currentTabRelevant = "Video" in contentDesc || "Comments" in contentDesc

        // Act based on whether the current tab is relevant
        //   if (currentTabRelevant || (currentTabRelevant&& text.contains("comments", ignoreCase = true))) {
        if (currentTabRelevant) {

            // Only activate if there has been a change in the content description or if it's a relevant tab switch
            if (lastContentDescription != contentDesc || !lastTabRelevant) {

                relevantTabActivationInsta(contentDesc)
                //   relevantTabActivation("com.facebook.katana",packageName)
            }

        } else {
            // If not relevant and the state has changed indicating a move away from a relevant tab
            /* if (lastTabRelevant) {
                     notRelevantTabDeactivation(contentDesc)


             }*/
            // currentTabRelevant==false
            if(!currentTabRelevant && lastTabRelevant){
                notRelevantTabDeactivation(contentDesc)
                isSessionDataUploaded=false

            }


        }



        // Update the last content description and relevance status for the next event comparison
        lastContentDescription = contentDesc
        lastTabRelevant = currentTabRelevant
    }

    private fun handleInstagramEvent(contentDesc: String) {

        Log.d("AccessServiceInsta", "Handling Instagram event. Current tab: $contentDesc")

        // Determine if the current tab is one of the relevant tabs
        val currentTabRelevant = ("Home" in contentDesc || "Reels" in contentDesc || "Comments" in contentDesc)

        // Act based on whether the current tab is relevant
        if (currentTabRelevant || (currentTabRelevant&& text.contains("comments", ignoreCase = true))) {
            Log.d("Messages", "current tab : $currentTabRelevant, description : $contentDesc")
            // Only activate if there has been a change in the content description or if it's a relevant tab switch
            if (lastContentDescription != contentDesc || !lastTabRelevant) {


                relevantTabActivationInsta(contentDesc)
            }

        } else {
            // If not relevant and the state has changed indicating a move away from a relevant tab
            /* if (lastTabRelevant) {
                     notRelevantTabDeactivation(contentDesc)


             }*/
            if(!currentTabRelevant && lastTabRelevant){
                notRelevantTabDeactivation(contentDesc)
                isSessionDataUploaded=false

            }


        }



        // Update the last content description and relevance status for the next event comparison
        lastContentDescription = contentDesc
        lastTabRelevant = currentTabRelevant
    }




    private fun handleTikTokEvent(contentDesc: String, eventType: Int) {
        Log.d(
            "AccessService",
            "Handling TikTok event, Content Desc: $contentDesc, Event Type: $eventType"
        )
        if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (!contentDesc.isEmpty()) {
                if (!isTimerStarted) {
                    Log.d("TikTokDebug", "Starting ISTimer for TikTok.")
                    startISTimer()  // Start the timer
                    isTimerStarted = true  // Mark the timer as started
                }
                currentTabRelevant=true
                Log.d(
                    "TikTokDebug",
                    "Content changed with description: $contentDesc"
                )
                relevantTabActivation("com.zhiliaoapp.musically",contentDesc)
            } else {
                Log.d(
                    "TikTokDebug",
                    "Content changed without description, possibly exiting."
                )
                if (!isAppStillInFocus("com.zhiliaoapp.musically")) {
                    Log.d(
                        "TikTokDebug",
                        "TikTok no longer in focus, handling as exit."
                    )
                    notRelevantTabDeactivation("TikTok exit")
                }
            }
        }
    }
    private fun isAppStillInFocus(packageName: String): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        // Get usage stats for the last 1 seconds
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 1000, currentTime)
        if (stats != null && stats.isNotEmpty()) {
            // Check if the passed package is the most recently foregrounded app
            val recentStat = stats.maxByOrNull { it.lastTimeUsed }
            return recentStat?.packageName == packageName && recentStat.lastTimeUsed + 1000 >= currentTime
        }
        return false
    }

    private fun isAppStillInFocus_Yt(packageName: String): Boolean {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val currentTime = System.currentTimeMillis()
        // Get usage stats for the last 1 seconds
        val stats = usageStatsManager.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 500, currentTime)
        if (stats != null && stats.isNotEmpty()) {
            // Check if the passed package is the most recently foregrounded app
            val recentStat = stats.maxByOrNull { it.lastTimeUsed }
            return recentStat?.packageName == packageName && recentStat.lastTimeUsed + 500 >= currentTime
        }
        return false
    }
    /*private fun handleYouTubeEvent(contentDesc: String,eventType: Int){
            val currentTabRelevant = "Shorts" in contentDesc

            if (currentTabRelevant || (currentTabRelevant&& text.contains("comments", ignoreCase = true))) {
                    // Only activate if there has been a change in the content description or if it's a relevant tab switch
                    if (lastContentDescription != contentDesc || !lastTabRelevant) {


                            relevantTabActivationInsta(contentDesc)
                    }

            } else {
                    // If not relevant and the state has changed indicating a move away from a relevant tab
                    /* if (lastTabRelevant) {
                             notRelevantTabDeactivation(contentDesc)


                     }*/
                    if(!currentTabRelevant && lastTabRelevant){
                            notRelevantTabDeactivation(contentDesc)
                    }


            }



            // Update the last content description and relevance status for the next event comparison
            lastContentDescription = contentDesc
            lastTabRelevant = currentTabRelevant
    }*/
    private fun handleYouTubeEvent(contentDesc: String, eventType: Int) {
        val currentTabRelevant = "Shorts" in contentDesc

        // Act based on whether the current tab is relevant
        if (currentTabRelevant) {
            // Only activate if there has been a change in the content description or if it's a relevant tab switch
            if (lastContentDescription != contentDesc || !lastTabRelevant) {

                relevantTabActivationInsta(contentDesc)
                // relevantTabActivation("Youtube", contentDesc)

            }
        } else {
            // If not relevant and the state has changed indicating a move away from a relevant tab
            /*  if (lastTabRelevant) {
                      notRelevantTabDeactivation(contentDesc)
              }*/
            if (!currentTabRelevant && lastTabRelevant) {
                Log.d("debugYT", "Exiting relevant tab in Youtube")
                //   stopISTimer()
                // recordSocialMediaUsage("Youtube")
                notRelevantTabDeactivation(contentDesc)
                isSessionDataUploaded=false
                //    sessionStarted=true

            }
        }



        // Update the last content description and relevance status for the next event comparison
        lastContentDescription = contentDesc
        lastTabRelevant = currentTabRelevant
    }
    /*  private fun handleYouTubeEvent(contentDesc: String, eventType: Int) {
              Log.d("AccessService", "Handling YouTube event, Content Desc: $contentDesc, Event Type: $eventType")
              when (eventType) {
                      AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED  -> {
                              // This might indicate app exit or tab switch
                              if (!isAppStillInFocus_Yt("com.google.android.youtube") && returnedFromCamera==true) {
                                      Log.d("YouTubeDebug", "YouTube no longer in focus, likely exited.")
                                      notRelevantTabDeactivation("YouTube exit")
                              } else {
                                      Log.d("YouTubeDebug", "YouTube still in focus, window state changed.")
                                      // If still in focus but state changed, it could be a tab switch
                                      if ("Shorts" in contentDesc) {
                                              relevantTabActivation("YouTube Shorts",contentDesc)
                                      } else {
                                              notRelevantTabDeactivation("Switched out of Shorts")
                                      }
                              }
                      }
                      else -> {
                              if ("Shorts" in contentDesc) {
                                      relevantTabActivation("YouTube Shorts",contentDesc)
                              } else {
                                      notRelevantTabDeactivation("Not in Shorts")
                              }
                      }
              }
              /*   if ("Shorts" in contentDesc) {
                         relevantTabActivation("com.google.android.youtube")
                 } else if (!isAppInFocus("com.google.android.youtube")) {
                         notRelevantTabDeactivation("YouTube exited")
                 } else {
                         notRelevantTabDeactivation(contentDesc)
                 }*/
      }*/


    private fun isInShortsTab(): Boolean {
        // Implement a check to see if YouTube Shorts is the current tab
        // This may require inspecting the UI elements or content descriptions specific to Shorts
        val rootNode = rootInActiveWindow ?: return false
        return rootNode.findAccessibilityNodeInfosByText("Shorts").isNotEmpty()
    }

    private fun switchTabs(packageName: String, contentDesc: String) {

    }

    @SuppressLint("SuspiciousIndentation")
    private fun relevantTabActivation(packageName: String, contentDesc: String) {
        val permissionsgranted=  PermissionsUtils.areAllPermissionsGranted(context)
        if(!permissionsgranted){
            showMissingPermissionNotification()
        }else {
            Log.d("AccessService", "Activating relevant tab for $packageName")
            //  coolingPhaseTimer?.cancel()
            //    coolingPhaseTimer = Timer()
            if (packageName != "com.zhiliaoapp.musically") {
                startISTimer()

            }
            currentTabRelevant = true
            //   startISTimer()
            exitTimer?.cancel()
            exitedForFourMinutes = false
            //     startISTimer()

            if (timerEndedWithoutCameraTrigger && currentTabRelevant) {

                triggerBackCamera()
                timerEndedWithoutCameraTrigger =
                    false  // Reset the flag after triggering the camera
                return
            }
            if (isFirstRelevantTabEntry && currentTabRelevant) {
                Log.d("debugFB", "first reltab entry & current tab relev")

                Log.d(
                    "CamTrigger",
                    " in the if for isfirstrelevant and trigerring backcamera"
                )

                triggerBackCamera()
                isFirstRelevantTabEntry = false  // Reset the first-time flag
                setupCoolingPhaseTimer_otherApps()
                return
            }/*else if(!isFirstRelevantTabEntry ){
                        Log.d("Reentry", "Reentry")
                        Handler(Looper.getMainLooper()).postDelayed({
                                triggerBackCamera()

                        }, 60*1000)
                }*/
            if (!isInVideoTab) {
                if (!lastTabRelevant && currentTabRelevant && coolingPhaseTimer == null) {

                    isInVideoTab = true
                    //     startISTimer()
                    if (timer != null) {
                        stopISTimer()
                    }
                    //  currentSessionId = UUID.randomUUID().toString() // Generate a new unique session ID
                    //   currentSessionId = incrementSessionCounter().toString()
                    //  currentSessionId = "${ScreenCaptureService.sessionCounter}"
                    // startISTimer()
                    //  recordSocialMediaUsage(packageName)
                    if (!getReturningFromCameraFlag()) {
                        Log.d(
                            "AccessCamera",
                            " startingBackCamera for other apps"
                        )
                        Log.d(
                            "CoolingPhase",
                            "Timer starting, $coolingPhaseSeconds"
                        )

                        //   coolingPhaseTimer = Timer()
                        //    coolingPhaseTimer?.schedule(object : TimerTask() {
                        //         override fun run() {

                        // triggerBackCamera()
                        // cameraTriggered = true
                        //  exitedApp=false
                        setupCoolingPhaseTimer_otherApps()

                        //     }
                        //    }, coolingPhaseSeconds)  // 0 delay for immediate start, 1800000 for 30-minute interval

                    }

                    setReturningFromCameraFlag(true)


                }


            }
            //    startISTimer()
            lastTabRelevant = currentTabRelevant

        }

    }
    private fun showMissingPermissionNotification() {
        val intent = Intent(this, Permissions::class.java).apply {
            putExtra("updateButton", true)
            /*    val myTextView: TextView = findViewById(R.id.permissions_header)
                val startB: TextView = findViewById(R.id.startButton)

                // Set new text to the TextView
                myTextView.text = "PLEASE GRANT THE MISSING REQUESTS"
                //  startB.text="Continue Study"
                startstudy.text="Continue Study"
                startstudy.setOnClickListener {
                   // val intent = Intent(this, ContinueStudyPage::class.java)
                    //startActivity(intent)
                    minimizeApp()
                }*/
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )


        val builder = NotificationCompat.Builder(this, "permissions_channel")
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("Permission Required")
            .setContentText("Please grant the app revoked permissions to continue.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(this).notify(1337, builder.build())
        }
    }

    fun hasEndStudyBeenReceived(context: Context): Boolean {
        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("endStudyReceived", false)
    }

    private fun triggerBackCamera(){
        //  cameraTriggered=false
        //   if (!cameraTriggered) {
        //   ScreenCapInt2 =ScreenCaptureService.getStopIntent(context)
        //   sessionManager.incrementBackCameraTriggerCount()
        //   startISTimer()
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        if(!endstudyReached || !hasEndStudyBeenReceived(context)) {
            sessionStarted=true
            val revokePermission = prefs.getBoolean("revokePermission", false)
            if (!revokePermission) {

                /*if (isFirstRelevantTabEntry){
               startActivity(intent)
       }else{
               Log.d("Reentry", "Reentry")
               Handler(Looper.getMainLooper()).postDelayed({
                      startActivity(intent)
               }, 30*1000)

       }*/
                startISTimer()

                Log.d("DebugCameraTrigger", " triggerBackCamera()")

                /**editeddd**/

                ScreenCapInt2 = ScreenCaptureService.getStopIntent(context)
                val intent2 = Intent(context, ScreenCaptureService::class.java)
                stopService(intent2)

                /*   val workerManager = WorkerManager(context)
         workerManager.stopWorker()
         ScreenCaptureWorker.MediaProjectionManagerSingleton.stopProjection()*/

                triggerForExitCam = true
                /*      val intent =
                 Intent(this, BackCameraButton::class.java).apply {
                         putExtra("callingPackageName", packageName)
                         //   putExtra("returnedFromCamera", true)

                         addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                 }
         startActivity(intent)*/
                /*   val intent = Intent(this, CameraViewLibrary::class.java).apply{
                 putExtra("callingPackageName", packageName)
                 addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
         }
         startActivity(intent)*/

                val intent = Intent(this, CombineCameras::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startService(intent)
                //    Permissions.setMyServiceIntent(intent3)
                /*Permissions.getMyServiceIntent()?.let {
                        startForegroundService(it)
                }*/
                val serviceIntent = Permissions.getMyServiceIntent()

                /* if (serviceIntent == null) {
                         if(endstudyReached || hasEndStudyBeenReceived(context)) return

                      /*   // No valid MediaProjection token available.
                         // Launch the activity to request permission immediately.
                         val permissionIntent = Intent(context, ResetScreenCapturePermission::class.java)
                         permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                         context.startActivity(permissionIntent)
                         // Optionally, return or postpone work until the token is obtained.
                         return*/
                         val permissionIntent = Intent(context, Permissions::class.java)
                         permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                         val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

                         context.startActivity(permissionIntent).apply {
                                 sharedPreferences.edit().putString("last_stage", "Stage_3_permMissing").apply()

                         }
                         updatingPermissions=true
                         if(serviceIntent!=null){
                                 serviceIntent.setClass(context, ScreenCaptureService::class.java)?.let {
                                         context.startForegroundService(it)
                                 }

                         }

                 }else{
                        /* Permissions.getMyServiceIntent()?.let {
                                 startForegroundService(it)
                         }*/
                        serviceIntent.setClass(context, ScreenCaptureService::class.java)?.let {
                                 context.startForegroundService(it)
                         }


                 }*/

                if(serviceIntent!=null){
                    serviceIntent.setClass(context, ScreenCaptureService::class.java)?.let {
                        context.startForegroundService(it)
                    }

                }else{
                    if(endstudyReached || hasEndStudyBeenReceived(context)) return
                    val permissionIntent = Intent(context, Permissions::class.java)
                    permissionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)

                    context.startActivity(permissionIntent).apply {
                        sharedPreferences.edit().putString("last_stage", "Stage_3_permMissing").apply()

                    }
                    updatingPermissions=true
                    if(currentTabRelevant){
                        Log.d("granting permissions & currenttavrelevant", "$currentTabRelevant")
                        serviceIntent?.setClass(context, ScreenCaptureService::class.java)?.let {
                            context.startForegroundService(it)
                        }
                    }
                }


                /*  if (intent3 != null) {
                 if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                         startForegroundService(intent3)
                 } else {
                         startService(intent3)
                 }
         } else {
                 Log.e("Screen ServiceStart", "Service intent is null. Ensure it is initialized correctly.")
         }*/
                /*   val intent3 = Intent(this, ScreenCaptureService::class.java)
         startService(intent3)*/

                Log.d("CombineCamera", "started service")
                /*  val intent = Intent(this, BuiltInBackCamera::class.java)

         startService(intent)*/

                //  cameraTriggered = true
                //  }
                //     cameraTriggered=false
                BCinstance.triggered = false
                exitedApp = false
                /** TODO. EDIT AND DEBUG THIS FOR FACEBOOK; COULD BE CAUSING TROUBLE **/
                /* if(isFirstRelevantTabEntry && currentForegroundApp=="com.facebook.katana" && currentTabRelevant){
         startService(intent)
          }*/

            } else {
                /**TODO : add when the end study reciever is here then dont do anything aka odnt trigger anything**/
                //if permissions got revoked do nothing aka dont trigger anything


            }
        }else if(endstudyReached || hasEndStudyBeenReceived(context)){
            return
        }

    }
    private fun notRelevantTabDeactivation(contentDesc: String) {
        //    stopISTimer()
        if (!sessionStarted) {
            Log.d("Notrelevanttab", "No session started, so no usage will be recorded.")
            return
        }

        val rootNode = rootInActiveWindow
        if (lastUsedPackage!="com.facebook.katana" && rootNode != null && rootNodeHasComments(rootNode)) {
            Log.d("Notrelevanttab", "Detected comments section, keeping tab relevant.")
            currentTabRelevant=true
            return // Prevent deactivation if in the comments section
        }
        //  stoppedInfScrollinf=getCurrentDateTime().toString()
        timerExceeded=true
        stopISTimer()
        recordSocialMediaUsage(lastUsedPackage.toString())

        Log.d("Notrelevanttab", "Deactivating tab, contentDesc: $contentDesc")
        /**edited**/

        ScreenCapInt2 = ScreenCaptureService.getStopIntent(context)
        val intent = Intent(context, ScreenCaptureService::class.java)
        stopService(intent)

        val intent2 = Intent(context, CombineCameras::class.java)
        stopService(intent2)
        /*val workerManager = WorkerManager(context)
        workerManager.stopWorker()
        ScreenCaptureWorker.MediaProjectionManagerSingleton.stopProjection()*/
        exitTimer?.cancel() // Cancel any existing timer
        exitTimer = Timer()
        exitTimer?.schedule(object : TimerTask() {
            override fun run() {
                exitedForFourMinutes = true
                Log.d("Notrelevanttab", "User exited for 4 minutes, preparing to upload.")
                //  uploadFirebase= true
                triggerUploadIfImageCaptured()
            }
        }, 240 * 1000)




        //    context.startService(ScreenCapInt2)
        //      stopService(ScreenCapInt2)
        //    if (Build.VERSION.SDK_INT > 31) {
        //      stopService(ScreenCapInt2)
        //     }
        currentTabRelevant = false
        lastTabRelevant = currentTabRelevant
        //  recordSocialMediaUsage(lastUsedPackage?:"unknown")
        if (isInVideoTab && getReturningFromCameraFlag()) {
            Log.d("AccessCamera", " notrelevanttabdeac ")

            setReturningFromCameraFlag(false)  // Reset the flag after launching other services
        }
        if (isInVideoTab && timerExceeded) {
            //  if(!currentTabRelevant){

            stoppedInfScrollinf = getCurrentDateTime().toString()
            /**edited**/
            //   recordSocialMediaUsage(lastUsedPackage ?: "unknown")
            //    }

        }

        if (isInVideoTab || cameraTriggered) {
            //    stopISTimer()
            isInVideoTab = false
            cameraTriggered = false
            Log.d("ExitiUnrelevant", "Exiting not relevant tab, now in $contentDesc")
        }

        sessionStarted = false

    }



    @SuppressLint("SuspiciousIndentation")
    private fun relevantTabActivationInsta(contentDesc: String) {
        Log.d("AccessServiceInsta", "Activating relevant tab for $packageName , returnedfrom camera : $isReturningFromCamera")
        //   startISTimer()
        val permissionsgranted = PermissionsUtils.areAllPermissionsGranted(context)

        if(!permissionsgranted){
            showMissingPermissionNotification()
        }else {
            currentTabRelevant = true
            exitTimer?.cancel()
            exitedForFourMinutes = false
            //   if (isFirstRelevantTabEntry && currentTabRelevant) {
            //            cameraTriggered = false  // Ensure camera can trigger after re-entry
            //    }

            //  startISTimer()

            if (timerEndedWithoutCameraTrigger && currentTabRelevant) {
                triggerBackCamera()
                timerEndedWithoutCameraTrigger =
                    false  // Reset the flag after triggering the camera
                return
            }
            if (isFirstRelevantTabEntry && currentTabRelevant) {
                Log.d(
                    "CamTrigger",
                    " in the if for isfirstrelevant and trigerring backcamera"
                )

                triggerBackCamera()
                isFirstRelevantTabEntry = false  // Reset the first-time flag
                setupCoolingPhaseTimer()
                return
            }
            if (exitedApp == true && currentTabRelevant && triggerForExitCam) {

                Log.d("CamTrigger", " in the if and trigerring backcamera")
                triggerBackCamera()

            }
            if (!cameraTriggered || !lastTabRelevant || (lastTabRelevant && lastContentDescription != contentDesc) && coolingPhaseTimer == null) {
                Log.d("AccessServiceInsta", "Camera activation conditions met.")
                //    startISTimer()
                cameraTriggered = true
                exitedApp = false
                setupCoolingPhaseTimer()


            }
            //  val rootNode = rootInActiveWindow ?: return
            /* if (rootNode != null && rootNodeHasComments(rootNode)) {
            Log.d("AccessComment", "Comment")
            currentTabRelevant = true
    } else {
          //  currentTabRelevant = false
            Log.d("AccessComment", "Not Comment")

    }*/
            //     startISTimer()
            // Update lastTabRelevant and lastContentDescription regardless of condition
            lastTabRelevant = currentTabRelevant
            lastContentDescription = contentDesc
        }
    }
    fun rootNodeHasComments(rootNode: AccessibilityNodeInfo): Boolean {
        // Traverse children to detect comment-specific nodes
        if (rootNode.childCount == 0) return false
        for (i in 0 until rootNode.childCount) {
            val child = rootNode.getChild(i) ?: continue
            if (child.text?.toString()?.contains("Comments", ignoreCase = true) == true ||
                child.contentDescription?.toString()?.contains("Comments", ignoreCase = true) == true) {
                return true
            }
            if (rootNodeHasComments(child)) {
                return true
            }
        }
        return false
    }
    /*   private fun clearCapturedImagePath(context: Context) {
               val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
               sharedPref.edit().remove(CAPTURED_IMAGE_PATH_KEY).apply()
       }*/

    /* private fun getCapturedImagePath(context: Context): String? {
             val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
             return sharedPref.getString(CAPTURED_IMAGE_PATH_KEY, null) // Return the saved path or null if not found
     }*/
    private fun getCapturedImagePath(context: Context): Set<String>? {
        val sharedPref = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        return sharedPref.getStringSet(CAPTURED_IMAGE_PATH_KEY, null)
    }


    private fun triggerUploadIfImageCaptured() {
        Log.d("UploadFromTemp", "triggerupload Accessibilty")
        //       val imagePath = getCapturedImagePath(context)
        //     if (imagePath != null && exitedForFourMinutes) {
        //           imagePath.forEach { imagePath ->
        //                 val imageFile = File(imagePath!!)
        //               if (imageFile.exists()) {
        Log.d(
            "AccessService",
            "Calling the upload broadcast"
        )
        /*  Handler(Looper.getMainLooper()).post {
        BackCameraButton.uploadImageToFirebaseRules(context, imageFile)
}*/
        /*   val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
sharedPreferences.edit()
        .putBoolean("triggerUpload", true)
        .apply()*/

        /* BackCameraButton().uploadImageToFirebaseRules(

         null,
         imageFile
 )*/
        val intent =
            Intent("com.example.cameraswitch.UPLOAD_TRIGGER")
        context.sendBroadcast(intent)
        Log.d("UploadFromTemp", "uploadToFirebaseRules Access done")
        /*   } else {
                   Log.e(
                           "AccessService",
                           "Image file does not exist: $imagePath"
                   )
           }*/
        //   }
        //  }
    }



    private fun setupCoolingPhaseTimer() {
        Log.d("coolingphasetimer", "starting")
        coolingPhaseTimer = Timer()
        coolingPhaseTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (currentTabRelevant ) {

                    triggerBackCamera()
                    triggerForExitCam=true
                    exitedApp=false
                    setReturningFromCameraFlag(true)


                } else {
                    if(!exitedApp) {
                        Log.d(
                            "Timer",
                            "Timer ended but not in a relevant tab, camera not triggered."
                        )
                        timerEndedWithoutCameraTrigger = true
                    }

                }


                cameraTriggered = false
                coolingPhaseTimer?.cancel()
                coolingPhaseTimer = null
            }
        }, coolingPhaseSeconds)

    }

    private fun setupCoolingPhaseTimer_otherApps() {
        Log.d("coolingphasetimer", "starting")
        coolingPhaseTimer = Timer()
        coolingPhaseTimer?.schedule(object : TimerTask() {
            override fun run() {
                if (currentTabRelevant) {
                    triggerBackCamera()
                    triggerForExitCam=true
                    exitedApp=false




                } else {
                    Log.d("Timer", "Timer ended but not in a relevant tab, camera not triggered.")
                    timerEndedWithoutCameraTrigger = true
                }
                cameraTriggered = false
                coolingPhaseTimer?.cancel()
                coolingPhaseTimer = null
            }
        }, coolingPhaseSeconds)

    }

    //this one works amazing but for 1xflow
    /*  private fun relevantTabActivationInsta(contentDesc: String) {
              Log.d("AccessServiceInsta", "Activating relevant tab for Instagram. Current tab: $contentDesc")
              currentTabRelevant = true

              // Trigger camera only if it has not been triggered yet for the current tab or if switching between relevant tabs
              if (!cameraTriggered || lastContentDescription != contentDesc) {
                      if (!getReturningFromCameraFlag()) {
                              Log.d("AccessServiceInsta", "Camera activation conditions met.")
                              triggerCamera()
                              cameraTriggered = true  // Set the flag as camera has been triggered
                      } else {
                              // Reset the flag if we are not actually returning from the camera but moving between tabs
                              setReturningFromCameraFlag(false)
                      }
              }

              // Update lastTabRelevant and lastContentDescription regardless of condition
              lastTabRelevant = currentTabRelevant
              lastContentDescription = contentDesc
      }

      private fun triggerCamera() {
              Log.d("AccessCamera", "Starting back camera")
              val intent = Intent(this, BackCameraButton::class.java).apply {
                      putExtra("callingPackageName", packageName)
                      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              startActivity(intent)
              // Reset the flag after camera is triggered
              setReturningFromCameraFlag(false)
      }
*/


    private fun extractActiveTabContentDescription(node: AccessibilityNodeInfo): String {
        val builder = StringBuilder()
        traverseViewHierarchyForActiveTab(node, builder)
        return builder.toString()
    }

    private fun traverseViewHierarchyForActiveTab(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.contentDescription?.let {
            if (isTabActive(node)) {
                builder.append(it.toString()).append(" ")
            }
        }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { childNode ->
                traverseViewHierarchyForActiveTab(childNode, builder)
                childNode.recycle()
            }
        }
    }

    private fun isTabActive(node: AccessibilityNodeInfo): Boolean {
        // This method should implement logic to determine if a tab is active.
        // It can be based on a specific view ID, text, or content description that is unique to the active tab.
        // For example:
        return node.isSelected || node.isAccessibilityFocused
    }
    /*      private fun generateSessionId(): String {
                  incrementSessionCounter() // Update the session counter first
                  val currentSessionNumber: Int = getSessionCounter() // Retrieve the updated counter
                  return "session_$currentSessionNumber"
          }
          private fun getSessionCounter(): Int {
                  val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                  return prefs.getInt("sessionCounter", 0) // Default to 0 if not found
          }

          private fun incrementSessionCounter() {
                  val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                  var currentCounter = prefs.getInt("sessionCounter", 0)
                  currentCounter++ // Increment the counter
                  val editor = prefs.edit()
                  editor.putInt("sessionCounter", currentCounter).apply()
                  editor.apply()

          }*/
    @Synchronized
    private fun incrementSessionCounter(): Int {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        try {
            var currentCounter = prefs.getInt("sessionCounter", 0)
            //  var currentCounter = "${ScreenCaptureService.sessionCounter}"
            currentCounter++
            val success = prefs.edit().putInt("sessionCounter", currentCounter).commit()
            if (!success) {
                Log.e("SessionCounter", "Failed to commit session counter")
                // Handle the error appropriately
            }
            return currentCounter
        } catch (e: Exception) {
            Log.e("SessionCounter", "Error accessing shared preferences", e)
            // Handle exception appropriately
            return -1 // Consider appropriate error handling or fallback strategy
        }
    }


    private fun startISTimer() {
        //    currentSessionId = generateSessionId()
        if(!currentTabRelevant || exitedApp){ /**OR exited**/
            return

        }
        isSessionDataUploaded = false
        timer?.cancel()

        Log.d("Timer", "Timer started in startIS")
        timer = Timer()
        startedInfScrollinf = getCurrentDateTime().toString()
        Log.d("Timer", "Timer started in startIS : $startedInfScrollinf")

        //   timerExceeded = false
        /*   timer?.schedule(object : TimerTask() {
                   override fun run() {
                           Log.d("AccessService_Timer", "IS Timer ended.")
                           timerExceeded = true
                    //      if(!currentTabRelevant) {


                                   stoppedInfScrollinf = getCurrentDateTime().toString()
                                //   recordSocialMediaUsage(lastUsedPackage ?: "unknown")
                         //  }
                   }
           }, ISTimer)*/
        /*    if(!currentTabRelevant || exitedApp){ /**OR exited**/
                    Log.d("Timer", "(in startIS)Timer ended because of tab not relevant or app exit")
                //   stoppedInfScrollinf=getCurrentDateTime().toString()
                    timerExceeded=true
                    stopISTimer()
                 //   stoppedInfScrollinf=getCurrentDateTime().toString()
                   // recordSocialMediaUsage(lastUsedPackage?: "unknown")

            }*/

    }

    private fun stopISTimer() {
        stoppedInfScrollinf=getCurrentDateTime()
        Log.d("AccessService_Timer", "stop IS Timer, stoppedTime : $stoppedInfScrollinf")

        timer?.cancel()
        timer = null
        timerExceeded = false
    }
    /*    fun getFirstLaunchTimestampFormatted(context: Context): String {
                val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0L)
                if (firstLaunchTimestamp == 0L) {
                        return "Not set" // Handle the case where timestamp is not set
                }

                // Format the timestamp
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                return formatter.format(Date(firstLaunchTimestamp))
        }*/
    private fun parseDateTime(dateTimeStr: String): Date? {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return try {
            formatter.parse(dateTimeStr)
        } catch (e: Exception) {
            null
        }
    }
    private fun recordDuration(start: String, end: String) : String {
        val startDate = parseDateTime(start)
        val endDate = parseDateTime(end)
        if (startDate != null && endDate != null) {
            duration = endDate.time - startDate.time
            Log.d("Time ScrollDuration", "Duration: $duration ms")

        }
        return duration.toString()

    }
    private fun isFlightModeOn(): Boolean {
        return Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }
    private fun getBatteryPercentage(): Int {
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level == -1 || scale == -1) -1 else (level * 100) / scale
    }
    @SuppressLint("ServiceCast")
    private fun isWifiConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    }

    private fun isMobileDataConnected(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
        return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    }
    private fun isPhonePluggedIn(): Boolean {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatus: Intent? = registerReceiver(null, intentFilter)
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
        val chargePlug = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val isPluggedIn = chargePlug == BatteryManager.BATTERY_PLUGGED_USB ||
                chargePlug == BatteryManager.BATTERY_PLUGGED_AC

        Log.d("AccessService", "Phone Charging: $isCharging, Plugged In: $isPluggedIn")
        return isCharging || isPluggedIn
    }
    private fun recordSocialMediaUsage(packageName: String) {
        Log.d("FirebaseVar", "saving in accessservice")
        if(endstudyReached || hasEndStudyBeenReceived(context)) return
        if (isSessionDataUploaded) return
        //  if(isSessionDataUploaded) sessionManager.
        val chargingStatus = if (isPhonePluggedIn()) "isCharging" else "notCharging"

        // Firebase document data
        val chargingData = hashMapOf(
            "chargingStatus" to chargingStatus

        )



        //   val sessionCounter = sessionManager.incrementSessionCounter()
        val sessionCounter=sessionManager.getSessionCounter()
        val flightModeStatus = isFlightModeOn()
        val batteryPercentage = getBatteryPercentage()
        val wifiStatus = isWifiConnected()
        val mobileDataStatus = isMobileDataConnected()
        val sanitizedPackageName = packageName.replace(".", "_")
        val timestamp = System.currentTimeMillis()
        // val database = FirebaseDatabase.getInstance().reference
        val currentDay = "Day_" + getCurrentDay(context)
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val participantId = prefs.getString("PID", "")
        if (participantId.isNullOrEmpty()) {
            Log.e("Firestore", "Participant ID is null or empty Access Service")
            return // Exit if the participant ID is missing
        }
        val front=false
        val back=false
        val screen=false
        // val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)
        //    val firstLaunchTimestamp_formatted=getFirstLaunchTimestampFormatted(this)
        //val sessionCounter = "${ScreenCaptureService.sessionCounter}"
        //   val sessionFolder = "session_${ScreenCaptureService.sessionCounter}"
        val participantsRef = db.collection("participants2")
        val counterRef = db.collection("metadata").document("participantCounter")
        val androidId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        //  database.child("userActivity").child(sanitizedPackageName).child(currentSessionId)
        //  database.child("userActivity").child(sanitizedPackageName).child(sessionCounter)
        /**edited**/
        /* if(BackCameraButton.backcamerapicTaken && HideCamera.frontCamerapictureTaken && ScreenCaptureService.screenshotsTaken){
                  allpicturesTaken =true
         }*/
        prefs.getBoolean("front",front)
        prefs.getBoolean("back",back)
        prefs.getBoolean("screen",screen)
        Log.d("SavingVariables", "front:$front,back:$back,screen: $screen")
        if(front&&back&&screen){
            Log.d("SavingVariables", "all true")

            allpicturesTaken =true
        }
        //   val duraationn= recordDuration(startedInfScrollinf,stoppedInfScrollinf)
        val duraationn= recordDuration(startedInfScrollinf,stoppedInfScrollinf)

        Log.d("Timer", "saving startedTimerat : $startedInfScrollinf, stoppedAt: $stoppedInfScrollinf for package: $packageName")
        val userData = hashMapOf(
            // "joinedAt" to firstLaunchTimestamp_formatted,
            "androidID" to androidId,
            "pID" to participantId,
            "Infinite Scrolling App" to packageName,
            "Started Infinite Scrolling at " to startedInfScrollinf,
            "Ended Infinite Scrolling at" to stoppedInfScrollinf,
            "Duration of Infinite Scrolling" to duraationn+"ms",
            //  "Pictures taken" to allpicturesTaken,
            "Flight Mode" to flightModeStatus,
            "Battery Percentage" to batteryPercentage,
            "WiFi Connected" to wifiStatus,
            "Mobile Data Connected" to mobileDataStatus,
            "Phone pluggged In" to chargingData
        )
        db.collection("participants2").document(participantId!!)
            .collection(currentDay).document("session_$sessionCounter")
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "Infinite Scrolling Time and App saved")
                isSessionDataUploaded = true
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }
    }

    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val currentDate = Date()
        return formatter.format(currentDate)
    }
    /*  public fun getCurrentDay(): Int {
              val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
              val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)
              if (firstLaunchTimestamp == 0L) {
                      return 1 // Just in case it's not set
              }
              val currentTime = System.currentTimeMillis()
              val difference = currentTime - firstLaunchTimestamp

              //  the number of days passed since the first launch
              val days = (difference / (24 * 60 * 60 * 1000)).toInt()
              return days + 1 // Day count starts at 1
      }*/


    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AccessService", "Service connected and state reset.")
        /*   permissionUpdatedReceiver = object : BroadcastReceiver() {
                   override fun onReceive(context: Context?, intent: Intent?) {
                           val serviceIntent = Permissions.getMyServiceIntent()
                           if (serviceIntent != null && updatingPermissions) {
                                   // Start the service
                                   serviceIntent.setClass(
                                           this@AccessService,
                                           ScreenCaptureService::class.java
                                   )?.let {
                                           startForegroundService(it)
                                   }
                                   updatingPermissions=false
                           }
                          /* if (intent?.action == "com.example.app.PermissionUpdated" ) {
                                   val serviceIntent = Permissions.getMyServiceIntent()
                                   serviceIntent?.setClass(
                                           this@AccessService,
                                           ScreenCaptureService::class.java
                                   )?.let {
                                           startForegroundService(it)
                                   }

                           }*/
                   }
           }
         registerReceiver(permissionUpdatedReceiver)*/

        endstudyReeceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.app.Endstudy" ) {
                    Log.d("EndStudy", "In the accessservice afte receiving")

                    MainActivity.triggerUpload(context!!)
                    WorkManager.getInstance(context).cancelUniqueWork("PermissionCheckWork")

                    endstudyReached=true
                    val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
                    prefs.edit().putBoolean("endStudyReceived", true).apply()
                }
            }
        }
        val filter = IntentFilter("com.example.app.Endstudy")

        registerReceiver(endstudyReeceiver,filter, RECEIVER_EXPORTED)

        batteryLowReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.app.ACTION_BATTERY_LOW" ) {
                    isCriticalBatteryLow = true

                }
            }
        }
        val filter2 = IntentFilter("com.example.app.ACTION_BATTERY_LOW")
        registerReceiver(batteryLowReceiver, filter2)
        //  setReturningFromCameraFlag(false)
    }
    /*   override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
               if (intent != null) {
                       val resultData = intent.getStringExtra("resultData")
                       val resultCode = intent.getIntExtra(
                               "resultCode",
                               404
                       )
                       Log.d("ScreenCaptureService", " Data sent to AS, result code : $resultCode, data: ${resultData}")
                       val resultDataUri = Intent.parseUri(resultData, Intent.URI_INTENT_SCHEME)

                       val extras = resultDataUri.extras
                       if (extras!= null) {
                               for (key in extras.keySet()) {
                                       Log.d("ScreenCaptureService", "Extra $key = ${extras.get(key)}")
                               }
                       }
                       // Use resultData and resultCode as needed
               }else{
                       Log.d("ScreenCaptureService", " Data sent to AS intent empty")

               }
               return START_STICKY
       }*/
    private fun sendEndStudyBroadcast(context: Context?) {
        val intent = Intent("com.example.app.Endstudy") // Custom action
        context?.sendBroadcast(intent) // Sending broadcast
    }




    fun isSevenDaysPassed(context: Context): Boolean {
        Log.d("EndStudy", "isSevendayspassed")

        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)

        if (firstLaunchTimestamp == 0L) {
            Log.d("EndStudy", "First launch time is not set")

            return false // First launch time is not set
        }
        val sevenDaysInMillis= 3 * 60 * 1000L
        val targetTime = firstLaunchTimestamp + sevenDaysInMillis
        val currentTime = System.currentTimeMillis()

        return currentTime >= targetTime // If current time is past the target time
    }
    override fun onInterrupt() {
        // Handle service interruption
        coolingPhaseTimer?.cancel()
        coolingPhaseTimer = null
        Log.d("AccessService", "Service interrupted, timer cancelled")
    }


    override fun onDestroy() {
        super.onDestroy()
        timer?.cancel()
        coolingPhaseTimer?.cancel() // Cancel the timer to clean up resources
        coolingPhaseTimer = null
//                unregisterReceiver(batteryLowReceiver)
        unregisterReceiver(endstudyReeceiver)
        unregisterReceiver(GlobalBroadcastReceiver())
        stopISTimer()
        recordSocialMediaUsage(lastUsedPackage.toString())

    }


}

