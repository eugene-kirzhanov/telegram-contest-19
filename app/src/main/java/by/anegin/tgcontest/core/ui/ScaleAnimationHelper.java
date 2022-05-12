package by.anegin.tgcontest.core.ui;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class ScaleAnimationHelper {

    public static class CalcResult {
        public final float targetScale;
        public final float maxY;

        public CalcResult(float targetScale, float maxY) {
            this.targetScale = targetScale;
            this.maxY = maxY;
        }
    }

    public interface Callback {
        CalcResult calculateNewScale();

        float getCurrentScale();

        void onScaleUpdated(float scale, CalcResult calcResult);
    }

    private final Callback callback;
    private final long animateDuration;

    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final ExecutorService scaleCalculateExecutor = Executors.newFixedThreadPool(2);
    private final AtomicLong lastCalculateGeneration = new AtomicLong(0);
    private Future<?> scaleCalculationTask;

    private ValueAnimator scaleAnimator;
    private float scaleAnimTo;

    public ScaleAnimationHelper(Callback callback, long animateDuration) {
        this.callback = callback;
        this.animateDuration = animateDuration;
    }

    public void calculate(boolean animateYScale) {
        long calculateGeneration = lastCalculateGeneration.incrementAndGet();

        if (scaleCalculationTask != null) {
            scaleCalculationTask.cancel(true);
            scaleCalculationTask = null;
        }

        scaleCalculationTask = scaleCalculateExecutor.submit(() -> {
            CalcResult calcResult = callback.calculateNewScale();
            if (lastCalculateGeneration.get() == calculateGeneration) {
                uiHandler.post(() -> {
                    if (animateYScale) {
                        animateYScale(calcResult);
                    } else {
                        callback.onScaleUpdated(calcResult.targetScale, calcResult);
                    }
                });
            }
        });
    }

    private void animateYScale(CalcResult calcResult) {
        float fromScale = callback.getCurrentScale();
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            if (scaleAnimTo != calcResult.targetScale) {
                scaleAnimator.cancel();
                scaleAnimator = null;
            } else {
                return;
            }
        }
        scaleAnimTo = calcResult.targetScale;

        scaleAnimator = ValueAnimator.ofFloat(fromScale, calcResult.targetScale);
        scaleAnimator.setInterpolator(new LinearInterpolator());
        scaleAnimator.setDuration(animateDuration);
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            callback.onScaleUpdated(scale, calcResult);
        });
        scaleAnimator.start();
    }

}
