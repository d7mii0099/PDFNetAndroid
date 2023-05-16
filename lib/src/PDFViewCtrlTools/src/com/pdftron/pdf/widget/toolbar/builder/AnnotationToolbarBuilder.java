package com.pdftron.pdf.widget.toolbar.builder;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MenuItem;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;

import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.toolbar.component.ToolModeMapper;
import com.pdftron.pdf.widget.toolbar.data.ToolbarItemEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Builder class used to create a custom annotation toolbar. Can be used to add supported tools
 * or custom buttons to the toolbar.
 */
public class AnnotationToolbarBuilder implements Parcelable {
    @NonNull
    private String mToolbarTag;
    @Nullable
    private String mToolbarName;
    @StringRes
    private int mToolbarNameRes = 0;
    @DrawableRes
    private int mToolbarIcon = 0;
    @NonNull
    private List<ToolbarItem> mToolbarItems = new ArrayList();
    @NonNull
    private List<ToolbarItem> mStickyToolbarItems = new ArrayList();
    @NonNull
    private List<ToolbarItem> mLeadingStickyToolbarItems = new ArrayList();
    private HashSet<Id> mIds = new HashSet<>();

    private AnnotationToolbarBuilder() {
    }

    /**
     * The tag that will be used to reference the toolbar. Will be used to store toolbar state.
     *
     * @param tag Identifier used to reference the toolbar.
     * @return this ActionToolbarBuilder
     */
    public static AnnotationToolbarBuilder withTag(@NonNull String tag) {
        if (tag == null) {
            throw new RuntimeException("Toolbar must have a non-null tag");
        }

        AnnotationToolbarBuilder builder = new AnnotationToolbarBuilder();
        builder.mToolbarTag = tag;
        return builder;
    }

