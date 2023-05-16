package com.pdftron.pdf.dialog.signature;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.SparseBooleanArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.adapter.SavedSignatureAdapter;
import com.pdftron.pdf.interfaces.OnSavedSignatureListener;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.ToolbarActionMode;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.preset.signature.SignatureViewModel;
import com.pdftron.pdf.widget.preset.signature.model.SignatureData;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;
import com.pdftron.pdf.widget.recyclerview.ItemSelectionHelper;
import com.pdftron.pdf.widget.recyclerview.SimpleRecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SavedSignaturePickerFragment extends Fragment {

    private OnSavedSignatureListener mOnSavedSignatureListener;

    private SavedSignatureAdapter mSavedSignatureAdapter;
    private ItemSelectionHelper mItemSelectionHelper;
    private ToolbarActionMode mActionMode;
    private Toolbar mToolbar;
    private Toolbar mCabToolbar;
    @Nullable
    private SignatureViewModel mSignatureViewModel = null;
    private Theme mTheme;
    private Button mCreateNewBtn;

    public static SavedSignaturePickerFragment newInstance() {
        return new SavedSignaturePickerFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getActivity() != null) {
            mSignatureViewModel = ViewModelProviders.of(getActivity()).get(SignatureViewModel.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_signature_picker, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = view.getContext();
        mTheme = Theme.fromContext(context);

        mCreateNewBtn = view.findViewById(R.id.create_new_signature_btn);
        mCreateNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mOnSavedSignatureListener != null) {
                    mOnSavedSignatureListener.onCreateSignatureClicked();
                }
            }
        });

        SimpleRecyclerView recyclerView = view.findViewById(R.id.stamp_list);
        recyclerView.setLayoutManager(new GridLayoutManager(context, 1));

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(recyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView parent, View view, int position, long id) {
                if (mActionMode == null) {
                    File file = mSavedSignatureAdapter.getItem(position);
                    if (mOnSavedSignatureListener != null && file != null) {
                        mOnSavedSignatureListener.onSignatureSelected(file.getAbsolutePath());
                    }
                } else {
                    mItemSelectionHelper.setItemChecked(position, !mItemSelectionHelper.isItemChecked(position));
                    mActionMode.invalidate();
                }
            }
        });

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View view, int position, long id) {
                if (mActionMode == null) {
                    mItemSelectionHelper.setItemChecked(position, true);
                    mActionMode = new ToolbarActionMode(context, mCabToolbar);
                    mActionMode.setMainToolbar(mToolbar);
                    mActionMode.startActionMode(mActionModeCallback);
                    return true;
                }
                return false;
            }
        });

        mItemSelectionHelper = new ItemSelectionHelper();
        mItemSelectionHelper.attachToRecyclerView(recyclerView);
        mItemSelectionHelper.setChoiceMode(ItemSelectionHelper.CHOICE_MODE_MULTIPLE);

        mSavedSignatureAdapter = new SavedSignatureAdapter(context, mItemSelectionHelper);
        mSavedSignatureAdapter.registerAdapterDataObserver(mItemSelectionHelper.getDataObserver());
        if (mSignatureViewModel != null) {
            mSignatureViewModel.observeSignatures(this, new Observer<List<SignatureData>>() {
                @Override
                public void onChanged(List<SignatureData> signatureDatum) {
                    List<File> signatureFiles = new ArrayList<>();
                    for (SignatureData signatureFilePath : signatureDatum) {
                        signatureFiles.add(new File(signatureFilePath.getFilePath()));
                    }
                    mSavedSignatureAdapter.setSignatures(signatureFiles);
                    mCreateNewBtn.setVisibility(SignatureDialogFragment.atMaxSignatureCount(signatureDatum.size()) ? View.GONE : View.VISIBLE);
                }
            });
        }

        recyclerView.setAdapter(mSavedSignatureAdapter);

        TextView emptyTextView = view.findViewById(R.id.new_custom_stamp_guide_text_view);
        emptyTextView.setText(R.string.signature_new_guide);

        view.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                return event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK
                        && onBackPressed();
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        mSavedSignatureAdapter.dispose();
    }

    public void setOnSavedSignatureListener(OnSavedSignatureListener listener) {
        mOnSavedSignatureListener = listener;
    }

    public void setToolbars(@NonNull Toolbar toolbar, @NonNull Toolbar cabToolbar) {
        mToolbar = toolbar;
        mCabToolbar = cabToolbar;
    }

    public void resetToolbar(final Context context) {
        if (mToolbar != null) {
            finishActionMode();
            mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {

                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (mToolbar == null || mCabToolbar == null) {
                        return false;
                    }

                    if (item.getItemId() == R.id.controls_action_edit) {
                        // Start edit-mode
                        mActionMode = new ToolbarActionMode(context, mCabToolbar);
                        mActionMode.setMainToolbar(mToolbar);
                        mActionMode.startActionMode(mActionModeCallback);
                        return true;
                    }
                    return false;
                }
            });
        }
    }

    private ToolbarActionMode.Callback mActionModeCallback = new ToolbarActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ToolbarActionMode mode, Menu menu) {
            mode.inflateMenu(R.menu.cab_fragment_saved_signature);

            if (mOnSavedSignatureListener != null) {
                mOnSavedSignatureListener.onEditModeChanged(true);
            }

            return true;
        }

        @Override
        public boolean onActionItemClicked(ToolbarActionMode mode, MenuItem item) {
            int id = item.getItemId();

            SparseBooleanArray selectedItems = mItemSelectionHelper.getCheckedItemPositions();
            int count = selectedItems.size();
            final List<Integer> indexes = new ArrayList<>();
            for (int i = 0; i < count; ++i) {
                if (selectedItems.valueAt(i)) {
                    indexes.add(selectedItems.keyAt(i));
                }
            }

            if (indexes.size() == 0) {
                return false;
            }

            if (id == R.id.controls_signature_action_delete) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setMessage(R.string.signature_dialog_delete_message)
                        .setTitle(R.string.signature_dialog_delete_title)
                        .setPositiveButton(R.string.tools_misc_yes, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // remove repeated indexes and then sort them in ascending order
                                Set<Integer> hs = new HashSet<>(indexes);
                                indexes.clear();
                                indexes.addAll(hs);
                                Collections.sort(indexes);

                                for (int i = indexes.size() - 1; i >= 0; --i) {
                                    int index = indexes.get(i);
                                    mSavedSignatureAdapter.removeAt(index);
                                    mSavedSignatureAdapter.notifyItemRemoved(index);
                                }
                                mCreateNewBtn.setVisibility(SignatureDialogFragment.atMaxSignatureCount(mSavedSignatureAdapter.getItemCount()) ? View.GONE : View.VISIBLE);

                                clearSelectedList();
                            }
                        })
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .create()
                        .show();
            }

            return true;
        }

        @Override
        public void onDestroyActionMode(ToolbarActionMode mode) {
            mActionMode = null;
            clearSelectedList();

            if (mOnSavedSignatureListener != null) {
                mOnSavedSignatureListener.onEditModeChanged(false);
            }
        }

        @Override
        public boolean onPrepareActionMode(ToolbarActionMode mode, Menu menu) {
            if (Utils.isTablet(getContext()) || getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mode.setTitle(getString(R.string.controls_thumbnails_view_selected,
                        Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount()))));
            } else {
                mode.setTitle(Utils.getLocaleDigits(Integer.toString(mItemSelectionHelper.getCheckedItemCount())));
            }
            return true;
        }
    };

    private boolean finishActionMode() {
        boolean success = false;
        if (mActionMode != null) {
            success = true;
            mActionMode.finish();
            mActionMode = null;
        }
        clearSelectedList();
        return success;
    }

    private void clearSelectedList() {
        if (mItemSelectionHelper != null) {
            mItemSelectionHelper.clearChoices();
        }
        if (mActionMode != null) {
            mActionMode.invalidate();
        }
    }

    private boolean onBackPressed() {
        if (!isAdded()) {
            return false;
        }

        boolean handled = false;
        if (mActionMode != null) {
            handled = finishActionMode();
        }
        return handled;
    }

    static class Theme {
        @ColorInt
        public final int itemBackgroundColor;

        Theme(int itemBackgroundColor) {
            this.itemBackgroundColor = itemBackgroundColor;
        }

        public static Theme fromContext(@NonNull Context context) {
            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.SavedSignaturePickerDialogTheme, R.attr.pt_saved_signature_picker_dialog_style, R.style.PTSavedSignaturePickerDialogTheme);
            int iconColor = a.getColor(R.styleable.SavedSignaturePickerDialogTheme_itemBackgroundColor, context.getResources().getColor(R.color.pt_utility_variant_color));
            a.recycle();
            return new Theme(iconColor);
        }
    }
}
