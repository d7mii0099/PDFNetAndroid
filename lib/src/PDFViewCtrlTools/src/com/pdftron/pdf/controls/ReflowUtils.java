package com.pdftron.pdf.controls;

import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.ColorPt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ReflowUtils {
    public static String postProcessColor(@NonNull String html, @NonNull ReflowControl.OnPostProcessColorListener listener) throws PDFNetException {
        int index = 0;

        StringBuilder buffer = new StringBuilder();

        Pattern tagPattern = Pattern.compile("<.*?>");
        Pattern colorPattern = Pattern.compile("color:#[0-9|a-f|A-F]{6}");
        Pattern highlightPattern = Pattern.compile("background-color:#[0-9|a-f|A-F]{6}");
        Pattern paragraphPattern = Pattern.compile("<p.*?>");
        Pattern spanPattern = Pattern.compile("<span.*?>");

        Matcher tagMatcher = tagPattern.matcher(html);
        while (tagMatcher.find()) { // Find each match in turn
            String tag = tagMatcher.group();
            Matcher colorMatcher = colorPattern.matcher(tag);
            while (colorMatcher.find()) { // Find each match in turn
                // output until the matched string
                String subStr = html.substring(index, tagMatcher.start() + colorMatcher.start());
                buffer.append(subStr);
                index += subStr.length();

                if (!highlightPattern.matcher(tag).find() &&
                        (paragraphPattern.matcher(tag).find() || spanPattern.matcher(tag).find())) {
                    // the matched color pattern doesn't belong to background color which
                    // indicates highlighted text, while belongs to either paragraph or span
                    String color = colorMatcher.group();
                    ColorPt inputCP = getColorPt(color.substring(7, 13));
                    if (inputCP == null) {
                        inputCP = new ColorPt();
                    }
                    ColorPt outputCP = listener.getPostProcessedColor(inputCP);
                    if (outputCP == null) {
                        outputCP = inputCP;
                    }
                    // As an alternative solution we can cache outputCP for later
                    // use to ignore frequent calls to the core.
                    String customColor = "color:#" + getHexadecimal(outputCP);
                    buffer.append(customColor);
                    index += customColor.length();
                }
            }
        }

        buffer.append(html.substring(index));
        return buffer.toString();
    }

    private static ColorPt getColorPt(String str) {
        int len = str.length();
        if (len != 6 && len != 8) {
            return null;
        }

        double x, y, z, w = 0;
        if (len == 8) {
            w = Integer.parseInt(str.substring(len - 2, len), 16);
            len -= 2;
        }
        z = Integer.parseInt(str.substring(len - 2, len), 16);
        len -= 2;
        y = Integer.parseInt(str.substring(len - 2, len), 16);
        len -= 2;
        x = Integer.parseInt(str.substring(len - 2, len), 16);

        try {
            len = str.length();
            if (len == 6) {
                return new ColorPt(x / 255., y / 255., z / 255.);
            } else { // if (len == 8)
                return new ColorPt(x / 255., y / 255., z / 255., w / 255.);
            }
        } catch (PDFNetException e) {
            return null;
        }
    }

    private static String getHexadecimal(ColorPt cp) {
        try {
            int x = (int) (cp.get(0) * 255.0);
            int y = (int) (cp.get(1) * 255.0);
            int z = (int) (cp.get(2) * 255.0);
            return String.format("%1$02X%2$02X%3$02X", x, y, z);
        } catch (PDFNetException e) {
            return "";
        }
    }
}
