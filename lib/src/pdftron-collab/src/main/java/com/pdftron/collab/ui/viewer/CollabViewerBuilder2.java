package com.pdftron.collab.ui.viewer;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcel;
import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.StyleRes;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.config.BaseViewerBuilderImpl;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.interfaces.builder.SkeletalFragmentBuilder;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.tools.AnnotManager;

import org.json.JSONObject;

/**
 * Builder to create a {@link CollabViewerTabHostFragment2}.
 */
public class CollabViewerBuilder2 extends SkeletalFragmentBuilder<CollabViewerTabHostFragment2> {

    @NonNull
    private CollabViewerBuilderImpl mImpl = new CollabViewerBuilderImpl();

    private CollabViewerBuilder2() {
        super();
    }

    /**
     * Create a {@link CollabViewerBuilder2} with the specified document and password if applicable.
     *
     * @param file     Uri that specifies the location of the document
     * @param password used to open the document if required, null otherwise
     * @return builder with the specified document and password
     * @see #withUri(Uri) for variant without a password paramter
     */
    public static CollabViewerBuilder2 withUri(@NonNull Uri file, @Nullable String password) {
        CollabViewerBuilder2 builder = new CollabViewerBuilder2();
        builder.mImpl.withUri(file, password);
        return builder;
    }

