package com.pdftron.pdf.dialog.widgetchoice;

import androidx.lifecycle.ViewModelProviders;
import android.content.DialogInterface;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.clans.fab.FloatingActionButton;
import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.dialog.simplelist.EditListAdapter;
import com.pdftron.pdf.dialog.simplelist.EditListItemTouchHelperCallback;
import com.pdftron.pdf.dialog.simplelist.EditListViewHolder;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import org.apache.commons.lang3.RandomStringUtils;

import java.util.ArrayList;
import java.util.Arrays;

import co.paulburke.android.itemtouchhelperdemo.helper.ItemTouchHelperAdapter;

public class ChoiceDialogFragment extends CustomSizeDialogFragment {

    public final static String TAG = ChoiceDialogFragment.class.getName();

    public static final String WIDGET = "ChoiceDialogFragment_WIDGET";
    public static final String WIDGET_PAGE = "ChoiceDialogFragment_WIDGET_PAGE";
    public static final String FIELD_TYPE = "ChoiceDialogFragment_FIELD_TYPE";
    public static final String SELECTION_TYPE = "ChoiceDialogFragment_SELECTION_TYPE";
    public static final String EXISTING_OPTIONS = "ChoiceDialogFragment_EXISTING_OPTIONS";

    private long mWidget;
    private int mPage;
    private boolean mSingleChoice;
    private boolean mIsCombo;
    @Nullable
    private String[] mOptions;

    private SimpleRecyclerView mRecyclerView;
    private ChoiceAdapter mAdapter;

    private ItemTouchHelper mItemTouchHelper;
    private EditListItemTouchHelperCallback mTouchCallback;

    private FloatingActionButton mFab;

    private ChoiceViewModel mViewModel;

    private boolean mModified;

    public static ChoiceDialogFragment newInstance(long widget, int page, boolean isSingleChoice, boolean isCombo) {
        return newInstance(widget, page, isSingleChoice, isCombo, null);
    }

    public static ChoiceDialogFragment newInstance(long widget, int page, boolean isSingleChoice, boolean isCombo, @Nullable String[] options) {
        ChoiceDialogFragment dialog = new ChoiceDialogFragment();
        Bundle args = new Bundle();
        args.putLong(WIDGET, widget);
        args.putInt(WIDGET_PAGE, page);
        args.putBoolean(FIELD_TYPE, isCombo);
        args.putBoolean(SELECTION_TYPE, isSingleChoice);
        args.putStringArray(EXISTING_OPTIONS, options);
        dialog.setArguments(args);
        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mWidget = args.getLong(WIDGET);
            mPage = args.getInt(WIDGET_PAGE);
            mIsCombo = args.getBoolean(FIELD_TYPE);
            mSingleChoice = args.getBoolean(SELECTION_TYPE);
            mOptions = args.getStringArray(EXISTING_OPTIONS);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return null;
        }

        View view = inflater.inflate(R.layout.fragment_widget_choice_dialog, container);

        mViewModel = ViewModelProviders.of(activity).get(ChoiceViewModel.class);

        // setup components
        mRecyclerView = view.findViewById(R.id.recycler_view);
        mRecyclerView.initView(0, 0);
        ArrayList<String> options = null;
        if (mOptions != null) {
            options = new ArrayList<>(Arrays.asList(mOptions));
        }
        mAdapter = new ChoiceAdapter(options);
        mRecyclerView.setAdapter(mAdapter);

