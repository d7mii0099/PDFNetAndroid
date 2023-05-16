package com.pdftron.pdf.dialog.annotlist;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterAuthorItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterColorItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterHeaderItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterReviewStatusItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterStateItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterTypeItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationFilterOptionAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.viewmodel.AnnotationFilterViewModel;

import java.util.ArrayList;
import java.util.Set;

public class AnnotationFilterDialogFragment extends CustomSizeDialogFragment {
    public static final String TAG = AnnotationFilterDialogFragment.class.getName();

    private AnnotationFilterViewModel mAnnotationFilterViewModel;
    private Theme mTheme;

    @NonNull
    public static AnnotationFilterDialogFragment newInstance() {
        return new AnnotationFilterDialogFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentActivity activity = getActivity();
        if (activity != null) {
            mTheme = Theme.fromContext(activity);
            if (getParentFragment() != null) {
                mAnnotationFilterViewModel = ViewModelProviders.of(getParentFragment(),
                        new AnnotationFilterViewModel.Factory(
                                activity.getApplication(),
                                new AnnotationListFilterInfo(AnnotationListFilterInfo.FilterState.OFF)
                        )
                ).get(AnnotationFilterViewModel.class);
            }
        }
    }

    @SuppressLint("WrongConstant")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_annotation_filter, container);
        RecyclerView recyclerView = v.findViewById(R.id.annotation_filter_dialog_container);
        final AnnotationFilterOptionAdapter adapter = new AnnotationFilterOptionAdapter(v.getContext(), mTheme);
        adapter.addOnFilterChangeEventListener(new AnnotationFilterOptionAdapter.OnFilterChangeEventListener() {
            @Override
            public void onShowAllPressed() {
                mAnnotationFilterViewModel.onShowAllPressed();
            }

            @Override
            public void onHideAllPressed() {
                mAnnotationFilterViewModel.onHideAllPressed();
            }

            @Override
            public void onApplyFilterPressed() {
                mAnnotationFilterViewModel.onApplyFilterPressed();
            }

            @Override
            public void onApplyFilterToAnnotationListPressed() {
                mAnnotationFilterViewModel.onApplyFilterToAnnotationListPressed();
            }

            @Override
            public void onTypeClicked(int type) {
                mAnnotationFilterViewModel.onTypeClicked(type);
            }

            @Override
            public void onAuthorClicked(@NonNull String author) {
                mAnnotationFilterViewModel.onAuthorClicked(author);
            }

            @Override
            public void onStatusClicked(@NonNull String status) {
                mAnnotationFilterViewModel.onStatusClicked(status);
            }

            @Override
            public void onColorClicked(@NonNull String color) {
                mAnnotationFilterViewModel.onColorClicked(color);
            }
        });
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        mAnnotationFilterViewModel.getAnnotationFilterLiveData().observe(this,
                new Observer<AnnotationListFilterInfo>() {
                    @Override
                    public void onChanged(AnnotationListFilterInfo filterInfo) {
                        if (filterInfo != null) {
                            adapter.setData(filterInfo, createAnnotationFilterItemList(filterInfo));
                        }
                    }
                }
        );

        Toolbar toolbar = v.findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.annotation_filter_title));
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        toolbar.inflateMenu(R.menu.annotation_filter_reset);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_reset) {
                    mAnnotationFilterViewModel.deselectAllFilters();
                    adapter.notifyDataSetChanged();
                    return true;
                }
                return false;
            }
        });
        v.setBackgroundColor(mTheme.secondaryBackgroundColor);
        return v;
    }

    private ArrayList<AnnotationFilterItem> createAnnotationFilterItemList(@NonNull AnnotationListFilterInfo filterInfo) {
        ArrayList<AnnotationFilterItem> list = new ArrayList<>();
        list.add(new AnnotationFilterStateItem());

        if (!filterInfo.getStatusSet().isEmpty()) {
            list.add(new AnnotationFilterHeaderItem(getResources().getString(R.string.annotation_filter_title_status)));
            for (StatusState status : filterInfo.getStatusSet()) {
                list.add(new AnnotationFilterReviewStatusItem(
                        status.status,
                        status.status,
                        status.selected,
                        filterInfo.isStatusEnabled())
                );
            }
        }

        if (!(filterInfo.getAuthorSet().isEmpty() || (filterInfo.getAuthorSet().size() == 1 && filterInfo.containsAuthor("")))) {
            list.add(new AnnotationFilterHeaderItem(getResources().getString(R.string.annotation_filter_title_author)));
            AnnotationFilterAuthorItem nullAuthor = null;
            for (AuthorState author : filterInfo.getAuthorSet()) {
                if (author.name.isEmpty()) {
                    nullAuthor = new AnnotationFilterAuthorItem(
                            getResources().getString((R.string.annotation_filter_author_guest)),
                            author.name,
                            author.selected,
                            filterInfo.isAuthorEnabled()
                    );
                } else {
                    list.add(
                            new AnnotationFilterAuthorItem(
                                    author.name,
                                    author.name,
                                    author.selected,
                                    filterInfo.isAuthorEnabled())
                    );
                }
            }

            // Add empty author to the end
            if (nullAuthor != null) {
                list.add(nullAuthor);
            }
        }

        list.add(new AnnotationFilterHeaderItem(getResources().getString(R.string.annotation_filter_title_type)));
        for (TypeState typeId : filterInfo.getTypeSet()) {
            Context context = getContext();
            if (context != null) {
                String type = AnnotUtils.getAnnotTypeAsString(context, typeId.type);
                list.add(new AnnotationFilterTypeItem(
                        type,
                        typeId.type,
                        typeId.selected,
                        filterInfo.isTypeEnabled())
                );
            }
        }

        list.add(new AnnotationFilterHeaderItem(getResources().getString(R.string.annotation_filter_title_color)));
        Set<ColorState> colorSet = filterInfo.getColorSet();
        ArrayList<String> allColors = new ArrayList<>();
        ArrayList<String> selectedColors = new ArrayList<>();
        for (ColorState colorState : colorSet) {
            if (colorState.selected) {
                selectedColors.add(colorState.color);
            }
            allColors.add(colorState.color);
        }
        list.add(new AnnotationFilterColorItem(
                selectedColors,
                allColors,
                filterInfo.isColorEnabled())
        );
        return list;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Fragment parentFragment = getParentFragment();
        if (parentFragment == null) {
            throw new RuntimeException("This fragment should run as a child fragment of a containing parent fragment.");
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static class Theme {

        @ColorInt
        public final int backgroundColor;
        @ColorInt
        public final int secondaryBackgroundColor;
        @ColorInt
        public final int iconColor;
        @Nullable
        public final ColorStateList textColor;
        @Nullable
        public final ColorStateList headerTextColor;

        public Theme(int backgroundColor,
                int secondaryBackgroundColor,
                int iconColor,
                @Nullable ColorStateList textColor,
                @Nullable ColorStateList headerTextColor) {
            this.backgroundColor = backgroundColor;
            this.secondaryBackgroundColor = secondaryBackgroundColor;
            this.iconColor = iconColor;
            this.textColor = textColor;
            this.headerTextColor = headerTextColor;
        }

        public static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.AnnotationFilterDialogTheme, R.attr.pt_annotation_filter_dialog_style, R.style.PTAnnotationFilterDialogTheme);
            int backgroundColor = a.getColor(R.styleable.AnnotationFilterDialogTheme_colorBackground, Utils.getBackgroundColor(context));
            int secondaryBackgroundColor = a.getColor(R.styleable.AnnotationFilterDialogTheme_secondaryBackgroundColor, context.getResources().getColor(R.color.annotation_filter_item_background));
            int iconColor = a.getColor(R.styleable.AnnotationFilterDialogTheme_iconColor, context.getResources().getColor(R.color.toolbar_icon));
            ColorStateList textColor = a.getColorStateList(R.styleable.AnnotationFilterDialogTheme_textColor);
            ColorStateList headerTextColor = a.getColorStateList(R.styleable.AnnotationFilterDialogTheme_headerTextColor);
            a.recycle();

            return new Theme(backgroundColor, secondaryBackgroundColor,
                    iconColor,
                    textColor,
                    headerTextColor);
        }
    }
}
