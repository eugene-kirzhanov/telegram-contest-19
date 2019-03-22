package by.anegin.telegram_contests.core.ui.objects;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.animation.AccelerateInterpolator;

import by.anegin.telegram_contests.core.ui.view.ChartView;

public class Grid {

    public interface OnFadedOutListener {
        void onFadedOut(Grid grid);
    }

    private static final float GAPS_COUNT = 5.3f;
    private static final int LINES_COUNT = (int) Math.floor(GAPS_COUNT);

    private final Paint linePaint = new Paint();
    private final TextPaint textPaint = new TextPaint();

    private final float fromYScale;
    public final float targetYScale;

    private final float[] levels = new float[LINES_COUNT];

    private float alpha;

    private boolean hiding = false;

    public Grid(float fromYScale, float targetYScale, float maxY, int lineColor, float lineWidth, int textColor, float textSize) {
        this.fromYScale = fromYScale;
        this.targetYScale = targetYScale;

        float gapHeight = maxY / GAPS_COUNT;

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

    public void fadeOut(ChartView chartView, OnFadedOutListener listener) {
        if (hiding) return;
        hiding = true;
        ValueAnimator anim = ValueAnimator.ofFloat(alpha, 0f);
        anim.setInterpolator(new AccelerateInterpolator());
        anim.setDuration((long) (300 * alpha));
        anim.addUpdateListener(animation -> {
            alpha = (float) animation.getAnimatedValue();
            chartView.invalidateOnAnimation();
            if (alpha == 0f) {
                listener.onFadedOut(this);
            }
        });
        anim.start();
    }

    public void draw(Canvas canvas, float yScale) {
        if (!hiding) {
            if (yScale == targetYScale) {
                alpha = 1f;
            } else if ((fromYScale < yScale && yScale < targetYScale) || (targetYScale < yScale && yScale < fromYScale)) {
                alpha = (yScale - fromYScale) / (targetYScale - fromYScale);
            } else {
                alpha = 0f;
            }
        }

        if (alpha == 0f) return;

        linePaint.setAlpha((int) (255 * alpha));
        textPaint.setAlpha((int) (255 * alpha));

        for (float level : levels) {
            float y = canvas.getHeight() - level * yScale;
            canvas.drawLine(0f, y, canvas.getWidth(), y, linePaint);

            String yString = String.valueOf((int) level);
            canvas.drawText(yString, 0f, y - textPaint.descent(), textPaint);
        }

        canvas.drawText("0", 0f, canvas.getHeight() - textPaint.descent(), textPaint);
    }

}
