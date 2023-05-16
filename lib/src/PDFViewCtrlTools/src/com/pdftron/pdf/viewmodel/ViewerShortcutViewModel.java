package com.pdftron.pdf.viewmodel;

import android.view.KeyEvent;
import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

public class ViewerShortcutViewModel extends ViewModel {
    private MutableLiveData<KeyboardShortcut> mShortcutLiveData = new MutableLiveData<>();

    public void observeKeyboardEvents(@NonNull LifecycleOwner owner, @NonNull Observer<KeyboardShortcut> observer) {
        mShortcutLiveData.observe(owner, observer);
    }

    public void setKeyboardEvent(int keyCode, @NonNull KeyEvent event) {
        mShortcutLiveData.setValue(new KeyboardShortcut(keyCode, event));
    }

    public static class KeyboardShortcut {

        private final int mKeyCode;
        @NonNull
        private final KeyEvent mEvent;

        public KeyboardShortcut(int keyCode, @NonNull KeyEvent event) {
            mKeyCode = keyCode;
            mEvent = event;
        }

        public int getKeyCode() {
            return mKeyCode;
        }

        @NonNull
        public KeyEvent getEvent() {
            return mEvent;
        }
    }
}
