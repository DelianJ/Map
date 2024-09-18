package com.example.openstreetmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.preference.PreferenceManager
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.openstreetmap.databinding.ActivityMainBinding
import com.example.openstreetmap.manejoMapa.ManejoMapa
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import java.util.Calendar
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private val permisos = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET
    )

    private lateinit var binding: ActivityMainBinding

    private var geoUbicacion = GeoPoint(0.0, 0.0)
    private var manejoMapa: ManejoMapa = ManejoMapa()
    private val df: DecimalFormat = DecimalFormat("#.00")
    private lateinit var road: Road

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        binding =  ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            solisitarPermisos()
        }

        solicitarActualizacionUbicacion()
        initUI()
        manejoMapa.initMap(binding.map)
    }

    fun initUI(){
        binding.btNavegate.setOnClickListener {
            hideKeyboard()
            val endPoint = GeoPoint(binding.etLatitude.text.toString().toDouble(),
                binding.etLongitude.text.toString().toDouble())
            CoroutineScope(Dispatchers.IO).launch {
                road = manejoMapa.getRoad(geoUbicacion,endPoint, binding.map)
                mostrarInfo(road)
            }
            binding.etLatitude.isEnabled = false
            binding.etLongitude.isEnabled = false
            binding.uiInfo.visibility = View.VISIBLE
        }
    }

    @SuppressLint("SetTextI18n")
    fun mostrarInfo(road: Road){
        println("----------${road.mLength}-------------")
        binding.uiInfo.setTextDistancia("${df.format(road.mLength)}Km")
        binding.uiInfo.setTextTiempoEstimado("${tiempoEstimado(road.mDuration)}HH:mm:ss")
    }

    private fun solicitarActualizacionUbicacion() {
        locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            5000
        ).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                for (location in p0.locations) {
                    println("Location: ${location.toString()}")
                    geoUbicacion.latitude = location.latitude
                    geoUbicacion.longitude = location.longitude
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            solisitarPermisos()
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun solisitarPermisos(){
        ActivityCompat.requestPermissions( this, permisos, 1)
    }

    private fun setMarket(endpoint: GeoPoint){
        val marker = Marker(binding.map)
        marker.position = endpoint
        marker.icon = ContextCompat.getDrawable(this, org.osmdroid.bonuspack.R.drawable.center)
        marker.title = "EndPoint"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        binding.map.overlays.add(marker)
        binding.map.invalidate()
    }

    @SuppressLint("SimpleDateFormat")
    fun tiempoEstimado(segundos: Double): String{
        val tiempo = TimeUnit.SECONDS.toMillis(segundos.toLong())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = tiempo
        val time = SimpleDateFormat("HH:mm:ss")
        return time.format(calendar.time)
    }

    private fun hideKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.main.windowToken, 0)
    }
}