package com.pdftron.demo.browser.db.file;

import com.pdftron.demo.browser.ui.FilterSettingsViewModel;
import com.pdftron.pdf.model.FileInfo;

import java.util.List;

import io.reactivex.Flowable;

public interface FileDataSource {

    Flowable<List<FileEntity>> getFilesFlowable(QueryParams queryParams);

    void delete(FileInfo fileInfo);

    void add(FileInfo fileInfo);

    void clear();

    class QueryParams {
        final String searchQuery;
        final List<Integer> documentTypes;
        final FilterSettingsViewModel.SortMode sortMode;
        final int gridCount;

        public QueryParams(String searchQuery, List<Integer> documentTypes, FilterSettingsViewModel.SortMode sortMode, int gridCount) {
            this.searchQuery = searchQuery;
            this.documentTypes = documentTypes;
            this.sortMode = sortMode;
            this.gridCount = gridCount;
        }
    }
}
