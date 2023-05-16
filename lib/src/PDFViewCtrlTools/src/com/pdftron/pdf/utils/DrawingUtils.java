package com.pdftron.pdf.utils;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.PointF;
import android.graphics.RectF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.ink.InkItem;
import com.pdftron.pdf.tools.AnnotEditAdvancedShape;
import com.pdftron.pdf.tools.CloudCreate;
import com.pdftron.pdf.tools.LineCreate;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.AnnotView;

import java.util.ArrayList;

public class DrawingUtils {

    public static final int sSelectionBoxMargin = 16;

    /**
     * Draws the annotation selection box. Color and style of the box depends on whether
     * selection permission is granted. If permission granted, draws a blue rectangle with no padding,
     * otherwise draw a red dashed rectangle with padding.
     *
     * @param canvas to draw the box
     * @param left   The left side of the rectangle to be drawn
     * @param top    The top side of the rectangle to be drawn
     * @param right  The right side of the rectangle to be drawn
     * @param bottom The bottom side of the rectangle to be drawn
     */
    public static void drawSelectionBox(@NonNull Paint paint, @NonNull Context context, @NonNull Canvas canvas,
            float left, float top, float right, float bottom,
            boolean hasSelectionPermission) {
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{Utils.convDp2Pix(context, 4.5f), Utils.convDp2Pix(context, 2.5f)}, 0);
        paint.setStyle(Paint.Style.STROKE);
        final float thickness = Utils.convDp2Pix(context, 2.2f);
        final float padding = thickness * 1.5f;
        final float cornerRad = thickness / 2.0f;
        final float lineOverlapPadding = thickness / 2.0f;
        final float calculatedPadding;
        if (hasSelectionPermission) {
            calculatedPadding = lineOverlapPadding;
            paint.setColor(context.getResources().getColor(R.color.tools_annot_edit_line_shadow));
            paint.setStrokeWidth(thickness);
        } else {
            calculatedPadding = lineOverlapPadding + padding;
            paint.setColor(context.getResources().getColor(R.color.tools_annot_edit_line_shadow_no_permission));
            paint.setPathEffect(dashPathEffect);
            paint.setStrokeWidth(thickness * 1.1f); // dash effect makes line look thinner, so increase the stroke width
        }
        canvas.drawRoundRect(
                new RectF(left - calculatedPadding + thickness / 2,
                        top - calculatedPadding + thickness / 2,
                        right + calculatedPadding - thickness / 2,
                        bottom + calculatedPadding - thickness / 2),
                cornerRad,
                cornerRad,
                paint
        );
    }

    public static void drawCtrlPtsLine(@NonNull Resources resources, @NonNull Canvas canvas,
            @NonNull Paint paint, @NonNull PointF pt1, @NonNull PointF pt2,
            float radius, boolean hasPermission) {
        float left = pt1.x;
        float bottom = pt1.y;
        float right = pt2.x;
        float top = pt2.y;

        paint.setColor(resources.getColor(R.color.tools_selection_control_point));
        paint.setStyle(Paint.Style.FILL);
        if (hasPermission) {
            canvas.drawCircle(left, bottom, radius, paint);
            canvas.drawCircle(right, top, radius, paint);
        }

        paint.setColor(resources.getColor(R.color.tools_selection_control_point_border));
        paint.setStyle(Paint.Style.STROKE);
        if (hasPermission) {
            canvas.drawCircle(left, bottom, radius, paint);
            canvas.drawCircle(right, top, radius, paint);
        }
    }

    public static void drawCtrlPts(@NonNull Resources resources, @NonNull Canvas canvas,
            @NonNull Paint paint, @NonNull PointF pt1, @NonNull PointF pt2,
            @NonNull PointF midH, @NonNull PointF midV,
            float radius, boolean hasPermission,
            boolean maintainAspectRatio) {
        float left = Math.min(pt1.x, pt2.x);
        float right = Math.max(pt1.x, pt2.x);
        float top = Math.min(pt1.y, pt2.y);
        float bottom = Math.max(pt1.y, pt2.y);

        float middle_x = midH.x;
        float middle_y = midV.y;

        // Control point fill color
        paint.setColor(resources.getColor(R.color.tools_selection_control_point));
        paint.setStyle(Paint.Style.FILL);
        if (hasPermission) {
            canvas.drawCircle(left, bottom, radius, paint);
            canvas.drawCircle(right, bottom, radius, paint);
            canvas.drawCircle(right, top, radius, paint);
            canvas.drawCircle(left, top, radius, paint);
        }
        // if maintain aspect ratio is false, draw middle control pts
        if (!maintainAspectRatio) {
            if (hasPermission) {
                canvas.drawCircle(middle_x, bottom, radius, paint);
                canvas.drawCircle(right, middle_y, radius, paint);
                canvas.drawCircle(middle_x, top, radius, paint);
                canvas.drawCircle(left, middle_y, radius, paint);
            }
        }

        // Control point border
        paint.setColor(resources.getColor(R.color.tools_selection_control_point_border));
        paint.setStyle(Paint.Style.STROKE);
        if (hasPermission) {
            canvas.drawCircle(left, bottom, radius, paint);
            canvas.drawCircle(right, bottom, radius, paint);
            canvas.drawCircle(right, top, radius, paint);
            canvas.drawCircle(left, top, radius, paint);
        }
        // if maintain aspect ratio is false, draw middle control pts
        if (!maintainAspectRatio) {
            if (hasPermission) {
                canvas.drawCircle(middle_x, bottom, radius, paint);
                canvas.drawCircle(right, middle_y, radius, paint);
                canvas.drawCircle(middle_x, top, radius, paint);
                canvas.drawCircle(left, middle_y, radius, paint);
            }
        }
    }

    public static void drawCtrlPtsAdvancedShape(@NonNull Resources resources, @NonNull Canvas canvas,
            @NonNull Paint paint, @NonNull PointF[] ctrlPts,
            float radius, boolean hasPermission,
            boolean skipEndPoint) {
        // Control point fill color
        paint.setColor(resources.getColor(R.color.tools_selection_control_point));
        paint.setStyle(Paint.Style.FILL);
        if (hasPermission) {
            for (int i = 0; i < ctrlPts.length; i++) {
                if (skipEndPoint && i == AnnotEditAdvancedShape.CALLOUT_END_POINT_INDEX) {
                    // for callout we want to skip the end point (3rd point)
                    continue;
                }
                PointF pt = ctrlPts[i];
                if (pt != null) {
                    canvas.drawCircle(pt.x, pt.y, radius, paint);
                }
            }
        }

        // Control point border
        paint.setColor(resources.getColor(R.color.tools_selection_control_point_border));
        paint.setStyle(Paint.Style.STROKE);
        if (hasPermission) {
            for (int i = 0; i < ctrlPts.length; i++) {
                if (skipEndPoint && i == AnnotEditAdvancedShape.CALLOUT_END_POINT_INDEX) {
                    // for callout we want to skip the end point (3rd point)
                    continue;
                }
                PointF pt = ctrlPts[i];
                if (pt != null) {
                    canvas.drawCircle(pt.x, pt.y, radius, paint);
                }
            }
        }
    }

    private static float xWithRotation(float x, float y, float width, float height, int degree) {
        double rad = Math.toRadians(degree);
        return (float) (x + height * Math.sin(rad) + width * Math.cos(rad));
    }

    private static float yWithRotation(float x, float y, float width, float height, int degree) {
        double rad = Math.toRadians(degree);
        return (float) (y + height * Math.cos(rad) - width * Math.sin(rad));
    }

    private static void drawDashedLine(@NonNull Canvas canvas, @NonNull Path path,
            float startX, float startY, float stopX, float stopY,
            @NonNull Paint paint) {
        path.moveTo(startX, startY);
        path.lineTo(stopX, stopY);

        canvas.drawPath(path, paint);
    }

    public static void drawGuideline(@NonNull Canvas canvas, @NonNull Path path,
            float startX, float startY, float stopX, float stopY,
            @NonNull Paint paint) {

        path.reset();
        drawDashedLine(canvas, path, startX, startY, stopX, stopY, paint);
    }

    public static void drawGuideline(@NonNull AnnotView.SnapMode snapMode, float extend, @NonNull Canvas canvas,
            @NonNull RectF bbox, @NonNull Path path, @NonNull Paint paint) {
        float centerX = bbox.centerX();
        float centerY = bbox.centerY();

        float left = bbox.left;
        float top = bbox.top;
        float right = bbox.right;
        float bottom = bbox.bottom;

        // extend line
        double len = MeasureUtils.getLineLength(left, bottom, right, top);
        double newLeft = left + (left - right) / len * extend;
        double newBottom = bottom + (bottom - top) / len * extend;
        double newRight = right + (right - left) / len * extend;
        double newTop = top + (top - bottom) / len * extend;

        path.reset();

        if (snapMode == AnnotView.SnapMode.HORIZONTAL) {
            drawDashedLine(canvas, path, centerX, top - extend, centerX, bottom + extend, paint);
        } else if (snapMode == AnnotView.SnapMode.VERTICAL) {
            drawDashedLine(canvas, path, left - extend, centerY, right + extend, centerY, paint);
        } else if (snapMode == AnnotView.SnapMode.ASPECT_RATIO_L) {
            drawDashedLine(canvas, path, (float) newLeft, (float) newBottom, (float) newRight, (float) newTop, paint);
        } else if (snapMode == AnnotView.SnapMode.ASPECT_RATIO_R) {
            drawDashedLine(canvas, path, (float) newLeft, (float) newTop, (float) newRight, (float) newBottom, paint);
        }
    }

    /**
     * Degree in counter-clockwise
     */
    public static void drawGuideline(int degree, float radius, @NonNull Canvas canvas,
            @NonNull RectF bbox, @NonNull Path path, @NonNull Paint paint) {
        float centerX = bbox.centerX();
        float centerY = bbox.centerY();
        float maxSize = Math.max(bbox.width(), bbox.height());
        float size = maxSize / 4 * 3;
        paint.setStyle(Paint.Style.FILL);
        canvas.drawCircle(centerX, centerY, radius, paint);
        paint.setStyle(Paint.Style.STROKE);
        path.reset();

        drawDashedLine(canvas, path, centerX, centerY, centerX + size, centerY, paint); // baseline exists for all angles
        if (degree == 90) {
            drawDashedLine(canvas, path, centerX, centerY - size, centerX, centerY, paint);
        } else if (degree == 180) {
            drawDashedLine(canvas, path, centerX - size, centerY, centerX, centerY, paint);
        } else if (degree == -90) {
            drawDashedLine(canvas, path, centerX, centerY, centerX, centerY + size, paint);
        }
        if (degree == 45 || degree == 135 || degree == 225 || degree == -45) {
            drawDashedLine(canvas, path, centerX, centerY,
                    xWithRotation(centerX, centerY, size, 0, degree),
                    yWithRotation(centerX, centerY, size, 0, degree),
                    paint);
        }
    }

    public static void drawInk(@NonNull PDFViewCtrl pdfViewCtrl, @NonNull Canvas canvas,
            @NonNull ArrayList<InkItem> inks,
            @Nullable Matrix transform,
            @Nullable PointF offset) {
        for (InkItem ink : inks) {
            ink.draw(canvas, pdfViewCtrl, transform, offset);
        }
    }

    public static void drawRectangle(@NonNull Canvas canvas,
            @NonNull PointF pt1, @NonNull PointF pt2,
            float thicknessDraw,
            int fillColor, int strokeColor,
            @NonNull Paint fillPaint, @NonNull Paint paint, PathEffect borderPathEffect) {
        float min_x = Math.min(pt1.x, pt2.x);
        float max_x = Math.max(pt1.x, pt2.x);
        float min_y = Math.min(pt1.y, pt2.y);
        float max_y = Math.max(pt1.y, pt2.y);

        // Android aligns in the middle of the line, while PDFNet aligns along the outer boundary;
        // so need to adjust the temporary shape drawn.
        float adjust = thicknessDraw / 2;

        if (fillColor != Color.TRANSPARENT) {
            canvas.drawRect(min_x + thicknessDraw, min_y + thicknessDraw,
                    max_x - thicknessDraw, max_y - thicknessDraw, fillPaint);
        }
        if (strokeColor != Color.TRANSPARENT) {
            paint.setPathEffect(borderPathEffect);
            canvas.drawRect(min_x + adjust, min_y + adjust,
                    max_x - adjust, max_y - adjust, paint);
        }
    }

    public static void drawCloudyRectangle(@NonNull PDFViewCtrl pdfViewCtrl,
            @NonNull int pageNumber,
            @NonNull Canvas canvas,
            @NonNull Path path,
            @NonNull PointF pt1,
            @NonNull PointF pt2,
            int fillColor, int strokeColor,
            @NonNull Paint fillPaint, @NonNull Paint paint, double borderIntensity) {

        ArrayList<PointF> canvasPoints = new ArrayList<>();
        canvasPoints.add(new PointF(pt1.x, pt1.y));
        canvasPoints.add(new PointF(pt1.x, pt2.y));
        canvasPoints.add(new PointF(pt2.x, pt2.y));
        canvasPoints.add(new PointF(pt2.x, pt1.y));

        drawCloudyRect(pdfViewCtrl, pageNumber, canvas, canvasPoints, path, paint, strokeColor, fillPaint, fillColor, borderIntensity);
    }

    public static void drawOval(@NonNull Canvas canvas,
            @NonNull PointF pt1, @NonNull PointF pt2,
            float thicknessDraw,
            @NonNull RectF oval,
            int fillColor, int strokeColor,
            @NonNull Paint fillPaint, @NonNull Paint paint,
            PathEffect borderPathEffect) {
        float min_x = Math.min(pt1.x, pt2.x);
        float max_x = Math.max(pt1.x, pt2.x);
        float min_y = Math.min(pt1.y, pt2.y);
        float max_y = Math.max(pt1.y, pt2.y);

        // Android aligns in the middle of a line, while PDFNet aligns along the outer boundary;
        // so need to adjust the temporary shape drawn.
        float adjust = thicknessDraw / 2;
        min_x += adjust;
        max_x -= adjust;
        min_y += adjust;
        max_y -= adjust;

        oval.set(min_x, min_y, max_x, max_y);
        if (fillColor != Color.TRANSPARENT) {
            canvas.drawArc(oval, 0, 360, false, fillPaint);
        }
        if (strokeColor != Color.TRANSPARENT) {
            paint.setPathEffect(borderPathEffect);
            canvas.drawArc(oval, 0, 360, false, paint);
        }
    }

    public static void drawLine(Canvas canvas, PointF pt1, PointF pt2,
            PointF startPt, PointF endPt,
            PointF spt1, PointF spt2, PointF spt3, PointF spt4,
            PointF ept1, PointF ept2, PointF ept3, PointF ept4,
            LineEndingStyle startStyle, LineEndingStyle endStyle,
            Path path, Paint paint, PathEffect linePathEffect,
            float thickness, double zoom) {
        path.reset();

        startPt.set(pt1.x, pt1.y);
        endPt.set(pt2.x, pt2.y);

        // draw the line ending style
        if (startStyle != LineEndingStyle.NONE) {
            LineCreate.calculateLineEndingStyle(startStyle, endPt, startPt, spt1, spt2, spt3, spt4, thickness, zoom);
            drawLineEndingStyle(startStyle, path, endPt, startPt, spt1, spt2, spt3, spt4, thickness, zoom);
        }
        if (endStyle != LineEndingStyle.NONE) {
            LineCreate.calculateLineEndingStyle(endStyle, startPt, endPt, ept1, ept2, ept3, ept4, thickness, zoom);
            drawLineEndingStyle(endStyle, path, startPt, endPt, ept1, ept2, ept3, ept4, thickness, zoom);
        }

        // draw the line - it must run after line ending calc
        // because the end point will update with call by reference
        path.moveTo(startPt.x, startPt.y);
        path.lineTo(endPt.x, endPt.y);
        paint.setPathEffect(linePathEffect);
        canvas.drawPath(path, paint);
    }

    static void drawLineEndingStyle(LineEndingStyle lineEndingStyle, Path path,
            PointF startPoint, PointF endPoint, PointF pt1, PointF pt2, PointF pt3, PointF pt4,
            float thickness, double zoom) {
        if (lineEndingStyle != null) {
            LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
            Vec2 originalEnd = new Vec2(endPoint.x, endPoint.y);
            switch (lineEndingStyle) {
                case BUTT:
                case SLASH:
                    path.moveTo(pt1.x, pt1.y);
                    path.lineTo(pt2.x, pt2.y);
                    break;
                case DIAMOND:
                    endPoint.set(Vec2.subtract(originalEnd, Vec2.multiply(cv.line, cv.sqrt2 * cv.len)).toPointF());
                    path.moveTo(endPoint.x, endPoint.y);
                    path.lineTo(pt1.x, pt1.y);
                    path.lineTo(pt2.x, pt2.y);
                    path.lineTo(pt3.x, pt3.y);
                    path.lineTo(endPoint.x, endPoint.y);
                    break;
                case CIRCLE:
                    float len = (float) cv.len * 0.6f;
                    endPoint.set(Vec2.subtract(originalEnd, Vec2.multiply(cv.line, len * 2)).toPointF());
                    //down arc
                    path.moveTo(endPoint.x, endPoint.y);
                    final RectF ovalRect1 = new RectF(-len, -len, len, len);
                    ovalRect1.offset(pt1.x, pt1.y);
                    path.arcTo(ovalRect1, 0, 180, true);
                    //top arc
                    final RectF ovalRect2 = new RectF(-len, -len, len, len);
                    ovalRect2.offset(pt1.x, pt1.y);
                    path.arcTo(ovalRect2, 0, -180, true);
                    break;
                case OPEN_ARROW:
                    path.moveTo(pt1.x, pt1.y);
                    path.lineTo(endPoint.x, endPoint.y);
                    path.lineTo(pt2.x, pt2.y);
                    break;
                case CLOSED_ARROW:
                    endPoint.set(Vec2.subtract(originalEnd, Vec2.multiply(cv.line, cv.sqrt3 * cv.len * 0.5)).toPointF());
                    path.moveTo(endPoint.x, endPoint.y);
                    path.lineTo(pt1.x, pt1.y);
                    path.lineTo(originalEnd.toPointF().x, originalEnd.toPointF().y);
                    path.lineTo(pt2.x, pt2.y);
                    path.lineTo(endPoint.x, endPoint.y);
                    break;
                case R_OPEN_ARROW:
                    endPoint.set(Vec2.subtract(originalEnd, Vec2.multiply(cv.line, cv.sqrt3 * cv.len * 0.5)).toPointF());
                    path.moveTo(pt1.x, pt1.y);
                    path.lineTo(endPoint.x, endPoint.y);
                    path.lineTo(pt2.x, pt2.y);
                    break;
                case R_CLOSED_ARROW:
                    endPoint.set(Vec2.subtract(originalEnd, Vec2.multiply(cv.line, cv.sqrt3 * cv.len * 0.5)).toPointF());
                    path.moveTo(endPoint.x, endPoint.y);
                    path.lineTo(pt1.x, pt1.y);
                    path.lineTo(pt2.x, pt2.y);
                    path.lineTo(endPoint.x, endPoint.y);
                    break;
                case SQUARE:
                    endPoint.set(Vec2.subtract(originalEnd, Vec2.multiply(cv.line, cv.len * 0.5)).toPointF());
                    path.moveTo(endPoint.x, endPoint.y);
                    path.lineTo(pt1.x, pt1.y);
                    path.lineTo(pt2.x, pt2.y);
                    path.lineTo(pt3.x, pt3.y);
                    path.lineTo(pt4.x, pt4.y);
                    path.lineTo(endPoint.x, endPoint.y);
                    break;
            }
        }
    }

    public static void drawArrow(Canvas canvas, PointF startPoint, PointF endPoint,
            PointF ept1, PointF ept2,
            Path path, Paint paint, PathEffect linePathEffect) {
        path.reset();
        // draw the line
        path.moveTo(startPoint.x, startPoint.y);
        path.lineTo(endPoint.x, endPoint.y);

        // draw the arrow
        path.moveTo(ept1.x, ept1.y);
        path.lineTo(endPoint.x, endPoint.y);
        path.lineTo(ept2.x, ept2.y);

        paint.setPathEffect(linePathEffect);
        canvas.drawPath(path, paint);
    }

    public static void calcOpenArrow(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        Vec2 temp1 = Vec2.multiply(Vec2.subtract(cv.line90, Vec2.multiply(cv.line, cv.sqrt3)), cv.len * 0.5);
        pt1.set(Vec2.add(end, temp1).toPointF());
        Vec2 temp2 = Vec2.multiply(Vec2.add(cv.line90, Vec2.multiply(cv.line, cv.sqrt3)), cv.len * 0.5);
        pt2.set(Vec2.subtract(end, temp2).toPointF());
    }

    public static void calcClosedArrow(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        pt1.set(Vec2.add(end, Vec2.multiply(Vec2.subtract(cv.line90, Vec2.multiply(cv.line, cv.sqrt3)), cv.len * 0.5)).toPointF());
        pt2.set(Vec2.subtract(end, Vec2.multiply(Vec2.add(cv.line90, Vec2.multiply(cv.line, cv.sqrt3)), cv.len * 0.5)).toPointF());
    }

    public static void calcDiamond(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, @NonNull PointF pt3, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        Vec2 current = Vec2.subtract(end, Vec2.multiply(cv.line, cv.sqrt2 * cv.len));
        current = Vec2.add(current, Vec2.multiply(Vec2.add(cv.line, cv.line90), cv.len * cv.sqrt2 * 0.5));
        pt1.set(current.toPointF());
        current = Vec2.add(current, Vec2.multiply(Vec2.subtract(cv.line, cv.line90), cv.len * cv.sqrt2 * 0.5));
        pt2.set(current.toPointF());
        current = Vec2.subtract(current, Vec2.multiply(Vec2.add(cv.line, cv.line90), cv.len * cv.sqrt2 * 0.5));
        pt3.set(current.toPointF());
    }

    public static void calcCircle(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        float len = (float) cv.len * 0.6f;
        Vec2 current = Vec2.subtract(end, Vec2.multiply(cv.line, len * 2));
        current = Vec2.add(current, Vec2.multiply(cv.line, len));
        pt1.set(current.toPointF());
    }

    public static void calcROpenArrow(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        pt1.set(Vec2.add(end, Vec2.multiply(cv.line90, cv.len * 0.5)).toPointF());
        pt2.set(Vec2.subtract(end, Vec2.multiply(cv.line90, cv.len * 0.5)).toPointF());
    }

    public static void calcRClosedArrow(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        Vec2 current = Vec2.subtract(end, Vec2.multiply(cv.line, cv.sqrt3 * cv.len * 0.5));
        current = Vec2.add(current, Vec2.multiply(Vec2.add(Vec2.multiply(cv.line, cv.sqrt3), cv.line90), cv.len * 0.5));
        pt1.set(current.toPointF());
        current = Vec2.subtract(current, Vec2.multiply(cv.line90, cv.len));
        pt2.set(current.toPointF());
    }

    public static void calcSlash(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        Vec2 current = Vec2.add(end, Vec2.multiply(Vec2.subtract(Vec2.multiply(cv.line90, cv.sqrt3), Vec2.multiply(cv.line, -1)), cv.len * ((double) 1 / 3)));
        pt1.set(current.toPointF());
        current = Vec2.subtract(current, Vec2.multiply(Vec2.subtract(Vec2.multiply(cv.line90, cv.sqrt3), Vec2.multiply(cv.line, -1)), cv.len * ((double) 2 / 3)));
        pt2.set(current.toPointF());
    }

    public static void calcSquare(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, @NonNull PointF pt3, @NonNull PointF pt4,
            float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        Vec2 current = Vec2.subtract(end, Vec2.multiply(cv.line, cv.len * 0.5));
        current = Vec2.add(current, Vec2.multiply(cv.line90, cv.len * 0.5));
        pt1.set(current.toPointF());
        current = Vec2.add(current, Vec2.multiply(cv.line, cv.len));
        pt2.set(current.toPointF());
        current = Vec2.subtract(current, Vec2.multiply(cv.line90, cv.len));
        pt3.set(current.toPointF());
        current = Vec2.subtract(current, Vec2.multiply(cv.line, cv.len));
        pt4.set(current.toPointF());
    }

    private static double getLenOfLine(@NonNull PointF pt1, @NonNull PointF pt2,
            float thickness, double zoom, boolean halfALine) {
        double len = 5 * thickness + 2;
        len = len * zoom;
        double constant = 0.35;
        if (halfALine) {
            constant = 0.7;
        }
        len = Math.min(distance(pt1, pt2) * constant, len);
        return len;
    }

    public static double distance(@NonNull PointF p1, @NonNull PointF p2) {
        double w = p1.x - p2.x;
        double h = p1.y - p2.y;
        return Math.sqrt(w * w + h * h);
    }

    public static void drawRuler(Canvas canvas, PointF pt1, PointF pt2,
            PointF startPt, PointF endPt,
            PointF spt1, PointF spt2, PointF spt3, PointF spt4,
            PointF ept1, PointF ept2, PointF ept3, PointF ept4,
            LineEndingStyle startStyle, LineEndingStyle endStyle,
            String text,
            Path path, Paint paint, PathEffect linePathEffect,
            float thickness, double zoom) {
        drawLine(canvas, pt1, pt2, startPt, endPt, spt1, spt2, spt3, spt4,
                ept1, ept2, ept3, ept4, startStyle, endStyle,
                path, paint, linePathEffect, thickness, zoom);

        // text
        path.reset();
        path.moveTo(startPt.x, startPt.y);
        path.lineTo(endPt.x, endPt.y);

        drawTextOnRuler(canvas, text, path, paint, zoom);
    }

    private static void drawTextOnRuler(Canvas canvas, String text, Path path, Paint paint, double zoom) {
        // Store pain parameters to reset later
        float width = paint.getStrokeWidth();
        PathEffect pathEffect = paint.getPathEffect();
        Paint.Style style = paint.getStyle();
        paint.setStrokeWidth(0);
        paint.setStyle(Paint.Style.FILL_AND_STROKE);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize((float) (12 * zoom));
        paint.setPathEffect(null);

        double yOffset = -4 * zoom;
        canvas.drawTextOnPath(text, path, 0, (float) yOffset, paint);

        // Reset the paint parameters
        paint.setStrokeWidth(width);
        paint.setStyle(style);
        paint.setPathEffect(pathEffect);
    }

    public static void calButt(@NonNull PointF startPoint, @NonNull PointF endPoint,
            @NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
        LineEndingCalculationVariables cv = new LineEndingCalculationVariables(startPoint, endPoint, thickness, zoom);
        Vec2 temp = Vec2.multiply(cv.line90, cv.len * 0.5);
        Vec2 end = new Vec2(endPoint.x, endPoint.y);
        pt1.set(Vec2.subtract(end, temp).toPointF());
        pt2.set(Vec2.add(end, temp).toPointF());
    }

    public static PointF midpoint(@NonNull PointF pt1, @NonNull PointF pt2) {
        return new PointF((pt1.x + pt2.x) / 2, (pt1.y + pt2.y) / 2);
    }

    public static void drawPolyline(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @NonNull Canvas canvas,
            @NonNull ArrayList<PointF> canvasPoints,
            PointF pt1, PointF pt2, PointF pt3, PointF pt4, // 4 points for 2 line segments
            PointF spt1, PointF spt2, PointF spt3, PointF spt4,
            PointF ept1, PointF ept2, PointF ept3, PointF ept4,
            LineEndingStyle startStyle, LineEndingStyle endStyle,
            @NonNull Path path,
            @NonNull Paint paint, int strokeColor, PathEffect linePathEffect,
            float thickness, double zoom) {
        if (canvasPoints.size() < 2) {
            return;
        }
        path.reset();

        pt1.set(canvasPoints.get(0));
        pt2.set(canvasPoints.get(1));
        pt3.set(canvasPoints.get(canvasPoints.size() - 2));
        pt4.set(canvasPoints.get(canvasPoints.size() - 1));

        // draw the line ending style
        if (startStyle != LineEndingStyle.NONE) {
            LineCreate.calculateLineEndingStyle(startStyle, pt2, pt1, spt1, spt2, spt3, spt4, thickness, zoom);
            drawLineEndingStyle(startStyle, path, pt2, pt1, spt1, spt2, spt3, spt4, thickness, zoom);
        }
        if (endStyle != LineEndingStyle.NONE) {
            LineCreate.calculateLineEndingStyle(endStyle, pt3, pt4, ept1, ept2, ept3, ept4, thickness, zoom);
            drawLineEndingStyle(endStyle, path, pt3, pt4, ept1, ept2, ept3, ept4, thickness, zoom);
        }

        for (int i = 0; i < canvasPoints.size(); i++) {
            PointF point = canvasPoints.get(i);
            if (i == 0) {
                // first point
                path.moveTo(pt1.x, pt1.y);
            } else if (i == canvasPoints.size() - 1) {
                // last point
                path.lineTo(pt4.x, pt4.y);
            } else {
                path.lineTo(point.x, point.y);
            }
        }

        paint.setPathEffect(linePathEffect);

        if (strokeColor != Color.TRANSPARENT) {
            if (pdfViewCtrl.isMaintainZoomEnabled()) {
                canvas.save();
                try {
                    canvas.translate(0, -pdfViewCtrl.getScrollYOffsetInTools(pageNum));
                    canvas.drawPath(path, paint);
                } finally {
                    canvas.restore();
                }
            } else {
                canvas.drawPath(path, paint);
            }
        }
    }

    public static void drawPolygon(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @NonNull Canvas canvas,
            @NonNull ArrayList<PointF> canvasPoints, @NonNull Path path,
            @NonNull Paint paint, int strokeColor,
            @NonNull Paint fillPaint, int fillColor, PathEffect borderPathEffect) {
        drawPolygon(pdfViewCtrl, pageNum, canvas, canvasPoints, path, paint, strokeColor, fillPaint, fillColor, null, borderPathEffect);
    }

    public static void drawPolygon(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @NonNull Canvas canvas,
            @NonNull ArrayList<PointF> canvasPoints, @NonNull Path path,
            @NonNull Paint paint, int strokeColor,
            @NonNull Paint fillPaint, int fillColor, @Nullable Matrix transform, PathEffect borderPathEffect) {
        path.reset();
        PointF startPoint = null;
        for (PointF point : canvasPoints) {
            if (startPoint != null) {
                path.lineTo(point.x, point.y);
            } else {
                startPoint = point;
                path.moveTo(point.x, point.y);
            }
        }
        if (startPoint == null) {
            return;
        }
        path.lineTo(startPoint.x, startPoint.y);

        paint.setPathEffect(borderPathEffect);

        if (pdfViewCtrl.isMaintainZoomEnabled()) {
            canvas.save();
            try {
                canvas.translate(0, -pdfViewCtrl.getScrollYOffsetInTools(pageNum));
                drawPolygonHelper(canvas, path, paint, strokeColor, fillPaint, fillColor, transform);
            } finally {
                canvas.restore();
            }
        } else {
            drawPolygonHelper(canvas, path, paint, strokeColor, fillPaint, fillColor, transform);
        }
    }

    public static void drawCloud(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @NonNull Canvas canvas,
            @NonNull ArrayList<PointF> canvasPoints, @NonNull Path path,
            @NonNull Paint paint, int strokeColor,
            @NonNull Paint fillPaint, int fillColor, double borderIntensity) {
        ArrayList<PointF> poly = CloudCreate.getClosedPoly(canvasPoints);
        int size = poly.size();
        if (size < 3) {
            return;
        }

        final double SAME_VERTEX_TH = 1.0 / 8192.0;
        if (borderIntensity < 0.1) {
            borderIntensity = CloudCreate.BORDER_INTENSITY;
        }
        borderIntensity *= pdfViewCtrl.getZoom();
        final boolean clockwise = CloudCreate.IsPolyWrapClockwise(poly);
        final double sweepDirection = clockwise ? -1.0 : 1.0;
        final double maxCloudSize = 8 * borderIntensity;

        double lastCloudSize = maxCloudSize;
        double firstCloudSize = maxCloudSize;
        double edgeDegrees = 0.0;
        PointF firstPos = poly.get(0);
        PointF lastEdge = CloudCreate.subtract(poly.get(0), poly.get(size - 2));
        boolean useLargeFirstArc = true;
        boolean hasFirstPoint = false;
        path.reset();
        float startX = 0, startY = 0;

        for (int i = 0; i < size - 1; ++i) {
            PointF pos = poly.get(i);
            PointF edge = CloudCreate.subtract(poly.get(i + 1), pos);
            double length = edge.length();
            // avoid division by 0 from duplicated points.
            if (length <= SAME_VERTEX_TH) {
                continue;
            }

            // split the edge into some integral number of clouds
            PointF direction = CloudCreate.divide(edge, length);
            int numClouds = (int) Math.max(Math.floor(length / maxCloudSize), 1);
            double cloudSize = length / numClouds;
            double edgeAngle = Math.atan2(direction.y, direction.x); // angle from x-axis

            // back start position out to before the vertex
            // as we're going to increment before using it
            pos = CloudCreate.subtract(pos, CloudCreate.multiply(direction, cloudSize * .5));

            // which direction are we turning on this vertex?
            double cross = CloudCreate.cross(lastEdge, edge);

            int c = 0;
            if (!hasFirstPoint) {
                // skip the first iteration for the first leg (we'll complete it at the end)
                ++c;
                firstCloudSize = cloudSize;
                useLargeFirstArc = (cross * sweepDirection) < 0;
                pos = CloudCreate.add(pos, CloudCreate.multiply(direction, cloudSize));
                firstPos = pos;
                // start the curve
                path.moveTo(firstPos.x, firstPos.y);
                startX = firstPos.x;
                startY = firstPos.y;
                hasFirstPoint = true;
            }
            // for the first iteration, combine the radius with the previous edge
            double radius = (lastCloudSize + cloudSize) * 0.25;
            for (; c < numClouds; ++c) {
                if (c == 1) {
                    // on the second iteration on, we can use values exclusive to this edge
                    edgeDegrees = CloudCreate.toDegreesMod360(edgeAngle);
                    radius = cloudSize * 0.5;
                }
                pos = CloudCreate.add(pos, CloudCreate.multiply(direction, cloudSize));
                boolean useLargeArc = (c == 0 && (cross * sweepDirection) < 0);
                PointF point = CloudCreate.arcTo(path, startX, startY, radius, radius, edgeDegrees, useLargeArc, clockwise, pos.x, pos.y);
                startX = point.x;
                startY = point.y;
            }
            edgeDegrees = CloudCreate.toDegreesMod360(edgeAngle);
            lastEdge = edge;
            lastCloudSize = cloudSize;
        }
        if (!hasFirstPoint) {
            path.moveTo(firstPos.x, firstPos.y);
            startX = firstPos.x;
            startY = firstPos.y;
        }
        double closingRadius = (firstCloudSize + lastCloudSize) * 0.25;
        // now we close the poly, using the values we saved on the first vertex.
        CloudCreate.arcTo(path, startX, startY, closingRadius, closingRadius, edgeDegrees,
                useLargeFirstArc, clockwise, firstPos.x, firstPos.y);

        // reset dash effect
        paint.setPathEffect(null);

        if (pdfViewCtrl.isMaintainZoomEnabled()) {
            canvas.save();
            try {
                canvas.translate(0, -pdfViewCtrl.getScrollYOffsetInTools(pageNum));
                drawPolygonHelper(canvas, path, paint, strokeColor, fillPaint, fillColor, null);
            } finally {
                canvas.restore();
            }
        } else {
            drawPolygonHelper(canvas, path, paint, strokeColor, fillPaint, fillColor, null);
        }
    }

    public static void drawCloudyRect(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @NonNull Canvas canvas,
            @NonNull ArrayList<PointF> canvasPoints, @NonNull Path path,
            @NonNull Paint paint, int strokeColor,
            @NonNull Paint fillPaint, int fillColor, double borderIntensity) {

        drawCloud(pdfViewCtrl, pageNum, canvas, canvasPoints, path, paint, strokeColor, fillPaint, fillColor, borderIntensity);
    }

    public static void drawCloudyPolygon(@NonNull PDFViewCtrl pdfViewCtrl, int pageNum, @NonNull Canvas canvas,
            @NonNull ArrayList<PointF> canvasPoints, @NonNull Path path,
            @NonNull Paint paint, int strokeColor,
            @NonNull Paint fillPaint, int fillColor, double borderIntensity) {

        drawCloud(pdfViewCtrl, pageNum, canvas, canvasPoints, path, paint, strokeColor, fillPaint, fillColor, borderIntensity);
    }

    private static void drawPolygonHelper(@NonNull Canvas canvas, @NonNull Path path,
            @NonNull Paint paint, int strokeColor,
            @NonNull Paint fillPaint, int fillColor, @Nullable Matrix transform) {
        Path drawPath = path;
        if (transform != null) {
            drawPath = new Path();
            drawPath.addPath(path, transform);
        }
        if (fillColor != Color.TRANSPARENT) {
            canvas.drawPath(drawPath, fillPaint);
        }
        if (strokeColor != Color.TRANSPARENT) {
            canvas.drawPath(drawPath, paint);
        }
    }

    public static DashPathEffect getAnnotationPreviewDashPathEffect(Context context) {
        return new DashPathEffect(new float[]{
                Utils.convDp2Pix(context, 5f),
                Utils.convDp2Pix(context, 7f)
        }, 0);
    }

    public static DashPathEffect getDashPathEffect(Context context) {
        return new DashPathEffect(new float[]{
                Utils.convDp2Pix(context, 3f),
                Utils.convDp2Pix(context, 1.5f)
        }, 0);
    }

    public static double[] getShapesDashIntervals() {
        return new double[]{4.5, 2.5};
    }

    private static class LineEndingCalculationVariables {
        public double sqrt2;
        public double sqrt3;
        public double len;
        public Vec2 line;
        public Vec2 line90;

        LineEndingCalculationVariables(@NonNull PointF pt1, @NonNull PointF pt2, float thickness, double zoom) {
            sqrt2 = Math.sqrt(2.0);
            sqrt3 = Math.sqrt(3.0);
            len = getLenOfLine(pt1, pt2, thickness, zoom, true);
            PointF midPt = midpoint(pt1, pt2);
            Vec2 mid = new Vec2(midPt.x, midPt.y);
            Vec2 end = new Vec2(pt2.x, pt2.y);
            Vec2 diff = Vec2.subtract(end, mid);
            double lineLen = diff.length();
            double max = Math.max(lineLen, 1.0 / 72.0);
            line = Vec2.multiply(diff, 1.0 / max);
            line90 = line.getPerp();
        }
    }
}
