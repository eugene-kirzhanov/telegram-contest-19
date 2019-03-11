package by.anegin.telegram_contests.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import by.anegin.telegram_contests.R

class ChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val contentBgPaint = Paint()

    init {
        val viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.ChartView, defStyleAttr, 0)
        val contentBgColor = viewAttrs.getColor(R.styleable.ChartView_content_bg_color, Color.TRANSPARENT)
        viewAttrs.recycle()

        contentBgPaint.style = Paint.Style.FILL
        contentBgPaint.color = contentBgColor
    }

    override fun onDraw(canvas: Canvas) {
        if (width == 0 || height == 0) return

        canvas.drawRect(
            paddingStart.toFloat(), paddingTop.toFloat(),
            (width - paddingEnd).toFloat(), (height - paddingBottom).toFloat(), contentBgPaint
        )

    }

}