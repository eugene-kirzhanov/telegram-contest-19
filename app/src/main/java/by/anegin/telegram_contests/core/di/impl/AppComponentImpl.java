package by.anegin.telegram_contests.core.di.impl;

import android.content.Context;
import by.anegin.telegram_contests.core.data.DataRepository;
import by.anegin.telegram_contests.core.data.source.DataSource;
import by.anegin.telegram_contests.core.data.source.impl.AssetsDataSource;
import by.anegin.telegram_contests.core.di.AppComponent;
import by.anegin.telegram_contests.core.utils.ThemeHelper;

public class AppComponentImpl implements AppComponent {

    private final Context appContext;
    private DataRepository dataRepositoryInstance;

    public AppComponentImpl(Context appContext) {
        this.appContext = appContext;
    }

    @Override
    public DataRepository getDataRepository() {
        synchronized (this) {
            if (dataRepositoryInstance == null) {
                DataSource dataSource = new AssetsDataSource(appContext, "chart_data.json");
                dataRepositoryInstance = new DataRepository(dataSource);
            }
            return dataRepositoryInstance;
        }
    }

    @Override
    public ThemeHelper getThemeHelper() {
        return new ThemeHelper(appContext);
    }

}