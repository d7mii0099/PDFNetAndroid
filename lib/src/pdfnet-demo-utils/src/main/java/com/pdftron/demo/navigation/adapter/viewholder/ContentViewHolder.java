//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.demo.navigation.adapter.viewholder;

import android.annotation.TargetApi;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.demo.R;
import com.pdftron.demo.widget.ImageViewTopCrop;
import com.pdftron.pdf.utils.Utils;

public class ContentViewHolder extends RecyclerView.ViewHolder {

    public FrameLayout thumbnailContainer;
    public ImageViewTopCrop imageViewFileIcon;
    public ImageView imageViewFileLockIcon;
    public TextView docTextPlaceHolder;
    public TextView textViewFileName;
    public TextView textViewFileInfo;
    public ImageView imageViewInfoIcon;
    public View infoButton;
    @Nullable
    public AppCompatImageButton dragButton;
    @Nullable
    public CheckBox checkBox;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public ContentViewHolder(View itemView) {
        super(itemView);
        thumbnailContainer = itemView.findViewById(R.id.thumbnail_container);
        imageViewFileIcon = itemView.findViewById(R.id.file_icon);
        imageViewFileLockIcon = itemView.findViewById(R.id.file_lock_icon);
        docTextPlaceHolder = itemView.findViewById(R.id.docTextPlaceHolder);
        textViewFileName = itemView.findViewById(R.id.file_name);
        textViewFileInfo = itemView.findViewById(R.id.file_info);
        imageViewInfoIcon = itemView.findViewById(R.id.info_icon);
        infoButton = itemView.findViewById(R.id.info_button);
        dragButton = itemView.findViewById(R.id.drag_icon);
        checkBox = itemView.findViewById(R.id.check_box);

        if (Utils.isJellyBeanMR1() && textViewFileName != null && textViewFileInfo != null) {
            // instead of creating a different layout for v17 we set alignment in the code:
            if (textViewFileName.getGravity() != Gravity.CENTER) {
                textViewFileName.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
            }
            textViewFileName.setTextDirection(View.TEXT_DIRECTION_LTR);
            textViewFileInfo.setTextDirection(View.TEXT_DIRECTION_LOCALE);
        }
    }
}
