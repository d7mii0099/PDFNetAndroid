package com.pdftron.pdf.widget.richtext;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.PopupWindow;

import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.ToolManager;

import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RCContainer extends PopupWindow {

    private RCToolbar mRCToolbar;

    public RCContainer(Context context) {
        super(context);

        View root = LayoutInflater.from(context).inflate(R.layout.rc_container_popupwindow, null, false);
        setContentView(root);

        setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setInputMethodMode(PopupWindow.INPUT_METHOD_NEEDED);

        setWidth(WindowManager.LayoutParams.MATCH_PARENT);
        setHeight(WindowManager.LayoutParams.WRAP_CONTENT);

        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        mRCToolbar = root.findViewById(R.id.rc_toolbar);
    }

    public void setup(ToolManager toolManager) {
        mRCToolbar.setToolManager(toolManager);
    }

    public void updateToolbar(PTRichEditor.Type type, boolean checked) {
        mRCToolbar.updateDecorationType(type, checked);
    }

    @Override
    public void dismiss() {
        super.dismiss();
        mRCToolbar.deselectAll();
    }
}
