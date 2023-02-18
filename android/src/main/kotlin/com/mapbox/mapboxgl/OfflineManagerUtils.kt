package com.mapbox.mapboxgl

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.geometry.LatLngBounds
import com.mapbox.mapboxsdk.geometry.LatLngBounds.Builder
import com.mapbox.mapboxsdk.offline.*
import com.mapbox.mapboxsdk.offline.OfflineManager.Companion.getInstance
import io.flutter.plugin.common.MethodChannel
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

internal object OfflineManagerUtils {
    private const val TAG = "OfflineManagerUtils"
    fun mergeRegions(result: MethodChannel.Result?, context: Context?, path: String?) {
        getInstance(context!!)
            .mergeOfflineRegions(
                path!!,
                object : OfflineManager.MergeOfflineRegionsCallback {
                    override fun onMerge(offlineRegions: Array<OfflineRegion>?) {
                        if (result == null) return
                        val regionsArgs: MutableList<Map<String?, Any?>> = ArrayList()
                        for (offlineRegion in offlineRegions!!) {
                            regionsArgs.add(offlineRegionToMap(offlineRegion))
                        }
                        val json = Gson().toJson(regionsArgs)
                        result.success(json)
                    }

                    override fun onError(error: String) {
                        if (result == null) return
                        result.error("mergeOfflineRegions Error", error, null)
                    }
                })
    }

    fun setOfflineTileCountLimit(result: MethodChannel.Result, context: Context?, limit: Long) {
        getInstance(context!!).setOfflineMapboxTileCountLimit(limit)
        result.success(null)
    }

    fun downloadRegion(
        result: MethodChannel.Result,
        context: Context,
        definitionMap: Map<String, Any>,
        metadataMap: Map<String?, Any?>?,
        channelHandler: OfflineChannelHandlerImpl
    ) {
        val pixelDensity = context.resources.displayMetrics.density
        val definition = mapToRegionDefinition(definitionMap, pixelDensity)
        var metadata = "{}"
        if (metadataMap != null) {
            metadata = Gson().toJson(metadataMap)
        }
        val isComplete = AtomicBoolean(false)
        // Download region
        getInstance(context)
            .createOfflineRegion(
                definition,
                metadata.toByteArray(),
                object : OfflineManager.CreateOfflineRegionCallback {
                    private var _offlineRegion: OfflineRegion? = null
                    override fun onCreate(offlineRegion: OfflineRegion) {
                        val regionData: Map<String?, Any?> = offlineRegionToMap(offlineRegion)
                        result.success(Gson().toJson(regionData))
                        _offlineRegion = offlineRegion
                        // Start downloading region
                        _offlineRegion!!.setDownloadState(OfflineRegion.STATE_ACTIVE)
                        channelHandler.onStart()
                        // Observe downloading state
                        val observer: OfflineRegion.OfflineRegionObserver = object :
                            OfflineRegion.OfflineRegionObserver {
                            override fun onStatusChanged(status: OfflineRegionStatus) {
                                // Calculate progress of
                                // downloading
                                val progress = calculateDownloadingProgress(
                                    status.requiredResourceCount,
                                    status.completedResourceCount
                                )
                                // Check if downloading is
                                // complete
                                if (status.isComplete) {
                                    Log.i(TAG, "Region " + "downloaded " + "successfully.")
                                    // Reset downloading state
                                    _offlineRegion!!.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                    // This can be called
                                    // multiple times, and
                                    // result can be called
                                    // only once,
                                    // so there is need to
                                    // prevent it
                                    if (isComplete.get()) return
                                    isComplete.set(true)
                                    channelHandler.onSuccess()
                                } else {
                                    Log.i(TAG, "Region download progress = $progress")
                                    channelHandler.onProgress(progress)
                                }
                            }

                            override fun onError(error: OfflineRegionError) {
                                Log.e(TAG, "onError reason: " + error.reason)
                                Log.e(TAG, "onError message: " + error.message)
                                // Reset downloading state
                                _offlineRegion!!.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                isComplete.set(true)
                                channelHandler.onError(
                                    "Downloading error", error.message, error.reason
                                )
                            }

                            override fun mapboxTileCountLimitExceeded(limit: Long) {
                                Log.e(TAG, "Mapbox tile count limit exceeded: $limit")
                                // Reset downloading state
                                _offlineRegion!!.setDownloadState(OfflineRegion.STATE_INACTIVE)
                                isComplete.set(true)
                                channelHandler.onError(
                                    "mapboxTileCountLimitExceeded",
                                    "Mapbox tile count limit exceeded: $limit",
                                    null
                                )
                                // Mapbox even after crash
                                // and not downloading fully
                                // region still keeps part
                                // of it in database, so we
                                // have to remove it
                                deleteRegion(null, context, _offlineRegion!!.id)
                            }
                        }
                        _offlineRegion!!.setObserver(observer)
                    }

                    /**
                     * This will be call if given region definition is invalid
                     *
                     * @param error
                     */
                    override fun onError(error: String) {
                        Log.e(TAG, "Error: $error")
                        // Reset downloading state
                        _offlineRegion!!.setDownloadState(OfflineRegion.STATE_INACTIVE)
                        channelHandler.onError("mapboxInvalidRegionDefinition", error, null)
                        result.error("mapboxInvalidRegionDefinition", error, null)
                    }
                })
    }

