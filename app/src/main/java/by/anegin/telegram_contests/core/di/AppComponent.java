package by.anegin.telegram_contests.core.di;

import by.anegin.telegram_contests.data.DataRepository;
import by.anegin.telegram_contests.core.utils.ThemeHelper;

public interface AppComponent {

    DataRepository getDataRepository();

    ThemeHelper getThemeHelper();

}