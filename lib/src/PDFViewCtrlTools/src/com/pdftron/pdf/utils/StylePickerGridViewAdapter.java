package com.pdftron.pdf.utils;

import android.content.Context;
import android.graphics.PorterDuff;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;

import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.tools.R;

import java.util.List;

public class StylePickerGridViewAdapter extends ArrayAdapter<Integer> {

    private List<Integer> mSource;
    private String mSelected;

    private AnnotStyleDialogFragment.Theme mTheme;
    private boolean mRotateIcon = false;

    /**
     * @param context The context
     * @param list    the style list
     * @param rotateIcon whether the icons in the grid should be rotated 180 degrees
     */
    public StylePickerGridViewAdapter(Context context, List<Integer> list, boolean rotateIcon) {
        super(context, 0, list);
        mTheme = AnnotStyleDialogFragment.Theme.fromContext(context);
        setSource(list);
        setRotateIcon(rotateIcon);
        mSelected = "";
    }

    /**
     * Sets style list source
     *
     * @param list style list source
     */
    public void setSource(List<Integer> list) {
        mSource = list;
        notifyDataSetChanged();
    }

    /**
     * Sets whether the icons in the grid view should be rotate 180 degrees.
     *
     * @param rotateIcon whether the icons should be rotated 180 degrees
     */
    public void setRotateIcon(boolean rotateIcon) {
        mRotateIcon = rotateIcon;
    }

    /**
     * Gets selected style source
     *
     * @return The selected style source
     */
    public String getSelected() {
        return mSelected;
    }

    /**
     * Gets style source count.
     *
     * @return The style source count
     */
    @Override
    public int getCount() {
        return mSource.size();
    }

    /**
     * Get style item in list at specific position.
     *
     * @param position The specific position.
     * @return style item in style source list.
     */
    @Override
    public Integer getItem(int position) {
        if (mSource != null && position >= 0 && position < mSource.size()) {
            return mSource.get(position);
        }
        return null;
    }

    public boolean contains(Integer item) {
        return mSource.contains(item);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.tools_gridview_style_picker, parent, false);
            holder = new ViewHolder();
            holder.styleLayout = convertView.findViewById(R.id.style_cell_layout);
            holder.styleImage = convertView.findViewById(R.id.style_image_view);
            if (mRotateIcon) {
                holder.styleImage.setRotation(180);
            }
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        holder.styleImage.setImageResource(mSource.get(position));
        holder.styleImage.setColorFilter(mTheme.iconColor, PorterDuff.Mode.SRC_IN);
        return convertView;
    }

    private static class ViewHolder {
        RelativeLayout styleLayout;
        ImageView styleImage;
    }
}
