package by.anegin.telegram_contests.core.ui.view;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.OverScroller;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import by.anegin.telegram_contests.R;
import by.anegin.telegram_contests.core.ui.ScaleAnimationHelper;
import by.anegin.telegram_contests.core.ui.ToggleAnimationHelper;
import by.anegin.telegram_contests.core.ui.model.UiChart;
import by.anegin.telegram_contests.core.ui.model.UiDate;
import by.anegin.telegram_contests.core.ui.objects.Graph;
import by.anegin.telegram_contests.core.ui.objects.Grid;
import by.anegin.telegram_contests.core.ui.objects.Popup;
import by.anegin.telegram_contests.core.utils.AtomicRange;

public class ChartView extends View implements ScaleAnimationHelper.Callback, ToggleAnimationHelper.Callback {

    private static final int TOUCH_STATE_IDLE = 1;
    private static final int TOUCH_STATE_DRAG = 2;
    private static final int TOUCH_STATE_FLING = 3;

    private static final int AUTOSCALE_ANIMATION_DURATION = 250;
    private static final int TOGGLE_ANIMATION_DURATION = 200;

    private static final int DATE_LABEL_FADE_DURATION = 300;

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

    private final Paint gridLinePaint = new Paint();

    private final TextPaint textPaint = new TextPaint();
    private final Paint paint = new Paint();

    private final Matrix graphMatrix = new Matrix();

    private float gridLineWidth = 0f;
    private int gridLineColor;

    private float textSize;
    private int textColor;

    private float graphStrokeWidth = 0f;

    private final AtomicRange range = new AtomicRange();

    private float xOffs = 0f;
    private float xScale = 0f;
    private volatile float yScale = 0f;

    private long[] xValues;
    private long minX;
    private final List<UiDate> dates = new ArrayList<>();
    private final List<Graph> graphs = new ArrayList<>();
    private float uiChartWidth;

    private int touchSlop;
    private int minFlingVelocity;
    private int maxFlingVelocity;
    private int longPressTimeout;
    private VelocityTracker velocityTracker;
    private OverScroller flingScroller;

    private int touchState = TOUCH_STATE_IDLE;
    private float downX;
    private float lastTouchX;
    private long downTime;

    private final ScaleAnimationHelper scaleAnimationHelper = new ScaleAnimationHelper(this, AUTOSCALE_ANIMATION_DURATION);

    private final ToggleAnimationHelper toggleAnimationHelper = new ToggleAnimationHelper(this, TOGGLE_ANIMATION_DURATION);

    private final Rect textRect = new Rect();

    // =======

    private final List<DateLabel> dateLabels = new ArrayList<>();
    private final Map<UiDate, DateLabel> hidingDateLabels = new HashMap<>();

    private float dateLabelWidth = 0f;

    // =======

    private Popup popup;
    private Popup.Data popupData;

    private Float pendingClickX;

    // =======

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
        gridLineWidth = viewAttrs.getDimension(R.styleable.ChartView_grid_line_width, 0f);
        gridLineColor = viewAttrs.getColor(R.styleable.ChartView_grid_line_color, Color.TRANSPARENT);
        graphStrokeWidth = viewAttrs.getDimension(R.styleable.ChartView_graph_line_width, defaultGraphLineWidth);
        textSize = viewAttrs.getDimension(R.styleable.ChartView_text_size, 0f);
        textColor = viewAttrs.getColor(R.styleable.ChartView_text_color, Color.TRANSPARENT);
        int popupTitleTextColor = viewAttrs.getColor(R.styleable.ChartView_popup_title_color, Color.TRANSPARENT);
        float popupTitleTextSize = viewAttrs.getDimension(R.styleable.ChartView_popup_title_text_size, 0f);
        float popupValueTextSize = viewAttrs.getDimension(R.styleable.ChartView_popup_value_text_size, 0f);
        float popupTopBottomPadding = viewAttrs.getDimension(R.styleable.ChartView_popup_top_bottom_padding, 0f);
        float popupLeftRightPadding = viewAttrs.getDimension(R.styleable.ChartView_popup_left_right_padding, 0f);
        float pointRadius = viewAttrs.getDimension(R.styleable.ChartView_point_radius, 0f);
        int pointInnerColor = viewAttrs.getColor(R.styleable.ChartView_point_inner_color, Color.TRANSPARENT);
        viewAttrs.recycle();

        gridLinePaint.setStyle(Paint.Style.STROKE);
        gridLinePaint.setColor(gridLineColor);

