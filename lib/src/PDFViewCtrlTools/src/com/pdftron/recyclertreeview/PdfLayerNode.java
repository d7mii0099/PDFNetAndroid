package com.pdftron.recyclertreeview;

import androidx.annotation.Nullable;

import com.pdftron.pdf.dialog.pdflayer.PdfLayer;
import com.pdftron.pdf.tools.R;

public class PdfLayerNode implements LayoutItemType {
    public static final String PLACEHOLDER_TAG = "PLACEHOLDER";

    private PdfLayer mPdfLayer;
    private String mTitle;
    public boolean mIsOpen;
    public boolean mIsSelected;

    public PdfLayerNode(@Nullable PdfLayer pdfLayer) {
        mPdfLayer = pdfLayer;
        if (pdfLayer == null) {
            mTitle = PLACEHOLDER_TAG;// this should never be seen by the user
        } else {
            mTitle = pdfLayer.getName();
        }
        mIsOpen = true;
    }

    public PdfLayer getPdfLayer() {
        return mPdfLayer;
    }

    public String getTitle() {
        return this.mTitle;
    }

    public boolean isOpen() {
        return this.mIsOpen;
    }

    public PdfLayerNode setOpen(boolean open) {
        this.mIsOpen = open;
        return this;
    }

    @Override
    public int getLayoutId() {
        return R.layout.pdf_layer_tree_view_list_item;
    }
}
