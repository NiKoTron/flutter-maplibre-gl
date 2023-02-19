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

private data class BuilderData(
    val styleString: String = "https://demotiles.maplibre.org/style.json",
    var dragEnabled: Boolean = true,
    var trackCameraPosition : Boolean = false,
    var myLocationEnabled: Boolean = false,
    var myLocationTrackingMode: Int = 0,
    var myLocationRenderMode: Int = 0,    
    
    var initialCameraPosition: CameraPosition? = null,

    var cameraTargetBounds: LatLngBounds? = null,

    var compassEnabled: Boolean? = null,
    var compassGravity: Int? = null,
    var compassMarginX: Int? = null, var compassMarginY: Int? = null,

    var minZoomPreferences: Float? = null, var maxZoomPreferences: Float? = null,
    var logoViewMarginX: Int? = null, var logoViewMarginY: Int? = null,
    
    var rotateGesturesEnabled: Boolean? = null,
    var scrollGesturesEnabled: Boolean? = null,
    var tiltGesturesEnabled: Boolean? = null,
    var zoomGesturesEnabled: Boolean? = null,

    var attributionButtonGravity: Int? = null,
    var attributionButtonMarginX: Int? = null, var attributionButtonMarginY: Int? = null,
)

internal class MapboxMapBuilder : MapboxMapOptionsSink {
    val TAG = javaClass.simpleName

    private var builderData: BuilderData = BuilderData()

    fun build(
        id: Int,
        context: Context,
        messenger: BinaryMessenger,
        lifecycleProvider: LifecycleProvider
    ): MapboxMapController {

        val options = MapboxMapOptions.createFromAttributes(context)
        options.textureMode(true)
        options.attributionEnabled(true)
        options.logoEnabled(false)

        builderData.initialCameraPosition?.let {
            options.camera(it)
        }

        builderData.compassEnabled?.let {
            options.compassEnabled(it)
        }

        builderData.compassGravity?.let { g ->
            val gravity = when (g) {
                0 -> Gravity.TOP or Gravity.START
                1 -> Gravity.TOP or Gravity.END
                2 -> Gravity.BOTTOM or Gravity.START
                3 -> Gravity.BOTTOM or Gravity.END
                else -> Gravity.TOP or Gravity.END
            }
            val margins = builderData.compassMarginX?.let { x ->
                builderData.compassMarginY?.let { y ->
                    when (g) {
                        0 -> intArrayOf(x, y, 0, 0)
                        1 -> intArrayOf(0, y, x, 0)
                        2 -> intArrayOf(x, 0, 0, y)
                        3 -> intArrayOf(0, 0, x, y)
                        else -> intArrayOf(0, y, x, 0)
                    }
                }
            }

            options.compassGravity(gravity)
            margins?.let { m -> options.compassMargins(m) }
        }

        builderData.attributionButtonGravity?.let { g ->
            val gravity = when (g) {
                0 -> Gravity.TOP or Gravity.START
                1 -> Gravity.TOP or Gravity.END
                2 -> Gravity.BOTTOM or Gravity.START
                3 -> Gravity.BOTTOM or Gravity.END
                else -> Gravity.TOP or Gravity.END
            }
            val margins = builderData.attributionButtonMarginX?.let { x ->
                builderData.attributionButtonMarginY?.let { y ->
                    when (g) {
                        0 -> intArrayOf(x, y, 0, 0)
                        1 -> intArrayOf(0, y, x, 0)
                        2 -> intArrayOf(x, 0, 0, y)
                        3 -> intArrayOf(0, 0, x, y)
                        else -> intArrayOf(0, y, x, 0)
                    }
                }
            }

            options.attributionGravity(gravity)
            margins?.let { m -> options.attributionMargins(m) }
        }


        //TODO: Add logo gravity
        builderData.logoViewMarginX?.let { x ->
            builderData.logoViewMarginY?.let { y ->
                options.logoMargins(intArrayOf(x, 0, 0, y))
            }
        }

        builderData.minZoomPreferences?.let { min ->
            options.minZoomPreference(min.toDouble())
        }
        builderData.maxZoomPreferences?.let { max ->
            options.maxZoomPreference(max.toDouble())
        }

        builderData.rotateGesturesEnabled?.let { options.rotateGesturesEnabled(it) }
        builderData.scrollGesturesEnabled?.let { options.scrollGesturesEnabled(it) }
        builderData.tiltGesturesEnabled?.let { options.tiltGesturesEnabled(it) }
        builderData.zoomGesturesEnabled?.let { options.zoomGesturesEnabled(it) }

        val controller = MapboxMapController(
            id,
            context,
            messenger,
            lifecycleProvider,
            options,
            builderData.styleString,
            builderData.dragEnabled
        )

        controller.init()
        controller.setMyLocationEnabled(builderData.myLocationEnabled)
        controller.setMyLocationTrackingMode(builderData.myLocationTrackingMode)
        controller.setMyLocationRenderMode(builderData.myLocationRenderMode)
        controller.setTrackCameraPosition(builderData.trackCameraPosition)

        builderData.cameraTargetBounds?.let { bounds ->
            controller.setCameraTargetBounds(bounds)
        }

        return controller
    }

