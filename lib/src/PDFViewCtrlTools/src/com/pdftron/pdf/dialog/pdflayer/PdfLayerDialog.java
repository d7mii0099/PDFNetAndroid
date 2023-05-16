package com.pdftron.pdf.dialog.pdflayer;

import android.content.Context;
import android.os.Bundle;
import androidx.annotation.NonNull;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import java.util.ArrayList;

public class PdfLayerDialog extends BottomSheetDialog {

    private PdfLayerView mPdfLayerView;
    private PDFViewCtrl mPdfViewCtrl;
    private ArrayList<PdfLayer> mLayers;

    public PdfLayerDialog(@NonNull Context context,
                          PDFViewCtrl pdfViewCtrl) {
        super(context);
        init(context);

        mPdfViewCtrl = pdfViewCtrl;
    }

    private void init(@NonNull Context context) {
        View view = LayoutInflater.from(context).inflate(R.layout.controls_pdf_layers_layout, null);
        mPdfLayerView = view.findViewById(R.id.pdf_layer_view);
        setContentView(view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean shouldUnlockRead = false;
        try {
            mPdfViewCtrl.docLockRead();
            shouldUnlockRead = true;
            mLayers = PdfLayerUtils.getLayers(mPdfViewCtrl, mPdfViewCtrl.getDoc());
        } catch (Exception ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        } finally {
            if (shouldUnlockRead) {
                mPdfViewCtrl.docUnlockRead();
            }
        }
        mPdfLayerView.setup(mLayers);
        mPdfLayerView.getRecyclerView().setLayoutManager(
            new GridLayoutManager(getContext(), 2));
        mPdfLayerView.getItemClickHelper().setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                PdfLayerViewAdapter adapter = mPdfLayerView.getAdapter();
                if (adapter != null) {
                    PdfLayer layer = adapter.getItem(position);
                    if (layer != null && layer.isChecked() != null) {
                        layer.setChecked(!layer.isChecked());
                        adapter.notifyItemChanged(position);
                        try {
                            PdfLayerUtils.setLayerCheckedChange(mPdfViewCtrl, layer.getGroup(), layer.isChecked());
                        } catch (Exception ex) {
                            AnalyticsHandlerAdapter.getInstance().sendException(ex);
                        }
                    }
                }
            }
        });
    }
}
