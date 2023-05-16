package com.pdftron.pdf.dialog.measurecount;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.CountMeasurementCreateTool;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.AnnotationPropertyPreviewView;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.preset.component.PresetBarViewModel;
import com.pdftron.pdf.widget.toolbar.ToolManagerViewModel;

import java.util.ArrayList;
import java.util.List;

public class CountToolDialogFragment extends DialogFragment {
    private CountToolAdapter mCountToolAdapter;
    public final static String TAG = CountToolDialogFragment.class.getName();
    private Toolbar mToolbar;
    private CardView mTotalCountLayout;
    private Button mCreateNewBtn;
    private MeasureCountToolViewModel mMeasureCountToolViewModel;
    private ToolManagerViewModel mToolManagerViewModel;
    private PresetBarViewModel mPresetBarViewModel;
    private AnnotStyle mAnnotStyle;
    private ConstraintLayout mMainLayout;
    private String mActivePresetLabel;

    public static final String COUNT_MODE = "CountMode";
    public static final String EDIT_MODE = "EditMode";
    public static final String PRESET_MODE = "PresetMode";
    private String mCurrentMode = PRESET_MODE;
    private String mToolbarStyleId = "";
    private PDFViewCtrl mPdfViewCtrl;

    private Theme mTheme;

    private int mTotalAnnotCount = 0;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getActivity() != null) {
            mMeasureCountToolViewModel = ViewModelProviders.of(getActivity()).get(MeasureCountToolViewModel.class);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_count_tool, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mTheme = Theme.fromContext(view.getContext());

        mMainLayout = view.findViewById(R.id.main_layout);
        mToolbar = view.findViewById(R.id.count_tool_toolbar);

        TextView totalCount = view.findViewById(R.id.total_count);
        mTotalCountLayout = view.findViewById(R.id.total_layout);
        setCountVisibility();

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCurrentMode.equals(EDIT_MODE)) {
                    setMode(PRESET_MODE);
                    if (mToolbar != null) {
                        mToolbar.getMenu().findItem(R.id.controls_action_edit).setTitle(R.string.tools_qm_edit);
                        mToolbar.setTitle(R.string.count_measurement_group_presets);
                    }
                    setCreateBtnVisibility();
                    mCountToolAdapter.notifyDataSetChanged();
                } else {
                    dismiss();
                }
            }
        });
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.controls_action_edit) {
                    if (mCurrentMode.equals(PRESET_MODE)) {
                        setMode(EDIT_MODE);
                        item.setTitle(R.string.done);
                        mToolbar.setTitle(R.string.count_measurement_edit_group_presets);
                    } else {
                        setMode(PRESET_MODE);
                        item.setTitle(R.string.tools_qm_edit);
                        mToolbar.setTitle(R.string.count_measurement_group_presets);
                    }
                    if (mCountToolAdapter != null) {
                        mCountToolAdapter.notifyDataSetChanged();
                    }
                    setCreateBtnVisibility();
                }
                return false;
            }
        });
        RecyclerView recyclerView = view.findViewById(R.id.count_tool_recycler);
        mCountToolAdapter = new CountToolAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        recyclerView.setAdapter(mCountToolAdapter);

        mCreateNewBtn = view.findViewById(R.id.count_tool_new_group_btn);
        mCreateNewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAnnotStyle.setStampId("");
                showCountMeasurementPresetCreateDialog(mAnnotStyle);
            }
        });
        if (!mCurrentMode.equals(COUNT_MODE)) {
            mMeasureCountToolViewModel.observeCountToolPresets(this, new Observer<List<MeasureCountTool>>() {
                @Override
                public void onChanged(List<MeasureCountTool> measureCountTools) {
                    mCountToolAdapter.setAnnotStyles(measureCountTools);
                }
            });
        } else {
            try {
                List<MeasureCountTool> measureCountToolList = populateAnnotCount();
                totalCount.setText(String.valueOf(mTotalAnnotCount));
                mCountToolAdapter.setAnnotStyles(measureCountToolList);
            } catch (PDFNetException e) {
                e.printStackTrace();
            }
        }

        setCreateBtnVisibility();
        adjustRecyclerView();

        // theme
        totalCount.setTextColor(mTheme.textColor);
        TextView totalCountLabel = view.findViewById(R.id.count_total_label);
        totalCountLabel.setTextColor(mTheme.textColor);
        AppCompatImageView totalCountIcon = view.findViewById(R.id.total_count_icon);
        totalCountIcon.setColorFilter(mTheme.textColor);
    }

    public void setToolManagerViewModel(ToolManagerViewModel toolManagerViewModel) {
        mToolManagerViewModel = toolManagerViewModel;
    }

    public void setPresetViewModel(PresetBarViewModel presetBarViewModel) {
        mPresetBarViewModel = presetBarViewModel;
    }

    public void setPDFViewCtrl(PDFViewCtrl pdfViewCtrl) {
        mPdfViewCtrl = pdfViewCtrl;
    }

    public void setToolbarStyleId(String toolbarStyleId) {
        mToolbarStyleId = toolbarStyleId;
    }

    public void setPresetStyle(AnnotStyle annotStyle) {
        mAnnotStyle = annotStyle;
        mActivePresetLabel = mAnnotStyle.getStampId();
    }

    public void setMode(String mode) {
        mCurrentMode = mode;
    }

    private void adjustRecyclerView() {
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(mMainLayout);
        if (mCurrentMode.equals(COUNT_MODE)) {
            constraintSet.connect(R.id.count_tool_recycler, ConstraintSet.BOTTOM, R.id.total_layout, ConstraintSet.TOP, 0);
        } else {
            constraintSet.connect(R.id.count_tool_recycler, ConstraintSet.BOTTOM, R.id.count_tool_new_group_btn, ConstraintSet.TOP, 0);
        }
        constraintSet.applyTo(mMainLayout);
    }

    private void setCreateBtnVisibility() {
        if (mCurrentMode.equals(PRESET_MODE)) {
            mCreateNewBtn.setVisibility(View.VISIBLE);
        } else {
            mCreateNewBtn.setVisibility(View.GONE);
        }
    }

    private void setCountVisibility() {
        if (mCurrentMode.equals(COUNT_MODE)) {
            mTotalCountLayout.setVisibility(View.VISIBLE);
            mToolbar.setTitle(R.string.count_measurement_summary);
        } else {
            mToolbar.inflateMenu(R.menu.controls_fragment_edit_toolbar);
            mTotalCountLayout.setVisibility(View.GONE);
            mToolbar.findViewById(R.id.controls_action_edit).setVisibility(View.VISIBLE);
        }
    }

    private void showCountMeasurementPresetCreateDialog(@NonNull AnnotStyle annotStyle) {
        ToolManager toolManager = mToolManagerViewModel.getToolManager();

        FragmentManager fragmentManager = getChildFragmentManager();
        if (toolManager == null) {
            return;
        }
        annotStyle.setAnnotType(AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT);
        annotStyle.setIcon(CountMeasurementCreateTool.COUNT_MEASURE_CHECKMARK_ICON);
        CountToolCreatePresetDialog.Builder builder = new CountToolCreatePresetDialog.Builder();
        builder.setAnnotStyle(annotStyle);
        CountToolCreatePresetDialog dialog = builder.build();
        dialog.setAnnotStyleProperties(toolManager.getAnnotStyleProperties());
        dialog.setPresetBarViewModel(mPresetBarViewModel);
        dialog.setToolbarStyleId(mToolbarStyleId);
        dialog.setListener(new CountToolCreatePresetDialog.CountToolCreatePresetDialogListener() {
            @Override
            public void onPresetChanged(String newLabel, String originalLabel, AnnotStyle annotStyle) {
                mMeasureCountToolViewModel.update(newLabel, originalLabel, annotStyle);
                if (originalLabel.equals(mActivePresetLabel)) {
                    if (mPresetBarViewModel != null) {
                        mActivePresetLabel = newLabel;
                        mPresetBarViewModel.saveCountMeasurementPreset(newLabel, annotStyle, mToolbarStyleId, 0);
                    }
                }
            }
        });
        dialog.show(fragmentManager, CountToolCreatePresetDialog.TAG);
    }

    private List<MeasureCountTool> populateAnnotCount() throws PDFNetException {
        List<MeasureCountTool> measureCountToolList = new ArrayList<>();
        if (mPdfViewCtrl != null) {
            boolean shouldUnlockRead = false;
            try {
                mPdfViewCtrl.docLockRead();
                shouldUnlockRead = true;
                PDFDoc doc = mPdfViewCtrl.getDoc();
                if (doc != null) {
                    for (int i = 1; i <= doc.getPageCount(); i++) {
                        Page page = doc.getPage(i);
                        if (page.isValid()) {
                            int annotationCount = page.getNumAnnots();
                            for (int j = annotationCount; j >= 0; j--) {
                                Annot annot = page.getAnnot(j);
                                if (annot.isValid()) {
                                    if (annot.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_KEY).equals("true")) {
                                        String label = annot.getCustomData(CountMeasurementCreateTool.COUNT_MEASURE_LABEL_KEY);
                                        boolean exists = false;
                                        for (int index = 0; index < measureCountToolList.size(); index++) {
                                            MeasureCountTool preset = measureCountToolList.get(index);
                                            if (preset.label.equals(label)) {
                                                preset.annotCount++;
                                                exists = true;
                                                break;
                                            }
                                        }
                                        if (!exists) {
                                            MeasureCountTool docPreset = new MeasureCountTool();
                                            docPreset.annotCount = 1;
                                            docPreset.label = label;
                                            docPreset.annotStyleJson = AnnotUtils.getAnnotStyle(annot).toJSONString();
                                            measureCountToolList.add(docPreset);
                                        }
                                        mTotalAnnotCount++;
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (PDFNetException e) {
                e.printStackTrace();
            } finally {
                if (shouldUnlockRead) {
                    mPdfViewCtrl.docUnlockRead();
                }
            }
        }
        return measureCountToolList;
    }

    private class CountToolAdapter extends RecyclerView.Adapter<ViewHolder> {
        private List<MeasureCountTool> mMeasureCountToolList = new ArrayList<>();

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_measurement_count, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
            MeasureCountTool preset = mMeasureCountToolList.get(position);
            AnnotStyle annotStyle = AnnotStyle.loadJSONString(preset.annotStyleJson);

            if (mCurrentMode.equals(COUNT_MODE)) {
                viewHolder.mCount.setTextColor(mTheme.textColor);
                viewHolder.mCount.setText(String.valueOf(preset.annotCount));
            }

            viewHolder.mGroupName.setText(preset.label);
            viewHolder.mGroupName.setTextColor(mTheme.textColor);
            viewHolder.mDelete.setColorFilter(mTheme.iconColor);
            viewHolder.mDelete.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deletePopUp(v.getContext(), preset);
                }
            });
            viewHolder.mFillColour.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCountMeasurementPresetCreateDialog(annotStyle);
                }
            });

            viewHolder.mEdit.setColorFilter(mTheme.iconColor);
            viewHolder.mEdit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCountMeasurementPresetCreateDialog(annotStyle);
                }
            });
            if (!mCurrentMode.equals(COUNT_MODE)) {
                viewHolder.mGroupName.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mCurrentMode.equals(EDIT_MODE)) {
                            showCountMeasurementPresetCreateDialog(annotStyle);
                        } else {
                            mPresetBarViewModel.saveCountMeasurementPreset(preset.label, annotStyle, mToolbarStyleId, 0);
                            dismiss();
                        }
                    }
                });
            }

            viewHolder.mIcon.setColorFilter(annotStyle.getColor());

            updateFillColour(annotStyle, viewHolder.mFillColour);
            hideUI(viewHolder);
        }

        private void deletePopUp(Context context, MeasureCountTool preset) {
            String message = context.getString(R.string.count_measurement_delete_group, preset.label);
            int start = message.indexOf(preset.label);
            SpannableStringBuilder sb = new SpannableStringBuilder(message);
            sb.setSpan(new StyleSpan(Typeface.BOLD), start, start + preset.label.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(sb);
            builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (mMeasureCountToolViewModel != null) {
                        mMeasureCountToolViewModel.delete(preset);
                    }
                    dialog.dismiss();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
        }

        private void hideUI(ViewHolder viewHolder) {
            switch (mCurrentMode) {
                case PRESET_MODE:
                    viewHolder.mIcon.setVisibility(View.VISIBLE);
                    viewHolder.mCount.setVisibility(View.GONE);
                    viewHolder.mEdit.setVisibility(View.GONE);
                    viewHolder.mDelete.setVisibility(View.GONE);
                    viewHolder.mFillColour.setVisibility(View.GONE);
                    break;
                case COUNT_MODE:
                    viewHolder.mIcon.setVisibility(View.VISIBLE);
                    viewHolder.mCount.setVisibility(View.VISIBLE);
                    viewHolder.mEdit.setVisibility(View.GONE);
                    viewHolder.mDelete.setVisibility(View.INVISIBLE);
                    viewHolder.mFillColour.setVisibility(View.GONE);
                    break;
                case EDIT_MODE:
                    viewHolder.mIcon.setVisibility(View.INVISIBLE);
                    viewHolder.mCount.setVisibility(View.GONE);
                    viewHolder.mEdit.setVisibility(View.VISIBLE);
                    viewHolder.mDelete.setVisibility(View.VISIBLE);
                    viewHolder.mFillColour.setVisibility(View.VISIBLE);
                    break;
            }
        }

        @Override
        public int getItemCount() {
            return mMeasureCountToolList.size();
        }

        public void setAnnotStyles(List<MeasureCountTool> measureCountToolList) {
            mMeasureCountToolList = measureCountToolList;
            notifyDataSetChanged();
        }

        private void updateFillColour(AnnotStyle annotStyle, AnnotationPropertyPreviewView fillPreview) {
            int backgroundColor = Utils.getBackgroundColor(getContext());

            // set stroke preview
            Drawable drawable;
            if (annotStyle.getColor() == Color.TRANSPARENT) {
                drawable = getContext().getResources().getDrawable(R.drawable.oval_fill_transparent);
            } else if (annotStyle.getColor() == backgroundColor) {
                if (annotStyle.hasFillColor()) {
                    drawable = getContext().getResources().getDrawable(R.drawable.ring_stroke_preview);
                } else {
                    drawable = getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
                }

                drawable.mutate();
                ((GradientDrawable) drawable).setStroke((int) Utils.convDp2Pix(getContext(), 1), Color.GRAY);
            } else {
                if (annotStyle.hasFillColor()) {
                    drawable = getContext().getResources().getDrawable(R.drawable.oval_stroke_preview);
                } else {
                    drawable = getContext().getResources().getDrawable(R.drawable.oval_fill_preview);
                }
                drawable.mutate();
                drawable.setColorFilter(annotStyle.getColor(), PorterDuff.Mode.SRC_IN);
            }
            fillPreview.setImageDrawable(drawable);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        AppCompatImageView mEdit;
        AppCompatImageView mDelete;
        AppCompatImageView mIcon;
        TextView mGroupName;
        TextView mCount;
        AnnotationPropertyPreviewView mFillColour;

        ViewHolder(View itemView) {
            super(itemView);
            mEdit = itemView.findViewById(R.id.edit);
            mDelete = itemView.findViewById(R.id.delete);
            mIcon = itemView.findViewById(R.id.group_icon);
            mFillColour = itemView.findViewById(R.id.fill_preview);
            mGroupName = itemView.findViewById(R.id.group_name);
            mCount = itemView.findViewById(R.id.count);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final class Theme {
        @ColorInt
        public final int backgroundColor;
        @ColorInt
        public final int textColor;
        @ColorInt
        public final int iconColor;

        public Theme(int backgroundColor,
                int textColor,
                int iconColor) {
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.iconColor = iconColor;
        }

        public static Theme fromContext(@NonNull Context context) {

            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.CountToolDialogTheme, R.attr.pt_count_tool_dialog_style, R.style.PTCountToolDialogTheme);

            int backgroundColor = a.getColor(R.styleable.CountToolDialogTheme_backgroundColor, Utils.getBackgroundColor(context));
            int textColor = a.getColor(R.styleable.CountToolDialogTheme_textColor, Utils.getPrimaryTextColor(context));
            int iconColor = a.getColor(R.styleable.CountToolDialogTheme_iconColor, Utils.getForegroundColor(context));
            a.recycle();

            return new Theme(backgroundColor, textColor, iconColor);
        }
    }
}
