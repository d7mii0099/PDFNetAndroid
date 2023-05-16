package com.pdftron.pdf.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PointF;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

import com.chibde.visualizer.LineBarVisualizer;
import com.pdftron.common.PDFNetException;
import com.pdftron.pdf.PDFViewCtrl;
import com.pdftron.pdf.tools.R;
import com.pdftron.pdf.tools.SoundCreate;
import com.pdftron.pdf.tools.ToolManager;
import com.pdftron.pdf.utils.AnnotUtils;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class SoundAnnotView extends RelativeLayout {
    public final static String TAG = SoundAnnotView.class.getName();

    private final static int VIEW_MODE_SOUND_CREATE = 0;
    private final static int VIEW_MODE_SOUND_PLAY = 1;
    private static final int PADDING_OFFSET_DP = 40;

    private PDFViewCtrl mPdfViewCtrl;
    private CardView mMainLayout;

    private Theme mTheme;

    private int mPopupWidth;
    private int mPopupHeight;
    private int mMainWidth;
    private int mMainHeight;
    private int mPaddingOffsetPx;

    private AudioRecord mRecorder;
    private AudioTrack mPlayer;
    private volatile boolean mAppend = false;
    private long mCurrentRecordingTime;
    private long mLastRecordedTime;
    private int mBufferSize;

    private int mSampleRate;
    private int mEncodingBitRate;
    private int mNumChannelIn;
    private int mNumChannelOut;

    private boolean mShouldContinue; // Indicates if recording / playing should stop

    private boolean mRecordingDisabled;
    private boolean mPlayingDisabled;

    private boolean mStartRecording = true;
    private boolean mStartPlaying = true;

    private volatile boolean mHasRecording;

    private boolean mCleaningRecorder;
    private boolean mCleaningPlayer;

    private Thread mRecordingThread;
    private Thread mPlayingThread;

    private String mFilePath = null;

    private ImageView mDoneBtn;
    private ImageView mRecordBtn;
    private ImageView mPlayBackBtn;
    private TextView mLengthLabel;
    @Nullable
    private LineBarVisualizer mVisualizer;
    private RelativeLayout mSoundView;

    private int mPageNum = -1;
    private final PointF mTargetPagePoint = new PointF();

    private long mStartTime;
    private final Handler mHandler = new Handler();
    private final int SEC_UPDATE = 100;

    private int mViewMode = VIEW_MODE_SOUND_CREATE;

    private float mDX, mDY;

    private final Handler mRecordVisualizerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (msg != null && msg.obj != null) {
                byte[] data = (byte[]) msg.obj;
                if (mVisualizer != null) {
                    mVisualizer.setRecorder(data);
                }
            }
        }
    };

    private final Handler mPlayerHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            handlePlay();
        }
    };

    private final Runnable mUpdateTimeTaskRecord = new Runnable() {
        public void run() {
            if (mLengthLabel == null) {
                return;
            }
            final long start = mStartTime;
            mCurrentRecordingTime = System.currentTimeMillis() - start;

            updateLengthLabel(mLastRecordedTime + mCurrentRecordingTime);

            mHandler.postDelayed(this, SEC_UPDATE);
        }
    };
    private final Runnable mUpdateTimeTaskPlayBack = new Runnable() {
        public void run() {
            if (mLengthLabel == null) {
                return;
            }
            final long start = mStartTime;
            long milli = System.currentTimeMillis() - start;

            updateLengthLabel(milli);

            mHandler.postDelayed(this, SEC_UPDATE);
        }
    };

    public SoundAnnotView(PDFViewCtrl parent, String filePath, int sampleRate, int encodingBitRate, int channel) {
        super(parent.getContext());
        mFilePath = filePath;
        mSampleRate = sampleRate;
        mEncodingBitRate = encodingBitRate;
        mNumChannelOut = channel;
        mViewMode = VIEW_MODE_SOUND_PLAY;

        init(parent, VIEW_MODE_SOUND_PLAY);
    }

    public SoundAnnotView(PDFViewCtrl parent, PointF targetScreenPoint, int pageNum) throws PDFNetException {
        super(parent.getContext());
        mPageNum = pageNum;
        mNumChannelOut = AudioFormat.CHANNEL_OUT_MONO;

        double[] pts = parent.convScreenPtToPagePt(targetScreenPoint.x, targetScreenPoint.y, pageNum);
        mTargetPagePoint.x = Math.round(pts[0]);
        mTargetPagePoint.y = Math.round(pts[1]);

        init(parent, VIEW_MODE_SOUND_CREATE);
    }

    private void init(PDFViewCtrl parent, int viewMode) {
        mViewMode = viewMode;
        mPdfViewCtrl = parent;
        // get the offset at the top from the status bar
        mPaddingOffsetPx = (int) Utils.convDp2Pix(getContext(), PADDING_OFFSET_DP);

        if (mViewMode == VIEW_MODE_SOUND_CREATE) {
            File file = new File(parent.getContext().getFilesDir(), "audiorecord.out");
            mFilePath = file.getAbsolutePath();
            mSampleRate = SoundCreate.SAMPLE_RATE;
            mEncodingBitRate = AudioFormat.ENCODING_PCM_16BIT;
            mNumChannelIn = AudioFormat.CHANNEL_IN_MONO;

            mRecordingDisabled = false;
            mPlayingDisabled = true;
        } else {
            mRecordingDisabled = true;
            mPlayingDisabled = false;
        }

        initUi();
        measureLayoutSize();
    }

    public SoundAnnotView setFilePath(String filePath) {
        mFilePath = filePath;
        return this;
    }

    public SoundAnnotView setSampleRate(int sampleRate) {
        mSampleRate = sampleRate;
        return this;
    }

    public SoundAnnotView setEncodingBitRate(int encodingBitRate) {
        mEncodingBitRate = encodingBitRate;
        return this;
    }

    public SoundAnnotView setNumChannelOut(int numChannelOut) {
        mNumChannelOut = numChannelOut;
        return this;
    }

    private void initUi() {
        setVisibility(GONE);
        if (Utils.isLollipop()) {
            setElevation(2);
        }

        mSoundView = (RelativeLayout) LayoutInflater.from(mPdfViewCtrl.getContext()).inflate(R.layout.view_sound_annot, null);
        addView(mSoundView);

        mTheme = Theme.fromContext(mPdfViewCtrl.getContext());

        mMainLayout = mSoundView.findViewById(R.id.sound_create_main_lay);
        mRecordBtn = mSoundView.findViewById(R.id.sound_record_btn);
        mPlayBackBtn = mSoundView.findViewById(R.id.sound_playback_btn);
        mDoneBtn = mSoundView.findViewById(R.id.sound_done_btn);
        mLengthLabel = mSoundView.findViewById(R.id.record_length);
        mVisualizer = mSoundView.findViewById(R.id.visualizer);

        if (mViewMode == VIEW_MODE_SOUND_CREATE) {
            mDoneBtn.setImageDrawable(AppCompatResources.getDrawable(mDoneBtn.getContext(), R.drawable.ic_check_black_24dp));
            mRecordBtn.setVisibility(View.VISIBLE);
            setViewEnabled(mPlayBackBtn, false);
        } else {
            mDoneBtn.setImageDrawable(AppCompatResources.getDrawable(mDoneBtn.getContext(), R.drawable.ic_close_black_18dp));
            mRecordBtn.setVisibility(View.GONE);
            setViewEnabled(mPlayBackBtn, true);
        }

        mPlayBackBtn.setColorFilter(mTheme.iconColor);
        mDoneBtn.setColorFilter(mTheme.iconColor);
        mLengthLabel.setTextColor(mTheme.secondaryTextColor);

        if (mVisualizer != null) {
            mVisualizer.setColor(ContextCompat.getColor(mSoundView.getContext(), R.color.sound_visualizer_red));
            mVisualizer.setDensity(50f);
        }

        mRecordBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleRecord();
            }
        });

        mPlayBackBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mVisualizer != null) {
                    if (mViewMode == VIEW_MODE_SOUND_CREATE) {
                        mVisualizer.setColor(ContextCompat.getColor(mPlayBackBtn.getContext(), R.color.sound_visualizer_blue));
                    } else {
                        mVisualizer.setColor(ContextCompat.getColor(mPlayBackBtn.getContext(), R.color.sound_visualizer_red));
                    }
                }
                handlePlay();
            }
        });
        mDoneBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                handleDone();
            }
        });
    }

    public void onStop() {
        mShouldContinue = false;
        if (mRecordingThread != null) {
            mRecordingThread.interrupt();
            mRecordingThread = null;
        }
        if (mPlayingThread != null) {
            mPlayingThread.interrupt();
            mPlayingThread = null;
        }
        if (mRecorder != null && !mCleaningRecorder) {
            mCleaningRecorder = true;
            mRecorder.release();
            mRecorder = null;
            mCleaningRecorder = false;
        }
        if (mPlayer != null && !mCleaningPlayer) {
            mCleaningPlayer = true;
            mPlayer.release();
            mPlayer = null;
            mCleaningPlayer = false;
        }
        mHandler.removeCallbacksAndMessages(null);
        mPlayerHandler.removeCallbacksAndMessages(null);
    }

    private void updateLengthLabel(long millis) {
        String text = DurationFormatUtils.formatDuration(millis, "mm:ss");
        mLengthLabel.setText(text);
    }

    public void handleDone() {
        try {
            if (mFilePath == null) {
                return;
            }
            if (!mHasRecording) {
                // nothing recorded yet
                return;
            }
            if (!mStartRecording) {
                // stop current recording
                handleRecord();
            }

            AnnotUtils.createSoundAnnotation(mPdfViewCtrl, mTargetPagePoint, mPageNum, mFilePath);
        } finally {
            ToolManager tm = (ToolManager) mPdfViewCtrl.getToolManager();
            tm.getSoundManager().removeView(this);
            dismiss();
        }
    }

    private void handleRecord() {
        Context context = getContext();
        if (context == null) {
            return;
        }
        if (mRecordBtn == null || mVisualizer == null) {
            return;
        }
        if (mRecordingDisabled) {
            // ignore if currently playing
            return;
        }
        onRecord(mStartRecording);
        if (mStartRecording) {
            setViewEnabled(mPlayBackBtn, false);
            mRecordBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_sound_pause));
            mRecordBtn.setColorFilter(mTheme.iconColor);
            mVisualizer.setColor(ContextCompat.getColor(context, R.color.sound_visualizer_red));
        } else {
            setViewEnabled(mPlayBackBtn, true);
            mRecordBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_sound_record));
            mRecordBtn.setColorFilter(ContextCompat.getColor(context, R.color.sound_visualizer_red));
        }

        mStartRecording = !mStartRecording;
    }

    private void handlePlay() {
        Context context = getContext();
        if (context == null || mPlayBackBtn == null) {
            return;
        }

        if (mPlayingDisabled) {
            // ignore if currently recording
            return;
        }

        onPlay(mStartPlaying);
        if (mStartPlaying) {
            setViewEnabled(mRecordBtn, false, true);
            if (mViewMode == VIEW_MODE_SOUND_CREATE) {
                mPlayBackBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_sound_stop));
            } else {
                mPlayBackBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_sound_pause));
            }
        } else {
            setViewEnabled(mRecordBtn, true, true);
            mPlayBackBtn.setImageDrawable(AppCompatResources.getDrawable(context, R.drawable.ic_sound_play));
        }
        mStartPlaying = !mStartPlaying;
    }

    private void onPlay(boolean start) {
        if (start) {
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void stopPlaying() {
        mShouldContinue = false;

        mRecordingDisabled = false;

        mHandler.removeCallbacks(mUpdateTimeTaskRecord);
        mHandler.removeCallbacks(mUpdateTimeTaskPlayBack);
    }

    private void startPlaying() {
        mRecordingDisabled = true;
        mStartTime = System.currentTimeMillis();

        mShouldContinue = true;

        mBufferSize = AudioTrack.getMinBufferSize(mSampleRate, mNumChannelOut, mEncodingBitRate);
        if (mBufferSize == AudioTrack.ERROR_BAD_VALUE || mBufferSize == AudioTrack.ERROR) {
            // something went run
            return;
        }

        mPlayer = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                mSampleRate,
                mNumChannelOut,
                mEncodingBitRate,
                mBufferSize,
                AudioTrack.MODE_STREAM);

        if (mVisualizer != null) {
            try {
                mVisualizer.setPlayer(mPlayer.getAudioSessionId());
            } catch (Exception ex) {
                // likely we don't have record audio permission
                // but since we are playing audio, we will simply not use the visualizer
                mVisualizer = null;
            }
        }

        playAudio();
    }

    private void playAudio() {
        mPlayingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                FileInputStream fis = null;
                byte[] byteData = null;
                try {
                    fis = new FileInputStream(mFilePath);
                    byteData = IOUtils.toByteArray(fis);
                    byteData = swapByteArray(byteData);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    Utils.closeQuietly(fis);
                }
                if (byteData == null) {
                    return;
                }
                if (mPlayer == null || mPlayer.getState() != AudioTrack.STATE_INITIALIZED) {
                    Log.e(TAG, "Audio Track can't initialize!");
                    return;
                }

                mPlayer.play();

                mHandler.removeCallbacks(mUpdateTimeTaskPlayBack);
                mHandler.postDelayed(mUpdateTimeTaskPlayBack, SEC_UPDATE);

                Log.v(TAG, "Audio streaming started");

                int bytesRead = 0;
                int size = byteData.length;
                int bytesPerRead;

                while (bytesRead < size && mShouldContinue && !Thread.interrupted()) {
                    bytesPerRead = Math.min(mBufferSize, (size - bytesRead));
                    bytesRead += mPlayer.write(byteData, bytesRead, bytesPerRead);
                }

                if (mPlayer != null && !mCleaningPlayer) {
                    mCleaningPlayer = true;
                    mPlayer.stop();
                    mPlayer.release();
                    mPlayer = null;
                    mCleaningPlayer = false;
                }

                if (mShouldContinue) {
                    // user did not click stop
                    // let's finish player UI
                    mPlayerHandler.sendEmptyMessage(0);
                }

                Log.v(TAG, "Audio streaming finished. Samples written: " + byteData.length);
            }
        });
        mPlayingThread.start();
    }

    private void onRecord(boolean start) {
        if (start) {
            startRecording();
            if (mPlayBackBtn != null) {
                setViewEnabled(mPlayBackBtn, false);
            }
        } else {
            if (mPlayBackBtn != null) {
                setViewEnabled(mPlayBackBtn, true);
            }
            mLastRecordedTime += mCurrentRecordingTime;
            mAppend = true; //want to append any new audio that is recorded after first record is added
            stopRecording();
        }
    }

    private void stopRecording() {
        mShouldContinue = false;

        mPlayingDisabled = false;

        mHandler.removeCallbacks(mUpdateTimeTaskRecord);
        mHandler.removeCallbacks(mUpdateTimeTaskPlayBack);
    }

    private void startRecording() {
        mPlayingDisabled = true;
        mStartTime = System.currentTimeMillis();

        mShouldContinue = true;

        // buffer size in bytes
        mBufferSize = AudioRecord.getMinBufferSize(mSampleRate,
                mNumChannelIn,
                mEncodingBitRate);

        mRecorder = new AudioRecord(MediaRecorder.AudioSource.DEFAULT,
                mSampleRate,
                mNumChannelIn,
                mEncodingBitRate,
                mBufferSize);
        recordAudio();
    }

    private void recordAudio() {
        mRecordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

                if (mRecorder == null || mRecorder.getState() != AudioRecord.STATE_INITIALIZED) {
                    Log.e(TAG, "Audio Record can't initialize!");
                    return;
                }

                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(mFilePath, mAppend);
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }

                byte[] audioBuffer = new byte[mBufferSize];
                mRecorder.startRecording();

                mHandler.removeCallbacks(mUpdateTimeTaskRecord);
                mHandler.postDelayed(mUpdateTimeTaskRecord, SEC_UPDATE);

                Log.v(TAG, "Start recording");

                long bytesRead = 0;
                if (fos != null) {
                    while (mShouldContinue && !Thread.interrupted()) {
                        int numberOfByte = mRecorder.read(audioBuffer, 0, audioBuffer.length);
                        bytesRead += numberOfByte;

                        // Do something with the audioBuffer
                        if (AudioRecord.ERROR_INVALID_OPERATION != numberOfByte) {
                            try {
                                byte[] audioBufferReversed = swapByteArray(audioBuffer);

                                byte[] eightBit = toEightBitArray(audioBufferReversed);
                                if (eightBit != null) {
                                    Message m = new Message();
                                    m.obj = Arrays.copyOf(eightBit, eightBit.length);
                                    mRecordVisualizerHandler.sendMessage(m);
                                }

                                if (audioBufferReversed != null) {
                                    fos.write(audioBufferReversed);
                                    mHasRecording = true;
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                if (mRecorder != null && !mCleaningRecorder) {
                    mCleaningRecorder = true;
                    mRecorder.stop();
                    mRecorder.release();
                    mRecorder = null;
                    mCleaningRecorder = false;
                }

                Log.v(TAG, String.format("Recording stopped. Samples read: %d", bytesRead));
            }
        });
        mRecordingThread.start();
    }

    /**
     * check if the quick menu is visible
     *
     * @return true if visible
     */
    public boolean isShowing() {
        return getVisibility() == VISIBLE;
    }

    public void show() {
        if (mPdfViewCtrl.getParent() instanceof ViewGroup) {
            ((ViewGroup) mPdfViewCtrl.getParent()).addView(this);
        }
        setVisibility(VISIBLE);
        requestLocation();
        bringToFront();
        if (mViewMode == VIEW_MODE_SOUND_PLAY) {
            handlePlay();
        }
    }

    public void dismiss() {
        onStop();
        setVisibility(GONE);
        if (mPdfViewCtrl.getParent() instanceof ViewGroup) {
            ((ViewGroup) mPdfViewCtrl.getParent()).removeView(this);
        }
    }

    protected void measureLayoutSize() {
        mMainLayout.measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        mMainWidth = mMainLayout.getMeasuredWidth();
        mMainHeight = mMainLayout.getMeasuredHeight();

        mPopupWidth = mMainWidth + mPaddingOffsetPx;
        mPopupHeight = mMainHeight + mPaddingOffsetPx;

        mSoundView.setLayoutParams(new RelativeLayout.LayoutParams(mPopupWidth, mPopupHeight));
        mSoundView.setClickable(false);

        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(mPopupWidth, mPopupHeight);
        setLayoutParams(layoutParams);
    }

    /**
     * calculate location of quick menu
     */
    public void requestLocation() {
        int left = Math.max(0, mPdfViewCtrl.getWidth() / 2 - mMainWidth / 2);
        int top = mPdfViewCtrl.getHeight() - mPopupHeight;
        animate()
                .x(left)
                .y(top)
                .setDuration(0)
                .start();
    }

    private void setViewEnabled(@NonNull ImageView view, boolean enabled) {
        setViewEnabled(view, enabled, false);
    }

    private void setViewEnabled(@NonNull ImageView view, boolean enabled, boolean useAlpha) {
        view.setEnabled(enabled);
        if (enabled) {
            if (useAlpha) {
                view.setAlpha(1f);
            } else {
                view.setColorFilter(mTheme.iconColor);
            }
        } else {
            if (useAlpha) {
                view.setAlpha(0.54f);
            } else {
                view.setColorFilter(mTheme.disabledIconColor);
            }
        }
    }

    private static byte[] swapByteArray(byte[] a) {
        if (a == null) {
            return null;
        }
        byte[] ret = new byte[a.length];
        // if array is odd we set limit to a.length - 1.
        int limit = a.length - (a.length % 2);
        if (limit < 1) return null;
        for (int i = 0; i < limit - 1; i = i + 2) {
            ret[i] = a[i + 1];
            ret[i + 1] = a[i];
        }
        return ret;
    }

    private static byte[] toEightBitArray(byte[] a) {
        if (a == null) {
            return null;
        }
        byte[] ret = new byte[a.length / 2];
        int j = 0;
        for (int i = 0; i < a.length; i++) {
            if ((i & 1) == 0) {
                // even
                ret[j++] = a[i];
            }
        }
        return ret;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDX = getX() - event.getRawX();
                mDY = getY() - event.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                animate()
                        .x(event.getRawX() + mDX)
                        .y(event.getRawY() + mDY)
                        .setDuration(0)
                        .start();
                break;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                break;
            default:
                return false;
        }
        return true;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static final class Theme {
        @ColorInt
        final int iconColor;
        @ColorInt
        final int disabledIconColor;
        @ColorInt
        final int secondaryTextColor;

        public Theme(int iconColor, int disabledIconColor, int secondaryTextColor) {
            this.iconColor = iconColor;
            this.disabledIconColor = disabledIconColor;
            this.secondaryTextColor = secondaryTextColor;
        }

        public static Theme fromContext(@NonNull Context context) {
            final TypedArray a = context.obtainStyledAttributes(
                    null, R.styleable.SoundAnnotViewTheme, R.attr.pt_sound_annot_view_style, R.style.SoundAnnotViewTheme);
            int iconColor = a.getColor(R.styleable.SoundAnnotViewTheme_iconColor, context.getResources().getColor(R.color.pt_body_text_color));
            int disabledIconColor = a.getColor(R.styleable.SoundAnnotViewTheme_disabledIconColor, context.getResources().getColor(R.color.pt_disabled_state_color));
            int secondaryTextColor = a.getColor(R.styleable.SoundAnnotViewTheme_secondaryTextColor, context.getResources().getColor(R.color.pt_secondary_color));
            a.recycle();

            return new Theme(iconColor, disabledIconColor, secondaryTextColor);
        }
    }
}
