package com.pdftron.pdf.widget.preset.signature;

import androidx.annotation.IdRes;
import androidx.annotation.Nullable;

import java.util.List;

public interface SignatureSelectionView {
    void setSignatures(List<String> imageSignaturePaths);
    void setButtonEventListener(@Nullable SignatureSelectionDialog.ButtonClickListener listener);
    void setViewVisibility(@IdRes int id, int visibility);
    void show();
    void close();
}
