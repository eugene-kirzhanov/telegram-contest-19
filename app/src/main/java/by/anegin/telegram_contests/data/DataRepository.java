package by.anegin.telegram_contests.data;

import by.anegin.telegram_contests.data.model.Data;
import by.anegin.telegram_contests.data.source.DataSource;

import java.io.IOException;

public class DataRepository {

    private final DataSource dataSource;

    private Data data;

    public DataRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Data getData() throws IOException {
        if (data == null) {
            data = dataSource.getData();
        }
        return data;
    }

}