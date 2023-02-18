package com.mapbox.mapboxgl

import android.content.Context
import android.util.Log
import com.mapbox.mapboxsdk.net.ConnectivityReceiver
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterAssets
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry.Registrar
import java.io.*

internal class GlobalMethodHandler : MethodCallHandler {
    private val context: Context
    private val messenger: BinaryMessenger
    private var registrar: Registrar? = null
    private var flutterAssets: FlutterAssets? = null

    constructor(registrar: Registrar) {
        this.registrar = registrar
        context = registrar.activeContext()
        messenger = registrar.messenger()
    }

    constructor(binding: FlutterPluginBinding) {
        context = binding.applicationContext
        flutterAssets = binding.flutterAssets
        messenger = binding.binaryMessenger
    }

    override fun onMethodCall(methodCall: MethodCall, result: MethodChannel.Result) {
        MapBoxUtils.getMapbox(context)
        when (methodCall.method) {
            "installOfflineMapTiles" -> {
                val tilesDb = methodCall.argument<String>("tilesdb")
                installOfflineMapTiles(tilesDb)
                result.success(null)
            }
            "setOffline" -> {
                val offline = methodCall.argument<Boolean>("offline")!!
                ConnectivityReceiver.instance(context).setConnected(if (offline) false else null)
                result.success(null)
            }
            "mergeOfflineRegions" -> OfflineManagerUtils.mergeRegions(
                result,
                context,
                methodCall.argument("path")
            )
            "setOfflineTileCountLimit" -> OfflineManagerUtils.setOfflineTileCountLimit(
                result, context, methodCall.argument<Number>("limit")!!.toLong()
            )
            "setHttpHeaders" -> {
                val headers = methodCall.argument<Any>("headers") as Map<String, String>
                MapboxHttpRequestUtil.setHttpHeaders(headers, result)
            }
            "downloadOfflineRegion" -> {
                // Get args from caller
                val definitionMap = methodCall.argument<Any>("definition") as Map<String, Any>?
                val metadataMap = methodCall.argument<Any>("metadata") as Map<String?, Any>?
                val channelName = methodCall.argument<String>("channelName")

                // Prepare args
                val channelHandler = OfflineChannelHandlerImpl(messenger, channelName)

                // Start downloading
                OfflineManagerUtils.downloadRegion(
                    result, context, definitionMap!!, metadataMap, channelHandler
                )
            }
            "getListOfRegions" -> OfflineManagerUtils.regionsList(result, context)
            "updateOfflineRegionMetadata" -> {
                // Get download region arguments from caller
                val metadata = methodCall.argument<Any>("metadata") as Map<String?, Any>?
                OfflineManagerUtils.updateRegionMetadata(
                    result, context, methodCall.argument<Number>("id")!!.toLong(), metadata
                )
            }
            "deleteOfflineRegion" -> OfflineManagerUtils.deleteRegion(
                result, context, methodCall.argument<Number>("id")!!.toLong()
            )
            else -> result.notImplemented()
        }
    }

    private fun installOfflineMapTiles(tilesDb: String?) {
        val dest = File(context.filesDir, DATABASE_NAME)
        try {
            openTilesDbFile(tilesDb).use { input ->
                FileOutputStream(dest).use { output ->
                    copy(
                        input,
                        output
                    )
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    private fun openTilesDbFile(tilesDb: String?): InputStream {
        return if (tilesDb!!.startsWith("/")) { // Absolute path.
            FileInputStream(File(tilesDb))
        } else {
            val assetKey: String = if (registrar != null) {
                registrar!!.lookupKeyForAsset(tilesDb)
            } else if (flutterAssets != null) {
                flutterAssets!!.getAssetFilePathByName(tilesDb)
            } else {
                throw IllegalStateException()
            }
            context.assets.open(assetKey)
        }
    }

    companion object {
        private val TAG = GlobalMethodHandler::class.java.simpleName
        private const val DATABASE_NAME = "mbgl-offline.db"
        private const val BUFFER_SIZE = 1024 * 2
        @Throws(IOException::class)
        private fun copy(input: InputStream, output: OutputStream) {
            val buffer = ByteArray(BUFFER_SIZE)
            val `in` = BufferedInputStream(input, BUFFER_SIZE)
            val out = BufferedOutputStream(output, BUFFER_SIZE)
            var count = 0
            var n = 0
            try {
                while (`in`.read(buffer, 0, BUFFER_SIZE).also { n = it } != -1) {
                    out.write(buffer, 0, n)
                    count += n
                }
                out.flush()
            } finally {
                try {
                    out.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
                try {
                    `in`.close()
                } catch (e: IOException) {
                    Log.e(TAG, e.message, e)
                }
            }
        }
    }
}