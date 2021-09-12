package com.example.weatherclothesapp.weather

import android.Manifest

object Constants{

    //SaveInstance keys
    const val TEXT_KEY = "TEXT_KEY"
    const val LOCATION_KEY = "LOCATION_KEY"
    const val EXPECTED_INFO_TYPE_KEY = "EXPECTED_INFO_TYPE_KEY"
    const val IS_LOADING_KEY = "IS_LOADING_KEY"
    const val IS_SHOWING_RATIONALE_KEY = "IS_SHOWING_RATIONALE_KEY"

    //Strings
    const val LOCATION_PERMISSION_RATIONALE_TEXT =
        "Please give us permission to know your location."
    const val LOCATION_PERMISSION_DENIED_TEXT =
        "Sorry. We need your permission for location to work."
    const val LAST_LOCATION_ERROR = "Couldn't get your last location."
    const val CURRENT_LOCATION_ERROR = "Couldn't get your current location."
    const val WEATHER_FORECAST_ERROR = "Failed to get a forecast."

    //Permission name
    const val LOCATION_PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION

}