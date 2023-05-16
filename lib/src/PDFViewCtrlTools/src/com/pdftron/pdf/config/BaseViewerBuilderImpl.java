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
import androidx.fragment.app.Fragment;

import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.controls.PdfViewCtrlTabFragment2;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostBaseFragment;
import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment2;
import com.pdftron.pdf.interfaces.builder.SkeletalFragmentBuilder;
import com.pdftron.pdf.model.BaseFileInfo;
import com.pdftron.pdf.tools.AnnotManager;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.utils.cache.UriCacheManager;

import org.json.JSONObject;

import java.util.Arrays;

/**
 * A base implementation for builder classes used to create objects that subclass
 * {@link PdfViewCtrlTabHostFragment2}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class BaseViewerBuilderImpl<TH extends Fragment, T extends Fragment> extends SkeletalFragmentBuilder<TH> {

    @Nullable
    protected String mTabTitle;                   // optional for builder, specified title to use
    @Nullable
    protected Uri mFile;                          // optional for builder, will show an empty viewer
    @Nullable
    protected String mPassword;                   // optional for builder
    protected boolean mUseCacheFolder = true;     // default should use cache directory
    protected boolean mUseQuitAppMode = false;       // default should not close viewer when done viewing
    @Nullable
    protected ViewerConfig mConfig;               // default value is null
    @DrawableRes
    protected int mNavigationIcon = R.drawable.ic_arrow_back_white_24dp;   // default uses a back arrow icon
    @StyleRes
    protected int mTheme = 0;
    @Nullable
    protected int[] mCustomToolbarMenu = null;
    protected int mFileType = BaseFileInfo.FILE_TYPE_UNKNOWN;
    @Nullable
    protected String mFileExtension = null;

    @Nullable
    protected Class<T> mTabFragmentClass;
    @Nullable
    protected Class<TH> mTabHostFragmentClass;
    @Nullable
    protected String mCustomHeaders;              // optional for builder
    protected int mAnnotationManagerUndoMode = PDFViewCtrl.AnnotationManagerMode.ADMIN_UNDO_OWN.getValue();
    @NonNull
    protected String mAnnotationManagerEditMode = AnnotManager.EditPermissionMode.EDIT_OWN.name();

    protected BaseViewerBuilderImpl() {
        super();
    }

    @NonNull
    protected abstract Class<T> useDefaultTabFragmentClass();

    @NonNull
    protected abstract Class<TH> useDefaultTabHostFragmentClass();

    @NonNull
    protected abstract BaseViewerBuilderImpl useBuilder();

    /**
     * Create a {@link ViewerBuilder2} with the specified document and password if applicable.
     *
     * @param file     Uri that specifies the location of the document
     * @param password used to open the document if needed, null otherwise
     * @return builder with the specified document and password
     */
    public BaseViewerBuilderImpl withUri(@Nullable Uri file, @Nullable String password) {
        mFile = file;
        mPassword = password;
        return this;
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer tabs.
     *
     * @param tabClass the class that the viewer will used to instantiate tabs
     * @return this builder with the specified tab fragment class
     */
    public BaseViewerBuilderImpl usingTabClass(@NonNull Class<? extends T> tabClass) {
        useBuilder().mTabFragmentClass = tabClass;
        return useBuilder();
    }

    /**
     * Call to define the fragment class that will be used to instantiate viewer host fragment.
     *
     * @param tabHostClass the class that the viewer will
     * @return this builder with the specified tab host fragment class
     */
    public BaseViewerBuilderImpl usingTabHostClass(@NonNull Class<? extends TH> tabHostClass) {
        useBuilder().mTabHostFragmentClass = tabHostClass;
        return useBuilder();
    }

    /**
     * Call to define the navigation icon used by this fragment. By default, a menu list icon is used for
     * the navigation button.
     *
     * @return this builder with the specified navigation icon
     */
    public BaseViewerBuilderImpl usingNavIcon(@DrawableRes int navIconRes) {
        useBuilder().mNavigationIcon = navIconRes;
        return useBuilder();
    }

    /**
     * Call to define the theme used by this fragment. By default, CustomAppTheme is used.
     *
     * @return this builder with the specified theme
     */
    public BaseViewerBuilderImpl usingTheme(@StyleRes int theme) {
        useBuilder().mTheme = theme;
        return useBuilder();
    }

    /**
     * Call to initialize the document viewer with a specified {@link ViewerConfig}. Multi-tab
     * is unsupported for the collab documentation viewer and must be disabled in ViewerConfig.
     *
     * @param config to initialize the document viewer
     * @return this builder with the specified {@link ViewerConfig} configurations
     */
    public BaseViewerBuilderImpl usingConfig(@NonNull ViewerConfig config) {
        useBuilder().mConfig = config;
        return useBuilder();
    }

    /**
     * Call to enable or disable the use of the cache folder when creating temporary files. By default
     * the cache folder is used, and if set to false the Downloads folder is used.
     *
     * @param useCacheFolder true to enable using the cache folder, false to use the downloads folder
     * @return this builder with the specified use of the cache folder
     */
    public BaseViewerBuilderImpl usingCacheFolder(boolean useCacheFolder) {
        useBuilder().mUseCacheFolder = useCacheFolder;
        return useBuilder();
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
    public BaseViewerBuilderImpl usingFileType(int fileType) {
        useBuilder().mFileType = fileType;
        return useBuilder();
    }

    /**
     * Call to define the actual extension of a file. By default, file extension is
     * obtained from the file name unless otherwise specified
     * <p>
     * The file types are  defined in {@link BaseFileInfo}.
     *
     * @param fileExtension actual extension of a file.
     * @return this builder with actual extension of a file
     */
    public BaseViewerBuilderImpl usingFileExtension(@NonNull String fileExtension) {
        useBuilder().mFileExtension = fileExtension;
        return useBuilder();
    }

    /**
     * Call to set the tab title in the document viewer with the specified String. If null is specified,
     * then the default title handling in the document viewer will be used.
     *
     * @param title title used for the tab when viewing the specified document
     * @return this builder with the specified tab title
     */
    public BaseViewerBuilderImpl usingTabTitle(@Nullable String title) {
        useBuilder().mTabTitle = title;
        return useBuilder();
    }

    /**
     * Define the custom menu resources to use in document viewer toolbar.
     *
     * @param menu custom toolbar menu XML resources to use in the document viewer
     * @return this builder with the specified custom toolbar menu
     */
    public BaseViewerBuilderImpl usingCustomToolbar(@MenuRes int[] menu) {
        useBuilder().mCustomToolbarMenu = menu;
        return useBuilder();
    }

    /**
     * Sets custom headers to use with all requests.
     *
     * @param headers custom headers for all requests
     * @return this builder with the specified custom headers
     */
    public BaseViewerBuilderImpl usingCustomHeaders(@Nullable JSONObject headers) {
        useBuilder().mCustomHeaders = headers != null ? headers.toString() : null;
        return useBuilder();
    }

    /**
     * Sets the annotation manager undo mode
     *
     * @param mode the annotation manager undo mode
     * @return this builder with the specified annotation manager undo mode
     */
    public BaseViewerBuilderImpl usingAnnotationManagerUndoMode(@NonNull PDFViewCtrl.AnnotationManagerMode mode) {
        useBuilder().mAnnotationManagerUndoMode = mode.getValue();
        return useBuilder();
    }

    /**
     * Sets the annotation manager edit mode
     *
     * @param mode the annotation manager edit mode
     * @return this builder with the specified annotation manager edit mode
     */
    public BaseViewerBuilderImpl usingAnnotationManagerEditMode(@NonNull AnnotManager.EditPermissionMode mode) {
        useBuilder().mAnnotationManagerEditMode = mode.name();
        return useBuilder();
    }

    /**
     * Set true to enable {@link PdfViewCtrlTabHostFragment2#BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING}
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public BaseViewerBuilderImpl usingQuitAppMode(boolean useQuitAppMode) {
        useBuilder().mUseQuitAppMode = useQuitAppMode;
        return useBuilder();
    }

    @Override
    public TH build(@NonNull Context context) {
        return build(context, mTabHostFragmentClass == null ? useDefaultTabHostFragmentClass() : mTabHostFragmentClass);
    }

    @Override
    public Bundle createBundle(@NonNull Context context) {
        Bundle args;
        if (mFile == null) {
            args = new Bundle();
        } else {
            args = PdfViewCtrlTabFragment2.createBasicPdfViewCtrlTabBundle(context, mFile, mPassword, mConfig);
            if (mFileType != BaseFileInfo.FILE_TYPE_UNKNOWN) {
                args.putInt(PdfViewCtrlTabFragment2.BUNDLE_TAB_ITEM_SOURCE, mFileType);
            }
        }
        if (mTabTitle != null) {
            args.putString(PdfViewCtrlTabFragment2.BUNDLE_TAB_TITLE, mTabTitle);
        }
        args.putSerializable(PdfViewCtrlTabHostBaseFragment.BUNDLE_TAB_FRAGMENT_CLASS, mTabFragmentClass == null ? useDefaultTabFragmentClass() : mTabFragmentClass);
        args.putParcelable(PdfViewCtrlTabHostBaseFragment.BUNDLE_TAB_HOST_CONFIG, mConfig);
        args.putInt(PdfViewCtrlTabHostBaseFragment.BUNDLE_TAB_HOST_NAV_ICON, mNavigationIcon);
        args.putInt(PdfViewCtrlTabHostBaseFragment.BUNDLE_THEME, mTheme);
        args.putBoolean(UriCacheManager.BUNDLE_USE_CACHE_FOLDER, mUseCacheFolder);
        args.putIntArray(PdfViewCtrlTabHostBaseFragment.BUNDLE_TAB_HOST_TOOLBAR_MENU, mCustomToolbarMenu);
        args.putBoolean(PdfViewCtrlTabHostBaseFragment.BUNDLE_TAB_HOST_QUIT_APP_WHEN_DONE_VIEWING, mUseQuitAppMode);
        if (mCustomHeaders != null) {
            args.putString(PdfViewCtrlTabFragment2.BUNDLE_TAB_CUSTOM_HEADERS, mCustomHeaders);
        }
        args.putInt(PdfViewCtrlTabFragment2.BUNDLE_TAB_ANNOTATION_MANAGER_UNDO_MODE, mAnnotationManagerUndoMode);
        args.putString(PdfViewCtrlTabFragment2.BUNDLE_TAB_ANNOTATION_MANAGER_EDIT_MODE, mAnnotationManagerEditMode);
        if (mFileExtension != null) {
            args.putString(PdfViewCtrlTabFragment2.BUNDLE_TAB_FILE_EXTENSION, mFileExtension);
        }

        return args;
    }

    @Override
    public void checkArgs(@NonNull Context context) {
    }

    // Parcelable methods

    /**
     * {@hide}
     */
    @SuppressWarnings("unchecked")
    protected BaseViewerBuilderImpl(Parcel in) {
        this.mTabTitle = in.readString();
        this.mFile = in.readParcelable(Uri.class.getClassLoader());
        this.mPassword = in.readString();
        this.mUseCacheFolder = in.readByte() != 0;
        this.mUseQuitAppMode = in.readByte() != 0;
        this.mConfig = in.readParcelable(ViewerConfig.class.getClassLoader());
        this.mNavigationIcon = in.readInt();
        this.mTheme = in.readInt();
        this.mCustomToolbarMenu = in.createIntArray();
        this.mFileType = in.readInt();
        this.mFileExtension = in.readString();
        this.mTabFragmentClass = (Class<T>) in.readSerializable();
        this.mTabHostFragmentClass = (Class<TH>) in.readSerializable();
        this.mCustomHeaders = in.readString();
        this.mAnnotationManagerUndoMode = in.readInt();
        this.mAnnotationManagerEditMode = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.mTabTitle);
        dest.writeParcelable(this.mFile, flags);
        dest.writeString(this.mPassword);
        dest.writeByte(this.mUseCacheFolder ? (byte) 1 : (byte) 0);
        dest.writeByte(this.mUseQuitAppMode ? (byte) 1 : (byte) 0);
        dest.writeParcelable(this.mConfig, flags);
        dest.writeInt(this.mNavigationIcon);
        dest.writeInt(this.mTheme);
        dest.writeIntArray(this.mCustomToolbarMenu);
        dest.writeInt(this.mFileType);
        dest.writeString(this.mFileExtension);
        dest.writeSerializable(this.mTabFragmentClass);
        dest.writeSerializable(this.mTabHostFragmentClass);
        dest.writeString(this.mCustomHeaders);
        dest.writeInt(this.mAnnotationManagerUndoMode);
        dest.writeString(this.mAnnotationManagerEditMode);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BaseViewerBuilderImpl<?, ?> that = (BaseViewerBuilderImpl<?, ?>) o;

        if (mUseCacheFolder != that.mUseCacheFolder) return false;
        if (mUseQuitAppMode != that.mUseQuitAppMode) return false;
        if (mNavigationIcon != that.mNavigationIcon) return false;
        if (mTheme != that.mTheme) return false;
        if (mFileType != that.mFileType) return false;
        if (mFileExtension != null ? !mFileExtension.equals(that.mFileExtension) : that.mFileExtension != null)
            return false;
        if (mAnnotationManagerUndoMode != that.mAnnotationManagerUndoMode) return false;
        if (!mAnnotationManagerEditMode.equals(that.mAnnotationManagerEditMode)) return false;
        if (mTabTitle != null ? !mTabTitle.equals(that.mTabTitle) : that.mTabTitle != null)
            return false;
        if (mFile != null ? !mFile.equals(that.mFile) : that.mFile != null) return false;
        if (mPassword != null ? !mPassword.equals(that.mPassword) : that.mPassword != null)
            return false;
        if (mConfig != null ? !mConfig.equals(that.mConfig) : that.mConfig != null) return false;
        if (!Arrays.equals(mCustomToolbarMenu, that.mCustomToolbarMenu)) return false;
        if (mTabFragmentClass != null ? !mTabFragmentClass.equals(that.mTabFragmentClass) : that.mTabFragmentClass != null)
            return false;
        if (mTabHostFragmentClass != null ? !mTabHostFragmentClass.equals(that.mTabHostFragmentClass) : that.mTabHostFragmentClass != null)
            return false;
        return mCustomHeaders != null ? mCustomHeaders.equals(that.mCustomHeaders) : that.mCustomHeaders == null;
    }

    @Override
    public int hashCode() {
        int result = mTabTitle != null ? mTabTitle.hashCode() : 0;
        result = 31 * result + (mFile != null ? mFile.hashCode() : 0);
        result = 31 * result + (mPassword != null ? mPassword.hashCode() : 0);
        result = 31 * result + (mUseCacheFolder ? 1 : 0);
        result = 31 * result + (mUseQuitAppMode ? 1 : 0);
        result = 31 * result + (mConfig != null ? mConfig.hashCode() : 0);
        result = 31 * result + mNavigationIcon;
        result = 31 * result + mTheme;
        result = 31 * result + Arrays.hashCode(mCustomToolbarMenu);
        result = 31 * result + mFileType;
        result = 31 * result + (mFileExtension != null ? mFileExtension.hashCode() : 0);
        result = 31 * result + (mTabFragmentClass != null ? mTabFragmentClass.hashCode() : 0);
        result = 31 * result + (mTabHostFragmentClass != null ? mTabHostFragmentClass.hashCode() : 0);
        result = 31 * result + (mCustomHeaders != null ? mCustomHeaders.hashCode() : 0);
        result = 31 * result + mAnnotationManagerUndoMode;
        result = 31 * result + mAnnotationManagerEditMode.hashCode();
        return result;
    }
}
