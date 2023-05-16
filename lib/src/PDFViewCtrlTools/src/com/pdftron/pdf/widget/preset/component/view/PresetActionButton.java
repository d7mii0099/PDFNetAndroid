package com.pdftron.pdf.widget.preset.component.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.transition.Fade;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;

import java.util.ArrayList;

public class PresetActionButton extends ActionButton {

    protected AppCompatImageView mStyleIconView;
    protected AppCompatImageView mSecondaryIconView;

    protected int mBackgroundColor = -1;

    public PresetActionButton(@NonNull Context context) {
        super(context);
    }

    public PresetActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PresetActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PresetActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super.init(attrs, defStyleAttr, defStyleRes);
        mStyleIconView = findViewById(R.id.style_icon);
        mSecondaryIconView = findViewById(R.id.secondary_icon);
    }

    @Override
    protected int getLayoutRes() {
        return R.layout.presetbar_action_view;
    }

    @Override
    public void setIconColor(int color) {
        super.setIconColor(color);
    }

    public void setExpandStyleIconColor(int color) {
        mStyleIconView.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    public void setBackgroundWidth(int sizeInPx) {
        ImageView iconView = findViewById(R.id.background_container);
        if (iconView != null && sizeInPx != -1) {
            ViewGroup.LayoutParams layoutParams = iconView.getLayoutParams();
            layoutParams.width = sizeInPx;
            iconView.setLayoutParams(layoutParams);
        }
    }

    @Override
    public void select() {
        super.select();

        resetStyle();

        Transition transition = new Fade();
        transition.setDuration(150); // TODO
        TransitionManager.beginDelayedTransition(this, transition);
        mStyleIconView.setVisibility(View.VISIBLE);
    }

    @Override
    public void deselect() {
        super.deselect();

        Transition transition = new Fade();
        transition.setDuration(150); // TODO
        TransitionManager.beginDelayedTransition(this, transition);
        mStyleIconView.setVisibility(View.INVISIBLE);
    }

    public void openStyle() {
        mStyleIconView.setImageResource(R.drawable.ic_chevron_up);
    }

    public void resetStyle() {
        mStyleIconView.setImageResource(R.drawable.ic_chevron_down);
    }

    public void setClientBackgroundColor(@ColorInt int color) {
        mBackgroundColor = color;
    }

    @Override
    protected void updateBackgroundColor(Drawable backgroundDrawable) {
        super.updateBackgroundColor(backgroundDrawable);

        // update secondary background
        if (mSecondaryIconView != null && mSecondaryIconView.getBackground() instanceof GradientDrawable) {
            GradientDrawable drawable = (GradientDrawable) mSecondaryIconView.getBackground().mutate();
            if (backgroundDrawable != null) {
                drawable.setStroke(Math.round(Utils.convDp2Pix(getContext(), 2)), mSelectedBackgroundColor);
            } else {
                drawable.setStroke(Math.round(Utils.convDp2Pix(getContext(), 2)), mBackgroundColor);
            }
        }
    }

    @Override
    public void updateAppearance(@NonNull ArrayList<AnnotStyle> annotStyles) {
        if (annotStyles.size() == 1) {
            mSecondaryIconView.setVisibility(View.GONE);
            super.updateAppearance(annotStyles);
        } else if (annotStyles.size() == 2) {
            // smart pen tool
            mSecondaryIconView.setVisibility(View.VISIBLE);

            AnnotStyle markupStyle = annotStyles.get(1);
            int res = AnnotUtils.getAnnotImageResId(markupStyle.getAnnotType());
            setIcon(getResources().getDrawable(res));
            tintIcon(markupStyle);

            AnnotStyle inkStyle = annotStyles.get(0);
            mSecondaryIconView.setColorFilter(getPreviewColor(inkStyle));
        }
    }
}
