package com.pdftron.demo.browser.ui;

import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.NonNull;

import com.pdftron.demo.R;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.ViewerUtils;

class FilterSettingsView {

    private final MenuItem mGridToggle;

    private final MenuItem mList;
    private final MenuItem mColumn1;
    private final MenuItem mColumn2;
    private final MenuItem mColumn3;
    private final MenuItem mColumn4;
    private final MenuItem mColumn5;
    private final MenuItem mColumn6;

    private final MenuItem mFilterAll;
    private final MenuItem mFilterPdf;
    private final MenuItem mFilterOffice;
    private final MenuItem mFilterImage;
    private final MenuItem mFilterText;
    private final MenuItem mSortByName;
    private final MenuItem mSortByDate;

    public FilterSettingsView(@NonNull Context context, @NonNull Menu parentMenu) {
        mGridToggle = parentMenu.findItem(R.id.menu_grid_toggle);
        mList = parentMenu.findItem(R.id.menu_grid_count_0);
        mColumn1 = parentMenu.findItem(R.id.menu_grid_count_1);
        mColumn2 = parentMenu.findItem(R.id.menu_grid_count_2);
        mColumn3 = parentMenu.findItem(R.id.menu_grid_count_3);
        mColumn4 = parentMenu.findItem(R.id.menu_grid_count_4);
        mColumn5 = parentMenu.findItem(R.id.menu_grid_count_5);
        mColumn6 = parentMenu.findItem(R.id.menu_grid_count_6);
        mFilterAll = parentMenu.findItem(R.id.menu_file_filter_all);
        mFilterPdf = parentMenu.findItem(R.id.menu_file_filter_pdf);
        mFilterOffice = parentMenu.findItem(R.id.menu_file_filter_docx);
        mFilterImage = parentMenu.findItem(R.id.menu_file_filter_image);
        mFilterText = parentMenu.findItem(R.id.menu_file_filter_text);
        mSortByName = parentMenu.findItem(R.id.menu_file_sort_by_name);
        mSortByDate = parentMenu.findItem(R.id.menu_file_sort_by_date);
        ViewerUtils.keepOnScreenAfterClick(context, mFilterAll);
        ViewerUtils.keepOnScreenAfterClick(context, mFilterPdf);
        ViewerUtils.keepOnScreenAfterClick(context, mFilterOffice);
        ViewerUtils.keepOnScreenAfterClick(context, mFilterImage);
        ViewerUtils.keepOnScreenAfterClick(context, mFilterText);
    }

    public void updateView(FilterSettingsViewModel.FilterState filterState) {
        if (filterState != null) {
            mFilterAll.setChecked(filterState.shouldShowAll);
            mFilterPdf.setChecked(filterState.shouldFilterPdf);
            mFilterOffice.setChecked(filterState.shouldFilterOffice);
            mFilterImage.setChecked(filterState.shouldFilterImage);
            mFilterText.setChecked(filterState.shouldFilterText);
            updateSortState(filterState.mSortMode);
            updateGridState(filterState.mGridCount);
        }
    }

    private void updateSortState(FilterSettingsViewModel.SortMode sortMode) {
        switch (sortMode) {
            case DATE_MODIFIED:
                mSortByDate.setChecked(true);
                break;
            case NAME:
                mSortByName.setChecked(true);
                break;
        }
    }

    private void updateGridState(int gridCount) {
        switch (gridCount) {
            case 0:
                mList.setChecked(true);
                break;
            case 1:
                mColumn1.setChecked(true);
                break;
            case 2:
                mColumn2.setChecked(true);
                break;
            case 3:
                mColumn3.setChecked(true);
                break;
            case 4:
                mColumn4.setChecked(true);
                break;
            case 5:
                mColumn5.setChecked(true);
                break;
            case 6:
                mColumn6.setChecked(true);
                break;
        }

        if (gridCount > 0) {
            // In grid mode
            mGridToggle.setTitle(R.string.dialog_add_page_grid);
            mGridToggle.setIcon(R.drawable.ic_view_module_white_24dp);
            AnalyticsHandlerAdapter.getInstance()
                    .setString(AnalyticsHandlerAdapter.CustomKeys.ALL_FILE_BROWSER_MODE, "Grid");
        } else {
            // In list mode
            mGridToggle.setTitle(R.string.action_list_view);
            mGridToggle.setIcon(R.drawable.ic_view_list_white_24dp);
            AnalyticsHandlerAdapter.getInstance()
                    .setString(AnalyticsHandlerAdapter.CustomKeys.ALL_FILE_BROWSER_MODE, "List");
        }
    }
}