    fun setInitialCameraPosition(position: CameraPosition?) {
        builderData = builderData.copy(initialCameraPosition = position)
    }

    override fun setCompassEnabled(compassEnabled: Boolean) {
        builderData = builderData.copy(compassEnabled = compassEnabled)
    }

    override fun setCameraTargetBounds(bounds: LatLngBounds?) {
        builderData = builderData.copy(cameraTargetBounds = bounds)
    }

    override fun setStyleString(styleString: String?) {
        styleString?.let { style -> builderData = builderData.copy(styleString = style) }
    }

    override fun setMinMaxZoomPreference(min: Float?, max: Float?) {
        builderData = builderData.copy(minZoomPreferences = min)
        builderData = builderData.copy(maxZoomPreferences = max)
    }

    override fun setTrackCameraPosition(trackCameraPosition: Boolean) {
        builderData = builderData.copy(trackCameraPosition = trackCameraPosition)
    }

    override fun setRotateGesturesEnabled(rotateGesturesEnabled: Boolean) {
        builderData = builderData.copy(rotateGesturesEnabled = rotateGesturesEnabled)
    }

    override fun setScrollGesturesEnabled(scrollGesturesEnabled: Boolean) {
        builderData = builderData.copy(scrollGesturesEnabled = scrollGesturesEnabled)
    }

    override fun setTiltGesturesEnabled(tiltGesturesEnabled: Boolean) {
        builderData = builderData.copy(tiltGesturesEnabled = tiltGesturesEnabled)
    }

    override fun setZoomGesturesEnabled(zoomGesturesEnabled: Boolean) {
        builderData = builderData.copy(zoomGesturesEnabled = zoomGesturesEnabled)
    }

    override fun setMyLocationEnabled(myLocationEnabled: Boolean) {
        builderData = builderData.copy(myLocationEnabled = myLocationEnabled)
    }

    override fun setMyLocationTrackingMode(myLocationTrackingMode: Int) {
        builderData = builderData.copy(myLocationTrackingMode = myLocationTrackingMode)
    }

    override fun setMyLocationRenderMode(myLocationRenderMode: Int) {
        builderData = builderData.copy(myLocationRenderMode = myLocationRenderMode)
    }

    override fun setLogoViewMargins(x: Int, y: Int) {
        builderData = builderData.copy(logoViewMarginX = x)
        builderData = builderData.copy(logoViewMarginY = y)
    }

    override fun setCompassGravity(gravity: Int) {
        builderData = builderData.copy(compassGravity = gravity)
    }

    override fun setCompassViewMargins(x: Int, y: Int) {
        builderData = builderData.copy(compassMarginX = x)
        builderData = builderData.copy(compassMarginY = y)
    }

    override fun setAttributionButtonGravity(gravity: Int) {
        builderData = builderData.copy(attributionButtonGravity = gravity)
    }

    override fun setAttributionButtonMargins(x: Int, y: Int) {
        builderData = builderData.copy(attributionButtonMarginX = x)
        builderData = builderData.copy(attributionButtonMarginY = y)
    }

    fun setDragEnabled(enabled: Boolean) {
        builderData = builderData.copy(dragEnabled = enabled)
    }
}