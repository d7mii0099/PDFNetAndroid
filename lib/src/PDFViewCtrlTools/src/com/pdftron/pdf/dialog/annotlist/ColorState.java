package com.pdftron.pdf.dialog.annotlist;

import androidx.annotation.NonNull;

class ColorState {
    boolean selected;
    @NonNull
    String color;

    ColorState(boolean selected, @NonNull String color) {
        this.selected = selected;
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColorState that = (ColorState) o;

        return color.equals(that.color);
    }

    @Override
    public int hashCode() {
        return color.hashCode();
    }
}
