package com.pdftron.pdf.widget.bottombar.component;

import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

import com.pdftron.pdf.widget.bottombar.component.view.BottomBarView;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;

/**
 * Bottom bar UI Component that is in charge of handling the bottom bar logic and UI events.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// todo: bfung for now restrict as the bottom bar API is incomplete
public class BottomBarComponent {

    private final BottomBarView mBottomBarView;

    public BottomBarComponent(@NonNull Fragment fragment, @NonNull ViewGroup parent) {
        mBottomBarView = new BottomBarView(parent);
    }

    public void inflateWithBuilder(@NonNull AnnotationToolbarBuilder builder) {
        mBottomBarView.inflateWithBuilder(builder);
    }

    public void addOnMenuItemClickListener(@NonNull Toolbar.OnMenuItemClickListener listener) {
        mBottomBarView.addOnMenuItemClickListener(listener);
    }

    public void setItemEnabled(boolean isEnabled, int buttonId) {
        mBottomBarView.setItemEnabled(buttonId, isEnabled);
    }

    public void setItemAppearanceEnabled(int buttonId, boolean isAppearanceEnabled) {
        mBottomBarView.setItemAppearanceEnabled(buttonId, isAppearanceEnabled);
    }

    public void setItemVisibility(boolean isVisible, int buttonId) {
        mBottomBarView.setItemVisibility(buttonId, isVisible);
    }

    public void setItemSelected(boolean isSelected, int buttonId) {
        mBottomBarView.setItemSelected(buttonId, isSelected);
    }

    public boolean hasVisibleItems() {
        return mBottomBarView.hasVisibleItems();
    }

    public void setItemIcon(int id, @NonNull Drawable icon) {
        mBottomBarView.setItemIcon(id, icon);
    }

    public void setShowBackground(int id, boolean showBackground) {
        mBottomBarView.setShowBackground(id, showBackground);
    }
}
