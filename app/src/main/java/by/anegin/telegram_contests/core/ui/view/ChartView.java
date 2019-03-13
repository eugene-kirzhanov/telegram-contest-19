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
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.OverScroller;
import by.anegin.telegram_contests.R;
import by.anegin.telegram_contests.core.ui.model.Graph;
import by.anegin.telegram_contests.core.ui.model.UiChart;

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

    private OnUiChartChangeListener onUiChartChangeListener;

    private OnRangeChangeListener onRangeChangeListener;

    private final Paint contentBgPaint = new Paint();
    private final Paint graphPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final Matrix graphMatrix = new Matrix();

    private float rangeStart = 0f;
    private float rangeEnd = 1f;

    private float xScale = 1f;

    private UiChart uiChart;

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private VelocityTracker velocityTracker;
    private OverScroller flingScroller;

    private int touchState = TOUCH_STATE_IDLE;
    private float downX;
    private float lastTouchX;

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
        int contentBgColor = viewAttrs.getColor(R.styleable.ChartView_content_bg_color, Color.TRANSPARENT);
        float graphLineWidth = viewAttrs.getDimension(R.styleable.ChartView_graph_line_width, defaultGraphLineWidth);
        viewAttrs.recycle();

        contentBgPaint.setStyle(Paint.Style.FILL);
        contentBgPaint.setColor(contentBgColor);

        graphPathPaint.setStyle(Paint.Style.STROKE);
        graphPathPaint.setStrokeWidth(graphLineWidth);

        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();

        flingScroller = new OverScroller(context);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        updateScaleAndOffset();
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawRect(0f, 0f, getWidth(), getHeight(), contentBgPaint);

        UiChart uiChart = this.uiChart;
        if (uiChart != null) {
            for (Graph graph : uiChart.graphs) {
                Graph transformedGraph = graph.transform(graphMatrix);
                transformedGraph.draw(canvas, graphPathPaint);
            }
        }
    }

    public void setRange(float start, float end) {
        touchState = TOUCH_STATE_IDLE;

        flingScroller.forceFinished(true);

        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }

        updateRanges(start, end);
    }

    public void setUiChart(UiChart uiChart) {
        this.uiChart = uiChart;
        updateScaleAndOffset();
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

    // =======

    private void updateRanges(float start, float end) {
        if (start < 0f) {
            end -= start;
            start = 0f;
        }
        if (end > 1f) {
            start -= (end - 1f);
            end = 1f;
        }
        if (start != rangeStart || end != rangeEnd) {
            rangeStart = start;
            rangeEnd = end;

            updateScaleAndOffset();

            if (onRangeChangeListener != null) {
                onRangeChangeListener.onRangeChangeListener(start, end);
            }

            invalidate();
        }
    }

    private void updateScaleAndOffset() {
        UiChart uiChart = this.uiChart;
        if (uiChart == null) return;

        float visibleChartWidth = uiChart.width * (rangeEnd - rangeStart);
        xScale = getWidth() / visibleChartWidth;
        float yScale = getHeight() / (float) uiChart.height;

        float xOffs = -xScale * rangeStart * uiChart.width;

        graphMatrix.reset();
        graphMatrix.setTranslate(xOffs, getHeight());
        graphMatrix.preScale(xScale, -yScale);
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

        float newRangeStart = rangeStart - rangeOffs;
        float newRangeEnd = rangeEnd - rangeOffs;

        updateRanges(newRangeStart, newRangeEnd);
    }

    private void flingChart(float xVelocity) {
        UiChart uiChart = this.uiChart;
        if (uiChart == null) return;

        int startX = (int) (xScale * rangeStart * uiChart.width);
        int minX = 0;
        int maxX = (int) (xScale * uiChart.width) - getWidth();

        flingScroller.forceFinished(true);
        flingScroller.fling(startX, 0, (int) -xVelocity, 0, minX, maxX, 0, 0);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            postInvalidateOnAnimation();
        } else {
            postInvalidate();
        }
    }

    @Override
    public void computeScroll() {
        if (touchState != TOUCH_STATE_FLING) return;
        if (flingScroller.computeScrollOffset()) {
            UiChart uiChart = this.uiChart;
            if (uiChart == null) return;

            int currOffs = flingScroller.getCurrX();
            float newRangeStart = (float) currOffs / (xScale * uiChart.width);
            float newRangeEnd = newRangeStart + (rangeEnd - rangeStart);

            updateRanges(newRangeStart, newRangeEnd);
        } else {
            touchState = TOUCH_STATE_IDLE;
            invalidate();
        }
    }


    // =======

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.rangeStart = rangeStart;
        savedState.rangeEnd = rangeEnd;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        updateRanges(savedState.rangeStart, savedState.rangeStart);
    }

    private static class SavedState extends BaseSavedState {

        float rangeStart;
        float rangeEnd;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            rangeStart = in.readFloat();
            rangeEnd = in.readFloat();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
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
