package com.example.openstreetmap

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Looper
import android.os.StrictMode
import android.preference.PreferenceManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.openstreetmap.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

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
    private lateinit var mapController: MapController
    private lateinit var road: Road
    private var roadManager =  OSRMRoadManager(this, "raod")
    private lateinit var mCompassOverlay: CompassOverlay
    private lateinit var mrotateionOverlay: RotationGestureOverlay
    private lateinit var mLocationOverlay:MyLocationNewOverlay
    private lateinit var roadOverlay: Polyline

    private var geoUbicacion = GeoPoint(0.0, 0.0)
    val PREFS_NAME: String = "org.andnav.osm.prefs"

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
        initMap()
    }

    fun initUI(){
        binding.btNavegate.setOnClickListener {
            getRoad()
        }
    }

    private fun initMap() {
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        mapController = binding.map.controller as MapController
        mapController.setCenter(geoUbicacion)
        mapController.setZoom(18)
        binding.map.setMultiTouchControls(true)

        binding.map

        mCompassOverlay = CompassOverlay(binding.map.context,
            InternalCompassOrientationProvider(binding.map.context), binding.map)
        mCompassOverlay.enableCompass()
        mCompassOverlay.mOrientationProvider

        mrotateionOverlay = RotationGestureOverlay(binding.map)
        mrotateionOverlay.isEnabled

        mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.map)
        mLocationOverlay.enableMyLocation()
        mLocationOverlay.enableFollowLocation()
        binding.map.overlays.add(mLocationOverlay)
        binding.map.overlays.add(mCompassOverlay)
        binding.map.overlays.add(mrotateionOverlay)
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

    fun getRoad(){
        val lat = binding.etLatitude.text.toString().toDouble()
        val long = binding.etLongitude.text.toString().toDouble()
        val wayPoints = ArrayList<GeoPoint>()
        val endPoint = GeoPoint(lat, long)
        wayPoints.add(geoUbicacion)
        wayPoints.add(endPoint)

        road = roadManager.getRoad(wayPoints)
        roadOverlay = RoadManager.buildRoadOverlay(road)
        binding.map.overlays.add(roadOverlay)
        setMarket(endPoint)
    }

    private fun setMarket(endpoint: GeoPoint){
        val marker = Marker(binding.map)
        marker.position = endpoint
        marker.icon = ContextCompat.getDrawable(this, org.osmdroid.bonuspack.R.drawable.marker_default)
        marker.title = "EndPoint"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        binding.map.overlays.add(marker)
        binding.map.invalidate()
    }
}