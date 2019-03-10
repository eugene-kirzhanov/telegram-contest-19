package by.anegin.telegram_contests.ui

import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import by.anegin.telegram_contests.R
import by.anegin.telegram_contests.data.source.DataSource
import by.anegin.telegram_contests.di.app
import by.anegin.telegram_contests.data.model.Data
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private val dataSource: DataSource = app().dataSource

    private val loadExecutor = Executors.newSingleThreadExecutor()

    private val uiHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
        Log.v("ABC", "data: $data")
    }

}