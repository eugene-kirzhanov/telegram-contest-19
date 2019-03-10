package by.anegin.telegram_contests.di

import by.anegin.telegram_contests.data.source.DataSource

interface AppComponent {

    val dataSource: DataSource

    companion object {
        lateinit var instance: AppComponent
    }

}

fun app() = AppComponent.instance