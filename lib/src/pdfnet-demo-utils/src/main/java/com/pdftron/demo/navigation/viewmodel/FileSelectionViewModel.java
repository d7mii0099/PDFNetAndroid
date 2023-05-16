package com.pdftron.demo.navigation.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.pdftron.pdf.model.FileInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class FileSelectionViewModel extends ViewModel {

    @NonNull
    public final MutableLiveData<ArrayList<FileInfo>> mSelectedFiles = new MutableLiveData<>();

    public void setSelectedFiles(ArrayList<FileInfo> selectedFiles) {
        mSelectedFiles.setValue(selectedFiles);
    }

    public void addSelectedFiles(ArrayList<FileInfo> selectedFiles) {
        if (mSelectedFiles.getValue() != null) {
            Set<FileInfo> newFiles = new HashSet<>(mSelectedFiles.getValue());
            newFiles.addAll(selectedFiles);
            setSelectedFiles(new ArrayList<>(newFiles));
        } else {
            setSelectedFiles(selectedFiles);
        }
    }

    public void observe(LifecycleOwner lifecycleOwner, Observer<ArrayList<FileInfo>> observer) {
        mSelectedFiles.observe(lifecycleOwner, observer);
    }
}
