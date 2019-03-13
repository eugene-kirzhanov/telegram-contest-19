package by.anegin.telegram_contests.core.di.impl

import android.content.Context
import by.anegin.telegram_contests.core.data.DataRepository
import by.anegin.telegram_contests.core.data.source.impl.AssetsDataSource
import by.anegin.telegram_contests.core.di.AppComponent

class AppComponentImpl(appContext: Context) : AppComponent {

    private val dataRepositoryInstance: DataRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        DataRepository(dataSource)
    }

    override val dataSource = AssetsDataSource(appContext, "chart_data.json")

    override val dataRepository = dataRepositoryInstance

}