package com.mapbox.mapboxgl

import android.net.Uri
import com.google.gson.Gson
import com.mapbox.geojson.FeatureCollection
import com.mapbox.mapboxsdk.geometry.LatLngQuad
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.style.sources.*
import java.net.URI
import java.net.URISyntaxException

internal object SourcePropertyConverter {
    private const val TAG = "SourcePropertyConverter"
    fun buildTileset(data: Map<String?, Any?>): TileSet? {
        val tiles = data["tiles"] ?: return null
        val tilesArray = Convert.toList(tiles) as List<String>
        val t = tilesArray.toTypedArray()

        // options are only valid with tiles
        val tileSet =
            TileSet("2.1.0", *t)
        val bounds = data["bounds"]
        if (bounds != null) {
            val floats = (bounds as List<Double>).map { it -> it.toFloat() }.toFloatArray()

            tileSet.setBounds(*floats)

            //tileSet.setBounds(boundsFloat[0], boundsFloat[1], boundsFloat[2], boundsFloat[3])
        }
        val scheme = data["scheme"]
        if (scheme != null) {
            tileSet.scheme = Convert.toString(scheme)
        }
        val minzoom = data["minzoom"]
        if (minzoom != null) {
            tileSet.setMinZoom(Convert.toFloat(minzoom))
        }
        val maxzoom = data["maxzoom"]
        if (maxzoom != null) {
            tileSet.setMaxZoom(Convert.toFloat(maxzoom))
        }
        val attribution = data["attribution"]
        if (attribution != null) {
            tileSet.attribution = Convert.toString(attribution)
        }
        return tileSet
    }

    fun buildGeojsonOptions(data: Map<String?, Any?>): GeoJsonOptions {
        var options = GeoJsonOptions()
        val buffer = data["buffer"]
        if (buffer != null) {
            options = options.withBuffer(Convert.toInt(buffer))
        }
        val cluster = data["cluster"]
        if (cluster != null) {
            options = options.withCluster(Convert.toBoolean(cluster))
        }
        val clusterMaxZoom = data["clusterMaxZoom"]
        if (clusterMaxZoom != null) {
            options = options.withClusterMaxZoom(Convert.toInt(clusterMaxZoom))
        }
        val clusterRadius = data["clusterRadius"]
        if (clusterRadius != null) {
            options = options.withClusterRadius(Convert.toInt(clusterRadius))
        }
        val lineMetrics = data["lineMetrics"]
        if (lineMetrics != null) {
            options = options.withLineMetrics(Convert.toBoolean(lineMetrics))
        }
        val maxZoom = data["maxZoom"]
        if (maxZoom != null) {
            options = options.withMaxZoom(Convert.toInt(maxZoom))
        }
        val minZoom = data["minZoom"]
        if (minZoom != null) {
            options = options.withMinZoom(Convert.toInt(minZoom))
        }
        val tolerance = data["tolerance"]
        if (tolerance != null) {
            options = options.withTolerance(Convert.toFloat(tolerance))
        }
        return options
    }

    fun buildGeojsonSource(id: String?, properties: Map<String?, Any?>): GeoJsonSource? {
        val data = properties["data"]
        val options = buildGeojsonOptions(properties)
        if (data != null) {
            if (data is String) {
                try {
                    val uri = URI(Convert.toString(data))
                    return GeoJsonSource(id, uri, options)
                } catch (e: URISyntaxException) {
                }
            } else {
                val gson = Gson()
                val geojson = gson.toJson(data)
                val featureCollection = FeatureCollection.fromJson(geojson)
                return GeoJsonSource(id, featureCollection, options)
            }
        }
        return null
    }

    fun buildImageSource(id: String?, properties: Map<String?, Any?>): ImageSource? {
        val url = properties["url"]
        val coordinates = Convert.toLatLngList(properties["coordinates"], true)
        val quad = LatLngQuad(
            coordinates!![0], coordinates[1], coordinates[2], coordinates[3]
        )
        try {
            val uri = URI(url?.let { Convert.toString(it) })
            return ImageSource(id, quad, uri)
        } catch (e: URISyntaxException) {
        }
        return null
    }

    fun buildVectorSource(id: String?, properties: Map<String?, Any?>): VectorSource? {
        val url = properties["url"]
        if (url != null) {
            val uri = Uri.parse(Convert.toString(url))
            return uri?.let { VectorSource(id, it) }
        }
        val tileSet = buildTileset(properties)
        return tileSet?.let { VectorSource(id, it) }
    }

    fun buildRasterSource(id: String?, properties: Map<String?, Any?>): RasterSource? {
        val url = properties["url"]
        val tileSizeObj = properties["tileSize"]
        if (url != null) {
            val uri = Convert.toString(url)
            return if (tileSizeObj != null) {
                val tileSize = Convert.toInt(tileSizeObj)
                RasterSource(id, uri, tileSize)
            } else {
                RasterSource(id, uri)
            }
        }
        val tileSet = buildTileset(properties)
        return if (tileSet != null) {
            if (tileSizeObj != null) {
                val tileSize = Convert.toInt(tileSizeObj)
                RasterSource(id, tileSet, tileSize)
            } else {
                RasterSource(id, tileSet)
            }
        } else {
            null
        }
    }

    fun buildRasterDemSource(id: String?, properties: Map<String?, Any?>): RasterDemSource? {
        val url = properties["url"]
        if (url != null) {
            try {
                val uri = URI(Convert.toString(url))
                return RasterDemSource(id, uri)
            } catch (e: URISyntaxException) {
            }
        }
        val tileSet = buildTileset(properties)
        return tileSet?.let { RasterDemSource(id, it) }
    }

    fun addSource(id: String?, properties: Map<String?, Any?>, style: Style) {
        val type = properties["type"]
        var source: Source? = null
        if (type != null) {
            when (Convert.toString(type)) {
                "vector" -> source = buildVectorSource(id, properties)
                "raster" -> source = buildRasterSource(id, properties)
                "raster-dem" -> source = buildRasterDemSource(id, properties)
                "image" -> source = buildImageSource(id, properties)
                "geojson" -> source = buildGeojsonSource(id, properties)
                else -> {}
            }
        }
        if (source != null) {
            style.addSource(source)
        }
    }
}