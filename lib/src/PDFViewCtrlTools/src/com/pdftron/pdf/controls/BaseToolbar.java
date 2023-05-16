package com.pdftron.pdf.controls;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.TooltipCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.PointerIcon;
import android.view.View;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

import java.util.ArrayList;

public abstract class BaseToolbar extends InsectHandlerToolbar {

    protected int mToolbarBackgroundColor;
    protected int mToolbarToolBackgroundColor;
    protected int mToolbarToolIconColor;
    protected int mToolbarCloseIconColor;

    protected ToolManager mToolManager;
    protected int mSelectedButtonId;
    protected int mSelectedToolId = -1;
    protected ArrayList<View> mButtons;
    protected boolean mButtonStayDown;
    protected boolean mEventAction;

    public BaseToolbar(@NonNull Context context) {
        super(context);
    }

    public BaseToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public BaseToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BaseToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public abstract void selectTool(View view, int id);

    public boolean isShowing() {
        return getVisibility() == View.VISIBLE;
    }

    abstract void addButtons();

    protected Drawable getSpinnerBitmapDrawable(Context context, int width, int height, int color, boolean expanded) {
        int spinnerDrawableId = R.drawable.controls_toolbar_spinner_selected_blue;
        return ViewerUtils.getBitmapDrawable(context, spinnerDrawableId,
                width, height, color, expanded, true);
    }

    protected Drawable getNormalBitmapDrawable(Context context, int width, int height, int color, boolean expanded) {
        Drawable normalBitmapDrawable;
        if (expanded) {
            normalBitmapDrawable = Utils.getDrawable(context, R.drawable.rounded_corners);
            if (normalBitmapDrawable != null) {
                normalBitmapDrawable = normalBitmapDrawable.mutate();
                normalBitmapDrawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP);
            }
        } else {
            normalBitmapDrawable = ViewerUtils.getBitmapDrawable(context, R.drawable.controls_annotation_toolbar_bg_selected_blue,
                    width, height, color, false, true);
        }
        return normalBitmapDrawable;
    }

    protected void setViewDrawable(Context context, int id, boolean spinner, int resId, Drawable spinnerBitmapDrawable, Drawable normalBitmapDrawable, int iconColor) {
        View v = findViewById(id);
        if (v != null) {
            v.setBackground(ViewerUtils.createBackgroundSelector(spinner ? spinnerBitmapDrawable : normalBitmapDrawable));
            Drawable drawable = Utils.createImageDrawableSelector(context, resId, iconColor);
            ((AppCompatImageButton) v).setImageDrawable(drawable);
        }
    }

    protected void safeAddButtons(@IdRes int viewId) {
        View v = findViewById(viewId);
        if (v != null) {
            mButtons.add(v);
        }
    }

    protected void initializeButtons() {
        mButtons = new ArrayList<>();
        addButtons();

        View.OnClickListener clickListener = getButtonsClickListener();
        for (View view : mButtons) {
            if (view != null) {
                view.setOnClickListener(clickListener);
                TooltipCompat.setTooltipText(view, view.getContentDescription());
                if (Utils.isNougat()) {
                    view.setOnGenericMotionListener(new OnGenericMotionListener() {
                        @Override
                        public boolean onGenericMotion(View view, MotionEvent motionEvent) {
                            Context context = getContext();
                            if (context == null) {
                                return false;
                            }

                            if (view.isShown() && Utils.isNougat()) {
                                mToolManager.onChangePointerIcon(PointerIcon.getSystemIcon(context, PointerIcon.TYPE_HAND));
                            }
                            return true;
                        }
                    });
                }
            }
        }
    }

    protected View.OnClickListener getButtonsClickListener() {
        return new OnClickListener() {
            @Override
            public void onClick(View view) {
                selectTool(view, view.getId());
            }
        };
    }

    protected void selectButton(int id) {
        mSelectedButtonId = id;
        for (View view : mButtons) {
            if (view.getId() == id) {
                view.setSelected(true);
            } else {
                view.setSelected(false);
            }
        }
        if (mToolManager.isSkipNextTapEvent()) {
            // if we previously closed a popup without clicking viewer
            // let's clear the flag
            mToolManager.resetSkipNextTapEvent();
        }
    }

    class ToolItem {
        int color;
        @IdRes
        int id;
        @DrawableRes
        int drawable;
        boolean spinner;

        ToolItem(int type, @IdRes int id, boolean spinner) {
            this(type, id, AnnotUtils.getAnnotImageResId(type), spinner);
        }

        ToolItem(@SuppressWarnings("unused") int type, @IdRes int id, @DrawableRes int drawable, boolean spinner) {
            this(type, id, drawable, spinner, mToolbarToolIconColor);
        }

        ToolItem(@SuppressWarnings("unused") int type, @IdRes int id, @DrawableRes int drawable, boolean spinner, int color) {
            this.id = id;
            this.drawable = drawable;
            this.spinner = spinner;
            this.color = color;
        }
    }
}
