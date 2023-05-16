package com.pdftron.richeditor.spans;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.text.Layout;
import android.text.style.LeadingMarginSpan;
import android.text.style.QuoteSpan;

import com.pdftron.richeditor.Constants;

public class AreQuoteSpan extends QuoteSpan {

    @Override
    public int getLeadingMargin(boolean first) {
        return 45; // hard-coded..
    }

    @Override
    public void drawLeadingMargin(Canvas c, Paint p, int x, int dir, int top, int baseline, int bottom, CharSequence text, int start, int end, boolean first, Layout layout) {
        final int INDENT = 30;
        c.translate(INDENT, 0);
        Paint.Style style = p.getStyle();
        int color = p.getColor();

        p.setStyle(Paint.Style.FILL);
        p.setColor(Constants.COLOR_QUOTE);

        c.drawRect(x, top, x + dir * 2 + 5, bottom, p); // Hard-coded - right

        p.setStyle(style);
        p.setColor(color);
        c.translate(-INDENT, 0);
    }
}
