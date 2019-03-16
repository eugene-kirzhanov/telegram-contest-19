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
import by.anegin.telegram_contests.core.ui.ScaleAnimationHelper;
import by.anegin.telegram_contests.core.ui.ToggleAnimationHelper;
import by.anegin.telegram_contests.core.ui.model.Graph;
import by.anegin.telegram_contests.core.ui.model.UiChart;
import by.anegin.telegram_contests.core.utils.AtomicRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChartView extends View implements ScaleAnimationHelper.Callback, ToggleAnimationHelper.Callback {

    private static final int TOUCH_STATE_IDLE = 1;
    private static final int TOUCH_STATE_DRAG = 2;
    private static final int TOUCH_STATE_FLING = 3;

    private static final int AUTOSCALE_ANIMATION_DURATION = 250;
    private static final int TOGGLE_ANIMATION_DURATION = 200;


    public interface OnUiChartChangeListener {
        void onUiChartChanged(UiChart uiChart);
    }

    public interface OnRangeChangeListener {
        void onRangeChangeListener(float start, float end);
    }

    public interface OnGraphVisibilityChangeListener {
        void onGraphHidden(String id);

        void onGraphShown(String id);

        void onHiddenGraphChanged(Set<String> ids);
    }

    private OnUiChartChangeListener onUiChartChangeListener;

    private OnRangeChangeListener onRangeChangeListener;

    private OnGraphVisibilityChangeListener onGraphVisibilityChangeListener;

    private final Paint guidelinePaint = new Paint();

    private final Matrix graphMatrix = new Matrix();

    private float graphStrokeWidth = 0f;

    private final AtomicRange range = new AtomicRange();

    private float xOffs = 0f;
    private float xScale = 1f;
    private volatile float yScale = 0f;

    private final List<Graph> graphs = new ArrayList<>();
    private float uiChartWidth;

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private VelocityTracker velocityTracker;
    private OverScroller flingScroller;

    private int touchState = TOUCH_STATE_IDLE;
    private float downX;
    private float lastTouchX;

    private final ScaleAnimationHelper scaleAnimationHelper = new ScaleAnimationHelper(this, AUTOSCALE_ANIMATION_DURATION);

    private final ToggleAnimationHelper toggleAnimationHelper = new ToggleAnimationHelper(this, TOGGLE_ANIMATION_DURATION);

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
        graphStrokeWidth = viewAttrs.getDimension(R.styleable.ChartView_graph_line_width, defaultGraphLineWidth);
        viewAttrs.recycle();

        guidelinePaint.setStyle(Paint.Style.STROKE);
        guidelinePaint.setColor(guidelineColor);
        guidelinePaint.setStrokeWidth(guidelineWidth);

        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        flingScroller = new OverScroller(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        scaleAnimationHelper.calculate(false);
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), guidelinePaint);

        if (yScale == 0f) return;

        synchronized (graphs) {
            for (Graph graph : graphs) {
                graph.draw(canvas);
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

    public void setUiChart(UiChart uiChart, Set<String> hiddenGraphsIds) {
        synchronized (graphs) {
            graphs.clear();
            if (uiChart != null) {
                uiChartWidth = uiChart.width;
                for (Graph g : uiChart.graphs) {
                    Graph graph = new Graph(g, graphStrokeWidth);
                    if (hiddenGraphsIds.contains(graph.id)) {
                        graph.state = Graph.STATE_HIDDEN;
                        graph.alpha = 0f;
                    } else {
                        graph.state = Graph.STATE_VISIBLE;
                        graph.alpha = 1f;
                    }
                    graphs.add(graph);
                }
            } else {
                uiChartWidth = 0f;
                yScale = 0f;
            }
        }

        scaleAnimationHelper.calculate(false);

        if (onUiChartChangeListener != null) {
            onUiChartChangeListener.onUiChartChanged(uiChart);
        }

        if (onGraphVisibilityChangeListener != null) {
            onGraphVisibilityChangeListener.onHiddenGraphChanged(hiddenGraphsIds);
        }
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
        if (onGraphVisibilityChangeListener != null) {
            onGraphVisibilityChangeListener.onGraphHidden(id);
        }
        toggleAnimationHelper.hideGraph(id);
    }

    public void showGraph(String id) {
        if (onGraphVisibilityChangeListener != null) {
            onGraphVisibilityChangeListener.onGraphShown(id);
        }
        toggleAnimationHelper.showGraph(id);
    }

    public int getGraphsCount() {
        return graphs.size();
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
            scaleAnimationHelper.calculate(animateYScale);
        }
    }

    @Override
    public float calculateNewScale() {
        float uiChartWidth;
        List<Graph> graphs;
        synchronized (this.graphs) {
            uiChartWidth = this.uiChartWidth;
            graphs = new ArrayList<>(this.graphs);
        }
        if (uiChartWidth == 0f || graphs.isEmpty()) return yScale;

        xScale = getWidth() / (uiChartWidth * range.getSize());
        xOffs = xScale * range.getStart() * uiChartWidth;

        float startX = uiChartWidth * range.getStart();
        float endX = uiChartWidth * range.getEnd();

        float maxY = 0f;
        for (Graph graph : graphs) {
            float max = graph.findMaxYInRange(startX, endX);
            if (max > maxY) maxY = max;
        }

        return (float) getHeight() / maxY;
    }

    @Override
    public float getCurrentScale() {
        return yScale;
    }

    @Override
    public void onScaleUpdated(float scale) {
        yScale = scale;
        updateGraphMatrix();
        invalidateOnAnimation();
    }

    private void updateGraphMatrix() {
        graphMatrix.reset();
        graphMatrix.setTranslate(-xOffs, getHeight());
        graphMatrix.preScale(xScale, -yScale);

        synchronized (graphs) {
            for (Graph graph : graphs) {
                graph.transform(graphMatrix);
            }
        }
    }

    private void invalidateOnAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }

    // =======

    @Override
    public Graph getGraph(String id) {
        synchronized (graphs) {
            for (Graph g : this.graphs)
                if (g.id.equals(id)) return g;
        }
        return null;
    }

    @Override
    public void onGraphToggled() {
        scaleAnimationHelper.calculate(true);
    }

    @Override
    public void onGraphUpdated() {
        invalidateOnAnimation();
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
        float uiChartWidth = this.uiChartWidth;
        if (uiChartWidth == 0f) return;

        float unscaledOffset = dx / xScale;
        float rangeOffs = unscaledOffset / uiChartWidth;

        float newRangeStart = range.getStart() - rangeOffs;
        float newRangeEnd = range.getEnd() - rangeOffs;

        updateRanges(newRangeStart, newRangeEnd, true);
        invalidateOnAnimation();
    }

    private void flingChart(float xVelocity) {
        float uiChartWidth = this.uiChartWidth;
        if (uiChartWidth == 0f) return;

        int startX = (int) (xScale * range.getStart() * uiChartWidth);
        int minX = 0;
        int maxX = (int) (xScale * uiChartWidth) - getWidth();

        flingScroller.forceFinished(true);
        flingScroller.fling(startX, 0, (int) -xVelocity, 0, minX, maxX, 0, 0);

        invalidateOnAnimation();
    }

    @Override
    public void computeScroll() {
        if (touchState != TOUCH_STATE_FLING) return;

        float uiChartWidth = this.uiChartWidth;
        if (uiChartWidth == 0f) return;

        if (flingScroller.computeScrollOffset()) {

            int currOffs = flingScroller.getCurrX();
            float newRangeStart = (float) currOffs / (xScale * uiChartWidth);
            float newRangeEnd = newRangeStart + range.getSize();

            updateRanges(newRangeStart, newRangeEnd, true);
            invalidateOnAnimation();
        } else {
            touchState = TOUCH_STATE_IDLE;

            int currOffs = flingScroller.getFinalX();
            float newRangeStart = (float) currOffs / (xScale * uiChartWidth);
            float newRangeEnd = newRangeStart + range.getSize();

            updateRanges(newRangeStart, newRangeEnd, true);

            invalidateOnAnimation();
        }
    }

    // =======

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.rangeStart = range.getStart();
        savedState.rangeEnd = range.getEnd();
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
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

}
