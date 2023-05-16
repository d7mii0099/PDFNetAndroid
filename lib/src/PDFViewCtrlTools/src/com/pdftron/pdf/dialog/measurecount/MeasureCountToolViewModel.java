package com.pdftron.pdf.dialog.measurecount;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.model.AnnotStyle;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

public class MeasureCountToolViewModel extends AndroidViewModel {
    private final MeasureCountToolRepository mRepo;
    private CompositeDisposable mDisposable = new CompositeDisposable();

    public MeasureCountToolViewModel(@NonNull Application application) {
        super(application);
        mRepo = new MeasureCountToolRepository(application);
    }

    public void insert(MeasureCountTool measureCountTool) {
        mDisposable.add(mRepo.insert(measureCountTool).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }

    public void delete(MeasureCountTool measureCountTool) {
        mDisposable.add(mRepo.delete(measureCountTool).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }

    public void update(String newLabel, String oldLabel, AnnotStyle annotStyle) {
        mDisposable.add(mRepo.updateLabel(newLabel, oldLabel, annotStyle).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe());
    }

    public void observeCountToolPresets(LifecycleOwner owner, Observer<List<MeasureCountTool>> observer) {
        mRepo.mCountToolPresetsLiveData.observe(owner, observer);
    }
}

class MeasureCountToolRepository {
    private MeasureCountToolDao mMeasureCountToolDao;

    protected LiveData<List<MeasureCountTool>> mCountToolPresetsLiveData;

    public MeasureCountToolRepository(Application application) {
        MeasureCountToolDb measureCountToolDb = MeasureCountToolDb.getInstance(application);
        if (measureCountToolDb != null) {
            mMeasureCountToolDao = measureCountToolDb.mMeasureCountToolDao();
            mCountToolPresetsLiveData = mMeasureCountToolDao.getCountToolPresets();
        }
    }

    public Single<Object> insert(MeasureCountTool measureCountTool) {
        return Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> emitter) throws Exception {
                if (mMeasureCountToolDao != null) {
                    mMeasureCountToolDao.insert(measureCountTool);
                } else {
                    emitter.tryOnError(new Exception("mMeasureCountToolDao cannot be null"));
                }
            }
        });
    }

    public Single<Object> delete(MeasureCountTool measureCountTool) {
        return Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> emitter) throws Exception {
                if (mMeasureCountToolDao != null) {
                    mMeasureCountToolDao.delete(measureCountTool);
                } else {
                    emitter.tryOnError(new Exception("mMeasureCountToolDao cannot be null"));
                }
            }
        });
    }

    public Single<Object> updateLabel(String newLabel, String oldLabel, AnnotStyle annotStyle) {
        return Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> emitter) throws Exception {
                if (mMeasureCountToolDao != null) {
                    MeasureCountTool preset = new MeasureCountTool();
                    preset.annotStyleJson = annotStyle.toJSONString();
                    preset.label = annotStyle.getStampId();
                    List<MeasureCountTool> items = mMeasureCountToolDao.getPresetByLabel(oldLabel);
                    if (items != null && !items.isEmpty()) {
                        MeasureCountTool item = items.get(0); //should only be one
                        item.label = newLabel;
                        annotStyle.setStampId(newLabel);
                        item.annotStyleJson = annotStyle.toJSONString();
                        mMeasureCountToolDao.update(item);
                    } else {
                        mMeasureCountToolDao.delete(preset);
                        preset.label = newLabel;
                        annotStyle.setStampId(newLabel);
                        preset.annotStyleJson = annotStyle.toJSONString();
                        mMeasureCountToolDao.insert(preset);
                    }
                } else {
                    emitter.tryOnError(new Exception("mMeasureCountToolDao cannot be null"));
                }
            }
        });
    }
}