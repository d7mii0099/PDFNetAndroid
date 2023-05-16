package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;

import com.pdftron.pdf.tools.R;

public class SelectionHandleView extends RotateHandleView {

    public SelectionHandleView(Context context) {
        this(context, null);
    }

    public SelectionHandleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SelectionHandleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_selection_handle, this);
        mFab = findViewById(R.id.fab);

        setCustomSize(R.dimen.selection_widget_size);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
