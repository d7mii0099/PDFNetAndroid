package com.pdftron.pdf.widget;

import android.graphics.PointF;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.RotateInfo;
import com.pdftron.pdf.utils.Utils;

public class RotationImpl {

    private final AnnotViewImpl mAnnotViewImpl;

    public float mRotDegree;
    public float mRotDegreeSave;
    public boolean mRotating;
    public boolean mRotated;
    public Integer mSnapDegree;

    public RotationImpl(@NonNull AnnotViewImpl annotViewImpl) {
        mAnnotViewImpl = annotViewImpl;
        // stamp rotation always starts with 0 regardless of current annot rotation
        // free text starts with current annot rotation
        if (annotViewImpl.mAnnotStyle.getAnnotType() == Annot.e_FreeText) {
            mRotDegreeSave = annotViewImpl.mAnnotUIRotation;
        }
    }

    public RotateInfo handleRotation(PointF downPt, PointF movePt, boolean done) {
        mRotating = !done;
        mRotated = true;
        PointF pivot = center();

        mRotDegree = (float) Utils.angleBetweenTwoPointsWithPivot(downPt.x, downPt.y, movePt.x, movePt.y, pivot.x, pivot.y);
        if (done) {
            mRotDegreeSave += mRotDegree;
        }
        return new RotateInfo(-mRotDegree, pivot);
    }

    public void snapToDegree(@Nullable Integer degree, float startDegree) {
        mSnapDegree = degree;
        if (mSnapDegree != null) {
            mRotDegree = -(mSnapDegree - startDegree); // clockwise
        }
    }

    public PointF center() {
        return new PointF(mAnnotViewImpl.mAnnotRectF.centerX(), mAnnotViewImpl.mAnnotRectF.centerY());
    }
}
