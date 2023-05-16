package com.pdftron.pdf.widget.signature;

import android.graphics.PointF;

import java.util.ArrayList;

class StrokeOutlineResult {
    private final ArrayList<PointF> pointPath;
    final double[] strokeOutline;

    StrokeOutlineResult(ArrayList<PointF> pointPath, double[] strokeOutline) {
        this.pointPath = pointPath;
        this.strokeOutline = strokeOutline;
    }
}
