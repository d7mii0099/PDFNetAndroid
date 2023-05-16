package com.pdftron.pdf.tools;

import android.graphics.PointF;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.pdftron.common.PDFNetException;
import com.pdftron.filters.MappedFile;
import com.pdftron.pdf.Annot;
import com.pdftron.pdf.PDFDoc;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.Rect;
import com.pdftron.pdf.annots.Sound;
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter;
import com.pdftron.pdf.utils.Utils;

import java.io.File;

@Keep
public class SoundCreate extends SimpleTapShapeCreate {

    public static final String SOUND_ICON = "sound";

    public static final int SAMPLE_RATE = 22050;
    private static final int BITS_PER_SAMPLE = 16;
    private static final int NUM_CHANNELS = 1;

    private String mOutputFilePath;

    /**
     * Class constructor
     */
    public SoundCreate(@NonNull PDFViewCtrl ctrl) {
        super(ctrl);
    }

    @Override
    public ToolManager.ToolModeBase getToolMode() {
        return ToolManager.ToolMode.SOUND_CREATE;
    }

    @Override
    public int getCreateAnnotType() {
        return Annot.e_Sound;
    }

    @Override
    public void addAnnotation() {
        if (mPt2 == null) {
            AnalyticsHandlerAdapter.getInstance().sendException(
                    new Exception("target point is not specified."));
            return;
        }

        if (mPdfViewCtrl == null) {
            return;
        }

        setNextToolModeHelper();
        setCurrentDefaultToolModeHelper(getToolMode());

        ToolManager toolManager = (ToolManager) mPdfViewCtrl.getToolManager();

        try {
            int page = mPdfViewCtrl.getPageNumberFromScreenPt(mPt2.x, mPt2.y);
            toolManager.getSoundManager().createSoundView(mPdfViewCtrl, mPt2, page).show();
        } catch (PDFNetException e) {
            e.printStackTrace();
        }
    }

    public void createSound(PointF targetPagePoint, int pageNum, String outputPath) {
        mOutputFilePath = outputPath;

        createAnnotation(null, targetPagePoint, pageNum);

        if (outputPath != null) {
            File file = new File(outputPath);
            if (file.exists() && file.isFile()) {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            }
        }
    }

    @Override
    protected Annot createMarkup(@NonNull PDFDoc doc, Rect bbox) throws PDFNetException {
        if (Utils.isNullOrEmpty(mOutputFilePath)) {
            return null;
        }
        MappedFile mappedFile = new MappedFile(mOutputFilePath);
        Sound sound = Sound.createWithData(mPdfViewCtrl.getDoc(),
                bbox, mappedFile, BITS_PER_SAMPLE, SAMPLE_RATE, NUM_CHANNELS);
        sound.setIcon(Sound.e_Speaker);
        sound.getSoundStream().putName("E", "Signed");

        sound.refreshAppearance();

        return sound;
    }
}
