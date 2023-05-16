package com.pdftron.pdf.dialog.signature;

import android.content.DialogInterface;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.controls.CustomSizeDialogFragment;
import com.pdftron.pdf.interfaces.OnCreateSignatureListener;
import com.pdftron.pdf.interfaces.OnDialogDismissListener;
import com.pdftron.pdf.interfaces.OnSavedSignatureListener;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.widget.CustomViewPager;
import com.pdftron.pdf.widget.preset.signature.SignatureViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_ANNOT_STYLE_PROPERTY;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_COLOR;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_CONFIRM_BUTTON_STRING_RES;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_DEFAULT_STORE_NEW_SIGNATURE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_DIALOG_MODE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_PERSIST_STORE_SIGNATURE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_PRESSURE_SENSITIVE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_SHOW_SAVED_SIGNATURES;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_SHOW_SIGNATURE_FROM_IMAGE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_SHOW_SIGNATURE_PRESETS;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_SHOW_TYPED_SIGNATURE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_SIGNATURE_COLORS;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_STROKE_WIDTH;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_TARGET_PAGE;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_TARGET_POINT_X;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_TARGET_POINT_Y;
import static com.pdftron.pdf.dialog.signature.SignatureDialogFragmentBuilder.BUNDLE_TARGET_WIDGET;

