package com.pdftron.richeditor;

import android.content.Context;
import android.graphics.Color;
import android.text.Layout;
import android.util.AttributeSet;

import com.pdftron.pdf.model.FontResource;
import com.pdftron.richeditor.helper.UndoRedoHelper;
import com.pdftron.richeditor.styles.ARE_Alignment;
import com.pdftron.richeditor.styles.ARE_BackgroundColor;
import com.pdftron.richeditor.styles.ARE_Bold;
import com.pdftron.richeditor.styles.ARE_FontColor;
import com.pdftron.richeditor.styles.ARE_FontSize;
import com.pdftron.richeditor.styles.ARE_Fontface;
import com.pdftron.richeditor.styles.ARE_IndentLeft;
import com.pdftron.richeditor.styles.ARE_IndentRight;
import com.pdftron.richeditor.styles.ARE_Italic;
import com.pdftron.richeditor.styles.ARE_ListBullet;
import com.pdftron.richeditor.styles.ARE_ListNumber;
import com.pdftron.richeditor.styles.ARE_Quote;
import com.pdftron.richeditor.styles.ARE_Strikethrough;
import com.pdftron.richeditor.styles.ARE_Subscript;
import com.pdftron.richeditor.styles.ARE_Superscript;
import com.pdftron.richeditor.styles.ARE_Underline;
import com.pdftron.richeditor.styles.IARE_Style;

import java.util.ArrayList;

public class PTAREditText extends AREditText {

    protected UndoRedoHelper mUndoRedoHelper;

    /**
     * Font-size Style
     */
    protected ARE_FontSize mFontSizeStyle;

    /**
     * Font-face Style
     */
    protected ARE_Fontface mFontfaceStyle;

    /**
     * Bold Style
     */
    protected ARE_Bold mBoldStyle;

    /**
     * Italic Style
     */
    protected ARE_Italic mItalicStyle;

    /**
     * Underline Style
     */
    protected ARE_Underline mUnderlineStyle;

    /**
     * Strikethrough Style
     */
    protected ARE_Strikethrough mStrikethroughStyle;

    /**
     * Subscript Style
     */
    protected ARE_Subscript mSubscriptStyle;

    /**
     * Superscript Style
     */
    protected ARE_Superscript mSuperscriptStyle;

    /**
     * Quote style
     */
    protected ARE_Quote mQuoteStyle;

    /**
     * Font color Style
     */
    protected ARE_FontColor mFontColorStyle;

    /**
     * Background color Style
     */
    protected ARE_BackgroundColor mBackgroundColorStyle;

    /**
     * List number Style
     */
    protected ARE_ListNumber mListNumberStyle;

    /**
     * List bullet Style
     */
    protected ARE_ListBullet mListBulletStyle;

    /**
     * Indent to right Style.
     */
    protected ARE_IndentRight mIndentRightStyle;

    /**
     * Indent to left Style.
     */
    protected ARE_IndentLeft mIndentLeftStyle;

    /**
     * Align left.
     */
    protected ARE_Alignment mAlignLeft;

    /**
     * Align center.
     */
    protected ARE_Alignment mAlignCenter;

    /**
     * Align right.
     */
    protected ARE_Alignment mAlignRight;

    public PTAREditText(Context context) {
        this(context, null);
    }

    public PTAREditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PTAREditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mUndoRedoHelper = new UndoRedoHelper(this);

        this.mFontSizeStyle = new ARE_FontSize(this);
        this.mFontfaceStyle = new ARE_Fontface(this);
        this.mBoldStyle = new ARE_Bold(this);
        this.mItalicStyle = new ARE_Italic(this);
        this.mUnderlineStyle = new ARE_Underline(this);
        this.mStrikethroughStyle = new ARE_Strikethrough(this);
        this.mSubscriptStyle = new ARE_Subscript(this);
        this.mSuperscriptStyle = new ARE_Superscript(this);
        this.mQuoteStyle = new ARE_Quote(this);
        this.mFontColorStyle = new ARE_FontColor(this);
        this.mBackgroundColorStyle = new ARE_BackgroundColor(this, Color.TRANSPARENT);
        this.mListNumberStyle = new ARE_ListNumber(this);
        this.mListBulletStyle = new ARE_ListBullet(this);
        this.mIndentRightStyle = new ARE_IndentRight(this);
        this.mIndentLeftStyle = new ARE_IndentLeft(this);
        this.mAlignLeft = new ARE_Alignment(this, Layout.Alignment.ALIGN_NORMAL);
        this.mAlignCenter = new ARE_Alignment(this, Layout.Alignment.ALIGN_CENTER);
        this.mAlignRight = new ARE_Alignment(this, Layout.Alignment.ALIGN_OPPOSITE);

        ArrayList<IARE_Style> styles = new ArrayList<>();
        styles.add(mFontSizeStyle);
        styles.add(mFontfaceStyle);
        styles.add(mBoldStyle);
        styles.add(mItalicStyle);
        styles.add(mUnderlineStyle);
        styles.add(mStrikethroughStyle);
        styles.add(mSubscriptStyle);
        styles.add(mSuperscriptStyle);
        styles.add(mQuoteStyle);
        styles.add(mFontColorStyle);
//        styles.add(mBackgroundColorStyle);
        styles.add(mListNumberStyle);
        styles.add(mListBulletStyle);
        styles.add(mIndentRightStyle);
        styles.add(mIndentLeftStyle);
        styles.add(mAlignLeft);
        styles.add(mAlignCenter);
        styles.add(mAlignRight);

        setStyles(styles);
    }

    public void undo() {
        if (mUndoRedoHelper != null) {
            mUndoRedoHelper.undo();
        }
    }

    public void redo() {
        if (mUndoRedoHelper != null) {
            mUndoRedoHelper.redo();
        }
    }

    public void setBold() {
        mBoldStyle.apply();
    }

    public void setItalic() {
        mItalicStyle.apply();
    }

    public void setSubscript() {
        mSubscriptStyle.apply();
    }

    public void setSuperscript() {
        mSuperscriptStyle.apply();
    }

    public void setStrikeThrough() {
        mStrikethroughStyle.apply();
    }

    public void setUnderline() {
        mUnderlineStyle.apply();
    }

    public void setTextColor(int color) {
        int r = Color.red(color);
        int g = Color.green(color);
        int b = Color.blue(color);
        color = Color.rgb(r, g, b);
        mFontColorStyle.apply(color);
    }

    public void setTextBackgroundColor(int color) {
    }

    public void setFontResource(FontResource fontResource) {
        mFontfaceStyle.apply(fontResource);
    }

    public void setFontSize(int fontSize) {
        mFontSizeStyle.apply(fontSize);
    }

    public void setIndent() {
        mIndentRightStyle.apply();
    }

    public void setOutdent() {
        mIndentLeftStyle.apply();
    }

    public void setAlignLeft() {
        mAlignLeft.apply();
    }

    public void setAlignCenter() {
        mAlignCenter.apply();
    }

    public void setAlignRight() {
        mAlignRight.apply();
    }

    public void setBlockquote() {
        mQuoteStyle.apply();
    }

    public void setBullets() {
        mListBulletStyle.apply();
    }

    public void setNumbers() {
        mListNumberStyle.apply();
    }

    @Override
    public void updateFont(FontResource font) {
        setFontResource(font);
    }
}
