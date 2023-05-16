//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.RectF;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Page;
import com.pdftron.pdf.Point;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.FreeText;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.FontResource;
import com.pdftron.pdf.tools.FreeTextCreate;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.viewmodel.RichTextEvent;
import com.pdftron.pdf.viewmodel.RichTextViewModel;
import com.pdftron.pdf.widget.AutoScrollEditText;
import com.pdftron.pdf.widget.AutoScrollEditor;
import com.pdftron.pdf.widget.richtext.PTRichEditor;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

/**
 * An EditText that can editing inline at PDFViewCtrl
 */
public class InlineEditText {

    /**
     * Callback interface to be invoked when an interaction is needed.
     */
    public interface InlineEditTextListener {
        /**
         * The implementation should specify the position of the inline edit text.
         *
         * @return The position of the inline edit text
         */
        RectF getInlineEditTextPosition();

        /**
         * The implementation should toggle to the free text dialog.
         *
         * @param interimText The interim text
         */
        void toggleToFreeTextDialog(String interimText);
    }

    private AutoScrollEditor mEditor;
    private ImageButton mToggleButton;
    private int mToggleButtonWidth;
    private boolean mIsEditing;
    private boolean mCreatingAnnot;

    private InlineEditTextListener mListener;
    private PDFViewCtrl mPdfViewCtrl;

    private boolean mTapToCloseConfirmed = false;

    private boolean mDelayViewRemoval = false;
    private boolean mDelaySetContents = false;
    private String mDelayedContents;

    private @ColorInt
    int mTextColor;
    private int mTextSize;

    @Nullable
    private RichTextViewModel mRichTextViewModel;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    /**
     * Class constructor. By default shows the inline toggle button. See
     * {@link #InlineEditText(PDFViewCtrl, Annot, int, Point, boolean, boolean, InlineEditTextListener)} for
     * additional parameters for hiding the inline toggle button.
     *
     * @param pdfView         The pdf view
     * @param annot           The annotation
     * @param pageNum         The annotation page number
     * @param targetPagePoint The target point page
     * @param listener        The inline edit text listener
     */
    @SuppressLint("ClickableViewAccessibility")
    public InlineEditText(
            PDFViewCtrl pdfView,
            Annot annot,
            int pageNum,
            com.pdftron.pdf.Point targetPagePoint,
            @NonNull InlineEditTextListener listener) {

        this(pdfView, annot, pageNum, targetPagePoint, true, false, listener);
    }

    /**
     * Class constructor
     *
     * @param pdfView                     The pdf view
     * @param annot                       The annotation
     * @param pageNum                     The annotation page number
     * @param targetPagePoint             The target point page
     * @param freeTextInlineToggleEnabled Whether to show the inline toggle button
     * @param listener                    The inline edit text listener
     */
    @SuppressLint("ClickableViewAccessibility")
    public InlineEditText(
            PDFViewCtrl pdfView,
            Annot annot,
            int pageNum,
            com.pdftron.pdf.Point targetPagePoint,
            boolean freeTextInlineToggleEnabled,
            boolean richContentEnabled,
            @NonNull InlineEditTextListener listener) {
        this(pdfView, annot, pageNum, targetPagePoint, freeTextInlineToggleEnabled, richContentEnabled, true, listener);
    }

