package by.anegin.telegram_contests.ui

import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.data.model.Data
import by.anegin.telegram_contests.data.source.DataSource
import by.anegin.telegram_contests.di.app
import by.anegin.telegram_contests.ui.view.MiniChartView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val dataSource: DataSource = app().dataSource

    private val loadExecutor = Executors.newSingleThreadExecutor()

    private val uiHandler = Handler()

    private lateinit var miniChartView: MiniChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        miniChartView = findViewById(R.id.miniChartView)

        loadData()
    }

    private fun loadData() {
        loadExecutor.execute {
            val data = dataSource.getData()
            uiHandler.post {
                showData(data)
            }
        }
    }

    private fun showData(data: Data) {
        if (data.charts.isNotEmpty()) {
            val chart = data.charts[0]
            miniChartView.setChart(chart)
        } else {
            Toast.makeText(this, "No charts in data", Toast.LENGTH_SHORT).show()
        }
    }

}