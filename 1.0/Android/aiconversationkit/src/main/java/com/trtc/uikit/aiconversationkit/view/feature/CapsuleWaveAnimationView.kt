package com.trtc.uikit.aiconversationkit.view.feature

import android.animation.Keyframe
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import android.view.animation.PathInterpolator

class CapsuleWaveAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Style {
        LARGE,
        SMALL
    }

    private data class StyleConfig(
        val capsuleWidth: Float,
        val minHeight: Float,
        val maxHeight: Float,
        val spacing: Float
    ) {
        val staticHeights: FloatArray
            get() {
                val side = minHeight + (maxHeight - minHeight) * 0.3f
                val center = minHeight + (maxHeight - minHeight) * 0.7f
                return floatArrayOf(side, center, side)
            }
    }

    companion object {
        private const val CAPSULE_COUNT = 3

        private const val ANIMATION_DURATION = 600L
        private const val STAGGER_DELAY = 150L

        private val LARGE_CONFIG = StyleConfig(
            capsuleWidth = 10f.dp,
            minHeight = 10f.dp,
            maxHeight = 20f.dp,
            spacing = 5f.dp
        )

        private val SMALL_CONFIG = StyleConfig(
            capsuleWidth = 5f.dp,
            minHeight = 5f.dp,
            maxHeight = 14f.dp,
            spacing = 3f.dp
        )

        private val Float.dp: Float
            get() = this * android.content.res.Resources.getSystem().displayMetrics.density
    }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val capsuleRects = Array(CAPSULE_COUNT) { RectF() }
    private val capsuleHeights = FloatArray(CAPSULE_COUNT)
    
    private var currentStyle: Style = Style.LARGE
    private var isAnimating = false
    private val animators = arrayOfNulls<ValueAnimator>(CAPSULE_COUNT)

    private val choreographer = Choreographer.getInstance()
    private var frameScheduled = false
    private val frameCallback = Choreographer.FrameCallback {
        frameScheduled = false
        if (isAnimating) {
            invalidate()
        }
    }

    private val easeInOutInterpolator = PathInterpolator(0.42f, 0f, 0.58f, 1f)

    private val config: StyleConfig
        get() = if (currentStyle == Style.LARGE) LARGE_CONFIG else SMALL_CONFIG

    init {
        resetHeightsToMin()
    }

    private var drawCount = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cfg = config
        val totalWidth = CAPSULE_COUNT * cfg.capsuleWidth + (CAPSULE_COUNT - 1) * cfg.spacing
        val startX = (width - totalWidth) / 2f
        val centerY = height / 2f

        drawCount++

        for (i in 0 until CAPSULE_COUNT) {
            val h = capsuleHeights[i]
            val x = startX + i * (cfg.capsuleWidth + cfg.spacing)
            val top = centerY - h / 2f
            val bottom = centerY + h / 2f

            capsuleRects[i].set(x, top, x + cfg.capsuleWidth, bottom)

            val cornerRadius = cfg.capsuleWidth / 2f
            canvas.drawRoundRect(capsuleRects[i], cornerRadius, cornerRadius, paint)
        }
    }

    fun setStyle(style: Style) {
        if (currentStyle != style) {
            currentStyle = style
            if (!isAnimating) {
                layoutCapsules()
                invalidate()
            }
        }
    }

    fun startAnimating() {
        if (isAnimating) return
        isAnimating = true
        currentStyle = Style.LARGE
        rebuildAnimations()
    }

    fun stopAnimating() {
        isAnimating = false
        stopAllAnimators()
        layoutCapsules(config.staticHeights)
        invalidate()
    }

    private fun stopAllAnimators() {
        for (i in 0 until CAPSULE_COUNT) {
            animators[i]?.cancel()
            animators[i] = null
        }
    }

    private fun resetHeightsToMin() {
        val cfg = config
        for (i in 0 until CAPSULE_COUNT) {
            capsuleHeights[i] = cfg.minHeight
        }
    }

    private fun layoutCapsules(heights: FloatArray? = null) {
        val cfg = config
        val defaultHeights = heights ?: FloatArray(CAPSULE_COUNT) { cfg.minHeight }
        for (i in 0 until CAPSULE_COUNT) {
            capsuleHeights[i] = defaultHeights[i]
        }
    }

    private fun rebuildAnimations() {
        stopAllAnimators()
        resetHeightsToMin()
        invalidate()

        val cfg = config

        for (i in 0 until CAPSULE_COUNT) {
            val capsuleIndex = i
            val kf0 = Keyframe.ofFloat(0f, cfg.minHeight)
            val kf1 = Keyframe.ofFloat(0.5f, cfg.maxHeight)
            val kf2 = Keyframe.ofFloat(1f, cfg.minHeight)
            kf0.interpolator = easeInOutInterpolator
            kf1.interpolator = easeInOutInterpolator
            kf2.interpolator = easeInOutInterpolator

            val pvh = PropertyValuesHolder.ofKeyframe("h", kf0, kf1, kf2)

            var updateCount = 0
            val animator = ValueAnimator.ofPropertyValuesHolder(pvh).apply {
                duration = ANIMATION_DURATION
                repeatCount = ValueAnimator.INFINITE
                interpolator = null
                startDelay = capsuleIndex * STAGGER_DELAY
                addUpdateListener {
                    val raw = it.animatedValue
                    val h = (raw as Number).toFloat()
                    capsuleHeights[capsuleIndex] = h
                    updateCount++
                    scheduleFrame()
                }
            }

            animators[capsuleIndex] = animator
            animator.start()
        }
    }

    private fun scheduleFrame() {
        if (!frameScheduled && isAnimating) {
            frameScheduled = true
            choreographer.postFrameCallback(frameCallback)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        isAnimating = false
        stopAllAnimators()
        choreographer.removeFrameCallback(frameCallback)
    }
    
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isAnimating) {
            rebuildAnimations()
        }
    }
}
