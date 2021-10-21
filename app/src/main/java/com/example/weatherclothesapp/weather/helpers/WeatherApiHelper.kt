package com.example.weatherclothesapp.weather.helpers

import com.example.weatherclothesapp.weather.simple_objects.web.ResponseDto
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

class WeatherApiHelper {

    private interface WeatherApi {
        @GET(
            "forecast.json?" +
                    "key=f5b9913d921b45218f383452210809" +
                    "&days=1" +
                    "&hour=24"
        )
        fun getCurrentDayForecast(@Query("q") location: String): Call<ResponseDto>
    }

    fun makeWeatherRequest(
        location: Pair<Double, Double>,
        onForecastRequestSuccess: (ResponseDto) -> Unit,
        onForecastRequestFailure: () -> Unit
    ) {
        val retrofit = Retrofit
            .Builder()
            .baseUrl("https://api.weatherapi.com/v1/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val weatherApi = retrofit.create(WeatherApi::class.java)

        weatherApi
            .getCurrentDayForecast(location.first.toString() + "," + location.second.toString())
            .enqueue(
                object : Callback<ResponseDto> {
                    override fun onResponse(
                        call: Call<ResponseDto>,
                        response: Response<ResponseDto>
                    ) {
                        val responseDto = response.body()

                        if (responseDto == null) {
                            onForecastRequestFailure()
                            return
                        }

                        onForecastRequestSuccess(responseDto)
                    }

                    override fun onFailure(call: Call<ResponseDto>, t: Throwable) {
                        onForecastRequestFailure()
                    }
                }
            )
    }
}