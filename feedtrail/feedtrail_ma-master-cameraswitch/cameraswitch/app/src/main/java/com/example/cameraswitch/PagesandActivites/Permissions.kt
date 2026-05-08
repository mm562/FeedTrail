package com.example.cameraswitch.PagesandActivites

import android.Manifest
import android.app.Activity
import android.app.AppOpsManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcel
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.lifecycle.lifecycleScope
import com.example.cameraswitch.Capturing.ScreenCaptureService
import com.example.cameraswitch.R
import com.example.cameraswitch.UtilsAndCons.PermissionsUtils
import com.example.cameraswitch.Workers.WorkerManagerScreenshots
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream


class Permissions : AppCompatActivity() {
    private val PERMISSION_ACCESSIBILITY_REQUEST_CODE = 103
    private val PERMISSION_CAMERA_REQUEST_CODE = 105
    private val PERMISSION_SCREENSHOT_CODE = 104
    private val PERMISSION_STORAGE_REQUEST_CODE = 107
    private val LOCATION_REQUEST_CODE = 108
    private val PERMISSION_NOTIFICATION_REQUEST_CODE = 109
    private val PERMISSION_USAGE_STATS_REQUEST_CODE = 110
    var  access_clicked=false
    var  camera_clicked=false
    var screenshots_clicked=false
    var storage_clicked=false
    var location_clicked=false
    var notification_clicked=false
    var usagestats_clicked=false
    private lateinit var accessibility: Button
    private lateinit var camera: Button
    private lateinit var screenshots: Button
    private lateinit var storage: Button
    private lateinit var location : Button
    private lateinit var usagestats : Button
    private lateinit var notification: Button
    private lateinit var startstudy:Button
    private lateinit var closeReceiver: BroadcastReceiver
    private var mediaProjectionResultCode:Int = 0
    private var mediaProjectionCaptureIntent: Intent? = null


