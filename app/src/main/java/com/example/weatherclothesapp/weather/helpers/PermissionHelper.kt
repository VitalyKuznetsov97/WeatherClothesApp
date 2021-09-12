package com.example.weatherclothesapp.weather.helpers

import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import com.example.weatherclothesapp.weather.Constants

class PermissionHelper(
    private val activityResultLauncher: ActivityResultLauncher<String>
) {

    fun hasLocationPermission(activity: Activity): Boolean {
        val permissionState = ContextCompat.checkSelfPermission(
            activity,
            Constants.LOCATION_PERMISSION_STRING
        )
        return (permissionState == PackageManager.PERMISSION_GRANTED)
    }

    fun shouldShowLocationPermissionRationale(activity: Activity): Boolean {
        return activity.shouldShowRequestPermissionRationale(Constants.LOCATION_PERMISSION_STRING)
    }

    fun askLocationPermission() {
        activityResultLauncher.launch(Constants.LOCATION_PERMISSION_STRING)
    }

}