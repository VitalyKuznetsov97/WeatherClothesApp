package com.example.weatherclothesapp.weather.helpers

import com.example.weatherclothesapp.weather.simple_objects.local.PredictionModel
import com.example.weatherclothesapp.weather.simple_objects.local.WeatherModel
import java.util.*

class PredictionHelper {

    fun createPrediction(
        newId: Int,
        weatherModel: WeatherModel
    ): PredictionModel {
        return PredictionModel(
            id = newId,
            timestamp = Date().time,
            text = getText(weatherModel),
        )
    }

    private fun getText(weatherModel: WeatherModel): String {
        return when {
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
    }
}
