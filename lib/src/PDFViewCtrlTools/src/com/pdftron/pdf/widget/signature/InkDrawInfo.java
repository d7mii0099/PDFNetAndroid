package com.pdftron.pdf.widget.signature;

import android.graphics.Paint;
import android.graphics.Path;

class InkDrawInfo {
    public final int left, right, top, bottom;
    public final Path path;
    public final Paint paint;

    InkDrawInfo(int left, int right, int top, int bottom, Path path, Paint paint) {
        this.left = left;
        this.right = right;
        this.top = top;
        this.bottom = bottom;
        this.path = path;
        this.paint = paint;
    }
}
