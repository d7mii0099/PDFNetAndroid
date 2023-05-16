package com.pdftron.pdf.utils;

import android.view.Gravity;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.sdf.Obj;

public class FreeTextAlignmentUtils {

    private static final String DS_KEY = "DS";
    private static final String TEXT_VERTICAL_ALIGNMENT_KEY = "text-vertical-align";
    private static final String TEXT_HORIZONTAL_ALIGNMENT_KEY = "text-align";

    private static final String TOP_ALIGN_VAL = "top";
    private static final String VERTICAL_CENTER_ALIGN_VAL = "center";
    private static final String BOTTOM_ALIGN_VAL = "bottom";

    private static final String LEFT_ALIGN_VAL = "left";
    private static final String HORIZONTAL_CENTER_ALIGN_VAL = "center";
    private static final String RIGHT_ALIGN_VAL = "right";

    public static boolean isLeftAligned(int gravity) {
        return Gravity.LEFT == gravity;
    }

    /**
     * Gets the vertical alignment for the Free Text. This method requires a read lock.
     *
     * @param freeText the Free Text to obtain vertical alignment
     */
    public static int getHorizontalAlignment(@NonNull FreeText freeText) throws PDFNetException {
        Obj annotObj = freeText.getSDFObj();
        Obj defaultStyle = annotObj.findObj(DS_KEY);
        if (defaultStyle != null && defaultStyle.isString()) {
            String defaultStyleStr = defaultStyle.getAsPDFText();
            String[] components = defaultStyleStr.split(";");
            for (String component : components) {
                String[] pair = component.split(":");
                if (pair.length == 2) {
                    String key = pair[0];
                    String val = pair[1];
                    if (key.contains(TEXT_HORIZONTAL_ALIGNMENT_KEY)) {
                        switch (val) {
                            case LEFT_ALIGN_VAL:
                                return Gravity.LEFT;
                            case HORIZONTAL_CENTER_ALIGN_VAL:
                                return Gravity.CENTER_HORIZONTAL;
                            case RIGHT_ALIGN_VAL:
                                return Gravity.RIGHT;
                        }
                    }
                }
            }
        }
        // If we can't find the alignment from default styles, we obtain it from quadding format
        int quaddingFormat = freeText.getQuaddingFormat();
        switch (quaddingFormat) {
            case 0:
                return Gravity.LEFT;
            case 1:
                return Gravity.CENTER_HORIZONTAL;
            case 2:
                return Gravity.RIGHT;
        }
        return Gravity.LEFT;
    }

    /**
     * Sets the horizontal alignment for the Free Text. This method requires a write lock.
     *
     * @param freeText the Free Text to update
     * @param gravity  the Gravity used to determine the alignment direction.
     */
    public static void setHorizontalAlignment(@NonNull FreeText freeText, int gravity) throws PDFNetException {
        Obj annotObj = freeText.getSDFObj();
        Obj defaultStyle = annotObj.findObj(DS_KEY);
        if (defaultStyle != null && defaultStyle.isString()) {
            // Determine the alignment to set in PDFDoc, either "top", "center", or "bottom"
            String newHorizontalAlignment;
            switch (gravity) {
                case Gravity.LEFT:
                    newHorizontalAlignment = LEFT_ALIGN_VAL;
                    freeText.setQuaddingFormat(0);
                    break;
                case Gravity.CENTER_HORIZONTAL:
                    newHorizontalAlignment = HORIZONTAL_CENTER_ALIGN_VAL;
                    freeText.setQuaddingFormat(1);
                    break;
                case Gravity.RIGHT:
                    newHorizontalAlignment = RIGHT_ALIGN_VAL;
                    freeText.setQuaddingFormat(2);
                    break;
                case Gravity.NO_GRAVITY:
                default:
                    boolean isRightToLeft = Utils.isRightToLeftString(freeText.getContents());
                    if (isRightToLeft) {
                        newHorizontalAlignment = RIGHT_ALIGN_VAL;
                        freeText.setQuaddingFormat(2); // right justification
                    } else {
                        newHorizontalAlignment = LEFT_ALIGN_VAL;
                        freeText.setQuaddingFormat(0); // left justification
                    }
                    break;
            }

            // Then iterate through key value pairs in default style and either replace
            // existing vertical alignment or add a new one
            String defaultStyleStr = defaultStyle.getAsPDFText();
            String[] components = defaultStyleStr.split(";");
            StringBuilder newDefaultStyle = new StringBuilder();
            boolean vertAlignExists = false;
            for (int i = 0; i < components.length; i++) {
                String component = components[i];
                String[] pair = component.split(":");
                if (pair.length == 2) {
                    String key = pair[0];
                    String val = pair[1];
                    newDefaultStyle.append(key).append(":");
                    // Update existing key value pair
                    if (key.contains(TEXT_HORIZONTAL_ALIGNMENT_KEY)) {
                        newDefaultStyle.append(newHorizontalAlignment);
                        vertAlignExists = true;
                    } else {
                        newDefaultStyle.append(val);
                    }
                    if (i < (components.length - 1)) {
                        newDefaultStyle.append(";");
                    }
                }
            }
            // If no key exists, then add a new one
            if (!vertAlignExists) {
                newDefaultStyle.append(";").append(TEXT_HORIZONTAL_ALIGNMENT_KEY).append(":").append(newHorizontalAlignment);
            }
            annotObj.putText(DS_KEY, newDefaultStyle.toString());
        }
    }

