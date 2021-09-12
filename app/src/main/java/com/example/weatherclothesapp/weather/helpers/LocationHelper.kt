package com.example.weatherclothesapp.weather.helpers

import android.annotation.SuppressLint
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.tasks.CancellationTokenSource

class LocationHelper(private val fusedLocationClient: FusedLocationProviderClient) {

    @SuppressLint("MissingPermission")
    fun getLastLocation(
        onLocationReceived: (Location?) -> Unit
    ) {
        fusedLocationClient
            .lastLocation
            .addOnSuccessListener {
                onLocationReceived(it)
            }
    }

    /**
     * Works unreliably.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentLocation(
        onLocationReceived: (Location?) -> Unit
    ) {
        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient
            .getCurrentLocation(
                LocationRequest.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource.token
            )
            .addOnCompleteListener {
                cancellationTokenSource.cancel()
                onLocationReceived(it.result)
            }
    }

}
