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
import android.os.Build
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
import me.alvince.android.httptrapdoor.buoy.util.dipOf
import kotlin.math.min

/**
 * Built-in [TrapdoorBuoy] floating indicator [AppCompatTextView]
 *
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

    // TODO: drag support
    private val touchStartPoint = PointF()
    private val touchMovedPoint = PointF()
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    private var dragPerformed = false
    private var attached = false

    init {
        id = indicatorViewId
        background = BuoyBackgroundDrawable(context.dipOf(4F).toInt())
        gravity = Gravity.CENTER
        setTextColor(Color.WHITE)
    }

    @UiThread
    internal fun attach() {
        floatAttrs?.also {
            val wm =
                ContextCompat.getSystemService(context, WindowManager::class.java) as WindowManager
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

private class BuoyBackgroundDrawable(private val insets: Int) : Drawable() {

    enum class BuoyDirection {
        LEFT, RIGHT
    }

    private val paint = Paint()
        .apply {
            color = 0x33000000
            style = Paint.Style.FILL_AND_STROKE
            isAntiAlias = true
        }

    private val bgPath = Path()
    private var compatCornerRectF = RectF()

    private var direction = BuoyDirection.LEFT

    override fun draw(canvas: Canvas) {
        bounds.takeIf { min(it.width(), it.height()) > insets.times(2) }
            ?.also {
                innerDraw(canvas, it.width().toFloat(), it.height().toFloat(), paint, direction)
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

    private fun innerDraw(
        canvas: Canvas,
        width: Float,
        height: Float,
        paint: Paint,
        direction: BuoyDirection
    ) {
        bgPath.apply {
            reset()
            val cornerRadius = height.div(2F)
            if (direction == BuoyDirection.LEFT) {
                moveTo(0F, 0F)
                lineTo(0F, height)
                lineTo(width - cornerRadius, height)
                arcToCompat(width - height, 0F, width, height, 90F, -180F)
                close()
            } else {
                moveTo(width, 0F)
                lineTo(width, height)
                lineTo(cornerRadius, height)
                arcToCompat(0F, 0F, height, height, 90F, 180F)
                close()
            }
        }.also {
            canvas.drawPath(it, paint)
        }
    }

    private fun Path.arcToCompat(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        startAngle: Float,
        sweepAngle: Float
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            arcTo(left, top, right, bottom, startAngle, sweepAngle, false)
        } else {
            compatCornerRectF.apply {
                set(left, top, right, bottom)
            }.also { rectF ->
                arcTo(rectF, startAngle, sweepAngle)
            }
        }
    }

}