    /**
     * The icon that will shown in the {@link com.pdftron.pdf.dialog.toolbarswitcher.dialog.ToolbarSwitcherDialog}
     * if there are multiple toolbars available. Default value is 0 to indicate no icon.
     *
     * @param icon drawable resource
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder setIcon(@DrawableRes int icon) {
        mToolbarIcon = icon;
        return this;
    }

    /**
     * The name of the toolbar that will be shown in the {@link com.pdftron.pdf.dialog.toolbarswitcher.dialog.ToolbarSwitcherDialog}
     * if there are multiple toolbars available. If a resource is set, then {@link #setToolbarName(String) } is ignored.
     *
     * @param name string resource for toolbar name
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder setToolbarName(@StringRes int name) {
        this.mToolbarNameRes = name;
        return this;
    }

    /**
     * The name of the toolbar that will be shown in the {@link com.pdftron.pdf.dialog.toolbarswitcher.dialog.ToolbarSwitcherDialog}
     * if there are multiple toolbars available. If not set, then the tag is used.
     * <p>
     * If a resource is set for toolbar name via {@link #setToolbarName(int)}, then the strng name
     * set by this method is ignored.
     *
     * @param name string toolbar name
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder setToolbarName(@NonNull String name) {
        this.mToolbarName = name;
        return this;
    }

    public AnnotationToolbarBuilder addToolStickyButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId, int order) {
        return addStickyButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                order
        );
    }

    public AnnotationToolbarBuilder addToolStickyButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId) {
        return addStickyButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                mStickyToolbarItems.size()
        );
    }

    public AnnotationToolbarBuilder addToolStickyOptionButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId) {
        return addStickyButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                true,
                mStickyToolbarItems.size()
        );
    }

    public AnnotationToolbarBuilder addToolLeadingStickyButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId) {
        return addLeadingStickyButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                mLeadingStickyToolbarItems.size()
        );
    }

    /**
     * Removes the sticky toolbar button with given id
     *
     * @param buttonId of the button to remove
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder removeToolStickyButton(int buttonId) {
        mIds.remove(new Id(buttonId));
        Iterator<ToolbarItem> iterator = mStickyToolbarItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().buttonId == buttonId) {
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * Removes the leading sticky toolbar button with given id
     *
     * @param buttonId of the button to remove
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder removeToolLeadingStickyButton(int buttonId) {
        mIds.remove(new Id(buttonId));
        Iterator<ToolbarItem> iterator = mLeadingStickyToolbarItems.iterator();
        while (iterator.hasNext()) {
            if (iterator.next().buttonId == buttonId) {
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * Add a sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addCustomStickyButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        return addStickyButton(
                ToolbarButtonType.CUSTOM_UNCHECKABLE,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_UNCHECKABLE.isCheckable,
                mStickyToolbarItems.size()
        );
    }

    /**
     * Add a sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addCustomStickyButton(
            String title,
            @DrawableRes int icon,
            int buttonId) {
        return addStickyButton(
                ToolbarButtonType.CUSTOM_UNCHECKABLE,
                0,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_UNCHECKABLE.isCheckable,
                mStickyToolbarItems.size()
        );
    }

    /**
     * Add a leading sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addLeadingCustomStickyButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        return addLeadingStickyButton(
                ToolbarButtonType.CUSTOM_UNCHECKABLE,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_UNCHECKABLE.isCheckable,
                mLeadingStickyToolbarItems.size()
        );
    }

    /**
     * Add a leading sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addLeadingCustomStickyButton(
            String title,
            @DrawableRes int icon,
            int buttonId) {
        return addLeadingStickyButton(
                ToolbarButtonType.CUSTOM_UNCHECKABLE,
                0,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_UNCHECKABLE.isCheckable,
                mLeadingStickyToolbarItems.size()
        );
    }

    public AnnotationToolbarBuilder addLeadingToolStickyButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId) {
        return addLeadingStickyButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                mLeadingStickyToolbarItems.size()
        );
    }

    /**
     * Add a selectable sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addCustomSelectableStickyButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        return addStickyButton(
                ToolbarButtonType.CUSTOM_CHECKABLE,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_CHECKABLE.isCheckable,
                mStickyToolbarItems.size()
        );
    }

    /**
     * Add a selectable sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addCustomSelectableStickyButton(
            String title,
            @DrawableRes int icon,
            int buttonId) {
        return addStickyButton(
                ToolbarButtonType.CUSTOM_CHECKABLE,
                0,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_CHECKABLE.isCheckable,
                mStickyToolbarItems.size()
        );
    }

    /**
     * Add a selectable leading sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addCustomSelectableLeadingStickyButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        return addLeadingStickyButton(
                ToolbarButtonType.CUSTOM_CHECKABLE,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_CHECKABLE.isCheckable,
                mLeadingStickyToolbarItems.size()
        );
    }

    /**
     * Add a selectable leading sticky button to the annotation toolbar.
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addCustomSelectableLeadingStickyButton(
            String title,
            @DrawableRes int icon,
            int buttonId) {
        return addLeadingStickyButton(
                ToolbarButtonType.CUSTOM_CHECKABLE,
                0,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_CHECKABLE.isCheckable,
                mLeadingStickyToolbarItems.size()
        );
    }

    private AnnotationToolbarBuilder addStickyButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            int order) {
        return addStickyButton(toolbarButtonType, titleRes, null, icon, buttonId, showAsAction, isCheckable, false, order);
    }

    private AnnotationToolbarBuilder addStickyButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            @Nullable String title,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            int order) {
        return addStickyButton(toolbarButtonType, titleRes, title, icon, buttonId, showAsAction, isCheckable, false, order);
    }

    private AnnotationToolbarBuilder addStickyButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            boolean hasOption,
            int order) {
        return addStickyButton(toolbarButtonType, titleRes, null, icon, buttonId, showAsAction, isCheckable, hasOption, order);
    }

    private AnnotationToolbarBuilder addStickyButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            @Nullable String title,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            boolean hasOption,
            int order) {
        Id idObj = new Id(buttonId);
        if (mIds.contains(idObj)) {
            throw new RuntimeException("You must pass in unique ids to the builder. The following buttonId was passed " + buttonId);
        }
        mStickyToolbarItems.add(
                new ToolbarItem(
                        mToolbarTag,
                        toolbarButtonType,
                        buttonId,
                        isCheckable,
                        hasOption,
                        titleRes,
                        title,
                        icon,
                        showAsAction,
                        order
                )
        );
        mIds.add(idObj);
        return this;
    }

    private AnnotationToolbarBuilder addLeadingStickyButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            int order) {
        return addLeadingStickyButton(toolbarButtonType, title, null, icon, buttonId, showAsAction, isCheckable, order);
    }

    private AnnotationToolbarBuilder addLeadingStickyButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            String title,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            int order) {
        Id idObj = new Id(buttonId);
        if (mIds.contains(idObj)) {
            throw new RuntimeException("You must pass in unique ids to the builder. The following buttonId was passed " + buttonId);
        }
        mLeadingStickyToolbarItems.add(
                new ToolbarItem(
                        mToolbarTag,
                        toolbarButtonType,
                        buttonId,
                        isCheckable,
                        titleRes,
                        title,
                        icon,
                        showAsAction,
                        order
                )
        );
        mIds.add(idObj);
        return this;
    }

    public AnnotationToolbarBuilder addCustomButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        return addButton(
                ToolbarButtonType.CUSTOM_UNCHECKABLE,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_UNCHECKABLE.isCheckable,
                mToolbarItems.size()
        );
    }

    public AnnotationToolbarBuilder addCustomButton(
            String title,
            @DrawableRes int icon,
            int buttonId) {
        return addButton(
                ToolbarButtonType.CUSTOM_UNCHECKABLE,
                0,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_UNCHECKABLE.isCheckable,
                mToolbarItems.size()
        );
    }

    public AnnotationToolbarBuilder addCustomSelectableButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        return addButton(
                ToolbarButtonType.CUSTOM_CHECKABLE,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_CHECKABLE.isCheckable,
                mToolbarItems.size()
        );
    }

    public AnnotationToolbarBuilder addCustomSelectableButton(
            String title,
            @DrawableRes int icon,
            int buttonId) {
        return addButton(
                ToolbarButtonType.CUSTOM_CHECKABLE,
                0,
                title,
                icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                ToolbarButtonType.CUSTOM_CHECKABLE.isCheckable,
                mToolbarItems.size()
        );
    }

    /**
     * Adds a button to the toolbar that controls a specific tool
     *
     * @param toolbarButtonType type of tool to add
     * @param buttonId          of the button, that is used to reference key press event. All buttons in a single
     *                          toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addToolButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId) {
        return addButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                mToolbarItems.size()
        );
    }

    /**
     * Adds a button to the toolbar that controls a specific tool
     *
     * @param toolbarButtonType type of tool to add
     * @param buttonId          of the button, that is used to reference key press event. All buttons in a single
     *                          toolbar must have unique menu ids
     * @return this {@link AnnotationToolbarBuilder}
     */
    public AnnotationToolbarBuilder addToolButton(@NonNull ToolbarButtonType toolbarButtonType, int buttonId, int order) {
        return addButton(toolbarButtonType,
                toolbarButtonType.title,
                toolbarButtonType.icon,
                buttonId,
                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                toolbarButtonType.isCheckable,
                order
        );
    }

