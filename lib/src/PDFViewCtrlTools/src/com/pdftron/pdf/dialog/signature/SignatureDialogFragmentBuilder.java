package com.pdftron.pdf.dialog.signature;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.pdftron.pdf.interfaces.builder.SkeletalFragmentBuilder;
import com.pdftron.pdf.model.AnnotStyleProperty;
import com.pdftron.pdf.tools.R;

import java.util.HashMap;

public class SignatureDialogFragmentBuilder extends SkeletalFragmentBuilder<SignatureDialogFragment> {

    final static String BUNDLE_TARGET_POINT_X = "target_point_x";
    final static String BUNDLE_TARGET_POINT_Y = "target_point_y";
    final static String BUNDLE_TARGET_WIDGET = "target_widget";
    final static String BUNDLE_TARGET_PAGE = "target_page";
    final static String BUNDLE_COLOR = "bundle_color";
    final static String BUNDLE_SIGNATURE_COLORS = "bundle_signature_colors";
    final static String BUNDLE_STROKE_WIDTH = "bundle_stroke_width";
    final static String BUNDLE_SHOW_SAVED_SIGNATURES = "bundle_show_saved_signatures";
    final static String BUNDLE_SHOW_SIGNATURE_PRESETS = "bundle_show_signature_presets";
    final static String BUNDLE_SHOW_SIGNATURE_FROM_IMAGE = "bundle_signature_from_image";
    final static String BUNDLE_SHOW_TYPED_SIGNATURE = "bundle_typed_signature";
    final static String BUNDLE_CONFIRM_BUTTON_STRING_RES = "bundle_confirm_button_string_res";
    final static String BUNDLE_PRESSURE_SENSITIVE = "bundle_pressure_sensitive";
    final static String BUNDLE_ANNOT_STYLE_PROPERTY = "annot_style_property";
    public final static String BUNDLE_HAS_DEFAULT_KEYSTORE = "bundle_digital_signature";
    final static String BUNDLE_DEFAULT_STORE_NEW_SIGNATURE = "bundle_store_new_signature";
    final static String BUNDLE_PERSIST_STORE_SIGNATURE = "bundle_persist_store_signature";
    final static String BUNDLE_DIALOG_MODE = "bundle_dialog_mode";

    // Default values for builder
    public final static boolean HAS_DEFAULT_KEYSTORE = false;
    final static int TARGET_POINT_X = -1;
    final static int TARGET_POINT_Y = -1;
    final static int TARGET_PAGE = -1;
    final static boolean SHOW_SAVED_SIGNATURE = true;
    final static boolean SHOW_SIGNATURE_PRESETS = true;
    final static boolean SHOW_SIGNATURE_FROM_IMAGE = true;
    final static boolean SHOW_TYPED_SIGNATURE = true;
    @StringRes
    final static int CONFIRM_BUTTON_RES = R.string.done;
    final static boolean PRESSURE_SENSITIVE = true;
    final static boolean DEFAULT_STORE_NEW_SIGNATURE = true;
    final static boolean PERSIST_STORE_SIGNATURE = true;

    // Variable values
    private PointF mTargetPoint; // keep this in the fragment so that can retrieve it when the fragment is re-created
    private int mTargetPage;
    private Long mTargetWidget;
    private int mColor;
    private int mSignatureColorsSize;
    private int[] mSignatureColors;
    private float mStrokeWidth;

    private boolean mShowSavedSignatures = SHOW_SAVED_SIGNATURE;
    private boolean mShowSignaturePresets = SHOW_SIGNATURE_PRESETS;
    private boolean mShowSignatureFromImage = SHOW_SIGNATURE_FROM_IMAGE;
    private boolean mShowTypedSignature = SHOW_TYPED_SIGNATURE;
    private boolean mPressureSensitive = PRESSURE_SENSITIVE;
    @Deprecated
    private boolean mDigitalSignature = false;
    private boolean mHasDefaultKeystore = HAS_DEFAULT_KEYSTORE;
    protected HashMap<Integer, AnnotStyleProperty> mAnnotStyleProperties;
    private boolean mDefaultStoreNewSignature = DEFAULT_STORE_NEW_SIGNATURE;
    private boolean mPersistStoreSignatureSetting = PERSIST_STORE_SIGNATURE;
    @Nullable
    private SignatureDialogFragment.DialogMode mDialogMode = null;

