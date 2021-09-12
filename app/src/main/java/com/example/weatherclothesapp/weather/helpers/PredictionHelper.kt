package com.example.weatherclothesapp.weather.helpers

import com.example.weatherclothesapp.weather.simple_objects.PredictionModel
import com.example.weatherclothesapp.weather.simple_objects.WeatherModel

class PredictionHelper {

    fun getPrediction(
        weatherModel: WeatherModel
    ): PredictionModel {
        return PredictionModel(
            when {
                weatherModel.forecast.averageTemp < 5 -> {
                    "It's cold"
                }
                weatherModel.forecast.averageTemp.toInt() in 5..15 -> {
                    "It's ok"
                }
                else -> {
                    "It's warm"
                }
            }
        )
    }
}
