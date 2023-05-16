package com.pdftron.pdf.dialog.toolbarswitcher.button;

import android.content.Context;
import android.util.AttributeSet;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.card.MaterialCardView;
import com.pdftron.pdf.tools.R;

public class ToolbarSwitcherCompactButton extends ToolbarSwitcherButton {

    public ToolbarSwitcherCompactButton(@NonNull Context context) {
        super(context);
    }

    public ToolbarSwitcherCompactButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ToolbarSwitcherCompactButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ToolbarSwitcherCompactButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.view_toolbar_switcher_action_compact_button;
    }

    @Override
    protected void init() {
        super.init();

        ToolbarSwitcherButtonTheme theme = ToolbarSwitcherButtonTheme.fromContext(getContext());

        MaterialCardView cardView = findViewById(R.id.region_background);
        cardView.setCardBackgroundColor(theme.backgroundColorCompact);
    }
}