    /**
     * Class constructor
     *
     * @param pdfView                     The pdf view
     * @param annot                       The annotation
     * @param pageNum                     The annotation page number
     * @param targetPagePoint             The target point page
     * @param freeTextInlineToggleEnabled Whether to show the inline toggle button
     * @param richContentEnabled          Whether to show rich content editor
     * @param editingEnabled              Whether text editing is enabled
     * @param listener                    The inline edit text listener
     */
    @SuppressLint("ClickableViewAccessibility")
    public InlineEditText(
            @NonNull PDFViewCtrl pdfView,
            @Nullable final Annot annot,
            int pageNum,
            @Nullable com.pdftron.pdf.Point targetPagePoint,
            boolean freeTextInlineToggleEnabled,
            boolean richContentEnabled,
            boolean editingEnabled,
            @NonNull InlineEditTextListener listener) {

        mCreatingAnnot = targetPagePoint != null;
        mPdfViewCtrl = pdfView;
        mListener = listener;

        // set edit text
        mEditor = new AutoScrollEditor(mPdfViewCtrl.getContext());
        mEditor.setRichContentEnabled(richContentEnabled);
        boolean tapOutsideToCommit = false;
        try {
            AnnotStyle annotStyle = annot != null ? AnnotUtils.getAnnotStyle(annot) :
                    ToolStyleConfig.getInstance().getCustomAnnotStyle(mPdfViewCtrl.getContext(), Annot.e_FreeText, "");
            boolean isSpacingText = annotStyle != null && annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
            boolean isAutoResize = false;
            ToolManager tm = null;
            if (pdfView.getToolManager() instanceof ToolManager) {
                tm = (ToolManager) pdfView.getToolManager();
                isAutoResize = tm.isAutoResizeFreeText();
            }
            tapOutsideToCommit = isSpacingText || isAutoResize;
            if (annot != null && !richContentEnabled && !isSpacingText && !isAutoResize) {
                // existing freetext
                // exclude rich text and spacing text
                mEditor.setAnnot(mPdfViewCtrl, annot, pageNum);
                boolean shouldUnlockRead = false;
                try {
                    mPdfViewCtrl.docLockRead();
                    shouldUnlockRead = true;
                    mEditor.setAnnotStyle(pdfView, annotStyle);
                } catch (Exception ignored) {
                } finally {
                    if (shouldUnlockRead) {
                        mPdfViewCtrl.docUnlockRead();
                    }
                }
            } else {
                if (null == targetPagePoint && annot != null) {
                    // for rc freetext, we want to expand to full size
                    // when editing so we can obtain the correct appearance
                    boolean shouldUnlockRead = false;
                    try {
                        mPdfViewCtrl.docLockRead();
                        shouldUnlockRead = true;
                        if (annot.getType() == Annot.e_FreeText) {
                            FreeText freeText = new FreeText(annot);
                            Rect r = freeText.getContentRect();
                            r.normalize();
                            if (mPdfViewCtrl.getRightToLeftLanguage()) {
                                targetPagePoint = new Point(r.getX2(), r.getY2());
                            } else {
                                targetPagePoint = new Point(r.getX1(), r.getY2());
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        if (shouldUnlockRead) {
                            mPdfViewCtrl.docUnlockRead();
                        }
                    }
                }
                if (targetPagePoint != null) {
                    // new freetext
                    Rect screenRect = Utils.getScreenRectInPageSpace(pdfView, pageNum);
                    Rect pageRect = Utils.getPageRect(pdfView, pageNum);
                    Rect intersectRect = new Rect();
                    if (screenRect != null && pageRect != null) {
                        screenRect.normalize();
                        pageRect.normalize();
                        intersectRect.intersectRect(screenRect, pageRect);
                        intersectRect.normalize();
                        Rect freeTextRect = new Rect();

                        int pageRotation = Page.e_0;
                        int viewRotation = Page.e_0;

                        boolean shouldUnlockRead = false;
                        try {
                            mPdfViewCtrl.docLockRead();
                            shouldUnlockRead = true;

                            pageRotation = mPdfViewCtrl.getDoc().getPage(pageNum).getRotation();
                            viewRotation = mPdfViewCtrl.getPageRotation();
                        } catch (Exception ignored) {
                        } finally {
                            if (shouldUnlockRead) {
                                mPdfViewCtrl.docUnlockRead();
                            }
                        }
                        int annotRotation = ((pageRotation + viewRotation) % 4) * 90;

                        if (mPdfViewCtrl.getRightToLeftLanguage()) {
                            if (annotRotation == 0) {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, 0);
                            } else if (annotRotation == 90) {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), 0);
                            } else if (annotRotation == 180) {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), pageRect.getY2());
                            } else {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, pageRect.getY2());
                            }
                        } else {
                            if (annotRotation == 0) {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), 0);
                            } else if (annotRotation == 90) {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, pageRect.getX2(), pageRect.getY2());
                            } else if (annotRotation == 180) {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, pageRect.getY2());
                            } else {
                                freeTextRect.set(targetPagePoint.x, targetPagePoint.y, 0, 0);
                            }
                        }
                        freeTextRect.normalize();
                        intersectRect.intersectRect(intersectRect, freeTextRect);
                        intersectRect.normalize();
                        mEditor.setRect(mPdfViewCtrl, intersectRect, pageNum);
                        mEditor.setAnnotStyle(mPdfViewCtrl, annotStyle);

                        final ToolManager tmFinal = tm;
                        mPdfViewCtrl.docLockRead(new PDFViewCtrl.LockRunnable() {
                            @Override
                            public void run() throws Exception {
                                Rect defaultRect = null;
                                if (tmFinal != null && !tmFinal.isDeleteEmptyFreeText() && tmFinal.isAutoResizeFreeText()) {
                                    if (annot.getType() == Annot.e_FreeText) {
                                        FreeText freeText = new FreeText(annot);
                                        defaultRect = FreeTextCreate.getDefaultRect(freeText);
                                    }
                                }
                                mEditor.getEditText().setDefaultRect(defaultRect);
                            }
                        });
                    }
                }
            }
        } catch (Exception e) {
            AnalyticsHandlerAdapter.getInstance().sendException(e);
        }

        LayoutInflater inflater = (LayoutInflater) mPdfViewCtrl.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (inflater == null) {
            return;
        }

        int padding = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.padding_small);
        mEditor.getEditText().setPadding(padding, 0, 0, 0);

        // set up toggle button
        View toggleButtonView = inflater.inflate(R.layout.tools_free_text_inline_toggle_button, null);
        mToggleButton = toggleButtonView.findViewById(R.id.inline_toggle_button);
        mToggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mListener.toggleToFreeTextDialog(mEditor.getEditText().getText().toString());
            }
        });
        if (!freeTextInlineToggleEnabled || richContentEnabled) {
            mToggleButton.setVisibility(View.GONE);
        }
        mToggleButtonWidth = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.free_text_inline_toggle_button_width);

        // hide PDFView scroll bars
        mPdfViewCtrl.setVerticalScrollBarEnabled(false);
        mPdfViewCtrl.setHorizontalScrollBarEnabled(false);

        // set position and size of edit text in post
        // since we need the height of a line
        mEditor.getActiveEditor().post(new Runnable() {
            @Override
            public void run() {
                RectF editBoxRect = mListener.getInlineEditTextPosition();
                setEditTextLocation(editBoxRect);
            }
        });

        mPdfViewCtrl.addView(mEditor);
        mPdfViewCtrl.addView(mToggleButton);

        // bring up keyboard
        if (editingEnabled && mEditor.getActiveEditor().requestFocus()) {
            // Bring up soft keyboard in case it is not shown automatically
            Utils.showSoftKeyboard(mEditor.getContext(), mEditor.getActiveEditor());
        }
        if (mEditor.isRichContentEnabled()) {
            mEditor.getRichEditor().setOnDecorationChangeListener(new PTRichEditor.OnDecorationStateListener() {

                @Override
                public void onStateChangeListener(PTRichEditor.Type type, boolean checked) {
                    if (mRichTextViewModel != null) {
                        mRichTextViewModel.onUpdateDecorationType(type, checked);
                    }
                }
            });
        }
        if (mCreatingAnnot || tapOutsideToCommit) {
            // set touch listener for edittext
            // add touch listener to edit view to determine if
            // the user has tapped text or blank space within the text box.
            // If blank space, save text to free text annotation and remove edit view
            mEditor.getActiveEditor().setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            int y = (int) event.getY();

                            if (mEditor.getActiveEditor() != null) {
                                int height = 0;
                                if (mEditor.getActiveEditor() instanceof EditText) {
                                    int width = mEditor.getEditText().getMeasuredWidth();

                                    TextPaint textPaint = mEditor.getEditText().getPaint();
                                    String text = mEditor.getEditText().getText().toString();

                                    StaticLayout layout = new StaticLayout(text, textPaint, width,
                                            Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                                    height = layout.getHeight();
                                }

                                // if tap is not on text
                                int buffer = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50, mPdfViewCtrl.getContext().getResources().getDisplayMetrics());
                                if (y > (height + buffer)) {
                                    mTapToCloseConfirmed = true;
                                }
                            }
                            break;

                        case MotionEvent.ACTION_SCROLL:
                            mTapToCloseConfirmed = false;
                            break;

                        case MotionEvent.ACTION_UP:
                            if (mTapToCloseConfirmed) {
                                mTapToCloseConfirmed = false;

                                // give the tool manager a chance to process onUp event.
                                // This is particularly useful for AnnotEdit to set
                                // next tool mode and pop up quick menu
                                mPdfViewCtrl.getToolManager().onUp(event, PDFViewCtrl.PriorEventMode.OTHER);

                                return true;
                            }
                        default:
                            break;
                    }

                    return false;
                }
            });
        }

        // Hide any additional components
        ((ToolManager) mPdfViewCtrl.getToolManager()).onInlineFreeTextEditingStarted();

        mIsEditing = true;
    }

    /**
     * Close inline eidt text
     *
     * @param manuallyRemoveView whether remove view manually
     */
    public void close(boolean manuallyRemoveView) {
        close(manuallyRemoveView, true);
    }

    /**
     * Close inline edit text
     *
     * @param manuallyRemoveView whether remove view manually
     * @param hideKeyboard       whether hides keyboard
     */
    public void close(boolean manuallyRemoveView, boolean hideKeyboard) {
        // remove toggle button
        mPdfViewCtrl.removeView(mToggleButton);
        // remove resizing button
        if (Utils.isLollipop()) {
            mEditor.getEditText().removeSpacingHandle();
        }

        // Hide soft keyboard
        if (hideKeyboard) {
            InputMethodManager imm = (InputMethodManager) mPdfViewCtrl.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(mPdfViewCtrl.getRootView().getWindowToken(), 0);
            }
        }

        // reset editor
        if (mEditor.isRichContentEnabled()) {
            mEditor.getRichEditor().setOnDecorationChangeListener(null);
        }

        // show toolbars
        mPdfViewCtrl.setVerticalScrollBarEnabled(false);
        mPdfViewCtrl.setHorizontalScrollBarEnabled(false);

        mIsEditing = false;

        if (manuallyRemoveView) {
            mPdfViewCtrl.removeView(mEditor);
        } else {
            mDelayViewRemoval = true;
        }

        mDisposable.clear();
    }

    /**
     * Whether it is editing
     *
     * @return true then editing, false otherwise
     */
    public Boolean isEditing() {
        return mIsEditing;
    }

    /**
     * Whether it delays when removing this view
     *
     * @return True if it delay when removing this view; False otherwise
     */
    public boolean delayViewRemoval() {
        return mDelayViewRemoval;
    }

    public void setHTMLContents(String htmlContents) {
        getRichEditor().fromHtml(htmlContents);
    }

    /**
     * Gets contents of this edit text
     *
     * @return The contents
     */
    public String getContents() {
        return mEditor.getActiveText();
    }

    /**
     * Sets delayed contents to the contents
     */
    public void setContents() {
        if (null != mDelayedContents) {
            EditText editText = mEditor.getEditText();
            editText.setText(mDelayedContents);
            if (editText.getText() != null) {
                editText.setSelection(editText.getText().length());
            }
        }
        mDelaySetContents = false;
        mDelayedContents = null;
    }

    /**
     * Sets contents
     *
     * @param contents The contents
     */
    public void setContents(String contents) {
        EditText editText = mEditor.getEditText();
        editText.setText(contents);
        if (editText.getText() != null) {
            editText.setSelection(editText.getText().length());
        }
    }

    /**
     * Whether it delays when setting contents
     *
     * @return true then delay, false otherwise
     */
    public boolean delaySetContents() {
        return mDelaySetContents;
    }

    /**
     * Sets delayed contents
     *
     * @param contents The contents
     */
    public void setDelaySetContents(String contents) {
        if (mPdfViewCtrl.isAnnotationLayerEnabled()) {
            setContents(contents);
        } else {
            mDelaySetContents = true;
            mDelayedContents = contents;
        }
    }

    /**
     * Sets fontResource style
     */
    public void setFontResource(FontResource fontResource) {
        if (mEditor.isRichContentEnabled()) {
            mEditor.getRichEditor().setFontResource(fontResource);
        }
    }

    /**
     * Sets text size
     *
     * @param textSize The text size
     */
    public void setTextSize(int textSize) {
        mTextSize = textSize;
        if (mEditor.isRichContentEnabled()) {
            mEditor.getRichEditor().setFontSize(textSize);
        }
        textSize *= (float) mPdfViewCtrl.getZoom();
        mEditor.getEditText().setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
    }

    /**
     * Sets text color
     *
     * @param textColor The text color
     */
    public void setTextColor(@ColorInt int textColor) {
        mTextColor = textColor;
        mEditor.getEditText().setTextColor(textColor);
        if (mEditor.isRichContentEnabled()) {
            mEditor.getRichEditor().setTextColor(textColor);
        }
    }

    public AutoScrollEditor getEditor() {
        return mEditor;
    }

    /**
     * Gets the edit text
     *
     * @return The edit text
     */
    public AutoScrollEditText getEditText() {
        return mEditor.getEditText();
    }

    public PTRichEditor getRichEditor() {
        return mEditor.getRichEditor();
    }

    public void setRichTextViewModel(@Nullable RichTextViewModel viewModel) {
        mRichTextViewModel = viewModel;
        if (mRichTextViewModel != null) {
            mDisposable.add(mRichTextViewModel.getObservable()
                    .subscribe(new Consumer<RichTextEvent>() {
                        @Override
                        public void accept(RichTextEvent richTextEvent) throws Exception {
                            switch (richTextEvent.getEventType()) {
                                case TEXT_STYLE:
                                    AnnotStyle annotStyle = richTextEvent.getAnnotStyle();
                                    updateRichContentStyle(annotStyle);
                                    break;
                                case SHOW_KEYBOARD:
                                    break;
                                case HIDE_KEYBOARD:
                                    Utils.hideSoftKeyboard(getRichEditor().getContext(), getRichEditor());
                                    break;
                                case UNDO:
                                    getRichEditor().undo();
                                    break;
                                case REDO:
                                    getRichEditor().redo();
                                    break;
                                case BOLD:
                                    getRichEditor().setBold();
                                    break;
                                case ITALIC:
                                    getRichEditor().setItalic();
                                    break;
                                case STRIKE_THROUGH:
                                    getRichEditor().setStrikeThrough();
                                    break;
                                case UNDERLINE:
                                    getRichEditor().setUnderline();
                                    break;
                                case INDENT:
                                    getRichEditor().setIndent();
                                    break;
                                case OUTDENT:
                                    getRichEditor().setOutdent();
                                    break;
                                case ALIGN_LEFT:
                                    getRichEditor().setAlignLeft();
                                    break;
                                case ALIGN_CENTER:
                                    getRichEditor().setAlignCenter();
                                    break;
                                case ALIGN_RIGHT:
                                    getRichEditor().setAlignRight();
                                    break;
                                case SUPERSCRIPT:
                                    getRichEditor().setSuperscript();
                                    break;
                                case SUBSCRIPT:
                                    getRichEditor().setSubscript();
                                    break;
                                case BULLETS:
                                    getRichEditor().setBullets();
                                    break;
                                case NUMBERS:
                                    getRichEditor().setNumbers();
                                    break;
                                case BLOCK_QUOTE:
                                    getRichEditor().setBlockquote();
                                    break;
                            }
                        }
                    }, new Consumer<Throwable>() {
                        @Override
                        public void accept(Throwable throwable) throws Exception {
                            AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                        }
                    }));
        }
    }

    public void updateRichContentStyle(@Nullable AnnotStyle annotStyle) {
        if (null == annotStyle) {
            return;
        }
        setTextColor(annotStyle.getTextColor());
        setTextSize(Math.round(annotStyle.getTextSize()));
        setFontResource(annotStyle.getFont());
    }

    /**
     * Sets background color
     *
     * @param backgroundColor background color
     */
    public void setBackgroundColor(@ColorInt int backgroundColor) {
        mEditor.getEditText().setBackgroundColor(backgroundColor);
    }

    /**
     * Sets edit text location
     *
     * @param position The location
     */
    private void setEditTextLocation(RectF position) {
        int left = (int) position.left;
        int top = (int) position.top;
        int right = (int) position.right;
        int bottom = (int) position.bottom;

        if (mCreatingAnnot) {
            // if the text box is smaller than 1 line,
            // set the height to be equal to one line
            int lineHeight = mEditor.getEditText().getLineHeight();
            if (Math.abs(bottom - top) < (lineHeight * 1.5)) {
                top = bottom - (int) (lineHeight * 1.5);
            }

            int minWidth = mPdfViewCtrl.getContext().getResources().getDimensionPixelSize(R.dimen.free_text_inline_min_textbox_width);
            if (Math.abs(left - right) < minWidth) {
                left = right - minWidth;
            }
        }

        // set the location of the popup
        int screenWidth = Utils.getScreenWidth(mPdfViewCtrl.getContext());
        int editTextWidth = right - left;

        // buttons position relative to the screen
        int screenButtonPosRight = right - mPdfViewCtrl.getScrollX() + mPdfViewCtrl.getHScrollPos() + mToggleButtonWidth;
        int screenButtonPosLeft = left - mPdfViewCtrl.getScrollX() + mPdfViewCtrl.getHScrollPos() - mToggleButtonWidth;

        int screenPageLeft = mPdfViewCtrl.getHScrollPos(); // left edge of PDFViewCtrl relative to screen
        int screenPageRight = screenPageLeft + mPdfViewCtrl.getWidth(); // right edge of PDFViewCtrl relative to screen
        int buttonViewLeftPos = left - mToggleButtonWidth; // left position of button if put in PDFViewCtrl
        int buttonViewRightPos = right + mToggleButtonWidth; // right position of button if put in PDFViewCtrl

        // move button up if the line height is smaller than the toggle button height
        int buttonPosBottom = top + mToggleButtonWidth;
        if (mEditor.getEditText().getLineHeight() < mToggleButtonWidth) {
            buttonPosBottom = top + mEditor.getEditText().getLineHeight();

            int screenButtonPostTop = buttonPosBottom - mPdfViewCtrl.getScrollY() + mPdfViewCtrl.getVScrollPos() - mToggleButtonWidth;
            if (screenButtonPostTop < mPdfViewCtrl.getScrollY()) {
                buttonPosBottom = top + mToggleButtonWidth;
            }
        }

        if (mPdfViewCtrl.getRightToLeftLanguage()) {
            if (editTextWidth >= screenWidth) {
                // if the edit text occupies the entire screen, set the toggle button within the EditText
                mToggleButton.layout(left, buttonPosBottom - mToggleButtonWidth, left + mToggleButtonWidth, buttonPosBottom);
                mPdfViewCtrl.scrollBy(right - mPdfViewCtrl.getScrollX(), 0);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(270);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
            } else if (screenPageRight > screenButtonPosRight) {
                // if the button will fit to the right of the edit text on the screen (without having to
                // scroll the PDFViewCtrl)
                mToggleButton.setRotation(0);
                mToggleButton.layout(right, buttonPosBottom - mToggleButtonWidth, buttonViewRightPos, buttonPosBottom);
            } else if (screenButtonPosRight < screenPageRight) {
                // if there is room to scroll the PDFViewCtrl so that it will fit on the right, then
                // scroll the PDFViewCtrl to show the button
                mToggleButton.setRotation(0);
                mPdfViewCtrl.scrollBy(buttonViewRightPos - mPdfViewCtrl.getScrollX() - mPdfViewCtrl.getWidth(), 0);
                mToggleButton.layout(right, buttonPosBottom - mToggleButtonWidth, buttonViewRightPos, buttonPosBottom);
            } else if (screenButtonPosLeft > screenPageLeft) {
                // if there is room to the left of the edit text, place the
                // toggle button there
                mToggleButton.layout(left - mToggleButtonWidth, buttonPosBottom - mToggleButtonWidth, left, buttonPosBottom);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(270);
            } else {
                // rotate the button so that it is facing away
                mToggleButton.setRotation(270);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
                // if none of the above cases work, set it within the edit text
                mToggleButton.layout(left, buttonPosBottom - mToggleButtonWidth, left + mToggleButtonWidth, buttonPosBottom);
            }
        } else {
            if (editTextWidth >= screenWidth) {
                // if the edit text occupies the entire screen, set the toggle button within the EditText
                mToggleButton.layout(right - mToggleButtonWidth, buttonPosBottom - mToggleButtonWidth, right, buttonPosBottom);
                mPdfViewCtrl.scrollBy(left - mPdfViewCtrl.getScrollX(), 0);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(0);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
            } else if (screenPageLeft < screenButtonPosLeft) {
                // if the button will fit to the left of the edit text on the screen (without having to
                // scroll the PDFViewCtrl)
                mToggleButton.setRotation(270);
                mToggleButton.layout(buttonViewLeftPos, buttonPosBottom - mToggleButtonWidth, left, buttonPosBottom);
            } else if (screenButtonPosLeft > 0) {
                // if there is room to scroll the PDFViewCtrl so that it will fit on the left, then
                // scroll the PDFViewCtrl to show the button
                mToggleButton.setRotation(270);
                mPdfViewCtrl.scrollBy(buttonViewLeftPos - mPdfViewCtrl.getScrollX(), 0);
                mToggleButton.layout(buttonViewLeftPos, buttonPosBottom - mToggleButtonWidth, left, buttonPosBottom);
            } else if (screenButtonPosRight < screenPageRight) {
                // if there is room to the right of the edit text, place the
                // toggle button there
                mToggleButton.layout(right, buttonPosBottom - mToggleButtonWidth, right + mToggleButtonWidth, buttonPosBottom);
                // rotate the button so that it is facing away
                mToggleButton.setRotation(0);
            } else {
                // rotate the button so that it is facing away
                mToggleButton.setRotation(0);
                mToggleButton.setBackgroundResource(R.drawable.annotation_free_text_toggle_button_transparent_bgd);
                // if none of the above cases work, set it within the edit text
                mToggleButton.layout(right - mToggleButtonWidth, buttonPosBottom - mToggleButtonWidth, right, buttonPosBottom);
            }
        }
    }

    /**
     * Removes view
     */
    public void removeView() {
        if (mEditor != null) {
            mPdfViewCtrl.removeView(mEditor);
        }
    }

    /**
     * Adds text watcher listener
     *
     * @param textWatcherListener The text watcher listener
     */
    public void addTextWatcher(TextWatcher textWatcherListener) {
        mEditor.getEditText().addTextChangedListener(textWatcherListener);
    }

    public void setCalculateAlignment(boolean calculateAlignment) {
        mEditor.setCalculateAlignment(calculateAlignment);
    }

}
