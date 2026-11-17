package com.astroframe.tv.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.widget.FrameLayout
import com.astroframe.tv.data.MatStyle

/**
 * MuseumFrameView renders Samsung The Frame style architectural matting,
 * bevel shadows, and gallery borders around artwork.
 */
class MuseumFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentMatStyle: MatStyle = MatStyle.NONE

    private val matPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F4EFE6") // Archival Gallery Warm Off-White
        style = Paint.Style.FILL
    }

    private val innerShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#22000000") // 45-degree bevel shadow
        style = Paint.Style.STROKE
    }

    private val accentBevelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#33FFFFFF") // Subtle metallic highlight
        style = Paint.Style.STROKE
    }

    private val matRect = RectF()
    private val artRect = RectF()

    init {
        setWillNotDraw(false)
    }

    fun setMatStyle(style: MatStyle) {
        if (currentMatStyle != style) {
            currentMatStyle = style
            val paddingPx = (style.paddingDp * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            innerShadowPaint.strokeWidth = 2f * resources.displayMetrics.density
            accentBevelPaint.strokeWidth = 1f * resources.displayMetrics.density
            invalidate()
            requestLayout()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentMatStyle == MatStyle.NONE) return

        val w = width.toFloat()
        val h = height.toFloat()
        val pLeft = paddingLeft.toFloat()
        val pTop = paddingTop.toFloat()
        val pRight = paddingRight.toFloat()
        val pBottom = paddingBottom.toFloat()

        matRect.set(0f, 0f, w, h)
        artRect.set(pLeft, pTop, w - pRight, h - pBottom)

        // 1. Draw outer mat background
        canvas.drawRect(matRect, matPaint)

        // 2. Inner beveled edge shadow around art
        canvas.drawRect(artRect, innerShadowPaint)

        // 3. Crisp highlight along inner edge
        canvas.drawRect(
            artRect.left - 1f,
            artRect.top - 1f,
            artRect.right + 1f,
            artRect.bottom + 1f,
            accentBevelPaint
        )
    }
}
