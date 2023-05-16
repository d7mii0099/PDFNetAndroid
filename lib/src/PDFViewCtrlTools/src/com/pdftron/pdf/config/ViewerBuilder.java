package com.pdftron.pdf.config;

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

import com.pdftron.pdf.controls.PdfViewCtrlTabFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment;
import com.pdftron.pdf.interfaces.builder.SkeletalFragmentBuilder;
import com.pdftron.pdf.model.BaseFileInfo;

import org.json.JSONObject;

import java.io.File;

/**
 * @deprecated use {@link ViewerBuilder2} instead.
 *
 * Builder to create a {@link PdfViewCtrlTabHostFragment}.
 */
@Deprecated
public class ViewerBuilder extends SkeletalFragmentBuilder<PdfViewCtrlTabHostFragment> {

    @NonNull
    private ViewerBuilderImpl mImpl = new ViewerBuilderImpl();

    private ViewerBuilder() {
        super();
    }

    /**
     * Create a {@link ViewerBuilder} with the specified document and password if applicable.
     *
     * @param file     Uri that specifies the location of the document
     * @param password used to open the document if needed, null otherwise
     * @return builder with the specified document and password
     * @see #withUri(Uri) for variant without a password paramter
     */
    public static ViewerBuilder withUri(@Nullable Uri file, @Nullable String password) {
        ViewerBuilder builder = new ViewerBuilder();
        builder.mImpl.withUri(file, password);
        return builder;
    }

    /**
     * @see #withUri(Uri, String)
     */
    public static ViewerBuilder withUri(@Nullable Uri file) {
        return withUri(file, null);
    }

    /**
     * Similar to {@link #withUri(Uri, String)), but with a specified File object.
     */
    public static ViewerBuilder withFile(@Nullable File file, @Nullable String password) {
        return withUri(file != null ? Uri.fromFile(file) : null, password);
    }

    /**
     * Similar to {@link #withFile(File, String)), but without a specified password.
     */
    public static ViewerBuilder withFile(@Nullable File file) {
        return withUri(file != null ? Uri.fromFile(file) : null, null);
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer tabs.
     *
     * @param tabFragmentClass the class that the viewer will used to instantiate tabs
     * @return this builder with the specified tab fragment class
     */
    public ViewerBuilder usingTabClass(@NonNull Class<? extends PdfViewCtrlTabFragment> tabFragmentClass) {
        mImpl.usingTabClass(tabFragmentClass);
        return this;
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer host fragment.
     *
     * @param tabHostClass the class that the viewer will
     * @return this builder with the specified tab host fragment class
     */
    public ViewerBuilder usingTabHostClass(@NonNull Class<? extends PdfViewCtrlTabHostFragment> tabHostClass) {
        mImpl.usingTabHostClass(tabHostClass);
        return this;
    }

    /**
     * Call to define the navigation icon used by this fragment. By default, a menu list icon is used for
     * the navigation button.
     *
     * @param navIconRes the navigation icon res
     * @return this builder with the specified navigation icon
     */
    public ViewerBuilder usingNavIcon(@DrawableRes int navIconRes) {
        mImpl.usingNavIcon(navIconRes);
        return this;
    }

    /**
     * Call to define the theme. By default, CustomAppTheme is used.
     *
     * @param theme the theme res
     * @return this builder with the specified theme
     */
    public ViewerBuilder usingTheme(@StyleRes int theme) {
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
    public ViewerBuilder usingConfig(@NonNull ViewerConfig config) {
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
    public ViewerBuilder usingCacheFolder(boolean useCacheFolder) {
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
    public ViewerBuilder usingFileType(int fileType) {
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
    public ViewerBuilder usingFileExtension(@NonNull String fileExtension) {
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
    public ViewerBuilder usingTabTitle(@Nullable String title) {
        mImpl.usingTabTitle(title);
        return this;
    }

    /**
     * Define the custom menu resources to use in document viewer toolbar.
     *
     * @param menu custom toolbar menu XML resources to use in the document viewer
     * @return this builder with the specified custom toolbar menu
     */
    public ViewerBuilder usingCustomToolbar(@MenuRes int[] menu) {
        mImpl.usingCustomToolbar(menu);
        return this;
    }

    /**
     * Sets custom headers to use with all requests.
     *
     * @param headers custom headers for all requests
     * @return this builder with the specified custom headers
     */
    public ViewerBuilder usingCustomHeaders(@Nullable JSONObject headers) {
        mImpl.usingCustomHeaders(headers);
        return this;
    }

    /**
     * Set true to enable {@link PdfViewCtrlTabHostFragment#BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public ViewerBuilder usingQuitAppMode(boolean useQuitAppMode) {
        mImpl.usingQuitAppMode(useQuitAppMode);
        return this;
    }

    @Override
    public PdfViewCtrlTabHostFragment build(@NonNull Context context) {
        return mImpl.build(context);
    }

    @Override
    public Bundle createBundle(@NonNull Context context) {
        return mImpl.createBundle(context);
    }

    @Override
    public void checkArgs(@NonNull Context context) {
        // do nothing
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mImpl, flags);
    }

    @SuppressWarnings("ConstantConditions")
    protected ViewerBuilder(Parcel in) {
        this.mImpl = in.readParcelable(ViewerBuilderImpl.class.getClassLoader());
    }

    public static final Creator<ViewerBuilder> CREATOR = new Creator<ViewerBuilder>() {
        @Override
        public ViewerBuilder createFromParcel(Parcel source) {
            return new ViewerBuilder(source);
        }

        @Override
        public ViewerBuilder[] newArray(int size) {
            return new ViewerBuilder[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ViewerBuilder that = (ViewerBuilder) o;

        return mImpl.equals(that.mImpl);
    }

    @Override
    public int hashCode() {
        return mImpl.hashCode();
    }

    private static class ViewerBuilderImpl extends BaseViewerBuilderImpl<PdfViewCtrlTabHostFragment, PdfViewCtrlTabFragment> {

        ViewerBuilderImpl() {
        }

        /**
         * {@hide}
         */
        protected ViewerBuilderImpl(Parcel in) {
            super(in);
        }

        @NonNull
        @Override
        protected Class<PdfViewCtrlTabFragment> useDefaultTabFragmentClass() {
            return PdfViewCtrlTabFragment.class;
        }

        @NonNull
        @Override
        protected Class<PdfViewCtrlTabHostFragment> useDefaultTabHostFragmentClass() {
            return PdfViewCtrlTabHostFragment.class;
        }

        @NonNull
        @Override
        protected BaseViewerBuilderImpl useBuilder() {
            return this;
        }

        public static final Creator<ViewerBuilderImpl> CREATOR = new Creator<ViewerBuilderImpl>() {
            @Override
            public ViewerBuilderImpl createFromParcel(Parcel source) {
                return new ViewerBuilderImpl(source);
            }

            @Override
            public ViewerBuilderImpl[] newArray(int size) {
                return new ViewerBuilderImpl[size];
            }
        };
    }
}
