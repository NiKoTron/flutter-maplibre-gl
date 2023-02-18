package com.mapbox.mapboxgl

import com.mapbox.mapboxsdk.module.http.HttpRequestUtil
import io.flutter.plugin.common.MethodChannel
//import java.util.Map
import okhttp3.OkHttpClient
import okhttp3.Request

internal object MapboxHttpRequestUtil {
    fun setHttpHeaders(headers: Map<String, String>, result: MethodChannel.Result) {
        HttpRequestUtil.setOkHttpClient(getOkHttpClient(headers, result).build())
        result.success(null)
    }

    private fun getOkHttpClient(
        headers: Map<String, String>, result: MethodChannel.Result
    ): OkHttpClient.Builder {
        return try {
            OkHttpClient.Builder()
                .addNetworkInterceptor { chain ->
                    val builder: Request.Builder = chain.request().newBuilder()
                    for (header in headers.entries) {
                        if (header.key.trim().isEmpty()) {
                            continue
                        }
                        if (header.value.trim().isEmpty()) {
                            builder.removeHeader(header.key)
                        } else {
                            builder.header(header.key, header.value)
                        }
                    }
                    chain.proceed(builder.build())
                }
        } catch (e: Exception) {
            result.error(
                "OK_HTTP_CLIENT_ERROR",
                "An unexcepted error happened during creating http " + "client" + e.message,
                null
            )
            throw RuntimeException(e)
        }
    }
}