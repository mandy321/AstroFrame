package com.astroframe.tv.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.content.ContextCompat
import com.astroframe.tv.R
import com.astroframe.tv.data.MatStyle

/**
 * MuseumFrameView renders photorealistic luxury wooden frames generated via Banana AI:
 * - Natural Scandinavian Teak Wood
 * - Dark Walnut Wood & Archival Linen Mat
 * - Museum Gilded Antique Gold Leaf
 * - Plus clean architectural procedural mats and custom bezel options.
 */
class MuseumFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentMatStyle: MatStyle = MatStyle.NONE

    private var bmpWalnut: Bitmap? = null
    private var bmpTeak: Bitmap? = null
    private var bmpGold: Bitmap? = null

    private val destRect = Rect()
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)

    init {
        setWillNotDraw(false)
    }

    private fun loadBitmapsIfNeeded() {
        if (bmpWalnut == null) {
            try {
                bmpWalnut = BitmapFactory.decodeResource(resources, R.drawable.frame_walnut)
                bmpTeak = BitmapFactory.decodeResource(resources, R.drawable.frame_teak)
                bmpGold = BitmapFactory.decodeResource(resources, R.drawable.frame_gold)
            } catch (e: Exception) {
                // If decoding fails, fallback gracefully
            }
        }
    }

    fun setMatStyle(style: MatStyle) {
        if (currentMatStyle != style) {
            currentMatStyle = style
            val paddingPx = (style.paddingDp * resources.displayMetrics.density).toInt()
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)
            invalidate()
            requestLayout()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentMatStyle == MatStyle.NONE) return

        loadBitmapsIfNeeded()
        destRect.set(0, 0, width, height)

        val targetBitmap = when (currentMatStyle) {
            MatStyle.WALNUT_LUXURY -> bmpWalnut
            MatStyle.OAK_TEAK_WOOD -> bmpTeak
            MatStyle.ROYAL_GOLD_LEAF -> bmpGold
            else -> null
        }

        if (targetBitmap != null) {
            canvas.drawBitmap(targetBitmap, null, destRect, paint)
        } else {
            // Procedural fallback for linen / minimal mats
            drawProceduralMat(canvas)
        }
    }

    private fun drawProceduralMat(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density
        val matPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F4EFE6")
            style = Paint.Style.FILL
        }
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }

        val artRect = RectF(
            paddingLeft.toFloat(),
            paddingTop.toFloat(),
            w - paddingRight.toFloat(),
            h - paddingBottom.toFloat()
        )
        canvas.drawRect(0f, 0f, w, h, matPaint)
        canvas.drawRect(artRect, shadowPaint)
    }
}
