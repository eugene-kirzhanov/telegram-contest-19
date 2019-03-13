package by.anegin.telegram_contests.ui.model

import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint

class Graph(
    private val points: FloatArray,     // x0, y0, x1, y1,  x1, y1, x2, y2,  x2, y2, x3, y3, ...
    private val color: Int
) {

    fun draw(canvas: Canvas, paint: Paint) {
        paint.color = color
        if (points.size > 3) {
            canvas.drawLines(points, paint)
        }
    }

    fun transform(matrix: Matrix): Graph {
        val scaledPoints = FloatArray(points.size)
        matrix.mapPoints(scaledPoints, points)
        return Graph(scaledPoints, color)
    }

}