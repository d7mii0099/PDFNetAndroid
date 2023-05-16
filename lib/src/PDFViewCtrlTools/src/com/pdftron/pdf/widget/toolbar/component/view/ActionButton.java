package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.IntRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;

import java.util.ArrayList;

public class ActionButton extends FrameLayout implements ToolbarButton {

    @Nullable
    private MenuItem mMenuItem;
    protected int mSelectedBackgroundColor = -1;
    private boolean mIsChecked;
    private boolean mIsCheckable; // if checkable, button will have a check and unchecked state
    private boolean mIsEnabled = true; // enabled button is clickable, non clickable otherwise
    private boolean mIsVisible = true; // sets visibility of button
    private boolean mHasOption; // if has option, button will have option arrow
    private int mIconColor = -1; // default icon color when not selected
    private int mSelectedIconColor = -1; // default icon color when selected
    private int mDisabledIconColor = -1; // default icon color when button is disabled
    private boolean mShowBackground = true; // whether the background should be shown
    @IntRange(from = 0, to = 255)
    private int mIconAlpha = 255;
    protected int mIconSize = -1;

    protected ViewGroup mRoot;
    @Nullable
    protected AnnotationPropertyPreviewView mIconView;
    @Nullable
    private Drawable mSelectedBackground;
    private AppCompatImageView mBackgroundContainer;
    @Nullable
    private ColorMappedDrawableWrapper mDrawableWrapper;

    private boolean mAlwaysShowIconHighlightColor = false;
    private boolean mShowIconHighlightColor = true;
    @ColorInt
    private int mIconHighlightColor = -1; // icon highlight color used when enabled

