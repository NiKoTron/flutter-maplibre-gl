// Copyright 2018 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
package com.mapbox.mapboxgl

import android.app.Activity
import android.app.Application.ActivityLifecycleCallbacks
import android.os.Bundle
import androidx.annotation.NonNull
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterAssets
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.embedding.engine.plugins.lifecycle.HiddenLifecycleReference
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.PluginRegistry.Registrar

/**
 * Plugin for controlling a set of MapboxMap views to be shown as overlays on top of the Flutter
 * view. The overlay should be hidden during transformations or while Flutter is rendering on top of
 * the map. A Texture drawn using MapboxMap bitmap snapshots can then be shown instead of the
 * overlay.
 */
class MapboxMapsPlugin : FlutterPlugin, ActivityAware {
    private var lifecycle: Lifecycle? = null

    private lateinit var methodChannel : MethodChannel

    // New Plugin APIs
    override fun onAttachedToEngine(@NonNull binding: FlutterPluginBinding) {
        flutterAssets = binding.flutterAssets
        methodChannel = MethodChannel(binding.binaryMessenger, "plugins.flutter.io/mapbox_gl")
        methodChannel.setMethodCallHandler(GlobalMethodHandler(binding))
        binding
            .platformViewRegistry
            .registerViewFactory(
                "plugins.flutter.io/mapbox_gl",
                MapboxMapFactory(
                    binding.binaryMessenger,
                    object : LifecycleProvider {
                        override fun getLifecycle(): Lifecycle? {
                            return lifecycle
                        }
                    })
            )
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(@NonNull binding: ActivityPluginBinding) {
        lifecycle = FlutterLifecycleAdapter.getActivityLifecycle(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(@NonNull binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onDetachedFromActivity() {
        lifecycle = null
    }


    class ProxyLifecycleProvider(activity: Activity) :
        ActivityLifecycleCallbacks, LifecycleOwner, LifecycleProvider {
        private val lifecycle = LifecycleRegistry(this)
        private val registrarActivityHashCode: Int

        init {
            registrarActivityHashCode = activity.hashCode()
            activity.application.registerActivityLifecycleCallbacks(this)
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            if (activity.hashCode() != registrarActivityHashCode) {
                return
            }
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        override fun onActivityStarted(activity: Activity) {
            if (activity.hashCode() != registrarActivityHashCode) {
                return
            }
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
        }

        override fun onActivityResumed(activity: Activity) {
            if (activity.hashCode() != registrarActivityHashCode) {
                return
            }
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        override fun onActivityPaused(activity: Activity) {
            if (activity.hashCode() != registrarActivityHashCode) {
                return
            }
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        }

        override fun onActivityStopped(activity: Activity) {
            if (activity.hashCode() != registrarActivityHashCode) {
                return
            }
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        }

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {
            if (activity.hashCode() != registrarActivityHashCode) {
                return
            }
            activity.application.unregisterActivityLifecycleCallbacks(this)
            lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }

        override fun getLifecycle(): Lifecycle {
            return lifecycle
        }
    }

    interface LifecycleProvider {
        fun getLifecycle(): Lifecycle?
    }

    /** Provides a static method for extracting lifecycle objects from Flutter plugin bindings.  */
    object FlutterLifecycleAdapter {
        /**
         * Returns the lifecycle object for the activity a plugin is bound to.
         *
         *
         * Returns null if the Flutter engine version does not include the lifecycle extraction code.
         * (this probably means the Flutter engine version is too old).
         */
        fun getActivityLifecycle(
            activityPluginBinding: ActivityPluginBinding
        ): Lifecycle {
            val reference = activityPluginBinding.lifecycle as HiddenLifecycleReference
            return reference.lifecycle
        }
    }

    companion object {
        private const val VIEW_TYPE = "plugins.flutter.io/mapbox_gl"
        var flutterAssets: FlutterAssets? = null

        // Old Plugin APIs
        fun registerWith(registrar: Registrar) {
            val activity = registrar.activity()
                ?: // When a background flutter view tries to register the plugin, the registrar has no activity.
// We stop the registration process as this plugin is foreground only.
                return
            if (activity is LifecycleOwner) {
                registrar
                    .platformViewRegistry()
                    .registerViewFactory(
                        VIEW_TYPE,
                        MapboxMapFactory(
                            registrar.messenger(),
                            object : LifecycleProvider {
                                override fun getLifecycle(): Lifecycle? {
                                    return (activity as LifecycleOwner).lifecycle
                                }
                            })
                    )
            } else {
                registrar
                    .platformViewRegistry()
                    .registerViewFactory(
                        VIEW_TYPE,
                        MapboxMapFactory(registrar.messenger(), ProxyLifecycleProvider(activity))
                    )
            }
            val methodChannel = MethodChannel(registrar.messenger(), "plugins.flutter.io/mapbox_gl")
            methodChannel.setMethodCallHandler(GlobalMethodHandler(registrar))
        }
    }
}