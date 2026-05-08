package com.example.cameraswitch


import com.example.cameraswitch.Workers.DayChecker
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.cameraswitch.PagesandActivites.Permissions
import com.example.cameraswitch.PagesandActivites.PidPage
import com.example.cameraswitch.PagesandActivites.StartStudyPage
import com.example.cameraswitch.Receivers.NetworkChangeReceiver
import com.example.cameraswitch.Service.AccessService
import com.example.cameraswitch.UtilsAndCons.PermissionsUtils
import com.example.cameraswitch.UtilsAndCons.SharedPreferencesUtils
import com.example.cameraswitch.Workers.EndStudyWorker
import com.example.cameraswitch.Workers.PermissionCheckWorker
import com.example.cameraswitch.Workers.UploadToFirebaseWorker
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private lateinit var welcomebutton : Button
    private lateinit var storageRef: StorageReference
    private val db = FirebaseFirestore.getInstance()
    var isStartStudyClicked:Boolean=false
    private lateinit var endstudyReeceiver: BroadcastReceiver


    private lateinit var viewModel: MainViewModel
     val NOTIFICATION_CHANNEL_ID = "1"
        val NOTIFICATION_CHANNEL_NAME = "start_channel"
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!isDeviceSupported()) {

            showUnsupportedDeviceDialog()
            return
        }
        setContentView(R.layout.welcomepage)




        val networkReceiver = NetworkChangeReceiver()
        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)

        registerReceiver(networkReceiver, intentFilter,Context.RECEIVER_NOT_EXPORTED)

        FirebaseApp.initializeApp(this)
        WorkManager.getInstance(applicationContext).getWorkInfosByTag("ScreenCaptureWorker").get()
            .forEach { workInfo ->
                Log.d("WorkerManagerScreenshots", "Worker State: ${workInfo.state}")
            }
       // schedulePermissionCheck(this)
        schedulePermissionCheckWorker()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
                )
            } else {
                showStartNotification()
            }
        } else {
            showStartNotification()
        }

        createNotificationChannel()

        val savedStateHandle = SavedStateHandle()
        welcomebutton = findViewById(R.id.get_started_button)
        welcomebutton.setOnClickListener {
            // Intent to open SecondActivity
            val intent = Intent(this, PidPage::class.java)
            startActivity(intent)
        }

        //    Log.e("ScreenCapture", "Screen capture permission : ${hasScreenCapturePermission()}")

        val storage = FirebaseStorage.getInstance()
        storageRef = storage.reference
      //  storeInitialTimestamp()
  /**edited**/
        //  scheduleDayCheckWorker()
       // val factory = MainViewModelFactory(savedStateHandle)
        //viewModel = ViewModelProvider(this, factory).get(MainViewModel::class.java)
       // viewModel = ViewModelProvider(this, ViewModelProvider.NewInstanceFactory()).get(MainViewModel::class.java)

      //  viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
     //   recordSocialMediaUsage()
        // Restore last saved stage from ViewModel
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastStage = sharedPreferences.getString("last_stage", null)
        if (lastStage != null) {
         //   viewModel.saveCurrentStage(lastStage)
            navigateToStage(lastStage)
        }
        if ( lastStage=="Stage_2_startedsuccess" && !PermissionsUtils.areAllPermissionsGranted(this)) {
            Log.d("showmissingperm","test")
           // getNotification(this)
            if(isStartStudyClicked){  showMissingPermissionNotification() }
            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("last_stage", "Stage_3_permMissing")
            editor.apply()
            navigateToStage(lastStage!!)
            /*  val intent = Intent(this, Permissions::class.java)
              startActivity(intent)*/
        }
        //getNotification(this)
        isStartStudyClicked = sharedPreferences.getBoolean("isStartStudyClicked", false)


        endstudyReeceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == "com.example.app.Endstudy" ) {

                    triggerUpload(context!!)
                    WorkManager.getInstance(context).cancelUniqueWork("PermissionCheckWork")
                    Handler(Looper.getMainLooper()).post {
                        setContentView(R.layout.endstudy)
                    }

                }
            }
        }
        val filter = IntentFilter("com.example.app.Endstudy")
        registerReceiver(endstudyReeceiver, filter, RECEIVER_NOT_EXPORTED)

        val prefs = applicationContext.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
      /*  val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)
        if(firstLaunchTimestamp != 0L){
            if (isSevenDaysPassed(applicationContext)) {

                sendEndStudyBroadcast(applicationContext)
            }
        }*/
       /* val sevendayspassed= isSevenDaysPassed(applicationContext)
        val workRequest = OneTimeWorkRequestBuilder<EndStudyWorker>()
           // .setInitialDelay(7, TimeUnit.DAYS)  // Change to 3 minutes for testing
            .setInitialDelay(sevendayspassed, TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(applicationContext).enqueue(workRequest)*/


    }
   /* fun schedulePermissionCheck(context: Context) {
        val workRequest = OneTimeWorkRequestBuilder<PermissionCheckWorker>()
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
    }*/
   private fun sendEndStudyBroadcast(context: Context?) {
       val intent = Intent("com.example.app.Endstudy") // Custom action
       context?.sendBroadcast(intent) // Sending broadcast
   }




    fun isSevenDaysPassed(context: Context): Long {
        Log.d("EndStudy", "isSevendayspassed")

        val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0)
        Log.d("EndStudy", "firstlaunchtime : $firstLaunchTimestamp")

        if (firstLaunchTimestamp == 0L) {
            Log.d("EndStudy", "First launch time is not set")

            return 0 // First launch time is not set
        }
        val sevenDaysInMillis= 3 * 60 * 1000L
        val targetTime = firstLaunchTimestamp + sevenDaysInMillis
        val currentTime = System.currentTimeMillis()

        return targetTime
     //   return currentTime >= targetTime // If current time is past the target time
    }
    private fun isDeviceSupported(): Boolean {
        val currentSdkVersion = Build.VERSION.SDK_INT
        return currentSdkVersion >= MIN_SUPPORTED_SDK_VERSION && currentSdkVersion <= MAX_SUPPORTED_SDK_VERSION
    }

    private fun showUnsupportedDeviceDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsupported Device")
            .setMessage("This app is not compatible with your device's Android version. In order to get compensated please download and use this application only on an Android version 11/12/ 13")
            .setPositiveButton("OK") { _, _ ->
                terminateApp() // Terminate the app when the user presses OK
            }
            .setCancelable(false) // Prevent the user from dismissing the dialog
            .show()
    }

    private fun terminateApp() {
      //  val notsupported = "AndroidNotSupported"
        val versionRelease: String = Build.VERSION.RELEASE
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
        // Exit the app
        val userData = hashMapOf(
            //     "achieved80percent" to percenatgeComplete,
            "AndroidNotSupported" to versionRelease

        )
      /*  val participantId = SharedPreferencesUtils.getParticipantId(applicationContext!!)

        db.collection("participants").document(participantId!!)
            .collection(notsupported).document("finishedStudySuccessfully")
            .set(userData)
            .addOnSuccessListener {
                Log.d("Firestore", "Completed firebase saving")

            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error writing document", e)
            }*/
        finishAffinity() // Close all activities
        android.os.Process.killProcess(android.os.Process.myPid()) // Terminate the app process
    }
   private fun schedulePermissionCheckWorker() {
       val workRequest = PeriodicWorkRequestBuilder<PermissionCheckWorker>(15, TimeUnit.MINUTES)
           .build()

       WorkManager.getInstance(this).enqueueUniquePeriodicWork(
           "PermissionCheckWork",
           ExistingPeriodicWorkPolicy.REPLACE,
           workRequest
       )
   }
   override fun onRequestPermissionsResult(
       requestCode: Int, permissions: Array<String>, grantResults: IntArray
   ) {
       super.onRequestPermissionsResult(requestCode, permissions, grantResults)
       if (requestCode == 1) {
           if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
               showStartNotification()
           }
       }
   }

   private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "start_channel", // Use this ID everywhere
                "Permission Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Channel for permission notifications"

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }


    private fun navigateToStage(stage: String) {
        when (stage) {
            "Stage_1_pid" -> {
                if (this !is MainActivity) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                }
            }
            "Stage_2_startedsuccess" -> {
                // Save current stage first
             //   viewModel.saveCurrentStage("Stage_2_startedsuccess")
                val intent = Intent(this, StartStudyPage::class.java)
                startActivity(intent)
            }
            "Stage_3_permMissing" -> {
                // Save current stage first
            //    viewModel.saveCurrentStage("Stage_3_permMissing")
                val intent = Intent(this, Permissions::class.java)
                startActivity(intent)
            }
            else -> {
                Toast.makeText(this, "Unknown stage", Toast.LENGTH_SHORT).show()
            //    val intent = Intent(this, StartStudyPage::class.java)
             //   startActivity(intent)
                finish()
            }
        }
    }



    /*  private fun isSeventhDay(context: Context): Boolean {
          val prefs = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
          val firstLaunchTimestamp = prefs.getLong("firstLaunchTimestamp", 0L)

          if (firstLaunchTimestamp == 0L) return false

          // Calculate the difference in days between now and the first launch
          val diffInMillis = System.currentTimeMillis() - firstLaunchTimestamp
          val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

          return diffInDays.toInt() == 7
      }*/

    private fun scheduleDayCheckWorker() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Only schedule the worker if the variable hasn't already been saved
        if (!prefs.getBoolean("variableSaved", false)) {
            val workRequest = OneTimeWorkRequestBuilder<DayChecker>()
                .setInitialDelay(8, TimeUnit.DAYS)  // Run after 7 days
                .build()

            WorkManager.getInstance(this).enqueue(workRequest)
            Log.d("MainActivity", "DayCheckWorker scheduled to run after 7 days.")
        } else {
            Log.d("MainActivity", "Variable already saved. No need to schedule worker.")
        }
    }
    private fun storeInitialTimestamp() {
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        if (!prefs.contains("firstLaunchTimestamp")) {
            val editor = prefs.edit()
            editor.putLong("firstLaunchTimestamp", System.currentTimeMillis())
            editor.apply()
        }
    }
    private fun getPermissionsActivityPendingIntent(context: Context): PendingIntent {
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("last_stage", "Stage_3_permMissing")

        // Create an Intent for the Permissions activity
        val intent = Intent(context, Permissions::class.java)
        intent.putExtra("updateButtonContinue", true)
        // Wrap the Intent in a PendingIntent
        return PendingIntent.getActivity(
            context,
            0, // Request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE // Flags
        )
    }
    private fun showStartNotification() {
        if (!isDeviceSupported()) {
            return
        }
        val builder = NotificationCompat.Builder(this, "start_channel") // Use consistent channel ID
            .setSmallIcon(R.drawable.logo) // Ensure this drawable exists
            .setContentTitle("FeedTrail")
            .setContentText("Study Running.Thank you for participating in our study!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            return
        }
        NotificationManagerCompat.from(this).notify(1337, builder.build())
    }

    private fun showMissingPermissionNotification() {
        val pendingIntent = getPermissionsActivityPendingIntent(applicationContext)

        val builder = NotificationCompat.Builder(this, "start_channel")
            .setSmallIcon(R.drawable.logo) // Replace with your app's icon
            .setContentTitle("Permission Required")
            .setContentText("Please grant the app revoked permissions to continue the study!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            //ACTIVITY_Main change button
        val startB: TextView = findViewById(R.id.startButton)
        startB.text="Continue Study"
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
    }



 override fun onDestroy() {
   super.onDestroy()
  // unregisterReceiver(closeReceiver)
     //unregisterReceiver(network)
   //  unregisterReceiver(endstudyReeceiver)
    // unregisterReceiver(NetworkChangeReceiver())
     unregisterReceiver(endstudyReeceiver)
}

    override fun onResume() {
        super.onResume()
        showStartNotification()

       // val currentStage = viewModel.getCurrentStage()
      //  if (currentStage != null) {
        //    navigateToStage(currentStage)
        //}
         var  SKIP_LAST_STAGE_UPDATE = "SKIP_LAST_STAGE_UPDATE"

        val skipLastStage = intent.getBooleanExtra(SKIP_LAST_STAGE_UPDATE, false)
        if (skipLastStage) {
            // Skip restoring the last stage and proceed normally
            Log.d("MainActivity", "Skipping last stage update")
            return
        }
        val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val lastStage = sharedPreferences.getString("last_stage", null)
       /* if (lastStage=="Stage_2_startedsuccess"){
            showMissingPermissionNotification()

        }*/
        // If there is a saved stage, navigate to it
        if (lastStage != null) {
            navigateToStage(lastStage)
        }
        if (lastStage=="Stage_2_startedsuccess" && !PermissionsUtils.areAllPermissionsGranted(this)) {
           // getNotification(this)
            if(isStartStudyClicked){  showMissingPermissionNotification() }

            val sharedPreferences = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()
            editor.putString("last_stage", "Stage_3_permMissing")
            editor.apply()
            navigateToStage(lastStage)

        }
    }

    companion object {

            const val MIN_SUPPORTED_SDK_VERSION = 30 // Android 11
            const val MAX_SUPPORTED_SDK_VERSION = 33 // Android 13 (Tiramisu)


        fun triggerUpload(context: Context) {
            if (isWifiConnected(context) || NetworkChangeReceiver.wifi) {
                triggerUploadWorker(context)

            }

        }

        fun triggerUploadWorker(context: Context) {
            val workRequest = OneTimeWorkRequestBuilder<UploadToFirebaseWorker>().build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }

        fun isMobileDataConnected(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        }

        fun isWifiConnected(context: Context): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        }
    }

}
