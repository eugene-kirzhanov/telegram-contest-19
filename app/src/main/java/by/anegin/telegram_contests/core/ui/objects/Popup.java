package by.anegin.telegram_contests.core.ui.objects;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.text.TextPaint;
import by.anegin.telegram_contests.R;

import java.util.List;

public class Popup {

    private static final int POPUP_COLUMNS_COUNT = 2;

    private final TextPaint titleTextPaint = new TextPaint();
    private final TextPaint valuesTextPaint = new TextPaint();
    private final Paint circlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Rect textRect = new Rect();

    private final float topBottomPadding;
    private final float leftRightPadding;

    private final float titleTextLineHeight;
    private final float valuesTextLineHeight;

    private final float horizontalSpacing;
    private final float verticalSpacing;

    private final float circleRadius;
    private final int circleInnerColor;

    private final Drawable bgDrawable;

    @SuppressWarnings("deprecation")
    public Popup(Context context,
                 int titleTextColor, float titleTextSize,
                 float valuesTextSize,
                 float topBottomPadding, float leftRightPadding,
                 float circleStrokeWidth, float circleRadius, int circleInnerColor) {

        this.topBottomPadding = topBottomPadding;
        this.leftRightPadding = leftRightPadding;

        this.verticalSpacing = topBottomPadding;
        this.horizontalSpacing = leftRightPadding;

        this.circleRadius = circleRadius;
        this.circleInnerColor = circleInnerColor;

        titleTextPaint.setColor(titleTextColor);
        titleTextPaint.setTextSize(titleTextSize);
        titleTextPaint.setFakeBoldText(true);
        titleTextLineHeight = -titleTextPaint.ascent() + titleTextPaint.descent();

        valuesTextPaint.setTextSize(valuesTextSize);
        valuesTextLineHeight = -valuesTextPaint.ascent() + valuesTextPaint.descent();

        circlePaint.setStrokeWidth(circleStrokeWidth);

        TypedArray a = context.getTheme().obtainStyledAttributes(R.style.AppTheme, new int[]{R.attr.popup_background});
        int attributeResourceId = a.getResourceId(0, 0);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            bgDrawable = context.getDrawable(attributeResourceId);
        } else {
            bgDrawable = context.getResources().getDrawable(attributeResourceId);
        }
    }

    public void drawPopup(Canvas canvas, Data data, float xOffs, float x, float chartWidth) {

        titleTextPaint.getTextBounds(data.date, 0, data.date.length(), textRect);
        float popupTitleWidth = textRect.width();
        float popupHeight = topBottomPadding + titleTextLineHeight + verticalSpacing;

        float columnWidth = 0f;
        int column = 0;
        for (Value value : data.values) {

            valuesTextPaint.setFakeBoldText(true);
            valuesTextPaint.getTextBounds(value.stringValue, 0, value.stringValue.length(), textRect);
            if (textRect.width() > columnWidth) columnWidth = textRect.width();

            valuesTextPaint.setFakeBoldText(false);
            valuesTextPaint.getTextBounds(value.name, 0, value.name.length(), textRect);
            if (textRect.width() > columnWidth) columnWidth = textRect.width();

            column++;
            if (column == POPUP_COLUMNS_COUNT) {
                column = 0;
                popupHeight += 2 * valuesTextLineHeight + verticalSpacing;
            }

        }
        if (column > 0) popupHeight += 2 * valuesTextLineHeight + topBottomPadding;

        int columnsCount = Math.min(POPUP_COLUMNS_COUNT, data.values.size());
        float columnsWidthWithSpacing = columnsCount * columnWidth;
        if (columnsCount > 1) {
            columnsWidthWithSpacing += (columnsCount - 1) * horizontalSpacing;
        } else {
            popupHeight += topBottomPadding;
        }

        float contentWidth;
        if (popupTitleWidth > columnsWidthWithSpacing) {
            contentWidth = popupTitleWidth;
            columnWidth = (popupTitleWidth - (columnsCount - 1) * horizontalSpacing) / columnsCount;
        } else {
            contentWidth = columnsWidthWithSpacing;
        }
        float popupWidth = leftRightPadding * 2f + contentWidth;

        float popupLeft;
        float halfPopupWidth = popupWidth / 2f;
        if (x + halfPopupWidth > chartWidth) {
            popupLeft = chartWidth - popupWidth;
        } else if (x - halfPopupWidth < 0f) {
            popupLeft = 0f;
        } else {
            popupLeft = x - halfPopupWidth;
        }

        canvas.save();
        canvas.translate(-xOffs + popupLeft, 0f);

        // background
        drawBackground(canvas, popupWidth, popupHeight);

        // title
        float tx = (popupWidth - popupTitleWidth) / 2f;
        float ty = topBottomPadding;
        canvas.drawText(data.date, tx, ty - titleTextPaint.ascent(), titleTextPaint);

        ty += titleTextLineHeight + verticalSpacing;

        if (columnsCount == 1) {

            Value value = data.values.get(0);
            valuesTextPaint.setColor(value.color);

            valuesTextPaint.setFakeBoldText(true);
            valuesTextPaint.getTextBounds(value.stringValue, 0, value.stringValue.length(), textRect);
            tx = (popupWidth - textRect.width()) / 2f;
            canvas.drawText(value.stringValue, tx, ty - valuesTextPaint.ascent(), valuesTextPaint);

            valuesTextPaint.setFakeBoldText(false);
            valuesTextPaint.getTextBounds(value.name, 0, value.name.length(), textRect);
            tx = (popupWidth - textRect.width()) / 2f;
            canvas.drawText(value.name, tx, ty + valuesTextLineHeight - valuesTextPaint.ascent(), valuesTextPaint);

        } else {

            column = 0;
            for (Value value : data.values) {

                valuesTextPaint.setColor(value.color);

                tx = leftRightPadding + column * (columnWidth + horizontalSpacing);

                valuesTextPaint.setFakeBoldText(true);
                valuesTextPaint.getTextBounds(value.stringValue, 0, value.stringValue.length(), textRect);
                canvas.drawText(value.stringValue, tx + (columnWidth - textRect.width()) / 2f, ty - valuesTextPaint.ascent(), valuesTextPaint);

                valuesTextPaint.setFakeBoldText(false);
                valuesTextPaint.getTextBounds(value.name, 0, value.name.length(), textRect);
                canvas.drawText(value.name, tx + (columnWidth - textRect.width()) / 2f, ty + valuesTextLineHeight - valuesTextPaint.ascent(), valuesTextPaint);

                column++;
                if (column == POPUP_COLUMNS_COUNT) {
                    column = 0;
                    ty += 2 * valuesTextLineHeight + verticalSpacing;
                }
            }

        }

        canvas.restore();
    }

    public void drawPoints(Canvas canvas, Data data, float xOffs, float x, float yScale) {
        canvas.save();
        canvas.translate(-xOffs, 0f);
        for (Value value : data.values) {

            float y = canvas.getHeight() - value.value * yScale;

            circlePaint.setStyle(Paint.Style.FILL);
            circlePaint.setColor(circleInnerColor);
            canvas.drawCircle(x, y, circleRadius, circlePaint);

            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setColor(value.color);
            canvas.drawCircle(x, y, circleRadius, circlePaint);

        }
        canvas.restore();
    }

    private void drawBackground(Canvas canvas, float width, float height) {
        bgDrawable.setBounds(0, 0, (int) width, (int) height);
        bgDrawable.draw(canvas);
    }

    public static class Data {
        public final float clickX;
        public final long chartX;
        private final String date;
        private final List<Value> values;

        public Data(float clickX, long chartX, String date, List<Value> values) {
            this.clickX = clickX;
            this.chartX = chartX;
            this.date = date;
            this.values = values;
        }
    }

    public static class Value {
        private final String name;
        private final long value;
        private final String stringValue;
        private final int color;

        public Value(String name, long value, int color) {
            this.name = name;
            this.value = value;
            this.stringValue = String.valueOf(value);
            this.color = color;
        }
    }

}
