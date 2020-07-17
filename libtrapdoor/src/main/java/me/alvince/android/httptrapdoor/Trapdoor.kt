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

package me.alvince.android.httptrapdoor

import android.util.SparseArray
import me.alvince.android.httptrapdoor.okhttp.HttpCallFactoryProxy
import me.alvince.android.httptrapdoor.okhttp.interceptor.NetworkTrafficLogInterceptor
import okhttp3.Call
import okhttp3.OkHttpClient

/**
 * Trapper is an enhanced [OkHttpClient] holder
 *
 * It support dynamic request proxy for replace host configured
 *
 * Created by alvince on 2020/7/14
 *
 * @author alvince.zy@gmail.com
 */
class Trapdoor private constructor(private val source: OkHttpClient) {

    companion object {
        const val TRAPDOOR_TAG = "Trapdoor"

        private val cTrapdoorPool = SparseArray<Trapdoor>()

        fun with(client: OkHttpClient): Trapdoor =
            client.hashCode().let { key ->
                synchronized(this) {
                    cTrapdoorPool[key]
                        ?: Trapdoor(client).also { cTrapdoorPool.put(key, it) }
                }
            }
    }

    private val instrumentation = TrapdoorInstrumentation.obtain()
    private val options = Options()

    /**
     * Config custom extension [HostElement] elements for this [Trapdoor] only
     */
    fun customConfig(vararg elements: HostElement): Trapdoor {
        elements.takeIf { it.isNotEmpty() }
            ?.also {
                instrumentation.extensionalConfig(it)
            }
        return this
    }

    /**
     * Enable http-request log print by default-implementation
     */
    fun enableHttpLog(): Trapdoor {
        options.withLog = true
        return this
    }

    /**
     * Get all [HostElement] configured as a read-only [List]
     */
    fun elements(): List<HostElement> {
        return instrumentation.hostElements()
    }

    /**
     * Generate the final [Call.Factory] for requests
     */
    fun factory(): Call.Factory {
        return source.newBuilder().apply {
            if (options.withLog) {
                addNetworkInterceptor(NetworkTrafficLogInterceptor())
            }
        }.let { builder ->
            HttpCallFactoryProxy.create(builder.build(), instrumentation)
                .also {
                    if (BuildConfig.DEBUG) {
                        TrapdoorLogger.i("Create call-factory: $it by trapdoor{$this} with instrumentation{$instrumentation} ")
                    }
                }
        }
    }

    /**
     * Retrieves current [HostElement] selected
     */
    fun host(): HostElement? = instrumentation.currentHost()

    /**
     * Change select host by [HostElement.tag]
     */
    fun select(tag: String) {
        tag.takeIf { it.isNotEmpty() }
            ?.also {
                if (BuildConfig.DEBUG) {
                    TrapdoorLogger.i("select host [$tag] by instrumentation{$instrumentation} from {$this}")
                }
                instrumentation.pick(it)
            }
    }

    fun release() {
        instrumentation.clear()
        cTrapdoorPool.apply {
            indexOfValue(this@Trapdoor)
                .takeIf { it != -1 }
                ?.also { removeAt(it) }
        }
    }


    private class Options {
        var withLog: Boolean = false
    }

}
