package by.anegin.telegram_contests.ui.view

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import androidx.annotation.WorkerThread
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.ui.model.Graph
import by.anegin.telegram_contests.ui.model.UiChart
import java.util.concurrent.Executors

class MiniChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {


    class Data(
        val chart: UiChart,
        val viewWidth: Int,
        val viewHeight: Int,
        val graphs: List<Graph>,
        val bitmap: Bitmap,
        val canvas: Canvas
    )

    companion object {
        private const val DRAG_RANGE = 1
        private const val DRAG_RANGE_START = 2
        private const val DRAG_RANGE_END = 3
        private const val DRAG_NONE = 4
    }

    private val windowPaint = Paint()
    private val bgPaint = Paint()
    private val touchRipplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val chartPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private val chartAlphaPaint = Paint()

    private val fadeBgColor: Int
    private val fadeColor: Int

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

    private var onRangeChangeListener: ((Float, Float) -> Unit)? = null

    private val uiHandler = Handler(Looper.getMainLooper())
    private val updateDataExecutor = Executors.newFixedThreadPool(2)
    private var lastUpdateGeneration = 0L

    private var data: Data? = null

    init {
        val typedValue = TypedValue()
        val defAttrs = context.obtainStyledAttributes(typedValue.data, intArrayOf(R.attr.colorPrimary))
        val primaryColor = defAttrs.getColor(0, 0)
        defAttrs.recycle()

        val viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.MiniChartView, defStyleAttr, 0)

        rangeStart = viewAttrs.getFloat(R.styleable.MiniChartView_range_start, 0f)
        rangeEnd = viewAttrs.getFloat(R.styleable.MiniChartView_range_end, 1f)
        if (rangeStart < 0f) rangeStart = 0f
        if (rangeEnd > 1f) rangeEnd = 1f

        fadeBgColor = viewAttrs.getColor(R.styleable.MiniChartView_fade_bg_color, primaryColor)
        val fadeAlpha = viewAttrs.getFloat(R.styleable.MiniChartView_fade_alpha, 0.5f)

        val windowStrokeColor = viewAttrs.getColor(R.styleable.MiniChartView_window_stroke_color, primaryColor)
        windowStrokeWidthTopBottom =
            viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_top_bottom, 0f)
        windowStrokeWidthLeftRight =
            viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_left_right, 0f)

        val chartLineWidth = viewAttrs.getDimension(R.styleable.MiniChartView_chart_line_width, 1f)

        viewAttrs.recycle()

        windowPaint.color = windowStrokeColor
        windowPaint.style = Paint.Style.STROKE

        bgPaint.style = Paint.Style.FILL

        touchRipplePaint.style = Paint.Style.FILL
        touchRipplePaint.color = Color.argb(
            (Color.alpha(windowStrokeColor) * 0.4f).toInt(),
            Color.red(windowStrokeColor), Color.green(windowStrokeColor), Color.blue(windowStrokeColor)
        )

        chartPaint.style = Paint.Style.STROKE
        chartPaint.strokeWidth = chartLineWidth

        chartAlphaPaint.style = Paint.Style.FILL
        chartAlphaPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)

        fadeColor = Color.argb((fadeAlpha * 255).toInt(), 255, 255, 255)

        touchSlop = ViewConfiguration.get(context).scaledTouchSlop
        dragSize = 1.5f * touchSlop
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        val width = width.toFloat()
        val height = height.toFloat()

        val rangeStartX = width * rangeStart
        val rangeEndX = width * rangeEnd

        // draw out-of-range background
        bgPaint.color = fadeBgColor
        if (rangeStart > 0f) {
            canvas.drawRect(0f, 0f, rangeStartX, height, bgPaint)
        }
        if (rangeEnd < 1f) {
            canvas.drawRect(rangeEndX, 0f, width, height, bgPaint)
        }

        // draw touch ripples
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
                    canvas.drawCircle(cx, height / 2f, touchRippleRadius, touchRipplePaint)
                }
                DRAG_RANGE_END -> {
                    val cx = rangeEndX - windowStrokeWidthLeftRight / 2f
                    canvas.drawCircle(cx, height / 2f, touchRippleRadius, touchRipplePaint)
                }
                DRAG_RANGE -> {
                    val cx = rangeStartX + (rangeEndX - rangeStartX) / 2f
                    canvas.drawCircle(cx, height / 2f, touchRippleRadius, touchRipplePaint)
                }
            }
        }

        // draw range window
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

        // draw Chart data
        val data = this.data
        if (data?.graphs?.isNotEmpty() == true) {

            // clear chart bitmap
            data.bitmap.eraseColor(Color.TRANSPARENT)

            // draw chart graphics on bitmap
            data.graphs.forEach { graph ->
                chartPaint.color = graph.color
                data.canvas.drawPath(graph.path, chartPaint)
            }

            // draw out-of-ranges on bitmap with xferMode to fade out parts of chart
            chartAlphaPaint.color = fadeColor
            if (rangeStart > 0f) {
                data.canvas.drawRect(0f, 0f, rangeStartX, data.bitmap.height.toFloat(), chartAlphaPaint)
            }
            if (rangeEnd < 1f) {
                data.canvas.drawRect(
                    rangeEndX,
                    0f,
                    data.bitmap.width.toFloat(),
                    data.bitmap.height.toFloat(),
                    chartAlphaPaint
                )
            }
            chartAlphaPaint.color = Color.WHITE
            data.canvas.drawRect(rangeStartX, 0f, rangeEndX, data.bitmap.height.toFloat(), chartAlphaPaint)

            // draw chart bitmap
            val xOffs = (width - data.bitmap.width) / 2f
            val yOffs = height - (height - data.bitmap.height) / 2f
            canvas.save()
            canvas.translate(xOffs, yOffs)
            canvas.scale(1f, -1f)
            canvas.drawBitmap(data.bitmap, 0f, 0f, null)
            canvas.restore()
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
        super.onSizeChanged(w, h, oldw, oldh)
        val data = this.data
        if (data != null && (data.viewWidth != width || data.viewHeight != height)) {
            enqueueUpdateData(data.chart, width, height)
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
            val newChartData = updateData(uiChart, viewWidth, viewHeight)
            if (lastUpdateGeneration == updateGeneration) {
                uiHandler.post {
                    data?.bitmap?.recycle()
                    data = newChartData
                    invalidate()
                }
            }
        }
    }

    @WorkerThread
    private fun updateData(uiChart: UiChart?, viewWidth: Int, viewHeight: Int): Data? {
        if (uiChart == null) return null

        val scaleX = 1f * viewWidth.toFloat() / uiChart.width
        val scaleY = 0.85f * viewHeight / uiChart.height

        val matrix = Matrix()
        matrix.postScale(scaleX, scaleY)
        uiChart.graphs.forEach {
            it.path.transform(matrix)
        }

        val scaledChartWidth = (uiChart.width * scaleX).toInt()
        val scaledChartHeight = (uiChart.height * scaleY).toInt()

        val chartBitmap = Bitmap.createBitmap(scaledChartWidth, scaledChartHeight, Bitmap.Config.ARGB_8888)
        val chartBitmapCanvas = Canvas(chartBitmap)

        return Data(
            uiChart, viewWidth, viewHeight,
            uiChart.graphs,
            chartBitmap, chartBitmapCanvas
        )
    }

}
