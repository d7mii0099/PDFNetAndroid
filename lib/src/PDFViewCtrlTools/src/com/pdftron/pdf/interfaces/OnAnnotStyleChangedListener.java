package com.pdftron.pdf.interfaces;

import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;

import java.util.ArrayList;

public interface OnAnnotStyleChangedListener {
    void onAnnotStyleColorChange(ArrayList<AnnotStyle> styles);

    void OnAnnotStyleDismiss(AnnotStyleDialogFragment styleDialogFragment);
}
