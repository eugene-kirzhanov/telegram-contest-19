package by.anegin.telegram_contests.core.ui.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import by.anegin.telegram_contests.R;
import by.anegin.telegram_contests.core.ui.model.Graph;
import by.anegin.telegram_contests.core.ui.model.UiChart;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MiniChartView extends View {

    private static final int DRAG_RANGE = 1;
    private static final int DRAG_RANGE_START = 2;
    private static final int DRAG_RANGE_END = 3;
    private static final int DRAG_NONE = 4;

    interface OnRangeChangeListener {
        void onRangeChanged(float start, float end);
    }

    private final Paint windowPaint = new Paint();
    private final Paint touchRipplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fadePaint = new Paint();

    private float windowStrokeWidthTopBottom;
    private float windowStrokeWidthLeftRight;

    private float chartLineWidth;

    private float rangeStart = 0f;
    private float rangeEnd = 1f;

    private float downX = 0f;
    private float lastTouchX = 0f;
    private boolean inDragMode = false;

    private int dragMode = DRAG_NONE;

    private int touchSlop;
    private float dragSize;

    private Animator touchRippleAnimation;
    private float touchRippleRadius = 0f;
    private float touchRippleAlpha = 0f;

    private OnRangeChangeListener onRangeChangeListener;

    private final Executor updateDataExecutor = Executors.newFixedThreadPool(2);
    private long lastUpdateGeneration = 0L;

    private UiChart uiChart;
    private List<Graph> graphs;

    private final Set<String> hiddenGraphIds = new HashSet<>();

    public MiniChartView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public MiniChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public MiniChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setSaveEnabled(true);

        TypedArray viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.MiniChartView, defStyleAttr, 0);
        rangeStart = viewAttrs.getFloat(R.styleable.MiniChartView_range_start, 0f);
        rangeEnd = viewAttrs.getFloat(R.styleable.MiniChartView_range_end, 1f);
        int fadeColor = viewAttrs.getColor(R.styleable.MiniChartView_fade_color, Color.TRANSPARENT);
        int windowColor = viewAttrs.getColor(R.styleable.MiniChartView_window_color, Color.TRANSPARENT);
        windowStrokeWidthTopBottom =
                viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_top_bottom, 0f);
        windowStrokeWidthLeftRight =
                viewAttrs.getDimension(R.styleable.MiniChartView_window_stroke_width_left_right, 0f);
        chartLineWidth = viewAttrs.getDimension(R.styleable.MiniChartView_chart_line_width, 1f);
        viewAttrs.recycle();

        if (rangeStart < 0f) rangeStart = 0f;
        if (rangeEnd > 1f) rangeEnd = 1f;

        windowPaint.setStyle(Paint.Style.FILL);
        windowPaint.setColor(windowColor);

        touchRipplePaint.setStyle(Paint.Style.FILL);
        touchRipplePaint.setColor(windowColor);

        fadePaint.setStyle(Paint.Style.FILL);
        fadePaint.setColor(fadeColor);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        dragSize = 1.5f * touchSlop;
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        float rangeStartX = width * rangeStart;
        float rangeEndX = width * rangeEnd;

        // draw window
        float x1 = rangeStartX + windowStrokeWidthLeftRight / 2f;
        float x2 = rangeEndX - windowStrokeWidthLeftRight / 2f;
        windowPaint.setStrokeWidth(windowStrokeWidthLeftRight);
        canvas.drawLine(x1, 0f, x1, height, windowPaint);
        canvas.drawLine(x2, 0f, x2, height, windowPaint);
        float x3 = rangeStartX + windowStrokeWidthLeftRight;
        float x4 = rangeEndX - windowStrokeWidthLeftRight;
        float y1 = windowStrokeWidthTopBottom / 2f;
        float y2 = height - windowStrokeWidthTopBottom / 2f;
        windowPaint.setStrokeWidth(windowStrokeWidthTopBottom);
        canvas.drawLine(x3, y1, x4, y1, windowPaint);
        canvas.drawLine(x3, y2, x4, y2, windowPaint);

        // draw Chart data
        List<Graph> graphs = this.graphs;
        if (graphs != null) {
            for (Graph graph : graphs) {
                if (graph.state == Graph.STATE_HIDDEN) continue;
                graph.draw(canvas);
            }
        }

        // fade out off-window regions
        if (rangeStartX > 0f) {
            canvas.drawRect(0f, 0f, rangeStartX, height, fadePaint);
        }
        if (rangeEndX < width) {
            canvas.drawRect(rangeEndX, 0f, width, height, fadePaint);
        }

        // draw touch ripple
        float touchRippleRadius = this.touchRippleRadius;
        if (touchRippleRadius > 0f) {
            Float cx = null;
            switch (dragMode) {
                case DRAG_RANGE_START: {
                    cx = rangeStartX + windowStrokeWidthLeftRight / 2f;
                    break;
                }
                case DRAG_RANGE_END: {
                    cx = rangeEndX - windowStrokeWidthLeftRight / 2f;
                    break;
                }
                case DRAG_RANGE: {
                    cx = rangeStartX + (rangeEndX - rangeStartX) / 2f;
                    break;
                }
            }
            if (cx != null) {
                touchRipplePaint.setAlpha((int) (102 * touchRippleAlpha));
                canvas.drawCircle(cx, height / 2f, touchRippleRadius, touchRipplePaint);
            }
        }
    }

    @Override
    @SuppressLint("ClickableViewAccessibility")
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                float rangeStartX = getWidth() * rangeStart;
                float rangeEndX = getWidth() * rangeEnd;

                downX = event.getX();
                lastTouchX = downX;
                inDragMode = false;

                // determine drag mode depending on touch region
                if (downX >= rangeStartX - dragSize && downX <= rangeStartX + dragSize + windowStrokeWidthLeftRight) {
                    dragMode = DRAG_RANGE_START;
                } else if (downX >= rangeEndX - dragSize - windowStrokeWidthLeftRight && downX <= rangeEndX + dragSize) {
                    dragMode = DRAG_RANGE_END;
                } else if (downX >= rangeStartX + dragSize + windowStrokeWidthLeftRight && downX <= rangeEndX - dragSize - windowStrokeWidthLeftRight) {
                    dragMode = DRAG_RANGE;
                } else {
                    dragMode = DRAG_NONE;
                }

                showTouchRippleAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                float touchX = event.getX();
                if (!inDragMode && Math.abs(touchX - downX) > touchSlop) {
                    inDragMode = true;
                }
                if (inDragMode && dragMode != DRAG_NONE) {
                    onMove(touchX, lastTouchX - touchX);
                }
                lastTouchX = touchX;
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                hideTouchRippleAnimation();
                break;
            }
        }
        return true;
    }

    private void onMove(float eventX, float distanceX) {
        int width = getWidth();
        float rangeStartX = width * rangeStart;
        float rangeEndX = width * rangeEnd;

        switch (dragMode) {
            case DRAG_RANGE_START: {
                // move left edge of range window (0..rangeEnd)
                float newRangeStart = eventX / width;
                if (newRangeStart < 0f) newRangeStart = 0f;

                float newRangeStartX = width * newRangeStart;
                if (newRangeStartX + dragSize + windowStrokeWidthLeftRight > rangeEndX - dragSize - windowStrokeWidthLeftRight) {
                    newRangeStartX = rangeEndX - 2f * (dragSize + windowStrokeWidthLeftRight);
                    newRangeStart = newRangeStartX / width;
                }
                updateRanges(newRangeStart, rangeEnd, true);
                break;
            }
            case DRAG_RANGE_END: {
                // move right edge of range window (rangeStart..1)
                float newRangeEnd = eventX / width;
                if (newRangeEnd > 1f) newRangeEnd = 1f;

                float newRangeEndX = width * newRangeEnd;
                if (newRangeEndX - dragSize - windowStrokeWidthLeftRight < rangeStartX + dragSize + windowStrokeWidthLeftRight) {
                    newRangeEndX = rangeStartX + 2f * (dragSize + windowStrokeWidthLeftRight);
                    newRangeEnd = newRangeEndX / width;
                }
                updateRanges(rangeStart, newRangeEnd, true);
                break;
            }
            case DRAG_RANGE: {
                // move whole window
                float newRangeStart = (rangeStartX - distanceX) / width;
                if (newRangeStart < 0f) newRangeStart = 0f;

                float newRangeEnd = newRangeStart + (rangeEnd - rangeStart);
                if (newRangeEnd > 1f) {
                    newRangeEnd = 1f;
                    newRangeStart = 1f - (rangeEnd - rangeStart);
                }
                updateRanges(newRangeStart, newRangeEnd, true);
                break;
            }
        }
    }

    private void updateRanges(float start, float end, boolean notifyListener) {
        if (rangeStart != start || rangeEnd != end) {
            rangeStart = start;
            rangeEnd = end;

            invalidate();

            if (notifyListener && onRangeChangeListener != null) {
                onRangeChangeListener.onRangeChanged(start, end);
            }
        }
    }

    private void showTouchRippleAnimation() {
        if (touchRippleAnimation != null) {
            touchRippleAnimation.cancel();
        }
        touchRippleAnimation = createTouchRippleAnimation(0f, 1f);
        touchRippleAnimation.start();
    }

    private void hideTouchRippleAnimation() {
        float currentValue = touchRippleAnimation != null ? (float) ((ValueAnimator) touchRippleAnimation).getAnimatedValue() : 1f;
        if (touchRippleAnimation != null) {
            touchRippleAnimation.cancel();
        }
        touchRippleAnimation = createTouchRippleAnimation(currentValue, 0f);
        touchRippleAnimation.start();
    }

    private Animator createTouchRippleAnimation(float from, float to) {
        ValueAnimator anim = ValueAnimator.ofFloat(from, to);
        anim.setDuration(200);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            touchRippleRadius = value * (0.7f * getHeight());
            touchRippleAlpha = value;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                postInvalidateOnAnimation();
            } else {
                postInvalidate();
            }
        });
        return anim;
    }

    // ================

    @Override
    public void onSizeChanged(int w, int h, int oldw, int oldh) {
        enqueueUpdateData();
    }

    public void attachToChartView(ChartView chartView) {
        chartView.setOnUiChartChangeListener(uiChart -> {
            this.uiChart = uiChart;
            enqueueUpdateData();
        });

        chartView.setOnRangeChangeListener((start, end) -> updateRanges(start, end, false));

        chartView.setOnGraphVisibilityChangeListener(new ChartView.OnGraphVisibilityChangeListener() {
            @Override
            public void onGraphHidden(String id) {
                hideGraph(id);
            }

            @Override
            public void onGraphShown(String id) {
                showGraph(id);
            }

            @Override
            public void onReset() {
                showAllGraphs();
            }
        });

        onRangeChangeListener = (start, end) -> chartView.setRange(start, end, true);

        chartView.setRange(rangeStart, rangeEnd, false);
    }

    private void enqueueUpdateData() {
        UiChart uiChart = this.uiChart;
        int viewWidth = getWidth();
        int viewHeight = getHeight();
        if (uiChart == null || viewWidth == 0 || viewHeight == 0) return;
        long updateGeneration = ++lastUpdateGeneration;
        updateDataExecutor.execute(() -> {
            final List<Graph> newGraphs = updateData(uiChart, viewWidth, viewHeight);
            if (lastUpdateGeneration == updateGeneration) {
                post(() -> {
                    this.graphs = newGraphs;
                    invalidate();
                });
            }
        });
    }

    private List<Graph> updateData(UiChart uiChart, int viewWidth, int viewHeight) {
        if (uiChart == null) return null;

        float scaleX = 1f * viewWidth / uiChart.width;
        float scaleY = 0.85f * viewHeight / uiChart.height;

        float scaledChartWidth = uiChart.width * scaleX;
        float scaledChartHeight = uiChart.height * scaleY;

        float xOffs = (viewWidth - scaledChartWidth) / 2f;
        float yOffs = viewHeight - (viewHeight - scaledChartHeight) / 2f;

        Matrix matrix = new Matrix();
        matrix.setTranslate(xOffs, yOffs);
        matrix.preScale(scaleX, -scaleY);

        List<Graph> graphs = new ArrayList<>();
        for (Graph g : uiChart.graphs) {
            Graph graph = new Graph(g, chartLineWidth);
            graph.transformPoints(matrix);
            graphs.add(graph);
        }
        return graphs;
    }

    // ===============

    private final Map<String, ValueAnimator> hideAnimators = new HashMap<>();
    private final Map<String, ValueAnimator> showAnimators = new HashMap<>();

    private long animduration = 500;

    private void hideGraph(String id) {
        if (hiddenGraphIds.add(id)) invalidate();

        // find Graph by id
        Graph graph = null;
        for (Graph g : this.graphs) {
            if (g.id.equals(id)) {
                graph = g;
                break;
            }
        }
        if (graph == null) return;

        graph.state = Graph.STATE_HIDING;

        // cancel show animation if exists
        ValueAnimator showAnimation = showAnimators.remove(id);
        if (showAnimation != null && showAnimation.isRunning()) {
            showAnimation.cancel();
        }

        // add hide animation if not exists
        if (!hideAnimators.containsKey(id)) {

            ValueAnimator hideAnimation = ValueAnimator.ofFloat(graph.animationValue, 0f);
            hideAnimators.put(id, hideAnimation);

            hideAnimation.setInterpolator(new AccelerateInterpolator());
            hideAnimation.setDuration(animduration);

            final Graph graphFinal = graph;

            hideAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    hideAnimators.remove(id);
                    if (graphFinal.state == Graph.STATE_HIDING) {
                        graphFinal.state = Graph.STATE_HIDDEN;
                        graphFinal.animationValue = 0f;
                        invalidate();
                    }
                }
            });

            hideAnimation.addUpdateListener(animation -> {
                if (graphFinal.state == Graph.STATE_HIDING) {
                    graphFinal.animationValue = (float) animation.getAnimatedValue();
                    invalidate();
                } else {
                    animation.cancel();
                }
            });

            hideAnimation.start();
        }
    }

    private void showGraph(String id) {
        if (hiddenGraphIds.remove(id)) invalidate();

        // find Graph by id
        Graph graph = null;
        for (Graph g : this.graphs) {
            if (g.id.equals(id)) {
                graph = g;
                break;
            }
        }
        if (graph == null) return;

        graph.state = Graph.STATE_SHOWING;

        // cancel hide animation if exists
        ValueAnimator hideAnimation = hideAnimators.remove(id);
        if (hideAnimation != null && hideAnimation.isRunning()) {
            hideAnimation.cancel();
        }

        // add hide animation if not exists
        if (!showAnimators.containsKey(id)) {

            ValueAnimator showAnimation = ValueAnimator.ofFloat(graph.animationValue, 1f);
            showAnimators.put(id, showAnimation);

            showAnimation.setInterpolator(new DecelerateInterpolator());
            showAnimation.setDuration(animduration);

            final Graph graphFinal = graph;

            showAnimation.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    hideAnimators.remove(id);
                    if (graphFinal.state == Graph.STATE_SHOWING) {
                        graphFinal.state = Graph.STATE_VISIBLE;
                        graphFinal.animationValue = 1f;
                        invalidate();
                    }
                }
            });

            showAnimation.addUpdateListener(animation -> {
                if (graphFinal.state == Graph.STATE_SHOWING) {
                    graphFinal.animationValue = (float) animation.getAnimatedValue();
                    invalidate();
                } else {
                    animation.cancel();
                }
            });

            showAnimation.start();
        }
    }

    private void showAllGraphs() {
        hiddenGraphIds.clear();

        List<Graph> graphs = this.graphs;
        for (Graph graph : graphs) {
            graph.state = Graph.STATE_VISIBLE;
            graph.animationValue = 1f;
        }

        for (ValueAnimator anim : hideAnimators.values()) {
            if (anim.isRunning()) anim.cancel();
        }
        hideAnimators.clear();
        for (ValueAnimator anim : showAnimators.values()) {
            if (anim.isRunning()) anim.cancel();
        }
        showAnimators.clear();

        invalidate();
    }

    // ===============

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.hiddenGraphIds = new ArrayList<>(hiddenGraphIds);
        savedState.rangeStart = rangeStart;
        savedState.rangeEnd = rangeEnd;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        hiddenGraphIds.clear();
        hiddenGraphIds.addAll(savedState.hiddenGraphIds);
        rangeStart = savedState.rangeStart;
        rangeEnd = savedState.rangeEnd;
    }

    private static class SavedState extends BaseSavedState {

        List<String> hiddenGraphIds;
        float rangeStart;
        float rangeEnd;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            hiddenGraphIds = in.createStringArrayList();
            rangeStart = in.readFloat();
            rangeEnd = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeStringList(hiddenGraphIds);
            out.writeFloat(rangeStart);
            out.writeFloat(rangeEnd);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

}