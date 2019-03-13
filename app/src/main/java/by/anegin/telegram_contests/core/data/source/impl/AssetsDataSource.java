package by.anegin.telegram_contests.core.data.source.impl;

import android.content.Context;
import android.graphics.Color;
import by.anegin.telegram_contests.core.data.model.Chart;
import by.anegin.telegram_contests.core.data.model.Column;
import by.anegin.telegram_contests.core.data.model.Data;
import by.anegin.telegram_contests.core.data.source.DataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class AssetsDataSource implements DataSource {

    private final Context context;
    private final String assetName;

    public AssetsDataSource(Context context, String assetName) {
        this.context = context;
        this.assetName = assetName;
    }

    @Override
    public Data getData() throws IOException {
        String jsonString;
        try (InputStream is = context.getAssets().open(assetName)) {
            Reader reader = new InputStreamReader(is, Charset.forName("UTF-8"));
            Writer buffer = new StringWriter();
            char[] buf = new char[8192];
            int chars = reader.read(buf);
            while (chars >= 0) {
                buffer.write(buf, 0, chars);
                chars = reader.read(buf);
            }
            jsonString = buffer.toString();
        }
        try {
            return parseJsonString(jsonString);
        } catch (JSONException e) {
            throw new IOException(e);
        }
    }

    private Data parseJsonString(String jsonString) throws JSONException {
        List<Chart> charts = new ArrayList<>();

        JSONArray dataJson = new JSONArray(jsonString);
        for (int i = 0; i < dataJson.length(); i++) {
            JSONObject chartJson = dataJson.optJSONObject(i);
            if (chartJson == null) continue;

            JSONArray columnsJson = chartJson.optJSONArray("columns");
            JSONObject typesJson = chartJson.optJSONObject("types");
            JSONObject namesJson = chartJson.optJSONObject("names");
            JSONObject colorsJson = chartJson.optJSONObject("colors");
            if (columnsJson == null || typesJson == null || namesJson == null || colorsJson == null) continue;
            if (columnsJson.length() == 0 || typesJson.length() == 0 || namesJson.length() == 0 || colorsJson.length() == 0)
                continue;

            Column.X x = null;
            List<Column.Line> lines = new ArrayList<>();
            for (int j = 0; j < columnsJson.length(); j++) {

                JSONArray columnJson = columnsJson.optJSONArray(j);
                if (columnJson == null) continue;

                String columnId = columnJson.optString(0);
                String columnType = typesJson.optString(columnId);
                if (columnType == null) continue;

                int valuesCount = columnJson.length() - 1;
                if ("x".equals(columnType)) {

                    long[] columnValues = new long[valuesCount];
                    for (int v = 0; v < valuesCount; v++) {
                        columnValues[v] = columnJson.optLong(v + 1);
                    }

                    x = new Column.X(columnId, columnValues);

                } else if ("line".equals(columnType)) {

                    String columnName = namesJson.optString(columnId);
                    if (columnName == null) columnName = "No name";

                    int columnColor;
                    try {
                        columnColor = Color.parseColor(colorsJson.optString(columnId));
                    } catch (IllegalArgumentException e) {
                        columnColor = Color.BLACK;
                    }

                    long[] columnValues = new long[valuesCount];
                    for (int v = 0; v < valuesCount; v++) {
                        columnValues[v] = columnJson.optLong(v + 1);
                    }

                    lines.add(new Column.Line(columnId, columnName, columnColor, columnValues));
                }
            }

            if (x != null && lines.size() > 0) {
                charts.add(new Chart(x, lines));
            }
        }

        return new Data(charts);
    }

}