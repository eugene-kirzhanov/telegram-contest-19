package by.anegin.telegram_contests.ui.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.ui.model.UiChart

class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var onUiChartChangeListener: ((UiChart?) -> Unit)? = null

    private val contentBgPaint = Paint()
    private val graphPathPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG)

    private var rangeStart = 0f
    private var rangeEnd = 1f

    private var uiChart: UiChart? = null

    private val graphPath = Path()
    private val pathMatrix = Matrix()

    init {
        val viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.ChartView, defStyleAttr, 0)
        val contentBgColor = viewAttrs.getColor(R.styleable.ChartView_content_bg_color, Color.TRANSPARENT)
        viewAttrs.recycle()

        contentBgPaint.style = Paint.Style.FILL
        contentBgPaint.color = contentBgColor

        graphPathPaint.style = Paint.Style.STROKE
        graphPathPaint.strokeWidth =
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.resources.displayMetrics)
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        val width = width.toFloat()
        val height = height.toFloat()

        canvas.drawRect(0f, 0f, width, height, contentBgPaint)

        val uiChart = this.uiChart ?: return

        val visibleChartWidth = uiChart.width * (rangeEnd - rangeStart)
        val xScale = width / uiChart.width
        val yScale = height / uiChart.height

        Log.v("ABC", String.format("visibleChartWidth: %.5f", visibleChartWidth))

        val xOffs = -xScale * rangeStart * uiChart.width

        pathMatrix.reset()
        pathMatrix.setTranslate(xOffs, height)
        pathMatrix.preScale(1f, -yScale)

        uiChart.graphs.forEach { graph ->
            graphPathPaint.color = graph.color

            val p = Path(graph.path)
            //graphPath.reset()
            //graphPath.addPath(graph.path)

            //canvas.drawPath(graphPath, graphPathPaint)

            p.transform(pathMatrix)

            canvas.drawPath(p, graphPathPaint)
        }
    }

    fun setRange(start: Float, end: Float) {
        if (rangeStart != start || rangeEnd != end) {
            rangeStart = start
            rangeEnd = end
            invalidate()
        }
    }

    fun setUiChart(uiChart: UiChart) {
        this.uiChart = uiChart
        onUiChartChangeListener?.invoke(uiChart)
        invalidate()
    }

    fun getUiChart() = uiChart

    fun setOnUiChartChangeListener(listener: ((UiChart?) -> Unit)?) {
        this.onUiChartChangeListener = listener
    }

}