package by.anegin.telegram_contests.core.ui.view;

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
import android.util.TypedValue;
import android.view.*;
import android.widget.OverScroller;
import by.anegin.telegram_contests.R;
import by.anegin.telegram_contests.core.ui.model.Graph;
import by.anegin.telegram_contests.core.ui.model.UiChart;
import by.anegin.telegram_contests.core.utils.AtomicRange;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

public class ChartView extends View {

    private static final int TOUCH_STATE_IDLE = 1;
    private static final int TOUCH_STATE_DRAG = 2;
    private static final int TOUCH_STATE_FLING = 3;

    public interface OnUiChartChangeListener {
        void onUiChartChanged(UiChart uiChart);
    }

    public interface OnRangeChangeListener {
        void onRangeChangeListener(float start, float end);
    }

    public interface OnGraphVisibilityChangeListener {
        void onGraphVisibilityChanged(Set<String> hiddenGraphIds);
    }

    private OnUiChartChangeListener onUiChartChangeListener;

    private OnRangeChangeListener onRangeChangeListener;

    private OnGraphVisibilityChangeListener onGraphVisibilityChangeListener;

    private final Paint guidelinePaint = new Paint();
    private final Paint graphPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final Matrix graphMatrix = new Matrix();

    private final AtomicRange range = new AtomicRange();

    private float xOffs = 0f;
    private float xScale = 1f;
    private volatile float yScale = 1f;

    private UiChart uiChart;

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private VelocityTracker velocityTracker;
    private OverScroller flingScroller;

    private int touchState = TOUCH_STATE_IDLE;
    private float downX;
    private float lastTouchX;

    private final Set<String> hiddenGraphIds = new HashSet<>();

    private ScaleThread scaleThread;

    private final ExecutorService yScaleCalculateExecutor = Executors.newFixedThreadPool(2);
    private Future<?> yScaleCalculationTask;
    private final AtomicLong lastCalculateGeneration = new AtomicLong(0);

    public ChartView(Context context) {
        super(context);
        init(context, null, 0);
    }

    public ChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public ChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        setSaveEnabled(true);

