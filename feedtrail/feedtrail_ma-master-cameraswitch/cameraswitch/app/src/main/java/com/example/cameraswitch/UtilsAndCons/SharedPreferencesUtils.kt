package com.example.cameraswitch.UtilsAndCons

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity

object SharedPreferencesUtils {
    fun getCapturedImagePaths(context: Context): Set<String> {
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getStringSet(Constants.CAPTURED_IMAGE_PATH_KEY, emptySet()) ?: emptySet()
    }


    fun clearCapturedImagePath(context: Context) {
        val sharedPref = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().remove(Constants.CAPTURED_IMAGE_PATH_KEY).apply()
    }
    fun getParticipantId(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("AppPrefs",
            AppCompatActivity.MODE_PRIVATE
        )
        return sharedPreferences.getString("PID", "")
    }

    fun getSavedLocation(context: Context): Location? {
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val latitude = sharedPreferences.getString("latitude", null)?.toDoubleOrNull()
        val longitude = sharedPreferences.getString("longitude", null)?.toDoubleOrNull()
        return if (latitude != null && longitude != null) {
            Location(LocationManager.GPS_PROVIDER).apply {
                this.latitude = latitude
                this.longitude = longitude
            }
        } else {
            null
        }
    }

    @SuppressLint("CommitPrefEdits")
    fun savePictureVariables(context: Context, front:Boolean, back:Boolean){
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("front",front)
        sharedPreferences.edit().putBoolean("back",back)
        Log.d("SavingVariablees", "front: $front, back: $back")


    }
    @SuppressLint("CommitPrefEdits")
    fun saveScreenVar(context: Context, screen:Boolean){
        val sharedPreferences = context.getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean("screen",screen)
        Log.d("SavingVariablees", "screenshottaken: $screen")

    }
}