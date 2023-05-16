package com.pdftron.pdf.utils;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import androidx.core.content.ContextCompat;

import com.pdftron.pdf.GeometryCollection;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;

public class SnapUtils {

    private int mSnapModeFlags = GeometryCollection.SnappingMode.e_default_snap_mode;
    private SnappingMode mSnappedToMode;
    private PointF mSnapToPoint;
    private final PDFViewCtrl mPdfViewCtrl;

    public SnapUtils(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    private boolean mCanDraw = false;

    public void setSnappingMode(int modeFlags) {
        if (modeFlags != 0) {
            mSnapModeFlags = modeFlags;
        }
        mPdfViewCtrl.setSnappingMode(mSnapModeFlags);
    }

    public PointF snapToNearest(double x, double y) {
        mSnapToPoint = mPdfViewCtrl.snapToNearestInDoc(x, y);

        mPdfViewCtrl.setSnappingMode(GeometryCollection.SnappingMode.e_point_on_line);
        PointF e_point_on_line = mPdfViewCtrl.snapToNearestInDoc(x, y);
        mPdfViewCtrl.setSnappingMode(GeometryCollection.SnappingMode.e_line_intersection);
        PointF e_line_intersection = mPdfViewCtrl.snapToNearestInDoc(x, y);
        mPdfViewCtrl.setSnappingMode(GeometryCollection.SnappingMode.e_line_midpoint);
        PointF e_line_midpoint = mPdfViewCtrl.snapToNearestInDoc(x, y);
        mPdfViewCtrl.setSnappingMode(GeometryCollection.SnappingMode.e_path_endpoint);
        PointF e_path_endpoint = mPdfViewCtrl.snapToNearestInDoc(x, y);

        //reset to original modes set
        mPdfViewCtrl.setSnappingMode(mSnapModeFlags);

        if (((mSnapModeFlags & GeometryCollection.SnappingMode.e_line_intersection) != 0) & mSnapToPoint.x == e_line_intersection.x && mSnapToPoint.y == e_line_intersection.y) {
            mSnappedToMode = SnappingMode.INTERSECTION;
        } else if (((mSnapModeFlags & GeometryCollection.SnappingMode.e_line_midpoint) != 0) & mSnapToPoint.x == e_line_midpoint.x && mSnapToPoint.y == e_line_midpoint.y) {
            mSnappedToMode = SnappingMode.MIDPOINT;
        } else if (((mSnapModeFlags & GeometryCollection.SnappingMode.e_path_endpoint) != 0) & mSnapToPoint.x == e_path_endpoint.x && mSnapToPoint.y == e_path_endpoint.y) {
            mSnappedToMode = SnappingMode.ENDPOINT;
        } else if (((mSnapModeFlags & GeometryCollection.SnappingMode.e_point_on_line) != 0) & mSnapToPoint.x == e_point_on_line.x && mSnapToPoint.y == e_point_on_line.y) {
            mSnappedToMode = SnappingMode.POINT_ON_LINE;
        } else {
            mSnappedToMode = SnappingMode.DEFAULT;
        }

        return mSnapToPoint;
    }

    public void setCanDraw(boolean canDraw) {
        mCanDraw = canDraw;
    }

    public void drawSnapToShape(Canvas canvas) {

        if (mCanDraw) {

            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(ContextCompat.getColor(mPdfViewCtrl.getContext(), R.color.tools_snap_mode_icon));
            paint.setAntiAlias(true);
            paint.setStrokeWidth(8f);

            if (mSnapToPoint != null) {
                float adjustedSnapPointX = mSnapToPoint.x + mPdfViewCtrl.getScrollX();
                float adjustedSnapPointY = mSnapToPoint.y + mPdfViewCtrl.getScrollY();

                float radius = 30f;

                float left = adjustedSnapPointX - radius;
                float mid = adjustedSnapPointX;
                float right = adjustedSnapPointX + radius;
                float top = adjustedSnapPointY - radius;
                float bottom = adjustedSnapPointY + radius;

                if (mSnappedToMode != null) {
                    switch (mSnappedToMode) {
                        case POINT_ON_LINE:
                            //PointOnLine → Circle
                            canvas.drawCircle(adjustedSnapPointX, adjustedSnapPointY, radius, paint);
                            break;
                        case MIDPOINT:
                            //Midpoint → Triangle
                            Path trianglePath = new Path();
                            trianglePath.moveTo(left, bottom);
                            trianglePath.lineTo(mid, top);
                            trianglePath.moveTo(mid, top);
                            trianglePath.lineTo(right, bottom);
                            trianglePath.moveTo(right, bottom);
                            trianglePath.lineTo(left, bottom);
                            trianglePath.close();
                            canvas.drawPath(trianglePath, paint);
                            break;
                        case INTERSECTION:
                            //Intersection → Cross
                            Path crossPath = new Path();
                            crossPath.moveTo(left, top);
                            crossPath.lineTo(right, bottom);
                            crossPath.moveTo(left, bottom);
                            crossPath.lineTo(right, top);
                            crossPath.close();
                            canvas.drawPath(crossPath, paint);
                            break;
                        case ENDPOINT:
                            //Endpoint → Square
                            canvas.drawRect(left, top, right, bottom, paint);
                            break;
                    }
                }
            }
        }
    }

    public enum SnappingMode {
        POINT_ON_LINE(1),
        MIDPOINT(2),
        INTERSECTION(4),
        ENDPOINT(8),
        DEFAULT(14);

        public int value;

        SnappingMode(int value) {
            this.value = value;
        }
    }
}
