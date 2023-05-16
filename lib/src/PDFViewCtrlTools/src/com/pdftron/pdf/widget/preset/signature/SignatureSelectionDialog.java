package com.pdftron.pdf.widget.preset.signature;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.pdftron.pdf.dialog.base.BaseBottomDialogFragment;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.preset.component.PresetBarTheme;
import com.pdftron.pdf.widget.preset.component.view.PresetSingleButton;

import java.io.File;
import java.util.List;

public class SignatureSelectionDialog extends BaseBottomDialogFragment implements SignatureSelectionView {
    public static final String TAG = SignatureSelectionDialog.class.getName();
    private final FragmentManager mFragmentManager;

    private PresetBarTheme mPresetBarTheme;
    private PresetSingleButton mFirstSignature;
    private PresetSingleButton mSecondSignature;
    private AppCompatImageView mBackButton;
    private MaterialButton mManageButton;
    private MaterialButton mCreateButton;
    private TextView mAdditionalSignature;

    @Nullable
    private ButtonClickListener mButtonClickListener = null;

    public SignatureSelectionDialog(@NonNull FragmentManager fragmentManager) {
        mFragmentManager = fragmentManager;
    }

    @Override
    protected Dialog onCreateDialogImpl(@NonNull Context context) {
        return new Dialog(context, R.style.SignatureSelectionDialogStyle) {
            @Override
            public void onBackPressed() {
                super.onBackPressed();
                SignatureSelectionDialog.this.dismiss();
            }
        };
    }

    @Override
    protected String getFragmentTag() {
        return TAG;
    }

    @Override
    protected int getContentLayoutResource() {
        return R.layout.preset_signature_content;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        mPresetBarTheme = PresetBarTheme.fromContext(root.getContext());

        View contentBackground = root.findViewById(R.id.content_background);
        mFirstSignature = root.findViewById(R.id.first_sig);
        mSecondSignature = root.findViewById(R.id.second_sig);
        mBackButton = root.findViewById(R.id.back_button);
        mManageButton = root.findViewById(R.id.manage_button);
        mCreateButton = root.findViewById(R.id.create_button);
        mAdditionalSignature = root.findViewById(R.id.additional_signature);

        mFirstSignature.setArrowIconVisible(false);
        mFirstSignature.presetIconWithBackground(true);
        mSecondSignature.setArrowIconVisible(false);
        mSecondSignature.presetIconWithBackground(true);

        mFirstSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonClickListener != null) {
                    mButtonClickListener.onFirstSignatureClicked();
                }
            }
        });
        mSecondSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonClickListener != null) {
                    mButtonClickListener.onSecondSignatureClicked();
                }
            }
        });
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });
        mManageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonClickListener != null) {
                    mButtonClickListener.onManageClicked();
                }
            }
        });
        mCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonClickListener != null) {
                    mButtonClickListener.onCreateClicked();
                }
            }
        });
        mAdditionalSignature.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mButtonClickListener != null) {
                    mButtonClickListener.onCreateClicked();
                }
            }
        });

        contentBackground.setBackgroundColor(mPresetBarTheme.backgroundColor);
        setTheme();

        return root;
    }

    private void setTheme() {
        setButtonTheme(mFirstSignature);
        setButtonTheme(mSecondSignature);

        mBackButton.setColorFilter(mPresetBarTheme.iconColor);
        Utils.updateDashedLineColor(mAdditionalSignature, mPresetBarTheme.iconColor);
        mAdditionalSignature.setTextColor(mPresetBarTheme.iconColor);

        mManageButton.setTextColor(mPresetBarTheme.accentColor);
        mManageButton.setStrokeColor(ColorStateList.valueOf(mPresetBarTheme.accentColor));
        mCreateButton.setTextColor(mPresetBarTheme.backgroundColor);
        mCreateButton.setBackgroundColor(mPresetBarTheme.accentColor);
    }

    private void setButtonTheme(PresetSingleButton view) {
        // Setup theme according to preset bar theme
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setIconColor(mPresetBarTheme.iconColor);
        view.setExpandStyleIconColor(mPresetBarTheme.expandIconColor);
        view.setSelectedIconColor(mPresetBarTheme.selectedIconColor);
        view.setDisabledIconColor(mPresetBarTheme.disabledIconColor);
        view.setSelectedBackgroundColor(mPresetBarTheme.selectedBackgroundColor);
        view.setClientBackgroundColor(mPresetBarTheme.backgroundColor);
        view.setEmptyState(R.string.tools_qm_new_signature);
    }

    @Override
    public void setButtonEventListener(@Nullable ButtonClickListener listener) {
        mButtonClickListener = listener;
    }

    public void setAnchorView(@Nullable View anchorView) {
        if (anchorView != null) {
            int[] pos = new int[2];
            anchorView.getLocationInWindow(pos);
            mAnchor = new Rect(pos[0], pos[1], pos[0] + anchorView.getWidth(), pos[1] + anchorView.getHeight());
        } else {
            mAnchor = null;
        }
    }

    @Override
    public void show() {
        show(mFragmentManager);
    }

    @Override
    public void setSignatures(List<String> signatures) {
        // In this bottom sheet we only set the signatures using the first two items
        if (signatures.size() >= 2) {
            String first = signatures.get(0);
            String second = signatures.get(1);
            mFirstSignature.setPresetFile(new File(first));
            mSecondSignature.setPresetFile(new File(second));
            mFirstSignature.setVisibility(View.VISIBLE);
            mSecondSignature.setVisibility(View.VISIBLE);
            mAdditionalSignature.setVisibility(View.INVISIBLE);
        } else if (signatures.size() == 1) {
            String first = signatures.get(0);
            mFirstSignature.setPresetFile(new File(first));
            mFirstSignature.setVisibility(View.VISIBLE);
            mSecondSignature.setVisibility(View.GONE);
            mAdditionalSignature.setVisibility(View.VISIBLE);
        } else {
            mFirstSignature.setVisibility(View.GONE);
            mSecondSignature.setVisibility(View.GONE);
            mAdditionalSignature.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void setViewVisibility(@IdRes int id, int visibility) {
        if (id == R.id.first_sig) {
            mFirstSignature.setVisibility(visibility);
        } else if (id == R.id.second_sig) {
            mSecondSignature.setVisibility(visibility);
        } else if (id == R.id.back_button) {
            mBackButton.setVisibility(visibility);
        } else if (id == R.id.manage_button) {
            mManageButton.setVisibility(visibility);
        } else if (id == R.id.create_button) {
            mCreateButton.setVisibility(visibility);
        } else if (id == R.id.additional_signature) {
            mAdditionalSignature.setVisibility(visibility);
        }
    }

    @Override
    public void close() {
        dismiss();
    }

    public interface ButtonClickListener {
        void onManageClicked();

        void onCreateClicked();

        void onFirstSignatureClicked();

        void onSecondSignatureClicked();
    }
}
