package com.pdftron.richeditor;

import android.annotation.TargetApi;
import android.content.ClipData;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Editable;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.CharacterStyle;
import android.text.style.QuoteSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;

import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.richeditor.android.inner.Html;
import com.pdftron.richeditor.render.AreTagHandler;
import com.pdftron.richeditor.spans.AreSubscriptSpan;
import com.pdftron.richeditor.spans.AreSuperscriptSpan;
import com.pdftron.richeditor.spans.AreUnderlineSpan;
import com.pdftron.richeditor.styles.ARE_Alignment;
import com.pdftron.richeditor.styles.ARE_BackgroundColor;
import com.pdftron.richeditor.styles.ARE_Bold;
import com.pdftron.richeditor.styles.ARE_FontColor;
import com.pdftron.richeditor.styles.ARE_FontSize;
import com.pdftron.richeditor.styles.ARE_Fontface;
import com.pdftron.richeditor.styles.ARE_Helper;
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
import java.util.HashMap;
import java.util.List;

/**
 * https://github.com/chinalwb/Android-Rich-text-Editor
 * Modified by PDFTron
 */
public class AREditText extends AutoScrollEditText {

    public enum Type {
        BOLD,
        ITALIC,
        SUBSCRIPT,
        SUPERSCRIPT,
        STRIKETHROUGH,
        UNDERLINE,
        NUMBERED_LIST,
        BULLET_LIST,
        JUSTIFY_CENTER,
        JUSTIFY_FULL,
        JUSTIFY_LEFT,
        JUSTIFY_RIGHT,
        QUOTE
    }

    public interface OnDecorationStateListener {
        void onStateChangeListener(Type type, boolean checked);
    }

    private OnDecorationStateListener mDecorationStateListener;

    public void setOnDecorationChangeListener(OnDecorationStateListener listener) {
        this.mDecorationStateListener = listener;
    }

    public OnDecorationStateListener getDecorationStateListener() {
        return this.mDecorationStateListener;
    }

    private static boolean LOG = false;

    private static boolean MONITORING = true;

    private List<IARE_Style> mStylesList = new ArrayList<>();
    private HashMap<Class<? extends IARE_Style>, IARE_Style> mStylesMap;

    private TextWatcher mTextWatcher;

    public AREditText(Context context) {
        this(context, null);
    }

    public AREditText(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AREditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
        setupListener();
    }

    private void init() {
        useSoftwareLayerOnAndroid8();
        this.setFocusableInTouchMode(true);
        this.setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE
                | EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
    }

    private void paste(ClipData clip) {
        Editable mText = this.getEditableText();
        int min = 0;
        int max = mText.length();
        if (clip != null) {
            boolean didFirst = false;
            for (int i = 0; i < clip.getItemCount(); i++) {
                final CharSequence paste;
                paste = getClipItemCharSequence(clip.getItemAt(i));
                if (paste != null) {
                    if (!didFirst) {
                        Selection.setSelection((Spannable) mText, max);
                        ((Editable) mText).replace(min, max, paste);
                        didFirst = true;
                    } else {
                        ((Editable) mText).insert(getSelectionEnd(), "\n");
                        ((Editable) mText).insert(getSelectionEnd(), paste);
                    }
                }
            }
        }
    }

