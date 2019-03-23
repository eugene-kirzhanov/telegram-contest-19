package by.anegin.tgcontest.core.di;

import by.anegin.tgcontest.data.DataRepository;
import by.anegin.tgcontest.core.utils.ThemeHelper;

public interface AppComponent {

    DataRepository getDataRepository();

    ThemeHelper getThemeHelper();

}