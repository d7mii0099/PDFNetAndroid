//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Intent;

/**
 * Constants which are used in {@link android.app.Activity#onActivityResult(int, int, Intent)}
 */
public class RequestCode {
    public static final int STORAGE_1                   = 10001;
    public static final int STORAGE_2                   = 10002;
    public static final int PICK_PHOTO_CAM              = 10003;
    public static final int PICK_PDF_FILE               = 10004;
    public static final int PRINT                       = 10005;
    public static final int DOCUMENT_TREE               = 10006;
    public static final int MOVE_FILE                   = 10007;
    public static final int MOVE_FILE_LIST              = 10008;
    public static final int SELECT_BLANK_DOC_FOLDER     = 10009;
    public static final int SELECT_PHOTO_DOC_FOLDER     = 10010;
    public static final int SELECT_FILE                 = 10011;
    public static final int MERGE_FILE_LIST             = 10012;
    public static final int SETTINGS                    = 10013;
    public static final int RECORD_AUDIO                = 10015;
    public static final int SYSTEM_PICKER               = 10016;
    public static final int SELECT_WEBPAGE_PDF_FOLDER   = 10017;
    public static final int DIGITAL_SIGNATURE_KEYSTORE  = 10018;
    public static final int DIGITAL_SIGNATURE_IMAGE     = 10020;
    public static final int CREATE_FILE_IN_SYSTEM       = 10021;
    public static final int PICK_GALLERY                = 10022;
    public static final int PICK_CAM_ONLY               = 10023;
}