    /**
     * Gets the vertical alignment for the Free Text. This method requires a read lock.
     *
     * @param freeText the Free Text to obtain vertical alignment
     */
    public static int getVerticalAlignment(@NonNull FreeText freeText) throws PDFNetException {
        Obj annotObj = freeText.getSDFObj();
        Obj defaultStyle = annotObj.findObj(DS_KEY);
        if (defaultStyle != null && defaultStyle.isString()) {
            String defaultStyleStr = defaultStyle.getAsPDFText();
            String[] components = defaultStyleStr.split(";");
            for (String component : components) {
                String[] pair = component.split(":");
                if (pair.length == 2) {
                    String key = pair[0];
                    String val = pair[1];
                    if (key.contains(TEXT_VERTICAL_ALIGNMENT_KEY)) {
                        switch (val) {
                            case TOP_ALIGN_VAL:
                                return Gravity.TOP;
                            case VERTICAL_CENTER_ALIGN_VAL:
                                return Gravity.CENTER_VERTICAL;
                            case BOTTOM_ALIGN_VAL:
                                return Gravity.BOTTOM;
                        }
                    }
                }
            }
        }
        return Gravity.TOP;
    }

    /**
     * Sets the vertical alignment for the Free Text. This method requires a write lock.
     *
     * @param freeText the Free Text to update
     * @param gravity  the Gravity used to determine the alignment direction.
     */
    public static void setVerticalAlignment(@NonNull FreeText freeText, int gravity) throws PDFNetException {
        Obj annotObj = freeText.getSDFObj();
        Obj defaultStyle = annotObj.findObj(DS_KEY);
        if (defaultStyle != null && defaultStyle.isString()) {
            // Determine the alignment to set in PDFDoc, either "top", "center", or "bottom"
            String newVerticalAlignment;
            switch (gravity) {
                default:
                case Gravity.TOP:
                    newVerticalAlignment = TOP_ALIGN_VAL;
                    break;
                case Gravity.CENTER_VERTICAL:
                    newVerticalAlignment = VERTICAL_CENTER_ALIGN_VAL;
                    break;
                case Gravity.BOTTOM:
                    newVerticalAlignment = BOTTOM_ALIGN_VAL;
                    break;
            }

            // Then iterate through key value pairs in default style and either replace
            // existing vertical alignment or add a new one
            String defaultStyleStr = defaultStyle.getAsPDFText();
            String[] components = defaultStyleStr.split(";");
            StringBuilder newDefaultStyle = new StringBuilder();
            boolean vertAlignExists = false;
            for (int i = 0; i < components.length; i++) {
                String component = components[i];
                String[] pair = component.split(":");
                if (pair.length == 2) {
                    String key = pair[0];
                    String val = pair[1];
                    newDefaultStyle.append(key).append(":");
                    // Update existing key value pair
                    if (key.contains(TEXT_VERTICAL_ALIGNMENT_KEY)) {
                        newDefaultStyle.append(newVerticalAlignment);
                        vertAlignExists = true;
                    } else {
                        newDefaultStyle.append(val);
                    }
                    if (i < (components.length - 1)) {
                        newDefaultStyle.append(";");
                    }
                }
            }
            // If no key exists, then add a new one
            if (!vertAlignExists) {
                newDefaultStyle.append(";").append(TEXT_VERTICAL_ALIGNMENT_KEY).append(":").append(newVerticalAlignment);
            }
            annotObj.putText(DS_KEY, newDefaultStyle.toString());
        }
    }
}
