package com.pdftron.pdf.dialog.toolbarswitcher.button;

import android.content.Context;
import android.graphics.PorterDuff;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

import com.pdftron.pdf.tools.R;

public class ToolbarSwitcherButton extends FrameLayout {

    private TextView mTitle;
    private AppCompatImageView mSwitcherIcon;
    private boolean mShouldShowSwitcherIcon = true;

    public ToolbarSwitcherButton(@NonNull Context context) {
        super(context);
        init();
    }

    public ToolbarSwitcherButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ToolbarSwitcherButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public ToolbarSwitcherButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.view_toolbar_switcher_action_button;
    }

    protected void init() {
        Context context = getContext();
        LayoutInflater.from(context).inflate(getLayoutResource(), this);

        ToolbarSwitcherButtonTheme theme = ToolbarSwitcherButtonTheme.fromContext(context);

        mTitle = findViewById(R.id.title);
        mTitle.setTextColor(theme.textColor);
        mSwitcherIcon = findViewById(R.id.switcher_icon);
        mSwitcherIcon.setColorFilter(theme.iconColor, PorterDuff.Mode.SRC_IN);

        // update any set states
        updateSwitcherIconAppearance();
    }

    public void setText(String text) {
        mTitle.setText(text);
    }

    public void showSwitcherIcon() {
        mShouldShowSwitcherIcon = true;
        updateSwitcherIconAppearance();
    }

    public void hideSwitcherIcon() {
        mShouldShowSwitcherIcon = false;
        updateSwitcherIconAppearance();
    }

    private void updateSwitcherIconAppearance() {
        if (mSwitcherIcon != null) {
            if (mShouldShowSwitcherIcon) {
                mSwitcherIcon.setVisibility(VISIBLE);
            } else {
                mSwitcherIcon.setVisibility(GONE);
            }
        }
    }
}
