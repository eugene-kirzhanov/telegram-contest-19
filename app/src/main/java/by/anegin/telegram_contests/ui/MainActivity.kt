package by.anegin.telegram_contests.ui

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.data.source.DataSource
import by.anegin.telegram_contests.di.app
import by.anegin.telegram_contests.ui.model.UiChart
import by.anegin.telegram_contests.ui.view.ChartView
import by.anegin.telegram_contests.ui.view.MiniChartView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val dataSource: DataSource = app().dataSource

    private val loadExecutor = Executors.newSingleThreadExecutor()

    private val uiHandler = Handler()

    private lateinit var chartView: ChartView
    private lateinit var miniChartView: MiniChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        chartView = findViewById(R.id.chartView)

        miniChartView = findViewById(R.id.miniChartView)
        miniChartView.attachToChartView(chartView)

        loadData()
    }

    private fun loadData() {
        loadExecutor.execute {
            val data = dataSource.getData()
            val uiChart = if (data.charts.isNotEmpty()) {
                UiChart.fromChart(data.charts[0])
            } else {
                null
            }
            uiHandler.post {
                showUiChart(uiChart)
            }
        }
    }

    private fun showUiChart(uiChart: UiChart?) {
        if (uiChart != null) {
            chartView.setUiChart(uiChart)
        } else {
            Toast.makeText(this, "No charts in data", Toast.LENGTH_SHORT).show()
        }
    }

}