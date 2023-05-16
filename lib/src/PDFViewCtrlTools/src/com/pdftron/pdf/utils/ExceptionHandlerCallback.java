package com.pdftron.pdf.utils;

/**
 * Interface for Exception Handling callbacks
 *
 * @see com.pdftron.pdf.utils.ViewerUtils#safeUpdatePageLayout
 */
public interface ExceptionHandlerCallback {
    void onException(Exception e);
}
