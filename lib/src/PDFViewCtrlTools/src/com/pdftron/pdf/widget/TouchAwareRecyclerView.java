package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

public class TouchAwareRecyclerView extends RecyclerView {

    private float mTouchX;
    private float mTouchY;

    public TouchAwareRecyclerView(@NonNull Context context) {
        super(context);
    }

    public TouchAwareRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public TouchAwareRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public float getTouchX() {
        return mTouchX;
    }

    public float getTouchY() {
        return mTouchY;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mTouchX = event.getRawX();
        mTouchY = event.getRawY();

        return super.onTouchEvent(event);
    }
}
