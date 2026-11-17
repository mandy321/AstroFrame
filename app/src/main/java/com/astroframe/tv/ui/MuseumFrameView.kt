package com.astroframe.tv.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import com.astroframe.tv.data.MatStyle

/**
 * MuseumFrameView renders realistic wooden frames (Scandinavian Teak, Walnut, Ebony, Gilded Gold Leaf, Birch)
 * with beveled inner shadows, authentic wood-grain gradient moldings, and museum linen mats.
 * Designed to compete directly with Samsung The Frame and premium TV gallery modes.
 */
class MuseumFrameView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var currentMatStyle: MatStyle = MatStyle.NONE

    private val outerFramePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val innerMatPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F1E8") // Archival gallery linen off-white
        style = Paint.Style.FILL
    }

    private val bevelShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val bevelHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val grainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
    }

    private val frameRect = RectF()
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
            invalidate()
            requestLayout()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (currentMatStyle == MatStyle.NONE) return

        val w = width.toFloat()
        val h = height.toFloat()
        val density = resources.displayMetrics.density
        val frameWidth = currentMatStyle.paddingDp * density

        frameRect.set(0f, 0f, w, h)
        artRect.set(paddingLeft.toFloat(), paddingTop.toFloat(), w - paddingRight.toFloat(), h - paddingBottom.toFloat())

        when (currentMatStyle) {
            MatStyle.OAK_TEAK_WOOD -> {
                // Natural Scandinavian Oak / Teak Wood: Warm honey tones with architectural grain
                drawWoodFrame(
                    canvas, w, h, frameWidth,
                    baseColors = intArrayOf(Color.parseColor("#B07D48"), Color.parseColor("#C9975D"), Color.parseColor("#9C6838")),
                    grainColor = Color.parseColor("#155A3818"),
                    innerMatWidth = 14f * density,
                    hasLinenMat = true
                )
            }
            MatStyle.WALNUT_LUXURY -> {
                // Deep Espresso Walnut Wood: Rich dark chocolate hues, deep shadows, paired with fine off-white linen
                drawWoodFrame(
                    canvas, w, h, frameWidth,
                    baseColors = intArrayOf(Color.parseColor("#2F1F17"), Color.parseColor("#442E23"), Color.parseColor("#22150E")),
                    grainColor = Color.parseColor("#250A0503"),
                    innerMatWidth = 18f * density,
                    hasLinenMat = true
                )
            }
            MatStyle.MODERN_EBONY -> {
                // Sleek Gallery Matte Ebony Black Wood
                drawWoodFrame(
                    canvas, w, h, frameWidth,
                    baseColors = intArrayOf(Color.parseColor("#141416"), Color.parseColor("#222326"), Color.parseColor("#0E0E10")),
                    grainColor = Color.parseColor("#2035383F"),
                    innerMatWidth = 12f * density,
                    hasLinenMat = true
                )
            }
            MatStyle.ROYAL_GOLD_LEAF -> {
                // Gilded Museum Antique Gold Leaf: Luxurious dual-tone gold foil with metallic luster
                drawWoodFrame(
                    canvas, w, h, frameWidth,
                    baseColors = intArrayOf(Color.parseColor("#B38728"), Color.parseColor("#FBF5B7"), Color.parseColor("#AA771C")),
                    grainColor = Color.parseColor("#2563450A"),
                    innerMatWidth = 14f * density,
                    hasLinenMat = true
                )
            }
            MatStyle.BIRCH_MINIMAL -> {
                // Scandinavian Pale Birch: Light modern wood with soft blond grain
                drawWoodFrame(
                    canvas, w, h, frameWidth,
                    baseColors = intArrayOf(Color.parseColor("#DBC7A2"), Color.parseColor("#E8D8BA"), Color.parseColor("#CEB58C")),
                    grainColor = Color.parseColor("#1885683E"),
                    innerMatWidth = 12f * density,
                    hasLinenMat = true
                )
            }
            MatStyle.MUSEUM_CLASSIC -> {
                // Pure Archival Linen Mat (Warm White) with deep bevel shadow
                innerMatPaint.color = Color.parseColor("#F4EFE6")
                canvas.drawRect(frameRect, innerMatPaint)
                drawBevelShadows(canvas, artRect, density)
            }
            MatStyle.NONE -> Unit
        }
    }

    private fun drawWoodFrame(
        canvas: Canvas,
        w: Float,
        h: Float,
        frameWidth: Float,
        baseColors: IntArray,
        grainColor: Int,
        innerMatWidth: Float,
        hasLinenMat: Boolean
    ) {
        val density = resources.displayMetrics.density

        // 1. Draw outer mitered wood molding with 45-degree architectural light gradient
        val woodShader = LinearGradient(
            0f, 0f, frameWidth, frameWidth,
            baseColors,
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        outerFramePaint.shader = woodShader
        canvas.drawRect(0f, 0f, w, h, outerFramePaint)
        outerFramePaint.shader = null

        // 2. Draw organic wood grain lines across borders for realistic texture
        grainLinePaint.color = grainColor
        // Top & bottom horizontal grain
        for (i in 0 until (frameWidth.toInt() - innerMatWidth.toInt()) step 8) {
            val yOffset = i.toFloat()
            canvas.drawLine(0f, yOffset, w, yOffset, grainLinePaint)
            canvas.drawLine(0f, h - yOffset, w, h - yOffset, grainLinePaint)
        }
        // Left & right vertical grain
        for (i in 0 until (frameWidth.toInt() - innerMatWidth.toInt()) step 8) {
            val xOffset = i.toFloat()
            canvas.drawLine(xOffset, 0f, xOffset, h, grainLinePaint)
            canvas.drawLine(w - xOffset, 0f, w - xOffset, h, grainLinePaint)
        }

        // 3. Draw inner museum archival linen mat if enabled
        if (hasLinenMat && innerMatWidth > 0f) {
            val woodBorderWidth = frameWidth - innerMatWidth
            matRect.set(woodBorderWidth, woodBorderWidth, w - woodBorderWidth, h - woodBorderWidth)

            // Inner linen mat
            innerMatPaint.color = Color.parseColor("#F5F2EB")
            canvas.drawRect(matRect, innerMatPaint)

            // Outer shadow between wood molding and linen mat
            bevelShadowPaint.color = Color.argb(80, 0, 0, 0)
            bevelShadowPaint.strokeWidth = 2.5f * density
            canvas.drawRect(matRect, bevelShadowPaint)

            // Subtle highlight along linen mat rim
            bevelHighlightPaint.color = Color.argb(45, 255, 255, 255)
            bevelHighlightPaint.strokeWidth = 1f * density
            canvas.drawRect(
                matRect.left + 1f, matRect.top + 1f,
                matRect.right - 1f, matRect.bottom - 1f,
                bevelHighlightPaint
            )
        }

        // 4. Draw architectural shadow around artwork edge (creating the 3D sunken canvas effect)
        drawBevelShadows(canvas, artRect, density)
    }

    private fun drawBevelShadows(canvas: Canvas, artBounds: RectF, density: Float) {
        // Deep sunken shadow (sunken 45-degree angle illusion)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(90, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
        }
        canvas.drawRect(artBounds, shadowPaint)

        // Inner bevel specular highlight
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(50, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1f * density
        }
        canvas.drawRect(
            artBounds.left - 1.5f,
            artBounds.top - 1.5f,
            artBounds.right + 1.5f,
            artBounds.bottom + 1.5f,
            highlightPaint
        )
    }
}
