package com.pdftron.pdf.widget.preset.component;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Pair;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;

import com.pdftron.pdf.config.ToolStyleConfig;
import com.pdftron.pdf.model.AnnotStyle;
import com.pdftron.pdf.model.CustomStampOption;
import com.pdftron.pdf.model.StandardStampPreviewAppearance;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.RubberStampCreate;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager;
import com.pdftron.pdf.utils.StampManager;
import com.pdftron.pdf.utils.Utils;
import com.pdftron.pdf.widget.base.ObservingLiveData;
import com.pdftron.pdf.widget.preset.component.model.PresetBarState;
import com.pdftron.pdf.widget.preset.component.model.PresetButtonState;
import com.pdftron.pdf.widget.preset.component.model.SinglePresetState;
import com.pdftron.pdf.widget.toolbar.builder.ToolbarButtonType;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * View Model that manages the preset bar state
 */
public class PresetBarViewModel extends AndroidViewModel {

    private final CompositeDisposable mDisposables = new CompositeDisposable();

    private final ObservingLiveData<PresetBarState> mPresetLiveData = new ObservingLiveData<>();

    private final ObservingLiveData<SinglePresetState> mSinglePresetLiveData = new ObservingLiveData<>();

    public PresetBarViewModel(@NonNull Application application) {
        super(application);
    }

