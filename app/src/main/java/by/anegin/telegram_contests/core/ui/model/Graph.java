package by.anegin.telegram_contests.core.ui.model;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class Graph {

    private final float[] points;     // x0, y0, x1, y1, x1, y1, x2, y2, ...
    private final int color;

    Graph(float[] points, final int color) {
        this.points = points;
        this.color = color;
    }

    public void draw(Canvas canvas, Paint paint) {
        paint.setColor(color);
        if (points.length > 3) {
            canvas.drawLines(points, paint);
        }
    }

    public Graph transform(Matrix matrix) {
        float[] scaledPoints = new float[points.length];
        matrix.mapPoints(scaledPoints, points);
        return new Graph(scaledPoints, color);
    }

}
