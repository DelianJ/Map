package com.example.openstreetmap.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.openstreetmap.databinding.LayoutInfoRutaBinding

class UIInfoRutaconstructor @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): ConstraintLayout(context, attrs, defStyleAttr){
    var bindingInfoRuta: LayoutInfoRutaBinding =
        LayoutInfoRutaBinding.inflate(LayoutInflater.from(context), this,true)

    fun setTextTiempoEstimado(text: String){
        bindingInfoRuta.txTiempoEstimado.text = text
    }

    fun setTextDistancia(text: String){
        bindingInfoRuta.txDistanciaEstimado.text = text
    }

}