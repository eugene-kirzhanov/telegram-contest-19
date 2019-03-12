package by.anegin.telegram_contests.ui.model

import android.graphics.Matrix
import android.graphics.Path
import by.anegin.telegram_contests.data.model.Chart

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

            val graphs = chart.lines.map { line ->
                val path = Path()
                val count = Math.min(xValuesCount, line.values.size)
                if (count > 0) {
                    path.moveTo(chart.x.values[0].toFloat(), line.values[0].toFloat())
                    for (i in 1 until count) {
                        path.lineTo(chart.x.values[i].toFloat(), line.values[i].toFloat())
                    }
                    path.transform(matrix)
                }
                Graph(path, line.color)
            }

            val chartWidth = maxX - minX
            val chartHeight = maxY - minY

            return UiChart(graphs, chartWidth, chartHeight)
        }
    }

}