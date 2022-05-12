package by.anegin.tgcontest.data.source;

import by.anegin.tgcontest.data.model.Data;

import java.io.IOException;

public interface DataSource {

    Data getData() throws IOException;

}
