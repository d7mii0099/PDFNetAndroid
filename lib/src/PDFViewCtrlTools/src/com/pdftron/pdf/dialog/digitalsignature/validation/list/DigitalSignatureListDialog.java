package com.pdftron.pdf.dialog.digitalsignature.validation.list;

import android.content.Context;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.Group;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.DigitalSignatureField;
import com.pdftron.pdf.DigitalSignatureFieldIterator;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.dialog.digitalsignature.validation.DigitalSignatureUtils;
import com.pdftron.pdf.dialog.digitalsignature.validation.properties.DigitalSignaturePropertiesDialog;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class DigitalSignatureListDialog extends DialogFragment {

    private Toolbar mToolbar;
    @Nullable
    private PDFViewCtrl mPDFViewCtrl;
    @Nullable
    private DigitalSignatureInfoListAdapter mAdapter;

    public static DigitalSignatureListDialog newInstance() {
        return new DigitalSignatureListDialog();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_digital_signature_list_dialog, container, false);
        RecyclerView digSigInfoList = root.findViewById(R.id.dig_sig_info_list);
        digSigInfoList.setLayoutManager(new LinearLayoutManager(root.getContext()));
        mAdapter = new DigitalSignatureInfoListAdapter();
        digSigInfoList.setAdapter(mAdapter);
        setDigitalSignatureInformation();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mToolbar = view.findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.dialog_digital_signature_list);

        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }

    public void setPDFViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        mPDFViewCtrl = pdfViewCtrl;
        setDigitalSignatureInformation();
    }

    private void setDigitalSignatureInformation() {
        if (mAdapter == null || mPDFViewCtrl == null) {
            return;
        }
        PDFDoc doc = mPDFViewCtrl.getDoc();
        try {
            DigitalSignatureFieldIterator digitalSignatureFieldIterator = doc.getDigitalSignatureFieldIterator();
            while (digitalSignatureFieldIterator.hasNext()) {
                DigitalSignatureField digSigField = digitalSignatureFieldIterator.next();
                DigitalSignatureInfoBase digSigInfo;
                if (digSigField.hasCryptographicSignature()) {
                    digSigInfo = DigitalSignatureUtils.getDigitalSignatureInformation(mPDFViewCtrl.getContext(), digSigField, mPDFViewCtrl);
                } else {
                    digSigInfo = new UnsignedDigitalSignatureInfo(Long.toString(digSigField.getSDFObj().getObjNum()));
                }

                mAdapter.addDigitalSignatureInfo(digSigInfo);
            }
        } catch (PDFNetException e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }
    }

    private class DigitalSignatureInfoListAdapter extends RecyclerView.Adapter<DigitalSignatureInfoListViewHolder> {

        private final List<DigitalSignatureInfoBase> digSigInfoList = new ArrayList<>();

        @NonNull
        @Override
        public DigitalSignatureInfoListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View root = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_digital_signature_info, parent, false);
            return new DigitalSignatureInfoListViewHolder(root);
        }

        @Override
        public void onBindViewHolder(@NonNull final DigitalSignatureInfoListViewHolder holder, final int position) {
            Context context = holder.itemView.getContext();
            final DigitalSignatureInfoBase digSigInfoBase = digSigInfoList.get(position);
            if (digSigInfoBase instanceof DigitalSignatureInfo) {
                final DigitalSignatureInfo digSigInfo = (DigitalSignatureInfo) digSigInfoBase;

                // Group visibility need to set first
                holder.headerGroup.setVisibility(View.VISIBLE);
                holder.detailsGroup.setVisibility(View.VISIBLE);

                holder.digSigUnsigned.setVisibility(View.GONE);

                switch (digSigInfo.badge) {
                    case ERROR:
                        holder.badge.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_digital_signature_error));
                        break;
                    case VALID:
                        holder.badge.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_digital_signature_valid));
                        break;
                    case WARNING:
                        holder.badge.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_digital_signature_warning));
                }
                holder.header.setText(digSigInfo.header);
                holder.headerClickArea.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        digSigInfo.expanded = !digSigInfo.expanded;
                        notifyItemChanged(position);
                    }
                });

                if (digSigInfo.expanded) {
                    holder.detailsGroup.setVisibility(View.VISIBLE);
                    holder.headerExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_down_white_24dp));

                    holder.verificationMessage.setText(digSigInfo.verificationStatus);
                    holder.permissionMessage.setText(digSigInfo.permissionStatus);
                    if (digSigInfo.disallowedChanges != null) {
                        holder.disallowedChanges.setVisibility(View.VISIBLE);
                        holder.disallowedChanges.setText(digSigInfo.disallowedChanges);
                    } else {
                        holder.disallowedChanges.setVisibility(View.GONE);
                    }
                    holder.trustResult.setText(digSigInfo.trustVerificationResult);
                    if (digSigInfo.trustVerificationResultDateTime != null) {
                        holder.trustResultDate.setVisibility(View.VISIBLE);
                        holder.trustResultDate.setText(digSigInfo.trustVerificationResultDateTime);
                    } else {
                        holder.trustResultDate.setVisibility(View.GONE);
                    }
                    if (digSigInfo.trustVerificationDateTimeMsg != null) {
                        holder.verificationTimeDetails.setVisibility(View.VISIBLE);
                        holder.verificationTimeDetails.setText(digSigInfo.trustVerificationDateTimeMsg);
                    } else {
                        holder.verificationTimeDetails.setVisibility(View.GONE);
                    }

                    holder.signatureProperties.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            DigitalSignaturePropertiesDialog dialog = DigitalSignaturePropertiesDialog.newInstance();
                            dialog.setStyle(DialogFragment.STYLE_NO_TITLE, getTheme());
                            dialog.setDigitalSignatureProperties(digSigInfo.digitalSignatureProperties);
                            FragmentManager fragmentManager = getFragmentManager();
                            if (fragmentManager != null) {
                                dialog.show(fragmentManager, "digital_sig_properties_dialog");
                            }
                        }
                    });

                    holder.additionalDetailsHeaderClickArea.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            digSigInfo.additionalDetailsExpanded = !digSigInfo.additionalDetailsExpanded;
                            notifyItemChanged(position);
                        }
                    });

                    if (digSigInfo.additionDetailsAvailable) {
                        holder.additionalDetailsHeaderGroup.setVisibility(View.VISIBLE);
                        if (digSigInfo.additionalDetailsExpanded) {
                            holder.additionalDetailsGroup.setVisibility(View.VISIBLE);
                            holder.additionalDetailsExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_arrow_down_white_24dp));

                            holder.contact.setText(digSigInfo.contactInfo);
                            holder.location.setText(digSigInfo.location);
                            holder.reason.setText(digSigInfo.reason);
                            holder.signingTime.setText(digSigInfo.signingTime);
                        } else {
                            holder.additionalDetailsGroup.setVisibility(View.GONE);
                            holder.additionalDetailsExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_white_24dp));
                        }
                    } else {
                        holder.additionalDetailsHeaderGroup.setVisibility(View.GONE);
                        holder.additionalDetailsGroup.setVisibility(View.GONE);
                    }
                } else {
                    holder.headerExpand.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_white_24dp));

                    holder.detailsGroup.setVisibility(View.GONE);
                    holder.additionalDetailsHeaderGroup.setVisibility(View.GONE);
                    holder.additionalDetailsGroup.setVisibility(View.GONE);

                    holder.disallowedChanges.setVisibility(View.GONE); // need to set manually, since it's not part of a group
                    holder.trustResultDate.setVisibility(View.GONE); // need to set manually, since it's not part of a group
                    holder.verificationTimeDetails.setVisibility(View.GONE); // need to set manually, since it's not part of a group
                }
            } else if (digSigInfoBase instanceof UnsignedDigitalSignatureInfo) {
                final UnsignedDigitalSignatureInfo digSigInfo = (UnsignedDigitalSignatureInfo) digSigInfoBase;

                // Group visibility need to set first
                holder.headerGroup.setVisibility(View.GONE);
                holder.detailsGroup.setVisibility(View.GONE);
                holder.additionalDetailsHeaderGroup.setVisibility(View.GONE);
                holder.additionalDetailsGroup.setVisibility(View.GONE);
                holder.digSigUnsigned.setVisibility(View.VISIBLE);

                holder.disallowedChanges.setVisibility(View.GONE); // need to set manually, since it's not part of a group
                holder.trustResultDate.setVisibility(View.GONE); // need to set manually, since it's not part of a group
                holder.verificationTimeDetails.setVisibility(View.GONE); // need to set manually, since it's not part of a group

                holder.digSigUnsigned.setText(String.format(context.getString(R.string.dialog_digital_signature_unsigned), digSigInfo.objNumber));
            }
        }

        public void addDigitalSignatureInfo(@NonNull DigitalSignatureInfoBase digitalSignatureInfo) {
            digSigInfoList.add(digitalSignatureInfo);
            notifyDataSetChanged();
        }

        @Override
        public int getItemCount() {
            return digSigInfoList.size();
        }
    }

    private static class DigitalSignatureInfoListViewHolder extends RecyclerView.ViewHolder {

        final Group headerGroup;
        final Group detailsGroup;
        final Group additionalDetailsHeaderGroup;
        final Group additionalDetailsGroup;

        final TextView digSigUnsigned;

        final View headerClickArea;
        final ImageView headerExpand;
        final ImageView badge;
        final TextView header;

        final TextView verificationMessage;
        final TextView permissionMessage;
        final TextView disallowedChanges;
        final TextView trustResult;
        final TextView trustResultDate;
        final TextView verificationTimeDetails;
        final TextView signatureProperties;

        final View additionalDetailsHeaderClickArea;
        final ImageView additionalDetailsExpand;
        final TextView contact;
        final TextView location;
        final TextView reason;
        final TextView signingTime;

        DigitalSignatureInfoListViewHolder(@NonNull View itemView) {
            super(itemView);
            headerGroup = itemView.findViewById(R.id.header_group);
            detailsGroup = itemView.findViewById(R.id.details_group);
            additionalDetailsHeaderGroup = itemView.findViewById(R.id.additional_details_header_group);
            additionalDetailsGroup = itemView.findViewById(R.id.additional_details_group);

            digSigUnsigned = itemView.findViewById(R.id.dig_sig_unsigned);

            headerClickArea = itemView.findViewById(R.id.header_click_area);
            headerExpand = itemView.findViewById(R.id.header_expand);
            badge = itemView.findViewById(R.id.badge);
            header = itemView.findViewById(R.id.header);

            verificationMessage = itemView.findViewById(R.id.sig_verification);
            permissionMessage = itemView.findViewById(R.id.doc_permission_message);
            disallowedChanges = itemView.findViewById(R.id.disallowed_changes);
            trustResult = itemView.findViewById(R.id.trust_result);
            trustResultDate = itemView.findViewById(R.id.trust_result_date);
            verificationTimeDetails = itemView.findViewById(R.id.verification_time_details);
            signatureProperties = itemView.findViewById(R.id.signature_properties);

            additionalDetailsHeaderClickArea = itemView.findViewById(R.id.additional_details_header_click_area);
            additionalDetailsExpand = itemView.findViewById(R.id.additional_details_expand);
            contact = itemView.findViewById(R.id.contact_info);
            location = itemView.findViewById(R.id.location);
            reason = itemView.findViewById(R.id.reason);
            signingTime = itemView.findViewById(R.id.signing_time);

            // Set theme colors
            Utils.applySecondaryTextTintToButton(headerExpand);
            Utils.applySecondaryTextTintToButton(additionalDetailsExpand);
            signatureProperties.setPaintFlags(signatureProperties.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
        }
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public enum DigitalSignatureBadge {
        VALID, WARNING, ERROR
    }
}
