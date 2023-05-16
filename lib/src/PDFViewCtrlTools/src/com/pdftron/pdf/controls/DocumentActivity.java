package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.pdftron.pdf.config.ViewerBuilder;
import com.pdftron.pdf.config.ViewerBuilder2;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.dialog.SoundDialogFragment;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AppUtils;
import com.pdftron.pdf.utils.PdfViewCtrlTabsManager;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.ShortcutHelper;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentActivity is derived from <a target="_blank" href="https://developer.android.com/reference/androidx/appcompat/app/AppCompatActivity">AppCompatActivity</a>
 * and is an all-in-one document reader and PDF editor. UI can be configured via {@link ViewerConfig class}.
 */
public class DocumentActivity extends AppCompatActivity implements
        PdfViewCtrlTabHostFragment.TabHostListener,
        PdfViewCtrlTabHostFragment2.TabHostListener {

    public static class IntentBuilder {

        @NonNull
        private Intent mIntent;

        private IntentBuilder(@NonNull Intent intent) {
            mIntent = intent;
        }

        public static IntentBuilder fromActivityClass(@NonNull Context context, @NonNull Class<? extends DocumentActivity> activityClass) {
            return new IntentBuilder(new Intent(context, activityClass));
        }

        public IntentBuilder withUri(@NonNull Uri fileUri) {
            mIntent.putExtra(EXTRA_FILE_URI, fileUri);
            return this;
        }

        public IntentBuilder withUris(@NonNull ArrayList<Uri> fileUris) {
            mIntent.putParcelableArrayListExtra(EXTRA_FILE_URI_LIST, fileUris);
            return this;
        }

        public IntentBuilder withFileRes(@RawRes int resId) {
            mIntent.putExtra(EXTRA_FILE_RES_ID, resId);
            return this;
        }

        public IntentBuilder usingPassword(@Nullable String password) {
            mIntent.putExtra(EXTRA_FILE_PASSWORD, password != null ? password : "");
            return this;
        }

        public IntentBuilder usingCustomHeaders(@Nullable JSONObject customHeaders) {
            if (null != customHeaders) {
                mIntent.putExtra(EXTRA_CUSTOM_HEADERS, customHeaders.toString());
            }
            return this;
        }

        public IntentBuilder usingNewUi(boolean newUi) {
            mIntent.putExtra(EXTRA_NEW_UI, newUi);
            return this;
        }

        public IntentBuilder usingNavIcon(@DrawableRes int navIconRes) {
            mIntent.putExtra(EXTRA_NAV_ICON, navIconRes);
            return this;
        }

        public IntentBuilder usingConfig(@Nullable ViewerConfig config) {
            mIntent.putExtra(EXTRA_CONFIG, config);
            return this;
        }

        public IntentBuilder usingTheme(@StyleRes int theme) {
            mIntent.putExtra(EXTRA_UI_THEME, theme);
            return this;
        }

        public IntentBuilder usingFileExtension(@NonNull String extension) {
            mIntent.putExtra(EXTRA_FILE_EXTENSION, extension);
            return this;
        }

        public Intent build() {
            return mIntent;
        }
    }

    public static final String EXTRA_FILE_URI = "extra_file_uri";
    public static final String EXTRA_FILE_RES_ID = "extra_file_res_id";
    public static final String EXTRA_FILE_PASSWORD = "extra_file_password";
    public static final String EXTRA_CONFIG = "extra_config";
    public static final String EXTRA_CUSTOM_HEADERS = "extra_custom_headers";
    public static final String EXTRA_FILE_EXTENSION = "extra_file_extension";
    public static final String EXTRA_FILE_URI_LIST = "extra_file_uri_list";
    public static final String EXTRA_NAV_ICON = "extra_nav_icon";
    public static final String EXTRA_NEW_UI = "extra_new_ui";
    public static final String EXTRA_UI_THEME = "extra_ui_theme";

    @DrawableRes
    public static final int DEFAULT_NAV_ICON_ID = R.drawable.ic_arrow_back_white_24dp;

    private static final String SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG = "tabbed_host_fragment";

    protected PdfViewCtrlTabHostFragment mPdfViewCtrlTabHostFragment;
    protected PdfViewCtrlTabHostFragment2 mPdfViewCtrlTabHostFragment2;
    protected ViewerConfig mViewerConfig;

    protected boolean mUseNewUi = true;
    @StyleRes
    protected int mTheme;

    @DrawableRes
    protected int mNavigationIconId = DEFAULT_NAV_ICON_ID;
    protected JSONObject mCustomHeaders;

    protected ArrayList<Uri> mFileUris;
    private boolean mShouldOpenDocuments;

    protected int mSampleRes = 0;
    protected int[] mToolbarMenuResArray = new int[]{R.menu.fragment_viewer_new};

    /**
     * Opens a file from Uri with empty password and default configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     */
    public static void openDocument(Context packageContext, Uri fileUri) {
        openDocument(packageContext, fileUri, "");
    }

    /**
     * Opens a file from Uri with empty password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, Uri fileUri, @Nullable ViewerConfig config) {
        openDocument(packageContext, fileUri, "", config);
    }

    /**
     * Opens a file from resource id with empty password and default configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     */
    public static void openDocument(Context packageContext, int resId) {
        openDocument(packageContext, resId, "");
    }

    /**
     * Opens a file from resource id with empty password and custom configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, int resId, @Nullable ViewerConfig config) {
        openDocument(packageContext, resId, "", config);
    }

    /**
     * Opens a file from Uri with password and default configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password) {
        openDocument(packageContext, fileUri, password, null);
    }

    /**
     * Opens a file from resource id with password and default configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param password       the password
     */
    public static void openDocument(Context packageContext, int resId, String password) {
        openDocument(packageContext, resId, password, null);
    }

    /**
     * Opens a file from Uri with password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password, @Nullable ViewerConfig config) {
        openDocument(packageContext, fileUri, password, null, config);
    }

    /**
     * Opens a file from Uri with password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     * @param customHeaders  the custom headers for the request (only applies to HTTP/HTTPS URLs)
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password, @Nullable JSONObject customHeaders, @Nullable ViewerConfig config) {
        openDocument(packageContext, fileUri, password, customHeaders, config, DEFAULT_NAV_ICON_ID);
    }

    /**
     * Opens a file from Uri with password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     * @param customHeaders  the custom headers for the request (only applies to HTTP/HTTPS URLs)
     * @param config         the configuration
     * @param navIconId      the drawable resource id for the navigation button
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password,
            @Nullable JSONObject customHeaders, @Nullable ViewerConfig config, @DrawableRes int navIconId) {
        openDocument(packageContext, fileUri, password, customHeaders, config, navIconId, true);
    }

    /**
     * Opens a file from Uri with password and custom configuration.
     *
     * @param packageContext the context
     * @param fileUri        the file Uri
     * @param password       the password
     * @param customHeaders  the custom headers for the request (only applies to HTTP/HTTPS URLs)
     * @param config         the configuration
     * @param navIconId      the drawable resource id for the navigation button
     * @param newUi          true if use new UI, legacy UI will be used otherwise
     */
    public static void openDocument(Context packageContext, Uri fileUri, String password,
            @Nullable JSONObject customHeaders, @Nullable ViewerConfig config, @DrawableRes int navIconId, boolean newUi) {
        IntentBuilder builder = IntentBuilder.fromActivityClass(packageContext, DocumentActivity.class)
                .withUri(fileUri)
                .usingPassword(password)
                .usingConfig(config)
                .usingCustomHeaders(customHeaders)
                .usingNavIcon(navIconId)
                .usingNewUi(newUi);
        packageContext.startActivity(builder.build());
    }

    /**
     * Opens a list of files from Uri with custom configuration.
     *
     * @param packageContext the context
     * @param fileUris       the file Uri list
     */
    public static void openDocuments(Context packageContext, @NonNull ArrayList<Uri> fileUris, @Nullable ViewerConfig config) {
        IntentBuilder builder = IntentBuilder.fromActivityClass(packageContext, DocumentActivity.class)
                .withUris(fileUris)
                .usingConfig(config);
        packageContext.startActivity(builder.build());
    }

    /**
     * Opens a file from resource id with password and custom configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param password       the password
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, @RawRes int resId, String password, @Nullable ViewerConfig config) {
        IntentBuilder builder = IntentBuilder.fromActivityClass(packageContext, DocumentActivity.class)
                .withFileRes(resId)
                .usingPassword(password)
                .usingConfig(config);
        packageContext.startActivity(builder.build());
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            AppUtils.initializePDFNetApplication(getApplicationContext());
        } catch (Exception ex) {
            ex.printStackTrace();
            return;
        }

        if (Utils.applyDayNight(this)) {
            return;
        }

        if (getIntent() != null && getIntent().getExtras() != null) {
            mViewerConfig = getIntent().getExtras().getParcelable(EXTRA_CONFIG);
            mNavigationIconId = getIntent().getExtras().getInt(EXTRA_NAV_ICON, DEFAULT_NAV_ICON_ID);
            try {
                String headers = getIntent().getExtras().getString(EXTRA_CUSTOM_HEADERS);
                if (headers != null) {
                    mCustomHeaders = new JSONObject(headers);
                }
            } catch (JSONException ignored) {
            }
            mUseNewUi = getIntent().getExtras().getBoolean(EXTRA_NEW_UI, true);
            mToolbarMenuResArray = getToolbarMenuResArray();
            mTheme = getIntent().getExtras().getInt(EXTRA_UI_THEME,
                    mUseNewUi ? R.style.PDFTronAppTheme : R.style.CustomAppTheme);
        }

        if (savedInstanceState != null) {
            // fragments management
            Fragment savedFragment = getSupportFragmentManager().getFragment(savedInstanceState,
                    SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG);
            if (savedFragment instanceof PdfViewCtrlTabHostFragment2) {
                mPdfViewCtrlTabHostFragment2 = (PdfViewCtrlTabHostFragment2) savedFragment;
            } else if (savedFragment instanceof PdfViewCtrlTabHostFragment) {
                mPdfViewCtrlTabHostFragment = (PdfViewCtrlTabHostFragment) savedFragment;
            }
            if (mPdfViewCtrlTabHostFragment2 != null) {
                mPdfViewCtrlTabHostFragment2.addHostListener(this);
            } else if (mPdfViewCtrlTabHostFragment != null) {
                mPdfViewCtrlTabHostFragment.addHostListener(this);
            }

            // removing existing tab fragments since they will be created from scratch in host fragment
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            List<Fragment> fragments = getSupportFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof PdfViewCtrlTabFragment2 ||
                        fragment instanceof DialogFragment) {
                    ft.remove(fragment);
                }
            }
            ft.commitNow();
        }

        setContentView(R.layout.activity_document);

        ShortcutHelper.enable(true);

        if (null == mPdfViewCtrlTabHostFragment2 && null == mPdfViewCtrlTabHostFragment) {
            onDocumentSelected();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (null != mPdfViewCtrlTabHostFragment2) {
            mPdfViewCtrlTabHostFragment2.removeHostListener(this);
        }
        if (null != mPdfViewCtrlTabHostFragment) {
            mPdfViewCtrlTabHostFragment.removeHostListener(this);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        Log.v("LifeCycle", "Main.onSaveInstanceState");
        super.onSaveInstanceState(outState);

        FragmentManager fm = getSupportFragmentManager();
        List<Fragment> fragments = fm.getFragments();
        if (mPdfViewCtrlTabHostFragment2 != null && fragments.contains(mPdfViewCtrlTabHostFragment2)) {
            fm.putFragment(outState, SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG, mPdfViewCtrlTabHostFragment2);
        } else if (mPdfViewCtrlTabHostFragment != null && fragments.contains(mPdfViewCtrlTabHostFragment)) {
            fm.putFragment(outState, SAVE_INSTANCE_TABBED_HOST_FRAGMENT_TAG, mPdfViewCtrlTabHostFragment);
        }
    }

    @Override
    public void onBackPressed() {
        boolean handled = false;
        if (mPdfViewCtrlTabHostFragment2 != null) {
            handled = mPdfViewCtrlTabHostFragment2.handleBackPressed();
        } else if (mPdfViewCtrlTabHostFragment != null) {
            handled = mPdfViewCtrlTabHostFragment.handleBackPressed();
        }
        if (!handled) {
            super.onBackPressed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == RequestCode.RECORD_AUDIO) {
            Fragment fragment = getSupportFragmentManager().findFragmentByTag(SoundDialogFragment.TAG);
            if (fragment != null && fragment instanceof SoundDialogFragment) {
                fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    protected int[] getToolbarMenuResArray() {
        if (mUseNewUi) {
            return new int[]{R.menu.fragment_viewer_new};
        } else {
            return new int[]{R.menu.fragment_viewer};
        }
    }

    protected void onDocumentSelected(Uri fileUri) {
        onDocumentSelected(fileUri, "");
    }

    protected void onDocumentSelected(Uri fileUri, String password) {
        int theme = mUseNewUi ? R.style.PDFTronAppTheme : R.style.CustomAppTheme;
        if (mTheme != 0) {
            theme = mTheme;
        }
        String fileExtension = null;
        if (getIntent() != null && getIntent().getExtras() != null) {
            fileExtension = getIntent().getExtras().getString(EXTRA_FILE_EXTENSION);
        }
        if (mUseNewUi) {
            ViewerBuilder2 builder = ViewerBuilder2.withUri(fileUri, password).usingTheme(theme);
            if (fileExtension != null) {
                builder.usingFileExtension(fileExtension);
            }
            startTabHostFragment2(builder);
        } else {
            ViewerBuilder builder = ViewerBuilder.withUri(fileUri, password).usingTheme(theme);
            if (fileExtension != null) {
                builder.usingFileExtension(fileExtension);
            }
            startTabHostFragment(builder);
        }
    }

    protected void onDocumentsSelected(ArrayList<Uri> fileUris) {
        mFileUris = fileUris;
        if (mFileUris == null || mFileUris.isEmpty()) {
            return;
        }
        mShouldOpenDocuments = true;
        // open the first file first
        onDocumentSelected(mFileUris.get(0));
    }

    protected void onDocumentSelected() {
        if (isFinishing()) {
            return;
        }

        Uri fileUri = null;
        ArrayList<Uri> fileUris = null;
        String password = "";
        try {
            if (getIntent() != null && getIntent().getExtras() != null) {
                fileUri = getIntent().getExtras().getParcelable(EXTRA_FILE_URI);
                fileUris = getIntent().getExtras().getParcelableArrayList(EXTRA_FILE_URI_LIST);
                if (fileUris != null && fileUris.size() > 0) {
                    onDocumentsSelected(fileUris);
                    return;
                }
                int fileResId = getIntent().getExtras().getInt(EXTRA_FILE_RES_ID, 0);
                password = getIntent().getExtras().getString(EXTRA_FILE_PASSWORD);

                if (null == fileUri && fileResId != 0) {
                    File file = Utils.copyResourceToLocal(this, fileResId,
                            "untitled", ".pdf");
                    if (null != file && file.exists()) {
                        fileUri = Uri.fromFile(file);
                    }
                }
            }
            int tabCount = PdfViewCtrlTabsManager.getInstance().getDocuments(this).size();
            if (null == fileUri && tabCount == 0 && mSampleRes != 0) {
                File file = Utils.copyResourceToLocal(this, mSampleRes,
                        "getting_started", ".pdf");
                if (null != file && file.exists()) {
                    fileUri = Uri.fromFile(file);
                    password = "";
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        onDocumentSelected(fileUri, password);
    }

    protected void startTabHostFragment(@NonNull ViewerBuilder builder) {
        if (isFinishing()) {
            return;
        }

        builder.usingQuitAppMode(true)
                .usingNavIcon(mNavigationIconId)
                .usingCustomToolbar(mToolbarMenuResArray)
                .usingConfig(mViewerConfig)
                .usingCustomHeaders(mCustomHeaders);

        if (mPdfViewCtrlTabHostFragment != null) {
            mPdfViewCtrlTabHostFragment.onOpenAddNewTab(builder.createBundle(this));
            return;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mPdfViewCtrlTabHostFragment = builder.build(this);
        mPdfViewCtrlTabHostFragment.addHostListener(this);

        ft.replace(R.id.container, mPdfViewCtrlTabHostFragment, null);
        ft.commitAllowingStateLoss();
    }

    protected void startTabHostFragment2(@NonNull ViewerBuilder2 builder) {
        if (isFinishing()) {
            return;
        }

        builder.usingNavIcon(mNavigationIconId)
                .usingCustomToolbar(mToolbarMenuResArray)
                .usingConfig(mViewerConfig)
                .usingCustomHeaders(mCustomHeaders);

        if (mPdfViewCtrlTabHostFragment2 != null) {
            mPdfViewCtrlTabHostFragment2.onOpenAddNewTab(builder.createBundle(this));
            return;
        }

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        mPdfViewCtrlTabHostFragment2 = builder.build(this);
        mPdfViewCtrlTabHostFragment2.addHostListener(this);

        ft.replace(R.id.container, mPdfViewCtrlTabHostFragment2, null);
        ft.commitAllowingStateLoss();
    }

    @Override
    public void onTabHostShown() {
        if (mShouldOpenDocuments) {
            mShouldOpenDocuments = false;
            if (mFileUris != null) {
                for (int i = 0; i < mFileUris.size(); i++) {
                    if (i != 0) {
                        Uri fileUri = mFileUris.get(i);
                        onDocumentSelected(fileUri);
                    }
                }
            }
        }
    }

    @Override
    public void onTabHostHidden() {

    }

    @Override
    public void onLastTabClosed() {
        finish();
    }

    @Override
    public void onTabChanged(String tag) {

    }

    @Override
    public boolean onOpenDocError() {
        return false;
    }

    @Override
    public void onNavButtonPressed() {
        finish();
    }

    @Override
    public void onShowFileInFolder(String fileName, String filepath, int itemSource) {

    }

    @Override
    public boolean canShowFileInFolder() {
        return false;
    }

    @Override
    public boolean canShowFileCloseSnackbar() {
        return true;
    }

    @Override
    public boolean onToolbarCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        return false;
    }

    @Override
    public boolean onToolbarPrepareOptionsMenu(Menu menu) {
        return false;
    }

    @Override
    public boolean onToolbarOptionsItemSelected(MenuItem item) {
        return false;
    }

    @Override
    public void onStartSearchMode() {

    }

    @Override
    public void onExitSearchMode() {

    }

    @Override
    public boolean canRecreateActivity() {
        return true;
    }

    @Override
    public void onTabPaused(FileInfo fileInfo, boolean isDocModifiedAfterOpening) {

    }

    @Override
    public void onJumpToSdCardFolder() {

    }

    @Override
    public void onTabDocumentLoaded(String tag) {

    }
}
