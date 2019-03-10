package by.anegin.telegram_contests.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import by.anegin.telegram_contests.R

class MiniChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    class Line(val path: Path, val color: Int)

    companion object {
        private const val DRAG_RANGE = 1
        private const val DRAG_RANGE_START = 2
        private const val DRAG_RANGE_END = 3
        private const val DRAG_NONE = 4
    }

    private val windowPaint = Paint()
    private val bgPaint = Paint()
    private val touchRipplePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val fadeBgColor: Int

    private val windowStrokeWidthTopBottom: Float
    private val windowStrokeWidthLeftRight: Float

    private var rangeStart = 0f
    private var rangeEnd = 1f

    private val touchSlop: Int
    private val dragSize: Float

    private var downX = 0f
    private var lastTouchX = 0f
    private var inDragMode = false

    private var dragMode: Int? = null

    private var touchRippleAnimation: Animator? = null
    private var touchRippleRadius = 0f
    private var touchRippleAlpha = 0f

    // =======

    private val values = ArrayList<LongArray>()
    private val colors = ArrayList<Int>()

    private val lines = ArrayList<Line>()

    fun setData(values: List<LongArray>, colors: List<Int>) {
        val width = width - paddingStart - paddingEnd
        val height = height - paddingTop - paddingBottom
        calculateLines(values, colors, width, height)
    }

    private fun calculateLines(values: List<LongArray>, colors: List<Int>, width: Int, height: Int) {
        this.values.clear()
        this.values.addAll(values)

        this.colors.clear()
        this.colors.addAll(colors)


    }

    // =======

    init {
        val typedValue = TypedValue()
        val defAttrs = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
        val primaryColor = defAttrs.getColor(0, 0)
        defAttrs.recycle()

        val viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.MiniChartView)

        rangeStart = viewAttrs.getFloat(R.styleable.MiniChartView_range_start, 0f)
        rangeEnd = viewAttrs.getFloat(R.styleable.MiniChartView_range_end, 1f)
        if (rangeStart < 0f) rangeStart = 0f
        if (rangeEnd > 1f) rangeEnd = 1f

        val windowStrokeColor = viewAttrs.getColor(R.styleable.MiniChartView_window_stroke_color, primaryColor)
        fadeBgColor = viewAttrs.getColor(R.styleable.MiniChartView_fade_bg_color, primaryColor)

        windowStrokeWidthTopBottom =
            viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_top_bottom, 0f)
        windowStrokeWidthLeftRight =
            viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_left_right, 0f)

        viewAttrs.recycle()

        windowPaint.color = windowStrokeColor
        windowPaint.style = Paint.Style.STROKE

        bgPaint.style = Paint.Style.FILL

        touchRipplePaint.style = Paint.Style.FILL
        touchRipplePaint.color = Color.argb(
            (Color.alpha(windowStrokeColor) * 0.4f).toInt(),
            Color.red(windowStrokeColor), Color.green(windowStrokeColor), Color.blue(windowStrokeColor)
        )

        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        dragSize = 1.5f * touchSlop
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        val width = (width - paddingStart - paddingEnd).toFloat()
        val height = (height - paddingTop - paddingBottom).toFloat()

        val rangeStartX = width * rangeStart
        val rangeEndX = width * rangeEnd

        canvas.save()
        canvas.translate(paddingStart.toFloat(), paddingTop.toFloat())
        bgPaint.color = fadeBgColor
        if (rangeStart > 0f) {
            canvas.drawRect(0f, 0f, rangeStartX, height, bgPaint)
        }
        if (rangeEnd < 1f) {
            canvas.drawRect(rangeEndX, 0f, width, height, bgPaint)
        }
        canvas.restore()

        val touchRippleRadius = this.touchRippleRadius
        if (touchRippleRadius > 0f) {

            val windowStrokeColor = windowPaint.color
            touchRipplePaint.color = Color.argb(
                (Color.alpha(windowStrokeColor) * 0.4f * touchRippleAlpha).toInt(),
                Color.red(windowStrokeColor), Color.green(windowStrokeColor), Color.blue(windowStrokeColor)
            )

            when (dragMode) {
                DRAG_RANGE_START -> {
                    val cx = rangeStartX + windowStrokeWidthLeftRight / 2f
                    canvas.drawCircle(cx, paddingTop + height / 2f, touchRippleRadius, touchRipplePaint)
                }
                DRAG_RANGE_END -> {
                    val cx = rangeEndX - windowStrokeWidthLeftRight / 2f
                    canvas.drawCircle(cx, paddingTop + height / 2f, touchRippleRadius, touchRipplePaint)
                }
                DRAG_RANGE -> {
                    val cx = rangeStartX + (rangeEndX - rangeStartX) / 2f
                    canvas.drawCircle(cx, paddingTop + height / 2f, touchRippleRadius, touchRipplePaint)
                }
            }
        }

        canvas.save()
        canvas.translate(paddingStart.toFloat(), paddingTop.toFloat())
        canvas.clipRect(0f, 0f, width, height)

        if (windowStrokeWidthTopBottom > 0f) {
            windowPaint.strokeWidth = windowStrokeWidthTopBottom
            val halfStrokeWidth = windowStrokeWidthTopBottom / 2f
            canvas.drawLine(rangeStartX, halfStrokeWidth, rangeEndX, halfStrokeWidth, windowPaint)
            canvas.drawLine(rangeStartX, height - halfStrokeWidth, rangeEndX, height - halfStrokeWidth, windowPaint)
        }
        if (windowStrokeWidthLeftRight > 0f) {
            windowPaint.strokeWidth = windowStrokeWidthLeftRight
            val halfStrokeWidth = windowStrokeWidthLeftRight / 2f
            canvas.drawLine(rangeStartX + halfStrokeWidth, 0f, rangeStartX + halfStrokeWidth, height, windowPaint)
            canvas.drawLine(rangeEndX - halfStrokeWidth, 0f, rangeEndX - halfStrokeWidth, height, windowPaint)
        }

        canvas.restore()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val width = (width - paddingStart - paddingEnd).toFloat()
                val rangeStartX = width * rangeStart
                val rangeEndX = width * rangeEnd

                downX = event.x
                lastTouchX = event.x
                inDragMode = false

                dragMode = when (event.x) {
                    in (rangeStartX - dragSize)..(rangeStartX + dragSize + windowStrokeWidthLeftRight) -> DRAG_RANGE_START
                    in (rangeEndX - dragSize - windowStrokeWidthLeftRight)..(rangeEndX + dragSize) -> DRAG_RANGE_END
                    in (rangeStartX + dragSize + windowStrokeWidthLeftRight)..(rangeEndX - dragSize - windowStrokeWidthLeftRight) -> DRAG_RANGE
                    else -> DRAG_NONE
                }

                showTouchRipple()
            }
            MotionEvent.ACTION_MOVE -> {
                if (!inDragMode && Math.abs(event.x - downX) > touchSlop) {
                    inDragMode = true
                }
                if (inDragMode && dragMode != null && dragMode != DRAG_NONE) {
                    onMove(event.x, lastTouchX - event.x)
                    lastTouchX = event.x
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                hideTouchRippleAnimation()
            }
        }
        return true
    }

    private fun onMove(eventX: Float, distanceX: Float) {
        val width = (width - paddingStart - paddingEnd).toFloat()
        val rangeStartX = width * rangeStart
        val rangeEndX = width * rangeEnd

        when (dragMode) {
            DRAG_RANGE_START -> {

                var newRangeStart = (eventX - paddingLeft) / width
                if (newRangeStart < 0f) newRangeStart = 0f

                var newRangeStartX = width * newRangeStart
                if (newRangeStartX + dragSize + windowStrokeWidthLeftRight > rangeEndX - dragSize - windowStrokeWidthLeftRight) {
                    newRangeStartX = rangeEndX - 2f * (dragSize + windowStrokeWidthLeftRight)
                    newRangeStart = (newRangeStartX - paddingLeft) / width
                }

                if (newRangeStart != rangeStart) {
                    rangeStart = newRangeStart
                    invalidate()
                }

            }
            DRAG_RANGE_END -> {

                var newRangeEnd = (eventX - paddingLeft) / width
                if (newRangeEnd > 1f) newRangeEnd = 1f

                var newRangeEndX = width * newRangeEnd
                if (newRangeEndX - dragSize - windowStrokeWidthLeftRight < rangeStartX + dragSize + windowStrokeWidthLeftRight) {
                    newRangeEndX = rangeStartX + 2f * (dragSize + windowStrokeWidthLeftRight)
                    newRangeEnd = (newRangeEndX - paddingLeft) / width
                }

                if (newRangeEnd != rangeEnd) {
                    rangeEnd = newRangeEnd
                    invalidate()
                }

            }
            DRAG_RANGE -> {

                var newRangeStart = (rangeStartX - distanceX) / width
                if (newRangeStart < 0f) newRangeStart = 0f

                var newRangeEnd = newRangeStart + (rangeEnd - rangeStart)
                if (newRangeEnd > 1f) {
                    newRangeEnd = 1f
                    newRangeStart = 1f - (rangeEnd - rangeStart)
                }

                if (newRangeStart != rangeStart || newRangeEnd != rangeEnd) {
                    rangeStart = newRangeStart
                    rangeEnd = newRangeEnd
                    invalidate()
                }

            }
        }
    }

    private fun showTouchRipple() {
        touchRippleAnimation?.cancel()
        touchRippleAnimation = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 200L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                touchRippleRadius = (it.animatedValue as Float) * (0.7f * height)
                touchRippleAlpha = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
    }

    private fun hideTouchRippleAnimation() {
        val currentValue = (touchRippleAnimation as? ValueAnimator)?.animatedValue as? Float ?: 1f
        touchRippleAnimation?.cancel()
        touchRippleAnimation = ValueAnimator.ofFloat(currentValue, 0f).apply {
            duration = 200L
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener {
                touchRippleRadius = (it.animatedValue as Float) * (0.7f * height)
                touchRippleAlpha = it.animatedValue as Float
                postInvalidateOnAnimation()
            }
            start()
        }
    }

}
