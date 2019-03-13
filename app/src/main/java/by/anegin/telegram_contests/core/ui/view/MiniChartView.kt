package by.anegin.telegram_contests.core.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.core.ui.model.Graph
import by.anegin.telegram_contests.core.ui.model.UiChart
import java.util.concurrent.Executors

class MiniChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val DRAG_RANGE = 1
        private const val DRAG_RANGE_START = 2
        private const val DRAG_RANGE_END = 3
        private const val DRAG_NONE = 4
    }

    private val windowPaint = Paint()
    private val touchRipplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chartPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)
    private val fadePaint = Paint()

    private val windowStrokeWidthTopBottom: Float
    private val windowStrokeWidthLeftRight: Float

    private var rangeStart = 0f
    private var rangeEnd = 1f

    private var downX = 0f
    private var lastTouchX = 0f
    private var inDragMode = false

    private var dragMode: Int? = null

    private val touchSlop: Int
    private val dragSize: Float

    private var touchRippleAnimation: Animator? = null
    private var touchRippleRadius = 0f
    private var touchRippleAlpha = 0f

    private var onRangeChangeListener: ((Float, Float) -> Unit)? = null

    private val updateDataExecutor = Executors.newFixedThreadPool(2)
    private var lastUpdateGeneration = 0L

    private var uiChart: UiChart? = null
    private var graphs: List<Graph>? = null

    init {
        val typedValue = TypedValue()
        val defAttrs = context.obtainStyledAttributes(typedValue.data, intArrayOf(android.R.attr.colorPrimary))
        val primaryColor = defAttrs.getColor(0, 0)
        defAttrs.recycle()

        val viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.MiniChartView, defStyleAttr, 0)
        rangeStart = viewAttrs.getFloat(R.styleable.MiniChartView_range_start, 0f)
        rangeEnd = viewAttrs.getFloat(R.styleable.MiniChartView_range_end, 1f)
        val offWindowAlpha = viewAttrs.getFloat(R.styleable.MiniChartView_off_window_alpha, 0.5f)
        val windowBgColor = viewAttrs.getColor(R.styleable.MiniChartView_window_bg_color, primaryColor)
        windowStrokeWidthTopBottom =
            viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_top_bottom, 0f)
        windowStrokeWidthLeftRight =
            viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_left_right, 0f)
        val chartLineWidth = viewAttrs.getDimension(R.styleable.MiniChartView_chart_line_width, 1f)
        viewAttrs.recycle()

        if (rangeStart < 0f) rangeStart = 0f
        if (rangeEnd > 1f) rangeEnd = 1f

        windowPaint.style = Paint.Style.FILL
        windowPaint.color = windowBgColor

        touchRipplePaint.style = Paint.Style.FILL
        touchRipplePaint.color = windowPaint.color

        chartPaint.style = Paint.Style.STROKE
        chartPaint.strokeWidth = chartLineWidth

        fadePaint.style = Paint.Style.FILL
        fadePaint.color = Color.argb((offWindowAlpha * 255).toInt(), 255, 255, 255)
        fadePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)

        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        dragSize = 1.5f * touchSlop
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        val width = width.toFloat()
        val height = height.toFloat()

        val rangeStartX = width * rangeStart
        val rangeEndX = width * rangeEnd

        // draw window
        val windowStart = rangeStartX + windowStrokeWidthLeftRight
        val windowEnd = rangeEndX - windowStrokeWidthLeftRight
        canvas.drawRect(0f, 0f, windowStart, height, windowPaint)
        canvas.drawRect(windowEnd, 0f, width, height, windowPaint)
        canvas.drawRect(windowStart, 0f, windowEnd, windowStrokeWidthTopBottom, windowPaint)
        canvas.drawRect(windowStart, height - windowStrokeWidthTopBottom, windowEnd, height, windowPaint)

        // draw Chart data
        graphs?.forEach { graph ->
            graph.draw(canvas, chartPaint)
        }

        // fade out-of-window range
        if (rangeStart > 0f) {
            canvas.drawRect(0f, 0f, rangeStartX, height, fadePaint)
        }
        if (rangeEnd < 1f) {
            canvas.drawRect(rangeEndX, 0f, width, height, fadePaint)
        }

        // draw touch ripple
        val touchRippleRadius = this.touchRippleRadius
        if (touchRippleRadius > 0f) {
            val cx = when (dragMode) {
                DRAG_RANGE_START -> rangeStartX + windowStrokeWidthLeftRight / 2f
                DRAG_RANGE_END -> rangeEndX - windowStrokeWidthLeftRight / 2f
                DRAG_RANGE -> rangeStartX + (rangeEndX - rangeStartX) / 2f
                else -> null
            }
            if (cx != null) {
                touchRipplePaint.alpha = (102 * touchRippleAlpha).toInt()
                canvas.drawCircle(cx, height / 2f, touchRippleRadius, touchRipplePaint)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                val rangeStartX = width * rangeStart
                val rangeEndX = width * rangeEnd

                downX = event.x
                lastTouchX = event.x
                inDragMode = false

                // determine drag mode depending on touch region
                dragMode = when (event.x) {
                    in (rangeStartX - dragSize)..(rangeStartX + dragSize + windowStrokeWidthLeftRight) -> DRAG_RANGE_START
                    in (rangeEndX - dragSize - windowStrokeWidthLeftRight)..(rangeEndX + dragSize) -> DRAG_RANGE_END
                    in (rangeStartX + dragSize + windowStrokeWidthLeftRight)..(rangeEndX - dragSize - windowStrokeWidthLeftRight) -> DRAG_RANGE
                    else -> DRAG_NONE
                }

                showTouchRippleAnimation()
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
        val rangeStartX = width * rangeStart
        val rangeEndX = width * rangeEnd

        when (dragMode) {
            DRAG_RANGE_START -> {
                // move left edge of range window (0..rangeEnd)
                var newRangeStart = eventX / width
                if (newRangeStart < 0f) newRangeStart = 0f

                var newRangeStartX = width * newRangeStart
                if (newRangeStartX + dragSize + windowStrokeWidthLeftRight > rangeEndX - dragSize - windowStrokeWidthLeftRight) {
                    newRangeStartX = rangeEndX - 2f * (dragSize + windowStrokeWidthLeftRight)
                    newRangeStart = newRangeStartX / width
                }
                updateRanges(newRangeStart, rangeEnd)
            }
            DRAG_RANGE_END -> {
                // move right edge of range window (rangeStart..1)
                var newRangeEnd = eventX / width
                if (newRangeEnd > 1f) newRangeEnd = 1f

                var newRangeEndX = width * newRangeEnd
                if (newRangeEndX - dragSize - windowStrokeWidthLeftRight < rangeStartX + dragSize + windowStrokeWidthLeftRight) {
                    newRangeEndX = rangeStartX + 2f * (dragSize + windowStrokeWidthLeftRight)
                    newRangeEnd = newRangeEndX / width
                }
                updateRanges(rangeStart, newRangeEnd)
            }
            DRAG_RANGE -> {
                // move whole window
                var newRangeStart = (rangeStartX - distanceX) / width
                if (newRangeStart < 0f) newRangeStart = 0f

                var newRangeEnd = newRangeStart + (rangeEnd - rangeStart)
                if (newRangeEnd > 1f) {
                    newRangeEnd = 1f
                    newRangeStart = 1f - (rangeEnd - rangeStart)
                }
                updateRanges(newRangeStart, newRangeEnd)
            }
        }
    }

    private fun updateRanges(start: Float, end: Float) {
        if (rangeStart != start || rangeEnd != end) {
            rangeStart = start
            rangeEnd = end

            invalidate()

            onRangeChangeListener?.invoke(start, end)
        }
    }

    private fun showTouchRippleAnimation() {
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

    // =============

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        uiChart?.let {
            enqueueUpdateData(it, width, height)
        }
    }

    fun attachToChartView(chartView: ChartView) {
        enqueueUpdateData(chartView.getUiChart(), width, height)

        chartView.setOnUiChartChangeListener { uiChart ->
            enqueueUpdateData(uiChart, width, height)
        }

        onRangeChangeListener = { start, end -> chartView.setRange(start, end) }

        chartView.setRange(rangeStart, rangeEnd)
    }

    private fun enqueueUpdateData(uiChart: UiChart?, viewWidth: Int, viewHeight: Int) {
        val updateGeneration = ++lastUpdateGeneration
        updateDataExecutor.submit {
            val newGraphs = updateData(uiChart, viewWidth, viewHeight)
            if (lastUpdateGeneration == updateGeneration) {
                post {
                    graphs = newGraphs
                    invalidate()
                }
            }
        }
    }

    private fun updateData(uiChart: UiChart?, viewWidth: Int, viewHeight: Int): List<Graph>? {
        if (uiChart == null) return null

        val scaleX = 1f * viewWidth.toFloat() / uiChart.width
        val scaleY = 0.85f * viewHeight / uiChart.height

        val scaledChartWidth = uiChart.width * scaleX
        val scaledChartHeight = uiChart.height * scaleY

        val xOffs = (width - scaledChartWidth) / 2f
        val yOffs = height - (height - scaledChartHeight) / 2f

        val matrix = Matrix()
        matrix.setTranslate(xOffs, yOffs)
        matrix.preScale(scaleX, -scaleY)

        return uiChart.graphs.map {
            it.transform(matrix)
        }
    }

}
