package by.anegin.telegram_contests.core.ui.model

import android.graphics.Matrix
import by.anegin.telegram_contests.core.data.model.Chart

class UiChart(
    val graphs: List<Graph>,
    val width: Long,
    val height: Long
) {

    companion object {
        fun fromChart(chart: Chart): UiChart? {
            if (chart.x.values.isEmpty() || chart.lines.isEmpty()) return null

            val xValuesCount = chart.x.values.size

            var minX = chart.x.values[0]
            var maxX = minX
            for (i in 1 until xValuesCount) {
                if (chart.x.values[i] > maxX) maxX = chart.x.values[i]
                if (chart.x.values[i] < minX) minX = chart.x.values[i]
            }

            var minY = Long.MAX_VALUE
            var maxY = Long.MIN_VALUE
            chart.lines.forEach { line ->
                val count = Math.min(xValuesCount, line.values.size)
                for (i in 0 until count) {
                    if (line.values[i] > maxY) maxY = line.values[i]
                    if (line.values[i] < minY) minY = line.values[i]
                }
            }

            val matrix = Matrix()
            matrix.setTranslate(-minX.toFloat(), -minY.toFloat())
            val graphs = chart.lines.mapNotNull { line ->
                val count = Math.min(xValuesCount, line.values.size)
                if (count > 1) {
                    val points = FloatArray(4 * (count - 1))
                    points[0] = chart.x.values[0].toFloat()
                    points[1] = line.values[0].toFloat()
                    var j = 2
                    for (i in 1 until count - 1) {
                        points[j] = chart.x.values[i].toFloat()
                        points[j + 1] = line.values[i].toFloat()
                        points[j + 2] = points[j]
                        points[j + 3] = points[j + 1]
                        j += 4
                    }
                    points[j] = chart.x.values[count - 1].toFloat()
                    points[j + 1] = line.values[count - 1].toFloat()
                    matrix.mapPoints(points)
                    Graph(points, line.color)
                } else {
                    null
                }
            }

            val chartWidth = maxX - minX
            val chartHeight = maxY - minY

            return UiChart(graphs, chartWidth, chartHeight)
        }
    }

}