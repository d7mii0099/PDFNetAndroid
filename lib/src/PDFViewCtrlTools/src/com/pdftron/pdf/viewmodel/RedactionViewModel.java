package com.pdftron.pdf.viewmodel;

import android.app.Application;
import android.util.Pair;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.pdftron.pdf.TextSearchResult;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RedactionViewModel extends AndroidViewModel {

    public RedactionViewModel(@NonNull Application application) {
        super(application);
    }

    @NonNull
    private final PublishSubject<RedactionEvent> mObservable = PublishSubject.create();

    public void onRedactBySearch(@NonNull ArrayList<Pair<Integer, ArrayList<Double>>> searchResults) {
        RedactionEvent redactionEvent = new RedactionEvent(RedactionEvent.Type.REDACT_BY_SEARCH);
        redactionEvent.setSearchResults(searchResults);
        mObservable.onNext(redactionEvent);
    }

    public void onRedactByPage(@NonNull ArrayList<Integer> pages) {
        RedactionEvent redactionEvent = new RedactionEvent(RedactionEvent.Type.REDACT_BY_PAGE);
        redactionEvent.setPages(pages);
        mObservable.onNext(redactionEvent);
    }

    public void onRedactBySearchOpenSheet() {
        RedactionEvent redactionEvent = new RedactionEvent(RedactionEvent.Type.REDACT_BY_SEARCH_OPEN_SHEET);
        mObservable.onNext(redactionEvent);
    }

    public void onRedactBySearchItemClicked(@NonNull TextSearchResult result) {
        RedactionEvent redactionEvent = new RedactionEvent(RedactionEvent.Type.REDACT_BY_SEARCH_ITEM_CLICKED);
        redactionEvent.setSelectedItem(result);
        mObservable.onNext(redactionEvent);
    }

    public void onRedactBySearchCloseClicked() {
        mObservable.onNext(new RedactionEvent(RedactionEvent.Type.REDACT_BY_SEARCH_CLOSE_CLICKED));
    }

    public final Observable<RedactionEvent> getObservable() {
        return mObservable.serialize();
    }
}
