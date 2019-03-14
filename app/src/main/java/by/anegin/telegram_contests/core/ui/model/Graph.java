package by.anegin.telegram_contests.core.ui.model;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class Graph {

    public static final int STATE_VISIBLE = 1;
    public static final int STATE_HIDDEN = 2;
    public static final int STATE_SHOWING = 3;
    public static final int STATE_HIDING = 4;

    public final String id;

    private final float[] points;     // x0, y0, x1, y1, x1, y1, x2, y2, ...

    private final int color;

    private final int state;

    Graph(String id, float[] points, final int color, final int state) {
        this.id = id;
        this.points = points;
        this.color = color;
        this.state = state;
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
        return new Graph(id, scaledPoints, color, state);
    }

    public float findMaxYInRange(float startX, float endX) {
        if (state == STATE_HIDDEN || state == STATE_SHOWING) return 0f;
        float maxY = 0f;
        int i = 0;
        while (i + 3 < points.length) {
            if (!(points[i] < startX && points[i + 2] < startX) && !(points[i] > endX && points[i + 2] > endX)) {
                if (points[i + 1] > maxY) maxY = points[i + 1];
                if (points[i + 3] > maxY) maxY = points[i + 3];
            }
            i += 4;
        }
        return maxY;
    }

}
