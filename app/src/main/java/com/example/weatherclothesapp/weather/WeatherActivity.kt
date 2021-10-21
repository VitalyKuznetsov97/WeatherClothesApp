package com.example.weatherclothesapp.weather

import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.weatherclothesapp.R
import com.example.weatherclothesapp.weather.constants.Constants
import com.example.weatherclothesapp.weather.helpers.LocationHelper
import com.example.weatherclothesapp.weather.helpers.PermissionHelper
import com.example.weatherclothesapp.weather.helpers.PredictionHelper
import com.example.weatherclothesapp.weather.helpers.WeatherApiHelper
import com.example.weatherclothesapp.weather.mappers.WeatherModelMapper
import com.example.weatherclothesapp.weather.simple_objects.local.PredictionModel
import com.example.weatherclothesapp.weather.simple_objects.local.WeatherModel
import com.example.weatherclothesapp.weather.simple_objects.web.ResponseDto
import com.example.weatherclothesapp.weather.storage.StorageHelper
import com.google.android.gms.location.LocationServices
import java.util.*

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
    private lateinit var storageHelper: StorageHelper

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
        storageHelper = StorageHelper()

        //Creation callback
        if (savedInstanceState == null) {
            onActivityFirstCreated()
        }
    }

    //Restore ui state
    override fun onResume() {
        super.onResume()
        updateUi()


    }

    //Save data
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(Constants.TEXT_KEY, text)
        outState.putSerializable(Constants.LOCATION_KEY, location)
        outState.putInt(Constants.EXPECTED_INFO_TYPE_KEY, expectedInfoType.ordinal)
        outState.putBoolean(Constants.IS_SHOWING_RATIONALE_KEY, isShowingRationale)
        outState.putBoolean(Constants.IS_LOADING_KEY, isLoading)
        super.onSaveInstanceState(outState)
    }

    //Restore data
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        text = savedInstanceState.getString(Constants.TEXT_KEY, "")
        expectedInfoType = savedInstanceState
            .getInt(Constants.EXPECTED_INFO_TYPE_KEY, 0)
            .toExpectedInfoType()
        isLoading = savedInstanceState.getBoolean(Constants.IS_LOADING_KEY, false)
        isShowingRationale =
            savedInstanceState.getBoolean(Constants.IS_SHOWING_RATIONALE_KEY, false)

        val locationSerializable = savedInstanceState.getSerializable(Constants.LOCATION_KEY)
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

        onWeatherForecastRequested()
    }

    private fun onPredictionClicked() {
        expectedInfoType = ExpectedInfoType.Prediction
        isLoading = true
        isShowingRationale = false
        text = ""

        updateUi()

        onWeatherForecastRequested()
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
        text = Constants.LOCATION_PERMISSION_DENIED_TEXT

        updateUi()
    }

    //Location permission Rationale callbacks
    private fun onLocationPermissionRationaleRequested() {
        isLoading = false
        isShowingRationale = true
        text = Constants.LOCATION_PERMISSION_RATIONALE_TEXT

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
            onForecastReady(weatherModel)
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
        text = Constants.WEATHER_FORECAST_ERROR

        updateUi()
    }

    //Prediction callbacks
    private fun onForecastReady(weatherModel: WeatherModel) {
        val predictionModel = predictionHelper.createPrediction(0, weatherModel)

        isLoading = false
        isShowingRationale = false
        text = predictionModel.text

        updateUi()
    }

    //First time activity creation callbacks
    private fun onActivityFirstCreated() {
        val lastPrediction = storageHelper.getPrediction(this, 0)

        expectedInfoType = ExpectedInfoType.Prediction
        isLoading = false
        isShowingRationale = false
        text = getLastPredictionText(lastPrediction)

        updateUi()
    }

    //Support methods
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

    private fun onWeatherForecastRequested() {
        weatherApiHelper.makeWeatherRequest(
            location,
            { responseDto -> onForecastRequestSuccess(responseDto) },
            { onForecastRequestFailure() }
        )
    }

    private fun getLocation() {
        when (expectedInfoType) {
            ExpectedInfoType.LastLocation -> {
                locationHelper.getLastLocation { location ->
                    when {
                        location != null -> onLocationRequestSuccess(location)
                        else -> onLocationRequestFailure(Constants.LAST_LOCATION_ERROR)
                    }
                }
            }
            ExpectedInfoType.CurrentLocation -> {
                locationHelper.getCurrentLocation { location ->
                    when {
                        location != null -> onLocationRequestSuccess(location)
                        else -> onLocationRequestFailure(Constants.CURRENT_LOCATION_ERROR)
                    }
                }
            }
            else -> {
                //Do nothing
            }
        }
    }

    private fun getLastPredictionText(lastPrediction: PredictionModel?): String {
        return if (lastPrediction == null) {
            Constants.NO_SAVED_PREDICTIONS_TEXT
        } else {
            val diff = Date().time - lastPrediction.timestamp
            val hours = diff / (1000 * 60 * 60)
            val minutes = (diff / (1000 * 60)) % 60
            val seconds = (diff / (1000 * 60)) % 60


            "Last prediction: " + lastPrediction.text + '\n' +
                    "Last prediction was made " + hours + " hours " + minutes + " minutes " + seconds + " seconds ago"
        }
    }

    //UI
    private fun updateUi() {
        textView.visibility = if (isLoading) View.GONE else View.VISIBLE
        textView.text = text
        loadingView.visibility = if (isLoading) View.VISIBLE else View.GONE
        acceptRationaleButton.visibility = if (isShowingRationale) View.VISIBLE else View.GONE
    }

    //UI state
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
