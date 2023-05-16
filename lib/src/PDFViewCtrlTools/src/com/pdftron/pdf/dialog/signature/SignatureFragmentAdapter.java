package com.pdftron.pdf.dialog.signature;

import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.interfaces.OnSavedSignatureListener;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.R;

import java.util.HashMap;

public class SignatureFragmentAdapter extends FragmentPagerAdapter {

    private final String mStandardTitle;
    private final String mCustomTitle;
    private Toolbar mToolbar, mCabToolbar;

    private int mColor;
    private int[] mSignatureColors;
    private float mStrokeWidth;

    private Fragment mCurrentFragment;

    private boolean mShowSavedSignatures;
    private boolean mShowSignatureFromImage;
    private boolean mShowTypedSignature;
    private boolean mShowSignaturePresets;

    private int mConfirmBtnStrRes;
    private boolean mIsPressureSensitive;
    private boolean mDefaultStoreNewSignature = true;
    private boolean mPersistStoreSignatureSetting = true;

    @Nullable
    private HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;

    private OnCreateSignatureListener mOnCreateSignatureListener;
    private OnSavedSignatureListener mOnSavedSignatureListener;

    public SignatureFragmentAdapter(FragmentManager fm, String standardTitle, String customTitle,
            @NonNull Toolbar toolbar, @NonNull Toolbar cabToolbar,
            int color, float thickness,
            boolean showSavedSignatures,
            boolean showSignatureFromImage,
            boolean showSignaturePresets,
            int confirmBtnStrRes,
            OnCreateSignatureListener onCreateSignatureListener,
            OnSavedSignatureListener onSavedSignatureListener,
            boolean isPressureSensitive,
            @Nullable HashMap<Integer, AnnotStyleProperty> annotStyleProperties,
            boolean defaultStoreNewSignature,
            boolean persistStoreSignatureSetting) {
        super(fm);
        mStandardTitle = standardTitle;
        mCustomTitle = customTitle;
        mToolbar = toolbar;
        mCabToolbar = cabToolbar;
        mColor = color;
        mStrokeWidth = thickness;
        mShowSavedSignatures = showSavedSignatures;
        mShowSignatureFromImage = showSignatureFromImage;
        mShowSignaturePresets = showSignaturePresets;
        mOnCreateSignatureListener = onCreateSignatureListener;
        mOnSavedSignatureListener = onSavedSignatureListener;
        mConfirmBtnStrRes = confirmBtnStrRes;
        mIsPressureSensitive = isPressureSensitive;
        mAnnotStyleProperties = annotStyleProperties;
        mDefaultStoreNewSignature = defaultStoreNewSignature;
        mPersistStoreSignatureSetting = persistStoreSignatureSetting;

    }

    @Override
    public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        super.setPrimaryItem(container, position, object);

        Fragment fragment = (Fragment) object;
        if (mCurrentFragment != fragment) {
            mCurrentFragment = fragment;
            if (mCurrentFragment instanceof SavedSignaturePickerFragment) {
                ((SavedSignaturePickerFragment) mCurrentFragment).setOnSavedSignatureListener(mOnSavedSignatureListener);
                ((SavedSignaturePickerFragment) mCurrentFragment).resetToolbar(container.getContext());
                MenuItem menuEdit = mToolbar.getMenu().findItem(R.id.controls_action_edit);
                menuEdit.setTitle(R.string.tools_qm_edit);
            } else if (mCurrentFragment instanceof CreateSignatureFragment) {
                ((CreateSignatureFragment) mCurrentFragment).setOnCreateSignatureListener(mOnCreateSignatureListener);
                ((CreateSignatureFragment) mCurrentFragment).resetToolbar(container.getContext());
                MenuItem menuEdit = mToolbar.getMenu().findItem(R.id.controls_action_edit);
                menuEdit.setTitle(mConfirmBtnStrRes);
            }

            mToolbar.setVisibility(View.VISIBLE);
            mCabToolbar.setVisibility(View.GONE);
        }
    }

    @Override
    public Fragment getItem(int position) {
        if (mShowSavedSignatures) {
            switch (position) {
                case 0:
                   return getSavedSignaturePickerFragment();
                default:
                    return getCreateSignatureFragment();
            }
        } else {
            return getCreateSignatureFragment();
        }
    }

    private CreateSignatureFragment getCreateSignatureFragment() {
        CreateSignatureFragment signatureFragment =
                CreateSignatureFragment.newInstance(
                        mColor,
                        mStrokeWidth,
                        mShowSignatureFromImage,
                        mShowTypedSignature,
                        mShowSignaturePresets,
                        mShowSavedSignatures,
                        mIsPressureSensitive,
                        mAnnotStyleProperties,
                        mDefaultStoreNewSignature,
                        mPersistStoreSignatureSetting,
                        mSignatureColors
                );
        signatureFragment.setOnCreateSignatureListener(mOnCreateSignatureListener);
        signatureFragment.setToolbar(mToolbar);
        return signatureFragment;
    }

    private SavedSignaturePickerFragment getSavedSignaturePickerFragment() {
        SavedSignaturePickerFragment pickerFragment = SavedSignaturePickerFragment.newInstance();
        pickerFragment.setToolbars(mToolbar, mCabToolbar);
        pickerFragment.setOnSavedSignatureListener(mOnSavedSignatureListener);
        return pickerFragment;
    }

    @Override
    public int getCount() {
        return mShowSavedSignatures ? 2 : 1;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        if (mShowSavedSignatures) {
            switch (position) {
                case 0:
                    return mStandardTitle;
                case 1:
                    return mCustomTitle;
                default:
                    return null;
            }
        } else {
            return mCustomTitle;
        }
    }

    public SignatureFragmentAdapter setShowTypedSignature(boolean showTypedSignature) {
        mShowTypedSignature = showTypedSignature;
        return this;
    }

    public SignatureFragmentAdapter setSignatureColors(@ColorInt int... signatureColors) {
        mSignatureColors = signatureColors;
        return this;
    }
}
