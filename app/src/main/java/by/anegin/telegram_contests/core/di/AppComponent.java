package by.anegin.telegram_contests.core.di;

import by.anegin.telegram_contests.core.data.DataRepository;
import by.anegin.telegram_contests.core.ui.ThemeHelper;

public interface AppComponent {

    DataRepository getDataRepository();

    ThemeHelper getThemeHelper();

}