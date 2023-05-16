package com.pdftron.demo.browser.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Environment;
import android.view.Menu;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.pdftron.demo.R;
import com.pdftron.demo.browser.db.file.FileEntity;
import com.pdftron.demo.dialog.FilePickerDialogFragment;
import com.pdftron.pdf.model.FileInfo;
import com.pdftron.pdf.utils.RequestCode;
import com.pdftron.pdf.utils.Utils;

public class BackupViewFragment extends LocalFileViewFragment {

    public static BackupViewFragment newInstance() {
        return new BackupViewFragment();
    }

    @Override
    public void moveCurrentFile() {
        FragmentActivity activity = getActivity();
        if (activity == null || mSelectedFile == null) {
            return;
        }

        FilePickerDialogFragment dialogFragment = FilePickerDialogFragment.newInstance(RequestCode.MOVE_FILE,
                Environment.getExternalStorageDirectory());
        dialogFragment.setLocalFolderListener(this);
        dialogFragment.setExternalFolderListener(this);
        dialogFragment.setStyle(DialogFragment.STYLE_NORMAL, R.style.CustomAppTheme);
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            dialogFragment.show(fragmentManager, "file_picker_dialog_fragment");
        }
    }

    @Override
    public void onFileClicked(final FileInfo fileInfo) {
        mSelectedFile = fileInfo;
        moveCurrentFile();
    }
    @Override
    protected void setShowInfoButton(AllFilesListAdapter allFilesListAdapter) {
        allFilesListAdapter.setShowInfoButton(false);
    }

    @Override
    protected void setShowInfoButton(AllFilesGridAdapter allFilesGridAdapter) {
        allFilesGridAdapter.setShowInfoButton(false);
    }

    @Override
    protected void setStickyHeaderSettings(StickyHeader stickyHeader) {
        stickyHeader.setBackupFolder(true);
    }

    @Override
    protected AllFilesListAdapter createNewAllFilesListAdapter(@NonNull Context context) {
        return new AllFilesListAdapter(context, true, true);
    }

    @Override
    protected void handleBackupBanner() {
        mBinding.backupBanner.setVisibility(View.VISIBLE);
    }

    @Override
    protected void handleStickyHeader() {
        mBinding.stickyHeader.setBackupFolder(true);
    }

    @Override
    protected void handleFabMenuUI() {
        mBinding.fabMenu.setVisibility(View.GONE);
    }

    @Override
    protected boolean shouldListItemBeFiltered(FileEntity fileEntity) {
        if (getContext() != null) {
            return !fileEntity.getFilePath().contains(getContext().getExternalFilesDir(null).toString());
        } else {
            return false;
        }
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        itemDelete.setVisible(true);
        mode.setTitle(Utils.getLocaleDigits(Integer.toString(mFileInfoSelectedList.size())));
        return true;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mode.getMenuInflater().inflate(R.menu.cab_fragment_backup_view, menu);

        itemDelete = menu.findItem(R.id.cab_file_delete);
        return true;
    }
}
