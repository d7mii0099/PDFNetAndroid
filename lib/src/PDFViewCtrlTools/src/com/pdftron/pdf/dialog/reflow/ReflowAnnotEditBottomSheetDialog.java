package com.pdftron.pdf.dialog.reflow;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ReflowAnnotEditBottomSheetDialog extends BottomSheetDialog {

    public static final int ITEM_STYLE = 0;
    public static final int ITEM_NOTE = 1;
    public static final int ITEM_DELETE = 2;

    private final ItemClickHelper mItemClickHelper;

    public ReflowAnnotEditBottomSheetDialog(@NonNull Context context) {
        super(context);

        setContentView(LayoutInflater.from(context).inflate(R.layout.dialog_reflow_annot_edit, null));

        SimpleRecyclerView recyclerView = findViewById(R.id.recycler_view);
        List<ReflowAnnotEditItem> items = new ArrayList<>(3);
        items.add(new ReflowAnnotEditItem(R.drawable.ic_color_lens_black_24dp, R.string.tools_qm_appearance));
        items.add(new ReflowAnnotEditItem(R.drawable.ic_annotation_sticky_note_black_24dp, R.string.tools_qm_note));
        items.add(new ReflowAnnotEditItem(R.drawable.ic_delete_black_24dp, R.string.delete));
        ReflowAnnotEditAdapter adapter = new ReflowAnnotEditAdapter(items);
        recyclerView.setAdapter(adapter);

        mItemClickHelper = new ItemClickHelper();
        mItemClickHelper.attachToRecyclerView(recyclerView);
    }

    public void setOnItemClickListener(ItemClickHelper.OnItemClickListener listener) {
        if (mItemClickHelper != null) {
            mItemClickHelper.setOnItemClickListener(listener);
        }
    }
}
