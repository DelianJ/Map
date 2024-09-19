package com.example.openstreetmap

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.icu.text.DecimalFormat
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.openstreetmap.databinding.ActivityMainBinding
import com.example.openstreetmap.hora.ConvierteHora
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint

class MainActivity : AppCompatActivity() {
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private lateinit var road: Road
    private val permisos = arrayOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.INTERNET
    )

    private lateinit var binding: ActivityMainBinding

    private lateinit var endPoint: GeoPoint
    private var geoUbicacion = GeoPoint(0.0, 0.0)
    private var manejoMapa: ManejoMapa = ManejoMapa()
    private val df: DecimalFormat = DecimalFormat("#.00")
    private var enruta = false
    private val mostrar = true
    private val ocultar = false

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

    override fun onResume() {
        super.onResume()
        startLocationUpdates()
    }

    override fun onStop() {
        super.onStop()
        stopLocation()
    }

    fun initUI(){
        binding.btNavegate.setOnClickListener {
            hideKeyboard()
            endPoint = GeoPoint(binding.etLatitude.text.toString().toDouble(),
                binding.etLongitude.text.toString().toDouble())
            CoroutineScope(Dispatchers.IO).launch {
                road = manejoMapa.getRoad(geoUbicacion,endPoint, binding.map)
                mostrarInfo(road)
                enruta = true
                verificaUbicacion()
            }
            visibilidadBoton(ocultar)
            visibilidadLatLong(ocultar)
            visibilidadUIInfo(View.VISIBLE)
        }

        binding.uiInfo.bindingInfoRuta.btnNuevaNuta.setOnClickListener {
            visibilidadUIInfo(View.GONE)
            visibilidadLatLong(mostrar)
            visibilidadBoton(mostrar)
            manejoMapa.limpiaMapa(binding.map)
            binding.uiInfo.setTextTiempoEstimado(getString(R.string.calculando))
            binding.uiInfo.setTextDistancia(getString(R.string.calculando))
            enruta = false
        }
    }

    fun visibilidadLatLong(estado: Boolean){
        binding.etLatitude.isEnabled = estado
        binding.etLongitude.isEnabled = estado
    }

    fun visibilidadBoton(estado: Boolean){
        binding.btNavegate.isEnabled = estado
    }

    @SuppressLint("SetTextI18n")
    fun mostrarInfo(road: Road){
        binding.uiInfo.setTextDistancia("${df.format(road.mLength)}Km")
        binding.uiInfo.setTextTiempoEstimado("${ConvierteHora().tiempoSegundoHora(road.mDuration)}HH:mm:ss")
    }

    suspend fun verificaUbicacion(){
        while (enruta){
            val distancia = geoUbicacion.distanceToAsDouble(endPoint)
            Log.println(Log.INFO, "Verifica", "$distancia")
            if (distancia <= 10){
                enruta = false
                runOnUiThread {
                    mostarMensaje()
                }
            }
            delay(5000)
        }
    }

    fun mostarMensaje(){
        val mensaje = AlertDialog.Builder(this)
        mensaje.setMessage("Ha llegado a su destino")
            .setCancelable(false)
            .setPositiveButton("OK"){ _, _ ->
                manejoMapa.limpiaMapa(binding.map)
                visibilidadUIInfo(View.GONE)
                visibilidadBoton(mostrar)
                visibilidadLatLong(mostrar)
            }.create().show()
    }

    fun visibilidadUIInfo(estado: Int){
        binding.uiInfo.visibility = estado
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
        startLocationUpdates()
    }

    private fun solisitarPermisos(){
        ActivityCompat.requestPermissions( this, permisos, 1)
    }

    private fun hideKeyboard(){
        val imm: InputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.main.windowToken, 0)
    }

    private fun stopLocation(){
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates(){
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
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback,
            Looper.getMainLooper())
    }
}