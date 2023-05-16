package com.pdftron.pdf.widget.toolbar.component.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.widget.toolbar.component.AnnotationToolbarTheme;

public class UndoActionButton extends ActionButton {
    public UndoActionButton(@NonNull Context context) {
        this(context, null);
    }

    public UndoActionButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public UndoActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public UndoActionButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        setIcon(context.getResources().getDrawable(R.drawable.ic_undo_black_24dp));
        AnnotationToolbarTheme annotationToolbarTheme = AnnotationToolbarTheme.fromContext(context);
        setIconColor(annotationToolbarTheme.iconColor);
        setDisabledIconColor(annotationToolbarTheme.disabledIconColor);
        setHasOption(true);
    }
}
