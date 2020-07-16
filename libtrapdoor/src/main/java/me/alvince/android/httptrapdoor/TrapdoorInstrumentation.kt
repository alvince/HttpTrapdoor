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

import androidx.annotation.RestrictTo

/**
 * Internal Trapdoor instrumentation
 *
 * Created by alvince on 2020/7/14
 *
 * @author alvince.zy@gmail.com
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class TrapdoorInstrumentation private constructor() {

    companion object {
        /**
         * Global elements configured
         */
        private var elementsConfigured: List<HostElement>? = null

        internal fun obtain(): TrapdoorInstrumentation {
            return TrapdoorInstrumentation()
        }

        internal fun loadConfig(vararg elements: HostElement) {
            if (elements.isEmpty()) {
                return
            }
            elementsConfigured = elements
                .also {
                    if (BuildConfig.DEBUG) {
                        TrapdoorLogger.i(it.contentToString())
                    }
                }
                .toList()
        }
    }

    private var currentHostTag: String = ""

    /**
     * custom extensional elements
     */
    private var customElements = mutableListOf<HostElement>()

    fun hostElements() = (elementsConfigured?.toList() ?: emptyList()) + customElements

    internal fun extensionalConfig(elements: Array<out HostElement>) {
        elements.takeIf { it.isNotEmpty() }
            ?.forEach { element ->
                customElements.indexOf(element)
                    .takeIf { it != -1 }
                    ?.also {
                        customElements[it] = element
                    }
                    ?: customElements.add(element)
            }
    }

    internal fun checkHostConfigured(host: String): Boolean {
        if (host.isEmpty()) {
            return false
        }
        return hostElements().map { it.host }.contains(host)
    }

    internal fun currentHost(): HostElement? {
        return currentHostTag
            .takeIf { it.isNotEmpty() }
            ?.let { getElementByTag(it) }
    }

    internal fun pick(hostTag: String) {
        hostTag.takeIf { it != currentHostTag }
            ?.also { currentHostTag = it }
    }

    private fun getElementByTag(tag: String): HostElement? =
        tag.takeIf { it.isNotEmpty() }
            ?.let {
                elementsConfigured
                    ?.find { it.tag == tag }
                    ?: customElements.find { it.tag == tag }
            }

}
