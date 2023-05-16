//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.util.Patterns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Action;
import com.pdftron.pdf.ActionParameter;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Destination;
import com.pdftron.pdf.FileSpec;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.annots.Link;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.sdf.Obj;

/**
 * A utility class for annotation actions.
 */
public class ActionUtils {

    /**
     * A intercept callback for {@link #onInterceptExecuteAction(ActionParameter, PDFViewCtrl)}
     */
    public interface ActionInterceptCallback {
        /**
         * Called when {@link #onInterceptExecuteAction(ActionParameter, PDFViewCtrl)} is called
         *
         * @param actionParam The action parameter
         * @param pdfViewCtrl The PDFViewCtrl
         * @return true then intercept {@link #onInterceptExecuteAction(ActionParameter, PDFViewCtrl)}, false otherwise
         */
        boolean onInterceptExecuteAction(ActionParameter actionParam, PDFViewCtrl pdfViewCtrl);
    }

    private static class LazzyHolder {
        static final ActionUtils INSTANCE = new ActionUtils();
    }

    public static ActionUtils getInstance() {
        return LazzyHolder.INSTANCE;
    }

    private ActionInterceptCallback mActionCallback;

    /**
     * Sets {@link Action} intercept callback
     *
     * @param callback ActionInterceptCallback
     */
    public void setActionInterceptCallback(ActionInterceptCallback callback) {
        mActionCallback = callback;
    }

    /**
     * Gets {@link} intercept callback
     *
     * @return action intercept callback
     */
    public ActionInterceptCallback getActionInterceptCallback() {
        return mActionCallback;
    }

    /**
     * Executes an action on the PDF.
     * <p>
     * <div class="warning">
     * The PDF doc should have been locked when call this method.
     * In addition, ToolManager's raise annotation should be handled in the caller function.
     * </div>
     *
     * @param actionParam The action parameter
     * @param pdfViewCtrl The PDFViewCtrl
     */
    public void executeAction(ActionParameter actionParam, final PDFViewCtrl pdfViewCtrl) {
        try {
            if (getActionInterceptCallback() != null && getActionInterceptCallback().onInterceptExecuteAction(actionParam, pdfViewCtrl)) {
                return;
            }
            Action action = actionParam.getAction();
            int action_type = action.getType();
            if (action_type == Action.e_URI) {
                Obj o = action.getSDFObj();
                o = o.findObj("URI");
                if (o != null) {
                    String uri = o.getAsPDFText();
                    if (uri.startsWith("mailto:") || Patterns.EMAIL_ADDRESS.matcher(uri).matches()) {
                        // this is a mail intent
                        if (uri.startsWith("mailto:")) {
                            uri = uri.substring(7);
                        }
                        launchEmailIntent(pdfViewCtrl.getContext(), uri);
                    } else if (uri.startsWith("tel:") || Patterns.PHONE.matcher(uri).matches()) {
                        // this is a phone intent
                        if (uri.startsWith("tel:")) {
                            uri = uri.substring(4);
                        }
                        launchPhoneIntent(pdfViewCtrl.getContext(), uri);
                    } else {
                        // ACTION_VIEW needs the address to have http or https
                        if (!uri.startsWith("https://") && !uri.startsWith("http://")) {
                            uri = "http://" + uri;
                        }
                        launchWebPageIntent(pdfViewCtrl.getContext(), uri);
                    }
                }
            } else {
                // check if we can open a linked pdf
                boolean handled = false;
                if (action_type == Action.e_GoToR) {
                    Obj o = action.getSDFObj();
                    o = o.findObj("F");
                    if (o != null) {
                        FileSpec fileSpec = new FileSpec(o);
                        if (fileSpec.isValid()) {
                            String filePath = fileSpec.getFilePath();
                            if (!Utils.isNullOrEmpty(filePath)) {
                                ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
                                int pageNumber = getPageNumberFromAction(action);
                                handled = toolManager.onNewFileCreated(filePath, pageNumber);
                            }
                        }
                    }
                } else if (action_type == Action.e_Named) {
                    Obj o = action.getSDFObj();
                    o = o.findObj("N");
                    if (o != null) {
                        String name = o.getName();
                        if ("GoBack".equals(name)) {
                            ToolManager toolManager = (ToolManager) pdfViewCtrl.getToolManager();
                            toolManager.onGoBackSelected();
                        }
                    }
                }
                if (!handled) {
                    pdfViewCtrl.executeAction(actionParam);
                }
            }
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
    }

    private static int getPageNumberFromAction(@NonNull Action action) throws PDFNetException {
        Destination dest = action.getDest();
        int pageNum = 1;
        if (dest != null && dest.isValid()) {
            Obj destObj = dest.getSDFObj();
            if (destObj != null && destObj.isArray()) {
                if (destObj.size() > 0) {
                    Obj pageNumber = destObj.getAt(0);
                    if (pageNumber != null && pageNumber.isNumber()) {
                        pageNum = (int) pageNumber.getNumber() + 1;
                    }
                }
            }
        }

        return pageNum;
    }

    public static boolean isActionGoBack(@Nullable Annot annot) throws PDFNetException {
        if (annot != null && annot.isValid() && annot.getType() == Annot.e_Link) {
            Link link = new Link(annot);
            Action action = link.getAction();
            if (action != null && action.isValid()) {
                int action_type = action.getType();
                if (action_type == Action.e_Named) {
                    Obj o = action.getSDFObj();
                    o = o.findObj("N");
                    if (o != null) {
                        String name = o.getName();
                        if ("GoBack".equals(name)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static void launchEmailIntent(@NonNull final Context context, final String uri) {
        // Ask the user if he/she really want to open the link in an external app
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.tools_dialog_open_link_action_title)
                .setMessage(String.format(context.getResources().getString(R.string.tools_dialog_open_email_action_message), uri))
                .setIcon(null)
                .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", uri, null));
                        context.startActivity(Intent.createChooser(i, context.getResources().getString(R.string.tools_misc_sendemail)));
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    public static void launchPhoneIntent(@NonNull final Context context, final String uri) {
        // Ask the user if he/she really want to open the link in an external app
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.tools_dialog_open_link_action_title)
                .setMessage(String.format(context.getResources().getString(R.string.tools_dialog_open_tel_action_message), uri))
                .setIcon(null)
                .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", uri, null));
                        context.startActivity(Intent.createChooser(i, context.getResources().getString(R.string.tools_misc_dialphone)));
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }

    public static void launchWebPageIntent(@NonNull final Context context, final String uri) {
        // Ask the user if he/she really want to open the link in an external browser
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.tools_dialog_open_web_page_title)
                .setMessage(String.format(context.getResources().getString(R.string.tools_dialog_open_web_page_message), uri))
                .setIcon(null)
                .setPositiveButton(R.string.open, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                        context.startActivity(Intent.createChooser(i, context.getResources().getString(R.string.tools_misc_openwith)));
                    }
                })
                .setNegativeButton(R.string.cancel, null);
        builder.create().show();
    }
}
