package by.anegin.telegram_contests.core.ui;

import android.animation.ValueAnimator;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.LinearInterpolator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class ScaleAnimationHelper {

    public interface Callback {
        float calculateNewScale();

        float getCurrentScale();

        void onScaleUpdated(float scale);
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
            float calculatedScale = callback.calculateNewScale();
            if (lastCalculateGeneration.get() == calculateGeneration) {
                uiHandler.post(() -> {
                    if (animateYScale) {
                        animateYScale(callback.getCurrentScale(), calculatedScale);
                    } else {
                        callback.onScaleUpdated(calculatedScale);
                    }
                });
            }
        });
    }

    private void animateYScale(float from, float to) {
        if (scaleAnimator != null && scaleAnimator.isRunning()) {
            if (scaleAnimTo != to) {
                scaleAnimator.cancel();
                scaleAnimator = null;
            } else {
                return;
            }
        }
        scaleAnimTo = to;

        scaleAnimator = ValueAnimator.ofFloat(from, to);
        scaleAnimator.setInterpolator(new LinearInterpolator());
        scaleAnimator.setDuration(animateDuration);
        scaleAnimator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            callback.onScaleUpdated(scale);
        });
        scaleAnimator.start();
    }

}
