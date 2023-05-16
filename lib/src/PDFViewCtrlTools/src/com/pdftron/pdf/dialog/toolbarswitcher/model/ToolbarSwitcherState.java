package com.pdftron.pdf.dialog.toolbarswitcher.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.pdf.widget.base.BaseObservable;
import com.pdftron.pdf.widget.toolbar.builder.AnnotationToolbarBuilder;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarItem;

import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

public class ToolbarSwitcherState extends BaseObservable {

    @NonNull
    private final List<ToolbarSwitcherItem> mToolbars;

    public ToolbarSwitcherState(@NonNull List<ToolbarSwitcherItem> toolbars) {
        mToolbars = toolbars;
        assertState();
    }

    public boolean updateToolbarSwitcherItem(@NonNull ToolbarSwitcherItem item) {
        boolean wasAdded = false;
        ListIterator<ToolbarSwitcherItem> itr = mToolbars.listIterator();
        while (itr.hasNext()) {
            ToolbarSwitcherItem toolbarSwitcherItem = itr.next();
            if (toolbarSwitcherItem.getTag().equals(item.getTag())) {
                itr.remove();
                itr.add(item);
                wasAdded = true;
                break;
            }
        }
        if (wasAdded) {
            notifyChange();
        }
        return wasAdded;
    }

    public ToolbarSwitcherItem get(int index) {
        return mToolbars.get(index);
    }

    public boolean hasMultipleToolbars() {
        return mToolbars.size() > 1;
    }

    @NonNull
    public ToolbarSwitcherItem getSelectedToolbar() {
        assertState();
        for (ToolbarSwitcherItem item : mToolbars) {
            if (item.isSelected) {
                return item;
            }
        }
        throw new RuntimeException("State check at ToolbarSwitcherState failed. 1 state must always be selected.");
    }

    /**
     * Selects the toolbar specified by its tag if available.
     *
     * @param toolbarId tag that uniquely defines a toolbar
     */
    public void selectToolbar(@NonNull String toolbarId) {
        boolean changed = false;
        boolean containsToolbar = false;
        for (ToolbarSwitcherItem toolbar : mToolbars) {
            if (toolbar.getTag().equals(toolbarId)) {
                containsToolbar = true;
                break;
            }
        }
        if (containsToolbar) {
            for (ToolbarSwitcherItem item : mToolbars) {
                if (item.getTag().equals(toolbarId)) {
                    if (item.isSelected) { // already selected
                        break;
                    } else {
                        item.isSelected = true;
                        changed = true;
                    }
                } else {
                    item.isSelected = false;
                }
            }
        }

        assertState();

        if (changed) {
            notifyChange();
        }
    }

    public void setToolbarVisibility(@NonNull String toolbarId, boolean isVisible) {
        boolean changed = false;
        for (ToolbarSwitcherItem item : mToolbars) {
            if (item.getTag().equals(toolbarId)) {
                item.setVisibility(isVisible);
                changed = true;
            }
        }

        assertState();

        if (changed) {
            notifyChange();
        }
    }

    /**
     * Returns the toolbar that contains the toolbar button with the given id. If the current toolbar
     * contains the toolbar button, then return the tag for the current toolbar. Otherwise, search
     * all toolbars in order and returns the first toolbar that contains the button.
     *
     * @param buttonId unique identifier for the toolbar button to look for
     * @return unique tag that identifies the toolbar, null if none found
     */
    @Nullable
    public String getToolbarTagWithButtonId(int buttonId) {
        // First check currently selected toolbar to see if we can find the toolbar button
        ToolbarSwitcherItem selectedToolbar = getSelectedToolbar();
        if (toolbarContainsButton(selectedToolbar, buttonId)) {
            return selectedToolbar.getTag();
        }

        // If we can't find button in current toolbar, then look through the other toolbars
        for (ToolbarSwitcherItem switcherItem : mToolbars) {
            if (toolbarContainsButton(switcherItem, buttonId)) {
                return switcherItem.getTag();
            }
        }
        return null;
    }

    private static boolean toolbarContainsButton(@NonNull ToolbarSwitcherItem toolbarSwitcherItem, int buttonId) {
        AnnotationToolbarBuilder builder = toolbarSwitcherItem.builder;
        return toolbarItemsContainsButton(builder.getToolbarItems(), buttonId) ||
                toolbarItemsContainsButton(builder.getStickyToolbarItems(), buttonId) ||
                toolbarItemsContainsButton(builder.getLeadingStickyToolbarItems(), buttonId);
    }

    private static boolean toolbarItemsContainsButton(@NonNull List<ToolbarItem> toolbarItems, int buttonId) {
        for (ToolbarItem toolbarItem : toolbarItems) {
            if (toolbarItem.buttonId == buttonId) {
                return true;
            }
        }
        return false;
    }

    private void assertIds() {
        Set<String> ids = new HashSet<>();
        for (ToolbarSwitcherItem item : mToolbars) {
            String id = item.getTag();
            if (ids.contains(id)) {
                throw new RuntimeException("Toolbars must not have duplicate ids");
            } else {
                ids.add(id);
            }
        }
    }

    private void assertState() {
        // Check that 1 item is always selected
        int numSelected = 0;
        for (ToolbarSwitcherItem item : mToolbars) {
            if (item.isSelected) {
                numSelected++;
            }
            if (numSelected > 1) {
                break;
            }
        }
        if (numSelected != 1) {
            throw new RuntimeException("State check at ToolbarSwitcherState failed. 1 state must always be selected.");
        }
    }

    public int size() {
        return mToolbars.size();
    }
}
