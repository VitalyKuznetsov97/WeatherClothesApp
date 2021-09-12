package com.example.weatherclothesapp.weather.simple_objects

object WeatherModelMapper {

    fun fromResponseDto(responseDto: ResponseDto): WeatherModel {
        val localDateAndTime = responseDto.location.localtime.split(" ")

        return WeatherModel(
            WeatherModel.Location(
                localDate = localDateAndTime[0],
                localTime = localDateAndTime[1],
                country = responseDto.location.country,
                name = responseDto.location.name,
                region = responseDto.location.region
            ),
            WeatherModel.Forecast(
                text = responseDto.forecast.forecastday.first().day.condition.text,
                icon = responseDto.forecast.forecastday.first().day.condition.icon,
                averageTemp = responseDto.forecast.forecastday.first().day.avgtemp_c,
                minTemp = responseDto.forecast.forecastday.first().day.mintemp_c,
                maxTemp = responseDto.forecast.forecastday.first().day.maxtemp_c,
                dailyChanceOfRain = responseDto.forecast.forecastday.first().day.daily_chance_of_rain,
                dailyChanceOfSnow = responseDto.forecast.forecastday.first().day.daily_chance_of_snow,
                maxWindSpeed = responseDto.forecast.forecastday.first().day.maxwind_kph,
                totalPrecipitation = responseDto.forecast.forecastday.first().day.totalprecip_mm,
                uvIndex = responseDto.forecast.forecastday.first().day.uv,
            )
        )
    }
}