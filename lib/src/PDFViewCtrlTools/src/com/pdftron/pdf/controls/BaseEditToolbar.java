package com.pdftron.pdf.controls;

import android.view.KeyEvent;
import androidx.annotation.RestrictTo;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.model.AnnotStyle;

import java.util.ArrayList;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface BaseEditToolbar {
    void setVisibility(int gone);

    void setup(PDFViewCtrl pdfViewCtrl, OnToolSelectedListener onToolSelectedListener, ArrayList<AnnotStyle> drawStyles, boolean b, boolean hasEraseBtn, boolean b1, boolean shouldExpand, boolean isStyleFixed);

    void setOnEditToolbarChangeListener(EditToolbar.OnEditToolbarChangedListener listener);

    void show();

    boolean isShown();

    void updateControlButtons(boolean canClear, boolean canErase, boolean canUndo, boolean canRedo);

    void updateDrawColor(int drawIndex, int color);

    boolean handleKeyUp(int keyCode, KeyEvent event);

    void updateDrawStyles(ArrayList<AnnotStyle> drawStyles);
}
