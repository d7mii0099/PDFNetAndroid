package com.pdftron.pdf.dialog.menueditor;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.pdftron.pdf.dialog.menueditor.model.MenuEditorItem;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class MenuEditorViewModel extends AndroidViewModel {

    @NonNull
    private final MutableLiveData<ArrayList<MenuEditorItem>> mItems = new MutableLiveData<>();

    @NonNull
    private final MutableLiveData<ArrayList<MenuEditorItem>> mAllItems = new MutableLiveData<>();

    @NonNull
    private final MutableLiveData<ArrayList<MenuEditorItem>> mPinnedItems = new MutableLiveData<>();

    @NonNull
    private final PublishSubject<MenuEditorEvent> mObservable = PublishSubject.create();

    public MenuEditorViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<ArrayList<MenuEditorItem>> getItemsLiveData() {
        return mItems;
    }

    public void setItems(ArrayList<MenuEditorItem> items) {
        mItems.setValue(items);
    }

    public LiveData<ArrayList<MenuEditorItem>> getAllItemsLiveData() {
        return mAllItems;
    }

    public void setAllItems(ArrayList<MenuEditorItem> items) {
        mAllItems.setValue(items);
    }

    public LiveData<ArrayList<MenuEditorItem>> getPinnedItemsLiveData() {
        return mPinnedItems;
    }

    public void setPinnedItems(ArrayList<MenuEditorItem> items) {
        mPinnedItems.setValue(items);
    }

    public void onReset() {
        mObservable.onNext(new MenuEditorEvent(MenuEditorEvent.Type.RESET));
    }

    public final Observable<MenuEditorEvent> getObservable() {
        return mObservable.serialize();
    }
}
