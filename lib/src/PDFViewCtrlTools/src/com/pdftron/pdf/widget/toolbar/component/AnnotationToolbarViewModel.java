package com.pdftron.pdf.widget.toolbar.component;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.widget.base.BaseObservable;
import com.pdftron.pdf.widget.base.ObservingLiveData;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;

import java.util.HashSet;
import java.util.Set;

/**
 * View model in charge of handling the annotation toolbar's state
 */
public class AnnotationToolbarViewModel extends AndroidViewModel {

    private ObservingLiveData<ObservableAnnotationToolbarBuilder> mBuilder = new ObservingLiveData<>();

    protected ObservingLiveData<DisabledButtonTypes> mDisabledButtonTypes = new ObservingLiveData<>(new DisabledButtonTypes());
    private ObservingLiveData<DisabledToolModes> mDisabledToolModes = new ObservingLiveData<>(new DisabledToolModes()); // needed for backwards compatibility

    public AnnotationToolbarViewModel(@NonNull Application application) {
        super(application);
    }

    public void updateState() {
        mBuilder.setValue(mBuilder.getValue());
    }

    /**
     * Sets the {@link AnnotationToolbarBuilder} associated with the annotation toolbar
     *
     * @param builder to set
     */
    public void setAnnotationToolbarBuilder(@NonNull AnnotationToolbarBuilder builder) {
        mBuilder.setValue(new ObservableAnnotationToolbarBuilder(builder));
    }

    /**
     * Sets the visibility of toolbar buttons with given {@link ToolbarButtonType}
     *
     * @param buttonType the button type too set visibility
     * @param visibility true if buttons should be visible, false otherwise
     */
    public void setToolbarButtonVisibility(@NonNull ToolbarButtonType buttonType, boolean visibility) {
        DisabledButtonTypes disabledButtonTypes = mDisabledButtonTypes.getValue();
        if (disabledButtonTypes != null) {
            disabledButtonTypes.setToolbarButtonVisibility(buttonType, visibility);
        }
    }

    /**
     * Sets the set of {@link com.pdftron.pdf.tools.ToolManager.ToolMode} that should be
     * disabled in the annotation toolbar.
     *
     * @param disabledToolModesSet the set of {@link com.pdftron.pdf.tools.ToolManager.ToolMode} to disable
     */
    public void setToolModeFilter(@NonNull Set<ToolManager.ToolMode> disabledToolModesSet) {
        DisabledToolModes disabledToolModes = mDisabledToolModes.getValue();
        if (disabledToolModes != null) {
            disabledToolModes.setDisabledToolModes(disabledToolModesSet);
        }
    }

    /**
     * Observe when the {link AnnotationToolbarBuilder} is changed.
     *
     * @param owner    The LifecycleOwner which controls the observer
     * @param observer The observer that will receive the events
     */
    public void observeBuilderState(@NonNull LifecycleOwner owner, @NonNull final Observer<AnnotationToolbarBuilder> observer) {
        mBuilder.observe(owner, new Observer<ObservableAnnotationToolbarBuilder>() {
            @Override
            public void onChanged(ObservableAnnotationToolbarBuilder observableAnnotationToolbarBuilder) {
                if (observableAnnotationToolbarBuilder != null) {
                    AnnotationToolbarBuilder builder = observableAnnotationToolbarBuilder.mBuilder.copy();

                    // Disable all tools/buttons via either tool mode or button type
                    DisabledToolModes disabledToolModes = mDisabledToolModes.getValue();
                    if (disabledToolModes != null) {
                        builder = builder.removeButtons(disabledToolModes.getDisabledToolModes());
                    }
                    DisabledButtonTypes disabledButtonTypes = mDisabledButtonTypes.getValue();
                    if (disabledButtonTypes != null) {
                        builder = builder.copyWithoutToolbarItems(disabledButtonTypes.getToolbarButtonTypesToHide());
                    }

                    observer.onChanged(builder);
                }
            }
        });
    }

    /**
     * Observe when the set of disabled {@link com.pdftron.pdf.tools.ToolManager.ToolMode}s change.
     *
     * @param owner    The LifecycleOwner which controls the observer
     * @param observer The observer that will receive the events
     */
    public void observeDisabledToolModesState(@NonNull LifecycleOwner owner, @NonNull Observer<DisabledToolModes> observer) {
        mDisabledToolModes.observe(owner, observer);
    }

    /**
     * Observe when the set of disabled {@link ToolbarButtonType}s change.
     *
     * @param owner    The LifecycleOwner which controls the observer
     * @param observer The observer that will receive the events
     */
    public void observeHiddenButtonTypesState(@NonNull LifecycleOwner owner, @NonNull Observer<DisabledButtonTypes> observer) {
        mDisabledButtonTypes.observe(owner, observer);
    }

    /**
     * Wraps AnnotationToolbarBuilder so it's observable when changes happen.
     */
    public static class ObservableAnnotationToolbarBuilder extends BaseObservable {
        @NonNull
        private AnnotationToolbarBuilder mBuilder;

        public ObservableAnnotationToolbarBuilder() {
            super();
            throw new RuntimeException("Should not be called without builder");
        }

        public ObservableAnnotationToolbarBuilder(@NonNull AnnotationToolbarBuilder builder) {
            mBuilder = builder;
        }

        public void setAnnotationToolbarBuilder(@NonNull AnnotationToolbarBuilder builder) {
            mBuilder = builder;
            notifyChange();
        }

        @NonNull
        public AnnotationToolbarBuilder getBuilder() {
            return mBuilder;
        }
    }

    /**
     * Wraps our list of disabled {@link ToolbarButtonType} so that it's observable when changes happen.
     */
    public static class DisabledButtonTypes extends BaseObservable {
        @NonNull
        private Set<ToolbarButtonType> mToolbarButtonTypesToHide = new HashSet<>();

        public void setDisabledButtonTypes(@NonNull Set<ToolbarButtonType> disabledButtonTypes) {
            mToolbarButtonTypesToHide = disabledButtonTypes;
            notifyChange();
        }

        public void setToolbarButtonVisibility(@NonNull ToolbarButtonType buttonType, boolean visibility) {
            if (visibility) {
                mToolbarButtonTypesToHide.remove(buttonType);
            } else {
                mToolbarButtonTypesToHide.add(buttonType);
            }
            notifyChange();
        }

        public Set<ToolbarButtonType> getToolbarButtonTypesToHide() {
            return mToolbarButtonTypesToHide;
        }
    }

    /**
     * Wraps our list of disabled {@link com.pdftron.pdf.tools.ToolManager.ToolMode} so that
     * it's observable when changes happen.
     */
    public static class DisabledToolModes extends BaseObservable {
        @NonNull
        private Set<ToolManager.ToolMode> mDisabledToolModes = new HashSet<>();

        public void setDisabledToolModes(@NonNull Set<ToolManager.ToolMode> disabledToolModes) {
            mDisabledToolModes = disabledToolModes;
            notifyChange();
        }

        @NonNull
        public Set<ToolManager.ToolMode> getDisabledToolModes() {
            return mDisabledToolModes;
        }
    }
}
