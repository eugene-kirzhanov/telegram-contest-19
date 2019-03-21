package by.anegin.telegram_contests.core.ui.model;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import by.anegin.telegram_contests.R;

import java.util.List;

public class Popup {

    private static final int POPUP_COLUMNS_COUNT = 2;

    private final Paint paint = new Paint();

    private final Paint bgPaint = new Paint();
    private final TextPaint textPaint = new TextPaint();

    private final RectF popupRect = new RectF();

    private final Rect textRect = new Rect();

    private final Drawable popupDrawable;

    private final int titleTextColor;
    private final float spacing;

    // ========

    private final float textLineHeight;

    public Popup(Context context, int titleTextColor, float titleTextSize, float spacing) {
        this.titleTextColor = titleTextColor;
        this.spacing = spacing;

        textPaint.setTextSize(titleTextSize);
        textLineHeight = -textPaint.ascent() + textPaint.descent();

        TypedArray a = context.getTheme().obtainStyledAttributes(R.style.BaseAppTheme, new int[]{R.attr.popup_background});
        int attributeResourceId = a.getResourceId(0, 0);
        popupDrawable = context.getResources().getDrawable(attributeResourceId);

        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(Color.RED);
    }

    public void draw(Canvas canvas, Data data) {
        float popupLeft = spacing;
        float popupTop = spacing;

        float popupHeight = spacing * 2f;

        textPaint.getTextBounds(data.date, 0, data.date.length(), textRect);
        float popupTitleWidth = textRect.width();
        popupHeight += textLineHeight + spacing;

        float maxColumnWidth = 0f;
        int column = 0;
        for (Value value : data.values) {
            textPaint.getTextBounds(value.id, 0, value.id.length(), textRect);
            if (textRect.width() > maxColumnWidth) maxColumnWidth = textRect.width();
            textPaint.getTextBounds(value.value, 0, value.value.length(), textRect);
            if (textRect.width() > maxColumnWidth) maxColumnWidth = textRect.width();
            column++;
            if (column == POPUP_COLUMNS_COUNT) {
                column = 0;
                popupHeight += 2 * textLineHeight + spacing;
            }
        }
        if (column > 0) popupHeight += 2 * textLineHeight;

        int columnsCount = Math.min(POPUP_COLUMNS_COUNT, data.values.size());
        float columnsWidthWithSpacing = columnsCount * maxColumnWidth;
        if (columnsCount > 1) {
            columnsWidthWithSpacing += (columnsCount - 1) * (spacing * 2f);
        }

        float popupWidth = spacing * 2f + Math.max(popupTitleWidth, columnsWidthWithSpacing);

        popupRect.set(popupLeft, popupTop, popupLeft + popupWidth, popupTop + popupHeight);
        bgPaint.setStyle(Paint.Style.FILL);
        bgPaint.setColor(Color.WHITE);
        canvas.drawRoundRect(popupRect, 10, 10, bgPaint);
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setColor(Color.LTGRAY);
        canvas.drawRoundRect(popupRect, 10, 10, bgPaint);

        // title
        textPaint.setColor(titleTextColor);
        float tx = popupLeft + (popupWidth - popupTitleWidth) / 2f;
        float ty = popupTop + spacing - textPaint.ascent();
        canvas.drawText(data.date, tx, ty, textPaint);

        ty += spacing - textPaint.ascent();

        if (columnsCount == 1) {

            canvas.drawRect(popupLeft + spacing, ty + textPaint.ascent(), popupLeft + popupWidth - spacing, ty + textPaint.descent(), paint);

            Value value = data.values.get(0);
            textPaint.setColor(value.color);

            textPaint.getTextBounds(value.id, 0, value.id.length(), textRect);
            tx = popupLeft + (popupWidth - textRect.width()) / 2f;
            canvas.drawText(value.id, tx, ty, textPaint);

            ty += textLineHeight;

            canvas.drawRect(popupLeft + spacing, ty + textPaint.ascent(), popupLeft + popupWidth - spacing, ty + textPaint.descent(), paint);

            textPaint.getTextBounds(value.value, 0, value.value.length(), textRect);
            tx = popupLeft + (popupWidth - textRect.width()) / 2f;
            canvas.drawText(value.value, tx, ty, textPaint);

        } else {

        }

//        columnsWidthWithSpacing = 0;
//        for (int i = 0; i < columnsCount; i++) columnsWidthWithSpacing += popupColumnsMaxWidth[i];
//        float columnsSpacing;
//        if (columnsCount > 1) {
//            columnsSpacing = (popupWidth - columnsWidthWithSpacing - 2 * spacing) / (columnsCount - 1);
//        } else {
//            columnsSpacing = 0f;
//        }
//
//        ty += spacing - textPaint.ascent();
//        column = 0;
//        for (Value value : data.values) {
//            float columnWidth = popupColumnsMaxWidth[column];
//            float xx = popupLeft + spacing + column * (columnWidth + columnsSpacing);
//            canvas.drawRect(xx, ty + textPaint.ascent(), xx + columnWidth, ty + textPaint.descent(), paint);
//
//            textPaint.getTextBounds(value.id, 0, value.id.length(), textRect);
//
//
//            tx = xx + (columnWidth - textRect.width()) / 2f;
//
//            textPaint.setColor(value.color);
//            canvas.drawText(value.id, tx, ty, textPaint);
//
//            column++;
//            if (column == POPUP_COLUMNS_COUNT) {
//                column = 0;
//                ty += spacing - textPaint.ascent();
//            }
//        }
    }

    public static class Data {
        private final long x;
        private final String date;
        private final List<Value> values;

        private int alpha;

        public Data(long x, String date, List<Value> values) {
            this.x = x;
            this.date = date;
            this.values = values;
        }
    }

    public static class Value {
        private final String id;
        private final String value;
        private final int color;

        public Value(String id, String value, int color) {
            this.id = id;
            this.value = value;
            this.color = color;
        }
    }

}
