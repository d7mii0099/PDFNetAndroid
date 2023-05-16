//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.lang.ref.WeakReference;

// ref: https://stackoverflow.com/a/16103514/8033906

/**
 * Toast decorator allowing for easy cancellation of notifications. Use this class if you
 * want subsequent Toast notifications to overwrite current ones. </p>
 * <p/>
 * By default, a current CommonToast notification will be cancelled by a subsequent notification.
 * This default behaviour can be changed by calling certain methods like show(boolean).
 */
public class CommonToast {

    public interface CommonToastListener {
        /**
         * Whether to show a toast message, one of the string resource Id or text string will be valid
         * @param stringRes the string resource id
         * @param text the string
         * @return whether this toast message can show
         */
        boolean canShowToast(@StringRes int stringRes, @Nullable CharSequence text);
    }

    public static class CommonToastHandler {
        private static CommonToastHandler _INSTANCE;

        public static CommonToastHandler getInstance() {
            if (_INSTANCE == null) {
                _INSTANCE = new CommonToastHandler();
            }
            return _INSTANCE;
        }

        private CommonToastListener mCommonToastListener;

        /**
         * A callback that allows custom handling for toast messages
         */
        public void setCommonToastListener(CommonToastListener listener) {
            mCommonToastListener = listener;
        }

        CommonToastListener getCommonToastListener() {
            return mCommonToastListener;
        }

        boolean hasListener() {
            return mCommonToastListener != null;
        }
    }

    /**
     * Keeps track of certain CommonToast notifications that may need to be cancelled. This functionality
     * is only offered by some of the methods in this class.
     * <p>
     * Uses a WeakReference to avoid leaking the activity context used to show the original Toast.
     */
    @Nullable
    private volatile static WeakReference<CommonToast> weakCommonToast = null;

    @Nullable
    private static CommonToast getGlobalCommonToast() {
        if (weakCommonToast == null) {
            return null;
        }

        return weakCommonToast.get();
    }

    private static void setGlobalCommonToast(@Nullable CommonToast globalCommonToast) {
        CommonToast.weakCommonToast = new WeakReference<>(globalCommonToast);
    }


    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Internal reference to the Toast object that will be displayed.
     */
    private Toast internalToast;

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Private constructor creates a new CommonToast from a given Toast.
     *
     * @throws NullPointerException if the parameter is <code>null</code>.
     */
    private CommonToast(Toast toast) {
        // null check
        if (toast == null) {
            throw new NullPointerException("CommonToast.CommonToast(Toast) requires a non-null parameter.");
        }

        internalToast = toast;
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Make a standard CommonToast that just contains a text view.
     */
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, CharSequence text, int duration) {
        return new CommonToast(ToastCompat.makeText(context, text, duration));
    }

