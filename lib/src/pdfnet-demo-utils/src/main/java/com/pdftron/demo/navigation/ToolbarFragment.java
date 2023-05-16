package com.pdftron.demo.navigation;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.ForegroundLayout;

/**
 * A base fragment class used to provide basic support for an embedded <code>Toolbar</code> widget.
 * <p>
 * It handles setting a Toolbar with id {@link R.id#fragment_toolbar} as the containing
 * {@link FragmentActivity}'s {@link androidx.appcompat.app.ActionBar action bar}, applying a fallback shadow
 * to the main fragment content view with id {@link R.id#fragment_content} for pre-Lollipop devices,
 * and positioning the Toolbar correctly when the <code>View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN</code> flag
 * is used with <code>View.setSystemUiVisibility(int)</code>.
 * </p>
 */
public abstract class ToolbarFragment extends DialogFragment {

    private Delegate mDelegate;

    private int mTitleId;
    private CharSequence mTitle;

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDelegate = Delegate.create(getActivity(), useSupportActionBar());
    }

    @CallSuper
    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (mDelegate != null) {
            mDelegate.onViewCreated(view, savedInstanceState);
            if (mTitleId != 0) {
                mDelegate.setTitle(mTitleId);
            } else if (mTitle != null) {
                mDelegate.setTitle(mTitle);
            }
        }
    }

    @CallSuper
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mDelegate != null) {
            mDelegate.onActivityCreated(savedInstanceState);
        }
    }

    public void updateToolbarDrawable() {
        if (mDelegate != null) {
            mDelegate.updateToolbarDrawable();
        }
    }

    public void setTitle(CharSequence title) {
        mTitle = title;
        mTitleId = 0;
        if (mDelegate != null) {
            mDelegate.setTitle(title);
        }
    }

    public void setTitle(int titleId) {
        mTitleId = titleId;
        mTitle = null;
        if (mDelegate != null) {
            mDelegate.setTitle(titleId);
        }
    }

    protected boolean useSupportActionBar() {
        return true;
    }

    /**
     * This class defines a delegate which can be used to add inset-aware toolbar functionality to
     * any {@link Fragment fragment} inside a {@link FragmentActivity}. It can be used when it is
     * not possible to derive directly from {@link ToolbarFragment}, for instance when creating
     * a {@link DialogFragment} subclass.
     */
    public static abstract class Delegate {

        public static Delegate create(FragmentActivity activity, boolean useSupportActionBar) {
            if (Utils.isLollipop()) {
                return new ToolbarFragment.LollipopDelegate(activity, useSupportActionBar);
            } else {
                return new ToolbarFragment.KitKatDelegate(activity, useSupportActionBar);
            }
        }

        public abstract void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState);

        public abstract void onActivityCreated(@Nullable Bundle savedInstanceState);

        public abstract FragmentActivity getActivity();

        public abstract void updateToolbarDrawable();

        public abstract void setTitle(CharSequence title);

        public abstract void setTitle(int titleId);
    }

    private static abstract class BaseDelegate extends Delegate {
        FragmentActivity mActivity;
        boolean mUseSupportActionBar;

        LinearLayout mAppBarLayout;
        Toolbar mToolbar;
        View mContent;
        Drawable mToolbarNavDrawable;

        BaseDelegate(FragmentActivity activity, boolean useSupportActionBar) {
            mActivity = activity;
            mUseSupportActionBar = useSupportActionBar;
        }

        private Boolean isToolbarAvailable() {
            return mToolbar != null && mToolbar.getVisibility() != View.GONE;
        }

        @Override
        public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
            if (view != null) {
                mAppBarLayout = (LinearLayout) view.findViewById(R.id.fragment_app_bar);
                mToolbar = (Toolbar) view.findViewById(R.id.fragment_toolbar);
                if (isToolbarAvailable()) {
                    mToolbarNavDrawable = mToolbar.getNavigationIcon();
                }
                mContent = view.findViewById(R.id.fragment_content);

                if (isToolbarAvailable() && Utils.isLargeScreenWidth(mActivity)) {
                    mToolbar.setNavigationIcon(null);
                }
            }
        }

        @Override
        public void onActivityCreated(@Nullable Bundle savedInstanceState) {
            Activity activity = getActivity();
            if (mUseSupportActionBar && isToolbarAvailable() &&
                    activity instanceof AppCompatActivity) {
                ((AppCompatActivity) activity).setSupportActionBar(mToolbar);
            }
        }

        @Override
        public FragmentActivity getActivity() {
            return mActivity;
        }

        @Override
        public void updateToolbarDrawable() {
            if (mToolbar != null && mActivity != null) {
                if (Utils.isLargeScreenWidth(mActivity)) {
                    mToolbar.setNavigationIcon(null);
                } else {
                    mToolbar.setNavigationIcon(mToolbarNavDrawable);
                }
            }
        }

        @Override
        public void setTitle(CharSequence title) {
            if (mToolbar != null) {
                mToolbar.setTitle(title);
            }
        }

        @Override
        public void setTitle(int titleId) {
            if (mToolbar != null) {
                mToolbar.setTitle(titleId);
            }
        }
    }

    private static class KitKatDelegate extends BaseDelegate {
        KitKatDelegate(FragmentActivity activity, boolean useSupportActionBar) {
            super(activity, useSupportActionBar);
        }

        @Override
        public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if ((mAppBarLayout != null || mToolbar != null) && mContent != null) {
                // Try to apply a toolbar shadow as a foreground drawable.
                if (mContent instanceof FrameLayout) {
                    ((FrameLayout) mContent).setForeground(ContextCompat.getDrawable(getActivity(), R.drawable.controls_toolbar_dropshadow));
                    ((FrameLayout) mContent).setForegroundGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL);
                } else if (mContent instanceof ForegroundLayout) {
                    ((ForegroundLayout) mContent).setForeground(ContextCompat.getDrawable(getActivity(), R.drawable.controls_toolbar_dropshadow));
                    ((ForegroundLayout) mContent).setForegroundGravity(Gravity.TOP | Gravity.FILL_HORIZONTAL);
                }
            }
        }
    }

    private static class LollipopDelegate extends BaseDelegate implements
            View.OnAttachStateChangeListener {
        LollipopDelegate(FragmentActivity activity, boolean useSupportActionBar) {
            super(activity, useSupportActionBar);
        }

        @Override
        public void onViewCreated(@Nullable View view, @Nullable Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);

            if (view != null) {
                // Request a new dispatch of system window insets when the inset view is
                // attached to the view hierarchy.
                if (ViewCompat.isAttachedToWindow(view)) {
                    ViewCompat.requestApplyInsets(view);
                } else {
                    view.addOnAttachStateChangeListener(this);
                }
            }
        }

        @Override
        public void onViewAttachedToWindow(View view) {
            // Request a new dispatch of system window insets.
            ViewCompat.requestApplyInsets(view);
            view.removeOnAttachStateChangeListener(this);
        }

        @Override
        public void onViewDetachedFromWindow(View view) {
        }
    }
}