        // for dragging
        mTouchCallback = new EditListItemTouchHelperCallback(mAdapter, true, getResources().getColor(R.color.gray));
        mItemTouchHelper = new ItemTouchHelper(mTouchCallback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        // for long press
        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View view, final int position, long id) {
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        mTouchCallback.setDragging(true);
                        RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                        if (holder != null) {
                            mItemTouchHelper.startDrag(holder);
                        }
                    }
                });

                return true;
            }
        });

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.widget_choice_title);
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.inflateMenu(R.menu.fragment_widget_choice_dialog);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        if (mIsCombo) {
            // combo box is always single choice
            toolbar.getMenu().findItem(R.id.select_type).setVisible(false);
        } else {
            // list box can select single or multiple choices
            toolbar.getMenu().findItem(R.id.select_type).setVisible(true);
            if (mSingleChoice) {
                toolbar.getMenu().findItem(R.id.single_select).setChecked(true);
            } else {
                toolbar.getMenu().findItem(R.id.multiple_select).setChecked(true);
            }
        }
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.done) {
                    ChoiceResult result = new ChoiceResult(mWidget, mPage, mSingleChoice, mAdapter.getItems());
                    if (mModified) {
                        mViewModel.set(result);
                    }
                    dismiss();
                    return true;
                } else if (menuItem.getItemId() == R.id.single_select) {
                    menuItem.setChecked(true);
                    mSingleChoice = true;
                    mModified = true;
                    return true;
                } else if (menuItem.getItemId() == R.id.multiple_select) {
                    menuItem.setChecked(true);
                    mSingleChoice = false;
                    mModified = true;
                    return true;
                }
                return false;
            }
        });

        mFab = view.findViewById(R.id.add);
        mFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (null == mAdapter) {
                    return;
                }
                mAdapter.safeAddNewOption(getString(R.string.widget_choice_default_item));
                mAdapter.notifyItemInserted(mAdapter.getItemCount() - 1);
                mRecyclerView.smoothScrollToPosition(mAdapter.getItemCount() - 1);
                mModified = true;
            }
        });

        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        mViewModel.complete();
    }

    private void showPopupMenu(final int position, View anchor) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }

        PopupMenu popup = new PopupMenu(activity, anchor);
        popup.inflate(R.menu.popup_widget_choice_edit);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.item_rename) {
                    setEditingItem(true);
                    mAdapter.setSelectedIndex(position);
                    mAdapter.notifyItemChanged(position);
                    // TODO analytics
                    return true;
                } else if (menuItem.getItemId() == R.id.item_delete) {
                    mModified = true;
                    mAdapter.removeAt(position);
                    mAdapter.notifyItemRemoved(position);
                    // TODO analytics
                }
                return false;
            }
        });
        popup.show();
    }

    private void setEditingItem(boolean editing) {
        if (null == mAdapter) {
            return;
        }
        mAdapter.setEditing(editing);
        if (editing) {
            mFab.setVisibility(View.GONE);
        } else {
            mFab.setVisibility(View.VISIBLE);
        }
    }

    private class ChoiceAdapter extends EditListAdapter<String>
            implements ItemTouchHelperAdapter {

        private ArrayList<String> mOptions;

        ChoiceAdapter(@Nullable ArrayList<String> items) {
            mOptions = new ArrayList<>();
            if (null != items) {
                mOptions.addAll(items);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull EditListViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);

            holder.textView.setText(getItem(position));

            if (mEditing) {
                holder.editText.setText(getItem(position));
                holder.editText.requestFocus();
                holder.editText.selectAll();
                Utils.showSoftKeyboard(holder.editText.getContext(), null); // force
            }
        }

        public String[] getItems() {
            return mOptions.toArray(new String[0]);
        }

        @Override
        public String getItem(int position) {
            if (isValidIndex(position)) {
                return mOptions.get(position);
            }
            return null;
        }

        @Override
        public void add(String item) {
            mOptions.add(item);
        }

        @Override
        public boolean remove(String item) {
            if (mOptions.contains(item)) {
                mOptions.remove(item);
                return true;
            }
            return false;
        }

        @Override
        public String removeAt(int position) {
            if (isValidIndex(position)) {
                mOptions.remove(position);
            }
            return null;
        }

        @Override
        public void insert(String item, int position) {
            mOptions.add(position, item);
        }

        @Override
        public void updateSpanCount(int count) {

        }

        @Override
        public int getItemCount() {
            return mOptions.size();
        }

        @Override
        protected void contextButtonClicked(@NonNull EditListViewHolder holder, View v) {
            if (mEditing) {
                holder.itemView.requestFocus();
            } else {
                showPopupMenu(holder.getAdapterPosition(), v);
            }
        }

        @Override
        protected void handleEditTextFocusChange(@NonNull EditListViewHolder holder, View v, boolean hasFocus) {
            if (hasFocus) {
                return;
            }
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) {
                return;
            }
            Utils.hideSoftKeyboard(v.getContext(), v);

            saveEditTextChanges((TextView) v, pos);
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            String item = getItem(fromPosition);
            mOptions.remove(fromPosition);
            mOptions.add(toPosition, item);
            notifyItemMoved(fromPosition, toPosition);
            mModified = true;
            return true;
        }

        @Override
        public void onItemDrop(int fromPosition, int toPosition) {

        }

        @Override
        public void onItemDismiss(int position) {

        }

        public void safeAddNewOption(String option) {
            add(getValidOption(option));
        }

        private void saveEditTextChanges(TextView v, int position) {
            v.clearFocus();

            setEditingItem(false);

            String oldName = mOptions.get(position);

            String newName = v.getText().toString();
            if (newName.isEmpty()) {
                newName = oldName;
            }
            newName = getValidOption(newName);
            mOptions.set(position, newName);
            notifyItemChanged(position);
            if (!oldName.equals(newName)) {
                mModified = true;
            }
        }

        private boolean isValidOption(String option) {
            return !mOptions.contains(option);
        }

        private String getValidOption(String option) {
            if (isValidOption(option)) {
                return option;
            }
            while (!isValidOption(option)) {
                option = option + "-" + RandomStringUtils.randomAlphabetic(4);
            }
            return option;
        }
    }
}
