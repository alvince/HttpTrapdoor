/*
 * Copyright (c) 2020 Alvince
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.alvince.android.httptrapdoor.buoy

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.os.Bundle
import android.util.SparseArray
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import me.alvince.android.httptrapdoor.Trapdoor
import me.alvince.android.httptrapdoor.TrapdoorLogger
import me.alvince.android.httptrapdoor.buoy.util.ActivityLifecycleCallbacksAdapter
import me.alvince.android.httptrapdoor.buoy.util.dipOf
import me.alvince.android.httptrapdoor.buoy.util.indicateHost

/**
 * Floating buoy of [Trapdoor]
 *
 * Created by alvince on 2020/7/16
 *
 * @author alvince.zy@gmail.com
 */
class TrapdoorBuoy(private val trapdoor: Trapdoor) {

    companion object {
        private val cTrapdoorBuoyPool = SparseArray<TrapdoorBuoy>()

        fun with(trapdoor: Trapdoor): TrapdoorBuoy =
            trapdoor.hashCode().let { key ->
                synchronized(this) {
                    cTrapdoorBuoyPool[key]
                        ?: TrapdoorBuoy(trapdoor).also { cTrapdoorBuoyPool.put(key, it) }
                }
            }
    }

    private val buoyViewPool by lazy { SparseArray<FloatingBuoyView>() }

    private val activitiesMonitor by lazy { ensureActivitiesMonitor() }

    private var disposed = false
    private var monitoring = false
    private var monitorApp: Application? = null

    fun monitor(application: Application) {
        synchronized(this) {
            if (monitoring) {
                return
            }
            monitorApp = application.also {
                it.registerActivityLifecycleCallbacks(activitiesMonitor)
            }
            monitoring = true
        }
    }

    fun stop() {
        if (!monitoring) {
            return
        }
        monitorApp?.unregisterActivityLifecycleCallbacks(activitiesMonitor)
        buoyViewPool.clear()
        monitoring = false
    }

    fun destroy() {
        if (disposed) {
            return
        }
        stop()
        monitorApp = null
        cTrapdoorBuoyPool.apply {
            indexOfValue(this@TrapdoorBuoy)
                .takeIf { it != -1 }
                ?.also { removeAt(it) }
        }
        disposed = true
    }

    private fun ensureActivitiesMonitor() =
        object : ActivityLifecycleCallbacksAdapter() {
            private val allowListForBuoy = mutableListOf<Int>()

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                activity.packageManager.resolveActivity(
                    Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    },
                    PackageManager.MATCH_DEFAULT_ONLY
                )?.takeIf {
                    it.activityInfo.name == activity.javaClass.name
                }
                    ?: run {
                        val hash = activity.hashCode()
                        allowListForBuoy.add(hash)
                        prepareBuoyWindow(activity).also { v ->
                            buoyViewPool.put(hash, v)
                        }
                    }
            }

            override fun onActivityResumed(activity: Activity) {
                activity.takeIf { allowListForBuoy.contains(it.hashCode()) }
                    ?.let { buoyViewPool.get(it.hashCode()) }
                    ?.also {
                        if (BuildConfig.DEBUG) {
                            TrapdoorLogger.i("Attach floating-buoy to $activity")
                        }
                        it.attach()
                        it.indicateHost(trapdoor.host()?.label)
                    }
            }

            override fun onActivityDestroyed(activity: Activity) {
                activity.hashCode().also { hash ->
                    allowListForBuoy.remove(hash)
                    buoyViewPool.also {
                        it.get(hash)?.detach()
                        it.remove(hash)
                    }
                }
            }
        }

    private fun prepareBuoyWindow(activity: Activity): FloatingBuoyView {
        return WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            flags = flags.or(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
            format = PixelFormat.TRANSLUCENT
            activity.dipOf(44F).toInt()
                .also {
                    width = it
                    height = it
                }
            gravity = Gravity.START.or(Gravity.TOP)
            y = activity.dipOf(96F).toInt()
        }.let { floatLp ->
            FloatingBuoyView(activity).apply {
                setOnClickListener {
                    alertHostListSelections(it)
                }
                floatAttrs = floatLp
            }
        }
    }

    private fun alertHostListSelections(view: View) {
        if (disposed) {
            Toast.makeText(view.context, "Floating-Buoy destroyed!", Toast.LENGTH_LONG).show()
            return
        }
        trapdoor.elements().takeIf { it.isNotEmpty() }
            ?.let { elements ->
                mutableListOf<Map<String, String>>().apply {
                    elements.forEach {
                        add(mapOf("label" to it.label, "content" to "${it.tag} - ${it.url}"))
                    }
                }
            }
            ?.let { list ->
                val adapter = SimpleAdapter(
                    view.context,
                    list,
                    android.R.layout.simple_list_item_activated_2,
                    arrayOf("label", "content"),
                    intArrayOf(android.R.id.text1, android.R.id.text2)
                )
                AlertDialog.Builder(view.context).setAdapter(adapter) { dialog, which ->
                    list.getOrNull(which)?.get("content")?.takeIf {
                        it.isNotEmpty()
                    }?.let {
                        it.split("-").firstOrNull()?.trim()
                    }?.takeIf { tag ->
                        trapdoor.host()?.tag != tag
                    }?.also { tag ->
                        trapdoor.select(tag)
                        (view as? TextView)?.indicateHost(trapdoor.host()?.label)
                    }
                    dialog.dismiss()
                }
            }
            ?.show()
    }

}

fun Trapdoor.enableFloatingBuoy(application: Application): TrapdoorBuoy {
    return TrapdoorBuoy.with(this)
        .also {
            if (BuildConfig.DEBUG) {
                TrapdoorLogger.i("enable floating-buoy with {$this}: $it")
            }
            it.monitor(application)
        }
}
