package com.pdftron.pdf.widget.preset.component.model;

import android.content.Context;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.Annot;
import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.widget.base.BaseObservable;
import com.pdftron.pdf.widget.preset.component.view.PresetBarView;
import com.pdftron.pdf.widget.toolbar.builder.QuickMenuToolbarItem;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;

import java.util.ArrayList;
import java.util.List;

public class PresetBarState extends BaseObservable {

    @NonNull
    public final ToolbarItem toolbarItem;

    public static final int PRESET_NORMAL = 0;
    public static final int PRESET_SELECTED = 1;
    public static final int PRESET_STYLE_DIALOG_SHOWN = 2;
    public static final int PRESET_STYLE_DIALOG_DISMISSED = 3;

    public int styleDialogState;
    public boolean isVisible;
    public boolean isSinglePreset;

    @NonNull
    private final List<PresetButtonState> mPresetButtonStates = new ArrayList<>();

    private PresetBarState(@NonNull ToolbarItem toolbarItem) {
        this.toolbarItem = toolbarItem;
    }

    public static PresetBarState fromSavedState(@NonNull Context context, @NonNull ToolbarItem toolbarItem) {
        if (toolbarItem == null) {
            throw new RuntimeException("Toolbar Item must not be null");
        }

        String toolbarStyleId = toolbarItem.getStyleId();
        int toolbarButtonTypeValue = toolbarItem.toolbarButtonType.getValue();

        int presetIdx = ToolStyleConfig.getInstance().getLastSelectedPresetIndex(context, toolbarButtonTypeValue, toolbarStyleId);
        PresetBarState state = new PresetBarState(toolbarItem);

        int numAvailablePresets = Math.min(
                ToolStyleConfig.getInstance().numberOfAnnotPresetStyles(context, toolbarButtonTypeValue),
                PresetBarView.MAX_NUMBER_OF_PRESETS
        );

        List<PresetButtonState> buttonStates = new ArrayList<>();
        for (int i = 0; i < PresetBarView.MAX_NUMBER_OF_PRESETS; i++) {
            PresetButtonState presetButtonState = new PresetButtonState();
            if (toolbarButtonTypeValue == ToolbarButtonType.SMART_PEN.getValue()) {
                presetButtonState.initializeStyle(context, ToolbarButtonType.INK.getValue(), i, toolbarStyleId);
                int moreAnnotType = PdfViewCtrlSettingsManager.getAnnotStylesMoreAnnotType(context,
                        AnnotStyle.CUSTOM_SMART_PEN,
                        i,
                        toolbarStyleId,
                        Annot.e_Highlight);
                presetButtonState.addAnnotStyle(context, moreAnnotType, i, toolbarStyleId);
            } else {
                presetButtonState.initializeStyle(context, toolbarButtonTypeValue, i, toolbarStyleId);
            }
            if (i == presetIdx) {
                presetButtonState.setSelected(true);
            }
            // Only enable presets for those defined, max is 4
            if (presetButtonState.getAnnotStyles() != null) {
                for (AnnotStyle annotStyle : presetButtonState.getAnnotStyles()) {
                    annotStyle.setEnabled(i < numAvailablePresets);
                }
            }
            buttonStates.add(presetButtonState);
        }

        updatePresetFromQuickMenu(context, toolbarStyleId, toolbarButtonTypeValue, buttonStates);

        // Finally add preset button states
        for (PresetButtonState buttonState: buttonStates) {
            state.addPresetState(buttonState);
        }

        return state;
    }

