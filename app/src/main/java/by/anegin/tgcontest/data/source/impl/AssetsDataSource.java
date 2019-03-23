package by.anegin.tgcontest.data.source.impl;

import android.content.Context;
import android.graphics.Color;
import by.anegin.tgcontest.data.model.Chart;
import by.anegin.tgcontest.data.model.Column;
import by.anegin.tgcontest.data.model.Data;
import by.anegin.tgcontest.data.source.DataSource;
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
            if (columnsJson == null || typesJson == null
                    || namesJson == null || colorsJson == null
                    || columnsJson.length() == 0 || typesJson.length() == 0
                    || namesJson.length() == 0 || colorsJson.length() == 0) continue;

            int valuesCount = Integer.MAX_VALUE;
            Column.X x = null;
            List<Column.Line> lines = new ArrayList<>();
            for (int j = 0; j < columnsJson.length(); j++) {

                JSONArray columnJson = columnsJson.optJSONArray(j);
                if (columnJson == null) continue;

                String columnId = columnJson.optString(0);
                String columnType = typesJson.optString(columnId);
                if (columnId == null || columnType == null) continue;

                long[] columnValues = new long[columnJson.length() - 1];
                for (int v = 0; v < columnValues.length; v++) {
                    columnValues[v] = columnJson.optLong(v + 1);
                }
                if (columnValues.length < valuesCount) {
                    valuesCount = columnValues.length;
                }

                if ("x".equals(columnType)) {

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

                    lines.add(new Column.Line(columnId, columnName, columnColor, columnValues));
                }
            }

            if (valuesCount > 0 && x != null && !lines.isEmpty()) {

                if (x.values.length > valuesCount) {
                    long[] trimmedValues = new long[valuesCount];
                    System.arraycopy(x.values, 0, trimmedValues, 0, valuesCount);
                    x = new Column.X(x.id, trimmedValues);
                }

                for (int j = 0; j < lines.size(); j++) {
                    Column.Line line = lines.get(j);
                    if (line.values.length > valuesCount) {
                        long[] trimmedValues = new long[valuesCount];
                        System.arraycopy(line.values, 0, trimmedValues, 0, valuesCount);
                        lines.set(j, new Column.Line(line.id, line.name, line.color, trimmedValues));
                    }
                }

                charts.add(new Chart(x, lines));
            }
        }

        return new Data(charts);
    }

}