    /**
     * @see #withUri(Uri, String)
     */
    public static CollabViewerBuilder2 withUri(@NonNull Uri file) {
        return withUri(file, null);
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer tabs.
     *
     * @param tabFragmentClass the class that the viewer will used to instantiate tabs
     * @return this builder with the specified tab fragment class
     */
    public CollabViewerBuilder2 usingTabClass(@NonNull Class<? extends CollabViewerTabFragment2> tabFragmentClass) {
        mImpl.usingTabClass(tabFragmentClass);
        return this;
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer host fragment.
     *
     * @param tabHostClass the class that the viewer will
     * @return this builder with the specified tab host fragment class
     */
    public CollabViewerBuilder2 usingTabHostClass(@NonNull Class<? extends CollabViewerTabHostFragment2> tabHostClass) {
        mImpl.usingTabHostClass(tabHostClass);
        return this;
    }

    /**
     * Call to define the navigation icon used by this fragment. By default, a menu list icon is used for
     * the navigation button.
     *
     * @param navIconRes the class that the viewer will used to instantiate tabs
     * @return this builder with the specified navigation icon
     */
    public CollabViewerBuilder2 usingNavIcon(@DrawableRes int navIconRes) {
        mImpl.usingNavIcon(navIconRes);
        return this;
    }

    /**
     * Call to define the theme. By default, CustomAppTheme is used.
     *
     * @param theme the theme res
     * @return this builder with the specified theme
     */
    public CollabViewerBuilder2 usingTheme(@StyleRes int theme) {
        mImpl.usingTheme(theme);
        return this;
    }

    /**
     * Call to initialize the document viewer with a specified {@link ViewerConfig}. Multi-tab
     * is unsupported for the collab documentation viewer and must be disabled in ViewerConfig.
     *
     * @param config to initialize the document viewer
     * @return this builder with the specified {@link ViewerConfig} configurations
     */
    public CollabViewerBuilder2 usingConfig(@NonNull ViewerConfig config) {
        mImpl.usingConfig(config);
        return this;
    }

    /**
     * Call to enable or disable the use of the cache folder when creating temporary files. By default
     * the cache folder is used, and if set to false the Downloads folder is used.
     *
     * @param useCacheFolder true to enable using the cache folder, false to use the downloads folder
     * @return this builder with the specified use of the cache folder
     */
    public CollabViewerBuilder2 usingCacheFolder(boolean useCacheFolder) {
        mImpl.usingCacheFolder(useCacheFolder);
        return this;
    }

    /**
     * Call to define how the file will be handled by the document viewer. By default, this is
     * unspecified (value of 0) and the document viewer will automatically handle this; this
     * is usually called to fulfill certain requirements and will not be needed in most
     * cases.
     * <p>
     * The file types are  defined in {@link BaseFileInfo}.
     *
     * @param fileType specified to handle the file in a specific way.
     * @return this builder with the specified file type handling
     */
    public CollabViewerBuilder2 usingFileType(int fileType) {
        mImpl.usingFileType(fileType);
        return this;
    }

    /**
     * Call to define the actual extension of a file. By default, file extension is
     * obtained from the file name unless otherwise specified
     *
     * @param fileExtension actual extension of a file.
     * @return this builder with actual extension of a file
     */
    public CollabViewerBuilder2 usingFileExtension(@NonNull String fileExtension) {
        mImpl.usingFileExtension(fileExtension);
        return this;
    }

    /**
     * Call to set the tab title in the document viewer with the specified String. If null is specified,
     * then the default title handling in the document viewer will be used.
     *
     * @param title title used for the tab when viewing the specified document
     * @return this builder with the specified tab title
     */
    public CollabViewerBuilder2 usingTabTitle(@Nullable String title) {
        mImpl.usingTabTitle(title);
        return this;
    }

    /**
     * Define the custom menu resources to use in document viewer toolbar.
     *
     * @param menu custom toolbar menu XML resources to use in the document viewer
     * @return this builder with the specified custom toolbar menu
     */
    public CollabViewerBuilder2 usingCustomToolbar(@MenuRes int[] menu) {
        mImpl.usingCustomToolbar(menu);
        return this;
    }

    /**
     * Sets custom headers to use with all requests.
     *
     * @param headers custom headers for all requests
     * @return this builder with the specified custom headers
     */
    public CollabViewerBuilder2 usingCustomHeaders(@Nullable JSONObject headers) {
        mImpl.usingCustomHeaders(headers);
        return this;
    }

    /**
     * Sets annotation manager undo mode:
     * {@link com.pdftron.pdf.PDFViewCtrl.AnnotationManagerMode#ADMIN_UNDO_OWN} :
     * in this mode, you can undo only your own changes
     * {@link com.pdftron.pdf.PDFViewCtrl.AnnotationManagerMode#ADMIN_UNDO_OTHERS} :
     * in this mode, you can undo everyone's changes
     * Default to ADMIN_UNDO_OWN.
     *
     * @param mode one of ADMIN_UNDO_OWN and ADMIN_UNDO_OTHERS
     * @return this builder with the specified mode
     */
    public CollabViewerBuilder2 usingAnnotationManagerUndoMode(@NonNull PDFViewCtrl.AnnotationManagerMode mode) {
        mImpl.usingAnnotationManagerUndoMode(mode);
        return this;
    }

    /**
     * Sets annotation manager edit mode:
     * {@link com.pdftron.pdf.tools.AnnotManager.EditPermissionMode#EDIT_OWN} :
     * in this mode, you can edit only your own changes
     * {@link com.pdftron.pdf.tools.AnnotManager.EditPermissionMode#EDIT_OTHERS} :
     * in this mode, you can edit everyone's changes
     * Default to EDIT_OWN.
     *
     * @param mode one of EDIT_OWN and EDIT_OTHERS
     * @return this builder with the specified mode
     */
    public CollabViewerBuilder2 usingAnnotationManagerEditMode(@NonNull AnnotManager.EditPermissionMode mode) {
        mImpl.usingAnnotationManagerEditMode(mode);
        return this;
    }

    /**
     * Set true to enable {@link CollabViewerTabHostFragment2#BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public CollabViewerBuilder2 usingQuitAppMode(boolean useQuitAppMode) {
        mImpl.usingQuitAppMode(useQuitAppMode);
        return this;
    }

    @Override
    public CollabViewerTabHostFragment2 build(@NonNull Context context) {
        return mImpl.build(context);
    }

    @Override
    public Bundle createBundle(@NonNull Context context) {
        return mImpl.createBundle(context);
    }

    /**
     * Helpers to check builder parameters
     */

    @Override
    public void checkArgs(@NonNull Context context) {
        mImpl.checkArgs(context);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mImpl, flags);
    }

    protected CollabViewerBuilder2(Parcel in) {
        this.mImpl = in.readParcelable(CollabViewerBuilderImpl.class.getClassLoader());
    }

    public static final Creator<CollabViewerBuilder2> CREATOR = new Creator<CollabViewerBuilder2>() {
        @Override
        public CollabViewerBuilder2 createFromParcel(Parcel source) {
            return new CollabViewerBuilder2(source);
        }

        @Override
        public CollabViewerBuilder2[] newArray(int size) {
            return new CollabViewerBuilder2[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollabViewerBuilder2 that = (CollabViewerBuilder2) o;

        return mImpl.equals(that.mImpl);
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    private static class CollabViewerBuilderImpl extends BaseViewerBuilderImpl<CollabViewerTabHostFragment2, CollabViewerTabFragment2> {

        CollabViewerBuilderImpl() {
        }

        /**
         * {@hide}
         */
        protected CollabViewerBuilderImpl(Parcel in) {
            super(in);
        }

        @Override
        public void checkArgs(@NonNull Context context) {
            super.checkArgs(context);
            if (mConfig == null) {
                // Create default viewer config
                mConfig = getDefaultCollabConfigBuilder(context)
                        .build();
            } else {
                if (mConfig.isMultiTabEnabled()) {
                    throw new IllegalStateException("Multi tab option must be disabled in ViewerConfig, for the collaboration viewer.");
                }
            }
        }

        private static ViewerConfig.Builder getDefaultCollabConfigBuilder(@NonNull Context context) {
            return new ViewerConfig.Builder()
                    .multiTabEnabled(false)     // multi-tabs unsupported for collab fragment
                    .showCloseTabOption(false)  // multi-tabs unsupported for collab fragment
                    .saveCopyExportPath(context.getFilesDir().getAbsolutePath())
                    .openUrlCachePath(context.getFilesDir().getAbsolutePath());
        }

        @NonNull
        @Override
        protected Class<CollabViewerTabFragment2> useDefaultTabFragmentClass() {
            return CollabViewerTabFragment2.class;
        }

        @NonNull
        @Override
        protected Class<CollabViewerTabHostFragment2> useDefaultTabHostFragmentClass() {
            return CollabViewerTabHostFragment2.class;
        }

        @NonNull
        @Override
        protected BaseViewerBuilderImpl useBuilder() {
            return this;
        }

        public static final Creator<CollabViewerBuilderImpl> CREATOR = new Creator<CollabViewerBuilderImpl>() {
            @Override
            public CollabViewerBuilderImpl createFromParcel(Parcel source) {
                return new CollabViewerBuilderImpl(source);
            }

            @Override
            public CollabViewerBuilderImpl[] newArray(int size) {
                return new CollabViewerBuilderImpl[size];
            }
        };
    }
}
