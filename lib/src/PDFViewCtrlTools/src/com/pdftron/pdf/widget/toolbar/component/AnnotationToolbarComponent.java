package com.pdftron.pdf.widget.toolbar.component;

import android.content.Context;
import android.os.Bundle;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.BaseEditToolbar;
import com.pdftron.pdf.controls.EditToolbar;
import com.pdftron.pdf.controls.EditToolbarImpl;
import com.pdftron.pdf.controls.OnToolSelectedListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.AdvancedShapeCreate;
import com.pdftron.pdf.tools.AnnotEditRectGroup;
import com.pdftron.pdf.tools.FreehandCreate;
import com.pdftron.pdf.tools.QuickMenuItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.RubberStampCreate;
import com.pdftron.pdf.tools.Signature;
import com.pdftron.pdf.tools.SmartPenInk;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.tools.UndoRedoManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.EventHandler;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;
import com.pdftron.pdf.widget.preset.component.model.PresetBarState;
import com.pdftron.pdf.widget.preset.component.model.PresetButtonState;
import com.pdftron.pdf.widget.toolbar.ToolManagerViewModel;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.view.ActionButton;
import com.pdftron.pdf.widget.toolbar.component.view.AnnotationToolbarTextButtonInflater;
import com.pdftron.pdf.widget.toolbar.component.view.AnnotationToolbarView;
import com.pdftron.pdf.widget.toolbar.component.view.SingleButtonToolbar;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * Annotation Toolbar UI Component that is in charge of handling the toolbar logic and UI events.
 */
public class AnnotationToolbarComponent implements AdvancedShapeCreate.OnEditToolbarListener {

    // Key used to add bundle data to a tool created by ToolManager. Adds data associated with a annot toolbar item
    public static final String TOOLBAR_ITEM_BUNDLE = "toolbarItem";
    // Key used to add bundle data to a tool created by ToolManager. Adds boolean, true if default PAN tool was created due to disabled tools in ToolManager.
    public static final String TOOLMODE_DISABLED = "toolmode_disabled";
    protected Context mContext;

    @NonNull
    protected final AnnotationToolbarView mAnnotationToolbarView;
    protected final PresetBarViewModel mPresetBarViewModel;
    protected final ToolManagerViewModel mToolManagerViewModel;
    protected final AnnotationToolbarViewModel mAnnotationToolbarViewModel;

    protected final HashMap<Integer, ToolbarItem> mToolbarItemIdMap = new HashMap<>();

    protected final boolean mForceSameNextToolMode;

    protected EditToolbarImpl mEditToolbarImpl;

    @Nullable
    protected SingleButtonToolbar mEditToolbar;
    private boolean mLastUsedTool = true;
    protected final LifecycleOwner mLifecycleOwner;

    protected boolean mCompactMode;
    protected @DrawableRes
    int mNavigationIcon;
    protected boolean mNavigationIconVisible = true;

    @Nullable
    protected ToolManager.ToolMode mSelectedToolMode;

    // Listeners
    private List<AnnotationButtonClickListener> mButtonClickListeners = new ArrayList<>();
    private List<OnToolbarChangedListener> mOnToolbarChangedListeners = new ArrayList<>();
    private List<OnPreBuildToolbarListener> mOnPreBuildToolbarListeners = new ArrayList<>();

    @Nullable
    private UndoRedoManager.UndoRedoStateChangeListener mListener = null;

    /**
     * Creates a the default {@link AnnotationToolbarComponent}
     *
     * @param lifecycleOwner       the lifecycle owner that will handle this UI component's state
     * @param presetBarViewModel   the {@link PresetBarViewModel} that manages the preset bar state
     * @param toolManagerViewModel the {@link ToolManagerViewModel} that manages the current ToolManager
     * @param container            the container to add this {@link AnnotationToolbarComponent}
     */
    public AnnotationToolbarComponent(@NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final AnnotationToolbarViewModel annotationToolbarViewModel,
            @NonNull final PresetBarViewModel presetBarViewModel,
            @NonNull final ToolManagerViewModel toolManagerViewModel,
            @NonNull final ViewGroup container) {
        this(lifecycleOwner, annotationToolbarViewModel, presetBarViewModel, toolManagerViewModel, new AnnotationToolbarView(container));
    }

