package com.pdftron.pdf.dialog.toolbarswitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.pdftron.pdf.dialog.toolbarswitcher.model.ToolbarSwitcherState;
import com.pdftron.pdf.widget.base.ObservingLiveData;

/**
 * View Model in charge of managing ToolbarSwitcher's state.
 */
public class ToolbarSwitcherViewModel extends ViewModel {

    ObservingLiveData<ToolbarSwitcherState> mToolbarSwitcherSetLiveData = new ObservingLiveData<>();

    public void setState(@NonNull ToolbarSwitcherState state) {
        mToolbarSwitcherSetLiveData.setValue(state);
    }

    @Nullable
    public ToolbarSwitcherState getState() {
        return mToolbarSwitcherSetLiveData.getValue();
    }

    public void selectToolbar(@NonNull String toolbarId) {
        ToolbarSwitcherState state = mToolbarSwitcherSetLiveData.getValue();
        if (state != null) {
            state.selectToolbar(toolbarId);
        }
    }

    public void hideToolbar(@NonNull String toolbarId) {
        ToolbarSwitcherState state = mToolbarSwitcherSetLiveData.getValue();
        if (state != null) {
            state.setToolbarVisibility(toolbarId, false);
        }
    }

    public void showToolbar(@NonNull String toolbarId) {
        ToolbarSwitcherState state = mToolbarSwitcherSetLiveData.getValue();
        if (state != null) {
            state.setToolbarVisibility(toolbarId, true);
        }
    }

    public void observeToolbarSwitcherState(LifecycleOwner owner, Observer<ToolbarSwitcherState> observer) {
        mToolbarSwitcherSetLiveData.observe(owner, observer);
    }
}
