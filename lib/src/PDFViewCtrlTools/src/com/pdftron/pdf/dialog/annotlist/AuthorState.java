package com.pdftron.pdf.dialog.annotlist;

import androidx.annotation.NonNull;

class AuthorState {
    boolean selected;
    @NonNull
    String name;

    AuthorState(boolean selected, @NonNull String name) {
        this.selected = selected;
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AuthorState that = (AuthorState) o;

        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
