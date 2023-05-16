package com.pdftron.pdf.widget.richtext;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProviders;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.controls.AnnotStyleDialogFragment;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.utils.ViewerUtils;
import com.pdftron.pdf.viewmodel.RichTextEvent;
import com.pdftron.pdf.viewmodel.RichTextViewModel;

import java.util.HashMap;
import java.util.List;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class RCToolbar extends FrameLayout {

    private AnnotStyleDialogFragment mAnnotStyleDialog;
    private ToolManager mToolManager;
    private RichTextViewModel mRichTextViewModel;

    private HashMap<PTRichEditor.Type, View> mButtons = new HashMap<>();

    public RCToolbar(Context context) {
        this(context, null);
    }

    public RCToolbar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RCToolbar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        init(context);
    }

    private void init(Context context) {
        if (context instanceof FragmentActivity) {
            mRichTextViewModel = ViewModelProviders.of((FragmentActivity) context).get(RichTextViewModel.class);
        }

        LayoutInflater.from(context).inflate(R.layout.rc_toolbar, this, true);
        initViews();
    }

    private void initViews() {
        findViewById(R.id.action_undo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.UNDO);
            }
        });

        findViewById(R.id.action_redo).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.REDO);
            }
        });

        final View styleView = findViewById(R.id.action_style);
        styleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleStylePicker(styleView);
            }
        });

        View view = findViewById(R.id.action_bold);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.BOLD, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.BOLD);
            }
        });

        view = findViewById(R.id.action_italic);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.ITALIC, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.ITALIC);
            }
        });

        view = findViewById(R.id.action_strikethrough);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.STRIKETHROUGH, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.STRIKE_THROUGH);
            }
        });

        view = findViewById(R.id.action_underline);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.UNDERLINE, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.UNDERLINE);
            }
        });

        findViewById(R.id.action_indent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.INDENT);
            }
        });

        findViewById(R.id.action_outdent).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.OUTDENT);
            }
        });

        view = findViewById(R.id.action_align_left);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.JUSTIFY_LEFT, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.ALIGN_LEFT);
            }
        });

        view = findViewById(R.id.action_align_center);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.JUSTIFY_CENTER, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.ALIGN_CENTER);
            }
        });

        view = findViewById(R.id.action_align_right);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.JUSTIFY_RIGHT, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.ALIGN_RIGHT);
            }
        });

        view = findViewById(R.id.action_subscript);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.SUBSCRIPT, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.SUBSCRIPT);
            }
        });

        view = findViewById(R.id.action_superscript);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.SUPERSCRIPT, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.SUPERSCRIPT);
            }
        });

        view = findViewById(R.id.action_insert_bullets);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.BULLET_LIST, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.BULLETS);
            }
        });

        view = findViewById(R.id.action_insert_numbers);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.NUMBERED_LIST, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.NUMBERS);
            }
        });

        view = findViewById(R.id.action_blockquote);
        tintBackground(view);
        mButtons.put(PTRichEditor.Type.QUOTE, view);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.BLOCK_QUOTE);
            }
        });
    }

    public void toggleStylePicker(final View which) {
        AnnotStyle annotStyle = getFreeTextAnnotStyle();
        final FragmentActivity activity = mToolManager.getCurrentActivity();
        if (null == annotStyle || null == mToolManager ||
                null == activity) {
            return;
        }
        annotStyle.setTextHTMLContent("rc");
        final AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(annotStyle)
                .setAnchorView(which)
                .setWhiteListFont(mToolManager.getFreeTextFonts())
                .setFontListFromAsset(mToolManager.getFreeTextFontsFromAssets())
                .setFontListFromStorage(mToolManager.getFreeTextFontsFromStorage())
                .build();

        if (mAnnotStyleDialog != null) {
            // prev style picker is not closed yet
            return;
        }
        mAnnotStyleDialog = popupWindow;
        popupWindow.setAnnotStyleProperties(mToolManager.getAnnotStyleProperties());
        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mAnnotStyleDialog = null;

                Context context = getContext();
                if (context == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, "");
                Tool tool = (Tool) mToolManager.getTool();
                if (tool != null) {
                    tool.setupAnnotProperty(annotStyle);
                }

                updateStyle(annotStyle);

                // show keyboard
                mRichTextViewModel.onEditorAction(RichTextEvent.Type.SHOW_KEYBOARD);
                Utils.showSoftKeyboard(activity, null);
            }
        });
        // hide keyboard
        mRichTextViewModel.onEditorAction(RichTextEvent.Type.HIDE_KEYBOARD);
        Utils.hideSoftKeyboard(activity, this);
        // show popup
        popupWindow.show(activity.getSupportFragmentManager());
    }

    public void updateStyle(@Nullable AnnotStyle annotStyle) {
        if (annotStyle != null) {
            mRichTextViewModel.onUpdateTextStyle(annotStyle);

            SharedPreferences settings = Tool.getToolPreferences(getContext());
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(ToolStyleConfig.getInstance().getTextColorKey(Annot.e_FreeText, ""), annotStyle.getTextColor());
            editor.putFloat(ToolStyleConfig.getInstance().getTextSizeKey(Annot.e_FreeText, ""), annotStyle.getTextSize());
            editor.apply();
        }
    }

    public void setToolManager(ToolManager toolManager) {
        mToolManager = toolManager;
    }

    public void deselectAll() {
        for (View view : mButtons.values()) {
            view.setSelected(false);
        }
    }

    private void tintBackground(View view) {
        Drawable drawable = getResources().getDrawable(R.drawable.rounded_corners);
        drawable.mutate();
        drawable.setColorFilter(getContext().getResources().getColor(R.color.controls_edit_toolbar_tool), PorterDuff.Mode.SRC_ATOP);
        view.setBackground(ViewerUtils.createBackgroundSelector(drawable));
    }

    private AnnotStyle getFreeTextAnnotStyle() {
        Context context = getContext();
        if (context == null) {
            return null;
        }
        return ToolStyleConfig.getInstance().getCustomAnnotStyle(context, Annot.e_FreeText, "");
    }

    public void updateDecorationTypes(List<PTRichEditor.Type> types) {
        deselectAll();
        if (types != null) {
            for (PTRichEditor.Type type : types) {
                View view = mButtons.get(type);
                if (view != null) {
                    view.setSelected(true);
                }
            }
        }
    }

    public void updateDecorationType(PTRichEditor.Type type, boolean checked) {
        if (type != null) {
            View view = mButtons.get(type);
            if (view != null) {
                view.setSelected(checked);
            }
        }
    }
}
