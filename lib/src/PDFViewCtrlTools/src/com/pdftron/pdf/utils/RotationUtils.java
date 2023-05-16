package com.pdftron.pdf.utils;

import android.graphics.RectF;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Rect;

public class RotationUtils {

    public static double getRotationAngleInRadiansByDegrees(int angleInDegrees) {
        return angleInDegrees * Math.PI / 180f;
    }

    public static boolean isDivisibleByPiOverFour(double angleInRadians) {
        if (angleInRadians == 0) {
            return false;
        }

        double piOverFour = Math.PI / 4;
        double floatingPointTolerance = 0.000000001;

        boolean divisibleCheck = angleInRadians % piOverFour == 0;
        boolean smallRemainderDivisibleCheck = angleInRadians % piOverFour < floatingPointTolerance;
        boolean largeRemainderDivisibleCheck = Math.abs((angleInRadians % piOverFour) - piOverFour) < floatingPointTolerance;

        return (divisibleCheck || smallRemainderDivisibleCheck || largeRemainderDivisibleCheck);
    }

    public static Rect getUnrotatedDimensionsFromBBox(Rect rect, int angleInDegree) throws PDFNetException {
        double angleInRadian = getRotationAngleInRadiansByDegrees(angleInDegree);
        rect.normalize();
        double boundingBoxX = rect.getX1();
        double boundingBoxY = rect.getY2();
        double boundingBoxWidth = rect.getWidth();
        double boundingBoxHeight = rect.getHeight();

        // Preventing scenarios the denominators below would be 0
        if (isDivisibleByPiOverFour(angleInRadian)) {
            angleInRadian += 0.01;
        }

        double quadrant = Math.floor(angleInRadian / (Math.PI / 2)) % 4;
        angleInRadian = normalizeRadian(angleInRadian, quadrant);

        double height = getHeight(boundingBoxWidth, boundingBoxHeight, angleInRadian);
        double width = getWidth(boundingBoxWidth, height, angleInRadian);
        double x = boundingBoxX + (boundingBoxWidth - Math.abs(width)) / 2;
        double y = boundingBoxY - (boundingBoxHeight - Math.abs(height)) / 2;

        if (quadrant == 2) {
            x -= width;
            y += height;
        }

        return new Rect(x, y, x + width, y - height);
    }

    public static RectF getUnrotatedDimensionsFromBBoxRectF(Rect rect, int angleInDegree) throws PDFNetException {
        Rect unrotated = getUnrotatedDimensionsFromBBox(rect, angleInDegree);
        return new RectF((float) Math.min(unrotated.getX1(), unrotated.getX2()),
                (float) Math.min(unrotated.getY1(), unrotated.getY2()),
                (float) Math.max(unrotated.getX1(), unrotated.getX2()),
                (float) Math.max(unrotated.getY1(), unrotated.getY2()));
    }

    public static boolean shouldMaintainAspectRatio(int angleInDegree) {
        if (angleInDegree == 0 || angleInDegree == 90 ||
                angleInDegree == 180 || angleInDegree == 270) {
            return false;
        }
        return true;
    }

    private static double normalizeRadian(double angleInRadian, double quadrant) {
        double normalizedAngle = angleInRadian % (2 * Math.PI);

        if (quadrant == 1) {
            angleInRadian = normalizedAngle - 2 * (normalizedAngle - Math.PI / 2);
        } else if (quadrant == 3) {
            angleInRadian = normalizedAngle - 2 * (normalizedAngle - Math.PI);
        }
        return angleInRadian;
    }

    private static double getHeight(double boundingBoxWidth, double boundingBoxHeight, double angleInRadian) {
        return (boundingBoxWidth * Math.tan(angleInRadian) - boundingBoxHeight) / (Math.tan(angleInRadian) * Math.sin(angleInRadian) - Math.cos(angleInRadian));
    }

    private static double getWidth(double boundingBoxWidth, double height, double angleInRadian) {
        return (boundingBoxWidth - height * Math.sin(angleInRadian)) / Math.cos(angleInRadian);
    }
}
