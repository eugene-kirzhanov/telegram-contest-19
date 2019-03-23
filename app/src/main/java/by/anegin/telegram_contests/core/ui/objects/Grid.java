package by.anegin.telegram_contests.core.ui.objects;

import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.TextPaint;
import android.view.animation.AccelerateInterpolator;

import java.text.DecimalFormat;

import by.anegin.telegram_contests.core.ui.view.ChartView;

public class Grid {

    public interface OnFadedOutListener {
        void onFadedOut(Grid grid);
    }

    private static final float GAPS_COUNT = 5.55f;
    private static final int LINES_COUNT = (int) Math.floor(GAPS_COUNT);

    private final DecimalFormat decimalFormat = new DecimalFormat();

    private final Paint linePaint = new Paint();
    private final TextPaint textPaint = new TextPaint();

    private final float fromYScale;
    public final float targetYScale;

    private final float[] levels = new float[LINES_COUNT];
    private final String[] titles = new String[LINES_COUNT];

    private float alpha;

    private boolean hiding = false;

    public Grid(float fromYScale, float targetYScale, float maxY, int lineColor, float lineWidth, int textColor, float textSize) {
        this.fromYScale = fromYScale;
        this.targetYScale = targetYScale;

        float gapHeight = maxY / GAPS_COUNT;

        float y = gapHeight;
        for (int i = 0; i < LINES_COUNT; i++) {
            levels[i] = y;
            titles[i] = makeLevelTitle(y);
            y += gapHeight;
        }

        linePaint.setColor(lineColor);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(lineWidth);

        textPaint.setColor(textColor);
        textPaint.setTextSize(textSize);

        decimalFormat.setMinimumIntegerDigits(0);
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

        for (int i = 0; i < LINES_COUNT; i++) {
            float y = canvas.getHeight() - levels[i] * yScale;
            canvas.drawLine(0f, y, canvas.getWidth(), y, linePaint);
            canvas.drawText(titles[i], 0f, y - textPaint.descent(), textPaint);
        }

        canvas.drawText("0", 0f, canvas.getHeight() - textPaint.descent(), textPaint);
    }

    private String makeLevelTitle(float level) {
        float value;
        int maxFractionDigits;
        String suffix;
        if (level < 1_000) {
            value = (int) level;
            maxFractionDigits = 0;
            suffix = "";
        } else if (level < 10_000) {
            value = level / 1000;
            maxFractionDigits = 1;
            suffix = "k";
        } else if (level < 1_000_000) {
            value = level / 1000;
            maxFractionDigits = 0;
            suffix = "k";
        } else if (level < 10_000_000) {
            value = level / 1000000;
            maxFractionDigits = 1;
            suffix = "M";
        } else {
            value = level / 1000000;
            maxFractionDigits = 0;
            suffix = "M";
        }
        decimalFormat.setMaximumFractionDigits(maxFractionDigits);
        return decimalFormat.format(value) + suffix;
    }

}