    /**
     * Make a standard CommonToast that just contains a text view with the text from a resource.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, int resId, int duration)
        throws Resources.NotFoundException {
        return new CommonToast(ToastCompat.makeText(context, resId, duration));
    }

    /**
     * Make a standard CommonToast that just contains a text view. Duration defaults to
     * Toast.LENGTH_SHORT.
     */
    @SuppressWarnings("unused")
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, CharSequence text) {
        return new CommonToast(ToastCompat.makeText(context, text, Toast.LENGTH_SHORT));
    }

    /**
     * Make a standard CommonToast that just contains a text view with the text from a resource.
     * Duration defaults to Toast.LENGTH_SHORT.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    @SuppressWarnings("unused")
    @SuppressLint("ShowToast")
    private static CommonToast makeText(Context context, int resId) throws Resources.NotFoundException {
        return new CommonToast(ToastCompat.makeText(context, resId, Toast.LENGTH_SHORT));
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Show a standard CommonToast that just contains a text view.
     */
    public static void showText(
        @Nullable final Context context,
        final CharSequence text,
        final int duration) {

        if (!validContext(context)) {
            return;
        }

        if (CommonToastHandler.getInstance().hasListener() &&
                !CommonToastHandler.getInstance().getCommonToastListener().canShowToast(0, text)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast.makeText(context, text, duration).show();
                    }
                }
            });
        } else {
            CommonToast.makeText(context, text, duration).show();
        }

    }

    /**
     * Show a standard CommonToast that just contains a text view with the text from a resource.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void showText(
        @Nullable final Context context,
        final int resId,
        final int duration)
        throws Resources.NotFoundException {

        if (!validContext(context)) {
            return;
        }

        if (CommonToastHandler.getInstance().hasListener() &&
                !CommonToastHandler.getInstance().getCommonToastListener().canShowToast(resId, null)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast.makeText(context, resId, duration).show();
                    }
                }
            });
        } else {
            CommonToast.makeText(context, resId, duration).show();
        }

    }

    /**
     * Show a standard CommonToast that just contains a text view. Duration defaults to
     * Toast.LENGTH_SHORT.
     */
    public static void showText(
        @Nullable final Context context,
        final CharSequence text) {

        if (!validContext(context)) {
            return;
        }

        if (CommonToastHandler.getInstance().hasListener() &&
                !CommonToastHandler.getInstance().getCommonToastListener().canShowToast(0, text)) {
            return;
        }

        if (validContext(context)) {
            if (context instanceof Activity) {
                ((Activity) context).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (validContext(context)) {
                            CommonToast.makeText(context, text, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                CommonToast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        }

    }

    /**
     * Show a standard CommonToast that just contains a text view with the text from a resource.
     * Duration defaults to Toast.LENGTH_SHORT.
     * @throws Resources.NotFoundException if the resource can't be found.
     */
    public static void showText(
        @Nullable final Context context,
        final int resId) throws Resources.NotFoundException {

        if (!validContext(context)) {
            return;
        }

        if (CommonToastHandler.getInstance().hasListener() &&
                !CommonToastHandler.getInstance().getCommonToastListener().canShowToast(resId, null)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast.makeText(context, resId, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            CommonToast.makeText(context, resId, Toast.LENGTH_SHORT).show();
        }

    }

    public static void showText(
        @Nullable final Context context,
        final CharSequence text,
        final int duration,
        final int gravity,
        final int xOffset,
        final int yOffset) {

        if (!validContext(context)) {
            return;
        }

        if (CommonToastHandler.getInstance().hasListener() &&
                !CommonToastHandler.getInstance().getCommonToastListener().canShowToast(0, text)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast toast = CommonToast.makeText(context, text, duration);
                        toast.internalToast.setGravity(gravity, xOffset, yOffset);
                        toast.show();
                    }
                }
            });
        } else {
            CommonToast toast = CommonToast.makeText(context, text, duration);
            toast.internalToast.setGravity(gravity, xOffset, yOffset);
            toast.show();
        }

    }

    public static void showText(
        @Nullable final Context context,
        final int resId,
        final int duration,
        final int gravity,
        final int xOffset,
        final int yOffset)
        throws Resources.NotFoundException {

        if (!validContext(context)) {
            return;
        }

        if (CommonToastHandler.getInstance().hasListener() &&
                !CommonToastHandler.getInstance().getCommonToastListener().canShowToast(resId, null)) {
            return;
        }

        if (context instanceof Activity) {
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (validContext(context)) {
                        CommonToast toast = CommonToast.makeText(context, resId, duration);
                        toast.internalToast.setGravity(gravity, xOffset, yOffset);
                        toast.show();
                    }
                }
            });
        } else {
            CommonToast toast = CommonToast.makeText(context, resId, duration);
            toast.internalToast.setGravity(gravity, xOffset, yOffset);
            toast.show();
        }

    }

    private static boolean validContext(@Nullable Context context) {
        if (context == null) {
            return false;
        } else if (!(context instanceof Activity)) { // is likely an Application context
            return true;
        } else { // an activity
            return Utils.validActivity((Activity) context);
        }
    }

    // ////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Close the view if it's showing, or don't show it if it isn't showing yet. You do not normally
     * have to call this. Normally view will disappear on its own after the appropriate duration.
     */
    public void cancel() {
        internalToast.cancel();
    }

    /**
     * Show the view for the specified duration. By default, this method cancels any current
     * notification to immediately display the new one. For conventional Toast.show()
     * queueing behaviour, use method {@link #show(boolean)}.
     *
     * @see #show(boolean)
     */
    public void show() {
        show(true);
    }

    /**
     * Show the view for the specified duration. This method can be used to cancel the current
     * notification, or to queue up notifications.
     *
     * @param cancelCurrent <code>true</code> to cancel any current notification and replace it with this new
     *                      one
     * @see #show()
     */
    public void show(boolean cancelCurrent) {
        // cancel current
        if (cancelCurrent) {
            final CommonToast cachedGlobalCommonToast = getGlobalCommonToast();
            if ((cachedGlobalCommonToast != null)) {
                cachedGlobalCommonToast.cancel();
            }
        }

        // save an instance of this current notification
        setGlobalCommonToast(this);

        View internalToastView = internalToast.getView();
        if (Utils.isJellyBeanMR1() && internalToastView != null) {
            if (Utils.isRtlLayout(internalToastView.getContext())) {
                internalToastView.setTextDirection(View.TEXT_DIRECTION_RTL);
            } else {
                internalToastView.setTextDirection(View.TEXT_DIRECTION_LTR);
            }
        }

        internalToast.show();
    }
}
