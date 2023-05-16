package com.pdftron.completereader;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;

import com.pdftron.demo.app.SimpleReaderActivity;
import com.pdftron.pdf.config.PDFViewCtrlConfig;
import com.pdftron.pdf.config.ToolManagerBuilder;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DiffActivity;
import com.pdftron.pdf.controls.DocumentActivity;
import com.pdftron.pdf.controls.ThumbnailsViewFragment;
import com.pdftron.pdf.dialog.ViewModePickerDialogFragment;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.Utils;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PdfViewCtrlSettingsManager.setFollowSystemDarkMode(this, false);

        View newUiView = findViewById(R.id.simpleReaderLayoutNew);

        Button simpleReaderButtonNew = newUiView.findViewById(R.id.simpleReaderButton);
        simpleReaderButtonNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSimpleReaderActivity(true);
            }
        });

        ImageView simpleReaderImageNew = newUiView.findViewById(R.id.simpleReaderImage);
        simpleReaderImageNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSimpleReaderActivity(true);
            }
        });

        Button diffButton = findViewById(R.id.diffButton);
        diffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDiffActivity();
            }
        });

        ImageView diffImage = findViewById(R.id.diffImage);
        diffImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDiffActivity();
            }
        });
    }

    private void openDiffActivity() {
        DiffActivity.open(this, R.raw.diff_doc_1, R.raw.diff_doc_2);
    }

    private void openSimpleReaderActivity(boolean newUi) {
        PdfViewCtrlSettingsManager.setFollowSystemDarkMode(this, false); // for better dark mode experience in demo
        ToolManagerBuilder tmBuilder = ToolManagerBuilder.from()
                .setUseDigitalSignature(false)
                .setAutoResizeFreeText(false);
        int cutoutMode = 0;
        if (Utils.isPie()) {
            cutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        ViewerConfig.Builder builder = new ViewerConfig.Builder();
        builder = builder
                .useCompactViewer(false)
                .fullscreenModeEnabled(true)
                .multiTabEnabled(true)
                .documentEditingEnabled(true)
                .longPressQuickMenuEnabled(true)
                .showPageNumberIndicator(true)
                .permanentPageNumberIndicator(false)
                .pageStackEnabled(true)
                .hideToolbars(new String[]{
                })
                .showDocumentSlider(true)
                .showTopToolbar(true)
                .showBottomToolbar(true)
                .permanentToolbars(false)
                .showThumbnailView(true)
                .showBookmarksView(true)
                .toolbarTitle("")
                .showSearchView(true)
                .showShareOption(true)
                .showDocumentSettingsOption(true)
                .showAnnotationToolbarOption(true)
                .showFormToolbarOption(true)
                .showFillAndSignToolbarOption(true)
                .showReflowOption(true)
                .showEditMenuOption(true)
                .showViewLayersToolbarOption(true)
                .showOpenFileOption(true)
                .showOpenUrlOption(true)
                .showEditPagesOption(true)
                .showPrintOption(true)
                .showSaveCopyOption(true)
                .hideThumbnailFilterModes(new ThumbnailsViewFragment.FilterModes[]{
                })
                .hideViewModeItems(new ViewModePickerDialogFragment.ViewModePickerItems[]{
                })
                .hideSaveCopyOptions(new int[]{
                })
                .showCloseTabOption(true)
                .showAnnotationsList(true)
                .showOutlineList(true)
                .showUserBookmarksList(true)
                .navigationListAsSheetOnLargeDevice(true)
                .rightToLeftModeEnabled(false)
                .showRightToLeftOption(false)
                .openUrlCachePath(this.getCacheDir().getAbsolutePath())
                .saveCopyExportPath(this.getCacheDir().getAbsolutePath())
                .thumbnailViewEditingEnabled(true)
                .userBookmarksListEditingEnabled(true)
                .userBookmarkCreationEnabled(true)
                .quickBookmarkCreation(false)
                .annotationsListEditingEnabled(true)
                .outlineListEditingEnabled(true)
                .useStandardLibrary(false)
                .showToolbarSwitcher(true)
                .imageInReflowEnabled(true)
                .pdfViewCtrlConfig(PDFViewCtrlConfig.getDefaultConfig(this))
                .toolManagerBuilder(tmBuilder);
        if (Utils.isPie()) {
            builder = builder.layoutInDisplayCutoutMode(cutoutMode);
        }
        ViewerConfig config = builder.build();
        Intent intent = DocumentActivity.IntentBuilder.fromActivityClass(this, SimpleReaderActivity.class)
                .usingConfig(config)
                .usingNewUi(newUi)
                .build();
        startActivity(intent);
    }
}
