//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.pdftron.pdf.model.FontResource;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for fonts
 */
public class FontAdapter extends ArrayAdapter<FontResource> {

    private Context mContext;
    private List<FontResource> mSource;
    private int mLayoutResourceId;
    private int mDropDownResourceId;

    public FontAdapter(Context context, int textViewResource, List<FontResource> list) {
        super(context, textViewResource, list);

        mContext = context;
        mSource = list;
        mLayoutResourceId = textViewResource;
    }

    @Override
    public void setDropDownViewResource(int resource) {
        super.setDropDownViewResource(resource);
        mDropDownResourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(mLayoutResourceId, parent, false);
        }

        if (convertView instanceof TextView) {
            // get the language and set the text as the language name
            TextView textView = (TextView) convertView;
            FontResource font = mSource.get(position);
            textView.setText(font.getDisplayName());
            try {
                Typeface typeFace = Typeface.createFromFile(font.getFilePath());
                textView.setTypeface(typeFace);
            } catch (Exception ignored) { // when font not found
            }
        }

        return convertView;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        // cannot reuse convertView since for the first position we want the height
        // of the text view to be zero - this hides the font selection hint
        convertView = LayoutInflater.from(mContext).inflate(mDropDownResourceId, parent, false);

        if (convertView instanceof TextView) {
            // get the language and set the text as the language name
            TextView textView = (TextView) convertView;
            FontResource font = mSource.get(position);
            textView.setText(font.getDisplayName());
            try {
                Typeface typeFace = Typeface.createFromFile(font.getFilePath());
                textView.setTypeface(typeFace);
            } catch (Exception ignored) { // when font not found
            }
        }

        if (position == 0) {
            return new View(mContext);
        }
        return convertView;
    }

    @Override
    public int getPosition(FontResource font) {
        if (font == null) {
            return -1;
        }

        String filePath = font.getFilePath();
        for (int i = 0; i < mSource.size(); i++) {
            if (mSource.get(i).getFilePath().equals(filePath)) {
                return i;
            }
        }
        return -1;
    }

    public void setData(List<FontResource> data) {
        if (mSource == null) {
            mSource = new ArrayList<>();
        }
        mSource.clear();
        mSource.addAll(data);
        notifyDataSetChanged();
    }

    public List<FontResource> getData() {
        if (mSource == null) {
            mSource = new ArrayList<>();
        }
        return mSource;
    }
}
