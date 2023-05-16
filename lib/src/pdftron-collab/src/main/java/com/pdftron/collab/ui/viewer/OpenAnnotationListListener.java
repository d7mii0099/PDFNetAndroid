package com.pdftron.collab.ui.viewer;

import androidx.annotation.RestrictTo;

/**
 * Listener used to open the annotation list from a {@link CollabViewerTabFragment}
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface OpenAnnotationListListener {
    void openAnnotationList();
}