public class SignatureDialogFragment extends CustomSizeDialogFragment implements
        OnCreateSignatureListener,
        OnSavedSignatureListener {
    public final static String TAG = SignatureDialogFragment.class.getName();
    private Toolbar mToolbar;

    public enum DialogMode {
        MODE_CREATE(0),
        MODE_SAVED(1);

        public final int value;

        DialogMode(int value) {
            this.value = value;
        }

        @Nullable
        public static DialogMode fromValue(int value) {
            for (DialogMode dialogMode : DialogMode.values()) {
                if (dialogMode.value == value) {
                    return dialogMode;
                }
            }
            return null;
        }
    }

    protected List<OnCreateSignatureListener> mOnCreateSignatureListener = new ArrayList<>();
    public static int MAX_SIGNATURES = -1;

    protected PointF mTargetPointPage; // keep this in the fragment so that can retrieve it when the fragment is re-created
    protected int mTargetPage;
    protected Long mTargetWidget;
    protected int mColor;
    private int[] mSignatureColors;
    protected float mStrokeWidth;

    protected CustomViewPager mViewPager;

    protected boolean mShowSavedSignatures;
    protected boolean mShowSignaturePresets;
    protected boolean mShowSignatureFromImage;
    protected boolean mShowTypedSignature;
    protected boolean mPressureSensitive = true;
    private boolean mDefaultStoreNewSignature = true;
    private boolean mPersistStoreSignatureSetting = true;

    @Nullable
    protected HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;

    protected int mConfirmBtnStrRes;

    protected OnDialogDismissListener mOnDialogDismissListener;

    // new UI
    @Nullable
    protected DialogMode mDialogMode = null;

    @Nullable
    private SignatureViewModel mSignatureViewModel = null;

    @SuppressWarnings("unchecked")
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            float x = args.getFloat(BUNDLE_TARGET_POINT_X, SignatureDialogFragmentBuilder.TARGET_POINT_X);
            float y = args.getFloat(BUNDLE_TARGET_POINT_Y, SignatureDialogFragmentBuilder.TARGET_POINT_Y);
            if (x > 0 && y > 0) {
                mTargetPointPage = new PointF(x, y);
            }
            mTargetPage = args.getInt(BUNDLE_TARGET_PAGE, SignatureDialogFragmentBuilder.TARGET_PAGE);
            mTargetWidget = args.getLong(BUNDLE_TARGET_WIDGET);
            mColor = args.getInt(BUNDLE_COLOR);
            mSignatureColors = args.getIntArray(BUNDLE_SIGNATURE_COLORS);
            mStrokeWidth = args.getFloat(BUNDLE_STROKE_WIDTH);
            mShowSavedSignatures = args.getBoolean(BUNDLE_SHOW_SAVED_SIGNATURES, SignatureDialogFragmentBuilder.SHOW_SAVED_SIGNATURE);
            mShowSignaturePresets = args.getBoolean(BUNDLE_SHOW_SIGNATURE_PRESETS, SignatureDialogFragmentBuilder.SHOW_SIGNATURE_PRESETS);
            mShowSignatureFromImage = args.getBoolean(BUNDLE_SHOW_SIGNATURE_FROM_IMAGE, SignatureDialogFragmentBuilder.SHOW_SIGNATURE_FROM_IMAGE);
            mShowTypedSignature = args.getBoolean(BUNDLE_SHOW_TYPED_SIGNATURE, SignatureDialogFragmentBuilder.SHOW_TYPED_SIGNATURE);
            mConfirmBtnStrRes = args.getInt(BUNDLE_CONFIRM_BUTTON_STRING_RES, SignatureDialogFragmentBuilder.CONFIRM_BUTTON_RES);
            mPressureSensitive = args.getBoolean(BUNDLE_PRESSURE_SENSITIVE, SignatureDialogFragmentBuilder.PRESSURE_SENSITIVE);
            mAnnotStyleProperties = (HashMap<Integer, AnnotStyleProperty>) args.getSerializable(BUNDLE_ANNOT_STYLE_PROPERTY);
            mDefaultStoreNewSignature = args.getBoolean(BUNDLE_DEFAULT_STORE_NEW_SIGNATURE, SignatureDialogFragmentBuilder.DEFAULT_STORE_NEW_SIGNATURE);
            mPersistStoreSignatureSetting = args.getBoolean(BUNDLE_PERSIST_STORE_SIGNATURE, SignatureDialogFragmentBuilder.PERSIST_STORE_SIGNATURE);
            DialogMode dialogMode = DialogMode.fromValue(args.getInt(BUNDLE_DIALOG_MODE, -1));
            if (dialogMode != null) {
                mDialogMode = dialogMode;
            }
        }
        FragmentActivity activity = getActivity();
        if (activity != null) {
            mSignatureViewModel = new ViewModelProvider(activity).get(SignatureViewModel.class);
            mSignatureViewModel.populateSignaturesAsync(activity);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rubber_stamp_dialog, container);

        mToolbar = view.findViewById(R.id.stamp_dialog_toolbar);

        // Set DialogMode if not set yet
        if (mDialogMode == null) {
            mDialogMode = DialogMode.MODE_CREATE;
            File[] files = StampManager.getInstance().getSavedSignatures(view.getContext());
            if (files != null && files.length > 0) {
                mDialogMode = DialogMode.MODE_SAVED;
            }
        }

        if (mDialogMode == DialogMode.MODE_CREATE) {
            mToolbar.setTitle(R.string.tools_signature_create_title);
        } else {
            mToolbar.setTitle(R.string.tools_signature_saved_title);
        }
        mToolbar.inflateMenu(R.menu.controls_fragment_edit_toolbar);
        Toolbar cabToolbar = view.findViewById(R.id.stamp_dialog_toolbar_cab);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        mViewPager = view.findViewById(R.id.stamp_dialog_view_pager);
        SignatureFragmentAdapter viewPagerAdapter = new SignatureFragmentAdapter(getChildFragmentManager(),
                getString(R.string.saved), getString(R.string.create),
                mToolbar, cabToolbar,
                mColor, mStrokeWidth,
                mShowSavedSignatures,
                mShowSignatureFromImage,
                mShowSignaturePresets,
                mConfirmBtnStrRes,
                this,
                this,
                mPressureSensitive,
                mAnnotStyleProperties,
                mDefaultStoreNewSignature,
                mPersistStoreSignatureSetting);
        viewPagerAdapter.setShowTypedSignature(mShowTypedSignature);
        viewPagerAdapter.setSignatureColors(mSignatureColors);
        mViewPager.setAdapter(viewPagerAdapter);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (position <= viewPagerAdapter.getCount()) {
                    Fragment fragment = viewPagerAdapter.getItem(position);
                    if (fragment instanceof CreateSignatureFragment) {
                        mToolbar.setTitle(R.string.tools_signature_create_title);
                    } else if (fragment instanceof SavedSignaturePickerFragment) {
                        mToolbar.setTitle(R.string.tools_signature_saved_title);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        TabLayout tabLayout = view.findViewById(R.id.stamp_dialog_tab_layout);
        tabLayout.setupWithViewPager(mViewPager);

        // in new UI we always show only 1 dialog at a time
        tabLayout.setVisibility(View.GONE);
        mViewPager.setSwippingEnabled(false);

        if (mShowSavedSignatures) {
            if (mDialogMode == DialogMode.MODE_CREATE) {
                mViewPager.setCurrentItem(1);
            } else {
                mViewPager.setCurrentItem(0);
            }
        }

        return view;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (mOnDialogDismissListener != null) {
            mOnDialogDismissListener.onDialogDismiss();
        }
        if (mSignatureViewModel != null && getActivity() != null) {
            mSignatureViewModel.populateSignaturesAsync(getActivity());
        }
    }

    /**
     * Sets the listener to {@link OnDialogDismissListener}.
     *
     * @param listener The listener
     */
    public void setOnDialogDismissListener(OnDialogDismissListener listener) {
        mOnDialogDismissListener = listener;
    }

    /**
     * Adds a {@link OnCreateSignatureListener} to listen for signature events
     *
     * @param listener the listener to add
     * @deprecated use {@link #addOnCreateSignatureListener(OnCreateSignatureListener)} instead
     */
    @Deprecated
    public void setOnCreateSignatureListener(@Nullable OnCreateSignatureListener listener) {
        mOnCreateSignatureListener.clear();
        if (listener != null) {
            mOnCreateSignatureListener.add(listener);
        }
    }

    /**
     * Adds a {@link OnCreateSignatureListener} to listen for signature events
     *
     * @param listener the listener to add
     */
    public void addOnCreateSignatureListener(@NonNull OnCreateSignatureListener listener) {
        mOnCreateSignatureListener.add(listener);
    }

    /**
     * Removes the {@link OnCreateSignatureListener}
     *
     * @param listener the listener to remove
     */
    public void removeOnCreateSignatureListener(@NonNull OnCreateSignatureListener listener) {
        mOnCreateSignatureListener.remove(listener);
    }

    @Override
    public void onSignatureCreated(@Nullable String filepath, boolean saveSignature) {
        if (filepath != null) {
            onSignatureSelected(filepath, saveSignature);
        }
    }

    @Override
    public void onSignatureFromImage(@Nullable PointF targetPoint, int targetPage, @Nullable Long widget) {
        if (mOnCreateSignatureListener != null) {
            for (OnCreateSignatureListener listener : mOnCreateSignatureListener) {
                listener.onSignatureFromImage(mTargetPointPage, mTargetPage, mTargetWidget);
            }
        }
        dismiss();
    }

    @Override
    public void onAnnotStyleDialogFragmentDismissed(AnnotStyleDialogFragment styleDialog) {
        if (mOnCreateSignatureListener != null) {
            for (OnCreateSignatureListener listener : mOnCreateSignatureListener) {
                listener.onAnnotStyleDialogFragmentDismissed(styleDialog);
            }
        }
    }

    @Override
    public void onSignatureSelected(@NonNull String filepath) {
        onSignatureSelected(filepath, true);
    }

    @Override
    public void onCreateSignatureClicked() {
        mViewPager.setCurrentItem(1);
    }

    @Override
    public void onEditModeChanged(boolean isEdit) {

    }

    protected void onSignatureSelected(@NonNull String filepath, boolean saveSignature) {
        if (mOnCreateSignatureListener != null) {
            for (OnCreateSignatureListener listener : mOnCreateSignatureListener) {
                listener.onSignatureCreated(filepath, saveSignature);
            }
        }
        dismiss();
    }

    public static boolean atMaxSignatureCount(int numSignatures) {
        if (MAX_SIGNATURES == -1) {
            return false;
        } else {
            return numSignatures >= MAX_SIGNATURES;
        }
    }
}
