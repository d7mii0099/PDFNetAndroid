package com.pdftron.pdf.controls;

import android.content.Context;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.pdftron.pdf.config.ViewerBuilder2;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

import org.json.JSONObject;

/**
 * @hide
 */
public class DocumentView2 extends FrameLayout implements
        PdfViewCtrlTabHostFragment2.TabHostListener {

    private static final String TAG = DocumentView.class.getSimpleName();

    public PdfViewCtrlTabHostFragment2 mPdfViewCtrlTabHostFragment;
    public FragmentManager mFragmentManager;

    public int mNavIconRes = R.drawable.ic_arrow_back_white_24dp;
    public boolean mShowNavIcon = true;
    public Uri mDocumentUri;
    public String mPassword = "";
    public ViewerConfig mViewerConfig;
    public ViewerBuilder2 mViewerBuilder;
    public JSONObject mCustomHeaders;
    public PdfViewCtrlTabHostBaseFragment.TabHostListener mTabHostListener;

    public DocumentView2(@NonNull Context context) {
        super(context);
    }

    public DocumentView2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public DocumentView2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setDocumentUri(Uri documentUri) {
        mDocumentUri = documentUri;
    }

    public void setPassword(String password) {
        mPassword = password;
    }

    public void setViewerConfig(ViewerConfig config) {
        mViewerConfig = config;
    }

    public void setCustomHeaders(JSONObject customHeaders) {
        mCustomHeaders = customHeaders;
    }

    public void setSupportFragmentManager(FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    public void setNavIconResName(String resName) {
        if (resName == null) {
            return;
        }
        int res = Utils.getResourceDrawable(getContext(), resName);
        if (res != 0) {
            mNavIconRes = res;
        }
    }

    public void setShowNavIcon(boolean showNavIcon) {
        mShowNavIcon = showNavIcon;
    }

    public void setTabHostListener(PdfViewCtrlTabHostBaseFragment.TabHostListener listener) {
        mTabHostListener = listener;
    }

    protected void buildViewer() {
        if (mDocumentUri == null) {
            return;
        }
        mViewerBuilder = ViewerBuilder2.withUri(mDocumentUri, mPassword)
                .usingConfig(mViewerConfig)
                .usingNavIcon(mShowNavIcon ? mNavIconRes : 0)
                .usingCustomHeaders(mCustomHeaders);
    }

    protected PdfViewCtrlTabHostFragment2 getViewer() {
        return mViewerBuilder.build(getContext());
    }

    protected void prepView() {
        // Create a viewer builder with the specified parameters
        buildViewer();
        if (mViewerBuilder == null) {
            return;
        }

        if (mPdfViewCtrlTabHostFragment != null) {
            mPdfViewCtrlTabHostFragment.onOpenAddNewTab(mViewerBuilder.createBundle(getContext()));
        } else {
            mPdfViewCtrlTabHostFragment = getViewer();
            mPdfViewCtrlTabHostFragment.addHostListener(this);

            if (mFragmentManager != null) {
                mFragmentManager.beginTransaction()
                        .add(mPdfViewCtrlTabHostFragment, String.valueOf(getId()))
                        .commitNow();

                View fragmentView = mPdfViewCtrlTabHostFragment.getView();
                if (fragmentView != null) {
                    fragmentView.clearFocus(); // work around issue where somehow new ui obtains focus
                    addView(fragmentView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
                }
            }
        }
    }

    protected void cleanup() {
        if (mFragmentManager != null) {
            Fragment fragment = mFragmentManager.findFragmentByTag(String.valueOf(getId()));
            if (fragment != null) {
                mFragmentManager.beginTransaction()
                        .remove(fragment)
                        .commitAllowingStateLoss();
            }
        }
        mPdfViewCtrlTabHostFragment = null;
        mFragmentManager = null;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        prepView();

        if (mPdfViewCtrlTabHostFragment != null) {
            mPdfViewCtrlTabHostFragment.addHostListener(this);
            if (mTabHostListener != null) {
                mPdfViewCtrlTabHostFragment.addHostListener(mTabHostListener);
            }
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mPdfViewCtrlTabHostFragment != null) {
            mPdfViewCtrlTabHostFragment.removeHostListener(this);
            if (mTabHostListener != null) {
                mPdfViewCtrlTabHostFragment.removeHostListener(mTabHostListener);
                mTabHostListener = null;
            }
        }

        cleanup();
    }

    @Override
    public void onTabHostShown() {

    }

    @Override
    public void onTabHostHidden() {

    }

    @Override
    public void onLastTabClosed() {

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
        return false;
    }

    @Override
    public void onTabPaused(FileInfo fileInfo, boolean b) {

    }

    @Override
    public void onJumpToSdCardFolder() {

    }

    @Override
    public void onTabDocumentLoaded(String tag) {

    }
}
