package com.pdftron.pdf.utils;

import android.graphics.PointF;

import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.widget.SoundAnnotView;

import java.util.ArrayList;

public class SoundManager {

    private final ArrayList<SoundAnnotView> mSoundAnnotViews = new ArrayList<>();

    public SoundAnnotView createSoundView(PDFViewCtrl parent, PointF targetScreenPoint, int pageNum) throws PDFNetException {
        closeExistingViews();
        SoundAnnotView view = new SoundAnnotView(parent, targetScreenPoint, pageNum);
        mSoundAnnotViews.add(view);
        return view;
    }

    public SoundAnnotView createSoundView(PDFViewCtrl parent, String filePath, int sampleRate, int encodingBitRate, int channel) {
        closeExistingViews();
        SoundAnnotView view = new SoundAnnotView(parent, filePath, sampleRate, encodingBitRate, channel);
        mSoundAnnotViews.add(view);
        return view;
    }

    public void close() {
        closeExistingViews();
    }

    private void closeExistingViews() {
        if (!mSoundAnnotViews.isEmpty()) {
            for (SoundAnnotView v : mSoundAnnotViews) {
                v.handleDone();
            }
            mSoundAnnotViews.clear();
        }
    }

    public void removeView(SoundAnnotView view) {
        mSoundAnnotViews.remove(view);
    }
}
