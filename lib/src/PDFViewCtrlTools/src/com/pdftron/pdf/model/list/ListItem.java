package com.pdftron.pdf.model.list;

import androidx.annotation.RestrictTo;

/**
 * An annotation list item can either be a header or some annotation content.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface ListItem {
    int LAYOUT_HEADER = 0;
    int LAYOUT_CONTENT = 1;

    /**
     * @return true if this annotation list item is a header
     */
    boolean isHeader();
}
