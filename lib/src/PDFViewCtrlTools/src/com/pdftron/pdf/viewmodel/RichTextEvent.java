package com.pdftron.pdf.viewmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.widget.richtext.PTRichEditor;

import java.util.ArrayList;
import java.util.List;

public class RichTextEvent {
    @NonNull
    private final Type mType;
    @Nullable
    private AnnotStyle mAnnotStyle;
    @Nullable
    private PTRichEditor.Type mDecorationType;
    private boolean mChecked;

    RichTextEvent(@NonNull Type eventType) {
        mType = eventType;
    }

    RichTextEvent(@NonNull Type eventType, @Nullable AnnotStyle annotStyle) {
        mType = eventType;
        mAnnotStyle = annotStyle;
    }

    RichTextEvent(@NonNull Type eventType, @Nullable PTRichEditor.Type type, boolean checked) {
        mType = eventType;
        mDecorationType = type;
        mChecked = checked;
    }

    @NonNull
    public Type getEventType() {
        return mType;
    }

    @Nullable
    public AnnotStyle getAnnotStyle() {
        return mAnnotStyle;
    }

    @Nullable
    public PTRichEditor.Type getDecorationType() {
        return mDecorationType;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public enum Type {
        OPEN_TOOLBAR,
        CLOSE_TOOLBAR,
        UPDATE_TOOLBAR,
        SHOW_KEYBOARD,
        HIDE_KEYBOARD,
        UNDO,
        REDO,
        TEXT_STYLE,
        BOLD,
        ITALIC,
        STRIKE_THROUGH,
        UNDERLINE,
        INDENT,
        OUTDENT,
        ALIGN_LEFT,
        ALIGN_CENTER,
        ALIGN_RIGHT,
        BULLETS,
        NUMBERS,
        SUBSCRIPT,
        SUPERSCRIPT,
        BLOCK_QUOTE
    }
}
