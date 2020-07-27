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
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
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
        var elements = try {
            context.assets.open("trapdoor_host_config.json")
        } catch (ex: IOException) {
            TrapdoorLogger.e("Fail to parse host config json", ex)
            null
        }?.let {
            Okio.buffer(Okio.source(it)).use { buffer ->
                val text = buffer.readString(Charset.defaultCharset())
                JSONArray(text)
            }.let { json -> parseJson(json) }
        }

        if (elements.isNullOrEmpty()) {
            elements = try {
                context.assets.open("trapdoor_host_config.txt")
            } catch (ex: IOException) {
                TrapdoorLogger.e("Fail to parse host config", ex)
                null
            }?.let { parsePlain(it) }
        }

        return elements ?: emptyList()
    }

    private fun parseJson(src: JSONArray): List<HostElement> {
        return src.let {
            mutableListOf<JSONObject>().apply {
                for (pos in 0 until src.length()) {
                    add(src.optJSONObject(pos))
                }
            }
        }
            .takeIf { it.isNotEmpty() }
            ?.mapNotNull { json ->
                try {
                    json.toHostElement()
                } catch (ex: IllegalArgumentException) {
                    null
                }
            }
            ?: emptyList()
    }

    private fun parsePlain(input: InputStream): List<HostElement> {
        return mutableListOf<String>().apply {
            Okio.buffer(Okio.source(input)).use { buffer ->
                var tmp: String?
                try {
                    do {
                        tmp = buffer.readUtf8Line()
                        tmp?.takeIf { it.isNotEmpty() }
                            ?.also { add(it.trim()) }
                    } while (!tmp.isNullOrEmpty())
                } catch (ex: IOException) {
                    // interrupt load
                    TrapdoorLogger.e("Read config error.", ex)
                }
            }
        }
            .filter { it.isNotEmpty() && !(it.startsWith("#") or it.startsWith("//")) }
            .mapNotNull {
                TrapdoorLogger.iIfDebug(it)
                parseLine(it)
            }
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

    private fun JSONObject.toHostElement(): HostElement? {
        val label = this.optString("label")
        val tag = this.optString("tag")
        val host = this.optString("host")
        val scheme = this.optString("scheme", "http")
        var mode = this.optString("mode", "url")

        require(label.isNotEmpty() && tag.isNotEmpty() && host.isNotEmpty()) { "Illegal json-element: $this" }

        var inetAddresses = arrayOf("")
        if (mode == "dns") {
            val ipList = try {
                this.getJSONArray("inet").let { array ->
                    mutableListOf<String>().apply {
                        for (pos in 0 until array.length()) {
                            val ele = array.getString(pos)
                            if (ele.matches(regexIp4Pattern)) {
                                add(ele)
                            }
                        }
                    }
                }
            } catch (ex: JSONException) {
                emptyList<String>()
            }
            if (ipList.isEmpty()) {
                mode = "url"
            } else {
                inetAddresses = ipList.toTypedArray()
            }
        }
        return HostElement(label, tag, host, scheme, mode)
            .apply {
                inetAddress = inetAddresses[0]
            }
    }

    private class ModeSlice(val mode: String, val data: String)

}
