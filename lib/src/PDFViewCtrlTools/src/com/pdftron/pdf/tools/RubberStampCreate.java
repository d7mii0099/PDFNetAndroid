//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.tools;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.RubberStamp;
import com.pdftron.pdf.controls.RubberStampDialogFragment;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.interfaces.OnRubberStampSelectedListener;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.model.CustomStampPreviewAppearance;
import com.pdftron.pdf.model.StandardStampOption;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.ToolManager.ToolMode;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

import org.json.JSONObject;

/**
 * This class is for creating rubber stamp annotation.
 */
@Keep
public class RubberStampCreate extends Stamper {

    // default values
    public static final CustomStampPreviewAppearance[] sCustomStampPreviewAppearances = new CustomStampPreviewAppearance[]{
            new CustomStampPreviewAppearance("green", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85),
            new CustomStampPreviewAppearance("red", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85),
            new CustomStampPreviewAppearance("blue", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85),
            new CustomStampPreviewAppearance("dark yellow", 0xfffbf7aa, 0xffF8F055, 0xffe5da09, 0xFF3f3c02, 0xFFd0ad2e, 1),
            new CustomStampPreviewAppearance("dark_purple", 0xffc6bee6, 0xff8E7FCD, 0xff8878ca, 0xFF18122f, 0xFF413282, 1),
            new CustomStampPreviewAppearance("dark_red", 0xffda7a67, 0xffCF4E35, 0xffd5624b, 0xFF2a0f09, 0xFF6e0005, 1),
    };
    public static final StandardStampPreviewAppearance[] sStandardStampPreviewAppearance = new StandardStampPreviewAppearance[]{
            new StandardStampPreviewAppearance("APPROVED", R.string.standard_stamp_text_approved, new CustomStampPreviewAppearance("", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85)),
            new StandardStampPreviewAppearance("AS IS", R.string.standard_stamp_text_as_is, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("COMPLETED", R.string.standard_stamp_text_completed, new CustomStampPreviewAppearance("", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85)),
            new StandardStampPreviewAppearance("CONFIDENTIAL", R.string.standard_stamp_text_confidential, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("DEPARTMENTAL", R.string.standard_stamp_text_departmental, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("DRAFT", R.string.standard_stamp_text_draft, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("EXPERIMENTAL", R.string.standard_stamp_text_experimental, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("EXPIRED", R.string.standard_stamp_text_expired, new CustomStampPreviewAppearance("", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85)),
            new StandardStampPreviewAppearance("FINAL", R.string.standard_stamp_text_final, new CustomStampPreviewAppearance("", 0xffF4F8EE, 0xFFdee7d8, 0xffD4E0CC, 0xFF267F00, 0xFF2E4B11, .85)),
            new StandardStampPreviewAppearance("FOR COMMENT", R.string.standard_stamp_text_for_comment, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("FOR PUBLIC RELEASE", R.string.standard_stamp_text_for_public_release, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("INFORMATION ONLY", R.string.standard_stamp_text_information_only, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("NOT APPROVED", R.string.standard_stamp_text_not_approved, new CustomStampPreviewAppearance("", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85)),
            new StandardStampPreviewAppearance("NOT FOR PUBLIC RELEASE", R.string.standard_stamp_text_not_for_public_release, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("PRELIMINARY RESULTS", R.string.standard_stamp_text_preliminary_results, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("SOLD", R.string.standard_stamp_text_sold, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("TOP SECRET", R.string.standard_stamp_text_top_secret, new CustomStampPreviewAppearance("", 0xffeff3fa, 0xffE0E8F6, 0xffa6bde5, 0xFF2E3090, 0xFF2E3090, .85)),
            new StandardStampPreviewAppearance("VOID", R.string.standard_stamp_text_void, new CustomStampPreviewAppearance("", 0xffFAEBE8, 0xfffed6d6, 0xffFFC9C9, 0xFF9C0E04, 0xFF9C0E04, .85)),
            new StandardStampPreviewAppearance("SIGN HERE", R.string.standard_stamp_text_sign_here, new CustomStampPreviewAppearance("", 0xffda7a67, 0xffCF4E35, 0xffd5624b, 0xFF2a0f09, 0xFF6e0005, 1), true, false),
            new StandardStampPreviewAppearance("WITNESS", R.string.standard_stamp_text_witness, new CustomStampPreviewAppearance("", 0xfffbf7aa, 0xffF8F055, 0xffe5da09, 0xFF3f3c02, 0xFFd0ad2e, 1), true, false),
            new StandardStampPreviewAppearance("INITIAL HERE", R.string.standard_stamp_text_initial_here, new CustomStampPreviewAppearance("", 0xffc6bee6, 0xff8E7FCD, 0xff8878ca, 0xFF18122f, 0xFF413282, 1), true, false),
            new StandardStampPreviewAppearance("CHECK_MARK"),
            new StandardStampPreviewAppearance("CROSS_MARK"),
    };

    // DO NOT CHANGE
    // THESE ARE PAGE LABELS
    public static final String sCHECK_MARK_LABEL = "FILL_CHECK";
    public static final String sCROSS_LABEL = "FILL_CROSS";
    public static final String sDOT_LABEL = "FILL_DOT";

    private StandardStampPreviewAppearance[] mStandardStampPreviewAppearance = sStandardStampPreviewAppearance;
    private CustomStampPreviewAppearance[] mCustomStampPreviewAppearances = sCustomStampPreviewAppearances;

    @Nullable
    private String mStampLabel;

    /**
     * Class constructor
     */
    public RubberStampCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
        mNextToolMode = getToolMode();

        FragmentActivity activity = ((ToolManager) ctrl.getToolManager()).getCurrentActivity();
        if (activity != null) {
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(RubberStampDialogFragment.TAG);
            if (fragment instanceof RubberStampDialogFragment) {
                setRubberStampDialogFragmentListeners((RubberStampDialogFragment) fragment, mOnRubberStampSelectedListener, null);
            }
        }
    }

    /**
     * Sets how to show stamp appearances in custom rubber stamp dialog.
     *
     * @param standardStampPreviewAppearance An array of standard rubber stamp appearances; null for default
     * @param customStampPreviewAppearance   An array of custom rubber stamp appearances; null for default
     */
    @SuppressWarnings("unused")
    public void setCustomStampAppearance(@Nullable StandardStampPreviewAppearance[] standardStampPreviewAppearance, @Nullable CustomStampPreviewAppearance[] customStampPreviewAppearance) {
        if (standardStampPreviewAppearance != null) {
            mStandardStampPreviewAppearance = standardStampPreviewAppearance;
        }
        if (customStampPreviewAppearance != null) {
            mCustomStampPreviewAppearances = customStampPreviewAppearance;
        }
    }

    /**
     * Sets the exact name to stamp, if this is set, when clicking on a target, the specified stamp will be created.
     *
     * @param stampLabel the name of the stamp
     */
    public void setStampName(@Nullable String stampLabel) {
        mStampLabel = stampLabel;
    }

    /**
     * The overload implementation of {@link Tool#getToolMode()}.
     */
    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolMode.RUBBER_STAMPER;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Stamp;
    }

    @Override
    public void setupAnnotProperty(AnnotStyle annotStyle) {
        super.setupAnnotProperty(annotStyle);

        mStampLabel = annotStyle.getStampId();
    }

    /**
     * The overload implementation of {@link Stamper#addStamp()}.
     */
    @Override
    protected void addStamp() {
        if (mTargetPoint == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("target point is not specified."));
            return;
        }

        if (mPdfViewCtrl == null) {
            return;
        }

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }

        if (mStampLabel != null) {
            // create a pre-defined stamp
            if (isPredefinedStamp(mStampLabel)) {
                StandardStampPreviewAppearance standardStampAppearance = getStandardStampAppearance(mStampLabel);
                if (standardStampAppearance != null) {
                    String cacheLabel = standardStampAppearance.getText(activity);
                    Obj stampObj = StandardStampOption.getStandardStampObj(mPdfViewCtrl.getContext(), cacheLabel);
                    if (stampObj != null) {
                        createCustomStamp(stampObj);
                    }
                }
            } else {
                Obj obj = getCustomStampObj(mStampLabel);
                if (obj != null) {
                    createCustomStamp(obj);
                } else {
                    createStandardRubberStamp(mStampLabel);
                }
            }
            // reset target point
            clearTargetPoint();
            safeSetNextToolMode();
            return;
        }

        showRubberStampDialogFragment();
    }

    // stamp that is pre-defined but not from a PDF page
    private boolean isPredefinedStamp(@NonNull String stampId) {
        for (StandardStampPreviewAppearance appearance : mStandardStampPreviewAppearance) {
            if (stampId.equals(appearance.stampLabel) && appearance.previewAppearance != null) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    private StandardStampPreviewAppearance getStandardStampAppearance(@NonNull String stampId) {
        for (StandardStampPreviewAppearance appearance : mStandardStampPreviewAppearance) {
            if (stampId.equals(appearance.stampLabel)) {
                return appearance;
            }
        }
        return null;
    }

    @Nullable
    private Obj getCustomStampObj(@NonNull String stampId) {
        try {
            JSONObject jsonObject = new JSONObject(stampId);
            int index = jsonObject.optInt(CustomStampOption.KEY_INDEX);
            return CustomStampOption.getCustomStampObj(mPdfViewCtrl.getContext(), index);
        } catch (Exception ignored) {
        }
        return null;
    }

    private OnRubberStampSelectedListener mOnRubberStampSelectedListener = new OnRubberStampSelectedListener() {
        @Override
        public void onRubberStampSelected(@NonNull String stampLabel) {
            if (mPdfViewCtrl == null) {
                return;
            }

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            FragmentActivity activity = toolManager.getCurrentActivity();
            if (activity == null) {
                return;
            }
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(RubberStampDialogFragment.TAG);
            if (fragment instanceof RubberStampDialogFragment) {
                mTargetPoint = ((RubberStampDialogFragment) fragment).getTargetPoint();
            }
            if (!Utils.isNullOrEmpty(stampLabel) && mTargetPoint != null) {
                createStandardRubberStamp(stampLabel);
            }

            if (toolManager.getStampDialogListener() != null && !Utils.isNullOrEmpty(stampLabel)) {
                toolManager.getStampDialogListener().onSaveStampPreset(getCreateAnnotType(), stampLabel);
            }
        }

        @Override
        public void onRubberStampSelected(@Nullable String stampId, @Nullable Obj stampObj) {
            if (mPdfViewCtrl == null) {
                return;
            }

            ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
            FragmentActivity activity = toolManager.getCurrentActivity();
            if (activity == null) {
                return;
            }
            Fragment fragment = activity.getSupportFragmentManager().findFragmentByTag(RubberStampDialogFragment.TAG);
            if (fragment instanceof RubberStampDialogFragment) {
                mTargetPoint = ((RubberStampDialogFragment) fragment).getTargetPoint();
            }
            if (stampObj != null && mTargetPoint != null) {
                createCustomStamp(stampObj);
            }
            if (toolManager.getStampDialogListener() != null && !Utils.isNullOrEmpty(stampId)) {
                toolManager.getStampDialogListener().onSaveStampPreset(getCreateAnnotType(), stampId);
            }
        }
    };

    private void showRubberStampDialogFragment() {
        showRubberStampDialogFragment(mOnRubberStampSelectedListener, null);
    }

    public void showRubberStampDialogFragment(OnRubberStampSelectedListener selectedListener, OnDialogDismissListener dismissListener) {
        setCurrentDefaultToolModeHelper(getToolMode());
        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();
        FragmentActivity activity = toolManager.getCurrentActivity();
        if (activity == null) {
            return;
        }
        RubberStampDialogFragment fragment = RubberStampDialogFragment.newInstance(mTargetPoint, mStandardStampPreviewAppearance, mCustomStampPreviewAppearances);
        fragment.setStyle(DialogFragment.STYLE_NORMAL, toolManager.getTheme());
        fragment.show(activity.getSupportFragmentManager(), RubberStampDialogFragment.TAG);
        setRubberStampDialogFragmentListeners(fragment, selectedListener, dismissListener);
    }

    private void setRubberStampDialogFragmentListeners(@NonNull final RubberStampDialogFragment fragment,
            final OnRubberStampSelectedListener listener, final OnDialogDismissListener dismissListener) {
        fragment.setOnRubberStampSelectedListener(listener);

        fragment.setOnDialogDismissListener(new OnDialogDismissListener() {
            @Override
            public void onDialogDismiss() {
                // reset target point
                clearTargetPoint();
                safeSetNextToolMode();

                if (dismissListener != null) {
                    dismissListener.onDialogDismiss();
                }
            }
        });
    }

    private void createStandardRubberStamp(@NonNull String stampName) {
        if (mTargetPoint == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(mTargetPoint.x, mTargetPoint.y);
            if (pageNum < 1) {
                return;
            }

            double[] size = AnnotUtils.getStampSize(mPdfViewCtrl.getContext(), stampName);
            if (size == null) {
                return;
            }
            int width = (int) (size[0] + .5);
            int height = (int) (size[1] + .5);
            double[] pageTarget = mPdfViewCtrl.convScreenPtToPagePt(mTargetPoint.x, mTargetPoint.y, pageNum);
            Rect rect = new Rect(
                    pageTarget[0] - width / 2.0,
                    pageTarget[1] - height / 2.0,
                    pageTarget[0] + width / 2.0,
                    pageTarget[1] + height / 2.0);
            Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
            boundToCropBox(page, rect);
            RubberStamp stamp = RubberStamp.create(mPdfViewCtrl.getDoc(), rect);
            stamp.setIcon(stampName);
            AnnotUtils.refreshAnnotAppearance(mPdfViewCtrl.getContext(), stamp);
            setAuthor(stamp);

            page.annotPushBack(stamp);

            mPdfViewCtrl.update(stamp, pageNum);
            raiseAnnotationAddedEvent(stamp, pageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private void createCustomStamp(@NonNull Obj stampObj) {
        if (mTargetPoint == null) {
            return;
        }

        boolean shouldUnlock = false;
        try {
            mPdfViewCtrl.docLock(true);
            shouldUnlock = true;

            int pageNum = mPdfViewCtrl.getPageNumberFromScreenPt(mTargetPoint.x, mTargetPoint.y);
            if (pageNum < 1) {
                return;
            }

            Rect rubberRect = getCustomRubberRect(stampObj);
            int width = (int) (rubberRect.getWidth() + .5);
            int height = (int) (rubberRect.getHeight() + .5);
            double[] pageTarget = mPdfViewCtrl.convScreenPtToPagePt(mTargetPoint.x, mTargetPoint.y, pageNum);
            Rect rect = new Rect(
                    pageTarget[0] - width / 2.0,
                    pageTarget[1] - height / 2.0,
                    pageTarget[0] + width / 2.0,
                    pageTarget[1] + height / 2.0);
            Page page = mPdfViewCtrl.getDoc().getPage(pageNum);
            boundToCropBox(page, rect);
            RubberStamp stamp = RubberStamp.createCustom(mPdfViewCtrl.getDoc(), rect, stampObj);
            setAuthor(stamp);

            page.annotPushBack(stamp);

            mPdfViewCtrl.update(stamp, pageNum);
            raiseAnnotationAddedEvent(stamp, pageNum);
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        } finally {
            if (shouldUnlock) {
                mPdfViewCtrl.docUnlock();
            }
        }
    }

    private Rect getCustomRubberRect(@NonNull Obj stampObj) throws PDFNetException {
        PDFDoc tempDoc = null;
        try {
            tempDoc = new PDFDoc();
            tempDoc.initSecurityHandler();
            RubberStamp rubberStamp = RubberStamp.createCustom(tempDoc, new Rect(), stampObj);
            return rubberStamp.getRect();
        } finally {
            Utils.closeQuietly(tempDoc);
        }
    }

    private void boundToCropBox(@NonNull Page page, @NonNull Rect rect) throws PDFNetException {
        com.pdftron.pdf.Rect cropBox = page.getBox(mPdfViewCtrl.getPageBox());
        cropBox.normalize();

        double width = rect.getWidth();
        double height = rect.getHeight();
        if (rect.getX1() < cropBox.getX1()) {
            rect.setX1(cropBox.getX1());
            rect.setX2(cropBox.getX1() + width);
        }
        if (rect.getX2() > cropBox.getX2()) {
            rect.setX2(cropBox.getX2());
            rect.setX1(cropBox.getX2() - width);
        }
        if (rect.getY1() < cropBox.getY1()) {
            rect.setY1(cropBox.getY1());
            rect.setY2(cropBox.getY1() + height);
        }
        if (rect.getY2() > cropBox.getY2()) {
            rect.setY2(cropBox.getY2());
            rect.setY1(cropBox.getY2() - height);
        }
    }
}