    public ActionButton(@NonNull Context context) {
        super(context);
        init(null, 0, 0);
    }

    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, 0);
    }

    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(attrs, defStyleAttr, 0);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, defStyleAttr, defStyleRes);
    }

    protected void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        LayoutInflater.from(getContext()).inflate(getLayoutRes(), this);
        mRoot = findViewById(R.id.root);
        mIconView = findViewById(R.id.icon);
        mBackgroundContainer = findViewById(R.id.background_container);

        updateIconSize();
    }

    @LayoutRes
    protected int getLayoutRes() {
        return R.layout.toolbar_action_view;
    }

    public void setMenuItem(MenuItem item) {
        mMenuItem = item;
    }

    @Override
    public void setIcon(@NonNull Drawable drawable) {
        drawable.mutate(); // starting Android Q and above, mutate does not seem to be required but we'll call it anyways
        mDrawableWrapper = new ColorMappedDrawableWrapper(drawable);

        // Set the defined color/alpha for the drawable
        mDrawableWrapper.setIconColor(mIconColor);
        mDrawableWrapper.updateIconHighlightColor(mIconHighlightColor, mIconAlpha);
        if (mIconView != null) {
            mIconView.setImageDrawable(mDrawableWrapper.getDrawable());
            mIconView.invalidate();
        }
        updateButtonAppearanceFromState();
    }

    public void setSelectedBackgroundColor(@ColorInt int color) {
        mSelectedBackgroundColor = color;
        mSelectedBackground = getResources().getDrawable(R.drawable.background_toolbar_action_button);
        mSelectedBackground = mSelectedBackground.mutate();
        mSelectedBackground.setColorFilter(new PorterDuffColorFilter(mSelectedBackgroundColor, PorterDuff.Mode.SRC_ATOP));
        updateButtonAppearanceFromState();
    }

    public void setIconAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mIconAlpha = alpha;
        updateButtonAppearanceFromState();
    }

    public void setIconColor(@ColorInt int color) {
        mIconColor = color;
        updateButtonAppearanceFromState();
    }

    public void setSelectedIconColor(@ColorInt int iconColor) {
        mSelectedIconColor = iconColor;
        updateButtonAppearanceFromState();
    }

    public void setDisabledIconColor(@ColorInt int iconColor) {
        mDisabledIconColor = iconColor;
        updateButtonAppearanceFromState();
    }

    /**
     * Set the icon icon highlight color. If color is 0 (transparent), then the icon highlight color is set
     * to the icon color
     *
     * @param color color integer which can be 0
     */
    public void setIconHighlightColor(@ColorInt int color) {
        if (color == 0) {
            mIconHighlightColor = mIconColor;
        } else {
            mIconHighlightColor = color;
        }
        updateButtonAppearanceFromState();
    }

    private void updateIconColor(@ColorInt int color) {
        if (mDrawableWrapper != null) {
            mDrawableWrapper.setIconColor(color);
        }
        if (mIconView != null) {
            mIconView.invalidate();
        }
    }

    protected void updateBackgroundColor(Drawable backgroundDrawable) {
        if (mBackgroundContainer != null) {
            if (mShowBackground) {
                mBackgroundContainer.setBackground(backgroundDrawable);
            } else {
                mBackgroundContainer.setBackground(null);
            }
        }
    }

    private void updateIconHighlightColor(@ColorInt int color, @IntRange(from = 0, to = 255) int alpha) {
        if (mShowIconHighlightColor) {
            if (mDrawableWrapper != null) {
                mDrawableWrapper.updateIconHighlightColor(color, alpha);
            }
            if (mIconView != null) {
                mIconView.invalidate();
            }
        }
    }

    private void updateOptionArrowColor(@ColorInt int color) {
        AppCompatImageView arrowView = findViewById(R.id.option_arrow);
        if (arrowView != null) {
            arrowView.setColorFilter(color);
        }
    }

    private void updateButtonAppearanceFromState() {
        if (mIsEnabled) { // if enabled, then show default icon colors and icon highlight
            updateBackgroundColor(null);
            updateOptionArrowColor(mIconColor);
            if (mIsCheckable) {
                if (mIsChecked) {
                    updateIconColor(mSelectedIconColor);
                    updateBackgroundColor(mSelectedBackground);
                    updateIconHighlightColor(mIconHighlightColor, mIconAlpha);
                } else {
                    updateIconColor(mIconColor);
                    updateBackgroundColor(null);
                    if (mAlwaysShowIconHighlightColor) {
                        updateIconHighlightColor(mIconHighlightColor, mIconAlpha);
                    } else {
                        // This resets the icon highlight color to match icon color
                        updateIconHighlightColor(mIconColor, Color.alpha(mIconColor));
                    }
                }
            } else {
                updateBackgroundColor(null);
                updateIconColor(mIconColor);
                if (mAlwaysShowIconHighlightColor) {
                    updateIconHighlightColor(mIconHighlightColor, mIconAlpha);
                } else {
                    // This resets the icon highlight color to match icon color
                    updateIconHighlightColor(mIconColor, Color.alpha(mIconColor));
                }
            }
        } else { // otherwise, just show grayed out icon
            updateBackgroundColor(null);
            updateIconColor(mDisabledIconColor);
            updateOptionArrowColor(mDisabledIconColor);
        }
    }

    /**
     * Sets whether to show to show the icon highlight color event when button is disabled.
     * <p>
     * See {@link #setShowIconHighlightColor}
     *
     * @param alwaysShowIconHighlightColor whether to always show highlighted icon colors
     */
    public void setAlwaysShowIconHighlightColor(boolean alwaysShowIconHighlightColor) {
        mAlwaysShowIconHighlightColor = alwaysShowIconHighlightColor;
        updateButtonAppearanceFromState();
    }

    /**
     * Sets whether to show the icon highlight color. The highlight color is used to highlight
     * a certain area of the icon. When enabled, that section of the icon will be highlighted.
     *
     * @param showIconHighlightColor whether to show highlighted icon colors
     */
    public void setShowIconHighlightColor(boolean showIconHighlightColor) {
        mShowIconHighlightColor = showIconHighlightColor;
        updateButtonAppearanceFromState();
    }

    @Override
    public void deselect() {
        if (!isCheckable()) {
            return;
        }
        mIsChecked = false;
        if (mMenuItem != null) {
            mMenuItem.setChecked(false);
        }
        updateButtonAppearanceFromState();
    }

    @Override
    public void select() {
        if (!isCheckable()) {
            return;
        }
        mIsChecked = true;
        if (mMenuItem != null) {
            mMenuItem.setChecked(true);
        }
        updateButtonAppearanceFromState();
    }

    @Override
    public void enable() {
        mIsEnabled = true;
        setClickable(true);
        updateButtonAppearanceFromState();
        if (mMenuItem != null) {
            mMenuItem.setEnabled(true);
        }
    }

    @Override
    public void disable() {
        mIsEnabled = false;
        setClickable(false);
        updateButtonAppearanceFromState();
        if (mMenuItem != null) {
            mMenuItem.setEnabled(false);
        }
    }

    public void setAppearanceEnabled(boolean isAppearanceEnabled) {
        mIsEnabled = isAppearanceEnabled;
        setClickable(true);
        updateButtonAppearanceFromState();
    }

    @Override
    public boolean isCheckable() {
        return mIsCheckable;
    }

    @Override
    public boolean isSelected() {
        return mIsChecked;
    }

    @Override
    public void show() {
        mIsVisible = true;
        if (mMenuItem != null) {
            mMenuItem.setVisible(true);
        }
    }

    @Override
    public void hide() {
        mIsVisible = false;
        if (mMenuItem != null) {
            mMenuItem.setVisible(false);
        }
    }

    public boolean isVisible() {
        return mIsVisible;
    }

    @Override
    public void setCheckable(boolean isCheckable) {
        mIsCheckable = isCheckable;
    }

    @Override
    public boolean hasOption() {
        return mHasOption;
    }

    @Override
    public void setHasOption(boolean hasOption) {
        mHasOption = hasOption;
        AppCompatImageView arrowView = findViewById(R.id.option_arrow);
        if (arrowView != null) {
            arrowView.setVisibility(mHasOption ? VISIBLE : GONE);
        }
    }

    public static int getPreviewColor(@NonNull AnnotStyle annotStyle) {
        int strokeColor = annotStyle.getColor();
        int fillColor = annotStyle.getFillColor();
        int textColor = annotStyle.getTextColor();
        int result;
        if (fillColor == Color.TRANSPARENT && strokeColor == Color.TRANSPARENT && textColor == Color.TRANSPARENT) {
            result = 0;
        } else if (textColor != Color.TRANSPARENT) {
            result = textColor;
        } else if (fillColor != Color.TRANSPARENT) {
            result = fillColor;
        } else {
            result = strokeColor;
        }
        return result;
    }

    @Nullable
    public MenuItem getMenuItem() {
        return mMenuItem;
    }

    protected void updateIconSize() {
        updateIconSize(mIconView, mIconSize);
    }

    protected static void updateIconSize(ImageView iconView, int iconSize) {
        if (iconView != null && iconSize != -1) {
            ViewGroup.LayoutParams layoutParams = iconView.getLayoutParams();
            layoutParams.width = iconSize;
            layoutParams.height = iconSize;
            iconView.setLayoutParams(layoutParams);
        }
    }

    public void setIconSize(int sizeInPx) {
        mIconSize = sizeInPx;
        updateIconSize();
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (selected) {
            select();
        } else {
            deselect();
        }
    }

    public void updateAppearance(@NonNull ArrayList<AnnotStyle> annotStyles) {
        if (annotStyles.size() == 1) {
            AnnotStyle annotStyle = annotStyles.get(0);
            if ((annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_ERASER || annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT
                    || annotStyle.getAnnotType() == Annot.e_Text) && mIconView != null) {
                mIconView.setImageDrawable(null);
                mIconView.setAnnotType(annotStyle.getAnnotType());
                mIconView.updateFillPreview(annotStyle);
            } else {
                tintIcon(annotStyle);
            }
        }
    }

    protected void tintIcon(@NonNull AnnotStyle annotStyle) {
        int color = getPreviewColor(annotStyle);
        float opacity = annotStyle.getOpacity();
        setIconHighlightColor(color);
        setIconAlpha((int) (opacity * 255.0f));
    }

    public void setShowBackground(boolean showBackground) {
        mShowBackground = showBackground;
    }
}
