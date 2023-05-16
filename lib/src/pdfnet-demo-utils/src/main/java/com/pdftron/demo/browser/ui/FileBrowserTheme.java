package com.pdftron.demo.browser.ui;

import android.content.Context;
import android.content.res.TypedArray;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FileBrowserTheme {
    @ColorInt
    public final int headerBackgroundColor;
    @ColorInt
    public final int headerTextColor;
    @ColorInt
    public final int headerChevronColor;
    @ColorInt
    public final int emptyTextBackground;
    @ColorInt
    public final int contentBodyTextColor;
    @ColorInt
    public final int contentSecondaryTextColor;
    @ColorInt
    public final int iconColor;
    @ColorInt
    public final int itemDividerColor;

    FileBrowserTheme(int headerBackgroundColor, int headerTextColor, int headerChevronColor,
            int emptyTextBackground, int contentBodyTextColor, int contentSecondaryTextColor,
            int iconColor, int itemDividerColor) {
        this.headerBackgroundColor = headerBackgroundColor;
        this.headerTextColor = headerTextColor;
        this.headerChevronColor = headerChevronColor;
        this.emptyTextBackground = emptyTextBackground;
        this.contentBodyTextColor = contentBodyTextColor;
        this.contentSecondaryTextColor = contentSecondaryTextColor;
        this.iconColor = iconColor;
        this.itemDividerColor = itemDividerColor;
    }

    public static FileBrowserTheme fromContext(@NonNull Context context) {

        final TypedArray a = context.obtainStyledAttributes(
                null, R.styleable.AllDocumentFileBrowserTheme, R.attr.pt_all_document_browser_style, R.style.PTAllDocumentFileBrowserTheme);
        int headerBackgroundColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_headerBackgroundColor, context.getResources().getColor(R.color.recyclerview_header_bg));
        int headerTextColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_headerTextColor, context.getResources().getColor(android.R.color.tertiary_text_light));
        int headerChevronColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_headerChevronColor, context.getResources().getColor(android.R.color.tertiary_text_light));
        int emptyTextBackground = a.getColor(R.styleable.AllDocumentFileBrowserTheme_emptyTextBackground, context.getResources().getColor(R.color.empty_textBackground));
        int contentBodyTextColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_contentBodyTextColor, Utils.getPrimaryTextColor(context));
        int contentSecondaryTextColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_contentSecondaryTextColor, Utils.getSecondaryTextColor(context));
        int iconColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_iconColor, Utils.getPrimaryTextColor(context));
        int itemDividerColor = a.getColor(R.styleable.AllDocumentFileBrowserTheme_itemDividerColor, context.getResources().getColor(R.color.browser_divider_color));
        a.recycle();

        return new FileBrowserTheme(
                headerBackgroundColor,
                headerTextColor,
                headerChevronColor,
                emptyTextBackground,
                contentBodyTextColor,
                contentSecondaryTextColor,
                iconColor,
                itemDividerColor
        );
    }
}
