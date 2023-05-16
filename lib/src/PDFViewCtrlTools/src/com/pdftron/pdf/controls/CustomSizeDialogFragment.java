//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import com.pdftron.pdf.utils.Utils;

/**
 * A dialog with custom size which depends running on tablet or phone devices
 */
public abstract class CustomSizeDialogFragment extends DialogFragment {

    protected int mWidth = 500;
    protected int mHeight = -1;
    protected boolean mHasCustomSize = false;

    private DialogDismissListener mListener;

    @Override
    public void onStart() {
        super.onStart();
        setDialogSize();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setDialogSize();
    }

    private void setDialogSize() {
        View view = getView();
        Window window = getDialog().getWindow();
        if (view == null || window == null) {
            return;
        }

        Context context = view.getContext();

        int marginWidth = (int) Utils.convDp2Pix(context, 600);
        if (mHasCustomSize) {
            dimBackground(window);
            window.setLayout(mWidth, mHeight);
        } else {
            if (Utils.isTablet(context) && Utils.getScreenWidth(context) > marginWidth) {
                dimBackground(window);
                int width = (int) Utils.convDp2Pix(context, mWidth);
                int height = Utils.getScreenHeight(context);
                if (mHeight > 0) {
                    window.setLayout(width, (int) Utils.convDp2Pix(context, mHeight));
                } else {
                    int dh = (int) Utils.convDp2Pix(context, 100);
                    window.setLayout(width, height - dh);
                }
            } else {
                if (mHeight > 0) {
                    dimBackground(window);
                    int width = (int) (Utils.getScreenWidth(context) * 0.9);
                    window.setLayout(width, (int) Utils.convDp2Pix(context, mHeight));
                } else {
                    window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
                }
            }
        }
    }

    /**
     * Sets the custom size to use when inflating this dialog
     *
     * @param width of the dialog in pixels
     * @param height of the dialog in pixels
     */
    public void setCustomSize(int width, int height) {
        mHasCustomSize = true;
        mWidth = width;
        mHeight = height;
    }

    private void dimBackground(@NonNull Window window) {
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.60f;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        window.setAttributes(windowParams);
    }

    public void setDialogDismissListener(DialogDismissListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mListener != null) {
            mListener.onMenuEditorDialogDismiss();
        }
    }

    public interface DialogDismissListener {
        void onMenuEditorDialogDismiss();
    }

}
