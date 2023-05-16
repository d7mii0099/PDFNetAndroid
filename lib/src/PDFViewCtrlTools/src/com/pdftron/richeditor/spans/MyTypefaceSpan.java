package com.pdftron.richeditor.spans;

import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.style.MetricAffectingSpan;

public class MyTypefaceSpan extends MetricAffectingSpan {

    private static Typeface sTypeface;
    public static String sFontPath;

    public MyTypefaceSpan(Typeface typeface, String path) {
        sTypeface = typeface;
        sFontPath = path;
    }

    public int getSpanTypeId() {
        return 13;
    }

    public int describeContents() {
        return 0;
    }

    private static void apply(Paint paint) {
        if (null != sTypeface) {
            paint.setTypeface(sTypeface);
        }
    }

    @Override
    public void updateMeasureState(TextPaint p) {
        apply(p);
    }

    @Override
    public void updateDrawState(TextPaint tp) {
        apply(tp);
    }
}
