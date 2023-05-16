package com.pdftron.pdf.dialog.annotlist;

class TypeState {
    boolean selected;
    int type;

    TypeState(boolean selected, int type) {
        this.selected = selected;
        this.type = type;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeState typeState = (TypeState) o;

        return type == typeState.type;
    }

    @Override
    public int hashCode() {
        return type;
    }
}