    companion object {
        const val ACTION_OPEN = "com.example.new_app.ACTION_OPEN"
        const val PREFS_NAME = "ScreenCapturePrefs"
        private const val KEY_SCREEN_CAPTURE_PERMISSION = "screen_capture_permission"
        private const val KEY_SCREEN_CAPTURE_DATA_URI = "screen_capture_data_uri"
        private var ScreenCapInt: Intent? = null
        private var lastStage: String? = null
        private var isStartStudyClicked: Boolean = false
        private var data1 = "DATA"
        var savedscreencapturedata=false

        private var screenshotPermission: Intent? = null
        private var mediaProjection: MediaProjection? = null
        private var mediaProjectionManager: MediaProjectionManager? = null
        var results: Pair<Int, Intent> = Pair(Activity.RESULT_CANCELED, Intent())

             fun getMyServiceIntent(): Intent? = ScreenCapInt
      /*  fun getMyServiceIntent(context: Context): Intent? {
            if (ScreenCapInt == null) {
                val (resultCode, dataResult) = results
                Log.d("ScreenCap", "Screencap is null in permission, results gotten : ${results.toString()}")
                ScreenCapInt =
                    ScreenCaptureService.getStartIntent(context, resultCode, dataResult)
             //   val (savedResultCode, savedData) = saveScreenCaptureData(context)
             //   val dataFromFile= getIntentFromFile(context)
                // data1 = data2.toString()
              //  if (savedResultCode == Activity.RESULT_OK && dataFromFile != null) {
                  /*  ScreenCapInt =
                        ScreenCaptureService.getStartIntent(context, resultCode, dataResult)*/
              //  }
            }
            return ScreenCapInt
        }*/

       /* fun getIntentFromFile(context: Context): Intent? {
            try {
                val file = File(context.filesDir, "screen_capture_intent.ser")
                if (file.exists()) {
                    ObjectInputStream(FileInputStream(file)).use { it ->
                        val intent = it.readObject() as? Intent  // Deserialize the Intent
                        if (intent != null) {
                            Log.d("ScreenCap", "Intent restored successfully.")
                        }
                        return intent
                    }
                } else {
                    Log.e("ScreenCap", "Saved Intent file does not exist.")
                }
            } catch (e: Exception) {
                Log.e("ScreenCap", "Failed to retrieve Intent: ${e.message}")
            }
            return null
        }*/

        fun setMyServiceIntent(intent: Intent) {
            ScreenCapInt = intent
        }

        /* fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
            val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putInt("resultCode", resultCode)
                val intentBytes = data.toUri(Intent.URI_INTENT_SCHEME).toByteArray(Charsets.UTF_8)
                val intentEncoded = android.util.Base64.encodeToString(intentBytes, android.util.Base64.DEFAULT)
             //   putString("data", data.toUri(Intent.URI_ANDROID_APP_SCHEME)) // Convert Intent to URI
                putString("data", intentEncoded)
               // putString("data", data.toString())

                apply()
            }
            Log.d("ScreenCap", "savscreencapdata Permissions : resultcode : $resultCode, data: $data")

        }
        fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
            val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
          //  val dataUri = sharedPref.getString("data", null)
            val encodedIntent = sharedPref.getString("data", null)
            val intentBytes = android.util.Base64.decode(encodedIntent, android.util.Base64.DEFAULT)
            val intentString = String(intentBytes, Charsets.UTF_8)
            val data = Intent.parseUri(intentString, Intent.URI_INTENT_SCHEME)
            Log.d("ScreenCap", "getting capdata : resultcode : $resultCode, data: $data")

          /*  val data = if (dataUri != null) Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME) else null*/
            return Pair(resultCode, data)
        }*/
        /*   fun getScreenCaptureData(context: Context): androidx.core.util.Pair<Int, Intent?> {
            val sharedPref = context.getSharedPreferences("Prefs", Context.MODE_PRIVATE)
            val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
            val dataUri = sharedPref.getString("data", null)
            val data = if (dataUri != null) Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME) else null
            return androidx.core.util.Pair(resultCode, data)
        }*/
        /*  fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
            val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)

            // Retrieve the stored Base64-encoded Intent string
            val encodedIntent = sharedPref.getString("data", null)
            val data: Intent? = if (encodedIntent != null) {
                try {
                    val intentBytes = android.util.Base64.decode(encodedIntent, android.util.Base64.DEFAULT)
                    val intentString = String(intentBytes, Charsets.UTF_8)
                    Intent.parseUri(intentString, Intent.URI_INTENT_SCHEME)
                } catch (e: Exception) {
                    Log.e("ScreenCap", "Failed to decode stored Intent: ${e.message}")
                    null
                }
            } else {
                null
            }

            Log.d("ScreenCap", "Restored screen capture data: resultCode: $resultCode, data: $data")
            return Pair(resultCode, data)
        }*/

        /*   fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
            val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            sharedPref.edit().apply {
                putInt("resultCode", resultCode)

                // Convert the Intent to a Base64-encoded string
                val intentBytes = data.toUri(Intent.URI_INTENT_SCHEME).toByteArray(Charsets.UTF_8)
                val intentEncoded = android.util.Base64.encodeToString(intentBytes, android.util.Base64.DEFAULT)
                putString("data", intentEncoded)

                apply()
            }
            Log.d("ScreenCap", "Saved screen capture data: resultCode: $resultCode, encoded intent.")
        }*/
        /*   fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
            val sharedPref =
                context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()

            // Convert Intent to a String URI
            val dataUri = data.toUri(Intent.URI_INTENT_SCHEME)

            editor.putInt("resultCode", resultCode)
            editor.putString("data", dataUri) // Store URI instead of serializing
            editor.apply()

            Log.d(
                "ScreenCap",
                "Saved screen capture data: resultCode: $resultCode, intent stored as URI."
            )
        }


        fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
            val sharedPref =
                context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
            val dataUri = sharedPref.getString("data", null)

            if (dataUri == null) {
                Log.e("ScreenCap", "No saved intent found")
                return Pair(resultCode, null)
            }

            return try {
                val restoredIntent = Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME)

                if (restoredIntent != null) {
                    Log.d("ScreenCap", "Restored valid screen capture Intent.")
                    Pair(resultCode, restoredIntent)
                } else {
                    Log.e("ScreenCap", "Failed to restore Intent from URI.")
                    Pair(resultCode, null)
                }

            } catch (e: Exception) {
                Log.e("ScreenCap", "Error restoring screen capture Intent: ${e.message}", e)
                Pair(resultCode, null)
            }
        }*/



        fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent): Pair<Int,Intent> {
            val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
            val editor = sharedPref.edit()

            // Save the result code and action
            editor.putInt("resultCode", resultCode)
            //  editor.putString("data", data.action) // Save the action of the Intent
            editor.putString("data", data.toString()) // Save the action of the Intent




            // Save the extras as a bundle (if any)
            val bundle = data.extras
            if (bundle != null) {
                val bundleString = bundle.toString() // You could convert the bundle to a String or save individual items
                editor.putString("extras", bundleString)
            }

            // Apply changes to SharedPreferences
            editor.apply()

            Log.d("ScreenCap", "Saved screen capture data: resultCode: $resultCode, action: ${data.action}, data: ${data.toString()}")

            savedscreencapturedata=true
            return Pair(resultCode,data)
        }
        fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
            val sharedPref =
                context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)

            // Retrieve the saved resultCode and action
            val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
            val action = sharedPref.getString("action", null)

            if (action == null) {
                Log.e("ScreenCap", "No saved intent action found")
                return Pair(resultCode, null)
            }

            // Create a new intent with the saved action
            val intent = Intent(action)

            // Retrieve and add the extras, if any
            val extrasString = sharedPref.getString("extras", null)
            if (extrasString != null) {
                // Assuming the extras are stored as a string, you would need to parse and reconstruct the Bundle here
                val bundle = Bundle()
                bundle.putString("data", extrasString) // Add the necessary data to the bundle
                intent.putExtras(bundle)
            }

            Log.d("ScreenCap", "Restored valid screen capture Intent.")
            return Pair(resultCode, intent)
        }
    }
        /* override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.example.ACTION_CLOSE_MAIN")
        registerReceiver(closeReceiver, filter)
    }*/
   lateinit var startB: TextView


    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        startB=findViewById(R.id.startButton)
        startstudy = findViewById(R.id.start_study_button)

        intent?.let {
            if (it.getBooleanExtra("updateButton", false)) {
              //  val startB: TextView = findViewById(R.id.startButton)
              //  startB.text = "Continue Study"
                startstudy.text="Continue Study"
            }
        }
        FirebaseApp.initializeApp(this)
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
         lastStage = sharedPreferences.getString("last_stage", null)
        if ( lastStage =="Stage_3_permMissing"){
            val myTextView: TextView = findViewById(R.id.permissions_header)

            // Set new text to the TextView
            myTextView.text = "PLEASE GRANT THE MISSING REQUESTS"

            startstudy.text="Continue Study"
           // startB.text="Continue Study"
            startstudy.setOnClickListener {
                val intent = Intent(this, ContinueStudyPage::class.java)
                startActivity(intent)
            }
        }
        onBackPressedDispatcher.addCallback(this) {
            // Do nothing or handle the back button press in some way
            // This prevents the default back behavior for api >33
        }
         isStartStudyClicked = sharedPreferences.getBoolean("isStartStudyClicked", false)

        fun onBackPressed() {
            // Leave this empty to disable the back button for api <  33
        }


        //    Log.e("ScreenCapture", "Screen capture permission : ${hasScreenCapturePermission()}")
     //   storeInitialTimestamp()

       /* if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }*/



        closeReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == "com.example.ACTION_CLOSE_MAIN") {
                    //finish()
                    (context as? Activity)?.finishAffinity() //to close the whole app not just activity
                }
            }
        }

        // Register the receiver
        val filter = IntentFilter("com.example.ACTION_CLOSE_MAIN")
        registerReceiver(closeReceiver, filter, RECEIVER_NOT_EXPORTED)

        notification = findViewById(R.id.notifications)

        notification.setOnClickListener {
            if (!areNotificationsEnabled()) {
                requestNotificationPermission()
            }
        }

        usagestats = findViewById(R.id.usagepermission)
        usagestats.setOnClickListener {
            if (!hasUsageStatsPermission()) {
                requestUsageStatsPermission()
            }
        }
        accessibility = findViewById(R.id.access_button)
        accessibility.setOnClickListener {

            Log.e("Permissions", "i am in clicklistener access")
            val requestedPermissions = arrayOf(

                Manifest.permission.BIND_ACCESSIBILITY_SERVICE

            )
            if (openAccessibilitySettings()) {
                requestPermissions(requestedPermissions, PERMISSION_ACCESSIBILITY_REQUEST_CODE)

            }

        }

        camera = findViewById(R.id.camera_button)
        camera.setOnClickListener {

            Log.e("Permissions", "i am in clicklistener camera")
        /*    val requestedPermissions = arrayOf(

                Manifest.permission.CAMERA

            )

            if (openCameraSettings()) {
                requestPermissions(requestedPermissions, PERMISSION_CAMERA_REQUEST_CODE)

            }*/
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                PERMISSION_CAMERA_REQUEST_CODE
            )

        }
        screenshots = findViewById(R.id.startButton)
        screenshots.setOnClickListener {
        /*    if (Build.VERSION.SDK_INT > Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION),
                        PERMISSION_SCREENSHOT_CODE
                    )
                } else {
                    startProjection() // Permission already granted
                }
            } else {
                startProjection() // Not required for older versions
            }*/
            startProjection()
        }
        //  startProjection()

        storage = findViewById(R.id.storage)
        storage.setOnClickListener {
            Log.e("Permissions", "i am in clicklistener storage")
            val requestedPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO
                )
            } else {
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            }
            requestPermissions(requestedPermissions, PERMISSION_STORAGE_REQUEST_CODE)
        }

        location = findViewById(R.id.location)

        location.setOnClickListener {
            var shouldUpdateState = false
            if (!isLocationEnabled()) {
                openLocationSettings()
                shouldUpdateState = true
            }
            if (!hasLocationPermission()) {
                val requestedPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                requestPermissions(requestedPermissions, LOCATION_REQUEST_CODE)
                shouldUpdateState = true
            }
            if (shouldUpdateState) {
                updateLocationButtonState()
            }
        }

     //   updateStartStudyButtonState()
        createNotificationChannel()

    }

    private val handler = Handler(Looper.getMainLooper())
    private val permissionCheckRunnable = object : Runnable {
        override fun run() {
            // Check if all permissions are granted
         //   if (!areAllPermissionsGranted() && lastStage=="Stage_2_startedsuccess") {
            if (!areAllPermissionsGranted() && isStartStudyClicked){
                showMissingPermissionNotification()
            }
            // Schedule the next check (5 seconds interval)
            handler.postDelayed(this, 5000)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "permissions_channel",
                "Permission Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifies users when a permission is missing"

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_NOTIFICATION_REQUEST_CODE)
    }

    private fun areNotificationsEnabled(): Boolean {
        val notificationManager = NotificationManagerCompat.from(this)
        return notificationManager.areNotificationsEnabled()
    }

    private fun requestUsageStatsPermission() {
        startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
    }


    private fun updateLocationButtonState() {
        val isGpsEnabled = isLocationEnabled()
        val hasLocationPermission = hasLocationPermission()

        // Enable the button if either GPS is disabled or permission is not granted
        location.isEnabled = !isGpsEnabled || !hasLocationPermission
        location.text = if (isGpsEnabled && hasLocationPermission) "Location Permission set ✓" else "Enable Location Services"
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER)
    }
    private fun openLocationSettings(): Boolean {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        startActivity(intent)
        return true
    }
    private fun requestLocationPermission():Boolean{
        Log.e("Permissions", "i am in requestpermission")
        val locationpermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return locationpermission== PackageManager.PERMISSION_GRANTED


    }
    fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), packageName)
        return mode == AppOpsManager.MODE_ALLOWED
    }
    private fun storeInitialTimestamp() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        if (!prefs.contains("firstLaunchTimestamp")) {
            val editor = prefs.edit()
            editor.putLong("firstLaunchTimestamp", System.currentTimeMillis())
            editor.apply()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //Check if all permissions were granted
        /* if (requestCode == PERMISSION_NOTIFICATION_REQUEST_CODE) {

             // Notifications are enabled for your app
             // You can enable the button or perform other actions here

             if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                 val notificationsEnabled = NotificationManagerCompat.from(this).areNotificationsEnabled()
                 if (notificationsEnabled) {
                     // Notifications are enabled
                     Log.e("Permissions", "Notifications are enabled")
                     notification.isEnabled=false
                     notification.text = "Notification Permission set ✓"
                     notification_clicked=true
                 } else {
                     // Notifications are not enabled
                     Log.e("Permissions", "Notifications are not enabled")
                     notification.isEnabled=true
                     notification.text = "Request Notification Permission"
                     notification_clicked=false
                 }
             } else {
                 // Notification permission denied
                 Log.d("Permissions", "Notification permission denied")
             }
         }else*/ if(requestCode == PERMISSION_ACCESSIBILITY_REQUEST_CODE){

            if (grantResults.isNotEmpty() && grantResults[0] !== PackageManager.PERMISSION_GRANTED) {

                Log.e("Permissions", "i am in OnRequestPermissions")
                if(!requestAccessibilityPermission()){
                    Log.e("Permissions", "Access denied")
                    accessibility.isEnabled=true
                    accessibility.text = "Request Accessibility Permission"
                    access_clicked=false

                    //  requestAccessibilityPermission()


                }else if(requestAccessibilityPermission()){
                    Log.e("Permissions", "Access enabled")
                    accessibility.isEnabled=false
                    accessibility.text = "Accessibility Permission set \u2713"
                    access_clicked=true

                }
                updateStartStudyButtonState()


            }else if(requestCode==PERMISSION_USAGE_STATS_REQUEST_CODE){
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permissions", "Usage stats permission granted")
                    usagestats.isEnabled = false
                    usagestats.text = "Usage Permissions Permission set ✓"

                } else {
                    Log.e("Permissions", "Usage permission denied")
                    usagestats.isEnabled = true
                    usagestats.text = "Usage Permission"
                }
                updateStartStudyButtonState()

            } else if(requestCode==PERMISSION_NOTIFICATION_REQUEST_CODE){
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permissions", "Notification permission granted")
                    notification.isEnabled = false
                    notification.text = "Notification Permission set ✓"

                } else {
                    Log.e("Permissions", "Notification permission denied")
                    notification.isEnabled = true
                    notification.text = "Request Notification Permission"
                }

                updateStartStudyButtonState()

            } else if(requestCode == PERMISSION_CAMERA_REQUEST_CODE){
                if (grantResults.isNotEmpty() && grantResults[0] !== PackageManager.PERMISSION_GRANTED) {
                    if(!requestCameraPermission(applicationContext)){
                        Log.e("Permissions", "Access denied")
                        camera.isEnabled=true
                        camera.text = "Request Camera Permission"
                        camera_clicked=false




                    }else if(requestCameraPermission(applicationContext)){
                        Log.e("Permissions", "Access enabled")
                        camera.isEnabled=false
                        camera.text = "Camera Permission set \u2713"
                        camera_clicked=true

                    }
                }
                updateStartStudyButtonState()

            }else if(requestCode == PERMISSION_STORAGE_REQUEST_CODE){
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permissions", "Storage permission granted")
                    storage.isEnabled = false
                    storage.text = "Storage Permission set ✓"
                    storage_clicked=true
                    updateStartStudyButtonState()

                } else {
                    Log.e("Permissions", "Storage permission denied")
                    storage.isEnabled = true
                    storage.text = "Request Storage Permission"
                    storage_clicked=false
                }
                updateStartStudyButtonState()

            }else if(requestCode == LOCATION_REQUEST_CODE){

                updateLocationButtonState()
                updateStartStudyButtonState()


            }else if (requestCode == PERMISSION_SCREENSHOT_CODE) {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("Permissions", "Screenshot permission granted")
                    screenshots.isEnabled = false
                    screenshots.text = "Screenshot Permission set ✓"
                    screenshots_clicked = true

                } else {
                    Log.e("Permissions", "Screenshot permission denied")
                    screenshots.isEnabled = true
                    screenshots.text = "Request Capture Permission"
                    screenshots_clicked = false
                }

                updateStartStudyButtonState()



            /*else if(requestCode==PERMISSION_SCREENSHOT_CODE){
                           if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                               Log.e("Permissions", "Screenshots permission granted")
                               screenshots.isEnabled = false
                               screenshots.text = "Capture Permission set ✓"
                               screenshots_clicked=true
                           }else{
                               Log.e("Permissions", "Capture permission denied")
                               screenshots.isEnabled = true
                               screenshots.text = "Request Storage Permission"
                               screenshots_clicked=false
                           }
                        } */
            }else{

                Log.e("Permissions", "Access permission denied")
                accessibility.isEnabled=true
                accessibility.text = "Request Accessibility Permissions"
                access_clicked=false

                camera.isEnabled=true
                camera.text = "Request Camera Permissions"
                camera_clicked=false

                screenshots.isEnabled=true
                screenshots.text="Request Capture Permissions"
                screenshots_clicked=false

                location.isEnabled=true
                location.text="Request Location Permissions"
                location_clicked=false

                notification.isEnabled=true
                notification.text="Request Notification Permissions"
                notification_clicked=false

                usagestats.isEnabled=true
                usagestats.text="Request Usage Permissions"
                usagestats_clicked=false



            }

        }/*else if(requestCode == PERMISSION_BATTERY_OPTIMIZATION_REQUEST_CODE) {
       if (grantResults.isNotEmpty() && grantResults[0] !== PackageManager.PERMISSION_GRANTED) {
           Log.e("Permissions", "i am in OnRequestPermissions")
           if(!isBatteryOptimizationGranted()){
               Log.e("Permissions", "Battery denied")
               batteryOptimization.isEnabled=true
               batteryOptimization.text = "Request Battery Permission"
           }else if(isBatteryOptimizationGranted()){
               Log.e("Permissions", "Battery enabled")
               batteryOptimization.isEnabled=false
               batteryOptimization.text = "Battery Permission set ✓"
           }
       } else {
           Log.e("Permissions", "Battery permission denied")
           batteryOptimization.isEnabled=true
           batteryOptimization.text = "Request Battery Permission"
       }
   }*/
      /*  if(!notification.isEnabled && !usagestats.isEnabled && !accessibility.isEnabled && !camera.isEnabled&& !screenshots.isEnabled&& !storage.isEnabled &&!location.isEnabled)
    {
        Log.d(
            "StartStudy",
            "${notification.isEnabled} ${usagestats.isEnabled}, ${accessibility.isEnabled} ${screenshots.isEnabled} ${storage.isEnabled} ${location.isEnabled}"
        )
        startstudy.isEnabled = true
        startstudy.setOnClickListener {

            val intent = Intent(this, StartStudyPage::class.java)
            startActivity(intent)
        }
    }*/
     //   updateStartStudyButtonState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PERMISSION_SCREENSHOT_CODE) {
            if (resultCode == RESULT_OK && data!=null) {
                screenshots.isEnabled = false
                screenshots.text = "Capture Permission set ✓"
                screenshots_clicked=true
                val sharedPreferences =  getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putBoolean("scsGrantd", true)
                PermissionsUtils.updateScreenCapturePermission(this, true)
                //   editor.putString(requestCode.toString(), "no req code")
                /*  startService(
                      ScreenCaptureService.getStartIntent(
                          this,
                          resultCode,
                          data
                      )
                  )*/
                // val dataUri = data.toUri(Intent.URI_INTENT_SCHEME)
                //  val dataUri = data.data?.toString() ?: "No Data URI"

                val dataUri = data.toString() ?: "No Data URI"
                //  editor.putString("resultData", dataUri)
                //  editor.putInt("resultCode", resultCode)
                //  editor.apply()

                val extras = data.extras
                if (extras!= null) {
                    for (key in extras.keySet()) {
                        Log.d("ScreenCaptureService", "Extra $key = ${extras.get(key)}")
                    }
                }

                //  startService(ScreenCaptureService.getStartIntent(this, resultCode, dataUri));
              /*  if(!savedscreencapturedata){
                    Log.d("Screencap", "saving capture data")
                    results =saveScreenCaptureData(applicationContext,resultCode,data)
                    savedscreencapturedata=true

                } else {
                    Log.d("ScreenCap", "Results already set, skipping save.")
                }*/

               // setScreenshotPermission(data.clone() as Intent)  // Save the cloned Intent to use for MediaProjection
               // getScreenshotPermission()
               saveScreenCaptureData(applicationContext,resultCode,data)
             /*   screenshotPermission?.let {
                    saveScreenCaptureData(applicationContext,resultCode,
                        it
                    )
                }*/

                // Store the service intent globally for later use
             //   setMyServiceIntent(ScreenCaptureService.getStartIntent(this, resultCode, data))
                /**edited**/
               // ScreenCapInt = ScreenCaptureService.getStartIntent(this, resultCode, data)
                mediaProjectionResultCode = resultCode
                 mediaProjectionCaptureIntent = data
                if (resultCode == RESULT_OK && data != null) {
                    ScreenCapInt = ScreenCaptureService.getStartIntent(this, resultCode, data)
                } else {
                    Log.e("Permissions", "Screen capture permission not granted. Cannot start service.")
                    setScreenshotPermission(null)
                }

                /*  val workerManager = WorkerManagerScreenshots(applicationContext)
                  workerManager.prepareWorker(resultCode,data)
                  Log.d("DebugWorker", "prepareWorker called with resultCode: $resultCode and data: ${data.toString()}")

                storeResultData(resultCode,data)*/

               /* if(!notification.isEnabled && !usagestats.isEnabled && !accessibility.isEnabled && !camera.isEnabled&& !screenshots.isEnabled&& !storage.isEnabled &&!location.isEnabled) {
                    Log.d(
                        "StartStudy",
                        "${notification.isEnabled} ${usagestats.isEnabled}, ${accessibility.isEnabled} screenshots : ${screenshots.isEnabled} ${storage.isEnabled} ${location.isEnabled}"
                    )
                    startstudy.isEnabled = true
                    startstudy.setOnClickListener {

                        val intent = Intent(this, StartStudyPage::class.java)
                        startActivity(intent)
                    }
                }*/
                updateStartStudyButtonState()
            }

        }else if (resultCode == RESULT_CANCELED || resultCode != RESULT_OK) {
            // Handle the cancel case
            PermissionsUtils.updateScreenCapturePermission(this, false)

           // Toast.makeText(this, "Screen capture canceled", Toast.LENGTH_SHORT).show()
            screenshots.isEnabled=true
            screenshots_clicked=false
        }
       /* if(!notification.isEnabled && !usagestats.isEnabled && !accessibility.isEnabled && !camera.isEnabled&& !screenshots.isEnabled&& !storage.isEnabled &&!location.isEnabled) {
            Log.d(
                "StartStudy",
                "${notification.isEnabled} ${usagestats.isEnabled}, ${accessibility.isEnabled} screenshots : ${screenshots.isEnabled} ${storage.isEnabled} ${location.isEnabled}"
            )
            startstudy.isEnabled = true
            startstudy.setOnClickListener {

                val intent = Intent(this, StartStudyPage::class.java)
                startActivity(intent)
            }
        }*/

    }
