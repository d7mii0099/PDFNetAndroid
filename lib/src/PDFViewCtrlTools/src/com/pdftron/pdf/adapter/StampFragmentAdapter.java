//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.adapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.appcompat.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.dialog.CustomStampPickerFragment;
import com.pdftron.pdf.dialog.StandardRubberStampPickerFragment;
import com.pdftron.pdf.interfaces.OnCustomStampSelectedListener;
import com.pdftron.pdf.interfaces.OnRubberStampSelectedListener;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.model.CustomStampPreviewAppearance;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.sdf.Obj;

public class StampFragmentAdapter extends FragmentPagerAdapter
    implements OnRubberStampSelectedListener, OnCustomStampSelectedListener {

    @SuppressWarnings("unused")
    private static final String TAG = StampFragmentAdapter.class.getName();

    private final String mStandardTitle;
    private final String mCustomTitle;
    private StandardStampPreviewAppearance[] mStandardStampPreviewAppearance;
    private CustomStampPreviewAppearance[] mCustomStampPreviewAppearances;
    private Toolbar mToolbar, mCabToolbar;

    private Fragment mCurrentFragment;

    private OnRubberStampSelectedListener mOnRubberStampSelectedListener;

    // theme
    private int mTheme;

    public StampFragmentAdapter(FragmentManager fm, String standardTitle, String customTitle, StandardStampPreviewAppearance[] standardStampPreviewAppearance, CustomStampPreviewAppearance[] customStampPreviewAppearances, @NonNull Toolbar toolbar, @NonNull Toolbar cabToolbar) {
        super(fm);
        mStandardTitle = standardTitle;
        mCustomTitle = customTitle;
        mCustomStampPreviewAppearances = customStampPreviewAppearances;
        mStandardStampPreviewAppearance = standardStampPreviewAppearance;
        mToolbar = toolbar;
        mCabToolbar = cabToolbar;
    }

    @Override
    public void setPrimaryItem(ViewGroup container, int position, Object object) {
        super.setPrimaryItem(container, position, object);

        Fragment fragment = (Fragment) object;
        if (mCurrentFragment != fragment) {
            mCurrentFragment = fragment;
            if (mCurrentFragment instanceof StandardRubberStampPickerFragment) {
                ((StandardRubberStampPickerFragment) mCurrentFragment).setOnRubberStampSelectedListener(this);
                MenuItem menuEdit = mToolbar.getMenu().findItem(R.id.controls_action_edit);
                menuEdit.setVisible(false);
            }
            if (mCurrentFragment instanceof CustomStampPickerFragment) {
                CustomStampPickerFragment customStampPickerFragment = (CustomStampPickerFragment) mCurrentFragment;
                customStampPickerFragment.setOnCustomStampSelectedListener(this);
                customStampPickerFragment.setToolbars(mToolbar, mCabToolbar);
                // the visibility of menuEdit is set in setToolbars
            }
            mToolbar.setVisibility(View.VISIBLE);
            mCabToolbar.setVisibility(View.GONE);
        }
    }

    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                StandardRubberStampPickerFragment standardStampPickerFragment =
                    StandardRubberStampPickerFragment.newInstance(mStandardStampPreviewAppearance);
                standardStampPickerFragment.setOnRubberStampSelectedListener(this);
                return standardStampPickerFragment;
            case 1:
                CustomStampPickerFragment customStampPickerFragment =
                    CustomStampPickerFragment.newInstance(mCustomStampPreviewAppearances);
                customStampPickerFragment.setOnCustomStampSelectedListener(this);
                customStampPickerFragment.setToolbars(mToolbar, mCabToolbar);
                customStampPickerFragment.setTheme(mTheme);
                return customStampPickerFragment;
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        switch (position) {
            case 0:
                return mStandardTitle;
            case 1:
                return mCustomTitle;
            default:
                return null;
        }
    }

    @Override
    public int getCount() {
        return 2;
    }

    @Override
    public void onRubberStampSelected(@NonNull String stampLabel) {
        if (mOnRubberStampSelectedListener != null) {
            mOnRubberStampSelectedListener.onRubberStampSelected(stampLabel);
        }
        sendStandardStampAnalytics(stampLabel);
    }

    @Override
    public void onRubberStampSelected(@Nullable String stampId, @Nullable Obj stampObj) {
        if (mOnRubberStampSelectedListener != null) {
            mOnRubberStampSelectedListener.onRubberStampSelected(stampId, stampObj);
        }
        sendStandardStampAnalytics(getStampName(stampObj));
    }

    @Override
    public void onCustomStampSelected(@Nullable String stampId, @Nullable Obj stampObj) {
        if (mOnRubberStampSelectedListener != null) {
            mOnRubberStampSelectedListener.onRubberStampSelected(stampId, stampObj);
        }
        sendCustomStampAnalytics(stampObj);
    }

    /**
     * Sets the listener to {@link OnRubberStampSelectedListener}.
     *
     * @param listener The listener
     */
    public void setOnRubberStampSelectedListener(OnRubberStampSelectedListener listener) {
        mOnRubberStampSelectedListener = listener;
    }

    /**
     * Sets theme for the child fragments, default to PDFTronAppTheme
     */
    public void setTheme(int theme) {
        mTheme = theme;
    }

    @Nullable
    private String getStampName(@Nullable Obj stampObj) {
        if (stampObj == null) {
            return null;
        }
        try {
            Obj found = stampObj.findObj("TEXT");
            if (found != null && found.isString()) {
                return found.getAsPDFText();
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
        return null;
    }

    private void sendStandardStampAnalytics(@Nullable String stampName) {
        if (Utils.isNullOrEmpty(stampName)) {
            return;
        }
        AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ADD_RUBBER_STAMP,
            AnalyticsParam.addRubberStampParam(AnalyticsHandlerAdapter.RUBBER_STAMP_STANDARD, stampName));
    }

    private void sendCustomStampAnalytics(@Nullable Obj stampObj) {
        if (stampObj == null) {
            return;
        }
        try {
            CustomStampOption stamp = new CustomStampOption(stampObj);
            String color = null;
            for (CustomStampPreviewAppearance mCustomStampPreviewAppearance : mCustomStampPreviewAppearances) {
                if (mCustomStampPreviewAppearance.bgColorStart == stamp.bgColorStart) {
                    color = mCustomStampPreviewAppearance.colorName;
                    break;
                }
            }
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_ADD_RUBBER_STAMP,
                AnalyticsParam.addCustomStampParam(AnalyticsHandlerAdapter.RUBBER_STAMP_CUSTOM, stamp, color));
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

}