    public void showPresetBar() {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            state.showPresetBar();
        }
    }

    public void hidePresetBar() {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            state.hidePresetBar();
        }
    }

    public void setPresetBarState(@NonNull PresetBarState presetStateList) {
        mPresetLiveData.setValue(presetStateList);
    }

    @Nullable
    public PresetBarState getPresetBarState() {
        return mPresetLiveData.getValue();
    }

    public void updateAnnotStyles(@NonNull ArrayList<AnnotStyle> annotStyles, @IntRange(from = 0, to = 3) int index) {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            state.updateAnnotStyles(annotStyles, index);
        }
    }

    public void selectPreset(@IntRange(from = 0, to = 3) int index) {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            state.selectPreset(index);
        }
    }

    public void dismissStyleDialog() {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            state.dismissStyleDialog();
        }
    }

    public void openStyleDialog() {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            state.openStyleDialog();
        }
    }

    public void reloadStampPreset(Context context, int annotType,
            @NonNull final String toolbarStyleId,
            final int index) {
        AnnotStyle annotStyle = ToolStyleConfig.getInstance().getAnnotPresetStyle(context, annotType, index, toolbarStyleId);
        String stampId = annotStyle.getStampId();
        try {
            JSONObject jsonObject = new JSONObject(stampId);
            int stampIndex = jsonObject.optInt(CustomStampOption.KEY_INDEX);
            String filePath = CustomStampOption.getCustomStampBitmapPath(context, stampIndex);
            File bitmapFile = new File(filePath);
            if (bitmapFile.exists()) {
                // we always want to reload custom stamp as editing in the custom stamp
                // fragment will change the stampId
                saveStampPreset(context, annotType, stampId, toolbarStyleId, index);
            } else {
                saveStampPreset(context, annotType, RubberStampCreate.sStandardStampPreviewAppearance[0].stampLabel, toolbarStyleId, index);
            }
        } catch (Exception ignored) {
        }
    }

    public void reloadPreset(Context context, int annotType) {
        PresetBarState presetBarState = getPresetBarState();
        Pair<PresetButtonState, Integer> presetPair = presetBarState != null ? presetBarState.getActivePresetState() : null;
        if (presetPair != null) {
            PresetButtonState buttonState = presetPair.first;
            ArrayList<AnnotStyle> annotStyles = buttonState.getAnnotStyles();
            if (annotStyles != null) {
                for (AnnotStyle annotStyle : annotStyles) {
                    if (annotStyle != null && annotStyle.getAnnotType() == annotType) {
                        int index = presetPair.second;
                        String toolbarStyleId = presetBarState.getToolbarStyleId();
                        if (annotType == AnnotStyle.CUSTOM_ANNOT_TYPE_SIGNATURE) {
                            String signaturePath = annotStyle.getStampId();
                            if (!Utils.isNullOrEmpty(signaturePath) && !(new File(signaturePath)).exists()) {
                                File[] files = StampManager.getInstance().getSavedSignatures(context);
                                if (files != null && files.length > 0) {
                                    saveStampPreset(context, annotType, files[0].getAbsolutePath(), toolbarStyleId, index);
                                } else {
                                    mSinglePresetLiveData.setValue(new SinglePresetState());
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void reloadSignaturePreset(Context context, int annotType,
            @NonNull final String toolbarStyleId,
            final int index) {
        AnnotStyle annotStyle = ToolStyleConfig.getInstance().getAnnotPresetStyle(context, annotType, index, toolbarStyleId);
        String signaturePath = annotStyle.getStampId();
        if (!Utils.isNullOrEmpty(signaturePath) && !(new File(signaturePath)).exists()) {
            File[] files = StampManager.getInstance().getSavedSignatures(context);
            if (files != null && files.length > 0) {
                saveStampPreset(context, annotType, files[0].getAbsolutePath(), toolbarStyleId, index);
            } else {
                mSinglePresetLiveData.setValue(new SinglePresetState());
            }
        }
    }

    public void saveStampPreset(Context context, int annotType,
            @NonNull String stampId,
            @NonNull final String toolbarStyleId,
            final int index) {
        AnnotStyle annotStyle = ToolStyleConfig.getInstance().getAnnotPresetStyle(context, annotType, index, toolbarStyleId);
        saveStampPresetImpl(annotStyle, stampId, toolbarStyleId, index);
    }

    public void saveCountMeasurementPreset(String label, AnnotStyle annotStyle, @NonNull final String toolbarStyleId,
            final int index) {
        saveStampPresetImpl(annotStyle, label, toolbarStyleId, index);
    }

    public boolean hasCountMeasurementPresets() {
        ArrayList<AnnotStyle> annotStyles = getAnnotStyles();
        if (annotStyles == null || annotStyles.isEmpty()) {
            return false;
        }
        //only need to check the fist one
        AnnotStyle annotStyle = annotStyles.get(0);
        if (annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT) {
            return annotStyle.hasStampId() && !annotStyle.getStampId().isEmpty();
        }
        return false;
    }

    public void saveStampPreset(int annotType, String stampId) {
        PresetBarState state = mPresetLiveData.getValue();
        if (state != null) {
            PresetButtonState buttonState = state.getPresetState(0);
            ArrayList<AnnotStyle> annotStyles = buttonState.getAnnotStyles();
            if (annotStyles != null) {
                for (AnnotStyle annotStyle : annotStyles) {
                    if (annotStyle != null && annotType == annotStyle.getAnnotType()) {
                        saveStampPresetImpl(annotStyle, stampId, state.getToolbarStyleId(), 0);
                    }
                }
            }
        }
    }

    private void saveStampPresetImpl(AnnotStyle annotStyle, @NonNull String stampId,
            @NonNull final String toolbarStyleId,
            final int index) {
        if (annotStyle != null) {
            annotStyle.setStampId(stampId);
            PdfViewCtrlSettingsManager.setAnnotStylePreset(getApplication().getApplicationContext(),
                    annotStyle.getAnnotType(),
                    index,
                    toolbarStyleId,
                    annotStyle.toJSONString()
            );
            ArrayList<AnnotStyle> annotStyles = new ArrayList<>(1);
            annotStyles.add(annotStyle);
            updateAnnotStyles(annotStyles, 0);
        }
    }

    public void generatePreview(int toolbarButtonType, @Nullable final String stampId) {
        if (null == stampId) {
            mSinglePresetLiveData.setValue(new SinglePresetState());
            return;
        }
        if (ToolbarButtonType.STAMP.getValue() == toolbarButtonType) {
            mDisposables.add(getStampPreview(stampId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Object>() {
                                   @Override
                                   public void accept(Object object) throws Exception {
                                       if (object instanceof File) {
                                           mSinglePresetLiveData.setValue(SinglePresetState.fromImageFile((File) object));
                                       } else if (object instanceof Bitmap) {
                                           mSinglePresetLiveData.setValue(SinglePresetState.fromBitmap((Bitmap) object));
                                       }
                                   }
                               }
                            , new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    mSinglePresetLiveData.setValue(new SinglePresetState());
                                }
                            }));
        } else if (ToolbarButtonType.SIGNATURE.getValue() == toolbarButtonType) {
            mDisposables.add(StampManager.getSignaturePreview(getApplication(), stampId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<File>() {
                                   @Override
                                   public void accept(File file) throws Exception {
                                       mSinglePresetLiveData.setValue(SinglePresetState.fromImageFile(file));
                                   }
                               }
                            , new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    mSinglePresetLiveData.setValue(SinglePresetState.fromEmptyState(R.string.tools_signature_create_title));
                                }
                            }));
        } else if (ToolbarButtonType.COUNT_MEASUREMENT.getValue() == toolbarButtonType) {
            mDisposables.add(getCountMeasurementPreview(stampId)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<AnnotStyle>() {
                                   @Override
                                   public void accept(AnnotStyle annotStyle) throws Exception {
                                       mSinglePresetLiveData.setValue(SinglePresetState.fromAnnotStyle(annotStyle, R.drawable.ic_measurement_count));
                                   }
                               }
                            , new Consumer<Throwable>() {
                                @Override
                                public void accept(Throwable throwable) throws Exception {
                                    mSinglePresetLiveData.setValue(SinglePresetState.fromEmptyState(R.string.count_measurement_create_group));
                                }
                            }));
        }
    }

    public void observePresetState(LifecycleOwner owner, Observer<PresetBarState> observer) {
        mPresetLiveData.observe(owner, observer);
    }

    public void observeSinglePresetImageFile(LifecycleOwner owner, Observer<SinglePresetState> observer) {
        mSinglePresetLiveData.observe(owner, observer);
    }

    @Override
    protected void onCleared() {
        super.onCleared();

        mDisposables.clear();
    }

    private Single<Object> getStampPreview(@NonNull final String stampId) {
        return Single.create(new SingleOnSubscribe<Object>() {
            @Override
            public void subscribe(SingleEmitter<Object> emitter) throws Exception {
                try {
                    // custom stamp
                    Context context = getApplication().getApplicationContext();
                    try {
                        JSONObject jsonObject = new JSONObject(stampId);
                        int index = jsonObject.optInt(CustomStampOption.KEY_INDEX);
                        String filePath = CustomStampOption.getCustomStampBitmapPath(context, index);
                        File bitmapFile = new File(filePath);
                        if (bitmapFile.exists()) {
                            emitter.onSuccess(new File(filePath));
                        } else {
                            emitter.tryOnError(new Exception("Could not create stamp preview"));
                        }
                        return;
                    } catch (Exception ignored) {
                    }

                    // rubber stamp
                    StandardStampPreviewAppearance[] defaultStamps = RubberStampCreate.sStandardStampPreviewAppearance;
                    StandardStampPreviewAppearance matchedStamp = null;
                    for (StandardStampPreviewAppearance stamp : defaultStamps) {
                        if (stampId.equals(stamp.stampLabel)) {
                            matchedStamp = stamp;
                            break;
                        }
                    }
                    if (null == matchedStamp) {
                        emitter.tryOnError(new Exception("Could not find the matching stamp"));
                    } else {
                        int height = context.getResources().getDimensionPixelSize(R.dimen.stamp_image_height);
                        if (matchedStamp.previewAppearance == null) {
                            int bgColor = Utils.isDeviceNightMode(context) ? Color.BLACK : Color.WHITE;
                            Bitmap bitmap = AnnotUtils.getStandardStampBitmapFromPdf(context, matchedStamp.stampLabel, bgColor, height);
                            emitter.onSuccess(bitmap);
                        } else {
                            String filePath = AnnotUtils.getStandardStampImageFileFromName(context, matchedStamp, height);
                            if (filePath != null) {
                                emitter.onSuccess(new File(filePath));
                            } else {
                                emitter.tryOnError(new Exception("Could not create stamp preview"));
                            }
                        }
                    }
                } catch (Exception ex) {
                    emitter.tryOnError(ex);
                }
            }
        });
    }

    private ArrayList<AnnotStyle> getAnnotStyles() {
        PresetBarState presetBarState = getPresetBarState();
        Pair<PresetButtonState, Integer> presetPair = presetBarState != null ? presetBarState.getActivePresetState() : null;
        if (presetPair != null) {
            PresetButtonState buttonState = presetPair.first;
            return buttonState.getAnnotStyles();
        }
        return null;
    }

    private Single<AnnotStyle> getCountMeasurementPreview(@NonNull final String stampId) {
        return Single.create(new SingleOnSubscribe<AnnotStyle>() {
            @Override
            public void subscribe(SingleEmitter<AnnotStyle> emitter) throws Exception {
                try {
                    if (stampId.isEmpty()) {
                        emitter.tryOnError(new Exception("no stamp id provided"));
                    }
                    ArrayList<AnnotStyle> annotStyles = getAnnotStyles();
                    if (annotStyles != null) {
                        for (AnnotStyle annotStyle : annotStyles) {
                            if (annotStyle != null && annotStyle.getAnnotType() == AnnotStyle.CUSTOM_ANNOT_TYPE_COUNT_MEASUREMENT) {
                                if (annotStyle.hasStampId() && annotStyle.getStampId().equals(stampId)) {
                                    emitter.onSuccess(annotStyle);
                                }
                            }
                        }
                    } else {
                        emitter.tryOnError(new Exception("could not find annot style"));
                    }
                } catch (Exception ex) {
                    emitter.tryOnError(ex);
                }
            }
        });
    }
}
