package com.pdftron.pdf.widget;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.os.Build;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.Magnifier;
import androidx.cardview.widget.CardView;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.BaseTool;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

public class SelectionLoupe extends CardView {

    private PDFViewCtrl mPdfViewCtrl;

    private ImageView mImageView;

    private Magnifier mMagnifier;

    private int mType;

    public SelectionLoupe(PDFViewCtrl pdfViewCtrl) {
        this(pdfViewCtrl, BaseTool.LOUPE_TYPE_TEXT);
    }

    public SelectionLoupe(PDFViewCtrl pdfViewCtrl, int type) {
        super(pdfViewCtrl.getContext(), null);
        mPdfViewCtrl = pdfViewCtrl;
        mType = type;

        init();
    }

    private void init() {
        if (canUseMagnifier()) {
            mMagnifier = new Magnifier(mPdfViewCtrl);
        } else {
            LayoutInflater.from(getContext()).inflate(R.layout.view_selection_loupe, this);
            mImageView = findViewById(R.id.imageview);

            if (!Utils.isLollipop()) {
                setPreventCornerOverlap(false);
            }
        }
    }

    public void setup(Bitmap bitmap) {
        int cornerRadius = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.pdftron_magnifier_corner_radius);
        setup(bitmap, cornerRadius);
    }

    public void setup(Bitmap bitmap, float cornerRadius) {
        if (canUseMagnifier()) {
            return;
        }

        int elevation = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.pdftron_magnifier_elevation);
        setCardElevation(elevation);

        if (Utils.isLollipop()) {
            mImageView.setImageBitmap(bitmap);
            setRadius(cornerRadius);
        } else {
            RoundCornersDrawable round = new RoundCornersDrawable(bitmap, cornerRadius, 0);
            round.enableBorder(getContext().getResources().getColor(R.color.light_gray_border),
                    Utils.convDp2Pix(getContext(), 1));
            mImageView.setBackground(round);
        }
    }

    public void show() {
        if (!canUseMagnifier()) {
            if (getParent() == null) {
                mPdfViewCtrl.addView(this);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.P)
    public void show(float xPosInView, float yPosInView) {
        if (canUseMagnifier()) {
            mMagnifier.show(xPosInView, yPosInView);
        }
    }

    public void dismiss() {
        if (canUseMagnifier()) {
            mMagnifier.dismiss();
        } else {
            mPdfViewCtrl.removeView(this);
        }
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (canUseMagnifier()) {
            return;
        }

        mImageView.layout(0, 0, r - l, b - t);
    }

    private boolean canUseMagnifier() {
        return Utils.isPie() && mType == BaseTool.LOUPE_TYPE_TEXT;
    }
}
