package com.pdftron.pdf.controls;

import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.fragment.app.FragmentActivity;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.config.ToolConfig;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.tools.Pan;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.RubberStampCreate;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.AnalyticsParam;
import com.pdftron.pdf.utils.ShortcutHelper;

import java.util.ArrayList;

public class FormToolbar extends BaseToolbar implements
        ToolManager.ToolChangedListener {

    protected String mSelectedButtonExtra;

    /**
     * Starts with prepare form toolbar
     */
    public static final int START_MODE_PREPARE_FORM_TOOLBAR = 0;
    /**
     * Starts with fill and sign toolbar
     */
    public static final int START_MODE_FILL_AND_SIGN_TOOLBAR = 1;

    // DO NOT CHANGE
    // THESE ARE PAGE LABELS
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String sCHECK_MARK_LABEL = RubberStampCreate.sCHECK_MARK_LABEL;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String sCROSS_LABEL = RubberStampCreate.sCROSS_LABEL;
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final String sDOT_LABEL = RubberStampCreate.sDOT_LABEL;

    public interface FormToolbarListener {

        /**
         * Called when the form toolbar has been closed.
         */
        void onFormToolbarWillClose();
    }

    private FormToolbarListener mFormToolbarListener;

    private AnnotStyleDialogFragment mAnnotStyleDialog;

    public FormToolbar(@NonNull Context context) {
        this(context, null);
    }

    public FormToolbar(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.form_toolbar);
    }

    public FormToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, R.style.FormToolbarStyle);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public FormToolbar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        // initialize colors
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FormToolbar, defStyleAttr, defStyleRes);
        try {
            mToolbarBackgroundColor = typedArray.getColor(R.styleable.FormToolbar_colorBackground, Color.BLACK);
            mToolbarToolBackgroundColor = typedArray.getColor(R.styleable.FormToolbar_colorToolBackground, Color.BLACK);
            mToolbarToolIconColor = typedArray.getColor(R.styleable.FormToolbar_colorToolIcon, Color.WHITE);
            mToolbarCloseIconColor = typedArray.getColor(R.styleable.FormToolbar_colorCloseIcon, Color.WHITE);
        } finally {
            typedArray.recycle();
        }

        LayoutInflater.from(context).inflate(R.layout.controls_form_toolbar, this, true);

        setBackgroundColor(mToolbarBackgroundColor);
    }

    /**
     * Sets the {@link FormToolbarListener} listener.
     *
     * @param listener The listener
     */
    public void setFormToolbarListener(FormToolbarListener listener) {
        mFormToolbarListener = listener;
    }

    /**
     * Sets whether the form toolbar is in prepare form mode or fill and sign mode
     *
     * @param mode the mode
     */
    public void setMode(int mode) {

        View prepareFormLayout = findViewById(R.id.prepare_form_layout);
        View fillAndSignLayout = findViewById(R.id.fill_and_sign_layout);

        if (mode == START_MODE_PREPARE_FORM_TOOLBAR) {
            prepareFormLayout.setVisibility(VISIBLE);
            fillAndSignLayout.setVisibility(GONE);
        } else {
            prepareFormLayout.setVisibility(GONE);
            fillAndSignLayout.setVisibility(VISIBLE);
        }
    }

    public void setup(@NonNull ToolManager toolManager) {
        mToolManager = toolManager;

        initButtons();

        mToolManager.addToolChangedListener(this);

        // Force tool to be Pan when using the toolbar.
        mToolManager.setTool(mToolManager.createTool(ToolManager.ToolMode.PAN, null));
        initSelectedButton();

        setVisibility(View.GONE);
    }

    public void selectTool(View view, int id) {
        Context context = getContext();
        if (context == null || mToolManager == null) {
            return;
        }

        View button = findViewById(id);
        if (null == button) {
            return;
        }

        ToolManager.ToolMode toolMode = null;
        int analyticId = -1;
        mSelectedButtonExtra = null;
        if (id == R.id.controls_form_field_toolbar_widget_text) {
            toolMode = ToolManager.ToolMode.FORM_TEXT_FIELD_CREATE;
            analyticId = AnalyticsHandlerAdapter.FORM_TOOL_TEXT;
        } else if (id == R.id.controls_form_field_toolbar_widget_checkbox) {
            toolMode = ToolManager.ToolMode.FORM_CHECKBOX_CREATE;
            analyticId = AnalyticsHandlerAdapter.FORM_TOOL_CHECKBOX;
        } else if (id == R.id.controls_form_field_toolbar_widget_signature) {
            toolMode = ToolManager.ToolMode.FORM_SIGNATURE_CREATE;
            analyticId = AnalyticsHandlerAdapter.FORM_TOOL_SIGNATURE;
        } else if (id == R.id.controls_form_field_toolbar_widget_radio) {
            toolMode = ToolManager.ToolMode.FORM_RADIO_GROUP_CREATE;
            analyticId = AnalyticsHandlerAdapter.FORM_TOOL_RADIOGROUP;
        } else if (id == R.id.controls_form_field_toolbar_widget_listbox) {
            toolMode = ToolManager.ToolMode.FORM_LIST_BOX_CREATE;
            analyticId = AnalyticsHandlerAdapter.FORM_TOOL_LISTBOX;
        } else if (id == R.id.controls_form_field_toolbar_widget_combobox) {
            toolMode = ToolManager.ToolMode.FORM_COMBO_BOX_CREATE;
            analyticId = AnalyticsHandlerAdapter.FORM_TOOL_COMBOBOX;
        } else if (id == R.id.controls_fill_and_sign_toolbar_text) {
            toolMode = ToolManager.ToolMode.FREE_TEXT_SPACING_CREATE;
            analyticId = AnalyticsHandlerAdapter.FILL_AND_SIGN_TOOL_TEXT;
        } else if (id == R.id.controls_fill_and_sign_toolbar_signature) {
            toolMode = ToolManager.ToolMode.SIGNATURE;
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_SIGNATURE;
        } else if (id == R.id.controls_fill_and_sign_toolbar_date) {
            toolMode = ToolManager.ToolMode.FREE_TEXT_DATE_CREATE;
            analyticId = AnalyticsHandlerAdapter.FILL_AND_SIGN_TOOL_DATE;
        } else if (id == R.id.controls_fill_and_sign_toolbar_checkmark) {
            toolMode = ToolManager.ToolMode.RUBBER_STAMPER;
            analyticId = AnalyticsHandlerAdapter.FILL_AND_SIGN_TOOL_CHECKMARK;
            mSelectedButtonExtra = sCHECK_MARK_LABEL;
        } else if (id == R.id.controls_fill_and_sign_toolbar_cross) {
            toolMode = ToolManager.ToolMode.RUBBER_STAMPER;
            analyticId = AnalyticsHandlerAdapter.FILL_AND_SIGN_TOOL_CROSS;
            mSelectedButtonExtra = sCROSS_LABEL;
        } else if (id == R.id.controls_fill_and_sign_toolbar_dot) {
            toolMode = ToolManager.ToolMode.RUBBER_STAMPER;
            analyticId = AnalyticsHandlerAdapter.FILL_AND_SIGN_TOOL_DOT;
            mSelectedButtonExtra = sDOT_LABEL;
        }
        boolean sendAnalytics = mSelectedButtonId != id;
        if (toolMode != null) {
            if (mSelectedButtonId == id && hasStyle(getToolOfResourceId(id))) {
                AnnotStyle annotStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(context, getAnnotTypeOfResourceId(id), "");
                if (annotStyle == null) {
                    return;
                }
                AnnotStyleDialogFragment popupWindow = new AnnotStyleDialogFragment.Builder(annotStyle)
                        .setAnchorView(button)
                        .setWhiteListFont(mToolManager.getFreeTextFonts())
                        .build();
                showAnnotPropertyPopup(popupWindow, view, analyticId);
            }
            mToolManager.setTool(mToolManager.createTool(toolMode, mToolManager.getTool()));
            ToolManager.Tool tool = mToolManager.getTool();
            ((Tool) tool).setForceSameNextToolMode(mButtonStayDown);
            if (tool instanceof RubberStampCreate && mSelectedButtonExtra != null) {
                ((RubberStampCreate) tool).setStampName(mSelectedButtonExtra);
            }
            selectButton(id);
            mEventAction = true;
        } else if (id == R.id.controls_annotation_toolbar_tool_pan) {
            analyticId = AnalyticsHandlerAdapter.ANNOTATION_TOOL_PAN;
            mToolManager.setTool(mToolManager.createTool(ToolManager.ToolMode.PAN, null)); // reset
            selectButton(id);
        } else if (id == R.id.controls_annotation_toolbar_btn_close) {
            sendAnalytics = false;
            close();
            mEventAction = false;
        }

        if (sendAnalytics) {
            AnalyticsHandlerAdapter.getInstance().sendEvent(AnalyticsHandlerAdapter.EVENT_FORM_FIELD_TOOLBAR, AnalyticsParam.annotationToolbarParam(analyticId));
        }
    }

    public void show(@Nullable ToolManager.ToolMode toolMode) {
        Context context = getContext();
        if (context == null) {
            return;
        }

        updateButtonsVisibility();
        setVisibility(View.VISIBLE);

        if (toolMode != null) {
            mSelectedToolId = getResourceIdOfTool(toolMode, null);
        }

        if (mSelectedToolId != -1) {
            selectTool(null, mSelectedToolId);
            mSelectedToolId = -1;
        }
    }

    public void close() {
        closePopups();
        if (mFormToolbarListener != null) {
            mFormToolbarListener.onFormToolbarWillClose();
        }
    }

    public void setButtonStayDown(boolean value) {
        mButtonStayDown = value;
    }

    /**
     * Updates the visibility of the buttons on the form toolbar.
     */
    public void updateButtonsVisibility() {
        Context context = getContext();
        if (context == null || mToolManager == null) {
            return;
        }

        for (View button : mButtons) {
            int viewId = button.getId();
            safeShowHideButton(viewId, VISIBLE);
        }
    }

    public boolean handleKeyUp(int keyCode,
            KeyEvent event) {

        Tool tool = (Tool) mToolManager.getTool();
        if (tool == null) {
            return false;
        }

        if (findViewById(R.id.controls_annotation_toolbar_tool_pan).isShown()
                && !(tool instanceof Pan)
                && ShortcutHelper.isCancelTool(keyCode, event)) {
            closePopups();
            selectTool(null, R.id.controls_annotation_toolbar_tool_pan);
            return true;
        }

        if (findViewById(R.id.controls_annotation_toolbar_btn_close).isShown()
                && ShortcutHelper.isCloseMenu(keyCode, event)) {
            closePopups();
            selectTool(null, R.id.controls_annotation_toolbar_btn_close);
            return true;
        }

        return false;
    }

    /**
     * Closes the popup windows.
     */
    public void closePopups() {
        if (mAnnotStyleDialog != null) {
            mAnnotStyleDialog.dismiss();
            mAnnotStyleDialog = null;
        }
    }

    @Override
    public void toolChanged(ToolManager.Tool newTool, ToolManager.Tool oldTool) {
        if (newTool == null || !isShowing()) {
            return;
        }

        boolean canSelectButton = false;

        if (oldTool != null && oldTool instanceof Tool && newTool instanceof Tool) {
            Tool oldT = (Tool) oldTool;
            Tool newT = (Tool) newTool;
            canSelectButton = !oldT.isForceSameNextToolMode() || !newT.isEditAnnotTool();
        }

        if (canSelectButton) {
            ToolManager.ToolMode newToolMode = ToolManager.getDefaultToolMode(newTool.getToolMode());
            selectButton(getResourceIdOfTool(newToolMode, mSelectedButtonExtra));
            if (newTool instanceof RubberStampCreate && mSelectedButtonExtra != null) {
                ((RubberStampCreate) newTool).setStampName(mSelectedButtonExtra);
            }
        }
    }

    private void safeShowHideButton(int viewId, int visibility) {
        View button = findViewById(viewId);
        ToolManager.ToolMode toolMode = ToolConfig.getInstance().getToolModeByAnnotationToolbarItemId(viewId);
        if (toolMode != null && button != null) {
            if (mToolManager.isToolModeDisabled(toolMode)) {
                button.setVisibility(GONE);
            } else {
                button.setVisibility(visibility);
            }
        }
    }

    private void showAnnotPropertyPopup(final AnnotStyleDialogFragment popupWindow, View view, final int annotToolId) {
        if (view == null || popupWindow == null) {
            return;
        }
        if (mToolManager.isSkipNextTapEvent()) {
            mToolManager.resetSkipNextTapEvent();
            return;
        }

        if (mAnnotStyleDialog != null) {
            // prev style picker is not closed yet
            return;
        }
        mAnnotStyleDialog = popupWindow;

        popupWindow.setAnnotStyleProperties(mToolManager.getAnnotStyleProperties());
        popupWindow.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                mAnnotStyleDialog = null;

                Context context = getContext();
                if (context == null || mToolManager == null) {
                    return;
                }

                AnnotStyle annotStyle = popupWindow.getAnnotStyle();
                ToolStyleConfig.getInstance().saveAnnotStyle(context, annotStyle, "");

                Tool tool = (Tool) mToolManager.getTool();
                if (tool != null) {
                    tool.setupAnnotProperty(annotStyle);
                }
            }
        });

        FragmentActivity activity = null;
        if (getContext() instanceof FragmentActivity) {
            activity = (FragmentActivity) getContext();
        } else if (mToolManager.getCurrentActivity() != null) {
            activity = mToolManager.getCurrentActivity();
        }
        if (activity == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(new Exception("AnnotationToolbar is not attached to with an Activity"));
            return;
        }
        popupWindow.show(activity.getSupportFragmentManager(), AnalyticsHandlerAdapter.STYLE_PICKER_LOC_ANNOT_TOOLBAR,
                AnalyticsHandlerAdapter.getInstance().getAnnotationTool(annotToolId));
    }

    private void initSelectedButton() {
        if (mToolManager == null) {
            return;
        }

        // Let's make the button selected according to the current tool
        ToolManager.ToolMode toolMode = ToolManager.getDefaultToolMode(mToolManager.getTool().getToolMode());
        selectButton(getResourceIdOfTool(toolMode, null));
    }

    private ToolManager.ToolMode getToolOfResourceId(int resId) {
        if (resId == R.id.controls_form_field_toolbar_widget_text) {
            return ToolManager.ToolMode.FORM_TEXT_FIELD_CREATE;
        } else if (resId == R.id.controls_form_field_toolbar_widget_checkbox) {
            return ToolManager.ToolMode.FORM_CHECKBOX_CREATE;
        } else if (resId == R.id.controls_form_field_toolbar_widget_radio) {
            return ToolManager.ToolMode.FORM_RADIO_GROUP_CREATE;
        } else if (resId == R.id.controls_form_field_toolbar_widget_signature) {
            return ToolManager.ToolMode.FORM_SIGNATURE_CREATE;
        } else if (resId == R.id.controls_form_field_toolbar_widget_listbox) {
            return ToolManager.ToolMode.FORM_LIST_BOX_CREATE;
        } else if (resId == R.id.controls_form_field_toolbar_widget_combobox) {
            return ToolManager.ToolMode.FORM_COMBO_BOX_CREATE;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_text) {
            return ToolManager.ToolMode.FREE_TEXT_SPACING_CREATE;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_signature) {
            return ToolManager.ToolMode.SIGNATURE;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_date) {
            return ToolManager.ToolMode.FREE_TEXT_DATE_CREATE;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_checkmark ||
                resId == R.id.controls_fill_and_sign_toolbar_cross ||
                resId == R.id.controls_fill_and_sign_toolbar_dot) {
            return ToolManager.ToolMode.RUBBER_STAMPER;
        }
        return ToolManager.ToolMode.PAN;
    }

    private int getResourceIdOfTool(ToolManager.ToolMode toolMode, @Nullable String extra) {
        switch (toolMode) {
            case FORM_TEXT_FIELD_CREATE:
                return R.id.controls_form_field_toolbar_widget_text;
            case FORM_CHECKBOX_CREATE:
                return R.id.controls_form_field_toolbar_widget_checkbox;
            case FORM_RADIO_GROUP_CREATE:
                return R.id.controls_form_field_toolbar_widget_radio;
            case FORM_SIGNATURE_CREATE:
                return R.id.controls_form_field_toolbar_widget_signature;
            case FORM_LIST_BOX_CREATE:
                return R.id.controls_form_field_toolbar_widget_listbox;
            case FORM_COMBO_BOX_CREATE:
                return R.id.controls_form_field_toolbar_widget_combobox;
            case SIGNATURE:
                return R.id.controls_fill_and_sign_toolbar_signature;
            case FREE_TEXT_SPACING_CREATE:
                return R.id.controls_fill_and_sign_toolbar_text;
            case FREE_TEXT_DATE_CREATE:
                return R.id.controls_fill_and_sign_toolbar_date;
            case RUBBER_STAMPER:
                if (sCHECK_MARK_LABEL.equals(extra)) {
                    return R.id.controls_fill_and_sign_toolbar_checkmark;
                } else if (sCROSS_LABEL.equals(extra)) {
                    return R.id.controls_fill_and_sign_toolbar_cross;
                } else if (sDOT_LABEL.equals(extra)) {
                    return R.id.controls_fill_and_sign_toolbar_dot;
                } else {
                    return 0;
                }
            case PAN:
            default:
                return R.id.controls_annotation_toolbar_tool_pan;
        }
    }

    private int getDrawableIdOfTool(ToolManager.ToolMode toolMode, String extra) {
        switch (toolMode) {
            case FORM_TEXT_FIELD_CREATE:
                return R.drawable.ic_text_fields_black_24dp;
            case FORM_CHECKBOX_CREATE:
                return R.drawable.ic_check_box_black_24dp;
            case FORM_RADIO_GROUP_CREATE:
                return R.drawable.ic_radio_button_checked_black_24dp;
            case FORM_SIGNATURE_CREATE:
                return R.drawable.ic_annotation_signature_field;
            case FORM_LIST_BOX_CREATE:
                return R.drawable.ic_annotation_listbox_black;
            case FORM_COMBO_BOX_CREATE:
                return R.drawable.ic_annotation_combo_black;
            case SIGNATURE:
                return R.drawable.ic_annotation_signature_black_24dp;
            case FREE_TEXT_SPACING_CREATE:
                return R.drawable.ic_fill_and_sign_spacing_text;
            case FREE_TEXT_DATE_CREATE:
                return R.drawable.ic_date_range_24px;
            case RUBBER_STAMPER:
                if (sCHECK_MARK_LABEL.equals(extra)) {
                    return R.drawable.ic_fill_and_sign_checkmark;
                } else if (sCROSS_LABEL.equals(extra)) {
                    return R.drawable.ic_fill_and_sign_crossmark;
                } else if (sDOT_LABEL.equals(extra)) {
                    return R.drawable.ic_fill_and_sign_dot;
                } else {
                    return 0;
                }
            case PAN:
            default:
                return R.drawable.ic_pan_black_24dp;
        }
    }

    private int getAnnotTypeOfResourceId(int resId) {
        if (resId == R.id.controls_form_field_toolbar_widget_text ||
                resId == R.id.controls_form_field_toolbar_widget_checkbox ||
                resId == R.id.controls_form_field_toolbar_widget_radio ||
                resId == R.id.controls_form_field_toolbar_widget_signature ||
                resId == R.id.controls_form_field_toolbar_widget_listbox ||
                resId == R.id.controls_form_field_toolbar_widget_combobox) {
            return Annot.e_Widget;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_text) {
            return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_signature) {
            return AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_date) {
            return AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE;
        } else if (resId == R.id.controls_fill_and_sign_toolbar_checkmark ||
                resId == R.id.controls_fill_and_sign_toolbar_cross ||
                resId == R.id.controls_fill_and_sign_toolbar_dot) {
            return Annot.e_Stamp;
        }
        return Annot.e_Unknown;
    }

    private void initButtons() {
        Context context = getContext();
        if (context == null || mToolManager == null) {
            return;
        }

        initializeButtons();

        ArrayList<ToolItem> tools = new ArrayList<>();

        // prepare form
        addTools(tools, ToolManager.ToolMode.FORM_TEXT_FIELD_CREATE);
        addTools(tools, ToolManager.ToolMode.FORM_SIGNATURE_CREATE);
        addTools(tools, ToolManager.ToolMode.FORM_CHECKBOX_CREATE);
        addTools(tools, ToolManager.ToolMode.FORM_RADIO_GROUP_CREATE);
        addTools(tools, ToolManager.ToolMode.FORM_LIST_BOX_CREATE);
        addTools(tools, ToolManager.ToolMode.FORM_COMBO_BOX_CREATE);

        // fill and sign
        addTools(AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_SPACING, tools, ToolManager.ToolMode.FREE_TEXT_SPACING_CREATE, null);
        addTools(Annot.e_Stamp, tools, ToolManager.ToolMode.SIGNATURE, null);
        addTools(AnnotStyle.CUSTOM_ANNOT_TYPE_FREE_TEXT_DATE, tools, ToolManager.ToolMode.FREE_TEXT_DATE_CREATE, null);
        addTools(Annot.e_Stamp, tools, ToolManager.ToolMode.RUBBER_STAMPER, sCHECK_MARK_LABEL);
        addTools(Annot.e_Stamp, tools, ToolManager.ToolMode.RUBBER_STAMPER, sCROSS_LABEL);
        addTools(Annot.e_Stamp, tools, ToolManager.ToolMode.RUBBER_STAMPER, sDOT_LABEL);

        // pan icon
        tools.add(new ToolItem(-1,
                R.id.controls_annotation_toolbar_tool_pan,
                R.drawable.ic_pan_black_24dp,
                false));
        // close icon
        tools.add(new ToolItem(-1,
                R.id.controls_annotation_toolbar_btn_close,
                R.drawable.ic_close_black_24dp,
                false, mToolbarCloseIconColor));

        int width = getToolWidth();
        int height = getToolHeight();

        Drawable spinnerBitmapDrawable = getSpinnerBitmapDrawable(context,
                width, height, mToolbarToolBackgroundColor, false);
        Drawable normalBitmapDrawable = getNormalBitmapDrawable(context,
                width, height, mToolbarToolBackgroundColor, false);

        for (ToolItem tool : tools) {
            setViewDrawable(context, tool.id, tool.spinner, tool.drawable,
                    spinnerBitmapDrawable, normalBitmapDrawable,
                    tool.color);
        }
    }

    private boolean hasStyle(ToolManager.ToolMode toolMode) {
        return ToolManager.ToolMode.FORM_TEXT_FIELD_CREATE == toolMode ||
                ToolManager.ToolMode.FORM_LIST_BOX_CREATE == toolMode ||
                ToolManager.ToolMode.FORM_COMBO_BOX_CREATE == toolMode ||
                ToolManager.ToolMode.FREE_TEXT_SPACING_CREATE == toolMode ||
                ToolManager.ToolMode.FREE_TEXT_DATE_CREATE == toolMode;
    }

    private void addTools(ArrayList<ToolItem> tools, ToolManager.ToolMode toolMode) {
        addTools(Annot.e_Widget, tools, toolMode, null);
    }

    private void addTools(int annotType, ArrayList<ToolItem> tools, ToolManager.ToolMode toolMode, @Nullable String extra) {
        tools.add(new ToolItem(annotType,
                getResourceIdOfTool(toolMode, extra),
                getDrawableIdOfTool(toolMode, extra),
                hasStyle(toolMode)));
    }

    @Override
    void addButtons() {
        safeAddButtons(R.id.controls_form_field_toolbar_widget_text);
        safeAddButtons(R.id.controls_form_field_toolbar_widget_checkbox);
        safeAddButtons(R.id.controls_form_field_toolbar_widget_signature);
        safeAddButtons(R.id.controls_form_field_toolbar_widget_radio);
        safeAddButtons(R.id.controls_form_field_toolbar_widget_listbox);
        safeAddButtons(R.id.controls_form_field_toolbar_widget_combobox);

        safeAddButtons(R.id.controls_fill_and_sign_toolbar_text);
        safeAddButtons(R.id.controls_fill_and_sign_toolbar_signature);
        safeAddButtons(R.id.controls_fill_and_sign_toolbar_date);
        safeAddButtons(R.id.controls_fill_and_sign_toolbar_checkmark);
        safeAddButtons(R.id.controls_fill_and_sign_toolbar_cross);
        safeAddButtons(R.id.controls_fill_and_sign_toolbar_dot);

        safeAddButtons(R.id.controls_annotation_toolbar_tool_pan);
        safeAddButtons(R.id.controls_annotation_toolbar_btn_close);
    }

    private int getToolWidth() {
        View panBtn = findViewById(R.id.controls_annotation_toolbar_tool_pan);
        if (panBtn != null) {
            panBtn.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            return panBtn.getMeasuredWidth();
        }
        return 0;
    }

    private int getToolHeight() {
        View panBtn = findViewById(R.id.controls_annotation_toolbar_tool_pan);
        if (panBtn != null) {
            panBtn.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
            return panBtn.getMeasuredHeight();
        }
        return 0;
    }
}
