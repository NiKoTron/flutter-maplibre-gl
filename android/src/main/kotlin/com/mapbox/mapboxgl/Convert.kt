// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.mapbox.mapboxgl

import android.content.Context
import com.mapbox.geojson.Point
import com.mapbox.geojson.Polygon
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraPosition.Builder
import com.mapbox.mapboxsdk.camera.CameraUpdate
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.bearingTo
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newCameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLng
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngBounds
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.newLatLngZoom
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.tiltTo
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.zoomBy
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.zoomIn
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.zoomOut
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory.zoomTo
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.LatLngBounds.Builder as LatLngBoundsBuilder
import com.mapbox.mapboxsdk.maps.MapboxMap
import java.util.*

/** Conversions between JSON-like values and MapboxMaps data types.  */
internal object Convert {
    private const val TAG = "Convert"
    @JvmStatic
    fun toBoolean(o: Any): Boolean {
        return o as Boolean
    }

    @JvmStatic
    fun toCameraPosition(o: Any): CameraPosition {
        val data = toMap(o)
        val builder = Builder()
        builder.bearing(toFloat(data["bearing"]).toDouble())
        builder.target(toLatLng(data["target"]))
        builder.tilt(toFloat(data["tilt"]).toDouble())
        builder.zoom(toFloat(data["zoom"]).toDouble())
        return builder.build()
    }

    fun isScrollByCameraUpdate(o: Any?): Boolean {
        return toString(toList(o)!![0]!!) == "scrollBy"
    }

    fun toCameraUpdate(o: Any, mapboxMap: MapboxMap, density: Float): CameraUpdate? {
        val data = toList(o)
        return when (toString(data!![0]!!)) {
            "newCameraPosition" -> newCameraPosition(
                toCameraPosition(
                    data[1]!!
                )
            )
            "newLatLng" -> newLatLng(toLatLng(data[1]))
            "newLatLngBounds" -> newLatLngBounds(
                toLatLngBounds(data[1])!!,
                toPixels(data[2]!!, density),
                toPixels(data[3]!!, density),
                toPixels(data[4]!!, density),
                toPixels(data[5]!!, density)
            )
            "newLatLngZoom" -> newLatLngZoom(
                toLatLng(data[1]), toFloat(
                    data[2]
                ).toDouble()
            )
            "scrollBy" -> {
                mapboxMap.scrollBy(
                    toFractionalPixels(data[1]!!, density), toFractionalPixels(
                        data[2]!!, density
                    )
                )
                null
            }
            "zoomBy" -> if (data.size == 2) {
                zoomBy(toFloat(data[1]).toDouble())
            } else {
                zoomBy(
                    toFloat(data[1]).toDouble(), toPoint(
                        data[2], density
                    )
                )
            }
            "zoomIn" -> zoomIn()
            "zoomOut" -> zoomOut()
            "zoomTo" -> zoomTo(toFloat(data[1]).toDouble())
            "bearingTo" -> bearingTo(toFloat(data[1]).toDouble())
            "tiltTo" -> tiltTo(toFloat(data[1]).toDouble())
            else -> throw IllegalArgumentException("Cannot interpret $o as CameraUpdate")
        }
    }

    fun toDouble(o: Any): Double {
        return (o as Number).toDouble()
    }

    fun toFloat(o: Any?): Float {
        return (o as Number?)!!.toFloat()
    }

    fun toFloatWrapper(o: Any?): Float? {
        return if (o == null) null else toFloat(o)
    }

    fun toInt(o: Any): Int {
        return (o as Number).toInt()
    }

    fun toJson(position: CameraPosition?): Any? {
        if (position == null) {
            return null
        }
        val data: MutableMap<String, Any> = HashMap()
        data["bearing"] = position.bearing
        data["target"] = toJson(position.target)
        data["tilt"] = position.tilt
        data["zoom"] = position.zoom
        return data
    }

    private fun toJson(latLng: LatLng?): Any {
        return listOf(latLng!!.latitude, latLng.longitude)
    }

    fun toLatLng(o: Any?): LatLng {
        val data = toList(o)
        return LatLng(
            toDouble(data!![0]!!), toDouble(
                data[1]!!
            )
        )
    }

    fun toLatLngBounds(o: Any?): LatLngBounds? {
        if (o == null) {
            return null
        }
        val data = toList(o)
        val boundsArray = arrayOf(
            toLatLng(
                data!![0]
            ), toLatLng(data[1])
        )
        val bounds = listOf(*boundsArray)
        val builder = LatLngBoundsBuilder()
        builder.includes(bounds)
        return builder.build()
    }

    fun toLatLngList(o: Any?, flippedOrder: Boolean): List<LatLng>? {
        if (o == null) {
            return null
        }
        val data = toList(o)
        val latLngList: MutableList<LatLng> = ArrayList()
        for (i in data!!.indices) {
            val coords = toList(data[i])
            if (flippedOrder) {
                latLngList.add(
                    LatLng(
                        toDouble(coords!![1]!!), toDouble(
                            coords[0]!!
                        )
                    )
                )
            } else {
                latLngList.add(
                    LatLng(
                        toDouble(coords!![0]!!), toDouble(
                            coords[1]!!
                        )
                    )
                )
            }
        }
        return latLngList
    }

    private fun toLatLngListList(o: Any?): List<List<LatLng>?>? {
        if (o == null) {
            return null
        }
        val data = toList(o)
        val latLngListList: MutableList<List<LatLng>?> = ArrayList()
        for (i in data!!.indices) {
            val latLngList = toLatLngList(
                data[i], false
            )
            latLngListList.add(latLngList)
        }
        return latLngListList
    }

