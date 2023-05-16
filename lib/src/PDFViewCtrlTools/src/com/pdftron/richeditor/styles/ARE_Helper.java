package com.pdftron.richeditor.styles;

import androidx.annotation.Nullable;

public class ARE_Helper {

    /**
     * Updates the check status.
     *
     * @param areStyle
     * @param checked
     */
    public static void updateCheckStatus(@Nullable IARE_Style areStyle, boolean checked) {
        if (areStyle != null) {
            areStyle.setChecked(checked);
        }
    }
}
