package com.pdftron.demo.utils;

import com.pdftron.pdf.model.FileInfo;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.pdftron.demo.navigation.adapter.BaseFileAdapter.VIEW_TYPE_HEADER;
import static com.pdftron.pdf.model.FileInfo.HEADER_TYPE_EARLIER;
import static com.pdftron.pdf.model.FileInfo.HEADER_TYPE_THIS_WEEK;
import static com.pdftron.pdf.model.FileInfo.HEADER_TYPE_TODAY;

public class DateHeaderUtils {
    public static final int NOT_SET = -1;
    public static final int weekInMilliseconds = 1000 * 60 * 60 * 24 * 7;

    private static class FileListIndex {
        private int mValue = NOT_SET;

        public void setValue(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public boolean isNotSet() {
            return mValue == NOT_SET;
        }

        public boolean isSet() {
            return mValue > NOT_SET;
        }

        public void shiftUpIndex() {
            if (isSet()) {
                mValue++;
            }
        }
    }

    private static void addHeader(List<FileInfo> fileInfos, int index, String label, int headerType) {
        FileInfo header = new FileInfo(VIEW_TYPE_HEADER, label, headerType);
        fileInfos.add(index, header);
    }

    public static Date createToday() {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static void insertDateLabelsIntoFileList(List<FileInfo> fileInfos, String todayLabel, String thisWeekLabel, String earlierLabel) {
        Date today = createToday();
        Collections.sort(fileInfos, (file1, file2) -> file2.getRawModifiedDate().compareTo(file1.getRawModifiedDate()));

        FileListIndex todayIndex = new FileListIndex();
        FileListIndex thisWeekIndex = new FileListIndex();
        FileListIndex earlierIndex = new FileListIndex();

        // Scan file list to determine index position for headers
        for (int index = 0; index < fileInfos.size(); index++) {
            Date modifiedDate = new Date(fileInfos.get(index).getRawModifiedDate());
            if (modifiedDate.getTime() >= today.getTime()) {
                todayIndex.setValue(0);
            } else if ((today.getTime() - weekInMilliseconds) < modifiedDate.getTime()) {
                if (thisWeekIndex.isNotSet()) {
                    thisWeekIndex.setValue(index);
                }
            } else {
                if (earlierIndex.isNotSet()) {
                    earlierIndex.setValue(index);
                }
            }
        }

        // For each set index, insert in the correct position in the fileList, adjust for shifts due to injecting a previous label
        if (todayIndex.isSet()) {
            addHeader(fileInfos, todayIndex.getValue(), todayLabel, HEADER_TYPE_TODAY);

            // Shift next list heading if set
            if (thisWeekIndex.isSet()) {
                thisWeekIndex.shiftUpIndex();
            }
            // Shift next list heading if set
            if (earlierIndex.isSet()) {
                earlierIndex.shiftUpIndex();
            }
        }
        if (thisWeekIndex.isSet()) {
            addHeader(fileInfos, thisWeekIndex.getValue(), thisWeekLabel, HEADER_TYPE_THIS_WEEK);

            // Shift next list heading if set
            if (earlierIndex.isSet()) {
                earlierIndex.shiftUpIndex();
            }
        }
        if (earlierIndex.isSet()) {
            addHeader(fileInfos, earlierIndex.getValue(), earlierLabel, HEADER_TYPE_EARLIER);
        }
    }
}