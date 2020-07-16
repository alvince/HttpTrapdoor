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

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.RestrictTo
import androidx.annotation.UiThread
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.min

/**
 * Created by alvince on 2020/7/16
 *
 * @author alvince.zy@gmail.com
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal class FloatingBuoyView
@JvmOverloads
constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    AppCompatTextView(context, attrs, defStyleAttr), CoroutineScope by MainScope() {

    companion object {
        private val indicatorViewId = ViewCompat.generateViewId()
    }

    internal var floatAttrs: WindowManager.LayoutParams? = null

    private val touchStartPoint = PointF()
    private val touchMovedPoint = PointF()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var dragPerformed = false
    private var attached = false

    init {
        id = indicatorViewId
        background = BuoyBackgroundDrawable()
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
        text = "N"
    }

    @UiThread
    internal fun attach() {
        floatAttrs?.also {
            val wm = ContextCompat.getSystemService(context, WindowManager::class.java) as WindowManager
            if (attached) {
                wm.updateViewLayout(this, it)
            } else {
                (parent as? ViewGroup)?.removeView(this)
                launch {
                    wm.addView(this@FloatingBuoyView, it)
                    attached = true
                }
            }
        }
    }

    @UiThread
    internal fun detach() {
        if (!attached || parent == null) {
            return
        }
        (ContextCompat.getSystemService(context, WindowManager::class.java) as WindowManager)
            .removeView(this)
    }

}

private class BuoyBackgroundDrawable : Drawable() {
    private val paint = Paint()
        .apply {
            color = 0x33000000
            style = Paint.Style.FILL_AND_STROKE
        }

    override fun draw(canvas: Canvas) {
        bounds.takeIf { it.width() > 0 && it.height() > 0 }
            ?.also {
                val w = it.width()
                val h = it.height()
                canvas.drawCircle(w.div(2F), h.div(2F), min(w, h).div(2F), paint)
            }
    }

    override fun setAlpha(alpha: Int) {
        alpha.takeIf { it != paint.alpha }
            ?.also {
                paint.alpha = it
                invalidateSelf()
            }
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

}
