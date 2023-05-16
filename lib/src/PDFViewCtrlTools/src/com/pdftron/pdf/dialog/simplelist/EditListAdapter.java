package com.pdftron.pdf.dialog.simplelist;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerViewAdapter;
import com.pdftron.pdf.widget.recyclerview.ViewHolderBindListener;

/**
 * This is a generic adapter used for a specific case,
 * where each item in the list is a string that can be modified,
 * and editing is triggered by clicking on the context menu
 */
public abstract class EditListAdapter<T> extends SimpleRecyclerViewAdapter<T, EditListViewHolder> {

    protected boolean mEditing;
    protected boolean mAllowEditing = true;
    protected int mSelectedIndex = -1;

    protected View mActiveEditText;

    public EditListAdapter() {
    }

    public EditListAdapter(ViewHolderBindListener listener) {
        super(listener);
    }

    public void setEditing(boolean editing) {
        if (this.mAllowEditing) {
            this.mEditing = editing;
        } else {
            this.mEditing = false;
        }
    }

    public void commitEditing() {
        if (mActiveEditText != null) {
            mActiveEditText.clearFocus();
        }
    }

    public void setAllowEditing(boolean allowEditing) {
        this.mAllowEditing = allowEditing;
    }

    public void setSelectedIndex(int index) {
        mSelectedIndex = index;
    }

    @NonNull
    @Override
    public EditListViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new EditListViewHolder(
                LayoutInflater.from(viewGroup.getContext())
                        .inflate(R.layout.dialog_edit_listview_item, viewGroup, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull final EditListViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);

        Utils.applySecondaryTextTintToButton(holder.contextButton);

        holder.contextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                contextButtonClicked(holder, v);
            }
        });

        holder.confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmButtonClicked(holder, v);
            }
        });

        holder.editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                return handleEditTextEditorAction(holder, v, actionId, event);
            }
        });

        holder.editText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mActiveEditText = v;
                } else {
                    mActiveEditText = null;
                }
                handleEditTextFocusChange(holder, v, hasFocus);
            }
        });

        if (mEditing) {
            holder.itemView.setFocusableInTouchMode(true);
            if (position == mSelectedIndex) {
                holder.textView.setVisibility(View.GONE);
                holder.contextButton.setVisibility(View.GONE);
                holder.editText.setVisibility(View.VISIBLE);
                holder.confirmButton.setVisibility(View.VISIBLE);
            }
        } else {
            holder.editText.clearFocus();
            holder.itemView.setFocusableInTouchMode(false);
            holder.textView.setVisibility(View.VISIBLE);
            if (mAllowEditing) {
                holder.contextButton.setVisibility(View.VISIBLE);
            } else {
                holder.contextButton.setVisibility(View.GONE);
            }
            holder.editText.setVisibility(View.GONE);
            holder.confirmButton.setVisibility(View.GONE);
        }
    }

    protected abstract void contextButtonClicked(@NonNull EditListViewHolder holder, View v);

    protected void confirmButtonClicked(@NonNull EditListViewHolder holder, View v) {
        holder.itemView.requestFocus();
    }

    protected boolean handleEditTextEditorAction(@NonNull EditListViewHolder holder, TextView v, int actionId, KeyEvent event) {
        int pos = holder.getAdapterPosition();
        if (pos == RecyclerView.NO_POSITION) {
            return false;
        }
        if (actionId == EditorInfo.IME_ACTION_DONE || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
            v.clearFocus();
            return true;
        }
        return false;
    }

    protected abstract void handleEditTextFocusChange(@NonNull EditListViewHolder holder, View v, boolean hasFocus);

    protected boolean isValidIndex(int position) {
        if (position >= 0 && position < getItemCount()) {
            return true;
        }
        return false;
    }
}
