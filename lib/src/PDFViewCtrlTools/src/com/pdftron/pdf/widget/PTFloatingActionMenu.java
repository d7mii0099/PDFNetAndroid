package com.pdftron.pdf.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.github.clans.fab.FloatingActionMenu;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

public class PTFloatingActionMenu extends FloatingActionMenu {
    public PTFloatingActionMenu(Context context) {
        this(context, null);
    }

    public PTFloatingActionMenu(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PTFloatingActionMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.PTFloatingActionMenu, defStyleAttr, 0);
        try {
            ImageView iconView = getMenuIconView();
            if (iconView != null) {
                if (attr.hasValue(R.styleable.PTFloatingActionMenu_android_tint)) {
                    int tintColor = attr.getColor(R.styleable.PTFloatingActionMenu_android_tint,
                            Utils.getThemeAttrColor(context, android.R.attr.textColorPrimary));
                    iconView.setColorFilter(tintColor);
                }
            }
        } finally {
            attr.recycle();
        }
    }
}
