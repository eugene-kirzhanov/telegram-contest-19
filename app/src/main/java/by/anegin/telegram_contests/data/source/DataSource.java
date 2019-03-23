package by.anegin.telegram_contests.data.source;

import by.anegin.telegram_contests.data.model.Data;

import java.io.IOException;

public interface DataSource {

    Data getData() throws IOException;

}
