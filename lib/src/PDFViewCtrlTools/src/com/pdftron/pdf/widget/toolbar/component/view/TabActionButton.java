package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.AppCompatImageView;

import com.pdftron.pdf.tools.R;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class TabActionButton extends FrameLayout {

    private TextView mTabCount;
    private AppCompatImageView mIcon;

    public TabActionButton(@NonNull Context context) {
        super(context);
        init();
    }

    public TabActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public TabActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public TabActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        LayoutInflater.from(getContext()).inflate(R.layout.toolbar_tab_action_view, this);
        mTabCount = findViewById(R.id.tab_count);
        mIcon = findViewById(R.id.icon);

        TabActionButtonTheme theme = TabActionButtonTheme.fromContext(getContext());
        mTabCount.setTextColor(theme.textColor);

        // Tint drawable
        Drawable background = mIcon.getDrawable().mutate();
        if (background instanceof GradientDrawable) {
            // cast to 'GradientDrawable'
            GradientDrawable gradientDrawable = (GradientDrawable) background;
            gradientDrawable.setStroke((int) getContext().getResources().getDimension(R.dimen.tab_icon_stroke_width), theme.iconColor);
        }
    }

    public void setTabCount(int numTabs) {
        mTabCount.setText(String.valueOf(numTabs));
    }
}
