package com.pdftron.pdf.widget.preset.component.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.File;

public class PresetSingleButton extends PresetActionButton {

    private CardView mBackground;
    private CardView mPresetIconWithBackgroundContainer;
    private CardView mPresetWithBodyContainer;
    private AppCompatImageView mPresetIconWithBackground;
    private AppCompatImageView mPresetIcon;
    private AppCompatImageView mPresetLeftIcon;
    private TextView mPresetBodyText;
    private TextView mEmptyStateText;
    private int mPresetBackgroundColor;

    private boolean mArrowIconVisible = true;
    private boolean mPresetIconHasBackground = false;
    private boolean mPresetBodyVisible = false; // represent mode where there is icon and text (i.e. count)

    public PresetSingleButton(@NonNull Context context) {
        super(context);
    }

    public PresetSingleButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public PresetSingleButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public PresetSingleButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void init(@Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        LayoutInflater.from(getContext()).inflate(R.layout.preset_single_view, this);

        mBackground = findViewById(R.id.background);
        mPresetIconWithBackgroundContainer = findViewById(R.id.preset_icon_background);
        mPresetIconWithBackground = findViewById(R.id.preset_icon_with_background);
        mPresetWithBodyContainer = findViewById(R.id.preset_with_body);
        mPresetIcon = findViewById(R.id.preset_icon);
        mPresetLeftIcon = findViewById(R.id.preset_left_icon);
        mPresetBodyText = findViewById(R.id.preset_body);
        mStyleIconView = findViewById(R.id.style_icon);
        mEmptyStateText = findViewById(R.id.empty_state);
    }

    @Override
    public void setIconSize(int sizeInPx) {
        super.setIconSize(sizeInPx);
        updateIconSize(mStyleIconView, mIconSize);
    }

    public void setPresetAnnotStyle(AnnotStyle annotStyle, int leftIconRes) {
        if (annotStyle.hasStampId()) {
            mPresetBodyVisible = true;
            setEmpty(false);
            mPresetWithBodyContainer.setCardBackgroundColor(mPresetBackgroundColor);
            mPresetLeftIcon.setImageDrawable(ContextCompat.getDrawable(mPresetLeftIcon.getContext(), leftIconRes));
            mPresetLeftIcon.setColorFilter(annotStyle.getColor());
            mPresetBodyText.setText(annotStyle.getStampId());
        }
    }

    /**
     * Sets the preset bitmap
     *
     * @param bitmap the preset bitmap
     */
    public void setPresetBitmap(@Nullable Bitmap bitmap) {
        mPresetBodyVisible = false;
        if (null == bitmap) {
            setEmpty(true);
        } else {
            setEmpty(false);
        }

        if (mPresetIconHasBackground) {
            mPresetIconWithBackground.setImageBitmap(bitmap);
        } else {
            mPresetIcon.setImageBitmap(bitmap);
        }
    }

    /**
     * Sets the preset image view from image file
     *
     * @param imageFile the preset image file
     */
    public void setPresetFile(@NonNull File imageFile) {
        mPresetBodyVisible = false;
        setEmpty(false);

        int[] dimensions = Utils.getImageFileDimensions(imageFile, 1024);
        RequestCreator requestCreator = Picasso.get()
                .load(imageFile)
                .resize(dimensions[0], dimensions[1])
                .onlyScaleDown()
                .memoryPolicy(MemoryPolicy.NO_CACHE, MemoryPolicy.NO_STORE);
        if (mPresetIconHasBackground) {
            requestCreator.into(mPresetIconWithBackground);
        } else {
            requestCreator.into(mPresetIcon);
        }
    }

    public void setEmptyState(@StringRes int emptyState) {
        setEmpty(true);

        mEmptyStateText.setText(emptyState);
    }

    private void setEmpty(boolean empty) {
        if (mPresetBodyVisible) {
            mPresetIcon.setVisibility(View.GONE);
            mPresetIconWithBackground.setVisibility(View.GONE);
            mPresetIconWithBackgroundContainer.setVisibility(View.GONE);
            mPresetWithBodyContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
        } else {
            mPresetWithBodyContainer.setVisibility(View.GONE);
        }
        if (!mPresetBodyVisible) {
            if (mPresetIconHasBackground) {
                mPresetIconWithBackground.setVisibility(empty ? View.GONE : View.VISIBLE);
                mPresetIconWithBackgroundContainer.setVisibility(empty ? View.GONE : View.VISIBLE);
            } else {
                mPresetIcon.setVisibility(empty ? View.GONE : View.VISIBLE);
            }
        }
        if (mArrowIconVisible) {
            mStyleIconView.setVisibility(empty ? View.GONE : View.VISIBLE);
        } else {
            mStyleIconView.setVisibility(View.GONE);
        }
        mEmptyStateText.setVisibility(empty ? View.VISIBLE : View.GONE);
    }

    public void setTextColor(int textColor) {
        mEmptyStateText.setTextColor(textColor);
    }

    public void setArrowIconVisible(boolean visible) {
        mArrowIconVisible = visible;
        mStyleIconView.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    public void setBackgroundColor(int color) {
        mPresetBackgroundColor = color;
        mBackground.setCardBackgroundColor(color);
    }

    public void presetIconWithBackground(boolean enabled) {
        mPresetIconHasBackground = enabled;
        mPresetIconWithBackgroundContainer.setVisibility(enabled ? View.VISIBLE : View.GONE);
        mPresetIcon.setVisibility(enabled ? View.GONE : View.VISIBLE);
    }
}
