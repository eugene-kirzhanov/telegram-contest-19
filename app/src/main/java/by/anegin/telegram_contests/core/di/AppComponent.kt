package by.anegin.telegram_contests.core.di

import by.anegin.telegram_contests.core.data.DataRepository
import by.anegin.telegram_contests.core.data.source.DataSource

interface AppComponent {

    val dataSource: DataSource

    val dataRepository: DataRepository

    companion object {
        lateinit var instance: AppComponent
    }

}

fun app() = AppComponent.instance