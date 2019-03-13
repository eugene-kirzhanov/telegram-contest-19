package by.anegin.telegram_contests

import android.app.Application
import by.anegin.telegram_contests.core.di.AppComponent
import by.anegin.telegram_contests.core.di.impl.AppComponentImpl

class ChartsApp : Application() {

    override fun onCreate() {
        super.onCreate()
        AppComponent.instance = AppComponentImpl(this)
    }

}