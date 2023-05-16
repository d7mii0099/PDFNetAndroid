package com.pdftron.pdf.widget.toolbar.component;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.preset.component.PresetBarComponent;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;
import com.pdftron.pdf.widget.preset.component.model.PresetBarState;
import com.pdftron.pdf.widget.preset.component.view.TabletPresetBarView;
import com.pdftron.pdf.widget.preset.signature.SignatureViewModel;
import com.pdftron.pdf.widget.toolbar.ToolManagerViewModel;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.view.AnnotationToolbarView;
import com.pdftron.pdf.widget.toolbar.component.view.SingleButtonToolbar;

import java.util.HashSet;

/**
 * Tablet version of {@link AnnotationToolbarComponent} which contains a preset style section
 * in the annotation toolbar.
 */
public class TabletAnnotationToolbarComponent extends AnnotationToolbarComponent {

    private final TabletPresetBarView mPresetBarView;
    private FrameLayout mEditToolbarPresetContainer;
    private boolean mTabletMode;
    private boolean mHidePresetBar;

    public TabletAnnotationToolbarComponent(@NonNull Fragment fragment,
            @NonNull FragmentManager fragmentManager,
            @NonNull final AnnotationToolbarViewModel annotationToolbarViewModel,
            @NonNull PresetBarViewModel presetBarViewModel,
            @NonNull ToolManagerViewModel toolManagerViewModel,
            @NonNull SignatureViewModel signatureViewModel,
            @NonNull AnnotationToolbarView view,
            boolean tabletMode,
            @Nullable HashSet<ToolbarButtonType> toolsToHidePresetBar,
            @Nullable boolean hidePresetBar
    ) {
        super(fragment, annotationToolbarViewModel, presetBarViewModel, toolManagerViewModel, view);
        mHidePresetBar = hidePresetBar;
        FrameLayout presetContainer = mAnnotationToolbarView.getPresetContainer();
        presetContainer.setVisibility(hidePresetBar ? View.GONE : View.VISIBLE);
        // For tablets, the preset bar will be contained in the annotation toolbar
        mPresetBarView = new TabletPresetBarView(presetContainer);
        new PresetBarComponent(fragment, fragmentManager, presetBarViewModel, toolManagerViewModel, signatureViewModel, mPresetBarView, toolsToHidePresetBar);

        mTabletMode = tabletMode;
        setTabletMode(mTabletMode);

        mPresetBarViewModel.observePresetState(fragment, new Observer<PresetBarState>() {
            @Override
            public void onChanged(PresetBarState presetState) {
                if (mTabletMode) {
                    ToolbarItem toolbarItem = presetState.toolbarItem;
                    FrameLayout presetContainer = mAnnotationToolbarView.getPresetContainer();
                    if (presetContainer != null) {
                        if (toolbarItem != null && toolsToHidePresetBar != null && toolsToHidePresetBar.contains(toolbarItem.toolbarButtonType)) {
                            presetContainer.setVisibility(View.INVISIBLE);
                        } else {
                            presetContainer.setVisibility(View.VISIBLE);
                        }
                    }
                }
            }
        });
    }

    public void setTabletMode(boolean tabletMode) {
        mTabletMode = tabletMode;

        FrameLayout presetContainer = mAnnotationToolbarView.getPresetContainer();
        presetContainer.setVisibility(mHidePresetBar || !mTabletMode ? View.GONE : View.VISIBLE);
        if (mEditToolbarPresetContainer != null) {
            mEditToolbarPresetContainer.setVisibility(mHidePresetBar || !mTabletMode ? View.GONE : View.VISIBLE);
        }

        mAnnotationToolbarView.updateTheme();
        mPresetBarView.updateTheme();
    }

    @Override
    public void setCompactMode(boolean compactMode) {
        super.setCompactMode(compactMode);

        mPresetBarView.setCompactMode(compactMode);
    }

    @Override
    protected SingleButtonToolbar createEditToolbar() {
        SingleButtonToolbar editToolbar = super.createEditToolbar();
        if (!mHidePresetBar && mTabletMode) {
            // For tablets, we will need to show the preset section in the annotation toolbar
            mEditToolbarPresetContainer = editToolbar.getPresetContainer();
            mEditToolbarPresetContainer.setVisibility(View.VISIBLE);
        }

        return editToolbar;
    }

    @Override
    protected void showEditToolbar(@NonNull ToolManager.ToolMode toolMode, @Nullable Annot annot, int pageNum, @NonNull Bundle bundle, boolean fromAnnotToolbar) {
        super.showEditToolbar(toolMode, annot, pageNum, bundle, fromAnnotToolbar);
        if (mTabletMode) {
            // Attach preset bar view to new parent in edit toolbar
            if (mEditToolbarPresetContainer != null) {
                mPresetBarView.attachToNewParent(mEditToolbarPresetContainer);
            }
        }
    }

    @Override
    public void closeEditToolbar() {
        super.closeEditToolbar();
        if (mTabletMode) {
            // Attach preset bar view to back to parent in the annotation toolbar
            if (mAnnotationToolbarView.getPresetContainer() != null) {
                mPresetBarView.attachToNewParent(mAnnotationToolbarView.getPresetContainer());
            }
        }
    }
}
