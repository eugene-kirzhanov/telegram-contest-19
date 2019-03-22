package by.anegin.telegram_contests.core.ui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;

public class Grid {

    private static final float GAPS_COUNT = 5f;
    private static final int LINES_COUNT = (int) Math.floor(GAPS_COUNT);

    private final Paint linePaint = new Paint();
    private final TextPaint textPaint = new TextPaint();

    private final float[] levels = new float[LINES_COUNT];

    private final float fromYScale;
    private final float targetYScale;

    public Grid(float fromYScale, float targetYScale, float minY, float maxY, int lineColor, float lineWidth, int textColor, float textSize) {
        this.fromYScale = fromYScale;
        this.targetYScale = targetYScale;

        float gapHeight = (maxY - minY) / GAPS_COUNT;

        float y = gapHeight;
        for (int i = 0; i < LINES_COUNT; i++) {
            levels[i] = y;
            y += gapHeight;
        }

        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineWidth);

        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);
    }

    public void draw(Canvas canvas, float yScale) {
        int alpha = 255;
        if (targetYScale != fromYScale) {
            alpha = (int) (255 * (yScale - fromYScale) / (targetYScale - fromYScale));
        }
        linePaint.setAlpha(alpha);
        textPaint.setAlpha(alpha);

        for (float level : levels) {
            float y = canvas.getHeight() - level * yScale;
            canvas.drawLine(0f, y, canvas.getWidth(), y, linePaint);

            String yString = String.valueOf((int) level);
            canvas.drawText(yString, 0f, y - textPaint.descent(), textPaint);
        }
    }

}
