package com.pdftron.pdf.viewmodel;

import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.widget.richtext.PTRichEditor;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

public class RichTextViewModel extends ViewModel {

    @NonNull
    private final PublishSubject<RichTextEvent> mObservable = PublishSubject.create();

    public void onCloseToolbar() {
        mObservable.onNext(new RichTextEvent(RichTextEvent.Type.CLOSE_TOOLBAR));
    }

    public void onOpenToolbar() {
        mObservable.onNext(new RichTextEvent(RichTextEvent.Type.OPEN_TOOLBAR));
    }

    public void onUpdateTextStyle(AnnotStyle style) {
        mObservable.onNext(new RichTextEvent(RichTextEvent.Type.TEXT_STYLE, style));
    }

    public void onEditorAction(RichTextEvent.Type actionType) {
        mObservable.onNext(new RichTextEvent(actionType));
    }

    public void onUpdateDecorationType(PTRichEditor.Type decorationType, boolean checked) {
        mObservable.onNext(new RichTextEvent(RichTextEvent.Type.UPDATE_TOOLBAR, decorationType, checked));
    }

    public final Observable<RichTextEvent> getObservable() {
        return mObservable.serialize();
    }

}
