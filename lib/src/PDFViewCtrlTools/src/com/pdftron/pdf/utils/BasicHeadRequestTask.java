package com.pdftron.pdf.utils;

import android.content.Context;

import org.json.JSONObject;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;

public class BasicHeadRequestTask extends CustomAsyncTask<String, Void, Boolean> {

    private String mURL;
    private BasicHeadRequestTaskListener mListener;
    private JSONObject mCustomHeaders;
    private String mResult;

    /**
     * @param context  The context
     * @param listener HTTP HEAD request task listener
     * @param url      Source URL
     */
    public BasicHeadRequestTask(Context context, BasicHeadRequestTaskListener listener, String url) {
        this(context, listener, url, null);
    }

    /**
     * @param context  The context
     * @param listener HTTP HEAD request task listener
     * @param url      Source URL
     * @param customHeaders Custom request headers
     */
    public BasicHeadRequestTask(Context context, BasicHeadRequestTaskListener listener, String url, JSONObject customHeaders) {
        super(context);
        mURL = url;
        mListener = listener;
        mCustomHeaders = customHeaders;
    }

    /**
     * Overload implementation of {@link CustomAsyncTask#doInBackground(Object[])}.
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
            urlConnection.setRequestMethod("HEAD");
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
            mResult = urlConnection.getContentType();
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
     * Overload implemntation of {@link CustomAsyncTask#onPostExecute(Object)}
     * It invokes the download listener.
     *
     * @param pass Whether it successfully downloads the file.
     */
    @Override
    protected void onPostExecute(Boolean pass) {
        mListener.onHeadRequestTask(pass, mResult);
    }

    /**
     * Callback interface to be invoked when basic HTTP download is finished.
     */
    public interface BasicHeadRequestTaskListener {
        /**
         * Called when download task has been done.
         *
         * @param pass   True if successful
         * @param result The content type
         */
        void onHeadRequestTask(Boolean pass, String result);
    }
}