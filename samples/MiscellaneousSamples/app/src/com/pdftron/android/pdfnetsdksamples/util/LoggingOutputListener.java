package com.pdftron.android.pdfnetsdksamples.util;

import android.widget.ScrollView;
import android.widget.TextView;

import com.pdftron.android.pdfnetsdksamples.OutputListener;

import java.util.Arrays;

public class LoggingOutputListener implements OutputListener {

    private static final boolean SHOULD_THROW = false; // flag is used for testing purposes
    private final TextView mOutputTextView;
    private final ScrollView mOutputScrollView;

    public LoggingOutputListener(TextView outputTextView, ScrollView outputScrollView) {
        mOutputTextView = outputTextView;
        mOutputScrollView = outputScrollView;
    }

    @Override
    public void print(String output) {
        mOutputTextView.append(output);

        mOutputScrollView.post(new Runnable() {
            // This is necessary to scroll the ScrollView to the bottom.
            @Override
            public void run() {
                mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void println(String output) {
        System.out.println(output);
        mOutputTextView.append(output + "\n");

        mOutputScrollView.post(new Runnable() {
            // This is necessary to scroll the ScrollView to the bottom.
            @Override
            public void run() {
                mOutputScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    @Override
    public void print() {
        print("");
    }

    @Override
    public void println() {
        println("");
    }

    @Override
    public void printError(String errorMessage) {
        if (SHOULD_THROW) {
            throw new RuntimeException(errorMessage);
        }
        println(errorMessage);
    }

    @Override
    public void printError(StackTraceElement[] stackTrace) {
        if (SHOULD_THROW) {
            throw new RuntimeException(Arrays.toString(stackTrace));
        }
        for (int i = 0; i < stackTrace.length; i++) {
            println(stackTrace[i].toString());
        }
    }
}
