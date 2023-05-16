package com.pdftron.pdf.dialog.simplelist;

import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.tools.R;

public class EditListViewHolder extends RecyclerView.ViewHolder {

    public final TextView textView;
    public final EditText editText;
    public final ImageButton contextButton;
    public final ImageButton confirmButton;
    public final TextView pageNumber;

    public EditListViewHolder(@NonNull View itemView) {
        super(itemView);

        this.textView = itemView.findViewById(R.id.text_view);
        this.editText = itemView.findViewById(R.id.edit_text);
        this.contextButton = itemView.findViewById(R.id.context_button);
        this.confirmButton = itemView.findViewById(R.id.confirm_button);
        this.pageNumber = itemView.findViewById(R.id.page_number);
    }
}
