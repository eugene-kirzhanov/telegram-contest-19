package by.anegin.telegram_contests.core.data.source;

import by.anegin.telegram_contests.core.data.model.Data;

import java.io.IOException;

public interface DataSource {

    Data getData() throws IOException;

}
