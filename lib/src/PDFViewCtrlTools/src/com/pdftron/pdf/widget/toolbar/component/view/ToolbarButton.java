package com.pdftron.pdf.widget.toolbar.component.view;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

public interface ToolbarButton {
    void deselect();
    void select();
    void enable();
    void disable();
    int getId();
    boolean isCheckable();
    void setCheckable(boolean isCheckable);
    boolean hasOption();
    void setHasOption(boolean hasOption);
    boolean isSelected();
    void show();
    void hide();
    void setIcon(@NonNull Drawable drawable);
}
