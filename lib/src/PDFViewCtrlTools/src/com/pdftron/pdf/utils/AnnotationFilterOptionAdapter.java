package com.pdftron.pdf.utils;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.pdf.controls.PresetColorGridView;
import com.pdftron.pdf.dialog.annotlist.AnnotationFilterDialogFragment;
import com.pdftron.pdf.dialog.annotlist.AnnotationListFilterInfo;
import com.pdftron.pdf.dialog.annotlist.AnnotationListFilterUtil;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterAuthorItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterColorItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterHeaderItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterReviewStatusItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterStateItem;
import com.pdftron.pdf.dialog.annotlist.model.AnnotationFilterTypeItem;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.InertCheckBox;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.List;

public class AnnotationFilterOptionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_STATE = 1;
    public static final int VIEW_TYPE_AUTHOR = 2;
    public static final int VIEW_TYPE_REVIEW_STATUS = 3;
    public static final int VIEW_TYPE_ANNOT_TYPE = 4;
    public static final int VIEW_TYPE_COLOR = 5;

    @NonNull
    protected final ArrayList<AnnotationFilterItem> mAnnotationFilterItems = new ArrayList<>();
    @Nullable
    protected AnnotationListFilterInfo mFilterInfo;

    private final List<OnFilterChangeEventListener> mEventListeners = new ArrayList<>();

    private final AnnotationFilterDialogFragment.Theme mTheme;

    public interface OnFilterChangeEventListener {
        void onShowAllPressed();

        void onHideAllPressed();

        void onApplyFilterPressed();

        void onApplyFilterToAnnotationListPressed();

        void onTypeClicked(int type);

        void onAuthorClicked(@NonNull String author);

        void onStatusClicked(@NonNull String status);

        void onColorClicked(@NonNull String color);
    }

    public AnnotationFilterOptionAdapter(@NonNull Context context, @NonNull AnnotationFilterDialogFragment.Theme theme) {
        mTheme = theme;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_annotation_filter_header, parent, false);
            return new HeaderViewHolder(view);
        } else if (viewType == VIEW_TYPE_STATE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_annotation_filter_radio, parent, false);
            return new RadioViewHolder(view);
        } else if (viewType == VIEW_TYPE_AUTHOR || viewType == VIEW_TYPE_REVIEW_STATUS || viewType == VIEW_TYPE_ANNOT_TYPE) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_annotation_filter_checkbox, parent, false);
            return new CheckBoxViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_annotation_filter_color, parent, false);
            return new ColorViewHolder(view);
        }
    }

    public void addOnFilterChangeEventListener(@NonNull OnFilterChangeEventListener listener) {
        mEventListeners.add(listener);
    }

    public void removeOnFilterChangeEventListener(@NonNull OnFilterChangeEventListener listener) {
        mEventListeners.remove(listener);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_HEADER) {
            AnnotationFilterHeaderItem item = (AnnotationFilterHeaderItem) mAnnotationFilterItems.get(position);
            ((HeaderViewHolder) holder).mTitle.setText(item.getTitle());
        } else if (holder.getItemViewType() == VIEW_TYPE_STATE) {
            RadioGroup rg = ((RadioViewHolder) holder).mRadioGroup;
            RadioButton radioButtonShowAll = rg.findViewById(R.id.radio_show_all);
            RadioButton radioButtonHideAll = rg.findViewById(R.id.radio_hide_all);
            RadioButton radioButtonApplyFilter = rg.findViewById(R.id.radio_apply_filter);
            RadioButton radioButtonApplyFilterToAnnotationList = rg.findViewById(R.id.radio_apply_filter_annotation_list);

            if (mFilterInfo != null) {
                radioButtonShowAll.setChecked(mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.OFF);
                radioButtonHideAll.setChecked(mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.HIDE_ALL);
                radioButtonApplyFilter.setChecked(mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.ON);
                radioButtonApplyFilterToAnnotationList.setChecked(mFilterInfo.getFilterState() == AnnotationListFilterInfo.FilterState.ON_LIST_ONLY);
            }

            radioButtonShowAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onShowAllPressed();
                        }
                    }
                }
            });
            radioButtonHideAll.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onHideAllPressed();
                        }
                    }
                }
            });
            radioButtonApplyFilter.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onApplyFilterPressed();
                        }
                    }
                }
            });
            radioButtonApplyFilterToAnnotationList.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onApplyFilterToAnnotationListPressed();
                        }
                    }
                }
            });
        } else if (holder.getItemViewType() == VIEW_TYPE_AUTHOR) {
            AnnotationFilterAuthorItem item = (AnnotationFilterAuthorItem) mAnnotationFilterItems.get(position);
            InertCheckBox cb = ((CheckBoxViewHolder) holder).mCheckBox;
            View row = ((CheckBoxViewHolder) holder).mLayout;
            cb.setEnabled(item.isEnabled());
            row.setEnabled(item.isEnabled());
            cb.setText(item.getTitle());
            final String tag = item.getTag();
            cb.setChecked(item.isSelected());
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (OnFilterChangeEventListener listener : mEventListeners) {
                        listener.onAuthorClicked(tag);
                    }
                }
            });
        } else if (holder.getItemViewType() == VIEW_TYPE_REVIEW_STATUS || holder.getItemViewType() == VIEW_TYPE_ANNOT_TYPE) {
            Context context = ((CheckBoxViewHolder) holder).mLayout.getContext();
            InertCheckBox cb = ((CheckBoxViewHolder) holder).mCheckBox;
            View row = ((CheckBoxViewHolder) holder).mLayout;
            Drawable img = null;
            if (holder.getItemViewType() == VIEW_TYPE_REVIEW_STATUS) {
                AnnotationFilterReviewStatusItem item = (AnnotationFilterReviewStatusItem) mAnnotationFilterItems.get(position);
                cb.setEnabled(item.isEnabled());
                row.setEnabled(item.isEnabled());
                final String tag = item.getTag();
                cb.setText(item.getTitle());
                img = context.getResources().getDrawable(AnnotationListFilterUtil.getReviewStatusImageResId(item.getTag())).mutate();
                cb.setChecked(item.isSelected());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onStatusClicked(tag);
                        }
                    }
                });
            } else {
                AnnotationFilterTypeItem item = (AnnotationFilterTypeItem) mAnnotationFilterItems.get(position);
                cb.setEnabled(item.isEnabled());
                row.setEnabled(item.isEnabled());
                cb.setText(WordUtils.capitalize(item.getTitle()));
                final int type = item.getTag();
                img = context.getResources().getDrawable(AnnotUtils.getAnnotImageResId(type)).mutate();
                cb.setChecked(item.isSelected());
                row.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onTypeClicked(type);
                        }
                    }
                });
            }
            int color = mTheme.iconColor;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                DrawableCompat.setTint(img, color);
            } else {
                img.mutate().setColorFilter(color, PorterDuff.Mode.SRC_IN);
            }
            cb.setCompoundDrawablesWithIntrinsicBounds(img, null, null, null);
        } else {
            AnnotationFilterColorItem item = (AnnotationFilterColorItem) mAnnotationFilterItems.get(position);
            final ColorPickerGridViewAdapter adapter = new ColorPickerGridViewAdapter(((ColorViewHolder) holder).mLayout.getContext(), item.getAllColors());
            final ArrayList<String> selectedList = item.getSelectedColors();
            adapter.setSelectedList(selectedList);
            final PresetColorGridView colorGridView = ((ColorViewHolder) holder).mColorView;
            colorGridView.setAdapter(adapter);
            colorGridView.setEnabled(item.isEnabled());
            colorGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String color = adapter.getItem(position);
                    if (color != null) {
                        for (OnFilterChangeEventListener listener : mEventListeners) {
                            listener.onColorClicked(color);
                        }
                    }

                    String c = adapter.getItem(position);
                    if (selectedList.contains(c)) {
                        selectedList.remove(c);
                    } else {
                        selectedList.add(adapter.getItem(position));
                    }
                    adapter.notifyDataSetChanged();
                }
            });

            //delay the display of color gridview as it will deprive the focus of scrollview
            colorGridView.post(new Runnable() {
                @Override
                public void run() {
                    colorGridView.setVisibility(View.VISIBLE);
                }
            });
        }
    }

    public AnnotationFilterItem getItem(int position) {
        return mAnnotationFilterItems.get(position);
    }

    @Override
    public int getItemViewType(int position) {
        AnnotationFilterItem item = mAnnotationFilterItems.get(position);
        if (item instanceof AnnotationFilterHeaderItem) {
            return VIEW_TYPE_HEADER;
        } else if (item instanceof AnnotationFilterStateItem) {
            return VIEW_TYPE_STATE;
        } else if (item instanceof AnnotationFilterAuthorItem) {
            return VIEW_TYPE_AUTHOR;
        } else if (item instanceof AnnotationFilterReviewStatusItem) {
            return VIEW_TYPE_REVIEW_STATUS;
        } else if (item instanceof AnnotationFilterTypeItem) {
            return VIEW_TYPE_ANNOT_TYPE;
        } else {
            return VIEW_TYPE_COLOR;
        }
    }

    public void setData(@NonNull AnnotationListFilterInfo filterInfo, ArrayList<AnnotationFilterItem> items) {
        mFilterInfo = filterInfo;
        mAnnotationFilterItems.clear();
        mAnnotationFilterItems.addAll(items);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mAnnotationFilterItems.size();
    }

    class CheckBoxViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mLayout;
        InertCheckBox mCheckBox;

        CheckBoxViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.layout_root);
            mCheckBox = itemView.findViewById(R.id.checkbox);
            if (mTheme.textColor != null) {
                mCheckBox.setTextColor(mTheme.textColor);
            }
            itemView.setBackgroundColor(mTheme.backgroundColor);
        }
    }

    class RadioViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mLayout;
        RadioGroup mRadioGroup;

        RadioViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.layout_root);
            mRadioGroup = itemView.findViewById(R.id.radio_group_filter_state);
            itemView.setBackgroundColor(mTheme.backgroundColor);
        }
    }

    class ColorViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mLayout;
        PresetColorGridView mColorView;

        ColorViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.layout_root);
            mColorView = itemView.findViewById(R.id.preset_colors);
            itemView.setBackgroundColor(mTheme.secondaryBackgroundColor);
        }
    }

    class HeaderViewHolder extends RecyclerView.ViewHolder {
        LinearLayout mLayout;
        TextView mTitle;

        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            mLayout = itemView.findViewById(R.id.layout_root);
            mTitle = itemView.findViewById(R.id.title);
            if (mTheme.headerTextColor != null) {
                mTitle.setTextColor(mTheme.headerTextColor);
            }
            itemView.setBackgroundColor(mTheme.secondaryBackgroundColor);
        }
    }
}
