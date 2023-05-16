package com.pdftron.demo.utils;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;

public class LifecycleUtils {
    /**
     * Ensures the function will only run when lifecycle owner is in a resumed state (e.g. not backgrounded)
     * Otherwise it creates a listener to execute once resumed
     *
     * @param owner    Lifecycle Owner
     * @param callback Callback to execute on resume
     */
    public static void runOnResume(LifecycleOwner owner, LifecycleCallback callback) {
        if (owner.getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            callback.onResume();
        } else {
            owner.getLifecycle().addObserver(new LifecycleObserver() {
                @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
                public void onResume() {
                    callback.onResume();
                    owner.getLifecycle().removeObserver(this);
                }
            });
        }
    }
}
