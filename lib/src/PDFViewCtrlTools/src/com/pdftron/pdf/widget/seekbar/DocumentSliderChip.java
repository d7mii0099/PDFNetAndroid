package com.pdftron.pdf.widget.seekbar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.ImageView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

public class DocumentSliderChip extends FrameLayout {

    private boolean mIsVertical;

    public DocumentSliderChip(@NonNull Context context) {
        this(context, null);
    }

    public DocumentSliderChip(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DocumentSliderChip(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_document_seek_bar_chip, this, true);
    }

    public void setIconTint(@ColorInt int color) {
        ImageView icon = findViewById(R.id.chip_icon);
        icon.setColorFilter(color);
    }

    public void setCardBackground(@ColorInt int color) {
        CardView cardView = findViewById(R.id.chip_card);
        cardView.setCardBackgroundColor(color);
    }

    public void setVertical(boolean isVertical) {
        mIsVertical = isVertical;
        Context context = getContext();
        ImageView icon = findViewById(R.id.chip_icon);
        int offset = context.getResources().getDimensionPixelSize(R.dimen.document_seek_bar_chip_image_offset);
        if (mIsVertical) {
            if (Utils.isRtlLayout(context)) {
                offset = -offset;
            }
            icon.setTranslationX(offset);
            icon.setRotation(90);
        } else {
            icon.setTranslationY(offset);
        }

        CardView cardView = findViewById(R.id.chip_card);
        int chipOffset = context.getResources().getDimensionPixelSize(R.dimen.document_seek_bar_chip_offset);
        if (mIsVertical) {
            if (Utils.isRtlLayout(context)) {
                chipOffset = context.getResources().getDimensionPixelSize(R.dimen.document_seek_bar_chip_offset_rtl);
            }
            cardView.setTranslationX(chipOffset);
        } else {
            cardView.setTranslationY(chipOffset);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Context context = getContext();
        int width = context.getResources().getDimensionPixelSize(R.dimen.document_seek_bar_chip_width);
        int height = context.getResources().getDimensionPixelSize(R.dimen.document_seek_bar_chip_height);
        int nextWidthMeasureSpec;
        int nextHeightMeasureSpec;
        if (mIsVertical) {
            nextWidthMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
            nextHeightMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
        } else {
            nextWidthMeasureSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY);
            nextHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        }
        super.onMeasure(nextWidthMeasureSpec, nextHeightMeasureSpec);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return false;
    }
}