    public AnnotationToolbarComponent(@NonNull final LifecycleOwner lifecycleOwner,
            @NonNull final AnnotationToolbarViewModel annotationToolbarViewModel,
            @NonNull final PresetBarViewModel presetBarViewModel,
            @NonNull final ToolManagerViewModel toolManagerViewModel,
            @NonNull final AnnotationToolbarView view) {

        mLifecycleOwner = lifecycleOwner;
        mContext = view.getContext();
        mForceSameNextToolMode = PdfViewCtrlSettingsManager.getContinuousAnnotationEdit(mContext);
        mAnnotationToolbarView = view;
        mAnnotationToolbarViewModel = annotationToolbarViewModel;
        mPresetBarViewModel = presetBarViewModel;
        mToolManagerViewModel = toolManagerViewModel;

        annotationToolbarViewModel.observeBuilderState(lifecycleOwner, new Observer<AnnotationToolbarBuilder>() {
            @Override
            public void onChanged(AnnotationToolbarBuilder builder) {

                mToolbarItemIdMap.clear();
                // Clear any existing toolbar states
                clearOptionalToolbarContainer();
                // Clear any visibility changes
                setToolRegionVisible(true);
                // Hide empty tool text view
                setEmptyToolTextVisible(false);

                // Repopulate toolbar item ID map
                List<ToolbarItem> toolbarItems = builder.getToolbarItems();
                List<ToolbarItem> stickyItems = builder.getStickyToolbarItems();
                List<ToolbarItem> leadingStickyItems = builder.getLeadingStickyToolbarItems();
                List<ToolbarItem> buttonItems = new ArrayList<>();
                buttonItems.addAll(toolbarItems);
                buttonItems.addAll(stickyItems);
                buttonItems.addAll(leadingStickyItems);
                for (ToolbarItem item : buttonItems) {
                    mToolbarItemIdMap.put(item.buttonId, item);
                }

                // If view toolbar, then hide annotation toolbar otherwise show it
                if (builder.getToolbarTag().equals(DefaultToolbars.TAG_VIEW_TOOLBAR)) {
                    if (mCompactMode) {
                        // in 1-line mode, we want to avoid animation
                        mAnnotationToolbarView.hide(false);
                    } else {
                        slideOut();
                    }
                } else {
                    if (mCompactMode) {
                        // in 1-line mode, we want to avoid animation
                        mAnnotationToolbarView.show(false);
                    } else {
                        slideIn();
                    }
                }

                // Special case for favorite toolbar, if empty we add a button to the toolbar
                if (builder.getToolbarTag().equals(DefaultToolbars.TAG_FAVORITE_TOOLBAR)) {
                    List<ToolbarItem> builderToolbarItems = builder.getToolbarItems();
                    boolean hasItems = false;
                    for (ToolbarItem toolbarItem : builderToolbarItems) {
                        if (toolbarItem.buttonId != DefaultToolbars.ButtonId.CUSTOMIZE.value()) {
                            hasItems = true;
                            break;
                        }
                    }
                    if (!hasItems) {
                        if (mCompactMode) {
                            ActionButton add = new ActionButton(mContext);
                            add.setIcon(mContext.getResources().getDrawable(R.drawable.ic_toolbar_customization));
                            add.setId(AnnotStyle.CUSTOM_EDIT_TOOLBAR);
                            add.setCheckable(false);
                            add.setShowIconHighlightColor(true);
                            add.setAlwaysShowIconHighlightColor(true);
                            TooltipCompat.setTooltipText(add, mContext.getResources().getString(R.string.action_edit_menu));
                            AnnotationToolbarTheme annotationToolbarTheme = AnnotationToolbarTheme.fromContext(mContext);
                            add.setIconHighlightColor(annotationToolbarTheme.highlightIconColor);
                            add.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    handleAddFavoriteMenuClicked(v);
                                }
                            });
                            // add icon
                            addToolbarActionsRightOptionalContainer(add);
                            // add empty text
                            setEmptyToolText(R.string.action_add_to_favorites);
                            setEmptyToolTextVisible(true);
                            setEmptyToolTextOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    handleAddFavoriteMenuClicked(v);
                                }
                            });
                        } else {
                            AppCompatButton add = AnnotationToolbarTextButtonInflater.inflate(mContext, R.string.add);
                            add.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    handleAddFavoriteMenuClicked(v);
                                }
                            });
                            addToolbarLeftOptionalContainer(add);
                        }
                        setToolRegionVisible(false);

                        annotationToolbarViewModel.observeHiddenButtonTypesState(lifecycleOwner, new Observer<AnnotationToolbarViewModel.DisabledButtonTypes>() {
                            @Override
                            public void onChanged(AnnotationToolbarViewModel.DisabledButtonTypes disabledButtonTypes) {

                                // Determine which menu items to hide
                                Collection<ToolbarItem> toolbarItems = mToolbarItemIdMap.values();
                                for (ToolbarItem toolbarItem : toolbarItems) {
                                    boolean isVisible = !disabledButtonTypes.getToolbarButtonTypesToHide().contains(toolbarItem.toolbarButtonType);
                                    view.setItemVisibility(toolbarItem.buttonId, isVisible);
                                }
                            }
                        });
                    }
                } else if (builder.getToolbarTag().equals(DefaultToolbars.TAG_REDACTION_TOOLBAR)) { // Special case for redaction toolbar
                    buildRedactionApplyButton();
                }

                for (OnPreBuildToolbarListener listener : mOnPreBuildToolbarListeners) {
                    listener.onPreBuildToolbar(builder);
                }

                mAnnotationToolbarView.inflateWithBuilder(builder);

                // Select last used tool
                int lastUsedButtonId = getLastUsedToolInSharedPrefs();
                ToolbarItem toolbarItem = mToolbarItemIdMap.get(lastUsedButtonId);
                if (toolbarItem != null) {
                    // Select tool in tool manager
                    onMenuClicked(toolbarItem);
                    // Also select tool in toolbar view
                    mAnnotationToolbarView.toggleToolbarButtons(toolbarItem);
                } else {
                    toolbarItem = ToolbarItem.DEFAULT_PAN_TOOl;
                    // Select tool in tool manager
                    onMenuClicked(toolbarItem);
                    // Also select tool in toolbar view
                    mAnnotationToolbarView.toggleToolbarButtons(toolbarItem);
                }

                HashMap<String, String> metadata = new HashMap<>();
                metadata.put(EventHandler.TOOLBAR_METADATA_KEY, builder.getToolbarTag());
                // here we will ignore the result because we will allow switching regardless of the result
                EventHandler.sendPreEvent(
                        EventHandler.ANNOT_TOOLBAR_EVENT,
                        metadata
                );

                for (OnToolbarChangedListener listener : mOnToolbarChangedListeners) {
                    listener.onToolbarChanged(builder.getToolbarTag());
                }
            }
        });

        annotationToolbarViewModel.observeDisabledToolModesState(lifecycleOwner, new Observer<AnnotationToolbarViewModel.DisabledToolModes>() {
            @Override
            public void onChanged(AnnotationToolbarViewModel.DisabledToolModes disabledToolModes) {
                updateToolbarState();
            }
        });

        annotationToolbarViewModel.observeHiddenButtonTypesState(lifecycleOwner, new Observer<AnnotationToolbarViewModel.DisabledButtonTypes>() {
            @Override
            public void onChanged(AnnotationToolbarViewModel.DisabledButtonTypes disabledButtonTypes) {

                // Determine which menu items to hide
                Collection<ToolbarItem> toolbarItems = mToolbarItemIdMap.values();
                for (ToolbarItem toolbarItem : toolbarItems) {
                    boolean isVisible = !disabledButtonTypes.getToolbarButtonTypesToHide().contains(toolbarItem.toolbarButtonType);
                    mAnnotationToolbarView.setItemVisibility(toolbarItem.buttonId, isVisible);
                }
            }
        });

        mAnnotationToolbarView.addOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                ToolbarItem toolbarItem = mToolbarItemIdMap.get(itemId);
                // Try to intercept it in the listener
                boolean handled = false;
                for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                    handled = handled || listener.onInterceptItemClick(toolbarItem, item);
                }
                if (!handled) {
                    for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                        listener.onPreItemClick(toolbarItem, item);
                    }
                    // If not intercept in listener, then handle it normally
                    if (toolbarItem != null) {
                        if (item.isEnabled()) {
                            if (item.isCheckable() && item.isChecked()) {
                                onMenuClicked(ToolbarItem.DEFAULT_PAN_TOOl);
                                if (!Utils.isTablet(mContext)) {
                                    // Send analytics for toggle closed tool
                                    AnalyticsHandlerAdapter.getInstance().sendEvent(
                                            AnalyticsHandlerAdapter.EVENT_NEW_ANNOTATION_TOOLBAR_TOOL_CLOSE,
                                            AnalyticsParam.toolToggleClose(true)
                                    );
                                }
                            } else {
                                onMenuClicked(toolbarItem);
                                AnalyticsHandlerAdapter.getInstance().sendEvent(
                                        AnalyticsHandlerAdapter.EVENT_NEW_ANNOTATION_TOOLBAR,
                                        AnalyticsParam.annotationToolbarParam(toolbarItem)
                                );
                            }
                            handled = true;
                        }
                    }
                    for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                        listener.onPostItemClick(toolbarItem, item);
                    }
                }
                return handled;
            }
        });

        mAnnotationToolbarView.addOnButtonLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if (v.getId() == DefaultToolbars.ButtonId.UNDO.value()) {
                    showUndoRedoPopup(v);
                    return true;
                }
                return false;
            }
        });

        mToolManagerViewModel.observeToolManagerChanges(lifecycleOwner, new Observer<ToolManager>() {

            @Override
            public void onChanged(final ToolManager toolManager) {
                if (toolManager != null) {
                    UndoRedoManager undoRedoManger = toolManager.getUndoRedoManger();
                    undoRedoManger.removeUndoRedoStateChangeListener(mListener);
                    mListener = new UndoRedoManager.UndoRedoStateChangeListener() {
                        @Override
                        public void onStateChanged() {
                            // Update undo/redo based on current undo/redo state and tool manager settings
                            UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
                            if (undoRedoManager.canUndo() && toolManager.isShowUndoRedo()) {
                                setItemEnabled(DefaultToolbars.ButtonId.UNDO.value(), true);
                            } else {
                                setItemEnabled(DefaultToolbars.ButtonId.UNDO.value(), false);
                            }

                            if (undoRedoManager.canRedo() && toolManager.isShowUndoRedo()) {
                                setItemEnabled(DefaultToolbars.ButtonId.REDO.value(), true);
                            } else {
                                setItemEnabled(DefaultToolbars.ButtonId.REDO.value(), false);
                            }
                        }
                    };
                    undoRedoManger.addUndoRedoStateChangeListener(mListener);
                }
            }
        });

        mToolManagerViewModel.observeToolChanges(lifecycleOwner, new Observer<ToolManagerViewModel.ToolChange>() {
            @Override
            public void onChanged(ToolManagerViewModel.ToolChange toolChange) {
                // update view if tool changes accordingly
                if (toolChange != null) {
                    Tool newTool = toolChange.newTool;
                    Tool oldTool = toolChange.oldTool;
                    if (newTool != null) {
                        if (newTool.getToolMode() == ToolManager.ToolMode.PAN) {
                            // If we change to pan tool, also close edit toolbar if showing
                            if (mEditToolbarImpl != null) {
                                closeEditToolbar();
                            }
                            mAnnotationToolbarView.deselectAllToolbarButtons();
                            setLastUsedToolInSharedPrefs(-1);
                        } else if (oldTool != null && oldTool.getToolMode() == ToolManager.ToolMode.ANNOT_EDIT &&
                                (newTool instanceof RubberStampCreate || newTool instanceof Signature)) {
                            // here new tool is created due to continuous annotating
                            PresetBarState presetBarState = mPresetBarViewModel.getPresetBarState();
                            if (presetBarState != null) {
                                Pair<PresetButtonState, Integer> presetPair = presetBarState.getActivePresetState();
                                if (presetPair != null && presetPair.first != null &&
                                        presetPair.first.getAnnotStyles() != null &&
                                        !presetPair.first.getAnnotStyles().isEmpty()) {
                                    newTool.setupAnnotProperty(presetPair.first.getAnnotStyles().get(0));
                                }
                            }
                        } else {
                            if (newTool instanceof AdvancedShapeCreate) {
                                ((AdvancedShapeCreate) newTool).setOnEditToolbarListener(AnnotationToolbarComponent.this);
                            }
                            ToolbarItem newToolbarItem = newTool.getBundle() == null ? null : (ToolbarItem) newTool.getBundle().getParcelable(TOOLBAR_ITEM_BUNDLE);
                            if (newToolbarItem != null) {
                                mAnnotationToolbarView.toggleToolbarButtons(newToolbarItem);
                            }
                        }
                    }
                } else {
                    // Select last used tool when toolbar is initialized
                    int lastUsedButtonId = getLastUsedToolInSharedPrefs();
                    ToolbarItem toolbarItem = mToolbarItemIdMap.get(lastUsedButtonId);
                    if (toolbarItem != null) {
                        // Select tool in tool manager
                        onMenuClicked(toolbarItem);
                        // Also select tool in toolbar view
                        mAnnotationToolbarView.toggleToolbarButtons(toolbarItem);
                    }
                }
            }
        });

        mToolManagerViewModel.observeToolSet(lifecycleOwner, new Observer<ToolManagerViewModel.ToolSet>() {
            @Override
            public void onChanged(ToolManagerViewModel.ToolSet toolSet) {
                if (toolSet != null) {
                    Tool newTool = toolSet.newTool;
                    if (newTool instanceof SmartPenInk && mSelectedToolMode == ToolManager.ToolMode.SMART_PEN_INK) { // only apply when the tool is selected
                        int lastUsedButtonId = getLastUsedToolInSharedPrefs();
                        ToolbarItem toolbarItem = mToolbarItemIdMap.get(lastUsedButtonId);
                        Bundle bundle = new Bundle();
                        bundle.putParcelable(TOOLBAR_ITEM_BUNDLE, toolbarItem);
                        bundle.putBoolean(TOOLMODE_DISABLED, getToolManager().isToolModeDisabled(ToolManager.ToolMode.SMART_PEN_INK));
                        initInkEditToolbar(ToolManager.ToolMode.SMART_PEN_INK, bundle);
                    }
                }
            }
        });

        mPresetBarViewModel.observePresetState(lifecycleOwner, new Observer<PresetBarState>() {
            @Override
            public void onChanged(PresetBarState presetState) {
                for (int i = 0; i < presetState.getNumberOfPresetStates(); i++) {
                    if (presetState.isPresetSelected(i)) {
                        PresetButtonState state = presetState.getPresetState(i);
                        ArrayList<AnnotStyle> annotStyles = state.getAnnotStyles();
                        if (annotStyles != null && !annotStyles.isEmpty()) {
                            // Set style for tool
                            if (getToolManager() != null) {
                                ((Tool) mToolManagerViewModel.getTool()).setupAnnotStyles(annotStyles);
                            }
                            AnnotStyle annotStyle = annotStyles.get(0);
                            mAnnotationToolbarView.updateAccentButton(
                                    presetState.getButtonId(),
                                    ActionButton.getPreviewColor(annotStyle),
                                    getPreviewAlpha(annotStyle)
                            );
                        }
                        break;
                    }
                }
            }
        });
    }

    @Nullable
    private ToolManager getToolManager() {
        return mToolManagerViewModel == null ? null : mToolManagerViewModel.getToolManager();
    }

    private void handleAddFavoriteMenuClicked(View v) {
        // Create a menu item programmatically. For convenience, we'll use the quick menu item
        // since all we need to have is the menu id
        MenuItem menuItem = new QuickMenuItem(v.getContext(), DefaultToolbars.ButtonId.CUSTOMIZE.value());
        menuItem.setVisible(false);

        // Try to intercept it in the listener
        boolean handled = false;
        for (AnnotationButtonClickListener listener : mButtonClickListeners) {
            handled = handled || listener.onInterceptItemClick(null, menuItem);
        }

        boolean intercept = EventHandler.sendPreEvent(EventHandler.FAVORITE_TOOLBAR_EVENT);

        if (!handled && !intercept) {
            for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                listener.onPreItemClick(null, menuItem);
            }
            for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                listener.onPostItemClick(null, menuItem);
            }
        }
    }

    private void onMenuClicked(@NonNull ToolbarItem toolbarItem) {
        if (mContext == null || getToolManager() == null) {
            return;
        }

        if (!getToolManager().getPDFViewCtrl().isValid()) {
            return;
        }
        mSelectedToolMode = null;

        ToolbarButtonType buttonType = toolbarItem.toolbarButtonType;
        Tool currentTool = (Tool) mToolManagerViewModel.getTool();

        if (currentTool != null) {
            ToolManager.ToolMode currentToolMode = ToolManager.getDefaultToolMode(currentTool.getToolMode());
            if (Utils.isAnnotationHandlerToolMode(currentToolMode) ||
                    currentToolMode == ToolManager.ToolMode.TEXT_CREATE ||
                    currentToolMode == ToolManager.ToolMode.CALLOUT_CREATE ||
                    currentToolMode == ToolManager.ToolMode.PAN) {
                getToolManager().onClose();
            }
        }

        ToolManager.ToolMode toolMode = ToolModeMapper.getToolMode(buttonType);
        // Three cases, button click is a tool, button click is undo/redo, button click is not a tool not undo/redo
        if (buttonType == ToolbarButtonType.UNDO || buttonType == ToolbarButtonType.REDO) {
            switch (buttonType) {
                case REDO:
                    redo();
                    break;
                case UNDO:
                    undo();
                    break;
            }
        } else if (toolMode != null) { // if tool, then we handle interaction internally
            Tool tool = null;
            Bundle bundle = new Bundle();
            bundle.putParcelable(TOOLBAR_ITEM_BUNDLE, toolbarItem);
            bundle.putBoolean(TOOLMODE_DISABLED, getToolManager().isToolModeDisabled(toolMode));

            // Always close edit toolbar before switching tools
            if (mEditToolbarImpl != null) {
                closeEditToolbar();
            }

            mSelectedToolMode = toolMode;

            if (mToolManagerViewModel.getTool() != null) {
                ((Tool) mToolManagerViewModel.getTool()).setCurrentDefaultToolModeHelper(null);
            }

            switch (buttonType) {
                case MULTI_SELECT:
                case LASSO_SELECT: {
                    if (buttonType == ToolbarButtonType.MULTI_SELECT) {
                        getToolManager().setMultiSelectMode(AnnotEditRectGroup.SelectionMode.RECTANGULAR);
                    } else {
                        getToolManager().setMultiSelectMode(AnnotEditRectGroup.SelectionMode.LASSO);
                    }
                    tool = (Tool) getToolManager().createTool(toolMode, null, bundle);
                    getToolManager().setTool(tool); // reset
                    tool.setForceSameNextToolMode(mForceSameNextToolMode);
                    break;
                }
                case POLY_CLOUD:
                case POLYGON:
                case POLYLINE:
                case AREA:
                case PERIMETER: {
                    tool = (Tool) getToolManager().createTool(toolMode, mToolManagerViewModel.getTool(), bundle);
                    getToolManager().setTool(tool);
                    if (tool instanceof AdvancedShapeCreate) {
                        ((AdvancedShapeCreate) tool).setOnEditToolbarListener(this);
                    }
                    break;
                }
                case PAN: {
                    tool = (Tool) getToolManager().createTool(toolMode, mToolManagerViewModel.getTool(), bundle);
                    getToolManager().setTool(tool);
                    ((Tool) tool).setForceSameNextToolMode(false);
                    break;
                }
                case INK:
                case SMART_PEN: {
                    tool = (Tool) getToolManager().createTool(toolMode, mToolManagerViewModel.getTool(), bundle);
                    getToolManager().setTool(tool);
                    ((Tool) tool).setForceSameNextToolMode(mForceSameNextToolMode);
                    initInkEditToolbar(toolMode, bundle);
                    break;
                }
                case RECT_AREA:
                case FREE_TEXT_SPACING:
                case DATE:
                case LINE:
                case ARROW:
                case IMAGE:
                case RULER:
                case SOUND:
                case CIRCLE:
                case ERASER:
                case SQUARE:
                case CALLOUT:
                case FREE_TEXT:
                case SIGNATURE:
                case STAMP:
                case CROSS:
                case CHECKMARK:
                case DOT:
                case STICKY_NOTE:
                case TEXT_SQUIGGLY:
                case FREE_HIGHLIGHT:
                case TEXT_HIGHLIGHT:
                case TEXT_STRIKEOUT:
                case TEXT_UNDERLINE:
                case SIGNATURE_FIELD:
                case RADIO_BUTTON:
                case CHECKBOX:
                case LIST_BOX:
                case COMBO_BOX:
                case TEXT_FIELD:
                case ATTACHMENT:
                case TEXT_REDACTION:
                case RECT_REDACTION:
                case COUNT_MEASUREMENT:
                case LINK: {
                    tool = (Tool) getToolManager().createTool(toolMode, mToolManagerViewModel.getTool(), bundle);
                    getToolManager().setTool(tool);
                    ((Tool) tool).setForceSameNextToolMode(mForceSameNextToolMode);
                    if (tool instanceof AdvancedShapeCreate) {
                        ((AdvancedShapeCreate) tool).setOnEditToolbarListener(this);
                    }
                    break;
                }
                case CUSTOM_UNCHECKABLE:
                case CUSTOM_CHECKABLE: {
                    // do nothing, handled outside
                    break;
                }
                default: {
                    throw new RuntimeException("Undefined Tool Type");
                }
            }

            if (buttonType == ToolbarButtonType.PAN) {
                setLastUsedToolInSharedPrefs(-1);
            } else {
                setLastUsedToolInSharedPrefs(toolbarItem.buttonId);
            }
        } else { // not a tool, so just select the button if possible and handle the interaction else where
            mAnnotationToolbarView.toggleToolbarButtons(toolbarItem);
        }
    }

    private void initInkEditToolbar(ToolManager.ToolMode toolMode, Bundle bundle) {
        FragmentActivity activity = getToolManager().getCurrentActivity();
        // Work around for undo/redo
        if (activity != null) {
            mEditToolbarImpl = new DummyEditToolbarImpl(
                    activity,
                    new DummyEditToolbar(),
                    getToolManager(),
                    toolMode,
                    null,
                    0,
                    true,
                    bundle
            );

            getToolManager().getUndoRedoManger().setEditToolbarImpl(mEditToolbarImpl);
        }
    }

    private void undo() {
        ToolManager toolManager = getToolManager();
        if (toolManager == null || toolManager.getPDFViewCtrl() == null) {
            return;
        }
        PDFViewCtrl mPdfViewCtrl = toolManager.getPDFViewCtrl();

        UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
        if (undoRedoManager != null && undoRedoManager.canUndo()) {
            String undoInfo = undoRedoManager.undo(AnalyticsHandlerAdapter.LOCATION_VIEWER, false);
            UndoRedoManager.jumpToUndoRedo(mPdfViewCtrl, undoInfo, true);

            // if we are in handler tools, should go back to default tool
            if (Utils.isAnnotationHandlerToolMode(ToolManager.getDefaultToolMode(toolManager.getTool().getToolMode()))) {
                toolManager.backToDefaultTool();
            }
        }
    }

    private void redo() {
        ToolManager toolManager = getToolManager();
        if (toolManager == null || toolManager.getPDFViewCtrl() == null) {
            return;
        }
        PDFViewCtrl mPdfViewCtrl = toolManager.getPDFViewCtrl();

        UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
        if (undoRedoManager != null && undoRedoManager.canRedo()) {
            String redoInfo = undoRedoManager.redo(AnalyticsHandlerAdapter.LOCATION_VIEWER, false);
            UndoRedoManager.jumpToUndoRedo(mPdfViewCtrl, redoInfo, false);

            // if we are in handler tools, should go back to default tool
            if (Utils.isAnnotationHandlerToolMode(ToolManager.getDefaultToolMode(toolManager.getTool().getToolMode()))) {
                toolManager.backToDefaultTool();
            }
        }
    }

    /**
     * In compact mode, the annotation toolbar will prepend toolbar switcher in front of scrollable tools.
     * In non-compact mode, it is assumed that toolbar switcher is located somewhere else in the viewer
     *
     * @param compactMode true if in compact mode, false otherwise. Default to false.
     */
    public void setCompactMode(boolean compactMode) {
        mCompactMode = compactMode;
        mAnnotationToolbarView.setCompactMode(compactMode);
    }

    public void setToolbarHeight(int height) {
        mAnnotationToolbarView.setToolbarHeight(height);
    }

    public void setNavigationIconProperty(int paddingLeft, int minWidth) {
        mAnnotationToolbarView.setNavigationIconProperty(paddingLeft, minWidth);
    }

    /**
     * Used only in compact mode, sets the navigation icon for edit toolbar.
     *
     * @param icon the icon resource.
     */
    public void setNavigationIcon(@DrawableRes int icon) {
        mNavigationIcon = icon;
        mAnnotationToolbarView.setNavigationIcon(icon);
    }

    /**
     * Used only in compact mode, sets the visibility of navigation icon for edit toolbar.
     *
     * @param visible true if visible, false otherwise.
     */
    public void setNavigationIconVisible(boolean visible) {
        mNavigationIconVisible = visible;
        mAnnotationToolbarView.setNavigationIconVisible(visible);
    }

    /**
     * Used only in compact mode, sets the visibility of toolbar switcher.
     *
     * @param visible true if visible, false otherwise.
     */
    public void setToolbarSwitcherVisible(boolean visible) {
        mAnnotationToolbarView.setToolbarSwitcherVisible(visible);
    }

    /**
     * Clears current state and inflates the annotation toolbar view with the given {@link AnnotationToolbarBuilder}.
     *
     * @param builder the builder that defines a new annotation toolbar state
     */
    public void inflateWithBuilder(AnnotationToolbarBuilder builder) {
        mAnnotationToolbarViewModel.setAnnotationToolbarBuilder(builder);
    }

    private int getPreviewAlpha(@NonNull AnnotStyle annotStyle) {
        return (int) (annotStyle.getOpacity() * 255);
    }

    public void hide(boolean animated) {
        mAnnotationToolbarView.hide(animated);
    }

    public void show(boolean animated) {
        mAnnotationToolbarView.show(animated);
    }

    public void slideOut() {
        mAnnotationToolbarView.hide(true);
    }

    public void slideIn() {
        mAnnotationToolbarView.show(true);
    }

    public boolean isEditing() {
        return mEditToolbarImpl != null;
    }

    private boolean isInEditMode() {
        return mEditToolbarImpl != null && mEditToolbarImpl.isToolbarShown();
    }

    /**
     * Show edit toolbar either for an existing annotation on a given page, or a brand new
     * annotation (i.e. when annot == null). After the edit toolbar is dismissed
     * the tool will still be selected and you can keep annotating.
     *
     * @param toolMode The tool mode that should be selected when open edit toolbar
     * @param annot    The selected annotation
     * @param pageNum  The page of the selected anntoation
     */
    @Override
    public void showEditToolbar(
            @NonNull ToolManager.ToolMode toolMode,
            @Nullable Annot annot,
            int pageNum) {
        showEditToolbar(toolMode, annot, pageNum, new Bundle(), true);
    }

    /**
     * Show edit toolbar either for an existing annotation on a given page, or a brand new
     * annotation (i.e. when annot == null). If keepAnnotatingAfterDismiss is true, then keep annotating
     * after the edit toolbar is dismissed, otherwise switch to pan tool when edit toolbar is dismissed.
     *
     * @param toolMode                   The tool mode that should be selected when open edit toolbar
     * @param annot                      The selected annotation
     * @param pageNum                    The page of the selected anntoation
     * @param keepAnnotatingAfterDismiss whether we should keep annotating after the edit toolbar is dismissed
     */
    public void showEditToolbar(
            @NonNull ToolManager.ToolMode toolMode,
            @Nullable Annot annot,
            int pageNum,
            final boolean keepAnnotatingAfterDismiss) {
        showEditToolbar(toolMode, annot, pageNum, new Bundle(), keepAnnotatingAfterDismiss);
    }

    protected void showEditToolbar(
            @NonNull ToolManager.ToolMode toolMode,
            @Nullable Annot annot,
            int pageNum,
            @NonNull Bundle bundle,
            final boolean keepAnnotatingAfterDismiss) {
        if (getToolManager() == null) {
            return;
        }
        FragmentActivity activity = getToolManager().getCurrentActivity();
        if (activity == null || isInEditMode()) {
            return;
        }

        // First remove any edit toolbars if available
        if (mEditToolbar != null) {
            ViewGroup parent = (ViewGroup) mEditToolbar.getParent();
            if (parent != null) {
                parent.removeView(mEditToolbar);
            }
        }

        if (mEditToolbar == null) {
            mEditToolbar = createEditToolbar();
            mEditToolbar.show();
        }

        // If ink toolbar, we also add delete and eraser buttons
        mEditToolbar.inflateDefaultEditToolbar(toolMode);

        if (annot == null) {
            mEditToolbar.setEditingAnnotation(false);
        } else {
            mEditToolbar.setEditingAnnotation(true);
        }

        // Hide any hidden buttons
        mAnnotationToolbarViewModel.observeHiddenButtonTypesState(mLifecycleOwner, new Observer<AnnotationToolbarViewModel.DisabledButtonTypes>() {
            @Override
            public void onChanged(AnnotationToolbarViewModel.DisabledButtonTypes disabledButtonTypes) {

                // AnnotationToolbarViewModel build state does not observe every possible state (such as toolbars being drawn over toolbars)
                // When annotation toolbar switches to single button toolbar, the toolbar is only being drawn on top of previous annotation toolbar
                // observeBuilderState does not call onChanged when the Single Button Toolbar is being shown
                // Buttons that aren't in the previous annotation toolbar do not get added to the collection, requires getting the buttons from the mEditToolbar
                Collection<ToolbarItem> toolbarItems = mEditToolbar.getButtonItems();
                // Determine which menu items to hide
                for (ToolbarItem toolbarItem : toolbarItems) {
                    boolean isVisible = !disabledButtonTypes.getToolbarButtonTypesToHide().contains(toolbarItem.toolbarButtonType);
                    mEditToolbar.setItemVisibility(toolbarItem.buttonId, isVisible);
                }
            }
        });

        mAnnotationToolbarView.addToolbarOverlay(mEditToolbar);
        mEditToolbarImpl = new EditToolbarImpl(activity, mEditToolbar, getToolManager(), toolMode, annot, pageNum, true, bundle);
        mEditToolbarImpl.setOnEditToolbarListener(new EditToolbarImpl.OnEditToolbarListener() {
            @Override
            public void onEditToolbarDismissed() {
                AnnotationToolbarComponent.this.onEditToolbarDismissed(keepAnnotatingAfterDismiss);
            }
        });
        mEditToolbarImpl.showToolbar();
    }

    @NonNull
    protected SingleButtonToolbar createEditToolbar() {
        final SingleButtonToolbar editToolbar = new SingleButtonToolbar(mContext);
        editToolbar.setCompactMode(mCompactMode);
        if (mCompactMode) {
            editToolbar.setNavigationIcon(mNavigationIcon);
            editToolbar.setNavigationIconVisible(mNavigationIconVisible);
        }
        editToolbar.setButtonText(mContext.getResources().getString(R.string.done));
        editToolbar.addOnButtonClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeEditToolbar();
            }
        });

        // Only add delete and eraser for ink tool
        editToolbar.addOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == DefaultToolbars.ButtonId.ERASER.value()) {
                    if (item.isChecked()) {
                        editToolbar.deselectAllTools();
                        mEditToolbarImpl.onDrawSelected(0, false, item.getActionView());
                    } else {
                        editToolbar.toggleToolbarButtons(id);
                        mEditToolbarImpl.onEraserSelected(false, item.getActionView());
                    }
                }
                return false;
            }
        });

        return editToolbar;
    }

    @Override
    public void closeEditToolbar() {
        if (mEditToolbarImpl != null) {
            if (mEditToolbarImpl.getToolMode() == ToolManager.ToolMode.INK_CREATE ||
                    mEditToolbarImpl.getToolMode() == ToolManager.ToolMode.SMART_PEN_INK) {
                setLastUsedToolInSharedPrefs(-1);
            }
            EditToolbarImpl impl = mEditToolbarImpl;
            mEditToolbarImpl = null; // close will trigger a tool change which will call close again prior to nulling it
            impl.close();
        }

        if (mEditToolbar != null) {
            ViewGroup parent = (ViewGroup) mEditToolbar.getParent();
            if (parent != null) {
                parent.removeView(mEditToolbar);
            }
        }
    }

    private void onEditToolbarDismissed(boolean keepAnnotatingAfterDismiss) {
        if (getToolManager() == null) {
            return;
        }
        Tool currentTool = (Tool) mToolManagerViewModel.getTool();
        if (currentTool == null) {
            return;
        }
        currentTool.setForceSameNextToolMode(false);

        ToolManager.ToolMode toolMode = ToolManager.getDefaultToolMode(currentTool.getToolMode());
        switch (toolMode) {
            case INK_CREATE:
            case SMART_PEN_INK: {
                // For ink toolbar: Always select the pan tool when the edit toolbar is dismissed, because the
                // edit toolbar is never launched from the annotation toolbar
                Bundle bundle = new Bundle();
                bundle.putParcelable(TOOLBAR_ITEM_BUNDLE, ToolbarItem.DEFAULT_PAN_TOOl);
                Tool panTool = (Tool) getToolManager().createTool(ToolManager.ToolMode.PAN, mToolManagerViewModel.getTool(), bundle);
                getToolManager().setTool(panTool);
            }
            break;
            case POLYGON_CREATE:
            case POLYLINE_CREATE:
            case CLOUD_CREATE:
            case AREA_MEASURE_CREATE:
            case PERIMETER_MEASURE_CREATE:
                // For poly toolbars: Reselect the current tool if we should keep annotating after completion,
                // othewise we switch back to the pan tool
                if (keepAnnotatingAfterDismiss) {
                    Bundle currentBundle = currentTool.getBundle();
                    Tool newTool = (Tool) getToolManager().createTool(toolMode, mToolManagerViewModel.getTool(), currentBundle);
                    getToolManager().setTool(newTool);
                    if (newTool instanceof AdvancedShapeCreate) {
                        ((AdvancedShapeCreate) newTool).setOnEditToolbarListener(this);
                    }
                } else {
                    Bundle bundle = new Bundle();
                    bundle.putParcelable(TOOLBAR_ITEM_BUNDLE, ToolbarItem.DEFAULT_PAN_TOOl);
                    Tool panTool = (Tool) getToolManager().createTool(ToolManager.ToolMode.PAN, mToolManagerViewModel.getTool(), bundle);
                    getToolManager().setTool(panTool);
                }
                break;
        }
        mAnnotationToolbarView.show(false);
    }

    public void setItemVisibility(int buttonId, boolean isVisible) {
        mAnnotationToolbarView.setItemVisibility(buttonId, isVisible);
    }

    /**
     * Sets whether a button is enabled in the toolbar. If the button is enabled, then it will be
     * clickable otherwise touch events are ignored.
     *
     * @param buttonId  unique id of the button
     * @param isEnabled whether the button should be enabled
     */
    public void setItemEnabled(int buttonId, boolean isEnabled) {
        mAnnotationToolbarView.setItemEnabled(buttonId, isEnabled);
    }

    /**
     * Sets whether a button appears disabled in the toolbar.
     * The button is still clickable
     *
     * @param buttonId            unique id of the button
     * @param isAppearanceEnabled whether the button should be appear disabled
     */
    public void setItemAppearanceEnabled(int buttonId, boolean isAppearanceEnabled) {
        mAnnotationToolbarView.setItemAppearanceEnabled(buttonId, isAppearanceEnabled);
    }

    public void disableAllItems() {
        mAnnotationToolbarView.disableAllItems();
    }

    /**
     * Clears annotation toolbar state and deselects any selected tools. By default, when no
     * tools are selected then the pan tool will be used.
     */
    public void clearState() {
        // Simply set default pan tool, all states should be cleared
        ToolManager toolManager = getToolManager();
        if (toolManager != null) {
            Bundle bundle = new Bundle();
            bundle.putParcelable(TOOLBAR_ITEM_BUNDLE, ToolbarItem.DEFAULT_PAN_TOOl);
            Tool panTool = (Tool) toolManager.createTool(ToolManager.ToolMode.PAN, mToolManagerViewModel.getTool(), bundle);
            toolManager.setTool(panTool);
        }
    }

    /**
     * Adds a view on top of the annotation toolbar in the non-sticky button area. This is used
     * for custom annotation toolbars that do not make use of tool buttons.
     *
     * @param view the view to overlay on top of the annotation toolbar
     */
    public void addToolbarOverlay(@NonNull View view) {
        mAnnotationToolbarView.addToolbarOverlay(view);
    }

    /**
     * Adds a view to the left of the non-sticky tools in the annotation toolbar in the non-sticky button area.
     * This is used for custom annotation toolbars that require additional UI components that are not tool buttons
     *
     * @param view the view to add to the container to the left of non-sticky tools
     */
    public void addToolbarLeftOptionalContainer(@NonNull View view) {
        mAnnotationToolbarView.addToolbarLeftOptionalContainer(view);
    }

    /**
     * Adds a view to the right of the non-sticky tools in the annotation toolbar in the non-sticky button area.
     * This is used for custom annotation toolbars that require additional UI components that are not tool buttons
     *
     * @param view the view to add to the container to the right of non-sticky tools
     */
    public void addToolbarActionsRightOptionalContainer(@NonNull View view) {
        mAnnotationToolbarView.addToolbarActionsRightOptionalContainer(view);
    }

    /**
     * Sets the layout gravity of toolbar items.
     *
     * @param layoutGravity gravity to set the toolbar items
     */
    public void setToolbarItemGravity(int layoutGravity) {
        mAnnotationToolbarView.setToolbarItemGravity(layoutGravity);
    }

    public AppCompatButton buildRedactionApplyButton() {
        AppCompatButton apply = AnnotationToolbarTextButtonInflater.inflate(mContext, R.string.apply);
        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean intercept = EventHandler.sendPreEvent(EventHandler.APPLY_REDACTION_EVENT);
                if (!intercept) {
                    ToolManager toolManager = getToolManager();
                    if (toolManager != null) {
                        toolManager.getRedactionManager().applyRedaction();
                    }
                }
            }
        });
        if (mCompactMode) {
            addToolbarActionsRightOptionalContainer(apply);
        } else {
            addToolbarLeftOptionalContainer(apply);
        }
        return apply;
    }

    /**
     * Sets the visibility of the tool region in the non-sticky button area.
     * This is used for special cases where tools are not ready to be shown yet.
     * Note, toolbar switcher will be shown in compact mode.
     *
     * @param visible true if the tool region is visible, false otherwise
     */
    public void setToolRegionVisible(boolean visible) {
        mAnnotationToolbarView.setToolRegionVisible(visible);
    }

    /**
     * Sets the string resource for empty text view
     *
     * @param emptyText the string resource
     */
    public void setEmptyToolText(@StringRes int emptyText) {
        mAnnotationToolbarView.setEmptyToolText(emptyText);
    }

    /**
     * Sets the visibility of the empty tool text view in the non-sticky button area.
     * This is used for special cases where no tools are in the toolbar.
     *
     * @param visible true if empty text view is visible, false otherwise
     */
    public void setEmptyToolTextVisible(boolean visible) {
        mAnnotationToolbarView.setEmptyToolTextVisible(visible);
    }

    /**
     * Sets the event when click on the empty tool text view in the non-sticky button area.
     *
     * @param listener the event
     */
    public void setEmptyToolTextOnClickListener(@Nullable View.OnClickListener listener) {
        mAnnotationToolbarView.setEmptyToolTextOnClickListener(listener);
    }

    /**
     * Removes the annotation toolbar's overlay view.
     */
    public void clearToolbarOverlayView() {
        mAnnotationToolbarView.clearToolbarOverlayView();
    }

    /**
     * Removes all child views from the optional contains which incldues the left container
     * and overlay views.
     */
    public void clearOptionalToolbarContainer() {
        mAnnotationToolbarView.clearToolbarOverlayView();
        mAnnotationToolbarView.clearOptionContainers();
    }

    /**
     * Selects the toolbar button if available. Button must be of selectable type,
     * see {@link AnnotationToolbarBuilder#addCustomSelectableButton(int, int, int)}.
     *
     * @param buttonId the unique id of the toolbar button
     */
    public void selectToolbarButton(int buttonId) {
        mAnnotationToolbarView.selectToolbarButtonIfAvailable(buttonId);
    }

    /**
     * Whether toolbars should re-select the last used tool when leave and returning back to the toolbar.
     *
     * @param lastUsedTool true if last used tool should be selected upon returning to the toolbar
     */
    public void rememberLastUsedTool(boolean lastUsedTool) {
        mLastUsedTool = lastUsedTool;
    }

    /**
     * Sets whether toolbar tool buttons should be visible in all toolbars
     *
     * @param buttonType that defines related tool buttons
     * @param visibility the visibility of these buttons in all toolbars
     */
    public void setToolbarButtonVisibility(@NonNull ToolbarButtonType buttonType, boolean visibility) {
        mAnnotationToolbarViewModel.setToolbarButtonVisibility(buttonType, visibility);
    }

    /**
     * Force annotation toolbar to update state.
     */
    public void updateToolbarState() {
        mAnnotationToolbarView.clearPreviousToolbarBuilder();
        mAnnotationToolbarViewModel.updateState();
    }

    public void setSelectedIconColor(int buttonId, @ColorInt int iconColor) {
        mAnnotationToolbarView.setSelectedIconColor(buttonId, iconColor);
    }

    public void setIconColor(int buttonId, @ColorInt int iconColor) {
        mAnnotationToolbarView.setIconColor(buttonId, iconColor);
    }

    /**
     * Defines tool modes that are used to filter out related tool buttons in the toolbar.
     * <p>
     * See {@link ToolModeMapper} for a map between tool mode and toolbar buttons
     *
     * @param disabledToolModes tool modes used to filter out related toolbar buttons
     */
    public void setToolModeFilter(@NonNull Set<ToolManager.ToolMode> disabledToolModes) {
        mAnnotationToolbarViewModel.setToolModeFilter(disabledToolModes);
    }

    private int getLastUsedToolInSharedPrefs() {
        if (mLastUsedTool) {
            return ToolbarSharedPreferences.getLastUsedTool(mContext);
        } else {
            return -1;
        }
    }

    private void setLastUsedToolInSharedPrefs(int buttonId) {
        if (mLastUsedTool) {
            ToolbarSharedPreferences.setLastUsedTool(mContext, buttonId);
        }
    }

    public void showUndoRedoPopup(final View anchor) {
        final PopupMenu popup = new PopupMenu(anchor.getContext(), anchor);
        popup.inflate(R.menu.menu_toolbar_undo_redo);

        // keep menu on screen after click
        final MenuItem menuUndo = popup.getMenu().findItem(R.id.undo);
        final MenuItem menuRedo = popup.getMenu().findItem(R.id.redo);
        ViewerUtils.keepOnScreenAfterClick(anchor.getContext(), menuUndo);
        ViewerUtils.keepOnScreenAfterClick(anchor.getContext(), menuRedo);

        updateUndoRedoPopupContent(menuUndo, menuRedo);

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.undo) {
                    ToolbarItem undoToolbarItem = new ToolbarItem(
                            "",
                            ToolbarButtonType.UNDO,
                            -1,
                            false,
                            false,
                            R.string.undo,
                            R.drawable.ic_undo_black_24dp,
                            MenuItem.SHOW_AS_ACTION_IF_ROOM,
                            0);
                    // HostFragment onOptionsItemSelected is called
                    for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                        listener.onPreItemClick(undoToolbarItem, menuItem);
                    }
                } else if (menuItem.getItemId() == R.id.redo) {
                    ToolbarItem redoToolbarItem = new ToolbarItem(
                            "",
                            ToolbarButtonType.REDO,
                            -1,
                            false,
                            false,
                            R.string.redo,
                            R.drawable.ic_redo_black_24dp,
                            MenuItem.SHOW_AS_ACTION_IF_ROOM,
                            0);
                    // HostFragment onOptionsItemSelected is called
                    for (AnnotationButtonClickListener listener : mButtonClickListeners) {
                        listener.onPreItemClick(redoToolbarItem, menuItem);
                    }
                }
                updateUndoRedoPopupContent(menuUndo, menuRedo);
                return false;
            }
        });
        if (popup.getMenu() instanceof MenuBuilder) {
            MenuPopupHelper menuHelper = new MenuPopupHelper(anchor.getContext(), (MenuBuilder) popup.getMenu(), anchor);
            menuHelper.setOnDismissListener(new PopupWindow.OnDismissListener() {
                @Override
                public void onDismiss() {
                    handleUndoRedoPopupDismiss();
                }
            });
            menuHelper.setForceShowIcon(true);
            menuHelper.show();
        } else {
            popup.setOnDismissListener(new PopupMenu.OnDismissListener() {
                @Override
                public void onDismiss(PopupMenu menu) {
                    handleUndoRedoPopupDismiss();
                }
            });
            popup.show();
        }
    }

    private void handleUndoRedoPopupDismiss() {
        ToolManager toolManager = getToolManager();
        if (toolManager != null) {
            toolManager.getUndoRedoManger().sendConsecutiveUndoRedoEvent();
        }
    }

    private void updateUndoRedoPopupContent(MenuItem undo, MenuItem redo) {
        ToolManager toolManager = getToolManager();
        if (toolManager == null || toolManager.getPDFViewCtrl() == null) {
            return;
        }
        // set item enable as well as text
        UndoRedoManager undoRedoManager = toolManager.getUndoRedoManger();
        String nextUndoAction = undoRedoManager.getNextUndoAction();
        if (!Utils.isNullOrEmpty(nextUndoAction)) {
            undo.setEnabled(true);
            undo.setTitle(nextUndoAction);
        } else {
            undo.setEnabled(false);
            undo.setTitle(R.string.undo);
        }
        String nextRedoAction = undoRedoManager.getNextRedoAction();
        if (!Utils.isNullOrEmpty(nextRedoAction)) {
            redo.setEnabled(true);
            redo.setTitle(nextRedoAction);
        } else {
            redo.setEnabled(false);
            redo.setTitle(R.string.redo);
        }
    }

    /**
     * Removes click listener for toolbar button clicks
     *
     * @param onMenuItemClickListener the listener
     */
    public void removeButtonClickListener(@NonNull AnnotationButtonClickListener onMenuItemClickListener) {
        mButtonClickListeners.remove(onMenuItemClickListener);
    }

    /**
     * Adds click listener for toolbar button clicks
     *
     * @param onMenuItemClickListener the listener
     */
    public void addButtonClickListener(@NonNull AnnotationButtonClickListener onMenuItemClickListener) {
        mButtonClickListeners.add(onMenuItemClickListener);
    }

    /**
     * Add listener to notify when the toolbar changes.
     *
     * @param listener to add
     */
    public void addOnToolbarChangedListener(@NonNull OnToolbarChangedListener listener) {
        mOnToolbarChangedListeners.add(listener);
    }

    /**
     * Remove listener that notifies when the toolbar changes.
     *
     * @param listener to remove
     */
    public void removeOnToolbarChangedListener(@NonNull OnToolbarChangedListener listener) {
        mOnToolbarChangedListeners.remove(listener);
    }

    /**
     * Add listener to notify prior to building a new AnnotationToolbar.
     *
     * @param listener to add
     */
    public void addOnPreBuildToolbarListener(@NonNull OnPreBuildToolbarListener listener) {
        mOnPreBuildToolbarListeners.add(listener);
    }

    /**
     * Remove listener that notifies prior to building a new AnnotationToolbar.
     *
     * @param listener to remove
     */
    public void removeOnPreBuildToolbarListener(@NonNull OnPreBuildToolbarListener listener) {
        mOnPreBuildToolbarListeners.remove(listener);
    }

    /**
     * Listener called when toolbar changes.
     */
    public interface OnToolbarChangedListener {
        void onToolbarChanged(String newToolbar);
    }

    /**
     * Listener called prior to building a new AnnotationToolbar.
     */
    public interface OnPreBuildToolbarListener {
        void onPreBuildToolbar(AnnotationToolbarBuilder builder);
    }

    /**
     * Annotation button click listeners
     */
    public interface AnnotationButtonClickListener {
        /**
         * Called when a annotation button has been invoked,  right before any internal log is handled.
         * If this method returns true, the button event is intercept and no other other callbacks will
         * be executed.
         *
         * @param toolbarItem The toolbar item that was invoked
         * @param item        The menu item that was invoked.
         * @return Return true to consume this click and prevent others from
         * executing.
         */
        boolean onInterceptItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item);

        /**
         * Called when an annotation button has been invoked, right before any internal logic is handled.
         *
         * @param toolbarItem The toolbar item that was invoked
         * @param item        The menu item that was invoked.
         */
        void onPreItemClick(@Nullable ToolbarItem toolbarItem, MenuItem item);

        /**
         * Called when an annotation button has been invoked, right after any internal logic is handled.
         *
         * @param toolbarItem The toolbar item that was invoked
         * @param item        The menu item that was invoked.
         */
        void onPostItemClick(@Nullable ToolbarItem toolbarItem, @NonNull MenuItem item);
    }

    /**
     * Dummy EditToolbarImpl workaround used for custom behavior in new toolbar.
     */
    private class DummyEditToolbarImpl extends EditToolbarImpl {

        public DummyEditToolbarImpl(@NonNull FragmentActivity activity, @NonNull EditToolbar editToolbar, @NonNull ToolManager toolManager, @NonNull ToolManager.ToolMode toolMode, @Nullable Annot editAnnot, int pageNumber, boolean shouldExpand) {
            super(activity, editToolbar, toolManager, toolMode, editAnnot, pageNumber, shouldExpand);
        }

        public DummyEditToolbarImpl(@NonNull FragmentActivity activity, @NonNull BaseEditToolbar editToolbar, @NonNull ToolManager toolManager, @NonNull ToolManager.ToolMode toolMode, @Nullable Annot editAnnot, int pageNumber, boolean shouldExpand, @NonNull Bundle bundle) {
            super(activity, editToolbar, toolManager, toolMode, editAnnot, pageNumber, shouldExpand, bundle);
        }

        @Override
        protected void initTool(ToolManager.ToolMode toolMode) {
            super.initTool(toolMode);
            if (toolMode == ToolManager.ToolMode.INK_CREATE && (mToolManager.getTool() instanceof FreehandCreate)) {
                ((FreehandCreate) mToolManager.getTool()).setForceSameNextToolMode(mForceSameNextToolMode);
            } else if (toolMode == ToolManager.ToolMode.SMART_PEN_INK && (mToolManager.getTool() instanceof SmartPenInk)) {
                ((SmartPenInk) mToolManager.getTool()).setForceSameNextToolMode(mForceSameNextToolMode);
            }
        }
    }

    /**
     * Edit toolbar used for undo/redo workaround for ink tool.
     */
    private class DummyEditToolbar implements BaseEditToolbar {

        @Override
        public void setVisibility(int gone) {

        }

        @Override
        public void setup(PDFViewCtrl pdfViewCtrl, OnToolSelectedListener onToolSelectedListener, ArrayList<AnnotStyle> drawStyles, boolean b, boolean hasEraseBtn, boolean b1, boolean shouldExpand, boolean isStyleFixed) {

        }

        @Override
        public void setOnEditToolbarChangeListener(EditToolbar.OnEditToolbarChangedListener listener) {

        }

        @Override
        public void show() {

        }

        @Override
        public boolean isShown() {
            return false;
        }

        @Override
        public void updateControlButtons(boolean canClear, boolean canErase, boolean canUndo, boolean canRedo) {
            // Every stroke, we will call listener to update the undo/redo state
            ToolManager toolManager = getToolManager();
            if (toolManager != null) {
                toolManager.getUndoRedoManger().notifyUndoRedoStateChange();
            }
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
}
