package com.pdftron.richeditor.styles;

import android.text.Editable;
import android.widget.EditText;

public interface IARE_Style {

    /**
     * Apply the style to the change start at start end at end.
     *
     * @param editable
     * @param start
     * @param end
     */
    public void applyStyle(Editable editable, int start, int end);

    /**
     * Sets if this style is checked.
     *
     * @param isChecked
     */
    public void setChecked(boolean isChecked);

    /**
     * Returns if current style is checked.
     *
     * @return
     */
    public boolean getIsChecked();

    /**
     * Gets the EditText being operated.
     *
     * @return
     */
    public EditText getEditText();
}
