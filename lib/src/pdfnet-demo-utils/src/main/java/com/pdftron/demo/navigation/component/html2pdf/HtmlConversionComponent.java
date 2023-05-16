package com.pdftron.demo.navigation.component.html2pdf;

import android.content.Context;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.pdftron.demo.R;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.demo.dialog.ImportWebpageUrlSelectorDialogFragment;
import com.pdftron.demo.navigation.component.html2pdf.view.Html2PdfView;
import com.pdftron.pdf.model.ExternalFileInfo;
import com.pdftron.pdf.utils.RequestCode;

import java.io.File;

/**
 * Component used to convert HTML pages to PDF
 */
public abstract class HtmlConversionComponent {

    protected static final String DEFAULT_FILE_NAME = "untitled.pdf";
    @NonNull
    protected String mOutputFilename = DEFAULT_FILE_NAME;

    @Nullable
    protected HtmlConversionListener mListener;
    @Nullable
    protected HtmlConversionActionListener mActionListener;
    @NonNull
    protected Html2PdfView mHtml2PdfView;

    public HtmlConversionComponent(@NonNull Html2PdfView view,
            @Nullable HtmlConversionListener listener) {
        mListener = listener;
        mHtml2PdfView = view;
    }

    /**
     * Converts HTML, specified in the webpage selector dialog, and outputs to
     * a PDF stored at the location specified by the folder picker dialog
     *
     * @param activity used to attach fragment dialogs and get content resolver
     */
    public void handleWebpageToPDF(@NonNull final FragmentActivity activity) {
        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        ImportWebpageUrlSelectorDialogFragment importWebpageUrlSelectorDialogFragment = ImportWebpageUrlSelectorDialogFragment.newInstance();
        importWebpageUrlSelectorDialogFragment.setOnLinkSelectedListener(
                new ImportWebpageUrlSelectorDialogFragment.OnLinkSelectedListener() {
                    @Override
                    public void linkSelected(final String link) {
                        if (activity == null || activity.isFinishing()) {
                            return;
                        }
                        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(
                                RequestCode.SELECT_WEBPAGE_PDF_FOLDER, Environment.getExternalStorageDirectory());
                        dialogFragment.setLocalFolderListener(new FilePickerDialogFragment.LocalFolderListener() {
                            @Override
                            public void onLocalFolderSelected(int requestCode, Object customObject, File folder) {
                                fromUrl(activity, link, Uri.parse(folder.getAbsolutePath()));
                            }
                        });
                        dialogFragment.setExternalFolderListener(new FilePickerDialogFragment.ExternalFolderListener() {
                            @Override
                            public void onExternalFolderSelected(int requestCode, Object customObject, ExternalFileInfo folder) {
                                fromUrl(activity, link, folder.getRootUri());
                            }
                        });
                        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
                        if (fragmentManager != null) {
                            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
                        }
                    }
                });
        if (fragmentManager != null) {
            importWebpageUrlSelectorDialogFragment.show(fragmentManager, "ImportWebpageUrlSelectorDialogFragment");
        }
    }

    /**
     * Converts HTML, specified in the webpage selector dialog, and outputs to
     * a PDF stored at the specified folder
     *
     * @param activity     used to attach fragment dialogs and get content resolver
     * @param outputFolder output folder used for conversion
     */
    public void handleWebpageToPDF(@NonNull final FragmentActivity activity, final Uri outputFolder) {
        handleWebpageToPDF(activity, outputFolder, null);
    }

    public void handleWebpageToPDF(@NonNull final FragmentActivity activity, final Uri outputFolder,
            @Nullable DialogInterface.OnDismissListener dismissListener) {
        handleWebpageToPDF(activity, outputFolder, dismissListener, null);
    }

    public void handleWebpageToPDF(@NonNull final FragmentActivity activity, final Uri outputFolder,
            @Nullable DialogInterface.OnDismissListener dismissListener, @Nullable HtmlConversionActionListener actionListener) {
        handleWebpageToPDF(
                activity,
                ImportWebpageUrlSelectorDialogFragment.newInstance(),
                outputFolder,
                dismissListener,
                actionListener
        );
    }

    public void handleWebpageToPDF(
            @NonNull final FragmentActivity activity,
            @NonNull final ImportWebpageUrlSelectorDialogFragment customFragment,
            @Nullable DialogInterface.OnDismissListener dismissListener,
            @Nullable ImportWebpageUrlSelectorDialogFragment.OnLinkSelectedListener linkSelectedListener
    ) {
        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        customFragment.setOnLinkSelectedListener(linkSelectedListener);
        customFragment.setOnDismissListener(dismissListener);
        if (fragmentManager != null) {
            customFragment.show(fragmentManager, "ImportWebpageUrlSelectorDialogFragment");
        }
    }

    public void handleWebpageToPDF(
            @NonNull final FragmentActivity activity,
            @NonNull final ImportWebpageUrlSelectorDialogFragment customFragment,
            final Uri outputFolder,
            @Nullable DialogInterface.OnDismissListener dismissListener,
            @Nullable HtmlConversionActionListener actionListener
    ) {
        final FragmentManager fragmentManager = activity.getSupportFragmentManager();
        customFragment.setOnLinkSelectedListener(
                new ImportWebpageUrlSelectorDialogFragment.OnLinkSelectedListener() {
                    @Override
                    public void linkSelected(final String link) {
                        if (activity == null || activity.isFinishing()) {
                            return;
                        }
                        if (actionListener == null || actionListener.shouldStartConversion()) {
                            fromUrl(activity, link, outputFolder);
                        }
                    }
                });
        customFragment.setOnDismissListener(dismissListener);
        if (fragmentManager != null) {
            customFragment.show(fragmentManager, "ImportWebpageUrlSelectorDialogFragment");
        }
    }

    /**
     * Converts HTML from a specified HTML link and outputs to
     * a PDF stored at a specified folder.
     *
     * @param activity     used to attach fragment dialogs and get content resolver
     * @param link         HTML link to convert to PDF
     * @param outputFolder output folder used for conversion
     */
    public void handleWebpageToPdf(FragmentActivity activity, String link, File outputFolder) {
        fromUrl(activity, link, Uri.parse(outputFolder.getAbsolutePath()));
    }

    /**
     * Converts specified HTML link to PDF and save it to the specified Uri folder
     * with the output file name speicifed by {@link #mOutputFilename}
     *
     * @param context      used to inflate context resolver
     * @param link         HTML to convert
     * @param outputFolder output for the converted PDF
     */
    public void fromUrl(@NonNull final Context context, @NonNull final String link,
            @NonNull final Uri outputFolder) {
        fromUrl(context, link, outputFolder, mOutputFilename);
    }

    /**
     * @return the converted file's name
     */
    abstract public void setOutputFilename(@NonNull String filename);

    /**
     * Converts specified HTML link to PDF and save it to the specified Uri folder
     *
     * @param context        used to inflate context resolver
     * @param link           HTML to convert
     * @param outputFolder   output for the converted PDF
     * @param outputFileName converted file name
     */
    abstract protected void fromUrl(@NonNull final Context context, @NonNull final String link,
            @NonNull final Uri outputFolder, @NonNull String outputFileName);

    /**
     * Callback for HTML conversion progress
     */
    public interface HtmlConversionListener {

        void onConversionFinished(String path, boolean isLocal);

        void onConversionFailed(String errorMessage);
    }

    public interface HtmlConversionActionListener {
        // return whether to start conversion, true if start conversion, false otherwise
        boolean shouldStartConversion();
    }
}
