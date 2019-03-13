package by.anegin.telegram_contests.core.data

import by.anegin.telegram_contests.core.data.model.Data
import by.anegin.telegram_contests.core.data.source.DataSource

class DataRepository(private val dataSource: DataSource) {

    val data: Data by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        dataSource.getData()
    }

}