package com.pdftron.pdf.dialog.pdflayer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.pdf.tools.R;

import java.util.ArrayList;

/**
 * Adapter for {@link PdfLayerView}.
 */
public class PdfLayerViewAdapter extends RecyclerView.Adapter<PdfLayerViewAdapter.ViewHolder> {

    @NonNull
    final private ArrayList<PdfLayer> mLayers;

    public PdfLayerViewAdapter(@NonNull ArrayList<PdfLayer> layers) {
        mLayers = layers;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.view_pdf_layer_item, viewGroup, false);
        return new PdfLayerViewAdapter.ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int i) {
        try {
            viewHolder.mSwitch.setText(mLayers.get(i).getName());
            viewHolder.mSwitch.setChecked(mLayers.get(i).isChecked());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Nullable
    public PdfLayer getItem(int i) {
        return mLayers.get(i);
    }

    @Override
    public int getItemCount() {
        return mLayers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        SwitchCompat mSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mSwitch = itemView.findViewById(R.id.layer_switch);
        }
    }
}
