package by.anegin.telegram_contests.di.impl

import android.content.Context
import by.anegin.telegram_contests.data.source.DataSource
import by.anegin.telegram_contests.data.source.impl.AssetsDataSource
import by.anegin.telegram_contests.di.AppComponent

class AppComponentImpl(private val appContext: Context) : AppComponent {

    override val dataSource: DataSource
        get() = AssetsDataSource(appContext, "chart_data.json")

}