    fun regionsList(result: MethodChannel.Result, context: Context?) {
        getInstance(context!!)
            .listOfflineRegions(
                object : OfflineManager.ListOfflineRegionsCallback {
                    override fun onList(offlineRegions: Array<OfflineRegion>?) {
                        val regionsArgs: MutableList<Map<String?, Any?>> = ArrayList()
                        for (offlineRegion in offlineRegions!!) {
                            regionsArgs.add(offlineRegionToMap(offlineRegion))
                        }
                        result.success(Gson().toJson(regionsArgs))
                    }

                    override fun onError(error: String) {
                        result.error("RegionListError", error, null)
                    }
                })
    }

    fun updateRegionMetadata(
        result: MethodChannel.Result?, context: Context?, id: Long, metadataMap: Map<String?, Any?>?
    ) {
        getInstance(context!!)
            .listOfflineRegions(
                object : OfflineManager.ListOfflineRegionsCallback {
                    override fun onList(offlineRegions: Array<OfflineRegion>?) {
                        for (offlineRegion in offlineRegions!!) {
                            if (offlineRegion.id != id) continue
                            var metadata = "{}"
                            if (metadataMap != null) {
                                metadata = Gson().toJson(metadataMap)
                            }
                            offlineRegion.updateMetadata(
                                metadata.toByteArray(),
                                object : OfflineRegion.OfflineRegionUpdateMetadataCallback {
                                    override fun onUpdate(metadataBytes: ByteArray) {
                                        val regionData = offlineRegionToMap(offlineRegion)
                                        regionData["metadata"] = metadataBytesToMap(metadataBytes)
                                        if (result == null) return
                                        result.success(Gson().toJson(regionData))
                                    }

                                    override fun onError(error: String) {
                                        if (result == null) return
                                        result.error("UpdateMetadataError", error, null)
                                    }
                                })
                            return
                        }
                        if (result == null) return
                        result.error(
                            "UpdateMetadataError",
                            "There is no " + "region with given id to " + "update.",
                            null
                        )
                    }

                    override fun onError(error: String) {
                        if (result == null) return
                        result.error("RegionListError", error, null)
                    }
                })
    }

    fun deleteRegion(result: MethodChannel.Result?, context: Context?, id: Long) {
        getInstance(context!!)
            .listOfflineRegions(
                object : OfflineManager.ListOfflineRegionsCallback {
                    override fun onList(offlineRegions: Array<OfflineRegion>?) {
                        for (offlineRegion in offlineRegions!!) {
                            if (offlineRegion.id != id) continue
                            offlineRegion.delete(
                                object : OfflineRegion.OfflineRegionDeleteCallback {
                                    override fun onDelete() {
                                        if (result == null) return
                                        result.success(null)
                                    }

                                    override fun onError(error: String) {
                                        if (result == null) return
                                        result.error("DeleteRegionError", error, null)
                                    }
                                })
                            return
                        }
                        if (result == null) return
                        result.error(
                            "DeleteRegionError",
                            "There is no " + "region with given id to " + "delete.",
                            null
                        )
                    }

                    override fun onError(error: String) {
                        if (result == null) return
                        result.error("RegionListError", error, null)
                    }
                })
    }

    private fun calculateDownloadingProgress(
        requiredResourceCount: Long, completedResourceCount: Long
    ): Double {
        return if (requiredResourceCount > 0) 100.0 * completedResourceCount / requiredResourceCount else 0.0
    }

    private fun mapToRegionDefinition(
        map: Map<String, Any>, pixelDensity: Float
    ): OfflineRegionDefinition {
        for ((key, value) in map) {
            Log.d(TAG, key)
            Log.d(TAG, value.toString())
        }
        // Create a bounding box for the offline region
        return OfflineTilePyramidRegionDefinition(
            map["mapStyleUrl"] as String?,
            listToBounds(map["bounds"] as List<List<Double>>?),
            (map["minZoom"] as Number?)!!.toDouble(),
            (map["maxZoom"] as Number?)!!.toDouble(),
            pixelDensity,
            (map["includeIdeographs"] as Boolean?)!!
        )
    }

    private fun listToBounds(bounds: List<List<Double>>?): LatLngBounds {
        return Builder()
            .include(LatLng(bounds!![1][0], bounds[1][1])) // Northeast
            .include(LatLng(bounds[0][0], bounds[0][1])) // Southwest
            .build()
    }

    private fun offlineRegionToMap(region: OfflineRegion): MutableMap<String?, Any?> {
        val result: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        result["id"] = region.id
        result["definition"] = offlineRegionDefinitionToMap(region.definition)
        result["metadata"] = metadataBytesToMap(region.metadata)
        return result
    }

    private fun offlineRegionDefinitionToMap(
        definition: OfflineRegionDefinition
    ): Map<String?, Any?> {
        val result: MutableMap<String?, Any?> = HashMap<String?, Any?>()
        result["mapStyleUrl"] = definition.styleURL
        result["bounds"] = boundsToList(definition.bounds)
        result["minZoom"] = definition.minZoom
        result["maxZoom"] = definition.maxZoom
        result["includeIdeographs"] = definition.includeIdeographs
        return result
    }

    private fun boundsToList(bounds: LatLngBounds?): List<List<Double>> {
        val boundsList: MutableList<List<Double>> = ArrayList()
        val northeast = listOf(
            bounds!!.getLatNorth(), bounds.getLonEast()
        )
        val southwest = listOf(
            bounds.getLatSouth(), bounds.getLonWest()
        )
        boundsList.add(southwest)
        boundsList.add(northeast)
        return boundsList
    }

    private fun metadataBytesToMap(metadataBytes: ByteArray?): Map<String?, Any?> {
        return if (metadataBytes != null) {
            Gson().fromJson(
                String(metadataBytes),
                HashMap::class.java
            ) as HashMap<String?, Any?>
        } else HashMap<String?, Any?>()
    }
}