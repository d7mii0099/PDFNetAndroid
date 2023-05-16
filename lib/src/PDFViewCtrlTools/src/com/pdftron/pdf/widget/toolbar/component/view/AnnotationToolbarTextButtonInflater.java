package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.view.LayoutInflater;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatButton;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;

/**
 * Class in charge of inflating and styling a text button that is part
 * of the annotation toolbar.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class AnnotationToolbarTextButtonInflater {

    private AnnotationToolbarTextButtonInflater() {

    }

    @NonNull
    public static AppCompatButton inflate(@NonNull Context context, @StringRes int text) {
        AppCompatButton button = (AppCompatButton) LayoutInflater.from(context).inflate(R.layout.toolbar_text_button, null);

        AnnotationToolbarTheme theme = AnnotationToolbarTheme.fromContext(context);
        button.setTextColor(theme.textColor);
        if (text != 0) {
            button.setText(text);
        }
        return button;
    }
}
