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
import androidx.annotation.RestrictTo
import me.alvince.android.httptrapdoor.HostElement
import me.alvince.android.httptrapdoor.TrapdoorLogger
import me.alvince.android.httptrapdoor.annotation.IoThread
import okio.Okio
import java.io.IOException
import java.util.*

/**
 * Static configuration parser on start-up
 *
 * Created by alvince on 2020/7/14
 *
 * @author alvince.zy@gmail.com
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class ConfigParser {

    private val allowListOfHostType = arrayOf("url", "dns")

    private val regexIp4Pattern =
        Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\$")

    @IoThread
    fun parse(context: Context): List<HostElement> {
        return try {
            context.assets.open("trapdoor_host_config.txt")
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
            ?.filter { it.isNotEmpty() && !(it.startsWith("#") or it.startsWith("//")) }
            ?.mapNotNull {
                TrapdoorLogger.iIfDebug(it)
                parseLine(it)
            }
            ?: emptyList()
    }

    /**
     * content should match that below
     * ```
     * {label},{tag},{host-url},{host-schema:`http`|`https`},{type-data:`url`|`dns:ip-address`}
     * ```
     */
    private fun parseLine(content: String): HostElement? {
        return content.takeIf { it.isNotEmpty() }
            ?.split(",")
            ?.let { sections ->
                when (sections.size) {
                    4 -> {
                        HostElement(sections[0], sections[1], sections[2], sections[3])
                    }
                    5 -> {
                        sections[4].toLowerCase(Locale.getDefault())
                            .let {
                                val slices = it.split(":")
                                if (allowListOfHostType.contains(slices[1]) &&
                                    (slices[0] == "dns").and(slices[1].matches(regexIp4Pattern))
                                ) {
                                    ModeSlice(slices[0], slices[1])
                                } else {
                                    ModeSlice("url", "")
                                }
                            }
                            .let { typeData ->
                                HostElement(
                                    sections[0],
                                    sections[1],
                                    sections[2],
                                    sections[3],
                                    typeData.mode
                                ).apply {
                                    if (typeData.data.isNotEmpty()) {
                                        inetAddress = typeData.data
                                    }
                                }
                            }
                    }
                    else -> {
                        if (sections.size == 3) {
                            HostElement(sections[0], sections[1], sections[2])
                        } else {
                            TrapdoorLogger.e("Invalid config line: $content", null)
                            null
                        }
                    }
                }
            }
    }

    private class ModeSlice(val mode: String, val data: String)

}
