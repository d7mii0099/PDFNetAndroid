package com.pdftron.pdf.widget.toolbar.builder;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.MenuItem;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.pdftron.pdf.tools.R;

public class ToolbarItem implements Parcelable {
    @NonNull
    public static final ToolbarItem DEFAULT_PAN_TOOl =
            new ToolbarItem(
                    "",
                    ToolbarButtonType.PAN,
                    -1,
                    false,
                    false,
                    R.string.controls_annotation_toolbar_tool_description_pan,
                    R.drawable.ic_pan_black_24dp,
                    MenuItem.SHOW_AS_ACTION_IF_ROOM,
                    0);
    @NonNull
    public final String toolbarId;
    @NonNull
    public final ToolbarButtonType toolbarButtonType;
    public final int buttonId;
    public final boolean isCheckable;
    public final boolean hasOption;
    @StringRes
    public final int titleRes;
    @Nullable
    public final String title;
    @DrawableRes
    public int icon;
    public final int showAsAction; // currently this is unused
    public boolean isVisible;
    public int order;

    public ToolbarItem(
            @NonNull String toolbarId,
            @NonNull ToolbarButtonType toolbarButtonType,
            int buttonId,
            boolean isCheckable,
            boolean hasOption,
            @StringRes int titleRes,
            @Nullable String title,
            @DrawableRes int icon,
            int showAsAction,
            boolean isVisible,
            int order) {
        this.toolbarId = toolbarId;
        this.toolbarButtonType = toolbarButtonType;
        this.buttonId = buttonId;
        this.isCheckable = isCheckable;
        this.hasOption = hasOption;
        this.titleRes = titleRes;
        this.title = title;
        this.icon = icon;
        this.showAsAction = showAsAction;
        this.isVisible = isVisible;
        this.order = order;
    }

    public ToolbarItem(
            @NonNull String toolbarId,
            @NonNull ToolbarButtonType toolbarButtonType,
            int buttonId,
            boolean isCheckable,
            boolean hasOption,
            @StringRes int titleRes,
            @DrawableRes int icon,
            int showAsAction,
            int order) {
        this(toolbarId, toolbarButtonType, buttonId, isCheckable, hasOption, titleRes, null, icon, showAsAction, order);
    }

    public ToolbarItem(
            @NonNull String toolbarId,
            @NonNull ToolbarButtonType toolbarButtonType,
            int buttonId,
            boolean isCheckable,
            boolean hasOption,
            @StringRes int titleRes,
            @Nullable String title,
            @DrawableRes int icon,
            int showAsAction,
            int order) {
        this(
                toolbarId,
                toolbarButtonType,
                buttonId,
                isCheckable,
                hasOption,
                titleRes,
                title,
                icon,
                showAsAction,
                true,
                order
        );
    }

    public ToolbarItem(
            @NonNull String toolbarId,
            @NonNull ToolbarButtonType toolbarButtonType,
            int buttonId,
            boolean isCheckable,
            @StringRes int titleRes,
            @DrawableRes int icon,
            int showAsAction,
            int order) {
        this(toolbarId, toolbarButtonType, buttonId, isCheckable, titleRes, null, icon, showAsAction, order);
    }

    public ToolbarItem(
            @NonNull String toolbarId,
            @NonNull ToolbarButtonType toolbarButtonType,
            int buttonId,
            boolean isCheckable,
            @StringRes int titleRes,
            @Nullable String title,
            @DrawableRes int icon,
            int showAsAction,
            int order) {
        this(
                toolbarId,
                toolbarButtonType,
                buttonId,
                isCheckable,
                false,
                titleRes,
                title,
                icon,
                showAsAction,
                true,
                order
        );
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public void setIcon(@DrawableRes int iconRes) {
        this.icon = iconRes;
    }

    public void setVisible(boolean visible) {
        this.isVisible = visible;
    }

    public String getStyleId() {
        return toolbarId + String.valueOf(toolbarButtonType.getValue()) + String.valueOf(buttonId);
    }

    public ToolbarItem copy() {
        return new ToolbarItem(
                toolbarId,
                toolbarButtonType,
                buttonId,
                isCheckable,
                hasOption,
                titleRes,
                title,
                icon,
                showAsAction,
                order
        );
    }

    public ToolbarItem copy(boolean visible) {
        return new ToolbarItem(
                toolbarId,
                toolbarButtonType,
                buttonId,
                isCheckable,
                hasOption,
                titleRes,
                title,
                icon,
                showAsAction,
                visible,
                order
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ToolbarItem that = (ToolbarItem) o;

        if (buttonId != that.buttonId) return false;
        if (toolbarButtonType != that.toolbarButtonType) return false;
        if (!toolbarId.equals(that.toolbarId)) return false;
        return order == that.order;
    }

    @Override
    public int hashCode() {
        return buttonId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.toolbarId);
        dest.writeInt(this.toolbarButtonType == null ? -1 : this.toolbarButtonType.ordinal());
        dest.writeInt(this.buttonId);
        dest.writeByte(this.isCheckable ? (byte) 1 : (byte) 0);
        dest.writeByte(this.hasOption ? (byte) 1 : (byte) 0);
        dest.writeInt(this.titleRes);
        dest.writeInt(this.icon);
        dest.writeInt(this.showAsAction);
        dest.writeByte(this.isVisible ? (byte) 1 : (byte) 0);
        dest.writeInt(this.order);
        dest.writeString(this.title);
    }

    protected ToolbarItem(Parcel in) {
        this.toolbarId = in.readString();
        int tmpToolbarButtonType = in.readInt();
        this.toolbarButtonType = tmpToolbarButtonType == -1 ? null : ToolbarButtonType.values()[tmpToolbarButtonType];
        this.buttonId = in.readInt();
        this.isCheckable = in.readByte() != 0;
        this.hasOption = in.readByte() != 0;
        this.titleRes = in.readInt();
        this.icon = in.readInt();
        this.showAsAction = in.readInt();
        this.isVisible = in.readByte() != 0;
        this.order = in.readInt();
        this.title = in.readString();
    }

    public static final Creator<ToolbarItem> CREATOR = new Creator<ToolbarItem>() {
        @Override
        public ToolbarItem createFromParcel(Parcel source) {
            return new ToolbarItem(source);
        }

        @Override
        public ToolbarItem[] newArray(int size) {
            return new ToolbarItem[size];
        }
    };
}

