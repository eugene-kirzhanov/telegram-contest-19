package by.anegin.telegram_contests.core.data.source

import by.anegin.telegram_contests.core.data.model.Data

interface DataSource {

    fun getData(): Data

}