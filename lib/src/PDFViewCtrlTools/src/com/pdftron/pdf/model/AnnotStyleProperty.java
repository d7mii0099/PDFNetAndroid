package com.pdftron.pdf.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Defines which properties in an {@link com.pdftron.pdf.controls.AnnotStyleDialogFragment}
 * should be shown and hidden if available.
 */
public final class AnnotStyleProperty implements Parcelable {
    private final int annotType;
    private boolean canShowStrokeColor = true;
    private boolean canShowFillColor = true;
    private boolean canShowThickness = true;
    private boolean canShowOpacity = true;
    private boolean canShowFont = true;
    private boolean canShowTextAlignment = true;
    private boolean canShowIcons = true;
    private boolean canShowTextSize = true;
    private boolean canShowTextColor = true;
    private boolean canShowRulerUnit = true;
    private boolean canShowRulerPrecision = true;
    private boolean canShowSnap = true;
    private boolean canShowRichContent = true;
    private boolean canShowTextOverlay = true;
    private boolean canShowPreset = true;
    private boolean canShowEraserType = true;
    private boolean canShowEraserMode = true;
    private boolean canShowDateFormat = true;
    private boolean canShowPressure = true;
    private boolean canShowSavedColorPicker = true;
    private boolean canShowAdvancedColorPicker = true;
    private boolean canShowBorderStyle = true;
    private boolean canShowLineStyle = true;
    private boolean canShowLineStartStyle = true;
    private boolean canShowLineEndStyle = true;

    public AnnotStyleProperty(int annotType) {
        this.annotType = annotType;
    }

    public int getAnnotType() {
        return annotType;
    }

    public boolean canShowStrokeColor() {
        return canShowStrokeColor;
    }

    public AnnotStyleProperty setCanShowStrokeColor(boolean canShowStrokeColor) {
        this.canShowStrokeColor = canShowStrokeColor;
        return this;
    }

    public boolean canShowFillColor() {
        return canShowFillColor;
    }

    public AnnotStyleProperty setCanShowFillColor(boolean canShowFillColor) {
        this.canShowFillColor = canShowFillColor;
        return this;
    }

    public boolean canShowThickness() {
        return canShowThickness;
    }

    public AnnotStyleProperty setCanShowThickness(boolean canShowThickness) {
        this.canShowThickness = canShowThickness;
        return this;
    }

    public boolean canShowOpacity() {
        return canShowOpacity;
    }

    public AnnotStyleProperty setCanShowOpacity(boolean canShowOpacity) {
        this.canShowOpacity = canShowOpacity;
        return this;
    }

    public boolean canShowFont() {
        return canShowFont;
    }

    public boolean canShowTextAlignment() {
        return canShowTextAlignment;
    }

    public AnnotStyleProperty setCanShowFont(boolean canShowFont) {
        this.canShowFont = canShowFont;
        return this;
    }

    public AnnotStyleProperty setCanShowTextAlignment(boolean canShowTextAlignment) {
        this.canShowTextAlignment = canShowTextAlignment;
        return this;
    }

    public boolean canShowIcons() {
        return canShowIcons;
    }

    public AnnotStyleProperty setCanShowIcons(boolean canShowIcons) {
        this.canShowIcons = canShowIcons;
        return this;
    }

    public boolean canShowTextSize() {
        return canShowTextSize;
    }

    public AnnotStyleProperty setCanShowTextSize(boolean canShowTextSize) {
        this.canShowTextSize = canShowTextSize;
        return this;
    }

    public boolean canShowTextColor() {
        return canShowTextColor;
    }

    public AnnotStyleProperty setCanShowTextColor(boolean canShowTextColor) {
        this.canShowTextColor = canShowTextColor;
        return this;
    }

    public boolean canShowRulerUnit() {
        return canShowRulerUnit;
    }

    public AnnotStyleProperty setCanShowRulerUnit(boolean canShowRulerUnit) {
        this.canShowRulerUnit = canShowRulerUnit;
        return this;
    }

    public boolean canShowRulerPrecision() {
        return canShowRulerPrecision;
    }

    public AnnotStyleProperty setCanShowRulerPrecision(boolean canShowRulerPrecision) {
        this.canShowRulerPrecision = canShowRulerPrecision;
        return this;
    }

    public boolean canShowSnap() {
        return canShowSnap;
    }

    public AnnotStyleProperty setCanShowSnap(boolean canShowSnap) {
        this.canShowSnap = canShowSnap;
        return this;
    }

    public boolean canShowRichContent() {
        return canShowRichContent;
    }

    public AnnotStyleProperty setCanShowRichContent(boolean canShowRichContent) {
        this.canShowRichContent = canShowRichContent;
        return this;
    }

    public boolean canShowTextOverlay() {
        return canShowTextOverlay;
    }

    public AnnotStyleProperty setCanShowTextOverlay(boolean canShowTextOverlay) {
        this.canShowTextOverlay = canShowTextOverlay;
        return this;
    }

    public boolean canShowPreset() {
        return canShowPreset;
    }

    public AnnotStyleProperty setCanShowPreset(boolean canShowPreset) {
        this.canShowPreset = canShowPreset;
        return this;
    }