    @StringRes
    private int mConfirmBtnStrRes = CONFIRM_BUTTON_RES;

    public SignatureDialogFragmentBuilder() {
    }

    @Override
    public SignatureDialogFragment build(@NonNull Context context) {
        return build(context, SignatureDialogFragment.class);
    }

    public SignatureDialogFragmentBuilder usingTargetPoint(PointF targetPoint) {
        mTargetPoint = targetPoint;
        return this;
    }

    public SignatureDialogFragmentBuilder usingTargetPage(int targetPage) {
        mTargetPage = targetPage;
        return this;
    }

    public SignatureDialogFragmentBuilder usingTargetWidget(Long targetWidget) {
        mTargetWidget = targetWidget;
        return this;
    }

    public SignatureDialogFragmentBuilder usingColor(int color) {
        mColor = color;
        return this;
    }

    public SignatureDialogFragmentBuilder usingSignatureColors(@ColorInt int... signatureColors) {
        mSignatureColors = signatureColors;
        return this;
    }

    public SignatureDialogFragmentBuilder usingStrokeWidth(float strokeWidth) {
        mStrokeWidth = strokeWidth;
        return this;
    }

    public SignatureDialogFragmentBuilder usingShowSavedSignatures(boolean showSavedSignatures) {
        mShowSavedSignatures = showSavedSignatures;
        return this;
    }

    public SignatureDialogFragmentBuilder usingShowSignaturePresets(boolean showSignaturePresets) {
        mShowSignaturePresets = showSignaturePresets;
        return this;
    }

    public SignatureDialogFragmentBuilder usingShowSignatureFromImage(boolean showSignatureFromImage) {
        mShowSignatureFromImage = showSignatureFromImage;
        return this;
    }

    public SignatureDialogFragmentBuilder usingShowTypedSignature(boolean showTypedSignature) {
        mShowTypedSignature = showTypedSignature;
        return this;
    }

    public SignatureDialogFragmentBuilder usingPressureSensitive(boolean pressureSensitive) {
        mPressureSensitive = pressureSensitive;
        return this;
    }

    public SignatureDialogFragmentBuilder usingDefaultKeystore(boolean hasDefaultKeystore) {
        mHasDefaultKeystore = hasDefaultKeystore;
        return this;
    }

    public SignatureDialogFragmentBuilder usingConfirmBtnStrRes(int confirmBtnStrRes) {
        mConfirmBtnStrRes = confirmBtnStrRes;
        return this;
    }

    public SignatureDialogFragmentBuilder usingAnnotStyleProperties(HashMap<Integer, AnnotStyleProperty> annotStyleProperties) {
        this.mAnnotStyleProperties = annotStyleProperties;
        return this;
    }

    public SignatureDialogFragmentBuilder usingDefaultStoreNewSignature(boolean defaultStoreNewSignature) {
        this.mDefaultStoreNewSignature = defaultStoreNewSignature;
        return this;
    }

    public SignatureDialogFragmentBuilder usingPersistStoreSignatureSetting(boolean persistStoreSignatureSetting) {
        this.mPersistStoreSignatureSetting = persistStoreSignatureSetting;
        return this;
    }

    /**
     * Sets the default {@link com.pdftron.pdf.dialog.signature.SignatureDialogFragment.DialogMode} to use when
     * showing the {@link SignatureDialogFragment}.
     *
     * If null is set, then the {@link SignatureDialogFragment} will determine the mode by checking the existing signatures
     * .
     * @param dialogMode to specify the mode of SignatureDialogFragment.
     *
     * @return the {@link SignatureDialogFragmentBuilder}
     */
    public SignatureDialogFragmentBuilder usingDialogMode(@Nullable SignatureDialogFragment.DialogMode dialogMode) {
        this.mDialogMode = dialogMode;
        return this;
    }

