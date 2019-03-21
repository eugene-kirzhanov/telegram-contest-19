package by.anegin.telegram_contests.core.ui.model;

import android.graphics.Matrix;
import by.anegin.telegram_contests.core.data.model.Chart;
import by.anegin.telegram_contests.core.data.model.Column;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UiChart {

    public final long[] xValues;
    public final List<UiDate> dates;
    public final List<Graph> graphs;
    public final long width;
    public final long minX;
    public final long minY;

    public UiChart(Chart chart) {
        if (chart.x.values.length == 0 || chart.lines.isEmpty()) {
            xValues = new long[0];
            dates = new ArrayList<>();
            graphs = new ArrayList<>();
            width = 0;
            minX = 0;
            minY = 0;
            return;
        }

        long[] minMaxX = findMinMax(chart.x.values);
        minX = minMaxX[0];

        xValues = new long[chart.x.values.length];
        for (int i = 0; i < xValues.length; i++) {
            xValues[i] = chart.x.values[i] - minMaxX[0];
        }

        long minY = Long.MAX_VALUE;
        long maxY = Long.MIN_VALUE;
        for (Column.Line line : chart.lines) {
            int count = Math.min(chart.x.values.length, line.values.length);
            for (int i = 0; i < count; i++) {
                if (line.values[i] > maxY) maxY = line.values[i];
                if (line.values[i] < minY) minY = line.values[i];
            }
        }

        this.minY = minY;

        Matrix matrix = new Matrix();
        matrix.setTranslate(-minMaxX[0], -minY);

        graphs = new ArrayList<>();
        for (Column.Line line : chart.lines) {
            float[] points = lineToPoints(chart.x.values, line.values);
            if (points.length > 0) {
                matrix.mapPoints(points);
                graphs.add(new Graph(line.id, line.values, points, line.color));
            }
        }

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.US);
        dates = new ArrayList<>(chart.x.values.length);
        for (long xv : chart.x.values) {
            String dateString = sdf.format(new Date(xv));
            dates.add(new UiDate(xv - minMaxX[0], dateString));
        }

        width = minMaxX[1] - minMaxX[0];
    }

    private long[] findMinMax(long[] values) {
        long[] minMax = new long[]{values[0], values[0]};
        for (int i = 1; i < values.length; i++) {
            if (values[i] < minMax[0]) minMax[0] = values[i];
            if (values[i] > minMax[1]) minMax[1] = values[i];
        }
        return minMax;
    }

    private float[] lineToPoints(long[] x, long[] y) {
        int count = Math.min(x.length, y.length);
        if (count < 2) return new float[]{};

        float[] points = new float[4 * (count - 1)];
        points[0] = x[0];
        points[1] = y[0];
        int j = 2;
        for (int i = 1; i < count - 1; i++) {
            points[j] = x[i];
            points[j + 1] = y[i];
            points[j + 2] = points[j];
            points[j + 3] = points[j + 1];
            j += 4;
        }
        points[j] = x[count - 1];
        points[j + 1] = y[count - 1];

        return points;
    }

}
