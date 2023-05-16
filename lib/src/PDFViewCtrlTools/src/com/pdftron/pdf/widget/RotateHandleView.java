package com.pdftron.pdf.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.DimenRes;
import androidx.annotation.DrawableRes;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.pdftron.pdf.tools.R;

public class RotateHandleView extends LinearLayout {

    protected ImageView mFab;

    public interface RotateHandleViewListener {
        void onDown(float rawX, float rawY);

        void onMove(float rawX, float rawY);

        void onUp(float rawX, float rawY, float x, float y);
    }

    private RotateHandleViewListener mListener;

    float mDX, mDY;

    public RotateHandleView(Context context) {
        this(context, null);
    }

    public RotateHandleView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RotateHandleView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init();
    }

    protected void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.view_rotate_handle, this);
        mFab = findViewById(R.id.rotate_fab);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        final LayoutParams lp = (LayoutParams) mFab.getLayoutParams();
        if (mFab.getMeasuredWidth() == 0 || mFab.getMeasuredHeight() == 0) {
            mFab.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        }
        final int width = mFab.getMeasuredWidth();
        final int height = mFab.getMeasuredHeight();
        mFab.layout(lp.leftMargin, lp.topMargin, lp.leftMargin + width, lp.topMargin + height);
    }

    public void setListener(RotateHandleViewListener listener) {
        mListener = listener;
    }

    public void setImageResource(@DrawableRes int resId) {
        mFab.setImageResource(resId);
    }

    public void setCustomSize(@DimenRes int sizeRes) {
        if (mFab instanceof FloatingActionButton) {
            int size = getResources().getDimensionPixelSize(sizeRes);
            ((FloatingActionButton) mFab).setCustomSize(size);
        } else {
            // remove padding for the image button for pre-lollipop
            mFab.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDX = getX() - event.getRawX();
                mDY = getY() - event.getRawY();

                if (mListener != null) {
                    mListener.onDown(event.getRawX(), event.getRawY());
                }
                break;
            case MotionEvent.ACTION_MOVE:
                animate()
                        .x(event.getRawX() + mDX)
                        .y(event.getRawY() + mDY)
                        .setDuration(0)
                        .start();

                if (mListener != null) {
                    mListener.onMove(event.getRawX(), event.getRawY());
                }
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (mListener != null) {
                    mListener.onUp(event.getRawX(), event.getRawY(), event.getX(), event.getY());
                }
                break;
            default:
                return false;
        }
        return true;
    }
}
