package com.pdftron.actions.ui.export;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.pdftron.actions.R;
import com.pdftron.pdf.utils.Utils;

public class ExportFileCompletionDialog extends BottomSheetDialog {

    private View mView;
    private Theme mTheme;

    public ExportFileCompletionDialog(@NonNull Context context) {
        this(context, 0);
    }

    public ExportFileCompletionDialog(@NonNull Context context, int theme) {
        super(context, theme);
        init(context);
    }

    protected ExportFileCompletionDialog(@NonNull Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
        init(context);
    }

    private void init(@NonNull Context context) {
        mView = LayoutInflater.from(context).inflate(R.layout.dialog_export_document, null);
        setContentView(mView);

        mTheme = Theme.fromContext(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView heading = mView.findViewById(R.id.heading);
        heading.setTextColor(mTheme.actionTextColor);

        ImageView saveIcon = mView.findViewById(R.id.save_icon);
        saveIcon.setColorFilter(mTheme.actionIconColor);
        TextView saveLabel = mView.findViewById(R.id.save_label);
        saveLabel.setTextColor(mTheme.actionTextColor);

        ImageView viewIcon = mView.findViewById(R.id.view_icon);
        viewIcon.setColorFilter(mTheme.actionIconColor);
        TextView viewLabel = mView.findViewById(R.id.view_label);
        viewLabel.setTextColor(mTheme.actionTextColor);

        ImageView shareIcon = mView.findViewById(R.id.share_icon);
        shareIcon.setColorFilter(mTheme.actionIconColor);
        TextView shareLabel = mView.findViewById(R.id.share_label);
        shareLabel.setTextColor(mTheme.actionTextColor);

        TextView textView = mView.findViewById(R.id.title);
        textView.setTextColor(mTheme.titleColor);
        textView = mView.findViewById(R.id.size);
        textView.setTextColor(mTheme.titleColor);

        Context context = getContext();
        if (Utils.isTablet(context) && getWindow() != null) {
            getWindow().setLayout(Math.round(Utils.convDp2Pix(context, 480)), ViewGroup.LayoutParams.MATCH_PARENT);
        }
    }

    public ExportFileCompletionDialog setFirstAction(@DrawableRes int iconRes,
            @StringRes int labelRes, final View.OnClickListener listener) {
        ImageView viewIcon = mView.findViewById(R.id.view_icon);
        TextView viewLabel = mView.findViewById(R.id.view_label);

        viewIcon.setVisibility(View.VISIBLE);
        viewLabel.setVisibility(View.VISIBLE);

        viewIcon.setImageResource(iconRes);
        viewLabel.setText(labelRes);

        viewIcon.setOnClickListener(listener);
        viewLabel.setOnClickListener(listener);

        return this;
    }

    public ExportFileCompletionDialog setSecondAction(@DrawableRes int iconRes,
            @StringRes int labelRes, final View.OnClickListener listener) {
        ImageView saveIcon = mView.findViewById(R.id.save_icon);
        TextView saveLabel = mView.findViewById(R.id.save_label);

        saveIcon.setVisibility(View.VISIBLE);
        saveLabel.setVisibility(View.VISIBLE);

        saveIcon.setImageResource(iconRes);
        saveLabel.setText(labelRes);

        saveIcon.setOnClickListener(listener);
        saveLabel.setOnClickListener(listener);

        return this;
    }

    public ExportFileCompletionDialog setThirdAction(@DrawableRes int iconRes,
            @StringRes int labelRes, final View.OnClickListener listener) {
        ImageView shareIcon = mView.findViewById(R.id.share_icon);
        TextView shareLabel = mView.findViewById(R.id.share_label);

        shareIcon.setVisibility(View.VISIBLE);
        shareLabel.setVisibility(View.VISIBLE);

        shareIcon.setImageResource(iconRes);
        shareLabel.setText(labelRes);

        shareIcon.setOnClickListener(listener);
        shareLabel.setOnClickListener(listener);

        return this;
    }

    public ExportFileCompletionDialog setTitle(@NonNull String title) {
        TextView titleView = mView.findViewById(R.id.title);
        titleView.setText(title);
        return this;
    }

    public ExportFileCompletionDialog setSize(@NonNull String size) {
        TextView sizeView = mView.findViewById(R.id.size);
        sizeView.setText(size);
        return this;
    }

    public ExportFileCompletionDialog setPreview(@NonNull String url) {
        final WebView previewView = mView.findViewById(R.id.preview);

        previewView.setInitialScale(1);
        previewView.getSettings().setLoadWithOverviewMode(true);
        previewView.getSettings().setUseWideViewPort(true);
        previewView.getSettings().setAllowFileAccess(true);
        previewView.setVerticalScrollBarEnabled(false);
        previewView.setHorizontalScrollBarEnabled(false);
        previewView.setScrollbarFadingEnabled(false);

        previewView.loadUrl(url);
        return this;
    }

    public static class Theme {

        @ColorInt
        public final int actionTextColor;
        @ColorInt
        public final int actionIconColor;
        @ColorInt
        public final int titleColor;

        public Theme(@ColorInt int actionTextColor, @ColorInt int actionIconColor, @ColorInt int titleColor) {
            this.actionTextColor = actionTextColor;
            this.actionIconColor = actionIconColor;
            this.titleColor = titleColor;
        }

        public static Theme fromContext(@NonNull Context context) {
            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.ExportFileCompletionDialogTheme, R.attr.pt_export_file_completion_dialog_style, R.style.PTExportFileCompletionDialogTheme);
            int textColor = a.getColor(R.styleable.ExportFileCompletionDialogTheme_actionTextColor, context.getResources().getColor(R.color.action_tint));
            int iconColor = a.getColor(R.styleable.ExportFileCompletionDialogTheme_actionIconColor, context.getResources().getColor(R.color.action_tint));
            int titleColor = a.getColor(R.styleable.ExportFileCompletionDialogTheme_titleColor, context.getResources().getColor(R.color.action_title));
            a.recycle();

            return new Theme(textColor, iconColor, titleColor);
        }
    }
}
