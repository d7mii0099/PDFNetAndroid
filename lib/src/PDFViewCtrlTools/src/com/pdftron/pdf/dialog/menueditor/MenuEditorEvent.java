package com.pdftron.pdf.dialog.menueditor;

public class MenuEditorEvent {

    private final Type mType;

    MenuEditorEvent(Type eventType) {
        this.mType = eventType;
    }

    public Type getEventType() {
        return mType;
    }

    public enum Type {
        RESET
    }
}
