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

import me.alvince.android.httptrapdoor.util.TcpPortHolder

/**
 * Trapdoor host element defined
 *
 * Created by alvince on 2020/7/14
 *
 * @author alvince.zy@gmail.com
 */
data class HostElement(
    /**
     * the user-visible host desc
     */
    val name: String,
    /**
     * the unique tag of host element
     */
    val tag: String,
    /**
     * the host part
     */
    val host: String,
    /**
     * the scheme of host element
     *
     * can be `http` or `https`
     */
    var scheme: String = "https"

) {

    val port: Int get() = TcpPortHolder.port(scheme)

    override fun toString(): String {
        return "HostElement(name='$name', tag='$tag', host_url='$scheme://$host')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HostElement

        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int = tag.hashCode()

}
