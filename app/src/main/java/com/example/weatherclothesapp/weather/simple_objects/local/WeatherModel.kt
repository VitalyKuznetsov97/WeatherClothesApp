package com.example.weatherclothesapp.weather.simple_objects.local

class WeatherModel(
    val location: Location,
    val forecast: Forecast
) {

    class Location(
        val localDate: String,
        val localTime: String,
        val country: String,
        val name: String,
        val region: String,
    ) {
        override fun toString(): String {
            return "Location(localDate='$localDate', localTime='$localTime', country='$country', name='$name', region='$region')"
        }
    }

    class Forecast(
        val text: String,
        val icon: String,
        val averageTemp: Double,
        val minTemp: Double,
        val maxTemp: Double,
        val dailyChanceOfRain: Int,
        val dailyChanceOfSnow: Int,
        val maxWindSpeed: Double,
        val totalPrecipitation: Double,
        val uvIndex: Double,
    ) {
        override fun toString(): String {
            return "Forecast(text='$text', icon='$icon', averageTemp=$averageTemp, minTemp=$minTemp, maxTemp=$maxTemp, dailyChanceOfRain=$dailyChanceOfRain, dailyChanceOfSnow=$dailyChanceOfSnow, maxWindSpeed=$maxWindSpeed, totalPrecipitation=$totalPrecipitation, uvIndex=$uvIndex)"
        }
    }

    override fun toString(): String {
        return "WeatherModel(location=$location, forecast=$forecast)"
    }

}
