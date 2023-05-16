package com.pdftron.pdf.dialog.annotlist;

import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.controls.AnnotationDialogFragment;
import com.pdftron.pdf.utils.AnnotUtils;

import java.util.Comparator;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class AnnotationListSorter extends BaseAnnotationListSorter<AnnotationDialogFragment.AnnotationInfo> {

    protected final Comparator<AnnotationDialogFragment.AnnotationInfo> mTopToBottomComparator =
            new Comparator<AnnotationDialogFragment.AnnotationInfo>() {
                @Override
                public int compare(AnnotationDialogFragment.AnnotationInfo thisObj, AnnotationDialogFragment.AnnotationInfo thatObj) {
                    return compareYPosition(thisObj, thatObj);
                }
            };

    protected final Comparator<AnnotationDialogFragment.AnnotationInfo> mDateComparator =
            new Comparator<AnnotationDialogFragment.AnnotationInfo>() {
                @Override
                public int compare(AnnotationDialogFragment.AnnotationInfo thisObj, AnnotationDialogFragment.AnnotationInfo thatObj) {
                    return compareCreationDate(thisObj, thatObj);
                }
            };

    public AnnotationListSorter(@NonNull BaseAnnotationSortOrder sortOrder) {
        super(sortOrder);
    }

    @NonNull
    @Override
    public Comparator<AnnotationDialogFragment.AnnotationInfo> getComparator() {
        BaseAnnotationSortOrder value = mSortOrder.getValue();
        if (value != null) {
            if (value instanceof AnnotationListSortOrder) {
                switch ((AnnotationListSortOrder) value) {
                    case DATE_ASCENDING:
                        return mDateComparator;
                    case POSITION_ASCENDING:
                        return mTopToBottomComparator;
                }
            }
        }
        return mDateComparator; // default we sort by descending date
    }

    public static int compareDate(AnnotationDialogFragment.AnnotationInfo thisObj,
            AnnotationDialogFragment.AnnotationInfo thatObj) {

        return AnnotUtils.compareDate(thisObj.getAnnotation(), thatObj.getAnnotation());
    }

    public static int compareCreationDate(AnnotationDialogFragment.AnnotationInfo thisObj,
            AnnotationDialogFragment.AnnotationInfo thatObj) {

        return AnnotUtils.compareCreationDate(thisObj.getAnnotation(), thatObj.getAnnotation());
    }

    public static int compareYPosition(AnnotationDialogFragment.AnnotationInfo thisObj,
            AnnotationDialogFragment.AnnotationInfo thatObj) {
        double thisY2 = thisObj.getY2();
        double thatY2 = thatObj.getY2();
        return Double.compare(thatY2, thisY2); // note reversed o1 and o2
    }

    public static class Factory implements ViewModelProvider.Factory {
        private BaseAnnotationSortOrder mSortOrder;

        public Factory(BaseAnnotationSortOrder sortOrder) {
            mSortOrder = sortOrder;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            if (modelClass.isAssignableFrom(AnnotationListSorter.class)) {
                return (T) new AnnotationListSorter(mSortOrder);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}
