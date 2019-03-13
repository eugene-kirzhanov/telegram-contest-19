package by.anegin.telegram_contests.core.di;

import by.anegin.telegram_contests.core.data.DataRepository;

public interface AppComponent {

    DataRepository getDataRepository();

}