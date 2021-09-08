package com.example.weatherclothesapp.weather

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.weatherclothesapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.CancellationTokenSource
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query


class WeatherActivity : AppCompatActivity() {

    //Views
    private lateinit var textView: TextView
    private lateinit var acceptRationaleButton: View
    private lateinit var loadingView: View

    //Tools
    private lateinit var activityResultLauncher: ActivityResultLauncher<String>
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //Data
    private var text: String = ""
    private var location: Pair<Double, Double> = Pair(0.0, 0.0)
    private var expectedInfoType: ExpectedInfoType = ExpectedInfoType.NoInfoExpected
    private var isLoading: Boolean = false
    private var isShowingRationale: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        //Init views
        textView = findViewById(R.id.text)
        val lastLocationButton = findViewById<View>(R.id.button_1)
        val currentLocationButton = findViewById<View>(R.id.button_2)
        val weatherButton = findViewById<View>(R.id.button_3)
        acceptRationaleButton = findViewById(R.id.button_4)
        loadingView = findViewById(R.id.pb)

        //Set listeners
        lastLocationButton.setOnClickListener {
            onLastLocationClicked()
        }
        currentLocationButton.setOnClickListener {
            onCurrentLocationClicked()
        }
        weatherButton.setOnClickListener {
            onWeatherClicked()
        }
        acceptRationaleButton.setOnClickListener {
            onLocationPermissionRationaleAccepted()
        }

        //Init tools
        activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isPermissionGranted ->
            when (isPermissionGranted) {
                true -> onLocationPermissionGranted()
                false -> onLocationPermissionDenied()
            }
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    //Restore ui state
    override fun onResume() {
        super.onResume()
        updateUi()
    }

