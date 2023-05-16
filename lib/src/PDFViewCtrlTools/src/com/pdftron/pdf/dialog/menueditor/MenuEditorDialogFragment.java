package com.pdftron.pdf.dialog.menueditor;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import java.util.ArrayList;

import co.paulburke.android.itemtouchhelperdemo.helper.SimpleItemTouchHelperCallback;

public class MenuEditorDialogFragment extends CustomSizeDialogFragment {

    public interface MenuEditorDialogFragmentListener {
        void onMenuEditorDialogDismiss();
    }

    private final static String BUNDLE_TOOLBAR_TITLE = "MenuEditor_toolbar_title";

    public static final String TAG = MenuEditorDialogFragment.class.getName();

    private static final int SPAN_COUNT = 5;

    private String mToolbarTitle;

    private RecyclerView mRecyclerView;
    private MenuEditorAdapter mAdapter;

    private MenuEditorViewModel mViewModel;

    private MenuEditorDialogFragmentListener mListener;

    private Theme mTheme;

    public static MenuEditorDialogFragment newInstance() {
        return new MenuEditorDialogFragment();
    }

    public static MenuEditorDialogFragment newInstance(String toolbarTitle) {
        MenuEditorDialogFragment fragment = new MenuEditorDialogFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_TOOLBAR_TITLE, toolbarTitle);
        fragment.setArguments(args);
        return fragment;
    }

    public void setMenuEditorDialogFragmentListener(MenuEditorDialogFragmentListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mToolbarTitle = getArguments().getString(BUNDLE_TOOLBAR_TITLE, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu_editor_dialog, container);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        if (Utils.isNullOrEmpty(mToolbarTitle)) {
            toolbar.setTitle(R.string.action_edit_menu);
        } else {
            toolbar.setTitle(String.format(view.getContext().getResources().getString(R.string.menu_editor_title), mToolbarTitle));
        }
        toolbar.inflateMenu(R.menu.fragment_menu_editor);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_reset) {
                    mViewModel.onReset();
                    mViewModel.getItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
                        @Override
                        public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                            // emit once
                            if (mAdapter != null) {
                                mAdapter.setData(menuEditorItems);
                            }
                            mViewModel.getItemsLiveData().removeObserver(this);
                        }
                    });
                    return true;
                }
                return false;
            }
        });

        mRecyclerView = view.findViewById(R.id.recycler_view);

        // layout manager
        final GridLayoutManager layoutManager = new GridLayoutManager(getContext(), SPAN_COUNT);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                switch (mAdapter.getItemViewType(position)) {
                    case MenuEditorAdapter.VIEW_TYPE_HEADER:
                        return layoutManager.getSpanCount();
                    case MenuEditorAdapter.VIEW_TYPE_CONTENT:
                    default:
                        return 1;
                }
            }
        });
        mRecyclerView.setLayoutManager(layoutManager);

        // adapter
        mAdapter = new MenuEditorAdapter();
        mRecyclerView.setAdapter(mAdapter);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);

        SimpleItemTouchHelperCallback simpleItemTouchHelperCallback = new SimpleItemTouchHelperCallback(mAdapter, SPAN_COUNT, false, false);
        simpleItemTouchHelperCallback.setAllowDragAmongSections(true);
        final ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchHelperCallback);
        itemTouchHelper.attachToRecyclerView(mRecyclerView);

        itemClickHelper.setOnItemLongClickListener(new ItemClickHelper.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(RecyclerView recyclerView, View view, final int position, long id) {
                mRecyclerView.post(new Runnable() {
                    @Override
                    public void run() {
                        if (!mAdapter.isHeader(position)) {
                            RecyclerView.ViewHolder holder = mRecyclerView.findViewHolderForAdapterPosition(position);
                            if (holder != null) {
                                mAdapter.setDragging(true);
                                mAdapter.notifyHeadersChanged();
                                itemTouchHelper.startDrag(holder);
                            }
                        }
                    }
                });
                return true;
            }
        });

        // Setup colors
        mTheme = Theme.fromContext(view.getContext());

        View background = view.findViewById(R.id.background);
        background.setBackgroundColor(mTheme.backgroundColor);

        // Colors for pinned area
        View pinnedLayout = view.findViewById(R.id.pinned_layout);
        pinnedLayout.setBackgroundColor(mTheme.pinnedBackgroundColor);
        Utils.updateDashedLineColor(mRecyclerView, mTheme.dottedLineColor);
        TextView label = view.findViewById(R.id.label);
        label.setTextColor(mTheme.textColor);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            throw new RuntimeException("This fragment should run as a child fragment of a containing parent fragment.");
        }

        mViewModel = ViewModelProviders.of(parentFragment).get(MenuEditorViewModel.class);
        mAdapter.setViewModel(mViewModel);

        mViewModel.getItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<MenuEditorItem>>() {
            @Override
            public void onChanged(ArrayList<MenuEditorItem> menuEditorItems) {
                // emit once
                if (mAdapter != null) {
                    mAdapter.setData(menuEditorItems);
                }
                mViewModel.getItemsLiveData().removeObserver(this);
            }
        });
    }

    /**
     * Returns whether the toolbar items have been modified.
     */
    public boolean hasModifiedToolbar() {
        if (mAdapter != null) {
            return mAdapter.orderHasBeenModified();
        } else {
            return false;
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);

        if (mListener != null) {
            mListener.onMenuEditorDialogDismiss();
        }
    }
}
