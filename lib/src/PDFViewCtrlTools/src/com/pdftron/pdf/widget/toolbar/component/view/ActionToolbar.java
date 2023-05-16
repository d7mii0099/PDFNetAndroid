package com.pdftron.pdf.widget.toolbar.component.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IntRange;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuItemImpl;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;
import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars;
import com.pdftron.pdf.widget.toolbar.component.ToolModeMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ActionToolbar extends FrameLayout {

    protected View mToolbarRoot;
    protected FrameLayout mToolbarViewContainer;
    protected FrameLayout mToolbarOverlay;
    protected FrameLayout mToolbarLeftOptionalContainer;
    protected FrameLayout mToolbarActionsRightOptionalContainer;
    protected MaterialCardView mPresetContainer; // by default visibility is gone
    protected TextView mNoPresetText; // by default visibility is gone
    protected TextView mNoToolText; // by default visibility is gone

    protected HorizontalScrollView mScrollView;
    protected View mDivider;

    protected ActionButton mToolbarSwitcher;

    protected boolean mCompactMode;
    protected @DrawableRes
    int mNavigationIcon;
    protected boolean mNavigationIconVisible = true;
    protected int mNavigationIconPaddingLeft = 0;
    protected int mNavigationIconMinWidth = -1;

    protected ActionMenuView mToolbarActions;
    protected ActionMenuView mStickyToolbarActions;
    protected ActionMenuView mLeadingStickyToolbarActions;
    protected List<ToolbarButton> mMainToolbarButtons = new ArrayList<>();
    protected List<ToolbarButton> mStickyToolbarButtons = new ArrayList<>();
    protected List<ToolbarButton> mLeadingStickyToolbarButtons = new ArrayList<>();

    protected List<Toolbar.OnMenuItemClickListener> mOnMenuItemClickListeners = new ArrayList<>();
    protected List<OnLongClickListener> mOnLongClickListeners = new ArrayList<>();

    protected AnnotationToolbarTheme mAnnotToolbarTheme;
    protected int mLayoutGravity = -1;

    public ActionToolbar(Context context) {
        super(context);
        init(null, 0, R.style.PTAnnotationToolbarTheme);
    }

    public ActionToolbar(Context context, AnnotationToolbarTheme annotToolbarTheme) {
        super(context);
        mAnnotToolbarTheme = annotToolbarTheme;
        init(null, 0, R.style.PTAnnotationToolbarTheme);
    }

    public ActionToolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0, R.style.PTAnnotationToolbarTheme);
    }

    public ActionToolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        init(attrs, 0, R.style.PTAnnotationToolbarTheme);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ActionToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(attrs, 0, defStyleRes);
    }

    @LayoutRes
    protected int getLayoutResource() {
        return R.layout.toolbar_action_scrollable;
    }

    protected void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {

        Context context = getContext();
        if (mAnnotToolbarTheme == null) {
            mAnnotToolbarTheme = AnnotationToolbarTheme.fromContext(context);
        }

        setBackgroundColor(mAnnotToolbarTheme.backgroundColor);

        // Inflate views
        LayoutInflater.from(context).inflate(getLayoutResource(), this);

        mToolbarRoot = findViewById(R.id.toolbar_root);
        mToolbarViewContainer = findViewById(R.id.toolbar_view_container);
        mToolbarOverlay = findViewById(R.id.toolbar_overlay);
        mToolbarLeftOptionalContainer = findViewById(R.id.left_optional_container);
        mToolbarActionsRightOptionalContainer = findViewById(R.id.toolbar_actions_right_container);
        mToolbarActions = findViewById(R.id.toolbar_actions);
        mStickyToolbarActions = findViewById(R.id.sticky_toolbar_actions);
        mLeadingStickyToolbarActions = findViewById(R.id.leading_sticky_toolbar_actions);
        mPresetContainer = findViewById(R.id.preset_background);
        mNoPresetText = findViewById(R.id.no_preset_text);
        mNoToolText = findViewById(R.id.no_tool_text);
        mPresetContainer.setCardBackgroundColor(mAnnotToolbarTheme.backgroundColorSecondary);
        mNoPresetText.setTextColor(mAnnotToolbarTheme.presetTextColor);

        mToolbarActions.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mOnMenuItemClickListeners != null) {
                    for (Toolbar.OnMenuItemClickListener listener : mOnMenuItemClickListeners) {
                        if (listener.onMenuItemClick(item)) {
                            return true;
                        }
                    }
                }
                return false;
            }
        });
        mStickyToolbarActions.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mOnMenuItemClickListeners != null) {
                    boolean handled = false;
                    for (Toolbar.OnMenuItemClickListener listener : mOnMenuItemClickListeners) {
                        handled = handled || listener.onMenuItemClick(item);
                    }
                    return handled;
                }
                return false;
            }
        });
        mLeadingStickyToolbarActions.setOnMenuItemClickListener(new ActionMenuView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (mOnMenuItemClickListeners != null) {
                    boolean handled = false;
                    for (Toolbar.OnMenuItemClickListener listener : mOnMenuItemClickListeners) {
                        handled = handled || listener.onMenuItemClick(item);
                    }
                    return handled;
                }
                return false;
            }
        });
        Drawable overflowIcon = Utils.getDrawable(context, R.drawable.ic_overflow_white_24dp);
        overflowIcon.setColorFilter(new PorterDuffColorFilter(mAnnotToolbarTheme.iconColor, PorterDuff.Mode.SRC_ATOP));
        mToolbarActions.setOverflowIcon(overflowIcon);
        mStickyToolbarActions.setOverflowIcon(overflowIcon);
        mLeadingStickyToolbarActions.setOverflowIcon(overflowIcon);

        mScrollView = findViewById(R.id.toolbar_actions_container);
        mDivider = findViewById(R.id.divider);
        mDivider.setBackgroundColor(mAnnotToolbarTheme.dividerColor);

        mToolbarSwitcher = findViewById(R.id.toolbar_switcher);
        mToolbarSwitcher.setIcon(context.getResources().getDrawable(R.drawable.ic_arrow_down_white_24dp));
        mToolbarSwitcher.setIconColor(mAnnotToolbarTheme.iconColor);
        mToolbarSwitcher.setCheckable(false);

        // fake item to pass on the click event
        final MenuItem toolbarSwitcherMenuItem = mToolbarActions.getMenu().add(0, R.id.toolbar_switcher, 0, R.string.toolbar_switcher_description);
        toolbarSwitcherMenuItem.setVisible(false);
        toolbarSwitcherMenuItem.setActionView(mToolbarSwitcher);
        TooltipCompat.setTooltipText(mToolbarSwitcher, toolbarSwitcherMenuItem.getTitle());
        mToolbarSwitcher.setOnClickListener(new OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View v) {
                mToolbarActions.invokeItem((MenuItemImpl) toolbarSwitcherMenuItem);
            }
        });

        setCompactMode(false);
    }

    public void inflateWithBuilder(AnnotationToolbarBuilder builder) {
        clearToolbarButtons();
        // Inflate scrollable tools
        inflateTools(builder.getToolbarItems(), mToolbarActions, mMainToolbarButtons);

        // Now inflate the sticky tools
        inflateTools(builder.getStickyToolbarItems(), mStickyToolbarActions, mStickyToolbarButtons);

        // Now inflate the start sticky tools
        inflateTools(builder.getLeadingStickyToolbarItems(), mLeadingStickyToolbarActions, mLeadingStickyToolbarButtons);

        updateDividerVisibility();

        // reset scroll to the beginning when we inflate with builder
        // otherwise if we inflate a new toolbar the previous scroll position will be retained
        mScrollView.scrollTo(0, 0);
    }

    @SuppressLint("RestrictedApi")
    private void inflateTools(List<ToolbarItem> toolbarItems,
            @NonNull final ActionMenuView actionMenuView,
            @NonNull List<ToolbarButton> toolbarButtonsToUpdate) {
        // First sort by order before adding to view
        List<ToolbarItem> sortedToolbarItems = new ArrayList<>(toolbarItems);
        Collections.sort(sortedToolbarItems, new Comparator<ToolbarItem>() {
            @Override
            public int compare(ToolbarItem o1, ToolbarItem o2) {
                return o1.order - o2.order;
            }
        });
        Menu menu = actionMenuView.getMenu();
        menu.clear();

        // Here we call stopDispatchingItemsChanged to  prevent menu from updating the presenter
        // until we have added all menu items. Othewise the presenter will be updated on every
        // call to MenuItem.setShowAsAction and MenuItem.setActionView
        if (menu instanceof MenuBuilder) { // ActionMenuView.getMenu() should return MenuBuilder but we will check type just in case
            MenuBuilder menuBuilder = (MenuBuilder) menu;
            menuBuilder.stopDispatchingItemsChanged();
        }
        // Currently we do not support submenus
        HashMap<Integer, Boolean> highlightedIconColorMap = new HashMap<>();
        HashMap<Integer, Boolean> hasOptionMap = new HashMap<>();
        for (ToolbarItem toolActionItem : sortedToolbarItems) {
            // handle special nav icon
            if (toolActionItem.buttonId == DefaultToolbars.ButtonId.NAVIGATION.value()) {
                toolActionItem.setVisible(mNavigationIconVisible);
                if (mNavigationIcon != 0) {
                    toolActionItem.setIcon(mNavigationIcon);
                }
            }
            highlightedIconColorMap.put(toolActionItem.buttonId, ToolModeMapper.hasAccentedIcon(toolActionItem.toolbarButtonType));
            hasOptionMap.put(toolActionItem.buttonId, toolActionItem.hasOption);
            if (toolActionItem.titleRes != 0) {
                menu.add(Menu.NONE, toolActionItem.buttonId, Menu.NONE, toolActionItem.titleRes);
            } else {
                menu.add(Menu.NONE, toolActionItem.buttonId, Menu.NONE, toolActionItem.title);
            }
            MenuItem menuItem = menu.findItem(toolActionItem.buttonId);
            // We need to set a drawable object directly and mutate first. Menu class does something
            // weird with the Drawable on pre-lollipop and below so this is the work around...
            if (!Utils.isLollipop()) {
                menuItem.setIcon(ContextCompat.getDrawable(getContext(), toolActionItem.icon).mutate());
            } else {
                menuItem.setIcon(toolActionItem.icon);
            }
            menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            menuItem.setCheckable(toolActionItem.isCheckable);
            if (toolActionItem.titleRes != 0) {
                menuItem.setTitle(toolActionItem.titleRes);
            } else {
                menuItem.setTitle(toolActionItem.title);
            }
            menuItem.setVisible(toolActionItem.isVisible);
        }
        initMenu(menu, actionMenuView, highlightedIconColorMap, hasOptionMap, toolbarButtonsToUpdate);

        // Now call startDispatchingItemsChanged so that the menu can update the presenter
        if (menu instanceof MenuBuilder) {
            MenuBuilder menuBuilder = (MenuBuilder) menu;
            menuBuilder.startDispatchingItemsChanged();
        }
    }

    private void initMenu(@NonNull Menu menu,
            @NonNull final ActionMenuView actionMenuView,
            @NonNull HashMap<Integer, Boolean> highlightedIconColorMap,
            @NonNull HashMap<Integer, Boolean> hasOptionMap,
            @NonNull List<ToolbarButton> toolbarButtonsToUpdate) {

        // We will need to manually determine which items to show as action, since we are using ActionMenuView
        // directly. If we do not do this manually and use it as is, the maximum number of
        // action items will depend on ActionBarPolicy.getMaxActionButtons() (typically 2 on a phone)
        int numMenuItems = menu.size();
        List<Integer> indexOfShowAlways = getShowAlwaysItemIndices(menu);
        List<Integer> indexOfShowIfRoom = getShowIfRoomItemIndices(menu);

        int maxNumberOfVisibleActionItems = getMaxVisibleActionItems(numMenuItems);
        if (indexOfShowAlways.size() > maxNumberOfVisibleActionItems) {
            // If we have too many "always show" items, we set some of them to "show if room"
            for (int i = maxNumberOfVisibleActionItems - 3; i < indexOfShowAlways.size(); i++) {
                Integer menuItemIndex = indexOfShowAlways.get(i);
                MenuItem alwaysShowMenuitem = menu.getItem(menuItemIndex);
                alwaysShowMenuitem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
            }
        } else if (indexOfShowAlways.size() < maxNumberOfVisibleActionItems) {
            // We don't have enough "always show" items, so we set some other menu items to "always show"
            int numItemsToSet = maxNumberOfVisibleActionItems - indexOfShowAlways.size();
            for (int i = 0; i < indexOfShowIfRoom.size() && numItemsToSet != 0; i++, numItemsToSet--) {
                Integer menuItemIndex = indexOfShowIfRoom.get(i);
                MenuItem showIfRoomMenuitem = menu.getItem(menuItemIndex);
                showIfRoomMenuitem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
            }
        }

        // Now create our buttons
        for (int i = 0; i < numMenuItems; i++) {
            final MenuItem item = menu.getItem(i);
            // Be default, ActionMenuView shows actions items based on the limit defined by
            // ActionBarPolicy.getMaxActionButtons(). So we want to force show manually to fill
            int numVisibleItems = maxNumberOfVisibleActionItems;
            // up the entire toolbar
            if (getShowAsActionFlag(item) == MenuItemImpl.SHOW_AS_ACTION_ALWAYS) {
                // Add items to view
                ActionButton actionButton = new ActionButton(getContext());
                actionButton.setMenuItem(item);
                actionButton.setId(item.getItemId());
                actionButton.setIcon(item.getIcon());
                actionButton.setIconColor(mAnnotToolbarTheme.iconColor);
                actionButton.setSelectedIconColor(mAnnotToolbarTheme.selectedIconColor);
                actionButton.setDisabledIconColor(mAnnotToolbarTheme.disabledIconColor);
                boolean hasHighlightedIcon = highlightedIconColorMap.get(item.getItemId());
                actionButton.setShowIconHighlightColor(hasHighlightedIcon);
                if (mCompactMode) {
                    actionButton.setSelectedBackgroundColor(mAnnotToolbarTheme.backgroundColor);
                } else {
                    actionButton.setSelectedBackgroundColor(mAnnotToolbarTheme.selectedBackgroundColor);
                }
                actionButton.setId(item.getItemId());
                actionButton.setCheckable(item.isCheckable());
                boolean hasOption = hasOptionMap.get(item.getItemId());
                actionButton.setHasOption(hasOption);
                actionButton.setOnClickListener(new OnClickListener() {
                    @SuppressLint("RestrictedApi")
                    @Override
                    public void onClick(View v) {
                        actionMenuView.invokeItem((MenuItemImpl) item);
                    }
                });
                actionButton.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        if (mOnLongClickListeners != null) {
                            boolean handled = false;
                            for (OnLongClickListener listener : mOnLongClickListeners) {
                                handled = handled || listener.onLongClick(v);
                            }
                            return handled;
                        }
                        return false;
                    }
                });
                if (!hasOption) {
                    // when option dropdown is required, don't show tooltip
                    TooltipCompat.setTooltipText(actionButton, item.getTitle());
                }
                if (actionButton.getId() == DefaultToolbars.ButtonId.NAVIGATION.value()) {
                    // adjust the padding for navigation icon to match default toolbar
                    int navButtonWidth = getContext().getResources().getDimensionPixelSize(R.dimen.navigation_button_width);
                    if (mNavigationIconMinWidth != -1) {
                        navButtonWidth = mNavigationIconMinWidth;
                    }
                    actionButton.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                    int diff = navButtonWidth - actionButton.getMeasuredWidth();
                    int extraPadding = diff / 2;
                    actionButton.setPadding(
                            mNavigationIconPaddingLeft + extraPadding,
                            0,
                            extraPadding,
                            0
                    );
                }
                item.setActionView(actionButton);
                if (actionButton.getId() == DefaultToolbars.ButtonId.CUSTOMIZE.value()) {
                    actionButton.setShowIconHighlightColor(true);
                    actionButton.setAlwaysShowIconHighlightColor(true);
                    actionButton.setIconHighlightColor(mAnnotToolbarTheme.highlightIconColor);
                }
                if (item.isVisible()) {
                    actionButton.show();
                } else {
                    actionButton.hide();
                }
                toolbarButtonsToUpdate.add(actionButton);
            } else {
                OverflowButton overflowButton = new OverflowButton(item);
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
                toolbarButtonsToUpdate.add(overflowButton);
            }
        }
    }

    public void addOnMenuItemClickListener(@NonNull Toolbar.OnMenuItemClickListener listener) {
        mOnMenuItemClickListeners.add(listener);
    }

    public void addOnButtonLongClickListener(@NonNull OnLongClickListener listener) {
        mOnLongClickListeners.add(listener);
    }

    public void setCompactMode(boolean compactMode) {
        mCompactMode = compactMode;
        mToolbarSwitcher.setVisibility(mCompactMode ? VISIBLE : GONE);
        if (mLayoutGravity == -1) {
            if (mCompactMode) {
                setToolbarItemGravity(Gravity.START);
            } else {
                setToolbarItemGravity(Gravity.END);
            }
        } else {
            setToolbarItemGravity(mLayoutGravity);
        }
        updateTheme();
    }

    public void setToolbarSwitcherVisible(boolean visible) {
        mToolbarSwitcher.setVisibility(mCompactMode && visible ? VISIBLE : GONE);
    }

    protected void updateTheme() {
        MaterialCardView regionBackground = findViewById(R.id.tool_region_background);
        if (mCompactMode) {
            regionBackground.setVisibility(VISIBLE);
            regionBackground.setCardBackgroundColor(mAnnotToolbarTheme.backgroundColorCompact);
            regionBackground.setRadius(getContext().getResources().getDimensionPixelOffset(R.dimen.toolbar_compact_region_radius));
            if (Utils.isLollipop() && !Utils.isMarshmallow()) {
                // the 2 versions of lollipop has rendering issue with the scroll view
                // if the toolbar switcher is visible, and without background
                // so here we will set the background
                regionBackground.getLayoutParams().height = getContext().getResources().getDimensionPixelSize(R.dimen.action_button_min_width);
                mScrollView.setBackgroundColor(mAnnotToolbarTheme.backgroundColorCompact);
            }
            mPresetContainer.setCardBackgroundColor(mAnnotToolbarTheme.backgroundColor);
        } else {
            regionBackground.setVisibility(GONE);
            mPresetContainer.setCardBackgroundColor(mAnnotToolbarTheme.backgroundColorSecondary);
        }
    }

    public void setNavigationIcon(@DrawableRes int icon) {
        mNavigationIcon = icon;
    }

    public void setNavigationIconVisible(boolean visible) {
        mNavigationIconVisible = visible;
    }

    public void setNavigationIconProperty(int paddingLeft, int minWidth) {
        mNavigationIconPaddingLeft = paddingLeft;
        mNavigationIconMinWidth = minWidth;
    }

    /**
     * If a button exists and is selectable, then select it and deselect all other buttons.
     *
     * @param buttonId of the toolbar button
     */
    // enforces invariant: only one item selected
    public void toggleToolbarButtons(int buttonId) {
        ToolbarButton myToolbarButton = null;
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            int id = toolbarButton.getId();
            if (id == buttonId) {
                myToolbarButton = toolbarButton;
                break;
            }
        }

        // If toolbar button exists and is checkable, then select the button and deselect all other checkable buttons
        boolean selected = false;
        if (myToolbarButton != null && myToolbarButton.isCheckable()) {
            for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
                int id = toolbarButton.getId();
                if (id == buttonId && toolbarButton.isCheckable()) {
                    toolbarButton.select();
                    selected = true;
                } else {
                    toolbarButton.deselect();
                }
            }
        }

        // Scroll to button if not visible if a button was selected
        if (selected) {
            for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
                int id = toolbarButton.getId();
                if (id == buttonId) {
                    if (toolbarButton instanceof ActionButton) {
                        ActionButton actionButton = (ActionButton) toolbarButton;
                        if (mScrollView != null) {
                            // Check to see if button is visible, if not then scroll to it
                            Rect scrollBounds = new Rect();
                            mScrollView.getHitRect(scrollBounds);
                            if (!actionButton.getLocalVisibleRect(scrollBounds)) {
                                mScrollView.smoothScrollTo(actionButton.getLeft(), actionButton.getTop());
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Click button if it exists, otherwise do nothing
     *
     * @param buttonId of the toolbar button
     */
    public void selectToolbarButtonIfAvailable(int buttonId) {
        MenuItem menuitem = getMenuItem(buttonId);
        if (menuitem != null) {
            for (Toolbar.OnMenuItemClickListener listener : mOnMenuItemClickListeners) {
                listener.onMenuItemClick(menuitem);
            }
        }
    }

    // does not enforce invariant where only one item is selected
    public void setItemSelected(int buttonId, boolean isSelected) {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            int id = toolbarButton.getId();
            if (id == buttonId) {
                if (isSelected) {
                    toolbarButton.select();
                } else {
                    toolbarButton.deselect();
                }
            }
        }
    }

    public void deselectAllTools() {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            toolbarButton.deselect();
        }
    }

    private List<Integer> getShowIfRoomItemIndices(@NonNull Menu menu) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < menu.size(); i++) {
            if (getShowAsActionFlag(menu.getItem(i)) == MenuItemImpl.SHOW_AS_ACTION_IF_ROOM) {
                indices.add(i);
            }
        }
        return indices;
    }

    private List<Integer> getShowAlwaysItemIndices(@NonNull Menu menu) {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < menu.size(); i++) {
            if (getShowAsActionFlag(menu.getItem(i)) == MenuItemImpl.SHOW_AS_ACTION_ALWAYS) {
                indices.add(i);
            }
        }
        return indices;
    }

    @SuppressLint("RestrictedApi")
    private static int getShowAsActionFlag(MenuItem item) {
        MenuItemImpl itemImpl = ((MenuItemImpl) item);
        if (itemImpl.requiresActionButton()) {
            return MenuItemImpl.SHOW_AS_ACTION_ALWAYS;
        } else if (itemImpl.requestsActionButton()) {
            return MenuItemImpl.SHOW_AS_ACTION_IF_ROOM;
        } else if (itemImpl.showsTextAsAction()) {
            return MenuItemImpl.SHOW_AS_ACTION_WITH_TEXT;
        } else {
            return MenuItemImpl.SHOW_AS_ACTION_NEVER;
        }
    }

    /**
     * Gets the max number of visible action items depending on screen size and icon size. This excludes
     * the overflow button, but includes the switcher button as this is considered an action button
     *
     * @param numMenuItems number of items you want to display
     * @return
     */
    protected int getMaxVisibleActionItems(int numMenuItems) {
        int numVisibleItems = getMaxNumberIconsFromWidth(getContext());
        numVisibleItems = numMenuItems > numVisibleItems ? numVisibleItems - 1 : numVisibleItems; // minus 1 action item for overflow
        return numVisibleItems;
    }

    protected int getMaxNumberIconsFromWidth(@NonNull Context context) {
        return Integer.MAX_VALUE;
    }

    public void addToolbarOverlay(@NonNull View view) {
        mToolbarOverlay.addView(view);
    }

    public void addToolbarLeftOptionalContainer(@NonNull View view) {
        mToolbarLeftOptionalContainer.addView(view);
    }

    public void addToolbarActionsRightOptionalContainer(@NonNull View view) {
        mToolbarActionsRightOptionalContainer.addView(view);
    }

    public void setToolRegionVisible(boolean visible) {
        mToolbarViewContainer.setVisibility(visible ? VISIBLE : INVISIBLE); // we still want to maintain the location
    }

    public void setEmptyToolText(@StringRes int emptyText) {
        mNoToolText.setText(emptyText);
    }

    public void setEmptyToolTextVisible(boolean visible) {
        mNoToolText.setVisibility(visible ? VISIBLE : GONE);
    }

    public void setEmptyToolTextOnClickListener(@Nullable OnClickListener listener) {
        mNoToolText.setOnClickListener(listener);
    }

    public FrameLayout getPresetContainer() {
        return mPresetContainer;
    }

    public void clearToolbarOverlayView() {
        mToolbarOverlay.removeAllViews();
    }

    public void clearOptionalContainers() {
        mToolbarOverlay.removeAllViews();
        mToolbarLeftOptionalContainer.removeAllViews();
        mToolbarActionsRightOptionalContainer.removeAllViews();
    }

    public void updateAccentButton(int buttonId, @ColorInt int color, @IntRange(from = 0, to = 255) int alpha) {
        for (ToolbarButton button : getAllToolbarButtons()) {
            if (button instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) button;
                MenuItem menuItem = actionButton.getMenuItem();
                if (menuItem != null && buttonId == menuItem.getItemId()) {
                    actionButton.setIconHighlightColor(color);
                    actionButton.setIconAlpha(alpha);
                }
            }
        }
    }

    public void setItemEnabled(int buttonId, boolean isEnabled) {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            if (toolbarButton instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) toolbarButton;
                MenuItem menuItem = actionButton.getMenuItem();
                if (menuItem != null && buttonId == menuItem.getItemId()) {
                    if (isEnabled) {
                        actionButton.enable();
                    } else {
                        actionButton.disable();
                    }
                }
            }
        }
    }

    public void setItemAppearanceEnabled(int buttonId, boolean isAppearanceEnabled) {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            if (toolbarButton instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) toolbarButton;
                MenuItem menuItem = actionButton.getMenuItem();
                if (menuItem != null && buttonId == menuItem.getItemId()) {
                    actionButton.setAppearanceEnabled(isAppearanceEnabled);
                }
            }
        }
    }

    public void setItemVisibility(int buttonId, boolean isVisible) {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            if (toolbarButton instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) toolbarButton;
                MenuItem menuItem = actionButton.getMenuItem();
                if (menuItem != null && buttonId == menuItem.getItemId()) {
                    if (isVisible) {
                        actionButton.show();
                    } else {
                        actionButton.hide();
                    }
                }
            }
        }

        updateDividerVisibility();
    }

    public boolean hasVisibleItems() {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            if (toolbarButton instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) toolbarButton;
                if (actionButton.isVisible()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    private MenuItem getMenuItem(int buttonId) {
        for (ToolbarButton button : getAllToolbarButtons()) {
            ActionButton actionButton = (ActionButton) button;
            MenuItem menuItem = actionButton.getMenuItem();
            if (menuItem != null && menuItem.getItemId() == buttonId) {
                return menuItem;
            }
        }
        return null;
    }

    private List<ToolbarButton> getAllToolbarButtons() {
        List<ToolbarButton> toolbarButtons = new ArrayList<>();
        toolbarButtons.addAll(mMainToolbarButtons);
        toolbarButtons.addAll(mStickyToolbarButtons);
        toolbarButtons.addAll(mLeadingStickyToolbarButtons);
        return toolbarButtons;
    }

    private void clearToolbarButtons() {
        mMainToolbarButtons.clear();
        mStickyToolbarButtons.clear();
        mLeadingStickyToolbarButtons.clear();
    }

    private void updateDividerVisibility() {
        // Hide divider if there are no visible items in the sticky section
        boolean hasVisibleStickyToolbarItems = false;
        for (ToolbarButton stickyToolbarButton : mStickyToolbarButtons) {
            if (stickyToolbarButton instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) stickyToolbarButton;
                MenuItem menuItem = actionButton.getMenuItem();
                if (menuItem.isVisible()) {
                    hasVisibleStickyToolbarItems = true;
                    break;
                }
            }
        }
        if (mCompactMode) {
            // in compact mode, we always hide divider
            mDivider.setVisibility(GONE);
        } else {
            mDivider.setVisibility(hasVisibleStickyToolbarItems ? VISIBLE : GONE);
        }
    }

    public void setToolbarItemGravity(int layoutGravity) {
        mLayoutGravity = layoutGravity;
        if (mToolbarActions != null) {
            ViewGroup.LayoutParams params = mToolbarActions.getLayoutParams();
            if (params instanceof FrameLayout.LayoutParams) {
                ((LayoutParams) params).gravity = layoutGravity;
            }
            mToolbarActions.setGravity(layoutGravity);
        }
    }

    public void setItemIcon(int id, @NonNull Drawable icon) {
        List<ToolbarButton> allToolbarButtons = getAllToolbarButtons();
        for (ToolbarButton toolbarButton : allToolbarButtons) {
            if (toolbarButton.getId() == id) {
                toolbarButton.setIcon(icon);
            }
        }
    }

    public void setSelectedIconColor(int buttonId, @ColorInt int iconColor) {
        List<ToolbarButton> allToolbarButtons = getAllToolbarButtons();
        for (ToolbarButton toolbarButton : allToolbarButtons) {
            if (toolbarButton.getId() == buttonId && toolbarButton instanceof ActionButton) {
                ((ActionButton) toolbarButton).setSelectedIconColor(iconColor);
            }
        }
    }

    public void setIconColor(int buttonId, @ColorInt int iconColor) {
        List<ToolbarButton> allToolbarButtons = getAllToolbarButtons();
        for (ToolbarButton toolbarButton : allToolbarButtons) {
            if (toolbarButton.getId() == buttonId && toolbarButton instanceof ActionButton) {
                ((ActionButton) toolbarButton).setIconColor(iconColor);
            }
        }
    }

    public void setBackground(int id, boolean showBackground) {
        List<ToolbarButton> allToolbarButtons = getAllToolbarButtons();
        for (ToolbarButton toolbarButton : allToolbarButtons) {
            if (toolbarButton.getId() == id) {
                if (toolbarButton instanceof ActionButton) {
                    ((ActionButton) toolbarButton).setShowBackground(showBackground);
                }
            }
        }
    }

    public void disableAllItems() {
        for (ToolbarButton toolbarButton : getAllToolbarButtons()) {
            if (toolbarButton instanceof ActionButton) {
                ActionButton actionButton = (ActionButton) toolbarButton;
                actionButton.disable();
            }
        }
    }
}
