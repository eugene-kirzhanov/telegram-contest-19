package by.anegin.telegram_contests;

import android.app.Application;
import by.anegin.telegram_contests.core.di.AppComponent;
import by.anegin.telegram_contests.core.di.impl.AppComponentImpl;

public class ChartsApp extends Application {

    private static AppComponent appComponent;

    @Override
    public void onCreate() {
        super.onCreate();
        appComponent = new AppComponentImpl(this);
    }

    public AppComponent getAppComponent() {
        return appComponent;
    }

}