    private AnnotationToolbarBuilder addButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            int order) {
        return addButton(toolbarButtonType, titleRes, null, icon, buttonId, showAsAction, isCheckable, order);
    }

    private AnnotationToolbarBuilder addButton(@NonNull ToolbarButtonType toolbarButtonType,
            @StringRes int titleRes,
            @Nullable String title,
            @DrawableRes int icon,
            int buttonId,
            int showAsAction,
            boolean isCheckable,
            int order) {
        Id idObj = new Id(buttonId);
        if (mIds.contains(idObj)) {
            throw new RuntimeException("You must pass in unique ids to the builder. The following buttonId was passed " + buttonId);
        }
        mToolbarItems.add(
                new ToolbarItem(
                        mToolbarTag,
                        toolbarButtonType,
                        buttonId,
                        isCheckable,
                        titleRes,
                        title,
                        icon,
                        showAsAction,
                        order
                )
        );
        mIds.add(idObj);
        return this;
    }

    private AnnotationToolbarBuilder addToolButton(@NonNull ToolbarItem toolbarItem) {
        Id idObj = new Id(toolbarItem.buttonId);
        if (mIds.contains(idObj)) {
            throw new RuntimeException("You must pass in unique ids to the builder. The following buttonId was passed " + toolbarItem.buttonId);
        }
        mToolbarItems.add(toolbarItem);
        mIds.add(idObj);
        return this;
    }

    @NonNull
    public String getToolbarTag() {
        return mToolbarTag;
    }

    /**
     * @param context to obtain string resources
     * @return String representing the toolbar's name. If {@link #setToolbarName(int)} is set, then
     * the string will be obtained from the resource, otherwise return value set by
     * {@link #setToolbarName(String)}. If no toolbar names have been set in any way,
     * then return the toolbar tag.
     */
    @NonNull
    public String getToolbarName(@NonNull Context context) {
        if (mToolbarNameRes == 0) {
            return mToolbarName == null ? mToolbarTag : mToolbarName;
        } else {
            return context.getResources().getString(mToolbarNameRes);
        }
    }

    @DrawableRes
    public int getToolbarIcon() {
        return mToolbarIcon;
    }

    @NonNull
    public List<ToolbarItem> getToolbarItems() {
        return Collections.unmodifiableList(mToolbarItems);
    }

    @NonNull
    public List<ToolbarItem> getStickyToolbarItems() {
        return Collections.unmodifiableList(mStickyToolbarItems);
    }

    @NonNull
    public List<ToolbarItem> getLeadingStickyToolbarItems() {
        return Collections.unmodifiableList(mLeadingStickyToolbarItems);
    }

    @NonNull
    public AnnotationToolbarBuilder copy() {
        return copyWithoutToolbarItems(new HashSet<ToolbarButtonType>());
    }

    /**
     * Copies AnnotationToolbarBuilder with existing toolbar items in the new specified toolbar orders.
     *
     * @param toolbarItemEntities the list containing toolbar ordering
     * @return the copied AnnotationToolbarBuilder
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public AnnotationToolbarBuilder copyWithNewOrder(Set<ToolbarItemEntity> toolbarItemEntities) {
        AnnotationToolbarBuilder builder = copy();
        HashMap<Integer, ToolbarItemEntity> toolbarItemsSet = new HashMap<>();
        for (ToolbarItemEntity toolbarItemEntity : toolbarItemEntities) {
            toolbarItemsSet.put(toolbarItemEntity.buttonId, toolbarItemEntity);
        }

        for (ToolbarItem toolbarItem : builder.mToolbarItems) {
            if (toolbarItemsSet.containsKey(toolbarItem.buttonId)) {
                ToolbarItemEntity toolbarItemEntityDb = toolbarItemsSet.get(toolbarItem.buttonId);
                toolbarItem.setOrder(toolbarItemEntityDb.order);
                // Remove from the map so we know which items have been processed
                toolbarItemsSet.remove(toolbarItem.buttonId);
            }
        }
        return builder;
    }

    /**
     * Copy toolbar with new specified toolbar items.
     *
     * @param toolbarItemEntities toolbar items to add to the new builder
     * @return the copied AnnotationToolbarBuilder
     */
    public AnnotationToolbarBuilder copyWithNewToolbarItems(Set<ToolbarItemEntity> toolbarItemEntities) {

        AnnotationToolbarBuilder builder = copy();

        for (ToolbarItemEntity toolbarItemEntity : toolbarItemEntities) {
            ToolbarButtonType buttonType = ToolbarButtonType.valueOf(toolbarItemEntity.buttonType);
            // If we should insert new items, then we do so. Used for favorite toolbar
            if (buttonType != null) {
                builder.mToolbarItems.add(
                        new ToolbarItem(
                                toolbarItemEntity.toolbarId,
                                buttonType,
                                toolbarItemEntity.buttonId,
                                buttonType.isCheckable,
                                buttonType.title,
                                buttonType.icon,
                                MenuItem.SHOW_AS_ACTION_IF_ROOM,
                                toolbarItemEntity.order
                        )
                );
            }
        }

        return builder;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public AnnotationToolbarBuilder copyWithoutToolbarItems(Set<ToolbarButtonType> itemsToRemove) {
        AnnotationToolbarBuilder builder = AnnotationToolbarBuilder.withTag(mToolbarTag);
        builder.setToolbarName(mToolbarNameRes);
        builder.setToolbarName(mToolbarName);
        builder.setIcon(mToolbarIcon);
        List<ToolbarItem> toolbarItems = new ArrayList<>();
        for (ToolbarItem item : mToolbarItems) {
            toolbarItems.add(item.copy(!itemsToRemove.contains(item.toolbarButtonType)));
        }

        List<ToolbarItem> stickyToolbarItems = new ArrayList<>();
        for (ToolbarItem item : mStickyToolbarItems) {
            stickyToolbarItems.add(item.copy(!itemsToRemove.contains(item.toolbarButtonType)));
        }

        List<ToolbarItem> leadingStickyToolbarItems = new ArrayList<>();
        for (ToolbarItem item : mLeadingStickyToolbarItems) {
            leadingStickyToolbarItems.add(item.copy(!itemsToRemove.contains(item.toolbarButtonType)));
        }

        builder.mToolbarItems = toolbarItems;
        builder.mStickyToolbarItems = stickyToolbarItems;
        builder.mLeadingStickyToolbarItems = leadingStickyToolbarItems;
        builder.mIds = new HashSet<>(mIds);
        return builder;
    }

    /**
     * Removes toolbar items from {@link AnnotationToolbarBuilder} that match the provided tool
     * types from the given list of Toolbar items
     *
     * @param removedToolModes tool modes to remove
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public AnnotationToolbarBuilder removeButtons(@NonNull Set<ToolManager.ToolMode> removedToolModes) {
        removeItems(mToolbarItems, removedToolModes);
        return this;
    }

    /**
     * Removes toolbar items that match the provided tool types from the given list of Toolbar items
     *
     * @param toolbarItems     to remove items from
     * @param removedToolModes tool modes to remove
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static void removeItems(@NonNull List<ToolbarItem> toolbarItems, @NonNull Set<ToolManager.ToolMode> removedToolModes) {
        Iterator<ToolbarItem> iterator = toolbarItems.iterator();
        while (iterator.hasNext()) {
            ToolbarItem item = iterator.next();
            ToolManager.ToolMode toolMode = ToolModeMapper.getToolMode(item.toolbarButtonType);
            if (removedToolModes.contains(toolMode)) {
                iterator.remove();
            }
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mToolbarTag);
        dest.writeString(this.mToolbarName);
        dest.writeTypedList(this.mToolbarItems);
        dest.writeTypedList(this.mStickyToolbarItems);
        dest.writeTypedList(this.mLeadingStickyToolbarItems);
        dest.writeSerializable(this.mIds);
        dest.writeInt(this.mToolbarNameRes);
        dest.writeInt(this.mToolbarIcon);
    }

    protected AnnotationToolbarBuilder(Parcel in) {
        this.mToolbarTag = in.readString();
        this.mToolbarName = in.readString();
        this.mToolbarItems = in.createTypedArrayList(ToolbarItem.CREATOR);
        this.mStickyToolbarItems = in.createTypedArrayList(ToolbarItem.CREATOR);
        this.mLeadingStickyToolbarItems = in.createTypedArrayList(ToolbarItem.CREATOR);
        this.mIds = (HashSet<Id>) in.readSerializable();
        this.mToolbarNameRes = in.readInt();
        this.mToolbarIcon = in.readInt();
    }

    public static final Parcelable.Creator<AnnotationToolbarBuilder> CREATOR = new Parcelable.Creator<AnnotationToolbarBuilder>() {
        @Override
        public AnnotationToolbarBuilder createFromParcel(Parcel source) {
            return new AnnotationToolbarBuilder(source);
        }

        @Override
        public AnnotationToolbarBuilder[] newArray(int size) {
            return new AnnotationToolbarBuilder[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AnnotationToolbarBuilder that = (AnnotationToolbarBuilder) o;

        if (mToolbarNameRes != that.mToolbarNameRes) return false;
        if (mToolbarIcon != that.mToolbarIcon) return false;
        if (!mToolbarTag.equals(that.mToolbarTag)) return false;
        if (mToolbarName != null ? !mToolbarName.equals(that.mToolbarName) : that.mToolbarName != null)
            return false;
        if (!toolItemsEqual(mToolbarItems, that.mToolbarItems)) return false;
        if (!toolItemsEqual(mStickyToolbarItems, that.mStickyToolbarItems)) return false;
        if (!toolItemsEqual(mLeadingStickyToolbarItems, that.mLeadingStickyToolbarItems))
            return false;
        return mIds.equals(that.mIds);
    }

    private static boolean toolItemsEqual(@NonNull List<ToolbarItem> items, @NonNull List<ToolbarItem> thoseItems) {
        if (items.size() != thoseItems.size()) return false;
        int size = items.size();
        for (int i = 0; i < size; i++) {
            if (!items.get(i).equals(thoseItems.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = mToolbarTag.hashCode();
        result = 31 * result + (mToolbarName != null ? mToolbarName.hashCode() : 0);
        result = 31 * result + mToolbarNameRes;
        result = 31 * result + mToolbarIcon;
        result = 31 * result + mToolbarItems.hashCode();
        result = 31 * result + mStickyToolbarItems.hashCode();
        result = 31 * result + mLeadingStickyToolbarItems.hashCode();
        result = 31 * result + mIds.hashCode();
        return result;
    }
}
