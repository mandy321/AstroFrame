package com.astroframe.tv.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.FrameLayout
import com.astroframe.tv.data.MatStyle

/**
 * MuseumFrameView renders slim, authentic architectural gallery frames:
 * - Direct rival to Samsung The Frame TV
 * - Realistic wood grain bevel moldings (Teak, Walnut, Ebony, Antique Gold Leaf)
 * - Mitered 45-degree corner joints
 * - Elegant, slim proportion: artwork occupies ~90% of the screen
 * - Archival linen matting with 45-degree core shadow
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
        style = Paint.Style.FILL
    }

    private val miterJointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
        color = Color.argb(45, 0, 0, 0)
    }

    private val bevelShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val bevelHighlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }

    private val grainLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1f
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
        val totalBorder = currentMatStyle.paddingDp * density

        frameRect.set(0f, 0f, w, h)
        artRect.set(paddingLeft.toFloat(), paddingTop.toFloat(), w - paddingRight.toFloat(), h - paddingBottom.toFloat())

        when (currentMatStyle) {
            MatStyle.OAK_TEAK_WOOD -> {
                // Natural Scandinavian Teak: Slim 16dp outer wood + 18dp archival warm mat
                drawArchitecturalFrame(
                    canvas, w, h, totalBorder,
                    woodWidth = 16f * density,
                    baseColors = intArrayOf(Color.parseColor("#9E6C3A"), Color.parseColor("#BA854D"), Color.parseColor("#875629")),
                    grainColor = Color.parseColor("#184A2C10"),
                    matColor = Color.parseColor("#F5F1E8")
                )
            }
            MatStyle.WALNUT_LUXURY -> {
                // Dark Walnut: Slim 18dp espresso wood + 22dp linen mat
                drawArchitecturalFrame(
                    canvas, w, h, totalBorder,
                    woodWidth = 18f * density,
                    baseColors = intArrayOf(Color.parseColor("#291A12"), Color.parseColor("#3B261B"), Color.parseColor("#1C100A")),
                    grainColor = Color.parseColor("#220A0503"),
                    matColor = Color.parseColor("#F3EFE6")
                )
            }
            MatStyle.MODERN_EBONY -> {
                // Gallery Ebony: Slim contemporary matte black frame
                drawArchitecturalFrame(
                    canvas, w, h, totalBorder,
                    woodWidth = 16f * density,
                    baseColors = intArrayOf(Color.parseColor("#141517"), Color.parseColor("#222327"), Color.parseColor("#0F1012")),
                    grainColor = Color.parseColor("#1E363940"),
                    matColor = Color.parseColor("#F4F1EB")
                )
            }
            MatStyle.ROYAL_GOLD_LEAF -> {
                // Museum Gilded Antique Gold Leaf: Florentine gold with burnished specular highlights
                drawArchitecturalFrame(
                    canvas, w, h, totalBorder,
                    woodWidth = 18f * density,
                    baseColors = intArrayOf(Color.parseColor("#A87A24"), Color.parseColor("#E6C56B"), Color.parseColor("#8E6315")),
                    grainColor = Color.parseColor("#25573B08"),
                    matColor = Color.parseColor("#F6F3EC")
                )
            }
            MatStyle.BIRCH_MINIMAL -> {
                // Nordic Pale Birch: Light blond modern Scandinavian profile
                drawArchitecturalFrame(
                    canvas, w, h, totalBorder,
                    woodWidth = 16f * density,
                    baseColors = intArrayOf(Color.parseColor("#D4BE97"), Color.parseColor("#E5D4B2"), Color.parseColor("#C4AD82")),
                    grainColor = Color.parseColor("#187C5E33"),
                    matColor = Color.parseColor("#F7F4ED")
                )
            }
            MatStyle.MUSEUM_CLASSIC -> {
                // Pure Archival Linen Mat (Warm White) with beveled drop shadow
                innerMatPaint.color = Color.parseColor("#F5F1E8")
                canvas.drawRect(frameRect, innerMatPaint)
                drawBevelShadows(canvas, artRect, density)
            }
            MatStyle.NONE -> Unit
        }
    }

    private fun drawArchitecturalFrame(
        canvas: Canvas,
        w: Float,
        h: Float,
        totalBorder: Float,
        woodWidth: Float,
        baseColors: IntArray,
        grainColor: Int,
        matColor: Int
    ) {
        val density = resources.displayMetrics.density

        // 1. Draw outer wood molding with subtle top-down studio lighting gradient
        val woodShader = LinearGradient(
            0f, 0f, 0f, woodWidth,
            baseColors,
            floatArrayOf(0.0f, 0.5f, 1.0f),
            Shader.TileMode.CLAMP
        )
        outerFramePaint.shader = woodShader
        canvas.drawRect(0f, 0f, w, h, outerFramePaint)
        outerFramePaint.shader = null

        // 2. Draw 45-degree mitered corner joints (hallmark of authentic custom framing)
        canvas.drawLine(0f, 0f, woodWidth, woodWidth, miterJointPaint)
        canvas.drawLine(w, 0f, w - woodWidth, woodWidth, miterJointPaint)
        canvas.drawLine(0f, h, woodWidth, h - woodWidth, miterJointPaint)
        canvas.drawLine(w, h, w - woodWidth, h - woodWidth, miterJointPaint)

        // 3. Draw fine organic wood grain across the molding
        grainLinePaint.color = grainColor
        var yOff = 4f * density
        while (yOff < woodWidth - 2f) {
            canvas.drawLine(woodWidth, yOff, w - woodWidth, yOff, grainLinePaint)
            canvas.drawLine(woodWidth, h - yOff, w - woodWidth, h - yOff, grainLinePaint)
            yOff += 5f * density
        }

        // 4. Draw inner museum archival linen mat
        matRect.set(woodWidth, woodWidth, w - woodWidth, h - woodWidth)
        innerMatPaint.color = matColor
        canvas.drawRect(matRect, innerMatPaint)

        // Subtle shadow between outer wood and inner mat
        bevelShadowPaint.color = Color.argb(60, 0, 0, 0)
        bevelShadowPaint.strokeWidth = 1.5f * density
        canvas.drawRect(matRect, bevelShadowPaint)

        // 5. Draw 3D sunken shadow and specular bevel around artwork edge
        drawBevelShadows(canvas, artRect, density)
    }

    private fun drawBevelShadows(canvas: Canvas, artBounds: RectF, density: Float) {
        // Deep sunken shadow (creates authentic 3D gallery depth)
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(70, 0, 0, 0)
            style = Paint.Style.STROKE
            strokeWidth = 2f * density
        }
        canvas.drawRect(artBounds, shadowPaint)

        // Inner bevel highlight
        val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(35, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 0.8f * density
        }
        canvas.drawRect(
            artBounds.left - 1f,
            artBounds.top - 1f,
            artBounds.right + 1f,
            artBounds.bottom + 1f,
            highlightPaint
        )
    }
}
