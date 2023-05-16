package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.ExpandableGridView;
import com.pdftron.pdf.utils.StylePickerGridViewAdapter;

import java.util.List;

public class PresetStyleGridView extends ExpandableGridView {
    private StylePickerGridViewAdapter mAdapter;

    /**
     * Class constructor
     */
    public PresetStyleGridView(Context context) {
        super(context);
        init(null, R.attr.preset_styles_style);
    }

    /**
     * Class constructor
     */
    public PresetStyleGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, R.attr.preset_styles_style);
    }

    /**
     * Class constructor
     */
    public PresetStyleGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PresetStyleGridView, defStyle, R.style.PresetStyleGridViewStyle);
        try {

            // spacing
            int hSpacing = a.getDimensionPixelOffset(R.styleable.PresetStyleGridView_android_horizontalSpacing, 0);
            setHorizontalSpacing(hSpacing);
            int vSpacing = a.getDimensionPixelOffset(R.styleable.PresetStyleGridView_android_verticalSpacing, 0);
            setVerticalSpacing(vSpacing);

            // column width
            int columnWidth = a.getDimensionPixelOffset(R.styleable.PresetStyleGridView_android_columnWidth, -1);
            if (columnWidth > 0) {
                setColumnWidth(columnWidth);
            }

            // stretchMode
            int index = a.getInt(R.styleable.PresetStyleGridView_android_stretchMode, STRETCH_COLUMN_WIDTH);
            if (index >= 0) {
                setStretchMode(index);
            }

            // num columns
            int numColumns = a.getInt(R.styleable.PresetStyleGridView_android_numColumns, 1);
            setNumColumns(numColumns);

            // gravity
            index = a.getInt(R.styleable.PresetStyleGridView_android_gravity, -1);
            if (index >= 0) {
                setGravity(index);
            }
        } finally {
            a.recycle();
        }
    }

    public void setStyleList(List<Integer> styleList) {
        setStyleList(styleList, false);
    }

    /**
     * Sets the list style icons for the grid view. If rotate icon is true, the icons
     * will be rotated 180 degrees.
     *
     * @param styleList the list of icons to display
     * @param rotateIcon whether the icons should be rotated.
     */
    public void setStyleList(List<Integer> styleList, boolean rotateIcon) {
        mAdapter = new StylePickerGridViewAdapter(getContext(), styleList, rotateIcon);
        setAdapter(mAdapter);
    }

    @Override
    public StylePickerGridViewAdapter getAdapter() {
        return mAdapter;
    }
}
