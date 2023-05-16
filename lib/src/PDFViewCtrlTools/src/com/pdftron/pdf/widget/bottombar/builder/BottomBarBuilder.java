package com.pdftron.pdf.widget.bottombar.builder;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;

import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;

/**
 * Builder class used to create a custom bottom bar. Used to add custom buttons to the bottom bar.
 */
public class BottomBarBuilder implements Parcelable {

    @NonNull
    private AnnotationToolbarBuilder mBuilder;

    private BottomBarBuilder() {
    }

    private BottomBarBuilder(@NonNull AnnotationToolbarBuilder builder) {
        this.mBuilder = builder;
    }

    /**
     * The tag that will be used to reference the toolbar.
     *
     * @param tag Identifier used to reference the toolbar.
     * @return this {@link BottomBarBuilder}
     */
    public static BottomBarBuilder withTag(@NonNull String tag) {
        if (tag == null) {
            throw new RuntimeException("Toolbar must have a non-null tag");
        }

        return new BottomBarBuilder(AnnotationToolbarBuilder.withTag(tag));
    }

    /**
     * Adds a custom button to the bottom bar
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link BottomBarBuilder}
     */
    public BottomBarBuilder addCustomButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        mBuilder.addCustomButton(
                title,
                icon,
                buttonId
        );
        return this;
    }

    /**
     * Adds a custom selectable button to the bottom bar
     *
     * @param title    of the button that will be shown when long pressed.
     * @param icon     of the button that will be shown in the toolbar
     * @param buttonId of the button, that is used to reference key press event. All buttons in a single
     *                 toolbar must have unique menu ids
     * @return this {@link BottomBarBuilder}
     */
    public BottomBarBuilder addCustomSelectableButton(
            @StringRes int title,
            @DrawableRes int icon,
            int buttonId) {
        mBuilder.addCustomSelectableButton(
                title,
                icon,
                buttonId
        );
        return this;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    public AnnotationToolbarBuilder getBuilder() {
        return mBuilder;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(this.mBuilder, flags);
    }

    protected BottomBarBuilder(Parcel in) {
        this.mBuilder = in.readParcelable(AnnotationToolbarBuilder.class.getClassLoader());
    }

    public static final Parcelable.Creator<BottomBarBuilder> CREATOR = new Parcelable.Creator<BottomBarBuilder>() {
        @Override
        public BottomBarBuilder createFromParcel(Parcel source) {
            return new BottomBarBuilder(source);
        }

        @Override
        public BottomBarBuilder[] newArray(int size) {
            return new BottomBarBuilder[size];
        }
    };
}
