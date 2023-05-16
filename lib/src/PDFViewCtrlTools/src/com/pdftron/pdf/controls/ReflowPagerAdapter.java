//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.controls;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Convert;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Reflow;
import com.pdftron.pdf.annots.Markup;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.dialog.reflow.ReflowAnnotEditBottomSheetDialog;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.model.LineEndingStyle;
import com.pdftron.pdf.model.LineStyle;
import com.pdftron.pdf.model.RulerItem;
import com.pdftron.pdf.model.ShapeBorderStyle;
import com.pdftron.pdf.tools.DialogAnnotNote;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.CommonToast;
import com.pdftron.pdf.utils.ReflowWebView;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.widget.recyclerview.ItemClickHelper;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

// NOTE: if the html file is very large, it may take time for viewpager to show the html content,
//    and therefore, before showing the content it may first show the content from another page

/**
 * pager adapter for reflow
 */
@SuppressWarnings({"deprecation", "WeakerAccess"})
public class ReflowPagerAdapter extends PagerAdapter
        implements ReflowWebView.ReflowWebViewCallback {
    private static final String TAG = ReflowPagerAdapter.class.getName();
    private static boolean sDebug;

    private final static String LIGHT_MODE_LOADING_FILE = "file:///android_asset/loading_page_light.html";
    private final static String DARK_MODE_LOADING_FILE = "file:///android_asset/loading_page_dark.html";
    private final static String NIGHT_MODE_LOADING_FILE = "file:///android_asset/loading_page_night.html";

    private final static String JSON_KEY_TYPE = "type";
    private final static String JSON_KEY_ID = "uniqueID";
    private final static String JSON_KEY_COLOR = "color";
    private final static String JSON_KEY_OPACITY = "opacity";

    private static final float TAP_REGION_THRESHOLD = (1f / 7f);

    private enum ColorMode {
        DayMode,
        NightMode,
        CustomMode
    }

    private ColorMode mColorMode = ColorMode.DayMode;
    private int mBackgroundColorMode = 0XFFFFFF;
    private final PDFDoc mDoc;
    private int mPageCount;
    private final ViewPager mViewPager; // need it to get current page
    private SparseArray<String> mReflowFiles;
    private ConcurrentHashMap<Integer, Reflow> mReflowMap;
    private SparseArray<ReflowWebView> mViewHolders;
    private boolean mIsRtlMode;
    private boolean mDoTurnPageOnTap;
    private boolean mIsInternalLinkClicked = false;

    /*
     * scaling parameters
     * NOTE: if change SCALES, you may also need to change mDefaultScaleIndex
     */
    private static float[] SCALES = {0.05f, 0.1f, 0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 3.0f, 4.0f, 8.0f, 16.0f};
    private final int mDefaultScaleIndex = 5;
    private final static int mMaxIndex = SCALES.length - 1;
    public final static int TH_MIN_SCAlE = Math.round(SCALES[0] * 100);
    public final static int TH_MAX_SCAlE = Math.round(SCALES[mMaxIndex] * 100);
    private final static float TH_SCAlE_GESTURE = 1.25f;
    private int mScaleIndex = mDefaultScaleIndex;
    private final Context mContext;
    private float mScaleFactor;
    private float mLastScaleFactor;
    private float mThConsecutiveScales;
    private boolean mZoomInFlag;

    private final LongSparseArray<Integer> mObjNumMap = new LongSparseArray<>();
    private int mLastProcessedObjNum = 0;
    private ReflowControl.OnPostProcessColorListener mOnPostProcessColorListener;

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    private boolean mPaused;

    private boolean mReflowWithImage = true;
    private boolean mIsHideBackgroundImages = false;
    private boolean mIsHideImagesUnderText = false;
    private boolean mIsDoNotReflowTextOverImages = false;
    private boolean mIsHideImagesUnderInvisibleText = false;

    // edit annots
    private boolean mEditingEnabled = true;
    private volatile String mSelectedAnnot;
    private volatile boolean mJustDeselectedAnnot;
    private int mAnnotNoteButtonPressed;
    @NonNull
    private final HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties = new HashMap<>();
    private ReflowAnnotEditBottomSheetDialog mReflowAnnotEditDialog;

    private static class ClickHandler extends Handler {
        private static final int GO_TO_NEXT_PAGE = 1;
        private static final int GO_TO_PREVIOUS_PAGE = 2;
        private static final int CLICK_ON_URL = 3;

        private final WeakReference<ReflowPagerAdapter> mCtrl;

        ClickHandler(ReflowPagerAdapter ctrl) {
            mCtrl = new WeakReference<>(ctrl);
        }

        @Override
        public void handleMessage(Message msg) {
            ReflowPagerAdapter ctrl = mCtrl.get();
            if (ctrl != null && ctrl.mViewPager != null) {
                ViewPager viewPager = ctrl.mViewPager;
                int position = viewPager.getCurrentItem();
                switch (msg.what) {
                    case GO_TO_NEXT_PAGE:
                        viewPager.setCurrentItem(position + 1);
                        break;
                    case GO_TO_PREVIOUS_PAGE:
                        viewPager.setCurrentItem(position - 1);
                        break;
                }
            }
            if (msg.what == CLICK_ON_URL) {
                removeMessages(GO_TO_NEXT_PAGE);
                removeMessages(GO_TO_PREVIOUS_PAGE);
            }
        }
    }

    private final ClickHandler mClickHandler = new ClickHandler(this);

    /**
     * Callback interfaces for tap
     */
    public interface ReflowPagerAdapterCallback {

        /**
         * Called with single tab up event
         *
         * @param event The motion event
         */
        void onReflowPagerSingleTapUp(WebView webView, MotionEvent event);

        /**
         * Called with long press event
         *
         * @param event The motion event
         */
        void onReflowPagerLongPress(WebView webView, MotionEvent event);
    }

    private ReflowPagerAdapterCallback mCallback;

    @Nullable
    private ReflowControl.ReflowUrlLoadedListener mUrlLoadedListener;

    @Nullable
    private ToolManager mToolManager;

    /**
     * Sets the listener to ReflowPagerAdapterCallback
     *
     * @param listener The listener
     */
    public void setListener(ReflowPagerAdapterCallback listener) {
        mCallback = listener;
    }

    /**
     * Sets a ReflowUrlLoadedListener for ReflowPagerAdapterCallback
     *
     * @param listener The listener
     */
    public void setReflowUrlLoadedListener(@Nullable ReflowControl.ReflowUrlLoadedListener listener) {
        mUrlLoadedListener = listener;
    }

    public void setToolManager(@Nullable ToolManager toolManager) {
        mToolManager = toolManager;
    }

    /**
     * Class constructor
     *
     * @param viewPager The view pager
     * @param context   The context
     * @param doc       The PDF doc
     */
    public ReflowPagerAdapter(ViewPager viewPager, Context context, PDFDoc doc) {
        mViewPager = viewPager;
        mDoc = doc;
        mContext = context;
        mPageCount = 0;
        boolean shouldUnlockRead = false;

        try {
            mDoc.lockRead();
            shouldUnlockRead = true;
            mPageCount = mDoc.getPageCount();
            mReflowFiles = new SparseArray<>(mPageCount);
            mReflowMap = new ConcurrentHashMap<>(mPageCount);
            mViewHolders = new SparseArray<>(mPageCount);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(mDoc);
            }
        }

        // NOTE: make sure that the off-screen page limit is set to one
        viewPager.setOffscreenPageLimit(1);
    }

    public void setAnnotStyleProperties(@NonNull HashMap<Integer, AnnotStyleProperty> annotStyleProperties) {
        mAnnotStyleProperties.clear();
        mAnnotStyleProperties.putAll(annotStyleProperties);
    }

    public void setEditingEnabled(boolean editingEnabled) {
        mEditingEnabled = editingEnabled;
    }

    public boolean isEditingEnabled() {
        return mEditingEnabled && Utils.isNougat();
    }

    /**
     * Sets whether to show images in reflow mode, default to true.
     */
    public void setImageInReflowEnabled(boolean imageInReflowEnabled) {
        mReflowWithImage = imageInReflowEnabled;
    }

    /**
     * Gets value to show images in reflow mode, default to true.
     */
    public boolean getImageInReflowEnabled() {
        return mReflowWithImage;
    }

    /**
     * Sets whether to show background images in reflow mode, default to false.
     */
    public void setHideBackgroundImages(boolean hideBackgroundImages) {
        mIsHideBackgroundImages = hideBackgroundImages;
    }

    /**
     * Gets value to show background images in reflow mode, default to false.
     */
    public boolean getIsHideBackgroundImages() {
        return mIsHideBackgroundImages;
    }

    /**
     * Sets whether to show images under text in reflow mode, default to false.
     */
    public void setHideImagesUnderText(boolean hideImagesUnderText) {
        mIsHideImagesUnderText = hideImagesUnderText;
    }

    /**
     * Gets value to show images under text in reflow mode, default to false.
     */
    public boolean getIsHideImagesUnderText() {
        return mIsHideImagesUnderText;
    }

    /**
     * Sets whether to show text over images in reflow mode, default to false.
     */
    public void setDoNotReflowTextOverImages(boolean doNotReflowTextOverImages) {
        mIsDoNotReflowTextOverImages = doNotReflowTextOverImages;
    }

    /**
     * Gets value to show text over images in reflow mode, default to false.
     */
    public boolean getIsDoNotReflowTextOverImages() {
        return mIsDoNotReflowTextOverImages;
    }

    /**
     * Sets whether to show images under invisible text in reflow mode, default to false.
     */
    public void setHideImagesUnderInvisibleText(boolean hideImagesUnderInvisibleText) {
        mIsHideImagesUnderInvisibleText = hideImagesUnderInvisibleText;
    }

    /**
     * Gets value to show images under invisible text in reflow mode, default to false.
     */
    public boolean getIsHideImagesUnderInvisibleText() {
        return mIsHideImagesUnderInvisibleText;
    }

    /**
     * reset adapter to reload all data
     */
    private void resetAdapter() {
        int curPosition = mViewPager.getCurrentItem();
        mViewPager.setAdapter(this);
        mViewPager.setCurrentItem(curPosition, false);
    }

    /**
     * Should be called when pages of the document have been edited
     */
    public void onPagesModified() {
        if (sDebug) Log.d(TAG, "pages were modified.");
        mPageCount = 0;
        boolean shouldUnlockRead = false;
        try {
            mDoc.lockRead();
            shouldUnlockRead = true;
            mPageCount = mDoc.getPageCount();
            mReflowFiles = new SparseArray<>(mPageCount);
            mReflowMap = new ConcurrentHashMap<>(mPageCount);
            mViewHolders = new SparseArray<>(mPageCount);
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlockRead) {
                Utils.unlockReadQuietly(mDoc);
            }
        }
        mObjNumMap.clear();
        mLastProcessedObjNum = 0;
        resetAdapter();
    }

    /**
     * Cleans up resources.
     */
    public void cleanup() {
        if (mPaused) {
            return;
        }
        if (sDebug) Log.d(TAG, "Cleanup");
        mReflowFiles.clear();
        for (Reflow reflow : mReflowMap.values()) {
            try {
                reflow.destroy();
            } catch (Exception ignored) {

            }
        }
        mReflowMap.clear();
        mPaused = true;
        mClickHandler.removeCallbacksAndMessages(null);
        mDisposables.clear();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private ReflowWebView getReflowWebView(final int pagePosition) {
        ReflowWebView webView = new ReflowWebView(mContext);
        if (mViewPager instanceof ReflowControl) {
            webView.setOrientation(((ReflowControl) mViewPager).getOrientation());
        }
        webView.clearCache(true); // reset reading css file
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setAllowFileAccess(true);
        webView.setWillNotCacheDrawing(false);
        webView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        webView.setListener(this);

        if (isEditingEnabled()) {
            webView.setTextSelectionCallback(new ReflowWebView.TextSelectionCallback() {
                @Override
                public boolean onMenuItemClick(WebView webView, MenuItem item) {
                    int annotType = Annot.e_Unknown;
                    if (item.getItemId() == R.id.qm_highlight) {
                        annotType = Annot.e_Highlight;
                    } else if (item.getItemId() == R.id.qm_underline) {
                        annotType = Annot.e_Underline;
                    } else if (item.getItemId() == R.id.qm_strikeout) {
                        annotType = Annot.e_StrikeOut;
                    } else if (item.getItemId() == R.id.qm_squiggly) {
                        annotType = Annot.e_Squiggly;
                    }
                    if (annotType != Annot.e_Unknown) {
                        String styleJson = initInitialStyle(annotType);
                        webView.evaluateJavascript("javascript:ReflowJS.addAnnot(" + annotType + ", " + pagePosition + ", " + styleJson + ");", null);

                        AnalyticsHandlerAdapter.getInstance()
                                .sendEvent(AnalyticsHandlerAdapter.EVENT_QUICK_MENU_REFLOW_ACTION,
                                        AnalyticsParam.quickMenuParam(AnalyticsHandlerAdapter.QUICK_MENU_TYPE_TEXT_SELECT,
                                                AnalyticsHandlerAdapter.getInstance().getQuickMenuAction(item.getItemId(), null)));
                        return true;
                    }
                    return false;
                }
            });
        }

        webView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                WebView.HitTestResult result = ((WebView) v).getHitTestResult();
                if (result != null) {
                    int type = result.getType();
                    if (WebView.HitTestResult.IMAGE_TYPE == type) {
                        if (result.getExtra() != null && v.getContext() instanceof Activity) {
                            Uri uri = Uri.parse(result.getExtra());
                            if (uri.toString().startsWith("data:")) {
                                String url = uri.toString();
                                try {
                                    String fileType = url.substring(url.indexOf("/") + 1, url.indexOf(";"));
                                    String base64EncodedString = url.substring(url.indexOf(",") + 1);
                                    byte[] decodedBytes = Base64.decode(base64EncodedString, Base64.DEFAULT);
                                    File tmpFile = File.createTempFile("tmp", "." + fileType);
                                    OutputStream os = new FileOutputStream(tmpFile);
                                    os.write(decodedBytes);
                                    os.close();
                                    Uri imageUri = Utils.getUriForFile(v.getContext(), tmpFile);
                                    if (imageUri != null) {
                                        Utils.shareGenericFile((Activity) v.getContext(), imageUri);
                                    }
                                } catch (Exception ignored) {
                                }
                            } else if (uri.getPath() != null) {
                                File imageFile = new File(uri.getPath());
                                if (imageFile.isFile() && imageFile.exists()) {
                                    Uri imageUri = Utils.getUriForFile(v.getContext(), imageFile);
                                    if (imageUri != null) {
                                        Utils.shareGenericFile((Activity) v.getContext(), imageUri);
                                    }
                                }
                            }
                        }
                    }
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient()); // enable the use of methods like alert in javascript
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                if (isEditingEnabled()) {
                    Reflow reflow = mReflowMap.get(pagePosition);
                    if (reflow != null) {
                        try {
                            String annots = reflow.getAnnot("");
                            webView.evaluateJavascript("javascript:ReflowJS.loadAnnotations(" + annots + ");", null);
                        } catch (Exception ex) {
                            AnalyticsHandlerAdapter.getInstance().sendException(ex);
                        }
                    }
                }
            }

            // now all links the user clicks load in your WebView
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.e(TAG, description + " url: " + failingUrl);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                super.onReceivedSslError(view, handler, error);
                Log.e(TAG, error.toString());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (mClickHandler != null) {
                    mClickHandler.sendEmptyMessage(ClickHandler.CLICK_ON_URL);
                }
                if (url.startsWith("file:///") && url.endsWith(".html")) {
                    int slashPos = url.lastIndexOf('/');
                    boolean shouldUnlockRead = false;
                    try {
                        long curObjNum = Long.parseLong(url.substring(slashPos + 1, url.length() - 5));
                        int curPageNum = 0;
                        if (mObjNumMap.get(curObjNum) != null) {
                            curPageNum = mObjNumMap.get(curObjNum);
                        } else {
                            try {
                                mDoc.lockRead();
                                shouldUnlockRead = true;
                                for (int i = mLastProcessedObjNum + 1; i <= mPageCount; i++) {
                                    Page page = mDoc.getPage(i);
                                    long objNum = page.getSDFObj().getObjNum();
                                    ++mLastProcessedObjNum;
                                    mObjNumMap.put(objNum, i);
                                    if (objNum == curObjNum) {
                                        curPageNum = i;
                                        break;
                                    }
                                }
                            } catch (Exception ignored) {

                            } finally {
                                if (shouldUnlockRead) {
                                    Utils.unlockReadQuietly(mDoc);
                                }
                            }
                        }
                        if (curPageNum != 0) {
                            int newPage = mIsRtlMode ? mPageCount - curPageNum : curPageNum - 1;
                            boolean handled = false;
                            if (mUrlLoadedListener != null) {
                                String processedUrl = "page:" + newPage;
                                handled = mUrlLoadedListener.onReflowInternalUrlLoaded(view, processedUrl);
                            }
                            if (!handled) {
                                mIsInternalLinkClicked = true;
                                mViewPager.setCurrentItem(newPage);
                            }
                        }
                    } catch (NumberFormatException e) {
                        return true;
                    }
                } else {
                    if (url.startsWith("mailto:") || android.util.Patterns.EMAIL_ADDRESS.matcher(url).matches()) {
                        if (url.startsWith("mailto:")) {
                            url = url.substring(7);
                        }
                        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", url, null));
                        mContext.startActivity(Intent.createChooser(intent, mContext.getResources().getString(R.string.tools_misc_sendemail)));
                    } else {

                        boolean handled = false;
                        if (mUrlLoadedListener != null) {
                            handled = mUrlLoadedListener.onReflowExternalUrlLoaded(view, url);
                        }

                        if (!handled) {
                            // ACTION_VIEW needs the address to have http or https
                            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                                url = "http://" + url;
                            }
                            if (sDebug) Log.d(TAG, url);
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                            mContext.startActivity(Intent.createChooser(intent, mContext.getResources().getString(R.string.tools_misc_openwith)));
                        }
                    }
                }
                return true;
            }
        });

        if (isEditingEnabled()) {
            webView.removeJavascriptInterface(ReflowAppInterface.sName);
            webView.addJavascriptInterface(new ReflowAppInterface(webView.getContext(), mReflowMap, new ReflowAppCallback() {

                @Override
                public void onAnnotClicked(String id, String annotData) {
                    mSelectedAnnot = id;
                    // post on UI
                    webView.post(new Runnable() {
                        @Override
                        public void run() {
                            mReflowAnnotEditDialog = new ReflowAnnotEditBottomSheetDialog(webView.getContext());
                            mReflowAnnotEditDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    deselectAnnot(webView);
                                }
                            });
                            mReflowAnnotEditDialog.setOnItemClickListener(new ItemClickHelper.OnItemClickListener() {
                                @Override
                                public void onItemClick(RecyclerView recyclerView, View view, int position, long id) {
                                    if (position == ReflowAnnotEditBottomSheetDialog.ITEM_STYLE) {
                                        showStylePicker(webView, pagePosition, annotData);
                                    } else if (position == ReflowAnnotEditBottomSheetDialog.ITEM_NOTE) {
                                        showAnnotNotePopup(webView.getContext(), annotData);
                                    } else if (position == ReflowAnnotEditBottomSheetDialog.ITEM_DELETE) {
                                        deleteAnnot(webView, pagePosition);
                                        mReflowAnnotEditDialog.dismiss();
                                    }
                                }
                            });
                            mReflowAnnotEditDialog.show();
                        }
                    });
                }

                @Override
                public void onCleanSelectedAnnot() {
                    if (mSelectedAnnot != null) {
                        mJustDeselectedAnnot = true;
                    }
                    mSelectedAnnot = null;
                }
            }), ReflowAppInterface.sName);
        }

        switch (mColorMode) {
            case DayMode:
                webView.setBackgroundColor(Color.WHITE);
                break;
            case NightMode:
                webView.setBackgroundColor(Color.BLACK);
                break;
            case CustomMode:
                webView.setBackgroundColor(mBackgroundColorMode);
                break;
        }
        webView.loadUrl("about:blank");

        return webView;
    }

    @Nullable
    private String initInitialStyle(int annotType) {
        if (mContext != null) {
            try {
                SharedPreferences settings = Tool.getToolPreferences(mContext);
                int color = settings.getInt(ToolStyleConfig.getInstance().getColorKey(annotType, ""), ToolStyleConfig.getInstance().getDefaultColor(mContext, annotType));
                float opacity = settings.getFloat(ToolStyleConfig.getInstance().getOpacityKey(annotType, ""), ToolStyleConfig.getInstance().getDefaultOpacity(mContext, annotType));
                JSONObject style = new JSONObject();
                style.put(JSON_KEY_COLOR, Utils.getColorHexString(color));
                style.put(JSON_KEY_OPACITY, opacity);
                if (mToolManager != null) {
                    style.put(JSON_KEY_ID, mToolManager.generateKey());
                }
                return style.toString();
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void showAnnotNotePopup(Context context, final String annotData) {
        try {
            JSONObject jsonObject = new JSONObject(annotData);
            String id = jsonObject.getString(JSON_KEY_ID);
            final int pageNum = getCurrentPage();
            final Annot annot = ViewerUtils.getAnnotById(mDoc, id, pageNum);
            if (annot != null) {
                String contents = AnnotUtils.getAnnotContents(mDoc, annot);

                // adding/editing a note to an annotation
                DialogAnnotNote dialogAnnotNote = new DialogAnnotNote(context, contents,
                        AnnotUtils.hasPermission(mDoc, mToolManager, annot, Tool.ANNOT_PERMISSION_MENU));
                dialogAnnotNote.setNegativeButtonRes(R.string.cancel);
                dialogAnnotNote.setAnnotNoteListener(new DialogAnnotNote.DialogAnnotNoteListener() {
                    @Override
                    public void onAnnotButtonPressed(int button) {
                        mAnnotNoteButtonPressed = button;
                    }
                });
                dialogAnnotNote.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialogInterface) {
                        if (mAnnotNoteButtonPressed == DialogInterface.BUTTON_POSITIVE) {
                            boolean shouldUnlock = false;
                            try {
                                mDoc.lock();
                                shouldUnlock = true;

                                raiseAnnotationPreModifyEvent(annot, pageNum);
                                Markup markup = new Markup(annot);
                                AnnotUtils.setAnnotContents(mDoc, markup, dialogAnnotNote.getNote());
                                raiseAnnotationModifiedEvent(annot, pageNum);
                            } catch (Exception ignored) {
                            } finally {
                                if (shouldUnlock) {
                                    Utils.unlockQuietly(mDoc);
                                }
                            }
                        }
                    }
                });
                dialogAnnotNote.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialogInterface) {

                    }
                });

                dialogAnnotNote.show();
            }
        } catch (JSONException ex) {
            AnalyticsHandlerAdapter.getInstance().sendException(ex);
        }
    }

    private void deleteAnnot(final WebView webView, final int pagePosition) {
        if (Utils.isNougat()) {
            webView.evaluateJavascript("javascript:ReflowJS.removeSelectedAnnot(" + pagePosition + ");", null);
        }
    }

    private void showStylePicker(final WebView webView, final int pagePosition, final String annotData) {
        if (mContext instanceof FragmentActivity) {
            try {
                JSONObject jsonObject = new JSONObject(annotData);
                int annotType = jsonObject.optInt(JSON_KEY_TYPE);
                String annotColor = jsonObject.optString(JSON_KEY_COLOR);
                double opacity = jsonObject.optDouble(JSON_KEY_OPACITY);

                if (annotType == Annot.e_Highlight ||
                        annotType == Annot.e_Squiggly ||
                        annotType == Annot.e_StrikeOut ||
                        annotType == Annot.e_Underline) {
                    final AnnotStyle annotStyle = new AnnotStyle();
                    annotStyle.setAnnotType(annotType);
                    annotStyle.setStrokeColor(Color.parseColor(annotColor));
                    annotStyle.setThickness(1f); // hard-coded as core does not support thickness at the moment
                    annotStyle.setOpacity((float) opacity);
                    // remove thickness as it's not supported at the moment
                    removeThickness(Annot.e_Underline);
                    removeThickness(Annot.e_StrikeOut);
                    removeThickness(Annot.e_Squiggly);

                    AnnotStyleDialogFragment annotStyleDialog = new AnnotStyleDialogFragment.Builder(annotStyle).setShowPreview(false).build();
                    annotStyleDialog.setAnnotStyleProperties(mAnnotStyleProperties);
                    annotStyleDialog.show(((FragmentActivity) mContext).getSupportFragmentManager());
                    annotStyleDialog.setOnAnnotStyleChangeListener(new AnnotStyle.OnAnnotStyleChangeListener() {
                        @Override
                        public void onChangeAnnotThickness(float thickness, boolean done) {

                        }

                        @Override
                        public void onChangeAnnotTextSize(float textSize, boolean done) {

                        }

                        @Override
                        public void onChangeAnnotTextColor(int textColor) {

                        }

                        @Override
                        public void onChangeAnnotOpacity(float opacity, boolean done) {
                            annotStyle.setOpacity(opacity);
                            int color = annotStyle.getColor();
                            String colorStr = Utils.getColorHexString(color);
                            editAnnot(webView, pagePosition, colorStr, opacity);
                        }

                        @Override
                        public void onChangeAnnotStrokeColor(int color) {
                            annotStyle.setStrokeColor(color);
                            String colorStr = Utils.getColorHexString(color);
                            float opacity = annotStyle.getOpacity();
                            editAnnot(webView, pagePosition, colorStr, opacity);
                        }

                        @Override
                        public void onChangeAnnotFillColor(int color) {

                        }

                        @Override
                        public void onChangeAnnotIcon(String icon) {

                        }

                        @Override
                        public void onChangeAnnotFont(FontResource font) {

                        }

                        @Override
                        public void onChangeRulerProperty(RulerItem rulerItem) {

                        }

                        @Override
                        public void onChangeOverlayText(String overlayText) {

                        }

                        @Override
                        public void onChangeSnapping(boolean snap) {

                        }

                        @Override
                        public void onChangeRichContentEnabled(boolean enabled) {

                        }

                        @Override
                        public void onChangeDateFormat(String dateFormat) {

                        }

                        @Override
                        public void onChangeAnnotBorderStyle(ShapeBorderStyle borderStyle) {

                        }

                        @Override
                        public void onChangeAnnotLineStyle(LineStyle lineStyle) {

                        }

                        @Override
                        public void onChangeAnnotLineStartStyle(LineEndingStyle lineStartStyle) {

                        }

                        @Override
                        public void onChangeAnnotLineEndStyle(LineEndingStyle lineEndStyle) {

                        }

                        @Override
                        public void onChangeTextAlignment(int horizontalAlignment, int verticalAlignment) {

                        }
                    });
                    annotStyleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            AnnotStyle resultStyle = annotStyleDialog.getAnnotStyle();
                            int color = resultStyle.getColor();
                            float opacity = resultStyle.getOpacity();

                            // save
                            SharedPreferences settings = Tool.getToolPreferences(webView.getContext());
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putInt(ToolStyleConfig.getInstance().getColorKey(annotType, ""), color);
                            editor.putFloat(ToolStyleConfig.getInstance().getOpacityKey(annotType, ""), opacity);
                            editor.apply();

                            if (mReflowAnnotEditDialog != null) {
                                mReflowAnnotEditDialog.dismiss();
                            }
                        }
                    });
                }
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
    }

    private void editAnnot(final WebView webView, final int pagePosition, String color, float opacity) {
        if (Utils.isNougat()) {
            try {
                JSONObject style = new JSONObject();
                style.put(JSON_KEY_COLOR, color);
                style.put(JSON_KEY_OPACITY, opacity);
                String inputParam = style.toString() + ", " + pagePosition;
                webView.evaluateJavascript("javascript:ReflowJS.setSelectedAnnotStyle(" + inputParam + ");", null);
            } catch (Exception ex) {
                AnalyticsHandlerAdapter.getInstance().sendException(ex);
            }
        }
    }

    private void deselectAnnot(final WebView webView) {
        if (Utils.isNougat()) {
            webView.evaluateJavascript("javascript:ReflowJS.deselectAnnot();", null);
        }
    }

    private void removeThickness(int annotType) {
        AnnotStyleProperty styleProperty = mAnnotStyleProperties.get(annotType);
        if (styleProperty == null) {
            styleProperty = new AnnotStyleProperty(annotType);
        }
        styleProperty.setCanShowThickness(false);
        mAnnotStyleProperties.put(annotType, styleProperty);
    }

    /**
     * Sets the right-to-left direction of the document.
     * Used for supporting right-to-left languages.
     *
     * @param isRtlMode True if right-to-left mode is enabled
     */
    public void setRightToLeftDirection(boolean isRtlMode) {
        mIsRtlMode = isRtlMode;
        if (sDebug) Log.d("Reflow Right to Left", mIsRtlMode ? "True" : "False");
    }

    /**
     * @return True if the direction is right-to-left
     */
    public boolean isRightToLeftDirection() {
        return mIsRtlMode;
    }

    /**
     * Sets colors in the day mode (default).
     */
    public void setDayMode() {
        mColorMode = ColorMode.DayMode;
        clearCacheAndReset();
    }

    /**
     * Sets colors in the night mode.
     */
    public void setNightMode() {
        mColorMode = ColorMode.NightMode;
        clearCacheAndReset();
    }

    /**
     * Sets custom color.
     */
    public void setCustomColorMode(int backgroundColorMode) {
        mBackgroundColorMode = backgroundColorMode;
        mColorMode = ColorMode.CustomMode;
        clearCacheAndReset();
    }

    /**
     * @return True if reflow is in day mode
     */
    boolean isDayMode() {
        return mColorMode == ColorMode.DayMode;
    }

    /**
     * @return True if reflow is in night mode
     */
    boolean isNightMode() {
        return mColorMode == ColorMode.NightMode;
    }

    /**
     * @return True if reflow is in custom color mode
     */
    boolean isCustomColorMode() {
        return mColorMode == ColorMode.CustomMode;
    }

    protected void clearCacheAndReset() {
        // note that reflowable files can be in custom color mode, night mode or in day mode.
        // An easy way to handle such situation is to clear everything in our cache (mReflowFiles)
        // and request again for reflowable files from the core.
        // It is not worst idea since core already cached the computed reflowable files so it
        // doesn't affect much on the performance, though it is possible to handle it here with some
        // effort. Right now we choose the easy solution.
        mReflowFiles.clear();
        for (Reflow reflow : mReflowMap.values()) {
            try {
                reflow.destroy();
            } catch (Exception ignored) {

            }
        }
        mReflowMap.clear();

        resetAdapter();
    }

    /**
     * Enables turn page on tap.
     *
     * @param enabled True if enabled
     */
    public void enableTurnPageOnTap(boolean enabled) {
        mDoTurnPageOnTap = enabled;
    }

    /**
     * Sets the text size using percentage.
     *
     * @param textSize The text size using percentage
     */
    public void setTextSizeInPercent(int textSize) {
        mScaleIndex = mDefaultScaleIndex;
        for (int i = 0; i <= mMaxIndex; i++) {
            if (textSize == Math.round(SCALES[i] * 100)) {
                mScaleIndex = i;
                return;
            }
        }
    }

    /**
     * @return The text size in percentage
     */
    public int getTextSizeInPercent() {
        return Math.round(SCALES[mScaleIndex] * 100);
    }

    /**
     * Zooms in.
     */
    public void zoomIn() {
        if (mScaleIndex == mMaxIndex) {
            return;
        }
        mScaleIndex++;
        setTextZoom();
    }

    /**
     * Zooms out.
     */
    public void zoomOut() {
        if (mScaleIndex == 0) {
            return;
        }
        mScaleIndex--;
        setTextZoom();
    }

    public void setTextZoom() {
        int position = mViewPager.getCurrentItem();
        int indexOfKey = mViewHolders.indexOfKey(position);
        if (indexOfKey >= 0) {
            ReflowWebView webView = mViewHolders.valueAt(indexOfKey);
            if (webView != null) {
                setTextZoom(webView);
                webView.invalidate();
            }
        }
    }

    private void setTextZoom(WebView webView) {
        if (webView != null) {
            webView.getSettings().setTextZoom(Math.round(SCALES[mScaleIndex] * 100));
        }
    }

    /**
     * @return True if an internal link has been clicked
     */
    public boolean isInternalLinkClicked() {
        return mIsInternalLinkClicked;
    }

    /**
     * When {@link #isInternalLinkClicked()} is called, this should be called to reset that
     * an internal link has been clicked.
     */
    public void resetInternalLinkClicked() {
        mIsInternalLinkClicked = false;
    }

    public int getCurrentPage() {
        return mIsRtlMode ? mPageCount - mViewPager.getCurrentItem() : mViewPager.getCurrentItem() + 1;
    }

    public void setCurrentPage(int pageNum) {
        mViewPager.setCurrentItem(mIsRtlMode ? mPageCount - pageNum : pageNum - 1, false);
    }

    @Override
    public void startUpdate(@NonNull ViewGroup container) {
    }

    @Override
    public int getCount() {
        return mPageCount;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        if (sDebug) Log.d(TAG, "Removing page #" + (position + 1));
        FrameLayout frameLayout = (FrameLayout) object;
        frameLayout.removeAllViews();
        ReflowWebView webView = mViewHolders.get(position);
        if (webView != null) {
            webView.dispose();
        }
        mViewHolders.put(position, null);
        container.removeView(frameLayout);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull final ViewGroup container, final int position) {
        FrameLayout layout = new FrameLayout(mContext);
        int pagePosition = mIsRtlMode ? mPageCount - 1 - position : position;
        ReflowWebView webView = getReflowWebView(pagePosition);

        String filename = mReflowFiles.get(pagePosition);
        boolean need_load_flag = true;
        if (filename != null) {
            File file = new File(filename);
            if (file.exists()) {
                need_load_flag = false;
                if (sDebug) Log.d(TAG, "the file at page #" + (position + 1) + " already received");
                webView.loadUrl("file:///" + filename);
                setTextZoom(webView);
            }
        }

        if (need_load_flag) {
            String loadingFile = LIGHT_MODE_LOADING_FILE;
            if (mColorMode == ColorMode.NightMode) {
                loadingFile = NIGHT_MODE_LOADING_FILE;
            } else if (mColorMode == ColorMode.CustomMode) {
                int r = (mBackgroundColorMode) & 0xFF;
                int g = (mBackgroundColorMode >> 8) & 0xFF;
                int b = (mBackgroundColorMode >> 16) & 0xFF;
                double luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b;
                if (luminance < 128) {
                    loadingFile = DARK_MODE_LOADING_FILE;
                }
            }
            webView.loadUrl(loadingFile);

            Disposable subscribe = requestPageAsync(webView.getContext(), pagePosition, position)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<File>() {
                        @Override
                        public void accept(File file) throws Exception {
                            mReflowFiles.put(pagePosition, file.getAbsolutePath());
                            webView.loadUrl("file:///" + file.getAbsolutePath());
                            setTextZoom(webView);
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            webView.loadUrl("about:blank");
                        }
                    });
            webView.setDisposable(subscribe);
            mDisposables.add(subscribe);
        }

        FrameLayout parent = (FrameLayout) webView.getParent();
        if (parent != null) {
            // note that we share WebViews,
            // so before adding a WebView make sure it's not a child of any layout
            parent.removeAllViews();
        }
        mViewHolders.put(position, webView);
        layout.addView(webView);
        container.addView(layout);
        return layout;
    }

    private Single<File> requestPageAsync(final Context context, final int pagePosition, final int position) {
        return Single.create(new SingleOnSubscribe<File>() {
            @Override
            public void subscribe(@NonNull SingleEmitter<File> emitter) throws Exception {
                if (emitter.isDisposed()) {
                    return;
                }

                boolean shouldUnlockRead = false;
                try {
                    // request for reflow output
                    int pageNumber = pagePosition + 1;
                    if (sDebug) Log.d(TAG, "request for page #" + pageNumber);
                    mDoc.lockRead();
                    shouldUnlockRead = true;
                    Page page = mDoc.getPage(pageNumber);
                    if (emitter.isDisposed()) {
                        return;
                    }
                    Reflow reflow = Convert.createReflow(page, "");
                    // for Nougat and up, we will load annotations in javascript
                    reflow.setHTMLOutputTextMarkup(!isEditingEnabled());
                    reflow.setIncludeImages(mReflowWithImage);
                    reflow.setHideImagesUnderText(mIsHideImagesUnderText);
                    reflow.setHideBackgroundImages(mIsHideBackgroundImages);
                    reflow.setDoNotReflowTextOverImages(mIsDoNotReflowTextOverImages);
                    reflow.setHideImagesUnderInvisibleText(mIsHideImagesUnderInvisibleText);
                    reflow.setMessageWhenNoReflowContent(context.getApplicationContext().getResources().getString(R.string.reflow_no_content));
                    reflow.setMessageWhenReflowFailed(context.getApplicationContext().getResources().getString(R.string.reflow_failed));
                    String reflowHtml = reflow.getHtml();
                    if (reflowHtml == null) {
                        return;
                    }
                    mReflowMap.put(pagePosition, reflow);
                    if (mColorMode == ColorMode.CustomMode || mColorMode == ColorMode.NightMode) {
                        if (mOnPostProcessColorListener != null) {
                            reflowHtml = ReflowUtils.postProcessColor(reflowHtml, mOnPostProcessColorListener);
                        }
                    }
                    if (isEditingEnabled()) {
                        // adds script
                        if (!reflowHtml.contains("</script></body>")) {
                            reflowHtml = reflowHtml.replace("</body>",
                                    "<script type=\"text/javascript\" src=\"file:///android_asset/reflow.js\"></script></body>");
                        }
                    }
                    File reflowOutputFile = File.createTempFile("reflow", ".html");
                    FileWriter fileWriter = null;
                    try {
                        fileWriter = new FileWriter(reflowOutputFile);
                        fileWriter.write(reflowHtml);
                        emitter.onSuccess(reflowOutputFile);
                        return;
                    } catch (Exception ignored) {

                    } finally {
                        if (fileWriter != null) {
                            Utils.closeQuietly(fileWriter);
                        }
                    }
                } catch (Exception e) {
                    AnalyticsHandlerAdapter.getInstance().sendException(e);
                } finally {
                    if (shouldUnlockRead) {
                        Utils.unlockReadQuietly(mDoc);
                    }
                }
                emitter.tryOnError(new Exception("Could not create reflow for this page"));
            }
        });
    }

    @Override
    public boolean onReflowWebViewScaleBegin(WebView webView, ScaleGestureDetector detector) {
        mScaleFactor = mLastScaleFactor = SCALES[mScaleIndex];
        mThConsecutiveScales = TH_SCAlE_GESTURE;
        mZoomInFlag = true;
        return true;
    }

    @Override
    public boolean onReflowWebViewScale(WebView webView, ScaleGestureDetector detector) {
        mScaleFactor *= detector.getScaleFactor();

        // avoid considering a small gesture as scaling
        if (Math.max(mLastScaleFactor / mScaleFactor, mScaleFactor / mLastScaleFactor) < TH_SCAlE_GESTURE) {
            return true;
        }

        if (mZoomInFlag && mScaleFactor > mLastScaleFactor && mScaleFactor / mLastScaleFactor < mThConsecutiveScales) {
            return true;
        }

        if (!mZoomInFlag && mLastScaleFactor > mScaleFactor && mLastScaleFactor / mScaleFactor < mThConsecutiveScales) {
            return true;
        }

        if (mLastScaleFactor > mScaleFactor) {
            if (mScaleIndex > 0) {
                mScaleIndex--;
                mLastScaleFactor = mScaleFactor = SCALES[mScaleIndex];
                if (mZoomInFlag) {
                    mThConsecutiveScales = TH_SCAlE_GESTURE;
                }
                mZoomInFlag = false;
            }
        } else {
            if (mScaleIndex < mMaxIndex) {
                mScaleIndex++;
                mLastScaleFactor = mScaleFactor = SCALES[mScaleIndex];
                if (!mZoomInFlag) {
                    mThConsecutiveScales = TH_SCAlE_GESTURE;
                }
                mZoomInFlag = true;
            }
        }

        setTextZoom(webView);

        mThConsecutiveScales *= TH_SCAlE_GESTURE;
        return true;
    }

    @Override
    public void onReflowWebViewScaleEnd(WebView webView, ScaleGestureDetector detector) {
    }

    @Override
    public void onReflowWebViewSingleTapUp(WebView webView, MotionEvent event) {
        webView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mJustDeselectedAnnot) {
                    mJustDeselectedAnnot = false;
                    return;
                }
                if (mSelectedAnnot != null) {
                    return;
                }
                if (mDoTurnPageOnTap) {
                    boolean isVertical = false;
                    if (webView instanceof ReflowWebView) {
                        isVertical = ((ReflowWebView) webView).isVertical();
                    }
                    if (!isVertical) {
                        int x = (int) (event.getX() + 0.5);
                        float width = mViewPager.getWidth();
                        float widthThresh = width * TAP_REGION_THRESHOLD;
                        int curPosition = mViewPager.getCurrentItem();
                        if (x <= widthThresh) {
                            if (curPosition > 0) {
                                mClickHandler.sendEmptyMessageDelayed(ClickHandler.GO_TO_PREVIOUS_PAGE, 200);
                                return;
                            }
                        } else if (x >= width - widthThresh) {
                            if (curPosition < mPageCount - 1) {
                                mClickHandler.sendEmptyMessageDelayed(ClickHandler.GO_TO_NEXT_PAGE, 200);
                                return;
                            }
                        }
                    }
                }
                if (mCallback != null) {
                    mCallback.onReflowPagerSingleTapUp(webView, event);
                }
            }
        }, 100);
    }

    @Override
    public void onReflowWebViewLongPress(WebView webView, MotionEvent event) {
        if (mCallback != null) {
            mCallback.onReflowPagerLongPress(webView, event);
        }
    }

    @Override
    public void onPageTop(WebView webView) {
        setCurrentPage(getCurrentPage() - 1);
    }

    @Override
    public void onPageBottom(WebView webView) {
        setCurrentPage(getCurrentPage() + 1);
    }

    void setOnPostProcessColorListener(ReflowControl.OnPostProcessColorListener listener) {
        mOnPostProcessColorListener = listener;
    }

    private void raiseAnnotationPreModifyEvent(Annot annot, int page) {
        if (annot == null || mToolManager == null) {
            return;
        }
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        mToolManager.raiseAnnotationsPreModifyEvent(annots);
    }

    private void raiseAnnotationModifiedEvent(Annot annot, int page) {
        if (annot == null || mToolManager == null) {
            return;
        }
        HashMap<Annot, Integer> annots = new HashMap<>(1);
        annots.put(annot, page);
        mToolManager.raiseAnnotationsModifiedEvent(annots, Tool.getAnnotationModificationBundle(null));
    }

    public static void setDebug(boolean debug) {
        sDebug = debug;
    }

    public interface ReflowAppCallback {
        void onAnnotClicked(String id, String annotData);

        void onCleanSelectedAnnot();
    }

    public static class ReflowAppInterface {

        public static final String sName = "pdfNetReflow";

        private final Context mContext;
        private final ConcurrentHashMap<Integer, Reflow> mReflowMap;
        private final ReflowAppCallback mCallback;

        ReflowAppInterface(Context c, ConcurrentHashMap<Integer, Reflow> reflowMap, ReflowAppCallback reflowAppCallback) {
            mContext = c;
            mReflowMap = reflowMap;
            mCallback = reflowAppCallback;
        }

        @JavascriptInterface
        public void showToast(String toast) {
            CommonToast.showText(mContext, toast);
        }

        @JavascriptInterface
        public void annotClicked(String id, String annotData) {
            if (mCallback != null) {
                mCallback.onAnnotClicked(id, annotData);
            }
        }

        @JavascriptInterface
        public void cleanSelectedAnnot() {
            if (mCallback != null) {
                mCallback.onCleanSelectedAnnot();
            }
        }

        @JavascriptInterface
        public String setAnnot(String annot, int pagePosition) {
            if (mReflowMap == null) {
                return null;
            }
            Reflow reflow = mReflowMap.get(pagePosition);
            try {
                if (reflow != null) {
                    return reflow.setAnnot(annot);
                }
            } catch (PDFNetException e) {
                AnalyticsHandlerAdapter.getInstance().sendException(e);
            }
            return null;
        }
    }
}
