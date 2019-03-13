package by.anegin.telegram_contests.core.ui.model;

import android.graphics.Matrix;
import by.anegin.telegram_contests.core.data.model.Chart;
import by.anegin.telegram_contests.core.data.model.Column;

import java.util.ArrayList;
import java.util.List;

public class UiChart {

    public final List<Graph> graphs;
    public final long width;
    public final long height;

    public UiChart(Chart chart) {
        if (chart.x.values.length == 0 || chart.lines.isEmpty()) {
            graphs = new ArrayList<>();
            width = 0;
            height = 0;
            return;
        }

        int xValuesCount = chart.x.values.length;

        long minX = chart.x.values[0];
        long maxX = minX;
        for (int i = 1; i < xValuesCount; i++) {
            if (chart.x.values[i] > maxX) maxX = chart.x.values[i];
            if (chart.x.values[i] < minX) minX = chart.x.values[i];
        }

        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        for (Column.Line line : chart.lines) {
            int count = Math.min(xValuesCount, line.values.length);
            for (int i = 0; i < count; i++) {
                if (line.values[i] > maxY) maxY = line.values[i];
                if (line.values[i] < minY) minY = line.values[i];
            }
        }

        Matrix matrix = new Matrix();
        matrix.setTranslate(-minX, -minY);

        graphs = new ArrayList<>();
        for (Column.Line line : chart.lines) {
            int count = Math.min(xValuesCount, line.values.length);
            if (count > 1) {
                float[] points = new float[4 * (count - 1)];
                points[0] = chart.x.values[0];
                points[1] = line.values[0];
                int j = 2;
                for (int i = 1; i < count - 1; i++) {
                    points[j] = chart.x.values[i];
                    points[j + 1] = line.values[i];
                    points[j + 2] = points[j];
                    points[j + 3] = points[j + 1];
                    j += 4;
                }
                points[j] = chart.x.values[count - 1];
                points[j + 1] = line.values[count - 1];
                matrix.mapPoints(points);
                graphs.add(new Graph(points, line.color));
            }
        }

        width = maxX - minX;
        height = maxY - minY;
    }

}
