package com.pdftron.pdf.widget.preset.component.model;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;

import java.util.ArrayList;

public class PresetButtonState {

    private boolean mIsSelected;

    @Nullable
    private ArrayList<AnnotStyle> mAnnotStyles;

    public void initializeStyle(@NonNull Context context, int annotType, int index, String toolbarStyleId) {
        addAnnotStyle(context, annotType, index, toolbarStyleId);
    }

    public void addAnnotStyle(@NonNull Context context, int annotType, int index, String toolbarStyleId) {
        if (mAnnotStyles == null) {
            mAnnotStyles = new ArrayList<>();
        }
        AnnotStyle annotStyle = ToolStyleConfig.getInstance().getAnnotPresetStyle(context, annotType, index, toolbarStyleId);
        mAnnotStyles.add(annotStyle);
    }

    public boolean isSelected() {
        return mIsSelected;
    }

    public void setSelected(boolean isSelected) {
        this.mIsSelected = isSelected;
    }

    public void setAnnotStyles(@Nullable ArrayList<AnnotStyle> annotStyles) {
        mAnnotStyles = annotStyles;
    }

    @Nullable
    public ArrayList<AnnotStyle> getAnnotStyles() {
        return mAnnotStyles;
    }
}