        ViewConfiguration vc = ViewConfiguration.get(context);
        touchSlop = vc.getScaledTouchSlop();
        minFlingVelocity = vc.getScaledMinimumFlingVelocity();
        maxFlingVelocity = vc.getScaledMaximumFlingVelocity();
        longPressTimeout = ViewConfiguration.getLongPressTimeout();

        flingScroller = new OverScroller(context);

        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);

        paint.setColor(Color.BLUE);
        paint.setStyle(Paint.Style.STROKE);

        popup = new Popup(context,
                popupTitleTextColor, popupTitleTextSize,
                popupValueTextSize,
                popupTopBottomPadding, popupLeftRightPadding,
                graphStrokeWidth, pointRadius, pointInnerColor);
    }

    @Override
    public void onDraw(Canvas canvas) {
        // horizontal grid lines
        gridLinePaint.setStrokeWidth(gridLineWidth * 2f);
        canvas.drawLine(0f, getHeight(), getWidth(), getHeight(), gridLinePaint);

        if (xScale == 0f || yScale == 0f) return;

        canvas.save();
        canvas.translate(-xOffs, 0f);

        // selected data (vertical line)
        Popup.Data popupData = this.popupData;
        if (popupData != null) {
            gridLinePaint.setStrokeWidth(gridLineWidth * 2f);
            float x = popupData.chartX * xScale;
            canvas.drawLine(x, 0, x, getHeight(), gridLinePaint);
        }

        // date labels
        float dateLabelsY = getHeight() + 1.2f * (-textPaint.ascent() + textPaint.descent());
        textPaint.setAlpha(255);
        synchronized (dates) {
            canvas.drawText(dates.get(0).text, 0f, dateLabelsY, textPaint);
            canvas.drawText(dates.get(dates.size() - 1).text, uiChartWidth * xScale - dateLabelWidth, dateLabelsY, textPaint);
        }
        float halfDateWidth = dateLabelWidth / 2f;
        for (DateLabel label : dateLabels) {
            textPaint.setAlpha((int) (label.alpha * 255));
            canvas.drawText(label.uiDate.text, label.sx - halfDateWidth, dateLabelsY, textPaint);
        }
        for (DateLabel label : hidingDateLabels.values()) {
            textPaint.setAlpha((int) (label.alpha * 255));
            canvas.drawText(label.uiDate.text, label.sx - halfDateWidth, dateLabelsY, textPaint);
        }

        canvas.restore();

        // y grid lines
        synchronized (grids) {
            for (Grid grid : grids) {
                grid.drawLines(canvas, yScale);
            }
        }
        synchronized (hidingGrids) {
            for (Grid grid : hidingGrids) {
                grid.drawLines(canvas, yScale);
            }
        }

        // charts
        synchronized (graphs) {
            canvas.save();
            canvas.clipRect(0f, 0f, getWidth(), getHeight());
            for (Graph graph : graphs) {
                graph.draw(canvas);
            }
            canvas.restore();
        }

        // y grid labels
        synchronized (grids) {
            for (Grid grid : grids) {
                grid.drawLabels(canvas, yScale);
            }
        }
        synchronized (hidingGrids) {
            for (Grid grid : hidingGrids) {
                grid.drawLabels(canvas, yScale);
            }
        }

        // popup
        if (popupData != null) {
            popup.drawPoints(canvas, popupData, xOffs, popupData.chartX * xScale, yScale);
            popup.drawPopup(canvas, popupData, xOffs, popupData.chartX * xScale, uiChartWidth * xScale);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        calculateXScale();
        scaleAnimationHelper.calculate(false);
    }

    private void calculateXScale() {
        if (uiChartWidth == 0 || getWidth() == 0) return;
        float newXScale = getWidth() / (uiChartWidth * range.getSize());
        float newXOffs = newXScale * range.getStart() * uiChartWidth;

        int labelsCount = dateLabels.size();
        if (labelsCount == 0) {
            // generate minimum set of dateLabels for minimum xScale

            // determine minimum amount of lables
            float minXScale = getWidth() / uiChartWidth;    // для rangeSize = 1
            float remainingWidth = minXScale * uiChartWidth - dateLabelWidth * 2;   // excluding first/last date
            float preSpacing = dateLabelWidth * 0.8f;
            labelsCount = (int) Math.floor((remainingWidth - preSpacing) / (dateLabelWidth + preSpacing));
            float labelsSpacing = (remainingWidth - dateLabelWidth * labelsCount) / (labelsCount + 1);

            // add initial set of labels
            float x = 1.5f * dateLabelWidth + labelsSpacing;
            for (int i = 0; i < labelsCount; i++) {
                dateLabels.add(new DateLabel(findNearestDate(x / minXScale, dates), x, 1f));
                x += dateLabelWidth + labelsSpacing;
            }

            if (range.getSize() < 1f) {
                // try to increase spacing between labels until we can insert new labels or reach newXScale
                float spacingToAddLabels = dateLabelWidth * 1.5f;
                float scale;
                do {
                    // determine scale if we increase spacing to size, when we can insert labels
                    scale = (spacingToAddLabels * (labelsCount + 1) + dateLabelWidth * (labelsCount + 2)) / uiChartWidth;

                    if (scale > newXScale) {
                        scale = newXScale;
                    }

                    // move existing visible lables for new scale
                    remainingWidth = scale * uiChartWidth - dateLabelWidth * 2;
                    labelsSpacing = (remainingWidth - dateLabelWidth * labelsCount) / (labelsCount + 1);
                    x = 1.5f * dateLabelWidth + labelsSpacing;
                    for (DateLabel label : dateLabels) {
                        label.sx = x;
                        x += dateLabelWidth + labelsSpacing;
                    }

                    float distance = dateLabels.get(0).sx - 1.5f * dateLabelWidth;
                    if (distance >= spacingToAddLabels) {
                        // insert new labels
                        List<DateLabel> newLabels = new ArrayList<>(dateLabels.size() * 2 + 1);

                        x = dateLabelWidth + distance / 2;

                        DateLabel newLabel = new DateLabel(findNearestDate(x / scale, dates), x, 1f);
                        newLabels.add(newLabel);

                        for (DateLabel label : dateLabels) {
                            newLabels.add(label);
                            x += dateLabelWidth + distance;
                            newLabel = new DateLabel(findNearestDate(x / scale, dates), x, 1f);
                            newLabels.add(newLabel);
                        }

                        dateLabels.clear();
                        dateLabels.addAll(newLabels);

                        labelsCount = dateLabels.size();
                    }

                } while (scale < newXScale);
            }

        } else {

            // move existing visible lables for new scale
            float remainingWidth = newXScale * uiChartWidth - dateLabelWidth * 2;   // excluding first/last date
            float labelsSpacing = (remainingWidth - dateLabelWidth * labelsCount) / (labelsCount + 1);
            float x = 1.5f * dateLabelWidth + labelsSpacing;
            for (DateLabel label : dateLabels) {
                label.sx = x;
                x += dateLabelWidth + labelsSpacing;
            }

            // move existing hiding lables for new scale
            for (DateLabel label : hidingDateLabels.values()) {
                label.sx = (label.sx / xScale) * newXScale;
            }

            // current spacing between labels
            float distance = dateLabels.get(0).sx - 1.5f * dateLabelWidth;

            if (newXScale > xScale && distance > dateLabelWidth * 1.5f) {
                // spacing is increasing and it enough to insert new labels
                // insert new lables with fade-in animation

                List<DateLabel> newLabels = new ArrayList<>(dateLabels.size() * 2 + 1);
                x = dateLabelWidth + distance / 2;
                DateLabel newLabel = new DateLabel(findNearestDate(x / newXScale, dates), x, 0f);
                newLabels.add(newLabel);
                newLabel.fadeIn();
                for (DateLabel label : dateLabels) {
                    newLabels.add(label);
                    x += dateLabelWidth + distance;
                    newLabel = new DateLabel(findNearestDate(x / newXScale, dates), x, 0f);
                    newLabels.add(newLabel);
                    newLabel.fadeIn();
                }
                dateLabels.clear();
                dateLabels.addAll(newLabels);

            } else if (newXScale < xScale && distance < dateLabelWidth * 0.3f) {
                // spacing is decreasing and became less than minimum
                // remove odd labels with animation

                // find all odd labels
                List<DateLabel> labelsToRemove = new ArrayList<>();
                for (int i = 0; i < dateLabels.size(); i += 2) {
                    labelsToRemove.add(dateLabels.get(i));
                }

                // remove from list of visible labels
                dateLabels.removeAll(labelsToRemove);

                // add to list of hiding labels, execute fade-out animation
                for (DateLabel label : labelsToRemove) {
                    hidingDateLabels.put(label.uiDate, label);
                    label.fadeOut();
                }
            }
        }

        this.xScale = newXScale;
        this.xOffs = newXOffs;
        invalidateOnAnimation();
    }

    private static UiDate findNearestDate(float x, List<UiDate> dates) {
        // find date nearest to X (canvas x)
        float dist;
        float minDist = Float.MAX_VALUE;
        UiDate nearestDate = null;
        for (UiDate uiDate : dates) {
            dist = Math.abs(uiDate.x - x);
            if (dist < minDist) {
                minDist = dist;
                nearestDate = uiDate;
            }
        }
        return nearestDate;
    }

    // ==========

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
            dates.clear();
            graphs.clear();

            dateLabels.clear();
            hidingDateLabels.clear();

            popupData = null;

            if (uiChart != null) {
                xValues = uiChart.xValues;
                minX = uiChart.minX;

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
                dates.addAll(uiChart.dates);

                float maxTextWidth = 0f;
                for (UiDate date : uiChart.dates) {
                    textPaint.getTextBounds(date.text, 0, date.text.length(), textRect);
                    if (textRect.width() > maxTextWidth) maxTextWidth = textRect.width();
                }
                dateLabelWidth = maxTextWidth;

            } else {
                xValues = null;
                minX = 0;
                uiChartWidth = 0f;
                yScale = 0f;
            }
        }
        calculateXScale();
        scaleAnimationHelper.calculate(false);

        if (onUiChartChangeListener != null) {
            onUiChartChangeListener.onUiChartChanged(uiChart);
        }

        if (onGraphVisibilityChangeListener != null) {
            onGraphVisibilityChangeListener.onHiddenGraphChanged(hiddenGraphsIds);
        }

        if (uiChart != null && pendingClickX != null) {
            onClick(pendingClickX);
            pendingClickX = null;
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

        if (popupData != null) {
            onClick(popupData.clickX);
        }
    }

    public void showGraph(String id) {
        if (onGraphVisibilityChangeListener != null) {
            onGraphVisibilityChangeListener.onGraphShown(id);
        }
        toggleAnimationHelper.showGraph(id);

        if (popupData != null) {
            onClick(popupData.clickX);
        }
    }

    public int getGraphsCount() {
        return graphs.size();
    }

    // ==========

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
            calculateXScale();
            scaleAnimationHelper.calculate(animateYScale);
        }
    }

    @Override
    public ScaleAnimationHelper.CalcResult calculateNewScale() {
        float uiChartWidth;
        List<Graph> graphs;
        synchronized (this.graphs) {
            uiChartWidth = this.uiChartWidth;
            graphs = new ArrayList<>(this.graphs);
        }
        if (uiChartWidth == 0f || graphs.isEmpty())
            return new ScaleAnimationHelper.CalcResult(yScale, 0f);

        float startX = uiChartWidth * range.getStart();
        float endX = uiChartWidth * range.getEnd();

        float maxY = 0f;
        for (Graph graph : graphs) {
            float max = graph.findMaxYInRange(startX, endX);
            if (max > maxY) maxY = max;
        }

        float scale = (float) getHeight() / maxY;

        return new ScaleAnimationHelper.CalcResult(scale, maxY);
    }

    @Override
    public float getCurrentScale() {
        return yScale;
    }

    @Override
    public void onScaleUpdated(float scale, ScaleAnimationHelper.CalcResult calcResult) {
        if (scale == 0f) return;
        yScale = scale;

        graphMatrix.reset();
        graphMatrix.setTranslate(-xOffs, getHeight());
        graphMatrix.preScale(xScale, -yScale);

        synchronized (graphs) {
            for (Graph graph : graphs) {
                graph.transform(graphMatrix);
            }
        }

        if (lastGrid != null) {

            if (lastGrid.targetYScale != calcResult.targetScale) {
                grids.remove(lastGrid);
                hidingGrids.add(lastGrid);
                lastGrid.fadeOut(this, hidingGrids::remove);
                lastGrid = null;

                lastGrid = new Grid(scale, calcResult.targetScale, calcResult.maxY,
                        gridLineColor, gridLineWidth, textColor, textSize);
                grids.add(lastGrid);
            }

        } else {
            lastGrid = new Grid(scale, calcResult.targetScale, calcResult.maxY,
                    gridLineColor, gridLineWidth, textColor, textSize);
            grids.add(lastGrid);
        }

        synchronized (grids) {
            if (grids.isEmpty()) {
                Grid grid = new Grid(yScale, calcResult.targetScale, calcResult.maxY,
                        gridLineColor, gridLineWidth, textColor, textSize);
                grids.add(grid);
            }
        }

        invalidateOnAnimation();
    }

    private Grid lastGrid;
    private final List<Grid> grids = new ArrayList<>();
    private final Set<Grid> hidingGrids = new HashSet<>();

    // ==================

    public void invalidateOnAnimation() {
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
        float touchX = event.getX();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                downX = touchX;
                lastTouchX = downX;
                touchState = TOUCH_STATE_IDLE;

                downTime = SystemClock.uptimeMillis();

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
                if (touchState == TOUCH_STATE_IDLE && Math.abs(touchX - downX) > touchSlop) {
                    touchState = TOUCH_STATE_DRAG;

                    ViewParent viewParent = getParent();
                    if (viewParent != null) {
                        viewParent.requestDisallowInterceptTouchEvent(true);
                    }
                }
                if (touchState == TOUCH_STATE_DRAG) {
                    moveChart(touchX - lastTouchX);

                    popupData = null;
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

                    if (SystemClock.uptimeMillis() - downTime < longPressTimeout) {
                        onClick(touchX);
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

    private class DateLabel {

        private final UiDate uiDate;
        private float sx;  // screen x
        private float alpha;
        private ValueAnimator fadeInAnimation;

        private DateLabel(UiDate uiDate, float sx, float alpha) {
            this.uiDate = uiDate;
            this.sx = sx;
            this.alpha = alpha;
        }

        private void fadeIn() {
            hidingDateLabels.remove(uiDate);   // cancel existing hiding label

            long duration = (long) (DATE_LABEL_FADE_DURATION * (1f - alpha));

            ValueAnimator anim = ValueAnimator.ofFloat(alpha, 1f);
            anim.setDuration(duration);
            anim.setInterpolator(new AccelerateInterpolator());
            anim.addUpdateListener(animation -> {
                alpha = (float) animation.getAnimatedValue();
                invalidateOnAnimation();
            });
            this.fadeInAnimation = anim;
            anim.start();
        }

        private void fadeOut() {
            if (fadeInAnimation != null && fadeInAnimation.isRunning()) {
                fadeInAnimation.cancel();
            }

            long duration = (long) (DATE_LABEL_FADE_DURATION * alpha);

            ValueAnimator anim = ValueAnimator.ofFloat(alpha, 0f);
            anim.setDuration(duration);
            anim.setInterpolator(new DecelerateInterpolator());
            anim.addUpdateListener(animation -> {
                alpha = (float) animation.getAnimatedValue();
                if (alpha == 0f) {
                    hidingDateLabels.remove(uiDate);
                }
                invalidateOnAnimation();
            });
            anim.start();
        }
    }

    private void onClick(float clickX) {
        if (xValues == null || xValues.length == 0) return;

        float chartX = (xOffs + clickX) / xScale;

        // find index of chart data nearest to clickX
        int nearestXIndex;
        if (xValues.length == 1) {
            nearestXIndex = 0;
        } else {
            if (chartX <= xValues[0]) {
                nearestXIndex = 0;
            } else if (chartX >= xValues[xValues.length - 1]) {
                nearestXIndex = xValues.length - 1;
            } else {
                int i = 0;
                while (i < xValues.length && chartX > xValues[i]) i++;
                if (chartX - xValues[i - 1] < xValues[i] - chartX) {
                    nearestXIndex = i - 1;
                } else {
                    nearestXIndex = i;
                }
            }
        }
        if (nearestXIndex != -1) {
            long nearestX = xValues[nearestXIndex];

            SimpleDateFormat sdf = new SimpleDateFormat("EEE, MMM dd", Locale.US);
            String date = sdf.format(new Date(nearestX + minX));

            List<Popup.Value> values = new ArrayList<>();
            for (Graph graph : graphs) {
                if (graph.isVisible() && nearestXIndex < graph.yValues.length) {
                    values.add(new Popup.Value(
                            graph.name,
                            graph.yValues[nearestXIndex],
                            graph.color));
                }
            }

            popupData = new Popup.Data(clickX, nearestX, date, values);
            invalidate();
        }
    }

    // =======

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.rangeStart = range.getStart();
        savedState.rangeEnd = range.getEnd();
        savedState.clickX = popupData != null ? popupData.clickX : null;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());
        pendingClickX = savedState.clickX;
        updateRanges(savedState.rangeStart, savedState.rangeEnd, false);
    }

    private static class SavedState extends BaseSavedState {

        List<String> hiddenGraphIds;
        float rangeStart;
        float rangeEnd;
        Float clickX;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            hiddenGraphIds = in.createStringArrayList();
            rangeStart = in.readFloat();
            rangeEnd = in.readFloat();
            clickX = (Float) in.readValue(Float.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeStringList(hiddenGraphIds);
            out.writeFloat(rangeStart);
            out.writeFloat(rangeEnd);
            out.writeValue(clickX);
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