    //Save data
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(TEXT_KEY, text)
        outState.putSerializable(LOCATION_KEY, location)
        outState.putInt(EXPECTED_INFO_TYPE_KEY, expectedInfoType.ordinal)
        outState.putBoolean(IS_SHOWING_RATIONALE_KEY, isShowingRationale)
        outState.putBoolean(IS_LOADING_KEY, isLoading)
        super.onSaveInstanceState(outState)
    }

    //Restore data
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        text = savedInstanceState.getString(TEXT_KEY, "")
        expectedInfoType = savedInstanceState
            .getInt(EXPECTED_INFO_TYPE_KEY, 0)
            .toExpectedInfoType()
        isLoading = savedInstanceState.getBoolean(IS_LOADING_KEY, false)
        isShowingRationale = savedInstanceState.getBoolean(IS_SHOWING_RATIONALE_KEY, false)

        val locationSerializable = savedInstanceState.getSerializable(LOCATION_KEY)
        if (locationSerializable is Pair<*, *>) {
            val first = locationSerializable.first
            val second = locationSerializable.second

            if (first is Double && second is Double) {
                location = Pair(first, second)
            }
        }
    }

    //UI Callbacks
    private fun onLastLocationClicked() {
        expectedInfoType = ExpectedInfoType.LastLocation
        isLoading = true
        isShowingRationale = false
        text = ""

        updateUi()

        onLocationRequested()
    }

    private fun onCurrentLocationClicked() {
        expectedInfoType = ExpectedInfoType.CurrentLocation
        isLoading = true
        isShowingRationale = false
        text = ""

        updateUi()

        onLocationRequested()
    }

    private fun onWeatherClicked() {
        expectedInfoType = ExpectedInfoType.Weather
        isLoading = true
        isShowingRationale = false
        text = ""

        updateUi()

        makeWeatherRequest()
    }

    //Location request callbacks
    private fun onLocationRequestSuccess(location: Location) {
        isLoading = false
        isShowingRationale = false
        text = location.latitude.toString() + " " + location.longitude
        this.location = Pair(location.latitude, location.longitude)

        updateUi()
    }

    private fun onLocationRequestFailure(errorText: String) {
        isLoading = false
        isShowingRationale = false
        text = errorText

        updateUi()
    }

    //Location permission callbacks
    private fun onLocationPermissionGranted() {
        getLocation()
    }

    private fun onLocationPermissionDenied() {
        isLoading = false
        isShowingRationale = false
        text = LOCATION_PERMISSION_DENIED_TEXT

        updateUi()
    }

    //Location permission Rationale callbacks
    private fun onLocationPermissionRationaleRequested() {
        isLoading = false
        isShowingRationale = true
        text = LOCATION_PERMISSION_RATIONALE_TEXT

        updateUi()
    }

    private fun onLocationPermissionRationaleAccepted() {
        isLoading = true
        isShowingRationale = false
        text = ""

        updateUi()

        askLocationPermission()
    }

    //Weather api callbacks
    private fun onForecastRequestSuccess(responseDto: ResponseDto) {
        isLoading = false
        isShowingRationale = false

        text = WeatherModelMapper.fromResponseDto(responseDto).toString()

        updateUi()
    }

    private fun onForecastRequestFailure() {
        isLoading = false
        isShowingRationale = false
        text = WEATHER_FORECAST_ERROR

        updateUi()
    }

    //Support methods
    private fun onLocationRequested() {
        if (hasLocationPermission()) {
            //Has permission
            getLocation()
        } else {
            //No permission
            when {
                shouldShowLocationPermissionRationale() -> onLocationPermissionRationaleRequested()
                else -> askLocationPermission()
            }
        }
    }

    private fun getLocation() {
        when (expectedInfoType) {
            ExpectedInfoType.LastLocation -> {
                getLastLocation { location ->
                    when {
                        location != null -> onLocationRequestSuccess(location)
                        else -> onLocationRequestFailure(LAST_LOCATION_ERROR)
                    }
                }
            }
            ExpectedInfoType.CurrentLocation -> {
                getCurrentLocation { location ->
                    when {
                        location != null -> onLocationRequestSuccess(location)
                        else -> onLocationRequestFailure(CURRENT_LOCATION_ERROR)
                    }
                }
            }
            else -> {
                //Do nothing
            }
        }
    }

    //UI
    private fun updateUi() {
        textView.visibility = if (isLoading) View.GONE else View.VISIBLE
        textView.text = text
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        acceptRationaleButton.visibility = if (isShowingRationale) View.VISIBLE else View.GONE
    }

    //Permission
    private fun hasLocationPermission(): Boolean {
        val permissionState = ContextCompat.checkSelfPermission(this, LOCATION_PERMISSION_STRING)
        return (permissionState == PackageManager.PERMISSION_GRANTED)
    }

    private fun shouldShowLocationPermissionRationale(): Boolean {
        return shouldShowRequestPermissionRationale(LOCATION_PERMISSION_STRING)
    }

    private fun askLocationPermission() {
        activityResultLauncher.launch(LOCATION_PERMISSION_STRING)
    }

    //Location
    @SuppressLint("MissingPermission")
    private fun getLastLocation(
        onLocationReceived: (Location?) -> Unit
    ) {
        fusedLocationClient
            .lastLocation
            .addOnSuccessListener {
                onLocationReceived.invoke(it)
            }
    }

    /**
     * Works unreliably.
     */
    @SuppressLint("MissingPermission")
    private fun getCurrentLocation(
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
                onLocationReceived.invoke(it.result)
            }
    }

    //WeatherApi
    interface WeatherApi {
        @GET(
            "forecast.json?" +
                    "key=f5b9913d921b45218f383452210809" +
                    "&days=1" +
                    "&hour=24"
        )
        fun getCurrentDayForecast(@Query("q") location: String): Call<ResponseDto>
    }

    private fun makeWeatherRequest() {
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

    //Ui state
    private enum class ExpectedInfoType {
        LastLocation,
        CurrentLocation,
        Weather,
        NoInfoExpected,
    }

    private fun Int.toExpectedInfoType(): ExpectedInfoType {
        return when (this) {
            ExpectedInfoType.LastLocation.ordinal -> ExpectedInfoType.LastLocation
            ExpectedInfoType.CurrentLocation.ordinal -> ExpectedInfoType.CurrentLocation
            ExpectedInfoType.Weather.ordinal -> ExpectedInfoType.Weather
            else -> ExpectedInfoType.NoInfoExpected
        }
    }

    //Constants
    private companion object {

        //SaveInstance keys
        private const val TEXT_KEY = "TEXT_KEY"
        private const val LOCATION_KEY = "LOCATION_KEY"
        private const val EXPECTED_INFO_TYPE_KEY = "EXPECTED_INFO_TYPE_KEY"
        private const val IS_LOADING_KEY = "IS_LOADING_KEY"
        private const val IS_SHOWING_RATIONALE_KEY = "IS_SHOWING_RATIONALE_KEY"

        //Strings
        private const val LOCATION_PERMISSION_RATIONALE_TEXT =
            "Please give us permission to know your location."
        private const val LOCATION_PERMISSION_DENIED_TEXT =
            "Sorry. We need your permission for location to work."
        private const val LAST_LOCATION_ERROR = "Couldn't get your last location."
        private const val CURRENT_LOCATION_ERROR = "Couldn't get your current location."
        private const val WEATHER_FORECAST_ERROR = "Failed to get a forecast."

        //Permission name
        private const val LOCATION_PERMISSION_STRING = Manifest.permission.ACCESS_FINE_LOCATION

    }

}
