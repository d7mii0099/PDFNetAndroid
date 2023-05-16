package com.pdftron.pdf.dialog.annotlist;

import androidx.annotation.NonNull;

class StatusState {
    boolean selected;
    @NonNull
    String status;

    StatusState(boolean selected, @NonNull String status) {
        this.selected = selected;
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StatusState that = (StatusState) o;

        return status.equals(that.status);
    }

    @Override
    public int hashCode() {
        return status.hashCode();
    }
}
