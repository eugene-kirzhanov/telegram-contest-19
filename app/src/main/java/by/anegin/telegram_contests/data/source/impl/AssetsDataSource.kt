package by.anegin.telegram_contests.data.source.impl

import android.content.Context
import android.graphics.Color
import by.anegin.telegram_contests.data.source.DataSource
import by.anegin.telegram_contests.data.model.Chart
import by.anegin.telegram_contests.data.model.Column
import by.anegin.telegram_contests.data.model.Data
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class AssetsDataSource(
    private val context: Context,
    private val assetName: String
) : DataSource {

    @Throws(IOException::class)
    override fun getData(): Data {
        return context.assets.open(assetName).use {
            val jsonString = it.bufferedReader().readText()
            try {
                parseJsonString(jsonString)
            } catch (e: JSONException) {
                throw IOException(e)
            }
        }
    }

    @Throws(JSONException::class)
    private fun parseJsonString(jsonString: String): Data {
        val charts = ArrayList<Chart>()

        val dataJson = JSONArray(jsonString)
        for (i in 0 until dataJson.length()) {
            val chartJson = dataJson.optJSONObject(i) ?: continue

            val columnsJson = chartJson.optJSONArray("columns") ?: continue
            val typesJson = chartJson.optJSONObject("types") ?: continue
            val namesJson = chartJson.optJSONObject("names") ?: continue
            val colorsJson = chartJson.optJSONObject("colors") ?: continue

            if (columnsJson.length() == 0 || typesJson.length() == 0
                || namesJson.length() == 0 || colorsJson.length() == 0
            ) continue

            var x: Column.X? = null
            val lines = ArrayList<Column.Line>()
            for (j in 0 until columnsJson.length()) {
                val columnJson = columnsJson.optJSONArray(j) ?: continue

                val columnId = columnJson.optString(0)
                val columnType = typesJson.opt(columnId) ?: continue

                val valuesCount = columnJson.length() - 1

                if (columnType == "x") {

                    val columnValues = LongArray(valuesCount)
                    for (v in 0 until valuesCount) {
                        columnValues[v] = columnJson.optLong(v + 1)
                    }

                    x = Column.X(columnId, columnValues)

                } else if (columnType == "line") {

                    val columnName = namesJson.optString(columnId) ?: "No name"

                    val columnColor: Int = try {
                        Color.parseColor(colorsJson.optString(columnId))
                    } catch (e: IllegalArgumentException) {
                        Color.BLACK
                    }

                    val columnValues = LongArray(valuesCount)
                    for (v in 0 until valuesCount) {
                        columnValues[v] = columnJson.optLong(v + 1)
                    }

                    lines.add(Column.Line(columnId, columnName, columnColor, columnValues))
                }
            }

            if (x != null && lines.isNotEmpty()) {
                charts.add(Chart(x, lines))
            }
        }

        return Data(charts)
    }

}