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

package me.alvince.android.httptrapdoor.okhttp.interceptor

import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import me.alvince.android.httptrapdoor.BuildConfig
import me.alvince.android.httptrapdoor.TrapdoorLogger
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.GzipSource
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.Charset
import java.util.concurrent.Executors

/**
 * Custom http request log [Interceptor]
 */
class NetworkTrafficLogInterceptor : Interceptor {

    companion object {
        private const val TRAFFIC_TAG = "TRAFFIC"
    }

    private val logPrintDispatcher by lazy {
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    }

    private val defaultCharset by lazy { Charset.defaultCharset() }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = try {
            chain.proceed(request)
        } catch (ex: IOException) {
            TrapdoorLogger.e("<-- HTTP FAILED: [${request.url()}] - ${ex.message}", ex)
            throw ex
        }

        printRequestLog(request, response)
        return response
    }

    private fun printRequestLog(request: Request, response: Response) {
        val jsonStringOrRaw = { inputStr: String? ->
            if (inputStr == null)
                null
            else {
                try {
                    JSONObject(inputStr).toString(4)
                } catch (e: JSONException) {
                    inputStr
                }
            }
        }
        val formatHeaders = { headers: Headers ->
            val resList = mutableListOf<String>()
            headers.names().onEach { name ->
                resList.add("$name: ${headers.get(name)}")
            }
            resList
        }

        val requestBodyStr = request.body()?.run {
            val buff = Buffer()
            writeTo(buff)
            buff.readString(defaultCharset)
        }
        val requestBodyString = jsonStringOrRaw(requestBodyStr)
        val responseString = readRespContent(response)

        val lineList = mutableListOf<String>()
        lineList.add("╔══════════════════════════════════════════════════════════════════════════")
        lineList.add("║ Request(${request.url()})")
        lineList.add("╠═════════════════════════════>Request Header<═════════════════════════════")
        lineList.addAll(formatHeaders(request.headers()).map { "║ $it" })
        lineList.add("╠═════════════════════════════>Request Body<═══════════════════════════════")
        requestBodyString?.also { reqBodyStr ->
            lineList.addAll(reqBodyStr.reader().readLines().map { "║ $it" })
        } ?: {
            lineList.add("║ null")
        }()
        lineList.add("╠═════════════════════════════>Response header<════════════════════════════")
        lineList.addAll(formatHeaders(response.headers()).map { "║ $it" })
        lineList.add("╠═════════════════════════════>Response Body<══════════════════════════════")
        responseString?.also { resBodyStr ->
            lineList.addAll(resBodyStr.reader().readLines().map { "║ $it" })
        } ?: {
            lineList.add("║ null")
        }()
        lineList.add("╚══════════════════════════════════════════════════════════════════════════")

        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            if (BuildConfig.DEBUG) {
                throwable.printStackTrace()
            }
        }
        GlobalScope.launch(logPrintDispatcher + exceptionHandler) {
            lineList.onEach { Log.i(TRAFFIC_TAG, it) }
        }
    }

    private fun readRespContent(response: Response): String? =
        response.body()
            ?.let { body ->
                val source = body.source()
                    .apply {
                        request(Long.MAX_VALUE)
                    }
                var buffer = source.buffer()
                val encoding = response.encoding()
                if ("gzip".equals(encoding, true)) {
                    GzipSource(buffer.clone()).use { gzippedBody ->
                        buffer = Buffer().also { it.writeAll(gzippedBody) }
                    }
                }
                buffer
            }
            ?.clone()
            ?.readString(response.charset())

    private fun Response.encoding() = this.header("content-encoding") ?: this.header("Content-Encoding")

    private fun Response.charset(): Charset {
        this.encoding()
            ?.takeIf { Charset.isSupported(it) }
            ?.also {
                return Charset.forName(it)
            }
        return body()?.contentType()?.charset() ?: defaultCharset
    }

}
