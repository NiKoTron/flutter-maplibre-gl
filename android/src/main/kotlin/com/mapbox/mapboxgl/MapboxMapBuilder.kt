// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.mapbox.mapboxgl

import android.content.Context
import android.view.Gravity
import com.mapbox.mapboxgl.MapboxMapsPlugin.LifecycleProvider
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.maps.MapboxMapOptions
import io.flutter.plugin.common.BinaryMessenger

internal class MapboxMapBuilder : MapboxMapOptionsSink {
    val TAG = javaClass.simpleName
    private val options =
        MapboxMapOptions().textureMode(true).attributionEnabled(true).logoEnabled(false)
    private var trackCameraPosition = false
    private var myLocationEnabled = false
    private var dragEnabled = true
    private var myLocationTrackingMode = 0
    private var myLocationRenderMode = 0
    private var styleString = "https://demotiles.maplibre.org/style.json"
    private var bounds: LatLngBounds? = null

    fun build(
        id: Int,
        context: Context?,
        messenger: BinaryMessenger?,
        lifecycleProvider: LifecycleProvider?
    ): MapboxMapController {
        //final MapboxMapOptions opt = MapboxMapOptions.createFromAttributes(context);
        val controller = MapboxMapController(
            id, context!!, messenger, lifecycleProvider!!, options, styleString, dragEnabled
        )
        controller.init()
        controller.setMyLocationEnabled(myLocationEnabled)
        controller.setMyLocationTrackingMode(myLocationTrackingMode)
        controller.setMyLocationRenderMode(myLocationRenderMode)
        controller.setTrackCameraPosition(trackCameraPosition)
        if (null != bounds) {
            controller.setCameraTargetBounds(bounds!!)
        }
        return controller
    }

    fun setInitialCameraPosition(position: CameraPosition?) {
        options.camera(position)
    }

    override fun setCompassEnabled(compassEnabled: Boolean) {
        options.compassEnabled(compassEnabled)
    }

    override fun setCameraTargetBounds(bounds: LatLngBounds?) {
        this.bounds = bounds
    }

    override fun setStyleString(styleString: String?) {
        if (styleString != null) {
            this.styleString = styleString
        }
        // options. styleString(styleString);
    }

    override fun setMinMaxZoomPreference(min: Float?, max: Float?) {
        if (min != null) {
            options.minZoomPreference(min.toDouble())
        }
        if (max != null) {
            options.maxZoomPreference(max.toDouble())
        }
    }

    override fun setTrackCameraPosition(trackCameraPosition: Boolean) {
        this.trackCameraPosition = trackCameraPosition
    }

    override fun setRotateGesturesEnabled(rotateGesturesEnabled: Boolean) {
        options.rotateGesturesEnabled(rotateGesturesEnabled)
    }

    override fun setScrollGesturesEnabled(scrollGesturesEnabled: Boolean) {
        options.scrollGesturesEnabled(scrollGesturesEnabled)
    }

    override fun setTiltGesturesEnabled(tiltGesturesEnabled: Boolean) {
        options.tiltGesturesEnabled(tiltGesturesEnabled)
    }

    override fun setZoomGesturesEnabled(zoomGesturesEnabled: Boolean) {
        options.zoomGesturesEnabled(zoomGesturesEnabled)
    }

    override fun setMyLocationEnabled(myLocationEnabled: Boolean) {
        this.myLocationEnabled = myLocationEnabled
    }

    override fun setMyLocationTrackingMode(myLocationTrackingMode: Int) {
        this.myLocationTrackingMode = myLocationTrackingMode
    }

    override fun setMyLocationRenderMode(myLocationRenderMode: Int) {
        this.myLocationRenderMode = myLocationRenderMode
    }

    override fun setLogoViewMargins(x: Int, y: Int) {
        options.logoMargins(
            intArrayOf(
                x,  // left
                0,  // top
                0,  // right
                y
            )
        )
    }

    override fun setCompassGravity(gravity: Int) {
        when (gravity) {
            0 -> options.compassGravity(Gravity.TOP or Gravity.START)
            1 -> options.compassGravity(Gravity.TOP or Gravity.END)
            2 -> options.compassGravity(Gravity.BOTTOM or Gravity.START)
            3 -> options.compassGravity(Gravity.BOTTOM or Gravity.END)
        }
    }

    override fun setCompassViewMargins(x: Int, y: Int) {
        when (options.compassGravity) {
            Gravity.TOP or Gravity.START -> options.compassMargins(
                intArrayOf(
                    x, y, 0, 0
                )
            )
            Gravity.TOP or Gravity.END -> options.compassMargins(intArrayOf(0, y, x, 0))
            Gravity.BOTTOM or Gravity.START -> options.compassMargins(intArrayOf(x, 0, 0, y))
            Gravity.BOTTOM or Gravity.END -> options.compassMargins(intArrayOf(0, 0, x, y))
            else -> options.compassMargins(intArrayOf(0, y, x, 0))
        }
    }

    override fun setAttributionButtonGravity(gravity: Int) {
        when (gravity) {
            0 -> options.attributionGravity(Gravity.TOP or Gravity.START)
            1 -> options.attributionGravity(Gravity.TOP or Gravity.END)
            2 -> options.attributionGravity(Gravity.BOTTOM or Gravity.START)
            3 -> options.attributionGravity(Gravity.BOTTOM or Gravity.END)
        }
    }

    override fun setAttributionButtonMargins(x: Int, y: Int) {
        when (options.attributionGravity) {
            Gravity.TOP or Gravity.START -> options.attributionMargins(
                intArrayOf(
                    x, y, 0, 0
                )
            )
            Gravity.TOP or Gravity.END -> options.attributionMargins(intArrayOf(0, y, x, 0))
            Gravity.BOTTOM or Gravity.START -> options.attributionMargins(intArrayOf(x, 0, 0, y))
            Gravity.BOTTOM or Gravity.END -> options.attributionMargins(intArrayOf(0, 0, x, y))
            else -> options.attributionMargins(intArrayOf(x, 0, 0, y))
        }
    }

    fun setDragEnabled(enabled: Boolean) {
        dragEnabled = enabled
    }
}