    @TargetApi(16)
    private CharSequence getClipItemCharSequence(ClipData.Item itemAt) {
        CharSequence text = getText();
        if (text instanceof Spanned) {
            return text;
        }
        String htmlText = itemAt.getHtmlText();
        if (htmlText != null) {
            try {
                Html.TagHandler tagHandler = new AreTagHandler();
                CharSequence newText = Html.fromHtml(htmlText, Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH, tagHandler);
                if (newText != null) {
                    return newText;
                }
            } catch (RuntimeException e) {
                // If anything bad happens, we'll fall back on the plain text.
            }
        }

        return itemAt.coerceToStyledText(getContext());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    /**
     * Sets up listeners for controls.
     */
    private void setupListener() {
        setupTextWatcher();
    } // #End of setupListener()

    /**
     * Monitoring text changes.
     */
    private void setupTextWatcher() {
        mTextWatcher = new TextWatcher() {

            int startPos = 0;
            int endPos = 0;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!MONITORING) {
                    return;
                }
                if (LOG) {
                    Util.log("beforeTextChanged:: s = " + s + ", start = " + start + ", count = " + count
                            + ", after = " + after);
                }
                // DO NOTHING FOR NOW
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!MONITORING) {
                    return;
                }

                if (LOG) {
                    Util.log("onTextChanged:: s = " + s + ", start = " + start + ", count = " + count + ", before = "
                            + before);
                }
                this.startPos = start;
                this.endPos = start + count;
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (!MONITORING) {
                    return;
                }

                if (LOG) {
                    Util.log("afterTextChanged:: s = " + s);
                }

                if (endPos <= startPos) {
                    Util.log("User deletes: start == " + startPos + " endPos == " + endPos);
                }

                for (IARE_Style style : mStylesList) {
                    style.applyStyle(s, startPos, endPos);
                }
            }
        };

        this.addTextChangedListener(mTextWatcher);
    }

    public void setStyles(List<IARE_Style> styles) {
        mStylesList.clear();
        mStylesList.addAll(styles);

        if (mStylesMap == null) {
            mStylesMap = new HashMap<>();
        }
        mStylesMap.clear();
        for (IARE_Style style : styles) {
            if (style instanceof ARE_FontSize) {
                mStylesMap.put(ARE_FontSize.class, style);
            } else if (style instanceof ARE_Fontface) {
                mStylesMap.put(ARE_Fontface.class, style);
            } else if (style instanceof ARE_Bold) {
                mStylesMap.put(ARE_Bold.class, style);
            } else if (style instanceof ARE_Italic) {
                mStylesMap.put(ARE_Italic.class, style);
            } else if (style instanceof ARE_Underline) {
                mStylesMap.put(ARE_Underline.class, style);
            } else if (style instanceof ARE_Strikethrough) {
                mStylesMap.put(ARE_Strikethrough.class, style);
            } else if (style instanceof ARE_Subscript) {
                mStylesMap.put(ARE_Subscript.class, style);
            } else if (style instanceof ARE_Superscript) {
                mStylesMap.put(ARE_Superscript.class, style);
            } else if (style instanceof ARE_Quote) {
                mStylesMap.put(ARE_Quote.class, style);
            } else if (style instanceof ARE_FontColor) {
                mStylesMap.put(ARE_FontColor.class, style);
            } else if (style instanceof ARE_BackgroundColor) {
                mStylesMap.put(ARE_BackgroundColor.class, style);
            } else if (style instanceof ARE_ListNumber) {
                mStylesMap.put(ARE_ListNumber.class, style);
            } else if (style instanceof ARE_ListBullet) {
                mStylesMap.put(ARE_ListBullet.class, style);
            } else if (style instanceof ARE_IndentRight) {
                mStylesMap.put(ARE_IndentRight.class, style);
            } else if (style instanceof ARE_IndentLeft) {
                mStylesMap.put(ARE_IndentLeft.class, style);
            } else if (style instanceof ARE_Alignment) {
                mStylesMap.put(ARE_Alignment.class, style);
            }
        }
    }

    @Override
    public void onSelectionChanged(int selStart, int selEnd) {
        boolean boldExists = false;
        boolean italicsExists = false;
        boolean underlinedExists = false;
        boolean strikethroughExists = false;
        boolean subscriptExists = false;
        boolean superscriptExists = false;
        boolean backgroundColorExists = false;
        boolean quoteExists = false;

        //
        // Two cases:
        // 1. Selection is just a pure cursor
        // 2. Selection is a range
        Editable editable = this.getEditableText();
        if (selStart > 0 && selStart == selEnd) {
            CharacterStyle[] styleSpans = editable.getSpans(selStart - 1, selStart, CharacterStyle.class);

            for (CharacterStyle styleSpan : styleSpans) {
                if (styleSpan instanceof StyleSpan) {
                    if (((StyleSpan) styleSpan).getStyle() == Typeface.BOLD) {
                        boldExists = true;
                    } else if (((StyleSpan) styleSpan).getStyle() == Typeface.ITALIC) {
                        italicsExists = true;
                    } else if (((StyleSpan) styleSpan).getStyle() == Typeface.BOLD_ITALIC) {
                        // TODO
                    }
                } else if (styleSpan instanceof AreUnderlineSpan) {
                    underlinedExists = true;
                } else if (styleSpan instanceof StrikethroughSpan) {
                    strikethroughExists = true;
                } else if (styleSpan instanceof BackgroundColorSpan) {
                    backgroundColorExists = true;
                }
            }

            QuoteSpan[] quoteSpans = editable.getSpans(selStart - 1, selStart, QuoteSpan.class);
            if (quoteSpans != null && quoteSpans.length > 0) {
                quoteExists = true;
            }

            AreSubscriptSpan[] subscriptSpans = editable.getSpans(selStart - 1, selStart, AreSubscriptSpan.class);
            if (subscriptSpans != null && subscriptSpans.length > 0) {
                subscriptExists = true;
            }

            AreSuperscriptSpan[] superscriptSpans = editable.getSpans(selStart - 1, selStart, AreSuperscriptSpan.class);
            if (superscriptSpans != null && superscriptSpans.length > 0) {
                superscriptExists = true;
            }
        } else {
            //
            // Selection is a range
            CharacterStyle[] styleSpans = editable.getSpans(selStart, selEnd, CharacterStyle.class);

            for (CharacterStyle styleSpan : styleSpans) {
                if (styleSpan instanceof StyleSpan) {
                    if (((StyleSpan) styleSpan).getStyle() == Typeface.BOLD) {
                        if (editable.getSpanStart(styleSpan) <= selStart
                                && editable.getSpanEnd(styleSpan) >= selEnd) {
                            boldExists = true;
                        }
                    } else if (((StyleSpan) styleSpan).getStyle() == Typeface.ITALIC) {
                        if (editable.getSpanStart(styleSpan) <= selStart
                                && editable.getSpanEnd(styleSpan) >= selEnd) {
                            italicsExists = true;
                        }
                    } else if (((StyleSpan) styleSpan).getStyle() == Typeface.BOLD_ITALIC) {
                        if (editable.getSpanStart(styleSpan) <= selStart
                                && editable.getSpanEnd(styleSpan) >= selEnd) {
                            italicsExists = true;
                            boldExists = true;
                        }
                    }
                } else if (styleSpan instanceof AreUnderlineSpan) {
                    if (editable.getSpanStart(styleSpan) <= selStart
                            && editable.getSpanEnd(styleSpan) >= selEnd) {
                        underlinedExists = true;
                    }
                } else if (styleSpan instanceof StrikethroughSpan) {
                    if (editable.getSpanStart(styleSpan) <= selStart
                            && editable.getSpanEnd(styleSpan) >= selEnd) {
                        strikethroughExists = true;
                    }
                } else if (styleSpan instanceof BackgroundColorSpan) {
                    if (editable.getSpanStart(styleSpan) <= selStart
                            && editable.getSpanEnd(styleSpan) >= selEnd) {
                        backgroundColorExists = true;
                    }
                }
            }
        }

        QuoteSpan[] quoteSpans = editable.getSpans(selStart, selEnd, QuoteSpan.class);
        if (quoteSpans != null && quoteSpans.length > 0) {
            if (editable.getSpanStart(quoteSpans[0]) <= selStart
                    && editable.getSpanEnd(quoteSpans[0]) >= selEnd) {
                quoteExists = true;
            }
        }

        AreSubscriptSpan[] subscriptSpans = editable.getSpans(selStart, selEnd, AreSubscriptSpan.class);
        if (subscriptSpans != null && subscriptSpans.length > 0) {
            if (editable.getSpanStart(subscriptSpans[0]) <= selStart
                    && editable.getSpanEnd(subscriptSpans[0]) >= selEnd) {
                subscriptExists = true;
            }
        }

        AreSuperscriptSpan[] superscriptSpans = editable.getSpans(selStart, selEnd, AreSuperscriptSpan.class);
        if (superscriptSpans != null && superscriptSpans.length > 0) {
            if (editable.getSpanStart(superscriptSpans[0]) <= selStart
                    && editable.getSpanEnd(superscriptSpans[0]) >= selEnd) {
                superscriptExists = true;
            }
        }

        //
        // Set style checked status
        if (mStylesMap != null) {
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Bold.class), boldExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Italic.class), italicsExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Underline.class), underlinedExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Strikethrough.class), strikethroughExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Subscript.class), subscriptExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Superscript.class), superscriptExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_BackgroundColor.class), backgroundColorExists);
            ARE_Helper.updateCheckStatus(mStylesMap.get(ARE_Quote.class), quoteExists);
        }
    } // #End of method:: onSelectionChanged

    /**
     * Sets html content to EditText.
     *
     * @param html
     * @return
     */
    public void fromHtml(String html) {
        Html.TagHandler tagHandler = new AreTagHandler();
        Spanned spanned = Html.fromHtml(html, Html.FROM_HTML_SEPARATOR_LINE_BREAK_PARAGRAPH, tagHandler);
        stopMonitor();
        this.getEditableText().append(spanned);
        startMonitor();
    }

    public String getHtml() {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        String editTextHtml = Html.toHtml(getEditableText(), Html.TO_HTML_PARAGRAPH_LINES_INDIVIDUAL);
        html.append(editTextHtml);
        html.append("</body></html>");
        return html.toString().replaceAll(Constants.ZERO_WIDTH_SPACE_STR_ESCAPE, "");
    }

    /**
     * Needs this because of this bug in Android O:
     * https://issuetracker.google.com/issues/67102093
     */
    public void useSoftwareLayerOnAndroid8() {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.O) {
            this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
    }

    public static void startMonitor() {
        MONITORING = true;
    }

    public static void stopMonitor() {
        MONITORING = false;
    }
    /* ----------------------
     * Customization part
     * ---------------------- */
}
