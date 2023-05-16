package com.pdftron.pdf.widget.bottombar.component.view;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.Toolbar;

import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.bottombar.component.BottomBarTheme;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class BottomBarView {
    private final BottomActionToolbar mActionToolbar;

    public BottomBarView(@NonNull ViewGroup parent) {
        BottomBarTheme bottomBarTheme = BottomBarTheme.fromContext(parent.getContext());
        AnnotationToolbarTheme annotToolbarTheme = new AnnotationToolbarTheme(
                bottomBarTheme.backgroundColor,
                bottomBarTheme.backgroundColor,
                bottomBarTheme.iconColor, // ignored
                bottomBarTheme.selectedBackgroundColor,
                bottomBarTheme.disabledIconColor,
                bottomBarTheme.selectedIconColor,
                bottomBarTheme.iconColor, // ignored
                bottomBarTheme.iconColor, // ignored
                bottomBarTheme.iconColor, // ignored
                bottomBarTheme.iconColor, // ignored
                bottomBarTheme.backgroundColor // ignored
        );

        mActionToolbar = new BottomActionToolbar(parent.getContext(), annotToolbarTheme);
        int pad = (int) Utils.convDp2Pix(parent.getContext(), 24);
        mActionToolbar.setPadding(pad, 0, pad, 0);
        parent.addView(mActionToolbar);
    }

    public void inflateWithBuilder(@NonNull AnnotationToolbarBuilder builder) {
        mActionToolbar.inflateWithBuilder(
                builder
        );
    }

    public void addOnMenuItemClickListener(@NonNull Toolbar.OnMenuItemClickListener listener) {
        mActionToolbar.addOnMenuItemClickListener(listener);
    }

    public void setItemEnabled(int buttonId, boolean isEnabled) {
        mActionToolbar.setItemEnabled(buttonId, isEnabled);
    }

    public void setItemAppearanceEnabled(int buttonId, boolean isAppearanceEnabled) {
        mActionToolbar.setItemAppearanceEnabled(buttonId, isAppearanceEnabled);
    }

    public void setItemVisibility(int buttonId, boolean isVisible) {
        mActionToolbar.setItemVisibility(buttonId, isVisible);
    }

    public void setItemSelected(int buttonId, boolean isSelected) {
        mActionToolbar.setItemSelected(buttonId, isSelected);
    }

    public boolean hasVisibleItems() {
        return mActionToolbar.hasVisibleItems();
    }

    public void setItemIcon(int id, @NonNull Drawable icon) {
        mActionToolbar.setItemIcon(id, icon);
    }

    public void setShowBackground(int id, boolean showBackground) {
        mActionToolbar.setBackground(id, showBackground);
    }
}
