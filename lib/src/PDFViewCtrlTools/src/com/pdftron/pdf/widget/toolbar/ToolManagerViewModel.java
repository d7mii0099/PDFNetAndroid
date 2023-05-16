package com.pdftron.pdf.widget.toolbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.pdftron.pdf.tools.Pan;
import com.pdftron.pdf.tools.Tool;
import com.pdftron.pdf.tools.ToolManager;

/**
 * View Model for {@link com.pdftron.pdf.tools.ToolManager} that allows you to observe tool change events.
 */
public class ToolManagerViewModel extends ViewModel {

    private MutableLiveData<ToolManager> mToolManager = new MutableLiveData<>();
    private MutableLiveData<ToolChange> mSelectedTool = new MutableLiveData<>();
    private MutableLiveData<ToolSet> mToolSet = new MutableLiveData<>();

    private ToolManager.ToolChangedListener mListener = new ToolManager.ToolChangedListener() {
        @Override
        public void toolChanged(ToolManager.Tool newTool, ToolManager.Tool oldTool) {
            if (newTool instanceof Pan) {
                ((Pan) newTool).enablePresetMode();
            }
            mSelectedTool.setValue(new ToolChange((Tool) oldTool, (Tool) newTool));
        }
    };

    private ToolManager.ToolSetListener mToolSetListener = new ToolManager.ToolSetListener() {
        @Override
        public void onToolSet(ToolManager.Tool newTool) {
            mToolSet.setValue(new ToolSet((Tool) newTool));
        }
    };

    public void setToolManager(@Nullable ToolManager toolManager) {
        if (mToolManager.getValue() != null) {
            mToolManager.getValue().removeToolCreatedListener(mListener);
            mToolManager.getValue().removeToolSetListener(mToolSetListener);
        }
        mToolManager.setValue(toolManager);
        if (toolManager != null) {
            mSelectedTool.setValue(null);
            toolManager.addToolCreatedListener(mListener);
            toolManager.addToolSetListener(mToolSetListener);
            mSelectedTool.setValue(new ToolChange(null, (Tool) toolManager.getTool()));
            mToolSet.setValue(new ToolSet((Tool) toolManager.getTool()));
        }
    }

    @Nullable
    public ToolManager getToolManager() {
        return mToolManager.getValue();
    }

    public void observeToolChanges(@NonNull LifecycleOwner owner, @NonNull Observer<ToolChange> observer) {
        mSelectedTool.observe(owner, observer);
    }

    public void observeToolSet(@NonNull LifecycleOwner owner, @NonNull Observer<ToolSet> observer) {
        mToolSet.observe(owner, observer);
    }

    public void observeToolManagerChanges(@NonNull LifecycleOwner owner, @NonNull Observer<ToolManager> observer) {
        mToolManager.observe(owner, observer);
    }

    @Nullable
    public ToolManager.Tool getTool() {
        return mSelectedTool.getValue() == null ? null : mSelectedTool.getValue().newTool;
    }

    public static final class ToolChange {
        @Nullable
        public final Tool oldTool;
        @Nullable
        public final Tool newTool;

        public ToolChange(@Nullable Tool oldTool, @Nullable Tool newTool) {
            this.oldTool = oldTool;
            this.newTool = newTool;
        }
    }

    public static final class ToolSet {
        @Nullable
        public final Tool newTool;

        public ToolSet(@Nullable Tool newTool) {
            this.newTool = newTool;
        }
    }
}
