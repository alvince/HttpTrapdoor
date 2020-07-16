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
import androidx.appcompat.app.AlertDialog
import me.alvince.android.httptrapdoor.Trapdoor
import me.alvince.android.httptrapdoor.buoy.util.dipOf

/**
 * Floating buoy of [Trapdoor]
 *
 * Created by alvince on 2020/7/16
 *
 * @author alvince.zy@gmail.com
 */
class TrapdoorBuoy(private val trapdoor: Trapdoor) {

    companion object {
        fun with(trapdoor: Trapdoor): TrapdoorBuoy {
            return TrapdoorBuoy(trapdoor)
        }
    }

    private val buoyViewPool by lazy { SparseArray<FloatingBuoyView>() }

    fun monitor(application: Application) {
        application.registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacksAdapter() {
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
                        it.floatAttrs?.apply {
                            x = activity.window.attributes.width.minus(width + activity.dipOf(4F).toInt())
                            y = activity.dipOf(64F).toInt()
                        }
                        it.attach()
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
        })
    }

    private fun prepareBuoyWindow(activity: Activity): FloatingBuoyView {
        val floatLp = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_PANEL
            format = PixelFormat.RGBA_8888
            activity.dipOf(40F).toInt()
                .also {
                    width = it
                    height = it
                }
            gravity = Gravity.START.or(Gravity.TOP)
            flags = flags.or(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        }
        return FloatingBuoyView(activity).apply {
            setOnClickListener {
                alertHostListSelections(it)
            }
            floatAttrs = floatLp
        }
    }

    private fun alertHostListSelections(view: View) {
        trapdoor.elements().takeIf { it.isNotEmpty() }
            ?.let { elements ->
                mutableListOf<Map<String, String>>().apply {
                    elements.forEach {
                        add(mapOf("label" to it.name, "tag" to it.tag))
                    }
                }
            }
            ?.let { list ->
                val adapter = SimpleAdapter(
                    view.context,
                    list,
                    android.R.layout.simple_list_item_activated_2,
                    arrayOf("label", "tag"),
                    intArrayOf(android.R.id.text1, android.R.id.text2)
                )
                AlertDialog.Builder(view.context).setAdapter(adapter) { dialog, which ->
                    list.getOrNull(which)?.get("tag")?.takeIf { tag ->
                        trapdoor.host()?.tag != tag
                    }?.also { tag ->
                        trapdoor.select(tag)
                        view.post {
                            (view as? TextView)?.text = trapdoor.host()?.name ?: "N"
                        }
                    }
                    dialog.dismiss()
                }
            }
            ?.show()
    }

}

fun Trapdoor.enableFloatingBuoy(application: Application) {
    TrapdoorBuoy.with(this).monitor(application)
}
