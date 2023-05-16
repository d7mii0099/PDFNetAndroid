package com.pdftron.pdf.widget.preset.component.view;

import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;

/**
 * Tablet version of {@link PresetBarView}. Does not have the arrow indicators and close button. Also,
 * preset buttons are a little smaller.
 */
public class TabletPresetBarView extends PresetBarView {
    public TabletPresetBarView(@NonNull ViewGroup parent) {
        super(parent);
        // Since the preset view is in the annotation toolbar, we can remove the close button
        mCloseContainer.setVisibility(View.GONE);

        // The preset view should have the same background as the annotation toolbar's preset container
        AnnotationToolbarTheme annotationToolbarTheme = AnnotationToolbarTheme.fromContext(getContext());
        mRootContainer.setBackgroundColor(annotationToolbarTheme.backgroundColorSecondary);

        // Also, the preset view should be a little smaller than the annotation toolbar buttons
        int size = parent.getContext().getResources().getDimensionPixelSize(R.dimen.toolbar_icon_tablet_size);
        int backgroundWidth = parent.getContext().getResources().getDimensionPixelSize(R.dimen.toolbar_icon_tablet_background_width);
        for (PresetActionButton button : mPresetButtons) {
            button.setIconSize(size);
            button.setBackgroundWidth(backgroundWidth);
        }
        mSinglePresetButton.setIconSize(size);
    }

    /**
     * Removes this preset bar view from its parent and adds it to the new parent.
     *
     * @param newParent the new parent to add this view to.
     */
    public void attachToNewParent(@NonNull ViewGroup newParent) {
        mParent.removeView(mRootContainer);
        newParent.addView(mRootContainer);
        mParent = newParent;
    }
}
