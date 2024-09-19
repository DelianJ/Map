package com.example.openstreetmap.manejoMapa

import android.annotation.SuppressLint
import android.icu.text.DecimalFormat
import android.icu.text.SimpleDateFormat
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat
import org.osmdroid.bonuspack.routing.OSRMRoadManager
import org.osmdroid.bonuspack.routing.Road
import org.osmdroid.bonuspack.routing.RoadManager
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ManejoMapa {
    private lateinit var mapController: MapController
    private lateinit var road: Road
    private lateinit var roadManager:  OSRMRoadManager
    private lateinit var mCompassOverlay: CompassOverlay
    private lateinit var mrotateionOverlay: RotationGestureOverlay
    private lateinit var mLocationOverlay:MyLocationNewOverlay
    private lateinit var roadOverlay: Polyline
    private lateinit var marker: Marker
    private val df: DecimalFormat = DecimalFormat("#.00")

    fun initMap(map: MapView): MapView {
        map.setTileSource(TileSourceFactory.MAPNIK)
        mapController = map.controller as MapController
        mapController.setCenter(GeoPoint(0.0,0.0))
        mapController.setZoom(18)
        map.setMultiTouchControls(true)

        mCompassOverlay = CompassOverlay(map.context,
            InternalCompassOrientationProvider(map.context), map)
        mCompassOverlay.enableCompass()
        mCompassOverlay.mOrientationProvider

        mrotateionOverlay = RotationGestureOverlay(map)
        mrotateionOverlay.isEnabled

        mLocationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(map.context), map)
        mLocationOverlay.enableMyLocation()
        mLocationOverlay.enableFollowLocation()
        map.overlays.add(mLocationOverlay)
        map.overlays.add(mCompassOverlay)
        map.overlays.add(mrotateionOverlay)
        return map
    }

    fun getRoad(geoUbicacion: GeoPoint, endPoint: GeoPoint, map: MapView): Road{
        val wayPoints = ArrayList<GeoPoint>()
        wayPoints.add(geoUbicacion)
        wayPoints.add(endPoint)

        roadManager =  OSRMRoadManager(map.context, "raod")
        road = roadManager.getRoad(wayPoints)
        val min = tiempoEstimado(road.mDuration)
        Log.println(Log.INFO, "Roud", "Duracion: $min Min, Distancia: ${df.format(road.mLength)} Km")
        roadOverlay = RoadManager.buildRoadOverlay(road)
        map.overlays.add(roadOverlay)
        setMarket(endPoint, map)
        return road
    }

    fun setMarket(endpoint: GeoPoint, map: MapView){
        marker = Marker(map)
        marker.position = endpoint
        marker.icon = ContextCompat.getDrawable(map.context, org.osmdroid.bonuspack.R.drawable.center)
        marker.title = "EndPoint"
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        map.overlays.add(marker)
        map.invalidate()
    }

    @SuppressLint("SimpleDateFormat")
    fun tiempoEstimado(segundos: Double): String{
        val tiempo = TimeUnit.SECONDS.toMillis(segundos.toLong())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = tiempo
        val time = SimpleDateFormat("HH:mm:ss")
        return time.format(calendar.time)
    }

    fun limpiaMapa(map: MapView) {
        map.overlays.remove(roadOverlay)
        map.overlays.remove(marker)
    }
}