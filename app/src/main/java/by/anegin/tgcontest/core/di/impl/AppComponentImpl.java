package by.anegin.tgcontest.core.di.impl;

import android.content.Context;
import by.anegin.tgcontest.data.DataRepository;
import by.anegin.tgcontest.data.source.DataSource;
import by.anegin.tgcontest.data.source.impl.AssetsDataSource;
import by.anegin.tgcontest.core.di.AppComponent;
import by.anegin.tgcontest.core.utils.ThemeHelper;

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