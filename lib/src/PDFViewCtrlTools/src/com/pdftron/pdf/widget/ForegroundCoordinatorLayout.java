package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.AttrRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import android.util.AttributeSet;
import android.view.Gravity;

import com.pdftron.pdf.tools.R;

/**
 * A Coordinator layout that can draw foreground drawable
 */
public class ForegroundCoordinatorLayout extends CoordinatorLayout implements
    ForegroundLayout {

    private static final int DEFAULT_FOREGROUND_GRAVITY = GravityCompat.START | Gravity.TOP;

    private Drawable mForeground = null;
    private int mForegroundGravity = DEFAULT_FOREGROUND_GRAVITY;
    private Rect mSelfBounds = new Rect();
    private Rect mOverlayBounds = new Rect();
    private boolean mForegroundBoundsChanged = false;

    public ForegroundCoordinatorLayout(Context context) {
        this(context, null);
    }

    public ForegroundCoordinatorLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ForegroundCoordinatorLayout(Context context, AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ForegroundCoordinatorLayout,
                defStyleAttr, 0);

        Drawable foreground;
        int foregroundGravity;
        try {
            foreground = a.getDrawable(R.styleable.ForegroundCoordinatorLayout_android_foreground);
            foregroundGravity = a.getInt(R.styleable.ForegroundCoordinatorLayout_android_foregroundGravity, DEFAULT_FOREGROUND_GRAVITY);
        } finally {
            a.recycle();
        }

        if (foreground != null) {
            setForeground(foreground);
        }

        setForegroundGravity(foregroundGravity);
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();

        if (mForeground != null && mForeground.isStateful()) {
            mForeground.setState(getDrawableState());
        }
    }

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == mForeground);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mForeground != null) mForeground.jumpToCurrentState();
    }

    @Override
    public Drawable getForeground() {
        return mForeground;
    }

    @Override
    public void setForeground(Drawable drawable) {
        if (mForeground != drawable) {
            if (mForeground != null) {
                mForeground.setCallback(null);
                unscheduleDrawable(mForeground);
            }

            mForeground = drawable;

            if (drawable != null) {
                setWillNotDraw(false);
                drawable.setCallback(this);
                if (drawable.isStateful()) {
                    drawable.setState(getDrawableState());
                }
            } else {
                setWillNotDraw(true);
            }

            requestLayout();
            invalidate();
        }
    }

    @Override
    public int getForegroundGravity() {
        return mForegroundGravity;
    }

    @Override
    public void setForegroundGravity(int gravity) {
        if (mForegroundGravity != gravity) {
            mForegroundGravity = gravity;

            invalidate();
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mForegroundBoundsChanged = true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mForegroundBoundsChanged = true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        final Drawable foreground = mForeground;
        if (foreground != null) {
            if (mForegroundBoundsChanged) {
                mForegroundBoundsChanged = false;
                final Rect selfBounds = mSelfBounds;
                final Rect overlayBounds = mOverlayBounds;

                selfBounds.set(0, 0, getWidth(), getHeight());

                final int ld = ViewCompat.getLayoutDirection(this);
                GravityCompat.apply(mForegroundGravity, foreground.getIntrinsicWidth(),
                        foreground.getIntrinsicHeight(), selfBounds, overlayBounds, ld);
                foreground.setBounds(overlayBounds);
            }

            foreground.draw(canvas);
        }
    }

    /**
     * It sets the hotspot changes to foreground drawable
     */
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);
        if (mForeground != null) {
            mForeground.setHotspot(x, y);
        }
    }
}
