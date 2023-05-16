//------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//------------------------------------------------------------------------------

package com.pdftron.pdf.model;

import android.content.SharedPreferences;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.Element;
import com.pdftron.pdf.ElementReader;
import com.pdftron.pdf.Font;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.tools.FreeTextCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.sdf.Obj;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FreeTextInfo {

    public static boolean setFont(PDFViewCtrl pdfViewCtrl, FreeText freeText, String pdfFontName) throws JSONException, PDFNetException {
        boolean changed = false;
        // Set Font
        // system will automatically set it to the default if font
        // is not embedded.
        if (pdfFontName != null && !pdfFontName.equals("")) {
            String fontDRName = "F0";

            // Create a DR entry for embedding the font
            Obj annotObj = freeText.getSDFObj();
            Obj drDict = annotObj.putDict("DR");

            // Embed the font
            Obj fontDict = drDict.putDict("Font");
            Font font = Font.create(pdfViewCtrl.getDoc(), pdfFontName, freeText.getContents());
            fontDict.put(fontDRName, font.GetSDFObj());
            String fontName = font.getName();

            // Set DA string
            String DA = freeText.getDefaultAppearance();
            int slashPosition = DA.indexOf("/", 0);

            // if DR string contains '/' which it always should.
            if (slashPosition > 0) {
                String beforeSlash = DA.substring(0, slashPosition);
                String afterSlash = DA.substring(slashPosition);
                String afterFont = afterSlash.substring(afterSlash.indexOf(" "));
                String updatedDA = beforeSlash + "/" + pdfFontName + afterFont;

                freeText.setDefaultAppearance(updatedDA);
                freeText.refreshAppearance();
                changed = true;
            }

            // save font name with font if not saved already
            SharedPreferences settings = Tool.getToolPreferences(pdfViewCtrl.getContext());
            String fontInfo = settings.getString(Tool.ANNOTATION_FREE_TEXT_FONTS, "");
            if (!fontInfo.equals("")) {
                JSONObject systemFontObject = new JSONObject(fontInfo);
                JSONArray systemFontArray = systemFontObject.getJSONArray(Tool.ANNOTATION_FREE_TEXT_JSON_FONT);

                for (int i = 0; i < systemFontArray.length(); i++) {
                    JSONObject fontObj = systemFontArray.getJSONObject(i);
                    // if has the same file name as the selected font, save the font name
                    if (fontObj.getString(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_PDFTRON_NAME).equals(pdfFontName)) {
                        fontObj.put(Tool.ANNOTATION_FREE_TEXT_JSON_FONT_NAME, fontName);
                        break;
                    }
                }
                fontInfo = systemFontObject.toString();
            }

            SharedPreferences.Editor editor = settings.edit();
            editor.putString(Tool.ANNOTATION_FREE_TEXT_FONTS, fontInfo);
            editor.putString(Tool.getFontKey(Annot.e_FreeText), pdfFontName);
            editor.apply();
        }

        return changed;
    }

    public static boolean setFontSimple(FreeText freeText, String pdfFontName) throws JSONException, PDFNetException {
        // Set DA string
        String DA = freeText.getDefaultAppearance();
        int slashPosition = DA.indexOf("/");

        // if DR string contains '/' which it always should.
        if (slashPosition > 0) {
            String beforeSlash = DA.substring(0, slashPosition);
            String afterSlash = DA.substring(slashPosition);
            String afterFont = afterSlash.substring(afterSlash.indexOf(" "));
            String updatedDA = beforeSlash + "/" + pdfFontName + afterFont;

            freeText.setDefaultAppearance(updatedDA);
            freeText.refreshAppearance();
            return true;
        }
        return false;
    }

    public static Rect getFreeTextBBoxSimple(FreeText freeText) throws PDFNetException {
        // Get the annotation's content stream and iterate through elements to union
        // their bounding boxes
        Obj contentStream = freeText.getSDFObj().findObj("AP").findObj("N");
        ElementReader er = new ElementReader();
        Rect unionRect = null;
        Element element;

        er.begin(contentStream);
        for (element = er.next(); element != null; element = er.next()) {
            Rect rect = element.getBBox();
            if (rect != null && element.getType() == Element.e_text) {
                if (unionRect == null) {
                    unionRect = rect;
                }
                unionRect = FreeTextCreate.getRectUnion(rect, unionRect);
            }
        }
        er.end();
        er.destroy();

        return unionRect;
    }
}