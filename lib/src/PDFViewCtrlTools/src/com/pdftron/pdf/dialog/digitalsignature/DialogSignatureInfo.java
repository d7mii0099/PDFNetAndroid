package com.pdftron.pdf.dialog.digitalsignature;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.Utils;

public class DialogSignatureInfo extends AlertDialog {

    private SignatureInfoView mPermission;
    private SignatureInfoView mName;
    private SignatureInfoView mLocation;
    private SignatureInfoView mContactInfo;
    private SignatureInfoView mReason;
    private SignatureInfoView mSigningTime;

    public DialogSignatureInfo(Context context) {
        super(context);

        View view = LayoutInflater.from(getContext()).inflate(R.layout.tools_dialog_signatureinfo, null);
        mPermission = view.findViewById(R.id.sig_info_permission);
        mName = view.findViewById(R.id.sig_info_name);
        mLocation = view.findViewById(R.id.sig_info_location);
        mContactInfo = view.findViewById(R.id.sig_info_contact);
        mReason = view.findViewById(R.id.sig_info_reason);
        mSigningTime = view.findViewById(R.id.sig_info_signing_time);

        setView(view);

        setTitle(context.getString(R.string.tools_digitalsignature_signature_info));

        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.ok), (DialogInterface.OnClickListener) null);
    }

    public void setLocation(@Nullable String text) {
        mLocation.setDetails(text);
        mLocation.setVisibility(Utils.isNullOrEmpty(text) ? View.GONE : View.VISIBLE);
    }

    public void setReason(@Nullable String reason) {
        mReason.setDetails(reason);
        mReason.setVisibility(Utils.isNullOrEmpty(reason) ? View.GONE : View.VISIBLE);
    }

    public void setName(@Nullable String name) {
        mName.setDetails(name);
        mName.setVisibility(Utils.isNullOrEmpty(name) ? View.GONE : View.VISIBLE);
    }

    public void setContactInfo(@Nullable String contactInfo) {
        mContactInfo.setDetails(contactInfo);
        mContactInfo.setVisibility(Utils.isNullOrEmpty(contactInfo) ? View.GONE : View.VISIBLE);
    }

    public void setSigningTime(@Nullable String signingTimeString) {
        mSigningTime.setDetails(signingTimeString);
        mSigningTime.setVisibility(Utils.isNullOrEmpty(signingTimeString) ? View.GONE : View.VISIBLE);
    }

    public void setDocumentPermission(@Nullable String documentPermissionMsg) {
        mPermission.setDetails(documentPermissionMsg);
        mPermission.setVisibility(Utils.isNullOrEmpty(documentPermissionMsg) ? View.GONE : View.VISIBLE);
    }
}
