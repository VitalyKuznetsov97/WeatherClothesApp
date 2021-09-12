package com.example.weatherclothesapp.weather

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherclothesapp.R
import com.example.weatherclothesapp.weather.Constants.CURRENT_LOCATION_ERROR
import com.example.weatherclothesapp.weather.Constants.EXPECTED_INFO_TYPE_KEY
import com.example.weatherclothesapp.weather.Constants.IS_LOADING_KEY
import com.example.weatherclothesapp.weather.Constants.IS_SHOWING_RATIONALE_KEY
import com.example.weatherclothesapp.weather.Constants.LAST_LOCATION_ERROR
import com.example.weatherclothesapp.weather.Constants.LOCATION_KEY
import com.example.weatherclothesapp.weather.Constants.LOCATION_PERMISSION_DENIED_TEXT
import com.example.weatherclothesapp.weather.Constants.LOCATION_PERMISSION_RATIONALE_TEXT
import com.example.weatherclothesapp.weather.Constants.TEXT_KEY
import com.example.weatherclothesapp.weather.Constants.WEATHER_FORECAST_ERROR
import com.example.weatherclothesapp.weather.helpers.LocationHelper
import com.example.weatherclothesapp.weather.helpers.PermissionHelper
import com.example.weatherclothesapp.weather.helpers.PredictionHelper
import com.example.weatherclothesapp.weather.helpers.WeatherApiHelper
import com.example.weatherclothesapp.weather.simple_objects.PredictionModel
import com.example.weatherclothesapp.weather.simple_objects.ResponseDto
import com.example.weatherclothesapp.weather.simple_objects.WeatherModel
import com.example.weatherclothesapp.weather.simple_objects.WeatherModelMapper
import com.google.android.gms.location.LocationServices

class WeatherActivity : AppCompatActivity() {

    //Views
    private lateinit var textView: TextView
    private lateinit var acceptRationaleButton: View
    private lateinit var loadingView: View

    //Helpers
    private lateinit var locationHelper: LocationHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var weatherApiHelper: WeatherApiHelper
    private lateinit var predictionHelper: PredictionHelper

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
        val lastLocationButton = findViewById<View>(R.id.button_1)
        val currentLocationButton = findViewById<View>(R.id.button_2)
        val weatherButton = findViewById<View>(R.id.button_3)
        val predictionButton = findViewById<View>(R.id.button_4)
        textView = findViewById(R.id.text)
        acceptRationaleButton = findViewById(R.id.button_accept)
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
        predictionButton.setOnClickListener {
            onPredictionClicked()
        }
        acceptRationaleButton.setOnClickListener {
            onLocationPermissionRationaleAccepted()
        }

        //Init tools
        val activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isPermissionGranted ->
            when (isPermissionGranted) {
                true -> onLocationPermissionGranted()
                false -> onLocationPermissionDenied()
            }
        }
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationHelper = LocationHelper(fusedLocationClient)
        permissionHelper = PermissionHelper(activityResultLauncher)
        weatherApiHelper = WeatherApiHelper()
        predictionHelper = PredictionHelper()
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

        weatherApiHelper.makeWeatherRequest(
            location,
            { responseDto -> onForecastRequestSuccess(responseDto) },
            { onForecastRequestFailure() }
        )
    }

    private fun onPredictionClicked() {
        expectedInfoType = ExpectedInfoType.Prediction
        isLoading = true
        isShowingRationale = false
        text = ""

        updateUi()

        weatherApiHelper.makeWeatherRequest(
            location,
            { responseDto -> onForecastRequestSuccess(responseDto) },
            { onForecastRequestFailure() }
        )
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

        permissionHelper.askLocationPermission()
    }

    //Weather api callbacks
    private fun onForecastRequestSuccess(responseDto: ResponseDto) {
        val weatherModel = WeatherModelMapper.fromResponseDto(responseDto)

        if (expectedInfoType == ExpectedInfoType.Prediction) {
            makePrediction(weatherModel)
            return
        }

        isLoading = false
        isShowingRationale = false
        text = weatherModel.toString()

        updateUi()
    }

    private fun onForecastRequestFailure() {
        isLoading = false
        isShowingRationale = false
        text = WEATHER_FORECAST_ERROR

        updateUi()
    }

    //Prediction callbacks
    private fun onPredictionReady(predictionModel: PredictionModel) {
        isLoading = false
        isShowingRationale = false
        text = predictionModel.predictionText

        updateUi()
    }

    //Location support methods
    private fun onLocationRequested() {
        if (permissionHelper.hasLocationPermission(this)) {
            //Has permission
            getLocation()
        } else {
            //No permission
            when {
                permissionHelper.shouldShowLocationPermissionRationale(this) -> onLocationPermissionRationaleRequested()
                else -> permissionHelper.askLocationPermission()
            }
        }
    }

    private fun getLocation() {
        when (expectedInfoType) {
            ExpectedInfoType.LastLocation -> {
                locationHelper.getLastLocation { location ->
                    when {
                        location != null -> onLocationRequestSuccess(location)
                        else -> onLocationRequestFailure(LAST_LOCATION_ERROR)
                    }
                }
            }
            ExpectedInfoType.CurrentLocation -> {
                locationHelper.getCurrentLocation { location ->
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

    private fun makePrediction(weatherModel: WeatherModel) {
        val predictionModel = predictionHelper.getPrediction(
            weatherModel
        )

        onPredictionReady(predictionModel)
    }

    //UI
    private fun updateUi() {
        textView.visibility = if (isLoading) View.GONE else View.VISIBLE
        textView.text = text
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        acceptRationaleButton.visibility = if (isShowingRationale) View.VISIBLE else View.GONE
    }

    //Ui state
    private enum class ExpectedInfoType {
        LastLocation,
        CurrentLocation,
        Weather,
        Prediction,
        NoInfoExpected,
    }

    private fun Int.toExpectedInfoType(): ExpectedInfoType {
        return when (this) {
            ExpectedInfoType.LastLocation.ordinal -> ExpectedInfoType.LastLocation
            ExpectedInfoType.CurrentLocation.ordinal -> ExpectedInfoType.CurrentLocation
            ExpectedInfoType.Weather.ordinal -> ExpectedInfoType.Weather
            ExpectedInfoType.Prediction.ordinal -> ExpectedInfoType.Prediction
            else -> ExpectedInfoType.NoInfoExpected
        }
    }
}
