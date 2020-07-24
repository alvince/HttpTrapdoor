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

package me.alvince.android.httptrapdoor.okhttp.dns

import me.alvince.android.httptrapdoor.HostType
import me.alvince.android.httptrapdoor.TrapdoorInstrumentation
import me.alvince.android.httptrapdoor.TrapdoorLogger
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException

internal class FacileDns(
    src: Dns? = null,
    private val instrumentation: TrapdoorInstrumentation
) : Dns {

    private val source = src ?: Dns.SYSTEM

    @Throws(UnknownHostException::class)
    override fun lookup(hostname: String?): MutableList<InetAddress> {
        if (hostname.isNullOrEmpty()) {
            throw UnknownHostException("Illegal hostname: $hostname")
        }
        return instrumentation
            .takeIf { it.checkHostConfigured(hostname) }
            ?.currentHost()
            ?.takeIf { it.hostType == HostType.DNS }
            ?.let {
                mutableListOf<InetAddress>().apply {
                    try {
                        InetAddress.getByName(it.inetAddress)
                    } catch (ex: UnknownHostException) {
                        TrapdoorLogger.e("Fail to get inetaddress: ${it.inetAddress}.", ex)
                    }
                }
            }
            ?.takeIf { it.isNotEmpty() }
            ?: source.lookup(hostname)
    }

}
