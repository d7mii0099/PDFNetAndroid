//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.pdf.utils;

import android.content.Context;

import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

/**
 * An async task for basic HTTP downloading
 */
public class BasicHTTPDownloadTask extends CustomAsyncTask<String, Void, Boolean> {

    private String mURL;
    private File mSaveFile;
    private JSONObject mCustomHeaders;
    private BasicHTTPDownloadTaskListener mListener;

    /**
     * @param context  The context
     * @param listener HTTP download task listener
     * @param url      Source URL to download
     * @param saveFile The file where it save to
     */
    public BasicHTTPDownloadTask(Context context, BasicHTTPDownloadTaskListener listener, String url, File saveFile) {
        this(context, listener, url, null, saveFile);
    }

    /**
     *
     * @param context  The context
     * @param listener HTTP download task listener
     * @param url      Source URL to download
     * @param customHeaders Custom request headers
     * @param saveFile The file where it save to
     */
    public BasicHTTPDownloadTask(Context context, BasicHTTPDownloadTaskListener listener, String url, JSONObject customHeaders, File saveFile) {
        super(context);
        mURL = url;
        mSaveFile = saveFile;
        mListener = listener;
        mCustomHeaders = customHeaders;
    }

    /**
     * It downloads the URL to the file
     *
     * @param params parameters
     * @return true if download success, false otherwise
     */
    @Override
    protected Boolean doInBackground(String... params) {
        URL url;
        HttpURLConnection urlConnection = null;
        try {
            url = new URL(mURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            if (mCustomHeaders != null) {
                Iterator<String> iter = mCustomHeaders.keys();
                while (iter.hasNext()) {
                    String key = iter.next();
                    String val = mCustomHeaders.optString(key);
                    if (!Utils.isNullOrEmpty(val)) {
                        urlConnection.setRequestProperty(key, val);
                    }
                }
            }
            try (OutputStream filestream = new BufferedOutputStream(new FileOutputStream(mSaveFile))) {
                try (InputStream inputStream = urlConnection.getInputStream()) {
                    IOUtils.copy(inputStream, filestream);
                }
            }
        } catch (MalformedURLException e) {
            return false;
        } catch (IOException e) {
            return false;
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }

        return true;
    }

    /**
     * It invokes the download listener.
     *
     * @param pass Whether it successfully downloads the file.
     */
    @Override
    protected void onPostExecute(Boolean pass) {
        mListener.onDownloadTask(pass, mSaveFile);
    }

    /**
     * Callback interface to be invoked when basic HTTP download is finished.
     */
    public interface BasicHTTPDownloadTaskListener {
        /**
         * Called when download task has been done.
         *
         * @param pass     True if successful
         * @param saveFile The saved file
         */
        void onDownloadTask(Boolean pass, File saveFile);
    }
}
