package com.pdftron.pdf.widget.preset.component.model;

import android.graphics.Bitmap;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.widget.base.BaseObservable;

import java.io.File;

public class SinglePresetState extends BaseObservable {
    @Nullable
    private File mImageFile;
    @Nullable
    private Bitmap mBitmap;
    @Nullable
    private @StringRes
    int mEmptyStateDesc;
    @Nullable
    private AnnotStyle mAnnotStyle;
    private int mIconRes;

    public static SinglePresetState fromAnnotStyle(@NonNull AnnotStyle annotStyle, int iconRes) {
        SinglePresetState state = new SinglePresetState();
        state.setIconRes(iconRes);
        state.setAnnotStyle(annotStyle);
        return state;
    }

    public static SinglePresetState fromImageFile(@NonNull File imageFile) {
        SinglePresetState state = new SinglePresetState();
        state.setImageFile(imageFile);
        return state;
    }

    public static SinglePresetState fromBitmap(@Nullable Bitmap bitmap) {
        SinglePresetState state = new SinglePresetState();
        state.setBitmap(bitmap);
        return state;
    }

    public static SinglePresetState fromEmptyState(@StringRes int description) {
        SinglePresetState state = new SinglePresetState();
        state.setEmptyState(description);
        return state;
    }

    public SinglePresetState() {

    }

    public void setImageFile(@NonNull File imageFile) {
        mImageFile = imageFile;
    }

    public void setAnnotStyle(@NonNull AnnotStyle annotStyle) {
        mAnnotStyle = annotStyle;
    }

    public void setIconRes(int iconRes) {
        mIconRes = iconRes;
    }

    public void setBitmap(@Nullable Bitmap bitmap) {
        mBitmap = bitmap;
    }

    public void setEmptyState(@StringRes int emptyStateDesc) {
        mEmptyStateDesc = emptyStateDesc;
    }

    @Nullable
    public File getImageFile() {
        return mImageFile;
    }

    @Nullable
    public Bitmap getBitmap() {
        return mBitmap;
    }

    public AnnotStyle getAnnotStyle() {
        return mAnnotStyle;
    }

    public int getIconRes() {
        return mIconRes;
    }

    public int getEmptyStateDesc() {
        return mEmptyStateDesc;
    }
}
