package by.anegin.telegram_contests.core.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import by.anegin.telegram_contests.core.ui.objects.Graph;

import java.util.HashMap;
import java.util.Map;

public class ToggleAnimationHelper {

    public interface Callback {
        Graph getGraph(String id);

        void onGraphToggled();

        void onGraphUpdated();
    }

    private final Callback callback;
    private final long animationDuration;

    public ToggleAnimationHelper(Callback callback, long animationDuration) {
        this.callback = callback;
        this.animationDuration = animationDuration;
    }

    private final Map<String, ValueAnimator> hideAnimators = new HashMap<>();
    private final Map<String, ValueAnimator> showAnimators = new HashMap<>();

    public void hideGraph(String id) {
        Graph graph = callback.getGraph(id);
        if (graph == null) return;

        if (graph.state != Graph.STATE_HIDING) {
            graph.state = Graph.STATE_HIDING;
            callback.onGraphToggled();
        }

        // cancel show animation if exists
        ValueAnimator showAnimation = showAnimators.remove(id);
        if (showAnimation != null && showAnimation.isRunning()) {
            showAnimation.cancel();
        }

        // add hide animation if not exists
        if (!hideAnimators.containsKey(id)) {
            ValueAnimator hideAnimation = makeToggleAnimation(graph, 0f,
                    Graph.STATE_HIDING, Graph.STATE_HIDDEN,
                    new DecelerateInterpolator(),
                    () -> hideAnimators.remove(id));
            hideAnimators.put(id, hideAnimation);
            hideAnimation.start();
        }
    }

    public void showGraph(String id) {
        Graph graph = callback.getGraph(id);
        if (graph == null) return;

        if (graph.state != Graph.STATE_SHOWING) {
            graph.state = Graph.STATE_SHOWING;
            callback.onGraphToggled();
        }

        // cancel hide animation if exists
        ValueAnimator hideAnimation = hideAnimators.remove(id);
        if (hideAnimation != null && hideAnimation.isRunning()) {
            hideAnimation.cancel();
        }

        // add show animation if not exists
        if (!showAnimators.containsKey(id)) {
            ValueAnimator showAnimation = makeToggleAnimation(graph, 1f,
                    Graph.STATE_SHOWING, Graph.STATE_VISIBLE,
                    new AccelerateInterpolator(),
                    () -> showAnimators.remove(id));
            showAnimators.put(id, showAnimation);
            showAnimation.start();
        }
    }

    private ValueAnimator makeToggleAnimation(Graph graph, float endValue, int progressState, int finalState, Interpolator interpolator, Runnable onEndAction) {
        float startValue = graph.alpha;
        long duration = (long) (animationDuration * Math.abs(endValue - startValue));

        ValueAnimator anim = ValueAnimator.ofFloat(startValue, endValue);
        anim.setInterpolator(interpolator);
        anim.setDuration(duration);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onEndAction.run();
                if (graph.state == progressState) {
                    graph.state = finalState;
                    graph.alpha = endValue;
                    callback.onGraphUpdated();
                }
            }
        });
        anim.addUpdateListener(animation -> {
            if (graph.state == progressState) {
                graph.alpha = (float) animation.getAnimatedValue();
                callback.onGraphUpdated();
            } else {
                animation.cancel();
            }
        });
        return anim;
    }

}
