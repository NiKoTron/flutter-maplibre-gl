package com.mapbox.mapboxgl

import android.content.Context
import com.mapbox.mapboxgl.Convert.interpretMapboxMapOptions
import com.mapbox.mapboxgl.Convert.toBoolean
import com.mapbox.mapboxgl.Convert.toCameraPosition
import com.mapbox.mapboxgl.MapboxMapsPlugin.LifecycleProvider
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.StandardMessageCodec
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory

class MapboxMapFactory(
    private val messenger: BinaryMessenger, private val lifecycleProvider: LifecycleProvider
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {
    override fun create(context: Context, id: Int, args: Any?): PlatformView {
        val params = args as Map<String, Any>?
        val builder = MapboxMapBuilder()
        interpretMapboxMapOptions(params!!["options"]!!, builder, context)
        if (params.containsKey("initialCameraPosition")) {
            val position = toCameraPosition(params["initialCameraPosition"]!!)
            builder.setInitialCameraPosition(position)
        }
        if (params.containsKey("dragEnabled")) {
            val dragEnabled = toBoolean(params["dragEnabled"]!!)
            builder.setDragEnabled(dragEnabled)
        }
        return builder.build(id, context, messenger, lifecycleProvider)
    }
}