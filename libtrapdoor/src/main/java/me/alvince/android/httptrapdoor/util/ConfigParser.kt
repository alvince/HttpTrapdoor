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

package me.alvince.android.httptrapdoor.util

import android.content.Context
import me.alvince.android.httptrapdoor.BuildConfig
import me.alvince.android.httptrapdoor.HostElement
import me.alvince.android.httptrapdoor.TrapdoorLogger
import okio.Okio
import java.io.IOException

/**
 * Static configuration parser on start-up
 *
 * Created by alvince on 2020/7/14
 *
 * @author alvince.zy@gmail.com
 */
internal class ConfigParser {

    fun parse(context: Context): List<HostElement> {
        return try {
            context.assets.open("trapdoor_host_config.cfg")
        } catch (ex: IOException) {
            TrapdoorLogger.e("Fail to parse host config", ex)
            null
        }?.let {
            mutableListOf<String>().apply {
                Okio.buffer(Okio.source(it)).use { buffer ->
                    var tmp: String?
                    try {
                        do {
                            tmp = buffer.readUtf8Line()
                            tmp?.takeIf { it.isNotEmpty() }
                                ?.also { add(it) }
                        } while (!tmp.isNullOrEmpty())
                    } catch (ex: IOException) {
                        // interrupt load
                        TrapdoorLogger.e("Read config error.", ex)
                    }
                }
            }
        }
            ?.filter { it.isNotEmpty() }
            ?.mapNotNull {
                if (BuildConfig.DEBUG) {
                    TrapdoorLogger.i(it)
                }
                it.split(",")
                    .takeIf { sections -> sections.size >= 4 }
                    ?.let { sections ->
                        HostElement(sections[0], sections[1], sections[2], sections[3])
                    }
            }
            ?: emptyList()
    }

}
