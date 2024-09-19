package com.example.openstreetmap.hora

import android.annotation.SuppressLint
import android.icu.text.SimpleDateFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ConvierteHora {

    @SuppressLint("SimpleDateFormat")
    fun tiempoSegundoHora(segundos: Double): String{
        val tiempo = TimeUnit.SECONDS.toMillis(segundos.toLong())
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = tiempo
        val time = SimpleDateFormat("HH:mm:ss")
        return time.format(calendar.time)
    }
}