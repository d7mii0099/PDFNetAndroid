package com.pdftron.pdf.dialog.tabswitcher;

import androidx.annotation.NonNull;

public class TabSwitcherEvent {

    private final Type mType;
    private final String mTabTag;

    TabSwitcherEvent(@NonNull Type eventType, @NonNull String tabTag) {
        mType = eventType;
        mTabTag = tabTag;
    }

    @NonNull
    public Type getEventType() {
        return mType;
    }

    @NonNull
    public String getTabTag() {
        return mTabTag;
    }

    public enum Type {
        CLOSE_TAB,
        SELECT_TAB
    }
}
