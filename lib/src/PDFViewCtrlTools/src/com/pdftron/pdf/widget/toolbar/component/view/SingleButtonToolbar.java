package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.Toolbar;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.BaseEditToolbar;
import com.pdftron.pdf.controls.EditToolbar;
import com.pdftron.pdf.controls.OnToolSelectedListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars;

import java.util.ArrayList;
import java.util.List;

/**
 * A special toolbar used as edit toolbar for multi-stroke shapes.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SingleButtonToolbar extends ActionToolbar implements BaseEditToolbar {

    public static final String BUILDER_TAG = "PDFTron Commit Toolbar";
    private boolean mIsEditingAnnotation = false; // true if this toolbar is being used to edit an annotation

    private AppCompatButton mButton;
    private String mButtonText = null;

    private AnnotationToolbarBuilder mBuilder;

    private final List<OnClickListener> mCommitButtonListeners = new ArrayList<>();
    @Nullable
    private OnToolSelectedListener mOnToolSelectedListener;

    public SingleButtonToolbar(Context context) {
        super(context);
    }

    public SingleButtonToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SingleButtonToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super.init(attrs, defStyleAttr, defStyleRes);

        addOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == DefaultToolbars.ButtonId.UNDO.value()) {
                    if (mOnToolSelectedListener != null) {
                        mOnToolSelectedListener.onUndoSelected();
                    }
                } else if (id == DefaultToolbars.ButtonId.REDO.value()) {
                    if (mOnToolSelectedListener != null) {
                        mOnToolSelectedListener.onRedoSelected();
                    }
                }
                return false;
            }
        });
    }

    @Override
    public void setCompactMode(boolean compactMode) {
        super.setCompactMode(compactMode);

        inflateDefaultEditToolbar(null);
    }

    public void inflateDefaultEditToolbar(@Nullable ToolManager.ToolMode toolMode) {
        clearToolbarOverlayView();
        clearOptionalContainers();

        mButton = AnnotationToolbarTextButtonInflater.inflate(getContext(), R.string.done);
        mButton.setText(mButtonText);
        mButton.setTextColor(mAnnotToolbarTheme.textColor);
        mButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                for (OnClickListener listener : mCommitButtonListeners) {
                    listener.onClick(v);
                }
            }
        });

        if (toolMode == ToolManager.ToolMode.INK_CREATE || toolMode == ToolManager.ToolMode.SMART_PEN_INK) {
            mBuilder = AnnotationToolbarBuilder.withTag(SingleButtonToolbar.BUILDER_TAG)
                    // Custom eraser here as we need to handle erasing separately
                    .addCustomSelectableStickyButton(ToolbarButtonType.ERASER.title, ToolbarButtonType.ERASER.icon, DefaultToolbars.ButtonId.ERASER.value())
                    .addToolStickyButton(ToolbarButtonType.UNDO, DefaultToolbars.ButtonId.UNDO.value())
                    .addToolStickyButton(ToolbarButtonType.REDO, DefaultToolbars.ButtonId.REDO.value());
        } else {
            mBuilder = AnnotationToolbarBuilder.withTag(SingleButtonToolbar.BUILDER_TAG)
                    .addToolStickyButton(ToolbarButtonType.UNDO, DefaultToolbars.ButtonId.UNDO.value())
                    .addToolStickyButton(ToolbarButtonType.REDO, DefaultToolbars.ButtonId.REDO.value());
        }

        AnnotationToolbarBuilder builderCompact = mBuilder.copy()
                .addLeadingToolStickyButton(ToolbarButtonType.NAVIGATION, DefaultToolbars.ButtonId.NAVIGATION.value());

        if (mCompactMode) {
            inflateWithBuilder(builderCompact);
            addToolbarActionsRightOptionalContainer(mButton);
        } else {
            inflateWithBuilder(mBuilder);
            addToolbarLeftOptionalContainer(mButton);
        }
    }

    public void setButtonText(@Nullable String buttonText) {
        mButtonText = buttonText;
        if (mButton != null) {
            mButton.setText(mButtonText);
        }
    }

    public List<ToolbarItem> getButtonItems() {
        // Get all of the toolbar items, sticky toolbar items, and leading sticky toolbar items and adds them to a list to return all the ToolbarItems
        List<ToolbarItem> toolbarItems = mBuilder.getToolbarItems();
        List<ToolbarItem> stickyItems = mBuilder.getStickyToolbarItems();
        List<ToolbarItem> leadingStickyItems = mBuilder.getLeadingStickyToolbarItems();
        List<ToolbarItem> buttonItems = new ArrayList<>();
        buttonItems.addAll(toolbarItems);
        buttonItems.addAll(stickyItems);
        buttonItems.addAll(leadingStickyItems);

        return buttonItems;
    }

    /**
     * Add listener when commit button is pressed
     *
     * @param clickListener click listener for when commit button is pressed.
     */
    public void addOnButtonClickListener(@NonNull OnClickListener clickListener) {
        mCommitButtonListeners.add(clickListener);
    }

    @Override
    public void setup(PDFViewCtrl pdfViewCtrl, OnToolSelectedListener onToolSelectedListener, ArrayList<AnnotStyle> drawStyles, boolean b, boolean hasEraseBtn, boolean b1, boolean shouldExpand, boolean isStyleFixed) {
        mOnToolSelectedListener = onToolSelectedListener;
    }

    /**
     * Sets whether this toolbar is editing an annotation.
     *
     * @param isEditingAnnotation true if this toolbar is editing an annotation, false otherwise (i.e. creating a new annotation)
     */
    public void setEditingAnnotation(boolean isEditingAnnotation) {
        mIsEditingAnnotation = isEditingAnnotation;
    }

    /**
     * @return true if this toolbar is editing an annotation, false otherwise (i.e. creating a new annotation)
     */
    public boolean isEditingAnnotation() {
        return mIsEditingAnnotation;
    }

    @Override
    public void setOnEditToolbarChangeListener(EditToolbar.OnEditToolbarChangedListener listener) {

    }

    @Override
    public void show() {
        setVisibility(VISIBLE);
    }

    @Override
    public void updateControlButtons(boolean canClear, boolean canErase, boolean canUndo, boolean canRedo) {

    }

    @Override
    public void updateDrawColor(int drawIndex, int color) {

    }

    @Override
    public boolean handleKeyUp(int keyCode, KeyEvent event) {
        return false;
    }

    @Override
    public void updateDrawStyles(ArrayList<AnnotStyle> drawStyles) {

    }
}
