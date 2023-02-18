package com.mapbox.mapboxgl

import android.content.Context
import com.mapbox.mapboxsdk.Mapbox

internal object MapBoxUtils {
    private const val TAG = "MapboxMapController"
    fun getMapbox(context: Context?): Mapbox {
        return Mapbox.getInstance(context!!)
    }
}