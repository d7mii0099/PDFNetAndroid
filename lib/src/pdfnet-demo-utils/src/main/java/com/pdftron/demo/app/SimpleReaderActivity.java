package com.pdftron.demo.app;
//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.pdftron.demo.R;
import com.pdftron.demo.dialog.DialogOpenUrl;
import com.pdftron.demo.utils.AppUtils;
import com.pdftron.pdf.config.PDFNetConfig;
import com.pdftron.pdf.config.ViewerBuilder2;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.controls.DocumentActivity;
import com.pdftron.pdf.utils.Utils;

/**
 * SimpleReaderActivity is derived from {@link DocumentActivity}.
 * and is an all-in-one document reader and PDF editor. UI can be configured via {@link ViewerConfig class}.
 */
public class SimpleReaderActivity extends DocumentActivity {

    private static final String TAG = SimpleReaderActivity.class.getName();

    private MenuItem mMenuOpenFile;
    private MenuItem mMenuOpenUrl;

    private static int sOpenFileRequestCode = 20001;

    /**
     * Opens a built-in sample document with default configuration.
     *
     * @param packageContext the context
     */
    public static void open(Context packageContext) {
        open(packageContext, null);
    }

    /**
     * Opens a built-in sample document with custom configuration.
     *
     * @param packageContext the context
     * @param config         the {@link ViewerConfig}
     */
    public static void open(Context packageContext, @Nullable ViewerConfig config) {
        open(packageContext, config, false);
    }

    public static void open(Context packageContext, @Nullable ViewerConfig config, boolean newUi) {
        Intent intent = new Intent(packageContext, SimpleReaderActivity.class);
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_NEW_UI, newUi);
        packageContext.startActivity(intent);
    }

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
        openDocument(packageContext, fileUri, password, config, false);
    }

    public static void openDocument(Context packageContext, Uri fileUri, String password, @Nullable ViewerConfig config, boolean newUi) {
        Intent intent = new Intent(packageContext, SimpleReaderActivity.class);
        if (null != fileUri) {
            intent.putExtra(EXTRA_FILE_URI, fileUri);
        }
        if (null != password) {
            intent.putExtra(EXTRA_FILE_PASSWORD, password);
        }
        intent.putExtra(EXTRA_CONFIG, config);
        intent.putExtra(EXTRA_NEW_UI, newUi);
        packageContext.startActivity(intent);
    }

    /**
     * Opens a file from resource id with password and custom configuration.
     *
     * @param packageContext the context
     * @param resId          the resource id
     * @param password       the password
     * @param config         the configuration
     */
    public static void openDocument(Context packageContext, int resId, String password, @Nullable ViewerConfig config) {
        Intent intent = new Intent(packageContext, SimpleReaderActivity.class);
        intent.putExtra(EXTRA_FILE_RES_ID, resId);
        if (null != password) {
            intent.putExtra(EXTRA_FILE_PASSWORD, password);
        }
        intent.putExtra(EXTRA_CONFIG, config);
        packageContext.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        try {
            AppUtils.initializePDFNetApplication(getApplicationContext(),
                    PDFNetConfig.loadFromXML(getApplicationContext(), R.xml.pdfnet_config));
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        mSampleRes = R.raw.getting_started;
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == sOpenFileRequestCode && data != null && data.getData() != null) {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                Uri uri = data.getData();
                getContentResolver().takePersistableUriPermission(uri, takeFlags);
                // open file from URI
                if (mUseNewUi) {
                    mPdfViewCtrlTabHostFragment2.onOpenAddNewTab(
                            ViewerBuilder2.withUri(uri, "")
                                    .usingConfig(mViewerConfig)
                                    .createBundle(SimpleReaderActivity.this)
                    );
                } else {
                    mPdfViewCtrlTabHostFragment.onOpenAddNewTab(
                            ViewerBuilder2.withUri(uri, "")
                                    .usingConfig(mViewerConfig)
                                    .createBundle(SimpleReaderActivity.this)
                    );
                }
            }
        }
    }

    @Override
    protected int[] getToolbarMenuResArray() {
        if (mUseNewUi) {
            return new int[]{R.menu.fragment_viewer_addon, R.menu.fragment_viewer_new};
        } else {
            return new int[]{R.menu.fragment_viewer_addon, R.menu.fragment_viewer};
        }
    }

    /**
     * @hide
     */
    @Override
    public boolean onToolbarCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        mMenuOpenFile = menu.findItem(R.id.action_open_file);
        mMenuOpenUrl = menu.findItem(R.id.action_open_url);
        return false;
    }

    /**
     * @hide
     */
    @Override
    public boolean onToolbarOptionsItemSelected(MenuItem item) {
        final int id = item.getItemId();
        FragmentManager fragmentManager = getSupportFragmentManager();

        if (id == R.id.action_open_file) {
            if (Utils.isKitKat()) {
                Intent intent = Utils.createSystemPickerIntent();
                startActivityForResult(intent, sOpenFileRequestCode);
            }
        } else if (id == R.id.action_open_url) {
            DialogOpenUrl dialogOpenUrl = new DialogOpenUrl(this, new DialogOpenUrl.DialogOpenUrlListener() {
                @Override
                public void onSubmit(String url) {
                    if (mUseNewUi) {
                        mPdfViewCtrlTabHostFragment2.onOpenAddNewTab(
                                ViewerBuilder2.withUri(Uri.parse(url), "")
                                        .usingConfig(mViewerConfig)
                                        .createBundle(SimpleReaderActivity.this)
                        );
                    } else {
                        mPdfViewCtrlTabHostFragment.onOpenAddNewTab(
                                ViewerBuilder2.withUri(Uri.parse(url), "")
                                        .usingConfig(mViewerConfig)
                                        .createBundle(SimpleReaderActivity.this)
                        );
                    }
                }
            });
            dialogOpenUrl.show();
        }
        return false;
    }
}
