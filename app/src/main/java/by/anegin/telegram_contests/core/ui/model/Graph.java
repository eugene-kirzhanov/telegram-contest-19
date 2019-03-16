package by.anegin.telegram_contests.core.ui.model;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;

public class Graph {

    private static final int STATE_VISIBLE = 1;
    private static final int STATE_HIDDEN = 2;
    private static final int STATE_SHOWING = 3;
    private static final int STATE_HIDING = 4;

    private final String id;

    private final float[] points;     // x0, y0, x1, y1, x1, y1, x2, y2, ...
    private final float[] transformedPoints;

    private int state = STATE_VISIBLE;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    // copy constructor
    public Graph(Graph graph, float strokeWidth) {
        this(graph.id, graph.points, graph.paint.getColor());
        paint.setStrokeWidth(strokeWidth);
    }

    Graph(String id, float[] points, final int color) {
        this.id = id;
        this.points = points;
        this.transformedPoints = new float[points.length];
        System.arraycopy(points, 0, transformedPoints, 0, points.length);
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
    }

    public void draw(Canvas canvas) {
        if (transformedPoints.length > 3 && state == STATE_VISIBLE) {
            canvas.drawLines(transformedPoints, paint);
        }
    }

    public void transformPoints(Matrix matrix) {
        matrix.mapPoints(transformedPoints, points);
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
