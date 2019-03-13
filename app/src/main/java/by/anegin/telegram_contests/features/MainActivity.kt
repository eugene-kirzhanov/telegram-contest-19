package by.anegin.telegram_contests.features

import android.app.Activity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.core.data.model.Data
import by.anegin.telegram_contests.core.di.app
import by.anegin.telegram_contests.core.ui.model.UiChart
import by.anegin.telegram_contests.core.ui.view.ChartView
import by.anegin.telegram_contests.core.ui.view.MiniChartView
import java.util.concurrent.Executors

class MainActivity : Activity() {

    companion object {
        private const val MENUITEM_ID_FIRST = 42
    }

    private val dataRepository = app().dataRepository

    private val loadExecutor = Executors.newSingleThreadExecutor()
    private val showExecutor = Executors.newSingleThreadExecutor()

    private lateinit var chartView: ChartView
    private lateinit var miniChartView: MiniChartView

    private var data: Data? = null
    private var currentChartIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chart_details)

        chartView = findViewById(R.id.chartView)

        miniChartView = findViewById(R.id.miniChartView)
        miniChartView.attachToChartView(chartView)

        loadData()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val chartsCount = data?.charts?.size ?: 0
        menu.removeGroup(R.id.action_group_chart_selection)
        for (i in 0 until chartsCount) {
            val item = menu.add(R.id.action_group_chart_selection, MENUITEM_ID_FIRST + i, i, "Chart ${i + 1}")
            item.isCheckable = true
            item.isChecked = currentChartIndex == i
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val itemId = item.itemId
        val chartsCount = data?.charts?.size ?: 0
        return if (chartsCount > 0 && itemId >= MENUITEM_ID_FIRST && itemId < MENUITEM_ID_FIRST + chartsCount) {
            val index = itemId - MENUITEM_ID_FIRST
            showChart(index)
            true
        } else {
            when (item.itemId) {
                R.id.action_toggle_theme -> {
                    toggleTheme()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
        }
    }

    private fun loadData() {
        loadExecutor.execute {
            this.data = dataRepository.data
            showChart(0)
        }
    }

    private fun showChart(index: Int) {
        currentChartIndex = index
        showExecutor.execute {
            val data = data
            val chartsCount = data?.charts?.size ?: return@execute
            if (index >= chartsCount) return@execute

            val chart = data.charts[index]
            val uiChart = UiChart.fromChart(chart)
            if (uiChart != null) {
                runOnUiThread {
                    chartView.setUiChart(uiChart)
                    invalidateOptionsMenu()
                }
            }
        }
    }

    private fun toggleTheme() {
        // todo
    }

}