    @Override
    public Bundle createBundle(@NonNull Context context) {
        Bundle bundle = new Bundle();
        if (mTargetPoint != null) {
            bundle.putFloat(BUNDLE_TARGET_POINT_X, mTargetPoint.x);
            bundle.putFloat(BUNDLE_TARGET_POINT_Y, mTargetPoint.y);
        }
        bundle.putInt(BUNDLE_TARGET_PAGE, mTargetPage);
        if (mTargetWidget != null) {
            bundle.putLong(BUNDLE_TARGET_WIDGET, mTargetWidget);
        }
        bundle.putInt(BUNDLE_COLOR, mColor);
        bundle.putIntArray(BUNDLE_SIGNATURE_COLORS, mSignatureColors);
        bundle.putFloat(BUNDLE_STROKE_WIDTH, mStrokeWidth);
        bundle.putBoolean(BUNDLE_SHOW_SAVED_SIGNATURES, mShowSavedSignatures);
        bundle.putBoolean(BUNDLE_SHOW_SIGNATURE_PRESETS, mShowSignaturePresets);
        bundle.putBoolean(BUNDLE_SHOW_SIGNATURE_FROM_IMAGE, mShowSignatureFromImage);
        bundle.putBoolean(BUNDLE_SHOW_TYPED_SIGNATURE, mShowTypedSignature);
        bundle.putBoolean(BUNDLE_PRESSURE_SENSITIVE, mPressureSensitive);
        bundle.putBoolean(BUNDLE_HAS_DEFAULT_KEYSTORE, mHasDefaultKeystore);
        bundle.putBoolean(BUNDLE_DEFAULT_STORE_NEW_SIGNATURE, mDefaultStoreNewSignature);
        bundle.putBoolean(BUNDLE_PERSIST_STORE_SIGNATURE, mPersistStoreSignatureSetting);
        bundle.putInt(BUNDLE_DIALOG_MODE, mDialogMode == null ? -1: mDialogMode.value);
        if (mConfirmBtnStrRes != 0) {
            bundle.putInt(BUNDLE_CONFIRM_BUTTON_STRING_RES, mConfirmBtnStrRes);
        }
        bundle.putSerializable(BUNDLE_ANNOT_STYLE_PROPERTY, mAnnotStyleProperties);

        return bundle;
    }

    public void checkArgs(@NonNull Context context) {
        // Nothing to check as all fields are optional
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mTargetPoint, flags);
        dest.writeInt(this.mTargetPage);
        dest.writeValue(this.mTargetWidget);
        dest.writeInt(this.mColor);
        dest.writeFloat(this.mStrokeWidth);
        dest.writeByte(this.mShowSavedSignatures ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mShowSignatureFromImage ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mPressureSensitive ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mDigitalSignature ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mConfirmBtnStrRes);
        dest.writeByte(this.mHasDefaultKeystore ? (byte) 1 : (byte) 0);
        dest.writeInt(this.mDialogMode == null ? -1 : mDialogMode.value);
        dest.writeByte(this.mShowTypedSignature ? (byte) 1 : (byte) 0);
        mSignatureColorsSize = mSignatureColors.length;
        dest.writeInt(mSignatureColorsSize);
        dest.writeIntArray(mSignatureColors);
    }

    protected SignatureDialogFragmentBuilder(Parcel in) {
        this.mTargetPoint = in.readParcelable(PointF.class.getClassLoader());
        this.mTargetPage = in.readInt();
        this.mTargetWidget = (Long) in.readValue(Long.class.getClassLoader());
        this.mColor = in.readInt();
        this.mStrokeWidth = in.readFloat();
        this.mShowSavedSignatures = in.readByte() != 0;
        this.mShowSignatureFromImage = in.readByte() != 0;
        this.mPressureSensitive = in.readByte() != 0;
        this.mDigitalSignature = in.readByte() != 0;
        this.mConfirmBtnStrRes = in.readInt();
        this.mHasDefaultKeystore = in.readByte() != 0;
        SignatureDialogFragment.DialogMode dialogMode = SignatureDialogFragment.DialogMode.fromValue(in.readInt());
        if (dialogMode != null) {
            this.mDialogMode = dialogMode;
        }
        this.mShowTypedSignature = in.readByte() != 0;
        mSignatureColorsSize = in.readInt();
        mSignatureColors = new int[mSignatureColorsSize];
        in.readIntArray(mSignatureColors);
    }

    public static final Creator<SignatureDialogFragmentBuilder> CREATOR = new Creator<SignatureDialogFragmentBuilder>() {
        @Override
        public SignatureDialogFragmentBuilder createFromParcel(Parcel source) {
            return new SignatureDialogFragmentBuilder(source);
        }

        @Override
        public SignatureDialogFragmentBuilder[] newArray(int size) {
            return new SignatureDialogFragmentBuilder[size];
        }
    };
}
