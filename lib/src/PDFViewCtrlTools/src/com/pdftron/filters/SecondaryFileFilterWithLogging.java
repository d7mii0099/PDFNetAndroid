package com.pdftron.filters;

import android.content.Context;
import android.net.Uri;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;

import java.io.FileNotFoundException;
import java.util.HashMap;

/**
 * @hide
 */
public class SecondaryFileFilterWithLogging extends SecondaryFileFilter {

    public SecondaryFileFilterWithLogging(Context context, Uri treeUri) throws PDFNetException, FileNotFoundException {
        super(context, treeUri);
    }

    public SecondaryFileFilterWithLogging(Context context, Uri treeUri, int mode) throws PDFNetException, FileNotFoundException {
        super(context, treeUri, mode);
    }

    @Override
    public long onWrite(byte[] bytes, Object o) {
        logPosition(this.mPosition);
        if (this.mPosition < this.mInitialSize) {
            return 0;
        }
        return super.onWrite(bytes, o);
    }

    @Override
    public long onFlush(Object o) {
        logPosition(this.mPosition);
        if (this.mPosition < this.mInitialSize) {
            return 0;
        }
        return super.onFlush(o);
    }

    @Override
    public long onTruncate(long new_size, Object user_object) {
        logPosition(new_size);
        if (new_size < this.mInitialSize) {
            return 0;
        }
        return super.onTruncate(new_size, user_object);
    }

    private void logPosition(long position) {
        if (position < mInitialSize) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(
                    AnalyticsHandlerAdapter.EVENT_FILTER_ERROR_CHECK,
                    failedParam(position)
            );
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.CATEGORY_GENERAL,
                    "ALERT truncate position: " + position + " from initial position: " + mInitialSize, AnalyticsHandlerAdapter.EVENT_FILTER_ERROR_CHECK);
        }
    }

    private static HashMap<String, String> failedParam(long size) {
        HashMap<String, String> result = new HashMap<>();
        result.put("truncate_size", String.valueOf(size));
        return result;
    }
}
