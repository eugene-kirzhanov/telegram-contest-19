package by.anegin.telegram_contests.core.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import by.anegin.telegram_contests.R;
import by.anegin.telegram_contests.core.ui.model.Graph;
import by.anegin.telegram_contests.core.ui.model.UiChart;

public class ChartView extends View {

    public interface OnUiChartChangeListener {
        void onUiChartChanged(UiChart uiChart);
    }

    private OnUiChartChangeListener onUiChartChangeListener;

    private final Paint contentBgPaint = new Paint();
    private final Paint graphPathPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);

    private final Matrix graphMatrix = new Matrix();

    private float rangeStart = 0f;
    private float rangeEnd = 1f;

    private UiChart uiChart;

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
    }

    @Override
    public void onDraw(Canvas canvas) {
        int width = getWidth();
        int height = getHeight();
        if (width == 0 || height == 0) return;

        canvas.drawRect(0f, 0f, width, height, contentBgPaint);

        UiChart uiChart = this.uiChart;
        if (uiChart == null) return;

        float visibleChartWidth = uiChart.width * (rangeEnd - rangeStart);
        float xScale = width / visibleChartWidth;
        float yScale = height / (float) uiChart.height;

        float xOffs = -xScale * rangeStart * uiChart.width;

        graphMatrix.reset();
        graphMatrix.setTranslate(xOffs, height);
        graphMatrix.preScale(xScale, -yScale);

        for (Graph graph : uiChart.graphs) {
            Graph transformedGraph = graph.transform(graphMatrix);
            transformedGraph.draw(canvas, graphPathPaint);
        }
    }

    public void setRange(float start, float end) {
        if (rangeStart != start || rangeEnd != end) {
            rangeStart = start;
            rangeEnd = end;
            invalidate();
        }
    }

    public void setUiChart(UiChart uiChart) {
        this.uiChart = uiChart;
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
        rangeStart = savedState.rangeStart;
        rangeEnd = savedState.rangeEnd;
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
