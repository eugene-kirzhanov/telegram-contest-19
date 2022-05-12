package by.anegin.tgcontest;

import android.app.Application;
import by.anegin.tgcontest.core.di.AppComponent;
import by.anegin.tgcontest.core.di.impl.AppComponentImpl;

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