    /**
     * Updates preset bar when tool is selected from quick menu.
     * For quick menu tools, first preset in preset bar will now change to last preset that was used
     * to create or update an annot of the same type. If a preset was used, then it will auto select
     * that preset instead of changing the first preset.
     */
    private static void updatePresetFromQuickMenu(@NonNull Context context, String toolbarStyleId, int toolbarButtonTypeValue, List<PresetButtonState> buttonStates) {
        if (toolbarStyleId.equals(QuickMenuToolbarItem.QUICK_MENU_TOOL_STYLE_ID) && toolbarButtonTypeValue != ToolbarButtonType.SMART_PEN.getValue()) { // ignore smart pen because it's a special case
            AnnotStyle oneTimeStyle = ToolStyleConfig.getInstance().getCustomAnnotStyle(context, toolbarButtonTypeValue, "");
            // First clear selection states
            for (PresetButtonState buttonState: buttonStates) {
                buttonState.setSelected(false);
            }
            // Then we check to see if any of the exiting presets is the same as our one time style
            boolean oneTimeStyleIsPreset = false;
            for (PresetButtonState buttonState: buttonStates) {
                AnnotStyle presetStyle = buttonState.getAnnotStyles().get(0);
                if (oneTimeStyle.equals(presetStyle)) {
                    oneTimeStyleIsPreset = true; // found preset that matches stored preset
                    buttonState.setSelected(true);
                    break;
                }
            }
            if (!oneTimeStyleIsPreset) {
                // First save the one time style to preset style at index 0
                PdfViewCtrlSettingsManager.setAnnotStylePreset(context,
                        toolbarButtonTypeValue,
                        0,
                        toolbarStyleId,
                        oneTimeStyle.toJSONString()
                );
                // Now update the preset buttons
                // Then update the first preset and update the first preset style
                PresetButtonState firstPreset = buttonStates.get(0);
                firstPreset.setSelected(true);
                ArrayList<AnnotStyle> newStyles = new ArrayList<>();
                newStyles.add(oneTimeStyle);
                firstPreset.setAnnotStyles(newStyles);
            }
        }
    }

    public void addPresetState(@NonNull PresetButtonState presetButtonState) {
        mPresetButtonStates.add(presetButtonState);
    }

    public int getNumberOfPresetStates() {
        return mPresetButtonStates.size();
    }

    @NonNull
    public PresetButtonState getPresetState(int index) {
        return mPresetButtonStates.get(index);
    }

    public boolean isPresetSelected(int index) {
        PresetButtonState presetButtonState = getPresetState(index);
        return presetButtonState.isSelected();
    }

    @Nullable
    public Pair<PresetButtonState, Integer> getActivePresetState() {
        for (int i = 0; i < getNumberOfPresetStates(); i++) {
            if (isPresetSelected(i)) {
                return new Pair<>(getPresetState(i), i);
            }
        }
        return null;
    }

    public void selectPreset(int index) {
        if (isPresetSelected(index)) { // already selected, so show annot style dialog
            styleDialogState = PRESET_SELECTED;
        } else { // not selected, so clear all selected states and do not show dialog
            styleDialogState = PRESET_NORMAL;
            deselectAll();
            PresetButtonState presetButtonState = getPresetState(index);
            presetButtonState.setSelected(true);
        }
        notifyChange();
    }

    private void deselectAll() {
        for (PresetButtonState presetButtonState : mPresetButtonStates) {
            presetButtonState.setSelected(false);
        }
    }

    public void updateAnnotStyles(@NonNull ArrayList<AnnotStyle> annotStyles, int index) {
        PresetButtonState presetButtonState = getPresetState(index);
        presetButtonState.setAnnotStyles(annotStyles);
        notifyChange();
    }

    public void openStyleDialog() {
        styleDialogState = PRESET_STYLE_DIALOG_SHOWN;
        notifyChange();
    }

    public void dismissStyleDialog() {
        styleDialogState = PRESET_STYLE_DIALOG_DISMISSED;
        notifyChange();
    }

    public void hidePresetBar() {
        isVisible = false;
        notifyChange();
    }

    public void showPresetBar() {
        isVisible = true;
        notifyChange();
    }

    public int getButtonId() {
        return toolbarItem.buttonId;
    }

    public String getToolbarStyleId() {
        return toolbarItem.getStyleId();
    }

    public int getToolbarButtonTypeId() {
        return toolbarItem.toolbarButtonType.getValue();
    }
}
