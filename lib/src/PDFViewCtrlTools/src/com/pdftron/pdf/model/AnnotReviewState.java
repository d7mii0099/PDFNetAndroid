package com.pdftron.pdf.model;

import android.util.SparseArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.utils.AnnotUtils;

public enum AnnotReviewState {

    ACCEPTED(0),
    REJECTED(1),
    CANCELLED(2),
    COMPLETED(3),
    NONE(4);

    private int value;
    private static SparseArray<AnnotReviewState> map = new SparseArray<>(5);

    AnnotReviewState(final int value) {
        this.value = value;
    }

    static {
        for (AnnotReviewState reviewState : AnnotReviewState.values()) {
            map.put(reviewState.value, reviewState);
        }
    }

    public int getValue() {
        return this.value;
    }

    @Nullable
    public static AnnotReviewState valueOf(int reviewState) {
        if (reviewState >= 0 && reviewState <= 4) {
            return map.get(reviewState);
        }
        return null;
    }

    @Nullable
    public static AnnotReviewState from(@NonNull String reviewState) {
        if (AnnotUtils.Key_StateNone.equals(reviewState)) {
            return NONE;
        } else if (AnnotUtils.Key_StateAccepted.equals(reviewState)) {
            return ACCEPTED;
        } else if (AnnotUtils.Key_StateRejected.equals(reviewState)) {
            return REJECTED;
        } else if (AnnotUtils.Key_StateCancelled.equals(reviewState)) {
            return CANCELLED;
        } else if (AnnotUtils.Key_StateCompleted.equals(reviewState)) {
            return COMPLETED;
        }
        return null;
    }
}
