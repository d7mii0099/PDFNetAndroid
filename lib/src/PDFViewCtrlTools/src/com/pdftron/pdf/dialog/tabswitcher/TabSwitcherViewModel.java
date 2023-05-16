package com.pdftron.pdf.dialog.tabswitcher;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.pdftron.pdf.dialog.tabswitcher.model.TabSwitcherItem;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class TabSwitcherViewModel extends AndroidViewModel {

    @NonNull
    private final MutableLiveData<ArrayList<TabSwitcherItem>> mItems = new MutableLiveData<>();

    @NonNull
    private final MutableLiveData<String> mSelectedTag = new MutableLiveData<>();

    @NonNull
    private final PublishSubject<TabSwitcherEvent> mObservable = PublishSubject.create();

    public TabSwitcherViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ArrayList<TabSwitcherItem>> getItemsLiveData() {
        return mItems;
    }

    public void setItems(ArrayList<TabSwitcherItem> items) {
        mItems.setValue(items);
    }

    public LiveData<String> getSelectedTag() {
        return mSelectedTag;
    }

    public void setSelectedTag(String selectedTag) {
        mSelectedTag.setValue(selectedTag);
    }

    public void onCloseTab(@NonNull String tabTag) {
        mObservable.onNext(new TabSwitcherEvent(TabSwitcherEvent.Type.CLOSE_TAB, tabTag));
    }

    public void onSelectTab(@NonNull String tabTag) {
        mObservable.onNext(new TabSwitcherEvent(TabSwitcherEvent.Type.SELECT_TAB, tabTag));
    }

    public final Observable<TabSwitcherEvent> getObservable() {
        return mObservable.serialize();
    }
}
