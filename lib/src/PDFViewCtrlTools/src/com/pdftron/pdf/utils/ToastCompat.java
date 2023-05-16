package com.pdftron.pdf.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.Resources;
import android.os.Build;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.lang.reflect.Field;

/**
 * Modified ToastCompat class from https://github.com/PureWriter/ToastCompat to address crashes on Android 25.
 */
final class ToastCompat extends Toast {
    @NonNull
    private final Toast toast;

    private ToastCompat(Context context, @NonNull Toast base) {
        super(context);
        this.toast = base;
    }

    public static ToastCompat makeText(Context context, CharSequence text, int duration) {
        // We cannot pass the SafeToastContext to Toast.makeText() because
        // the View will unwrap the base context and we are in vain.
        @SuppressLint("ShowToast")
        Toast toast = Toast.makeText(context, text, duration);
        setContextCompat(toast.getView(), new SafeToastContext(context, toast));
        return new ToastCompat(context, toast);
    }

    public static Toast makeText(Context context, @StringRes int resId, int duration)
            throws Resources.NotFoundException {
        return makeText(context, context.getResources().getText(resId), duration);
    }

    @Override
    public void show() {
        toast.show();
    }

    @Override
    public void setDuration(int duration) {
        toast.setDuration(duration);
    }

    @Override
    public void setGravity(int gravity, int xOffset, int yOffset) {
        toast.setGravity(gravity, xOffset, yOffset);
    }

    @Override
    public void setMargin(float horizontalMargin, float verticalMargin) {
        toast.setMargin(horizontalMargin, verticalMargin);
    }

    @Override
    public void setText(int resId) {
        toast.setText(resId);
    }

    @Override
    public void setText(CharSequence s) {
        toast.setText(s);
    }

    @Override
    public void setView(View view) {
        toast.setView(view);
        setContextCompat(view, new SafeToastContext(view.getContext(), this));
    }

    @Override
    public float getHorizontalMargin() {
        return toast.getHorizontalMargin();
    }

    @Override
    public float getVerticalMargin() {
        return toast.getVerticalMargin();
    }

    @Override
    public int getDuration() {
        return toast.getDuration();
    }

    @Override
    public int getGravity() {
        return toast.getGravity();
    }

    @Override
    public int getXOffset() {
        return toast.getXOffset();
    }

    @Override
    public int getYOffset() {
        return toast.getYOffset();
    }

    @Override
    public View getView() {
        return toast.getView();
    }

    @Override
    public void cancel() {
        toast.cancel();
    }

    private static void setContextCompat(@NonNull View view, @NonNull Context context) {
        if (Build.VERSION.SDK_INT == 25) {
            try {
                Field field = View.class.getDeclaredField("mContext");
                field.setAccessible(true);
                field.set(view, context);
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }
    }

    private static final class SafeToastContext extends ContextWrapper {
        @NonNull
        private Toast toast;

        SafeToastContext(@NonNull Context base, @NonNull Toast toast) {
            super(base);
            this.toast = toast;
        }

        @Override
        public Context getApplicationContext() {
            return new ApplicationContextWrapper(getBaseContext().getApplicationContext());
        }

        private final class ApplicationContextWrapper extends ContextWrapper {

            private ApplicationContextWrapper(@NonNull Context base) {
                super(base);
            }

            @Override
            public Object getSystemService(@NonNull String name) {
                if (Context.WINDOW_SERVICE.equals(name)) {
                    return new WindowManagerWrapper((WindowManager) getBaseContext().getSystemService(name));
                }
                return super.getSystemService(name);
            }
        }

        private final class WindowManagerWrapper implements WindowManager {

            private final @NonNull
            WindowManager base;

            private WindowManagerWrapper(@NonNull WindowManager base) {
                this.base = base;
            }

            @Override
            public Display getDefaultDisplay() {
                return base.getDefaultDisplay();
            }

            @Override
            public void removeViewImmediate(View view) {
                base.removeViewImmediate(view);
            }

            @Override
            public void addView(View view, ViewGroup.LayoutParams params) {
                try {
                    base.addView(view, params);
                } catch (BadTokenException e) {
                    // ignore
                } catch (Throwable throwable) {
                    AnalyticsHandlerAdapter.getInstance().sendException(new Exception(throwable));
                }
            }

            @Override
            public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
                base.updateViewLayout(view, params);
            }

            @Override
            public void removeView(View view) {
                base.removeView(view);
            }
        }
    }
}