    fun interpretListLatLng(geometry: List<List<LatLng>>): Polygon {
        val points: MutableList<List<Point>> = ArrayList(geometry.size)
        for (innerGeometry in geometry) {
            val innerPoints: MutableList<Point> = ArrayList(innerGeometry.size)
            for (latLng in innerGeometry) {
                innerPoints.add(
                    Point.fromLngLat(latLng.longitude, latLng.latitude)
                )
            }
            points.add(innerPoints)
        }
        return Polygon.fromLngLats(points)
    }

    fun toList(o: Any?): List<*>? {
        return o as List<*>?
    }

    fun toLong(o: Any): Long {
        return (o as Number).toLong()
    }

    @JvmStatic
    fun toMap(o: Any): Map<*, *> {
        return o as Map<*, *>
    }

    private fun toFractionalPixels(o: Any, density: Float): Float {
        return toFloat(o) * density
    }

    fun toPixels(o: Any, density: Float): Int {
        return toFractionalPixels(o, density).toInt()
    }

    private fun toPoint(o: Any?, density: Float): android.graphics.Point {
        val data = toList(o)
        return android.graphics.Point(
            toPixels(data!![0]!!, density), toPixels(
                data[1]!!, density
            )
        )
    }

    fun toString(o: Any): String {
        return o as String
    }

    @JvmStatic
    fun interpretMapboxMapOptions(o: Any, sink: MapboxMapOptionsSink, context: Context) {
        val metrics = context.resources.displayMetrics
        val data = toMap(o)
        val cameraTargetBounds = data["cameraTargetBounds"]
        if (cameraTargetBounds != null) {
            val targetData = toList(cameraTargetBounds)
            sink.setCameraTargetBounds(toLatLngBounds(targetData!![0]))
        }
        val compassEnabled = data["compassEnabled"]
        if (compassEnabled != null) {
            sink.setCompassEnabled(toBoolean(compassEnabled))
        }
        val styleString = data["styleString"]
        if (styleString != null) {
            sink.setStyleString(toString(styleString))
        }
        val minMaxZoomPreference = data["minMaxZoomPreference"]
        if (minMaxZoomPreference != null) {
            val zoomPreferenceData = toList(minMaxZoomPreference)
            sink.setMinMaxZoomPreference( //
                toFloatWrapper(zoomPreferenceData!![0]),  //
                toFloatWrapper(zoomPreferenceData[1])
            )
        }
        val rotateGesturesEnabled = data["rotateGesturesEnabled"]
        if (rotateGesturesEnabled != null) {
            sink.setRotateGesturesEnabled(toBoolean(rotateGesturesEnabled))
        }
        val scrollGesturesEnabled = data["scrollGesturesEnabled"]
        if (scrollGesturesEnabled != null) {
            sink.setScrollGesturesEnabled(toBoolean(scrollGesturesEnabled))
        }
        val tiltGesturesEnabled = data["tiltGesturesEnabled"]
        if (tiltGesturesEnabled != null) {
            sink.setTiltGesturesEnabled(toBoolean(tiltGesturesEnabled))
        }
        val trackCameraPosition = data["trackCameraPosition"]
        if (trackCameraPosition != null) {
            sink.setTrackCameraPosition(toBoolean(trackCameraPosition))
        }
        val zoomGesturesEnabled = data["zoomGesturesEnabled"]
        if (zoomGesturesEnabled != null) {
            sink.setZoomGesturesEnabled(toBoolean(zoomGesturesEnabled))
        }
        val myLocationEnabled = data["myLocationEnabled"]
        if (myLocationEnabled != null) {
            sink.setMyLocationEnabled(toBoolean(myLocationEnabled))
        }
        val myLocationTrackingMode = data["myLocationTrackingMode"]
        if (myLocationTrackingMode != null) {
            sink.setMyLocationTrackingMode(toInt(myLocationTrackingMode))
        }
        val myLocationRenderMode = data["myLocationRenderMode"]
        if (myLocationRenderMode != null) {
            sink.setMyLocationRenderMode(toInt(myLocationRenderMode))
        }
        val logoViewMargins = data["logoViewMargins"]
        if (logoViewMargins != null) {
            val logoViewMarginsData = toList(logoViewMargins)
            val point = toPoint(logoViewMarginsData, metrics.density)
            sink.setLogoViewMargins(point.x, point.y)
        }
        val compassGravity = data["compassViewPosition"]
        if (compassGravity != null) {
            sink.setCompassGravity(toInt(compassGravity))
        }
        val compassViewMargins = data["compassViewMargins"]
        if (compassViewMargins != null) {
            val compassViewMarginsData = toList(compassViewMargins)
            val point = toPoint(compassViewMarginsData, metrics.density)
            sink.setCompassViewMargins(point.x, point.y)
        }
        val attributionButtonGravity = data["attributionButtonPosition"]
        if (attributionButtonGravity != null) {
            sink.setAttributionButtonGravity(toInt(attributionButtonGravity))
        }
        val attributionButtonMargins = data["attributionButtonMargins"]
        if (attributionButtonMargins != null) {
            val attributionButtonMarginsData = toList(attributionButtonMargins)
            val point = toPoint(attributionButtonMarginsData, metrics.density)
            sink.setAttributionButtonMargins(point.x, point.y)
        }
    }
}