        float defaultGraphLineWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1f, context.getResources().getDisplayMetrics());

        TypedArray viewAttrs = context.obtainStyledAttributes(attrs, R.styleable.ChartView, defStyleAttr, 0);
        int guidelineColor = viewAttrs.getColor(R.styleable.ChartView_guide_line_color, Color.TRANSPARENT);
        float guidelineWidth = viewAttrs.getDimension(R.styleable.ChartView_guide_line_width, 0f);
        float graphLineWidth = viewAttrs.getDimension(R.styleable.ChartView_graph_line_width, defaultGraphLineWidth);
        viewAttrs.recycle();

        guidelinePaint.setStyle(Paint.Style.STROKE);
        guidelinePaint.setColor(guidelineColor);
        guidelinePaint.setStrokeWidth(guidelineWidth);

        graphPathPaint.setStyle(Paint.Style.STROKE);
        graphPathPaint.setStrokeWidth(graphLineWidth);

        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        flingScroller = new OverScroller(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        scaleThread = new ScaleThread(yScale);
        scaleThread.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (scaleThread != null) {
            scaleThread.cancel();
            scaleThread = null;
        }
        super.onDetachedFromWindow();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        calculateScaleAndOffset(false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), guidelinePaint);

        UiChart uiChart = this.uiChart;
        if (uiChart != null) {
            for (Graph graph : uiChart.graphs) {
                if (!hiddenGraphIds.contains(graph.id)) {
                    Graph transformedGraph = graph.transform(graphMatrix);
                    transformedGraph.draw(canvas, graphPathPaint);
                }
            }
        }
    }

    public void setRange(float start, float end, boolean animateYScale) {
        touchState = TOUCH_STATE_IDLE;

        flingScroller.forceFinished(true);

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }

        updateRanges(start, end, animateYScale);
        invalidateOnAnimation();
    }

    public void setUiChart(UiChart uiChart) {
        this.uiChart = uiChart;
        calculateScaleAndOffset(false);
        if (onUiChartChangeListener != null) {
            onUiChartChangeListener.onUiChartChanged(uiChart);
        }
        invalidate();
    }

    public UiChart getUiChart() {
        return uiChart;
    }

    public void setOnUiChartChangeListener(OnUiChartChangeListener listener) {
        this.onUiChartChangeListener = listener;
    }

    public void setOnRangeChangeListener(OnRangeChangeListener listener) {
        this.onRangeChangeListener = listener;
    }

    public void setOnGraphVisibilityChangeListener(OnGraphVisibilityChangeListener listener) {
        this.onGraphVisibilityChangeListener = listener;
    }

    public void hideGraph(String id) {
        if (hiddenGraphIds.add(id)) {
            if (onGraphVisibilityChangeListener != null) {
                onGraphVisibilityChangeListener.onGraphVisibilityChanged(hiddenGraphIds);
            }
            invalidate();
        }
    }

    public void showGraph(String id) {
        if (hiddenGraphIds.remove(id)) {
            if (onGraphVisibilityChangeListener != null) {
                onGraphVisibilityChangeListener.onGraphVisibilityChanged(hiddenGraphIds);
            }
            invalidate();
        }
    }

    public boolean isGraphVisible(String id) {
        return !hiddenGraphIds.contains(id);
    }

    public void showAllGraphs() {
        if (hiddenGraphIds.size() > 0) {
            hiddenGraphIds.clear();
            if (onGraphVisibilityChangeListener != null) {
                onGraphVisibilityChangeListener.onGraphVisibilityChanged(hiddenGraphIds);
            }
            invalidate();
        }
    }

    // =======

    private void updateRanges(float start, float end, boolean animateYScale) {
        if (start < 0f) {
            end -= start;
            start = 0f;
        }
        if (end > 1f) {
            start -= (end - 1f);
            end = 1f;
        }
        if (range.checkNotEqualsAndSet(start, end)) {
            if (onRangeChangeListener != null) {
                onRangeChangeListener.onRangeChangeListener(start, end);
            }
            calculateScaleAndOffset(animateYScale);
        }
    }

    private void calculateScaleAndOffset(boolean animateYScale) {
        UiChart uiChart = this.uiChart;
        if (uiChart == null) return;

        float visibleChartWidth = uiChart.width * range.getSize();
        xScale = getWidth() / visibleChartWidth;
        xOffs = -xScale * range.getStart() * uiChart.width;

        updateGraphMatrix(yScale);

        long calculateGeneration = lastCalculateGeneration.incrementAndGet();
        float rangeStart = range.getStart();
        float rangeEnd = range.getEnd();
        if (yScaleCalculationTask != null) {
            yScaleCalculationTask.cancel(true);
            yScaleCalculationTask = null;
        }
        yScaleCalculationTask = yScaleCalculateExecutor.submit(() -> {
            float startX = uiChart.width * rangeStart;
            float endX = uiChart.width * rangeEnd;

            float maxY = 0f;
            for (Graph graph : uiChart.graphs) {
                float max = graph.findMaxYInRange(startX, endX);
                if (max > maxY) maxY = max;
            }

            float calculatedYScale = (float) getHeight() / maxY;
            if (lastCalculateGeneration.get() == calculateGeneration) {
                scaleThread.setYScale(calculatedYScale, animateYScale);
            }
        });
    }

    private void updateGraphMatrix(float scale) {
        yScale = scale;
        graphMatrix.reset();
        graphMatrix.setTranslate(xOffs, getHeight());
        graphMatrix.preScale(xScale, -scale);
    }

    private void invalidateOnAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }

    // =======

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = event.getX();
                lastTouchX = downX;
                touchState = TOUCH_STATE_IDLE;

                flingScroller.forceFinished(true);

                if (velocityTracker == null) {
                    velocityTracker = VelocityTracker.obtain();
                } else {
                    velocityTracker.clear();
                }
                velocityTracker.addMovement(event);

                break;
            }
            case MotionEvent.ACTION_MOVE: {
                float touchX = event.getX();
                if (touchState == TOUCH_STATE_IDLE && Math.abs(touchX - downX) > touchSlop) {
                    touchState = TOUCH_STATE_DRAG;

                    ViewParent viewParent = getParent();
                    if (viewParent != null) {
                        viewParent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (touchState == TOUCH_STATE_DRAG) {
                    moveChart(touchX - lastTouchX);
                }
                lastTouchX = touchX;

                if (velocityTracker != null) {
                    velocityTracker.addMovement(event);
                }
                break;
            }
            case MotionEvent.ACTION_UP: {

                VelocityTracker velocityTracker = this.velocityTracker;
                this.velocityTracker = null;
                if (touchState == TOUCH_STATE_DRAG) {
                    if (velocityTracker != null) {
                        velocityTracker.computeCurrentVelocity(1000, maxFlingVelocity);
                        float xVelocity = velocityTracker.getXVelocity();
                        velocityTracker.recycle();

                        if (Math.abs(xVelocity) > minFlingVelocity) {
                            touchState = TOUCH_STATE_FLING;
                            flingChart(xVelocity);
                            break;
                        }
                    }
                } else {
                    if (velocityTracker != null) {
                        velocityTracker.recycle();
                    }
                }

                touchState = TOUCH_STATE_IDLE;
                break;
            }
            case MotionEvent.ACTION_CANCEL: {
                touchState = TOUCH_STATE_IDLE;
                if (velocityTracker != null) {
                    velocityTracker.recycle();
                    velocityTracker = null;
                }
            }
        }
        return true;
    }

    private void moveChart(float dx) {
        UiChart uiChart = this.uiChart;
        if (uiChart == null) return;

        float unscaledOffset = dx / xScale;
        float rangeOffs = unscaledOffset / uiChart.width;

        float newRangeStart = range.getStart() - rangeOffs;
        float newRangeEnd = range.getEnd() - rangeOffs;

        updateRanges(newRangeStart, newRangeEnd, true);
        invalidateOnAnimation();
    }

    private void flingChart(float xVelocity) {
        UiChart uiChart = this.uiChart;
        if (uiChart == null) return;

        int startX = (int) (xScale * range.getStart() * uiChart.width);
        int minX = 0;
        int maxX = (int) (xScale * uiChart.width) - getWidth();

        flingScroller.forceFinished(true);
        flingScroller.fling(startX, 0, (int) -xVelocity, 0, minX, maxX, 0, 0);

        invalidateOnAnimation();
    }

    @Override
    public void computeScroll() {
        if (touchState != TOUCH_STATE_FLING) return;
        if (flingScroller.computeScrollOffset()) {
            UiChart uiChart = this.uiChart;
            if (uiChart == null) return;

            int currOffs = flingScroller.getCurrX();
            float newRangeStart = (float) currOffs / (xScale * uiChart.width);
            float newRangeEnd = newRangeStart + range.getSize();

            updateRanges(newRangeStart, newRangeEnd, true);
            invalidateOnAnimation();
        } else {
            touchState = TOUCH_STATE_IDLE;
            invalidateOnAnimation();
        }
    }

    // =======

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.hiddenGraphIds = new ArrayList<>(hiddenGraphIds);
        savedState.rangeStart = range.getStart();
        savedState.rangeEnd = range.getEnd();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        hiddenGraphIds.clear();
        hiddenGraphIds.addAll(savedState.hiddenGraphIds);
        updateRanges(savedState.rangeStart, savedState.rangeEnd, false);
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

    // =======

    private class ScaleThread extends Thread {

        private final long interval = 10;

        private volatile boolean isRunning = true;

        private volatile float targetYScale;

        private ScaleThread(float yScale) {
            this.targetYScale = yScale;
        }

        private void cancel() {
            isRunning = false;
        }

        private void setYScale(float scale, boolean withAnimation) {
            targetYScale = scale;
            if (!withAnimation) {
                updateGraphMatrix(scale);
                invalidateOnAnimation();
            }
        }

        @Override
        public void run() {
            while (isRunning) {
                float yScale = ChartView.this.yScale;
                float targetScale = this.targetYScale;
                if (targetScale != yScale) {
                    float diff = targetScale - yScale;
                    if (Math.abs(diff) < 0.00001f) {
                        updateGraphMatrix(targetScale);
                    } else {
                        float offs = diff * interval / 100f;
                        updateGraphMatrix(yScale + offs);
                    }
                    invalidateOnAnimation();
                }
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

}
