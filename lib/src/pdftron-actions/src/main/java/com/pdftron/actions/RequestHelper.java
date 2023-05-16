package com.pdftron.actions;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Log;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class RequestHelper {

    private static final String TAG = RequestHelper.class.getName();

    @Nullable
    public static ResponsePair post(OkHttpClient client, String url, RequestBody body) throws IOException {
        return post(client, url, body, null);
    }

    @Nullable
    public static ResponsePair post(OkHttpClient client, String url, RequestBody body, @Nullable HashMap<String, String> headers) throws IOException {
        if (client == null) {
            return null;
        }
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .post(body);

        if (headers != null && !headers.isEmpty()) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                requestBuilder.addHeader(key, value);
            }
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                return new ResponsePair(response.code(), response.isSuccessful(), response.body().string());
            } else {
                return null;
            }
        }
    }

    @Nullable
    public static ResponsePair get(OkHttpClient client, String url,
            @Nullable HashMap<String, String> params, @Nullable HashMap<String, String> headers,
            File outputFile) throws IOException, NullPointerException {
        if (client == null) {
            return null;
        }
        HttpUrl httpUrl = HttpUrl.parse(url);
        if (httpUrl == null) {
            return null;
        }
        HttpUrl.Builder httpBuilder = httpUrl.newBuilder();
        if (params != null) {
            for (Map.Entry<String, String> entry : params.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                httpBuilder.addQueryParameter(key, value);
            }
        }

        Request.Builder requestBuilder = new Request.Builder()
                .url(httpBuilder.build());

        if (headers != null) {
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                requestBuilder.addHeader(key, value);
            }
        }

        Request request = requestBuilder.build();
        try (Response response = client.newCall(request).execute()) {
            if (response.body() != null) {
                if (outputFile != null) {
                    Log.d(TAG, "attempt to download: " + outputFile.getAbsolutePath());
                    BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
                    sink.writeAll(response.body().source());
                    sink.close();
                    return new ResponsePair(response.code(), response.isSuccessful(), outputFile.getAbsolutePath());
                } else {
                    return new ResponsePair(response.code(), response.isSuccessful(), response.body().string());
                }
            } else {
                return null;
            }
        }
    }
}
