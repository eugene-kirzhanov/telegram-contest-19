package by.anegin.telegram_contests.data.source

import by.anegin.telegram_contests.data.model.Data

interface DataSource {

    fun getData(): Data

}