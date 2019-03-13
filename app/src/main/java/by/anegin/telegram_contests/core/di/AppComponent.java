package by.anegin.telegram_contests.core.di;

import by.anegin.telegram_contests.core.data.DataRepository;
import by.anegin.telegram_contests.core.data.source.DataSource;

public interface AppComponent {

    DataSource getDataSource();

    DataRepository getDataRepository();

}