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

import com.pdftron.pdf.config.BaseViewerBuilderImpl;
import com.pdftron.pdf.config.ViewerConfig;
import com.pdftron.pdf.interfaces.builder.SkeletalFragmentBuilder;
import com.pdftron.pdf.model.BaseFileInfo;

import org.json.JSONObject;

/**
 * @deprecated use {@link CollabViewerBuilder2} instead
 *
 * Builder to create a {@link CollabViewerTabHostFragment}.
 */
@Deprecated
public class CollabViewerBuilder extends SkeletalFragmentBuilder<CollabViewerTabHostFragment> {

    @NonNull
    private CollabViewerBuilderImpl mImpl = new CollabViewerBuilderImpl();

    private CollabViewerBuilder() {
        super();
    }

    /**
     * Create a {@link CollabViewerBuilder} with the specified document and password if applicable.
     *
     * @param file     Uri that specifies the location of the document
     * @param password used to open the document if required, null otherwise
     * @return builder with the specified document and password
     * @see #withUri(Uri) for variant without a password paramter
     */
    public static CollabViewerBuilder withUri(@NonNull Uri file, @Nullable String password) {
        CollabViewerBuilder builder = new CollabViewerBuilder();
        builder.mImpl.withUri(file, password);
        return builder;
    }

    /**
     * @see #withUri(Uri, String)
     */
    public static CollabViewerBuilder withUri(@NonNull Uri file) {
        return withUri(file, null);
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer tabs.
     *
     * @param tabFragmentClass the class that the viewer will used to instantiate tabs
     * @return this builder with the specified tab fragment class
     */
    public CollabViewerBuilder usingTabClass(@NonNull Class<? extends CollabViewerTabFragment> tabFragmentClass) {
        mImpl.usingTabClass(tabFragmentClass);
        return this;
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer host fragment.
     *
     * @param tabHostClass the class that the viewer will
     * @return this builder with the specified tab host fragment class
     */
    public CollabViewerBuilder usingTabHostClass(@NonNull Class<? extends CollabViewerTabHostFragment> tabHostClass) {
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
    public CollabViewerBuilder usingNavIcon(@DrawableRes int navIconRes) {
        mImpl.usingNavIcon(navIconRes);
        return this;
    }

    /**
     * Call to define the theme. By default, CustomAppTheme is used.
     *
     * @param theme the theme res
     * @return this builder with the specified theme
     */
    public CollabViewerBuilder usingTheme(@StyleRes int theme) {
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
    public CollabViewerBuilder usingConfig(@NonNull ViewerConfig config) {
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
    public CollabViewerBuilder usingCacheFolder(boolean useCacheFolder) {
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
    public CollabViewerBuilder usingFileType(int fileType) {
        mImpl.usingFileType(fileType);
        return this;
    }

    /**
     * Call to set the tab title in the document viewer with the specified String. If null is specified,
     * then the default title handling in the document viewer will be used.
     *
     * @param title title used for the tab when viewing the specified document
     * @return this builder with the specified tab title
     */
    public CollabViewerBuilder usingTabTitle(@Nullable String title) {
        mImpl.usingTabTitle(title);
        return this;
    }

    /**
     * Define the custom menu resources to use in document viewer toolbar.
     *
     * @param menu custom toolbar menu XML resources to use in the document viewer
     * @return this builder with the specified custom toolbar menu
     */
    public CollabViewerBuilder usingCustomToolbar(@MenuRes int[] menu) {
        mImpl.usingCustomToolbar(menu);
        return this;
    }

    /**
     * Sets custom headers to use with all requests.
     *
     * @param headers custom headers for all requests
     * @return this builder with the specified custom headers
     */
    public CollabViewerBuilder usingCustomHeaders(@Nullable JSONObject headers) {
        mImpl.usingCustomHeaders(headers);
        return this;
    }

    /**
     * Set true to enable {@link CollabViewerTabHostFragment#BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public CollabViewerBuilder usingQuitAppMode(boolean useQuitAppMode) {
        mImpl.usingQuitAppMode(useQuitAppMode);
        return this;
    }

    @Override
    public CollabViewerTabHostFragment build(@NonNull Context context) {
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

    protected CollabViewerBuilder(Parcel in) {
        this.mImpl = in.readParcelable(CollabViewerBuilderImpl.class.getClassLoader());
    }

    public static final Creator<CollabViewerBuilder> CREATOR = new Creator<CollabViewerBuilder>() {
        @Override
        public CollabViewerBuilder createFromParcel(Parcel source) {
            return new CollabViewerBuilder(source);
        }

        @Override
        public CollabViewerBuilder[] newArray(int size) {
            return new CollabViewerBuilder[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CollabViewerBuilder that = (CollabViewerBuilder) o;

        return mImpl.equals(that.mImpl);
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    private static class CollabViewerBuilderImpl extends BaseViewerBuilderImpl<CollabViewerTabHostFragment, CollabViewerTabFragment> {

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
        protected Class<CollabViewerTabFragment> useDefaultTabFragmentClass() {
            return CollabViewerTabFragment.class;
        }

        @NonNull
        @Override
        protected Class<CollabViewerTabHostFragment> useDefaultTabHostFragmentClass() {
            return CollabViewerTabHostFragment.class;
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
