package com.pdftron.pdf.dialog.tabswitcher;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.dialog.tabswitcher.model.TabSwitcherItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import java.util.ArrayList;

/**
 * A fragment that shows all tabs, only used on phones.
 */
public class TabSwitcherDialogFragment extends CustomSizeDialogFragment {

    public static final String TAG = TabSwitcherDialogFragment.class.getName();

    protected final static String BUNDLE_SELECTED_TAB = "TabSwitcher_selected_tab";

    private static final int SPAN_COUNT = 2;

    private String mSelectedTabTag;

    private RecyclerView mRecyclerView;
    private TabSwitcherAdapter mAdapter;

    private TabSwitcherViewModel mViewModel;

    public static TabSwitcherDialogFragment newInstance(String selectedTabTag) {
        TabSwitcherDialogFragment fragment = new TabSwitcherDialogFragment();
        Bundle args = new Bundle();
        args.putString(BUNDLE_SELECTED_TAB, selectedTabTag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            mSelectedTabTag = getArguments().getString(BUNDLE_SELECTED_TAB);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_tab_switcher_dialog, container);

        Toolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.menu_tab_switcher_title);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        mRecyclerView = v.findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), SPAN_COUNT));
        mAdapter = new TabSwitcherAdapter(getContext());
        mAdapter.setSelectedTab(mSelectedTabTag);
        mRecyclerView.setAdapter(mAdapter);

        ItemClickHelper itemClickHelper = new ItemClickHelper();
        itemClickHelper.attachToRecyclerView(mRecyclerView);
        itemClickHelper.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
            @Override
            public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                if (mViewModel != null) {
                    mViewModel.onSelectTab(mAdapter.getItem(position).getTabTag());
                }
                dismiss();
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            throw new RuntimeException("This fragment should run as a child fragment of a containing parent fragment.");
        }

        mViewModel = ViewModelProviders.of(parentFragment).get(TabSwitcherViewModel.class);
        mAdapter.setViewModel(mViewModel);

        mViewModel.getItemsLiveData().observe(getViewLifecycleOwner(), new Observer<ArrayList<TabSwitcherItem>>() {
            @Override
            public void onChanged(ArrayList<TabSwitcherItem> tabSwitcherItems) {
                // emit once
                if (mAdapter != null) {
                    mAdapter.setData(tabSwitcherItems);
                }
                mViewModel.getItemsLiveData().removeObserver(this);
            }
        });

        mViewModel.getSelectedTag().observe(getViewLifecycleOwner(), new Observer<String>() {
            @Override
            public void onChanged(String s) {
                if (mAdapter != null) {
                    mAdapter.setSelectedTab(s);
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    static class Theme {
        @ColorInt
        final int backgrounColor;
        @ColorInt
        final int iconColor;
        @ColorInt
        final int textColor;
        @ColorInt
        final int selectedBorderColor;

        Theme(int backgrounColor, int iconColor, int textColor, int selectedBorderColor) {
            this.backgrounColor = backgrounColor;
            this.iconColor = iconColor;
            this.textColor = textColor;
            this.selectedBorderColor = selectedBorderColor;
        }

        static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.TabSwitcherDialogTheme, R.attr.pt_tab_switcher_dialog_style, R.style.TabSwitcherDialogTheme);

            int backgroundColor = a.getColor(R.styleable.TabSwitcherDialogTheme_backgroundColor, Utils.getBackgroundColor(context));
            int iconColor = a.getColor(R.styleable.TabSwitcherDialogTheme_iconColor, context.getResources().getColor(R.color.annot_toolbar_background_secondary));
            int textColor = a.getColor(R.styleable.TabSwitcherDialogTheme_textColor, Utils.getPrimaryTextColor(context));
            int selectedBorderColor = a.getColor(R.styleable.TabSwitcherDialogTheme_selectedBorderColor, Utils.getAccentColor(context));
            a.recycle();

            return new Theme(backgroundColor, iconColor, textColor, selectedBorderColor);
        }
    }
}
