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

import android.content.Context
import androidx.annotation.Keep
import androidx.startup.AppInitializer
import androidx.startup.Initializer
import kotlinx.coroutines.*
import me.alvince.android.httptrapdoor.util.ConfigParser

/**
 * Initializer for load config by [AppInitializer]
 */
@Keep
class StartupInitializer : Initializer<Boolean>, CoroutineScope by MainScope() {

    override fun create(context: Context): Boolean {
        TrapdoorLogger.i("do initialize on startup")
        launch {
            val elements = withContext(Dispatchers.IO) { ConfigParser().parse(context) }
            TrapdoorInstrumentation.loadConfig(*elements.toTypedArray())
        }
        return true
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> = mutableListOf()

}