/*    fun saveScreenCaptureData(context: Context, resultCode: Int, data: Intent) {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        sharedPref.edit().apply {
            putInt("resultCode", resultCode)
            putString("data", data.toUri(Intent.URI_INTENT_SCHEME)) // Convert Intent to URI
            apply()
        }
    }
    fun getScreenCaptureData(context: Context): Pair<Int, Intent?> {
        val sharedPref = context.getSharedPreferences("ScreenCapturePrefs", Context.MODE_PRIVATE)
        val resultCode = sharedPref.getInt("resultCode", Activity.RESULT_CANCELED)
        val dataUri = sharedPref.getString("data", null)
        val data = if (dataUri != null) Intent.parseUri(dataUri, Intent.URI_INTENT_SCHEME) else null
        return Pair(resultCode, data)
    }*/
    private fun storeResultData(resultCode: Int, data: Intent) {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(KEY_SCREEN_CAPTURE_PERMISSION, resultCode)
        editor.putString(KEY_SCREEN_CAPTURE_DATA_URI, data.toString())
        editor.apply()
        Log.d("DebugWorker", "storing result, result : $resultCode, data: $data")
    //    val workerManager = WorkerManagerScreenshots(applicationContext)
      //  workerManager.prepareWorker(resultCode,data)
    }
   /* override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)


        // Save MediaProjection data (resultCode and captureIntent details)
        outState.putInt("result_code", mediaProjectionResultCode)

        mediaProjectionCaptureIntent?.let {
            outState.putString("capture_intent_action", it.action)
            val extras = it.extras
            if (extras != null) {
                // Store any necessary extras, e.g., key-value pairs (you can adjust this)
                for (key in extras.keySet()) {
                    outState.putString("extra_$key", extras.getString(key)) // Save relevant extras
                }
            }
        }
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        mediaProjectionResultCode = savedInstanceState.getInt("result_code", Activity.RESULT_CANCELED)

        // Reconstruct the captureIntent if it's saved
        val action = savedInstanceState.getString("capture_intent_action", null)
        if (action != null) {
            val captureIntent = Intent(action)
            val extras = Bundle()

            // Restore any extras saved for the intent
            val allKeys = savedInstanceState.keySet()
            for (key in allKeys) {
                if (key.startsWith("extra_")) {
                    val extraKey = key.removePrefix("extra_")
                    extras.putString(extraKey, savedInstanceState.getString(key))
                }
            }
            captureIntent.putExtras(extras)
            mediaProjectionCaptureIntent = captureIntent
        }
    }*/

     fun startProjection(): Boolean {
        /*val mProjectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(),
            PERMISSION_SCREENSHOT_CODE
        )*/

        val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val captureIntent = mProjectionManager.createScreenCaptureIntent()
        startActivityForResult(captureIntent, PERMISSION_SCREENSHOT_CODE)


        return true
    }

    /********From here new edit*************/
 /*   fun startProjection(){
        getScreenshotPermission()
    }*/
    private fun getScreenshotPermission() {
        try {
            if (hasScreenshotPermission2()) {
                if (mediaProjection != null) {
                    mediaProjection!!.stop()
                    mediaProjection = null
                }
                // Clone the Intent to avoid consuming it
                mediaProjection = mediaProjectionManager?.getMediaProjection(Activity.RESULT_OK,
                    screenshotPermission!!.clone() as Intent
                )
            } else {
                val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                val captureIntent = mProjectionManager.createScreenCaptureIntent()
                startActivityForResult(captureIntent, PERMISSION_SCREENSHOT_CODE)  // Request permission if not granted
            }
        } catch (e: RuntimeException) {
            val mProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            val captureIntent = mProjectionManager.createScreenCaptureIntent()
            startActivityForResult(captureIntent, PERMISSION_SCREENSHOT_CODE)  // Handle permission request failure
        }
    }

    protected fun setScreenshotPermission(permissionIntent: Intent?) {
        screenshotPermission = permissionIntent
    }

    private fun hasScreenshotPermission2(): Boolean {
        // Check your saved preferences or other logic to determine if permission is granted
        return screenshotPermission != null
    }


    /*************new edit end********/




  /*  private fun hasScreenCapturePermission(): Boolean {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.contains(PERMISSION_SCREENSHOT_CODE.toString())
    }*/
    private fun hasScreenCapturePermission(): Boolean {
        // Return true only if both the SharedPreferences flag exists and your token is not null.
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return prefs.contains(PERMISSION_SCREENSHOT_CODE.toString()) && ScreenCapInt != null
    }



    /*  fun hasScreenCapturePermission(context: Context): Boolean {
          val permissions = arrayOf(
              Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,
            //  Manifest.permission.MANAGE_MEDIA_PROJECTION
          )

          val missingPermissions = permissions.any {
              ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
          }

          if (missingPermissions) {
              if (context is Activity) {
                  ActivityCompat.requestPermissions(
                      context,
                      permissions,
  PERMISSION_SCREENSHOT_CODE                )
              }
              return false
          }
          return true
      }*/

    private fun openAccessibilitySettings():Boolean {

        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
        /*  if(requestAccessibilityPermission()){
              Log.e("Permissions", "Access are enabled")
              accessibility.isEnabled=false
              access_clicked=true
          }*/
        return true
    }

    private fun openCameraSettings(): Boolean {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
        return true
    }

    private fun isAccessibilityPermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val permissionStatus = packageManager.checkPermission(packageName, Manifest.permission.BIND_ACCESSIBILITY_SERVICE)
            return permissionStatus == PackageManager.PERMISSION_GRANTED



        }
        return true
    }
 /*   private fun areAllPermissionsGranted(): Boolean {
//    val batteryOptimizationGranted = isBatteryOptimizationGranted()
//    val notificationGranted = isNotificationPermissionGranted()

        var allpermissionsgranted =false

        if(!notification.isEnabled && !usagestats.isEnabled && !accessibility.isEnabled && !camera.isEnabled&& !screenshots.isEnabled&& !storage.isEnabled &&!location.isEnabled){
          allpermissionsgranted=true
        }else{
            allpermissionsgranted=false
        }
        //  val appearGranted = isAppearOnTopPermissionGranted() // Pass the context here

//   return batteryOptimizationGranted && notificationGranted && accessibilityGranted && appearGranted
        return allpermissionsgranted
    }*/
 private fun updateStartStudyButtonState() {
     if (areAllPermissionsGranted()) {
         startstudy.isEnabled = true
        Log.d("StartStudyy", "all granted")
         startstudy.setOnClickListener {
             val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
             val editor = sharedPreferences.edit()
             editor.putBoolean("isStartStudyClicked", true)
             editor.apply()

             val intent = Intent(this, StartStudyPage::class.java)
             startActivity(intent)
         }
     }else{
         Log.d("StartStudyy", "all not yet granted")

         startstudy.isEnabled = false
      //   startstudy.text = "Please Grant All Permissions"
     }
 }

    private fun areAllPermissionsGranted(): Boolean {
     return requestAccessibilityPermission() &&
             requestCameraPermission(applicationContext) &&
             areStoragePermissionsGranted(applicationContext) &&
            // !screenshots.isEnabled &&
             //PermissionsUtils.hasScreenCapturePermission(applicationContext)&&
             hasScreenCapturePermission()&&
             areNotificationsEnabled() &&
             hasUsageStatsPermission() &&
             isLocationEnabled() &&
             hasLocationPermission()
 }

    private fun requestAccessibilityPermission() : Boolean{


        Log.e("Permissions", "i am in requestpermission")

        val enabledServices =
            Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        val packageName = packageName
        return enabledServices?.contains(packageName) == true



    }
    fun requestCameraPermission(applicationContext: Context): Boolean {
        val cameraPermission = ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
        return cameraPermission == PackageManager.PERMISSION_GRANTED
    }

   /* private fun requestCameraPermission(applicationContext: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.FOREGROUND_SERVICE_CAMERA)
            } else {
                arrayOf(Manifest.permission.CAMERA)
            }

            // Check if any of the required permissions are missing
            val missingPermissions = permissions.any {
                ContextCompat.checkSelfPermission(applicationContext, it) != PackageManager.PERMISSION_GRANTED
            }

            if (missingPermissions) {
                // Check if context is an instance of Activity before casting
                if (applicationContext is Activity) {
                    ActivityCompat.requestPermissions(
                        applicationContext,
                        permissions,
                        PERMISSION_CAMERA_REQUEST_CODE
                    )
                } else {
                    // If context is not an Activity, return false
                    return false
                }
                return false // Permissions are not granted yet
            }
        }
        // If all permissions are granted
        return true
    }
*/
    private fun areStoragePermissionsGranted(applicationContext: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

    }
    /* private val newAppReceiver = object : BroadcastReceiver() {
       override fun onReceive(context: Context, intent: Intent) {
           if (intent.action == ACTION_OPEN) {

           }
       }
    }*/
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

   /* private fun showMissingPermissionNotification() {
        val builder = NotificationCompat.Builder(this, "permissions_channel")
            .setSmallIcon(R.drawable.logo) // Replace with your app's icon
            .setContentTitle("Permission Required")
            .setContentText("Please grant the app revoked permissions to continue.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        NotificationManagerCompat.from(this).notify(1, builder.build())
    }*/
   fun requestScreenCapturePermission() {
       // Get the MediaProjectionManager system service
       val mediaProjectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

       // Create an Intent to request screen capture permission
       val captureIntent = mediaProjectionManager.createScreenCaptureIntent()

       // Start the activity for result to request the permission
       startActivityForResult(captureIntent, PERMISSION_SCREENSHOT_CODE)
   }

    override fun onResume() {
        super.onResume()





        handler.post(permissionCheckRunnable)
        val sharedPreferences1 = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
         isStartStudyClicked = sharedPreferences1.getBoolean("isStartStudyClicked", false)

        if (isStartStudyClicked) {
            handler.post(permissionCheckRunnable)
        }
        /* if(isAppearOnTopPermissionGranted()){
             appear.isEnabled=false
             appear.text = "Appear-On-Top Permission set ✓"
         }
         if(isBatteryOptimizationGranted()){
             batteryOptimization.isEnabled=false
             batteryOptimization.text = "Battery Permission set ✓"
         }
         if(isNotificationPermissionGranted()){
             notification.isEnabled=false
             notification.text = "Notification Permission set ✓"
         }
         if(isNotificationPermissionGranted_sdk30()){
             notification.isEnabled=false
             notification.text = "Notification Permission set ✓"
         }*/

        if(requestAccessibilityPermission()){
            accessibility.isEnabled=false
            accessibility.text = "Accessibility Permission set ✓"
        }
        if(requestCameraPermission(applicationContext)){
            camera.isEnabled=false
            camera.text = "Camera Permission set ✓"
        }
        if(areStoragePermissionsGranted(applicationContext)){
            storage.isEnabled=false
            storage.text="Storage Permission set ✓"
        }
        if (hasScreenCapturePermission()) {
            screenshots.isEnabled=false
            screenshots.text="ScreenCapture Permission set ✓"
        }else{
            screenshots.isEnabled = true
            screenshots.text = "Request Capture Permission"
        }

        updateLocationButtonState()

        if(areNotificationsEnabled()){
            notification.isEnabled=false
            notification.text="Notification Permission set ✓"
        }
        if(hasUsageStatsPermission()){
            usagestats.isEnabled=false
            usagestats.text="Usage Permission set ✓"
        }
        updateStartStudyButtonState()
        /*  if(startProjection()){
              screenshots.isEnabled=false
              screenshots.text= "Capture Permissions set ✓ "
          }*/
        /*  if(isAppearOnTopPermissionGranted()&&isBatteryOptimizationGranted()&& requestAccessibilityPermission() &&  isNotificationPermissionGranted_sdk30()){
              Log.e("Permissions", "Button activating")

              startButton.isEnabled=true
          }
          Log.e("Permissions", isAppearOnTopPermissionGranted().toString() + requestAccessibilityPermission() + isBatteryOptimizationGranted()+isNotificationPermissionGranted_sdk30())
        */
       /* if (!areAllPermissionsGranted()) {
            showMissingPermissionNotification()
        }*/






        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastStage = sharedPreferences.getString("last_stage", null)
        if (lastStage=="Stage_3_permMissing"){
            val myTextView: TextView = findViewById(R.id.permissions_header)
            val startB: TextView = findViewById(R.id.startButton)

            // Set new text to the TextView
            myTextView.text = "PLEASE GRANT THE MISSING REQUESTS"
          //  startB.text="Continue Study"
            startstudy.text="Continue Study"
            startstudy.setOnClickListener {
                val intent = Intent(this, ContinueStudyPage::class.java)
                startActivity(intent)
                minimizeApp()
            }
              // Delay for 2000 milliseconds (2 seconds)

        }


    }
    private fun minimizeApp() {
        lifecycleScope.launch {
        delay(2000)
            moveTaskToBack(true)

        }
    }
  /*  override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.let {
            if (it.getBooleanExtra("updateButton", false)) {
                startB.text = "Continue Study"
            }
        }
    }*/

    object IntentHolder {
        var savedIntent: Intent? = null
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(closeReceiver)
       /* val hasScreenCapturePermission = PermissionsUtils.hasScreenCapturePermission(this)

       if (hasScreenCapturePermission) {
            // Permission was granted previously, continue with the screen capture service
            if (mediaProjectionResultCode == Activity.RESULT_OK && mediaProjectionCaptureIntent != null) {
                // startProjection(mediaProjectionResultCode, mediaProjectionCaptureIntent!!)
                startProjection()
            }
        } else {
            // Screen capture permission was not granted or was revoked, request permission again
            requestScreenCapturePermission()
        }*/
    }



}