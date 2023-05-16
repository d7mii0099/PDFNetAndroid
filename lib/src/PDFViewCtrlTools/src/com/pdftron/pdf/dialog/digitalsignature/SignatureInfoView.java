package com.pdftron.pdf.dialog.digitalsignature;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.pdftron.pdf.tools.R;

/**
 * ViewGroup that captures digital signature information of a single type/label.
 */
public class SignatureInfoView extends FrameLayout {

    private TextView mLabel;
    private TextView mDetails;

    public SignatureInfoView(Context context) {
        this(context, null);
    }

    public SignatureInfoView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SignatureInfoView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        LayoutInflater.from(context).inflate(R.layout.view_signature_info, this, true);
        mLabel = findViewById(R.id.tools_dialog_signatureinfo_label);
        mDetails = findViewById(R.id.tools_dialog_signatureinfo_details);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SignatureInfoView, defStyleAttr, R.style.SignatureInfoViewDefault);
        try {
            // label title
            String label = a.getString(R.styleable.SignatureInfoView_info_label);
            mLabel.setText(label);
        } finally {
            a.recycle();
        }
    }

    /**
     * Updates the label text in the signature info view.
     *
     * @param label label to update the signature info view.
     */
    public void setLabel(@Nullable String label) {
        mLabel.setText(label);
    }

    /**
     * Updates the details text in the signature info view.
     *
     * @param details label to update the signature info view.
     */
    public void setDetails(@Nullable String details) {
        mDetails.setText(details);
    }
}
