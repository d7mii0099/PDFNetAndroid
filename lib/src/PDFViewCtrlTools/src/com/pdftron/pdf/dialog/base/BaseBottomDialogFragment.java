package com.pdftron.pdf.dialog.base;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.AnalyticsAnnotStylePicker;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;

/**
 * A dialog fragment that is a bottom sheet on small devices and a popup dialog on tablets.
 */
public abstract class BaseBottomDialogFragment extends DialogFragment {
    protected static final String ARGS_KEY_ANCHOR = "anchor";
    protected static final String ARGS_KEY_ANCHOR_SCREEN = "anchor_screen";

    protected CoordinatorLayout.Behavior mDialogBehavior;
    private BaseBottomDialogFragment.BottomSheetCallback mBottomSheetCallback;

    private DialogInterface.OnDismissListener mDismissListener;
    protected NestedScrollView mBottomSheet;
    protected Rect mAnchor;
    private boolean mIsAnchorInScreen;

    protected abstract Dialog onCreateDialogImpl(@NonNull Context context);

    protected abstract String getFragmentTag();

    @LayoutRes
    protected abstract int getContentLayoutResource();

    /**
     * Sets on dismiss listener
     *
     * @param listener dismiss listener
     */
    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        mDismissListener = listener;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mAnchor != null) {
            Bundle rect = new Bundle();
            rect.putInt("left", mAnchor.left);
            rect.putInt("top", mAnchor.top);
            rect.putInt("right", mAnchor.right);
            rect.putInt("bottom", mAnchor.bottom);
            outState.putBundle(ARGS_KEY_ANCHOR, rect);
        }
        outState.putBoolean(ARGS_KEY_ANCHOR_SCREEN, mIsAnchorInScreen);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARGS_KEY_ANCHOR)) {
                Bundle rect = savedInstanceState.getBundle(ARGS_KEY_ANCHOR);
                if (rect != null) {
                    mAnchor = new Rect(rect.getInt("left"), rect.getInt("top"), rect.getInt("right"), rect.getInt("bottom"));
                }
            }

            if (savedInstanceState.containsKey(ARGS_KEY_ANCHOR_SCREEN)) {
                mIsAnchorInScreen = savedInstanceState.getBoolean(ARGS_KEY_ANCHOR_SCREEN);
            }
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);
        Bundle arguments = getArguments();
        if (arguments == null) {
            return;
        }

        if (arguments.containsKey(ARGS_KEY_ANCHOR)) {
            Bundle bundle = arguments.getBundle(ARGS_KEY_ANCHOR);
            if (bundle != null) {
                mAnchor = new Rect(bundle.getInt("left"), bundle.getInt("top"), bundle.getInt("right"), bundle.getInt("bottom"));
            }
        }

        if (arguments.containsKey(ARGS_KEY_ANCHOR_SCREEN)) {
            mIsAnchorInScreen = arguments.getBoolean(ARGS_KEY_ANCHOR_SCREEN);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog d = onCreateDialogImpl(getActivity());
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        if (d.getWindow() != null) {
            lp.copyFrom(d.getWindow().getAttributes());
            lp.width = WindowManager.LayoutParams.MATCH_PARENT;
            lp.height = WindowManager.LayoutParams.MATCH_PARENT;
            d.getWindow().setAttributes(lp);

            if (ViewerUtils.isInFullScreenMode(getActivity())) {
                d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            }
        }
        return d;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_base_bottomsheet_dialog, container, false);

        mBottomSheet = view.findViewById(R.id.bottom_sheet);
        mBottomSheet.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return mBottomSheetCallback.isLocked();
            }
        });
        inflater.inflate(getContentLayoutResource(), mBottomSheet);

        adjustBottomSheetWidth();

        mBottomSheetCallback = new BaseBottomDialogFragment.BottomSheetCallback();

        if (isBottomSheet()) {
            BottomSheetBehavior behavior = new BottomSheetBehavior();
            behavior.setHideable(true);
            behavior.setPeekHeight((int) Utils.convDp2Pix(view.getContext(), 1));
            behavior.setBottomSheetCallback(mBottomSheetCallback);
            mDialogBehavior = behavior;
        } else {
            mDialogBehavior = new BaseBottomDialogFragment.StyleDialogBehavior();
        }

        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mBottomSheet.getLayoutParams();
        lp.setBehavior(mDialogBehavior);

        view.findViewById(R.id.background).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mDialogBehavior instanceof BottomSheetBehavior) {
                    ((BottomSheetBehavior) mDialogBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        }, 10);
    }

    protected boolean isBottomSheet() {
        return !Utils.isTablet(mBottomSheet.getContext()) || mAnchor == null;
    }

    private void adjustBottomSheetWidth() {
        CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) mBottomSheet.getLayoutParams();
        View child = mBottomSheet.getChildAt(0);
        child.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        lp.width = getDialogWidth();
        lp.gravity = isBottomSheet() ? Gravity.CENTER_HORIZONTAL : Gravity.LEFT;
        mBottomSheet.setLayoutParams(lp);
    }

    /**
     * Returns the width of this fragment. Called when setting layout params.
     *
     * @return the width to use if this fragment is a dialog
     */
    protected int getDialogWidth() {
        return Utils.isLandscape(mBottomSheet.getContext()) || Utils.isTablet(mBottomSheet.getContext()) ?
                mBottomSheet.getContext().getResources().getDimensionPixelSize(R.dimen.annot_style_picker_width) : ViewGroup.LayoutParams.MATCH_PARENT;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adjustBottomSheetWidth();
    }

    /**
     * Dismiss the dialog
     */
    @Override
    public void dismiss() {
        dismiss(true);
    }

    /**
     * Wrapper for dismiss where it will check lifecycle on the fragment to prevent crash when
     * dismissing while backgrounded
     *
     * @param waitBottomSheet whether to wait for bottom sheet to collapse.
     */
    public void dismiss(boolean waitBottomSheet) {
        // Not currently backgrounded or in process of backgrounded
        if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
            performDismiss(waitBottomSheet);
        } else {
            // Add listener for when the fragment gets resumed, then consume said listener
            getLifecycle().addObserver(new LifecycleEventObserver() {
                @Override
                public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                    if (event == Lifecycle.Event.ON_RESUME) {
                        performDismiss(waitBottomSheet);
                        getLifecycle().removeObserver(this);
                    }
                }
            });
        }
    }

    /**
     * Dismiss the dialog
     *
     * @param waitBottomSheet whether to wait for bottom sheet to collapse.
     */
    private void performDismiss(boolean waitBottomSheet) {
        if (waitBottomSheet && (mDialogBehavior instanceof BottomSheetBehavior)) {
            ((BottomSheetBehavior) mDialogBehavior).setState(BottomSheetBehavior.STATE_HIDDEN);
        } else {
            super.dismiss();
            if (mDismissListener != null) {
                mDismissListener.onDismiss(getDialog());
            }
        }
    }

    /**
     * @hide
     */
    public void show(@NonNull FragmentManager fragmentManager,
            @AnalyticsHandlerAdapter.StylePickerOpenedLocation int from,
            String annotation) {
        AnalyticsAnnotStylePicker.getInstance().showAnnotStyleDialog(from, annotation);
        show(fragmentManager);

        if (mDialogBehavior != null && mDialogBehavior instanceof StyleDialogBehavior) {
            ((StyleDialogBehavior) mDialogBehavior).setShowBottom(from == AnalyticsHandlerAdapter.LOCATION_ANNOTATION_TOOLBAR);
        }
    }

    /**
     * Show the dialog
     */
    public void show(
            @NonNull FragmentManager fragmentManager) {

        if (isAdded()) {
            return;
        }
        show(fragmentManager, getFragmentTag());
    }

    private class BottomSheetCallback extends BottomSheetBehavior.BottomSheetCallback {
        boolean mLocked = false;

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (mLocked && (newState == BottomSheetBehavior.STATE_DRAGGING
                    || newState == BottomSheetBehavior.STATE_COLLAPSED)) {
                ((BottomSheetBehavior) mDialogBehavior).setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (newState == BottomSheetBehavior.STATE_HIDDEN ||
                    newState == BottomSheetBehavior.STATE_COLLAPSED) {
                dismiss(false);
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }

        public boolean isLocked() {
            return mLocked;
        }

        public void setLocked(boolean locked) {
            this.mLocked = locked;
        }
    }

    private class StyleDialogBehavior extends CoordinatorLayout.Behavior<View> {
        private boolean mIsShowBottom = false;
        private boolean mInitialized = false;

        @Override
        public boolean onLayoutChild(CoordinatorLayout parent, View child, int layoutDirection) {
            if (ViewCompat.getFitsSystemWindows(parent) && !ViewCompat.getFitsSystemWindows(child)) {
                child.setFitsSystemWindows(true);
            }
            // First let the parent lay it out
            parent.onLayoutChild(child, layoutDirection);

            int margin = mBottomSheet.getContext().getResources().getDimensionPixelSize(R.dimen.padding_large);
            Rect anchor = new Rect(mAnchor);

            if (mIsAnchorInScreen) {
                int[] parentScreenPos = new int[2];
                parent.getLocationOnScreen(parentScreenPos);
                anchor.offset(-parentScreenPos[0], -parentScreenPos[1]);
            }
            int midAnchorX = anchor.left + (anchor.width() / 2);
            int midAnchorY = anchor.top + (anchor.height() / 2);

            int midPosY = midAnchorY - (child.getHeight() / 2);
            int midPosX = midAnchorX - (child.getWidth() / 2);

            int posX = 0;
            int posY = 0;
            boolean showTop = !mIsShowBottom;
            boolean showBottom = mIsShowBottom;
            boolean showLeft = false;
            boolean showRight = false;
            if (showTop) {
                posX = midPosX;
                posY = anchor.top - margin - child.getHeight();
                if (!mInitialized) {
                    mIsShowBottom = showBottom = posY < margin;
                    showTop = !showBottom;
                }
            }

            if (showBottom) {
                posX = midPosX;
                posY = anchor.bottom + margin;
                showLeft = posY + child.getHeight() > parent.getHeight();
                showBottom = !showLeft;
            }

            if (showLeft) {
                posX = anchor.left - margin - child.getWidth();
                posY = anchor.top < margin ? midPosY : anchor.top;
                showRight = posX < 0;
                showLeft = !showRight;
            }

            if (showRight) {
                posX = anchor.right + margin;
                posY = anchor.top < margin ? midPosY : anchor.top;
                showRight = !(posX + child.getWidth() > parent.getWidth());
            }

            if (!showTop && !showBottom && !showLeft && !showRight) {
                posX = midPosX;
                posY = midPosY;
            }

            if (posX < margin) {
                posX = margin;
            } else if (posX + child.getWidth() > parent.getWidth() - margin) {
                posX = parent.getWidth() - child.getWidth() - margin;
            }

            // two times margin on top because of the status bar
            if (posY < 2 * margin) {
                posY = 2 * margin;
            } else if (posY + child.getHeight() > parent.getHeight()) {
                posY = parent.getHeight() - child.getHeight();
            }

            mInitialized = true;

            ViewCompat.offsetTopAndBottom(child, posY);
            ViewCompat.offsetLeftAndRight(child, posX);

            return true;
        }

        void setShowBottom(boolean showBottom) {
            mIsShowBottom = showBottom;
            mInitialized = true;
        }
    }
}