    public boolean canShowEraserType() {
        return canShowEraserType;
    }

    public AnnotStyleProperty setCanShowEraserType(boolean canShowEraserType) {
        this.canShowEraserType = canShowEraserType;
        return this;
    }

    public boolean canShowEraserMode() {
        return canShowEraserMode;
    }

    public AnnotStyleProperty setCanShowEraserMode(boolean canShowEraserMode) {
        this.canShowEraserMode = canShowEraserMode;
        return this;
    }

    public boolean canShowDateFormat() {
        return canShowDateFormat;
    }

    public AnnotStyleProperty setCanShowDateFormat(boolean canShowDateFormat) {
        this.canShowDateFormat = canShowDateFormat;
        return this;
    }

    public boolean canShowPressure() {
        return canShowPressure;
    }

    public AnnotStyleProperty setCanShowPressure(boolean canShowPressure) {
        this.canShowPressure = canShowPressure;
        return this;
    }

    public boolean canShowSavedColorPicker() {
        return canShowSavedColorPicker;
    }

    public AnnotStyleProperty setCanShowSavedColorPicker(boolean canShowSavedColorPicker) {
        this.canShowSavedColorPicker = canShowSavedColorPicker;
        return this;
    }

    public boolean canShowAdvancedColorPicker() {
        return canShowAdvancedColorPicker;
    }

    public AnnotStyleProperty setCanShowAdvancedColorPicker(boolean canShowAdvancedColorPicker) {
        this.canShowAdvancedColorPicker = canShowAdvancedColorPicker;
        return this;
    }

    public boolean canShowBorderStyle() {
        return canShowBorderStyle;
    }

    public AnnotStyleProperty setCanShowBorderStyle(boolean canShowBorderStyle) {
        this.canShowBorderStyle = canShowBorderStyle;
        return this;
    }

    public boolean canShowLineStyle() {
        return canShowLineStyle;
    }

    public AnnotStyleProperty setCanShowLineStyle(boolean canShowLineStyle) {
        this.canShowLineStyle = canShowLineStyle;
        return this;
    }

    public boolean canShowLineStartStyle() {
        return canShowLineStartStyle;
    }

    public AnnotStyleProperty setCanShowLineStartStyle(boolean canShowLineStartStyle) {
        this.canShowLineStartStyle = canShowLineStartStyle;
        return this;
    }

    public boolean canShowLineEndStyle() {
        return canShowLineEndStyle;
    }

    public AnnotStyleProperty setCanShowLineEndStyle(boolean canShowLineEndStyle) {
        this.canShowLineEndStyle = canShowLineEndStyle;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.annotType);
        dest.writeByte(this.canShowStrokeColor ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowFillColor ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowThickness ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowOpacity ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowFont ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowIcons ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowTextSize ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowTextColor ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowRulerUnit ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowRulerPrecision ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowSnap ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowRichContent ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowTextOverlay ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowPreset ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowEraserType ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowEraserMode ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowDateFormat ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowPressure ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowSavedColorPicker ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowAdvancedColorPicker ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowBorderStyle ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowLineStyle ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowLineStartStyle ? (byte) 1 : (byte) 0);
        dest.writeByte(this.canShowLineEndStyle ? (byte) 1 : (byte) 0);
    }

    protected AnnotStyleProperty(Parcel in) {
        this.annotType = in.readInt();
        this.canShowStrokeColor = in.readByte() != 0;
        this.canShowFillColor = in.readByte() != 0;
        this.canShowThickness = in.readByte() != 0;
        this.canShowOpacity = in.readByte() != 0;
        this.canShowFont = in.readByte() != 0;
        this.canShowIcons = in.readByte() != 0;
        this.canShowTextSize = in.readByte() != 0;
        this.canShowTextColor = in.readByte() != 0;
        this.canShowRulerUnit = in.readByte() != 0;
        this.canShowRulerPrecision = in.readByte() != 0;
        this.canShowSnap = in.readByte() != 0;
        this.canShowRichContent = in.readByte() != 0;
        this.canShowTextOverlay = in.readByte() != 0;
        this.canShowPreset = in.readByte() != 0;
        this.canShowEraserType = in.readByte() != 0;
        this.canShowEraserMode = in.readByte() != 0;
        this.canShowDateFormat = in.readByte() != 0;
        this.canShowPressure = in.readByte() != 0;
        this.canShowSavedColorPicker = in.readByte() != 0;
        this.canShowAdvancedColorPicker = in.readByte() != 0;
        this.canShowBorderStyle = in.readByte() != 0;
        this.canShowLineStyle = in.readByte() != 0;
        this.canShowLineStartStyle = in.readByte() != 0;
        this.canShowLineEndStyle = in.readByte() != 0;
    }

    public static final Creator<AnnotStyleProperty> CREATOR = new Creator<AnnotStyleProperty>() {
        @Override
        public AnnotStyleProperty createFromParcel(Parcel source) {
            return new AnnotStyleProperty(source);
        }

        @Override
        public AnnotStyleProperty[] newArray(int size) {
            return new AnnotStyleProperty[size];
        }
    };
}
