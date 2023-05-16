package com.pdftron.demo.browser.ui;

import android.content.Context;

import androidx.annotation.Nullable;
import io.reactivex.Observable;
import io.reactivex.Single;

interface DatabaseUpdater {

    /**
     * @return an {@link Observable} that fetches files from disk. Emits list of supported that are
     * available (depending on permissions).
     */
    Single<Boolean> getFiles(@Nullable Context context);
}
