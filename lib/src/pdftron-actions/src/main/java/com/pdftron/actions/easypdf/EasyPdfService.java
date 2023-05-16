package com.pdftron.actions.easypdf;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.pdftron.actions.RequestHelper;
import com.pdftron.actions.ResponsePair;
import com.pdftron.pdf.utils.Utils;

import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public class EasyPdfService {

    public interface EasyPdfServiceInterface {
        @GET("jobs/{jobId}/output/{newFileName}")
        Call<ResponseBody> startJob(@HeaderMap Map<String, String> headers,
                @Path(KEY_jobId) String jobId, @Path("newFileName") String newFileName,
                @Query(KEY_type) String type);

        // start new dynamic workflow

        @PUT("jobs")
        Call<ResponseBody> createWorkflow(@Header(KEY_Authorization) String authorization, @Body RequestBody requestBody);

        @PUT("jobs/{jobId}/input/{filename}")
        Call<ResponseBody> uploadFile(@Header(KEY_Authorization) String authorization, @Path(KEY_jobId) String jobId, @Path("filename") String filename, @Body RequestBody requestBody);

        @FormUrlEncoded
        @POST("jobs/{jobId}")
        Call<ResponseBody> startOrCancelJob(@Header(KEY_Authorization) String authorization, @Path(KEY_jobId) String jobId, @Field("operation") String operation);

        @POST("jobs/{jobId}/event")
        Call<ResponseBody> waitForJob(@Header(KEY_Authorization) String authorization, @Path(KEY_jobId) String jobId);

        @GET("jobs/{jobId}/output")
        Call<ResponseBody> downloadOutput(@Header(KEY_Authorization) String authorization, @Path(KEY_jobId) String jobId, @Query(KEY_type) String type);

        @DELETE("jobs/{jobId}")
        Call<ResponseBody> deleteJob(@Header(KEY_Authorization) String authorization, @Path(KEY_jobId) String jobId);

        // end new dynamic workflow
    }

    private static final String TAG = EasyPdfService.class.getSimpleName();

    private static boolean sDebug;

    private static String sClientId;
    private static String sClientSecret;

    private static String sJOB_PDF_TO_WORD;
    private static String sJOB_PDF_TO_HTML;

    private static final String END_POINT_BASE = "https://api.easypdfcloud.com/v1/";
    private static final String END_POINT_AUTH = "https://www.easypdfcloud.com/oauth2/token";

    private static final String KEY_grant_type = "grant_type";
    private static final String KEY_client_id = "client_id";
    private static final String KEY_client_secret = "client_secret";
    private static final String KEY_scope = "scope";
    private static final String KEY_Authorization = "Authorization";
    private static final String KEY_access_token = "access_token";
    private static final String KEY_file = "file";
    private static final String KEY_type = "type";
    private static final String KEY_jobId = "jobId";
    private static final String KEY_status = "status";
    private static final String KEY_finished = "finished";
    private static final String KEY_status_completed = "completed";
    private static final String KEY_start = "start";
    private static final String KEY_stop = "stop";
    private static final String KEY_Tasks = "Tasks";
    private static final String KEY_TaskType = "Type";
    private static final String KEY_TaskRevision = "Revision";
    private static final String KEY_EmbedContents = "EmbedContents";

    private static final MediaType sJSONType = MediaType.parse("application/json");

    public enum Type {
        PDF_TO_WORD,
        PDF_TO_HTML
    }

    private final OkHttpClient mClient;
    private final EasyPdfServiceInterface mService;
    private String mAccessToken;

    private String mOutputName;

    @Nullable
    private JSONObject mTaskJSON;

    public static void initialize(@NonNull String clientId, @NonNull String clientSecret) {
        sClientId = clientId;
        sClientSecret = clientSecret;
    }

    @Deprecated
    public static void setJobId(@NonNull Type type, @NonNull String jobId) {
        if (Type.PDF_TO_WORD == type) {
            sJOB_PDF_TO_WORD = jobId;
        } else if (Type.PDF_TO_HTML == type) {
            sJOB_PDF_TO_HTML = jobId;
        }
    }

    public EasyPdfService() {
        mClient = new OkHttpClient.Builder()
                .readTimeout(300, TimeUnit.SECONDS)
                .build();
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(END_POINT_BASE)
                .client(mClient)
                .build();
        mService = retrofit.create(EasyPdfServiceInterface.class);
    }

    @Deprecated
    public void setOutputName(@Nullable String outputName) {
        mOutputName = outputName;
    }

    public void setCustomTask(@NonNull JSONObject taskJSON) {
        mTaskJSON = taskJSON;
    }

    public static String startDynamicJob(@NonNull String taskType, int revision, @NonNull final File file,
            @NonNull final File outputFolder, @NonNull String outputName) throws IOException, JSONException {
        return startDynamicJob(taskType, revision, file, outputFolder, outputName, null);
    }

    public static String startDynamicJob(@NonNull String taskType, int revision, @NonNull final File file,
            @NonNull final File outputFolder, @NonNull String outputName, @Nullable JSONObject taskJSON) throws IOException, JSONException {
        checkClientLicense();
        EasyPdfService easyPdfService = new EasyPdfService();
        if (taskJSON != null) {
            easyPdfService.setCustomTask(taskJSON);
        }
        return easyPdfService.startDynamicJobImpl(taskType, revision, file, outputFolder, outputName);
    }

    public static JSONObject createHTMLTask(@NonNull String taskType, int revision) throws JSONException {
        JSONObject task = new JSONObject();
        task.put(KEY_TaskType, taskType);
        task.put(KEY_TaskRevision, revision);
        task.put(KEY_EmbedContents, true);
        return task;
    }

    @Deprecated
    public static Single<String> startPdf2WordJob(@NonNull final File file, @NonNull final File outputFolder) {
        checkClientLicense();
        if (sJOB_PDF_TO_WORD == null) {
            throw new RuntimeException("Job ID must be set");
        }
        EasyPdfService easyPdfService = new EasyPdfService();
        return easyPdfService.startJob(Type.PDF_TO_WORD, file, outputFolder);
    }

    @Deprecated
    public static Single<String> startPdf2HtmlJob(@NonNull final File file, @NonNull final File outputFolder) {
        checkClientLicense();
        if (sJOB_PDF_TO_HTML == null) {
            throw new RuntimeException("Job ID must be set");
        }
        EasyPdfService easyPdfService = new EasyPdfService();
        return easyPdfService.startJob(Type.PDF_TO_HTML, file, outputFolder);
    }

    private static void checkClientLicense() {
        if (sClientId == null || sClientSecret == null) {
            throw new RuntimeException("Client ID and Client secret must be set");
        }
    }

    @Deprecated
    private Single<String> startJob(@NonNull final Type type, @NonNull final File file, @NonNull final File outputFolder) {
        return Single.create(new SingleOnSubscribe<String>() {
            @Override
            public void subscribe(SingleEmitter<String> emitter) {
                try {
                    String output = startJobSync(type, file, outputFolder);
                    if (output != null) {
                        emitter.onSuccess(output);
                    } else {
                        emitter.tryOnError(new IllegalStateException("Unable to finish job"));
                    }
                } catch (Exception ex) {
                    emitter.tryOnError(ex);
                }
            }
        });
    }

    @Deprecated
    private String startJobSync(@NonNull final Type type, @NonNull final File file, @NonNull final File outputFolder) throws IOException, JSONException {
        String output = null;
        if (mAccessToken != null) {
            output = startConversionJobImpl(type, file, outputFolder);
        } else {
            String serviceId = getAuthHeaderImpl();
            if (serviceId != null) {
                mAccessToken = "Bearer " + serviceId;
                output = startConversionJobImpl(type, file, outputFolder);
            }
        }
        return output;
    }

    @Deprecated
    @Nullable
    private String startConversionJobImpl(@NonNull final Type type, @NonNull final File file, @NonNull final File outputFolder) throws IOException, JSONException {
        String jobId = conversionJobImpl(file, type);
        if (jobId != null) {
            String status = waitJobImpl(jobId);
            if (status != null) {
                return downloadJobImpl(type, jobId, file.getName(), outputFolder, mOutputName);
            }
        }
        return null;
    }

    @Deprecated
    @Nullable
    private String downloadJobImpl(Type type, String jobId, String fileName, File outputFolder, @Nullable String outputName) throws IOException {
        // result file on server
        String nameWithoutExt = FilenameUtils.removeExtension(fileName);
        String newFileName = nameWithoutExt + "." + getTypeExtension(type);

        // file to be downloaded to
        String outputFileName = newFileName;
        if (!Utils.isNullOrEmpty(outputName)) {
            String outputWithoutExt = FilenameUtils.removeExtension(outputName);
            outputFileName = outputWithoutExt + "." + getTypeExtension(type);
        }
        File outputFile = new File(outputFolder, outputFileName);

        HashMap<String, String> headers = new HashMap<>();
        headers.put(KEY_Authorization, mAccessToken);

        Call<ResponseBody> response = mService.startJob(headers, jobId, newFileName, "file");
        Response<ResponseBody> result = response.execute();
        if (result.isSuccessful() && result.body() != null) {
            Log.d(TAG, "attempt to download: " + outputFile.getAbsolutePath());
            BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
            sink.writeAll(result.body().source());
            sink.close();
            return outputFile.getAbsolutePath();
        }
        return null;
    }

    @Deprecated
    @Nullable
    private String waitJobImpl(String jobId) throws IOException, JSONException {
        String endPoint = "https://api.easypdfcloud.com/v1/jobs/" + jobId + "/event";

        RequestBody formBody = new FormBody.Builder()
                .build();

        HashMap<String, String> headers = new HashMap<>();
        headers.put(KEY_Authorization, mAccessToken);

        ResponsePair responsePair = RequestHelper.post(mClient, endPoint, formBody, headers);

        if (responsePair != null && responsePair.successful) {
            JSONObject jsonObject = new JSONObject(responsePair.body);
            String status = jsonObject.getString(KEY_status);
            if (KEY_status_completed.equals(status)) {
                return status;
            }
        }
        return null;
    }

    @Deprecated
    @Nullable
    private String conversionJobImpl(@NonNull File file, Type type) throws IOException, JSONException {
        // Upload to easy pdf server
        RequestBody formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(KEY_file, file.getName(), RequestBody.create(MediaType.parse("application/pdf"), file))
                .build();

        HashMap<String, String> headers = new HashMap<>();
        headers.put(KEY_Authorization, mAccessToken);

        ResponsePair responsePair = RequestHelper.post(mClient, getTypeEndPoint(type), formBody, headers);
        if (responsePair != null && responsePair.successful) {
            JSONObject jsonObject = new JSONObject(responsePair.body);
            return jsonObject.getString("jobID");
        }

        return null;
    }

    @Nullable
    private String getAuthHeaderImpl() throws IOException, JSONException {
        RequestBody formBody = new FormBody.Builder()
                .add(KEY_grant_type, "client_credentials")
                .add(KEY_client_id, sClientId)
                .add(KEY_client_secret, sClientSecret)
                .add(KEY_scope, "epc.api")
                .build();

        ResponsePair responsePair = RequestHelper.post(mClient, END_POINT_AUTH, formBody);
        if (responsePair != null && responsePair.successful) {
            JSONObject jsonObject = new JSONObject(responsePair.body);
            return jsonObject.getString(KEY_access_token);
        }

        return null;
    }

    @Deprecated
    @Nullable
    private String getTypeEndPoint(@NonNull Type type) {
        if (type == Type.PDF_TO_WORD) {
            return "https://api.easypdfcloud.com/v1/workflows/" + sJOB_PDF_TO_WORD + "/jobs";
        } else if (type == Type.PDF_TO_HTML) {
            return "https://api.easypdfcloud.com/v1/workflows/" + sJOB_PDF_TO_HTML + "/jobs";
        }
        return null;
    }

    @Deprecated
    @Nullable
    private String getTypeExtension(@NonNull Type type) {
        if (type == Type.PDF_TO_WORD) {
            return "docx";
        } else if (type == Type.PDF_TO_HTML) {
            return "html";
        }
        return null;
    }

    // === NEW DYNAMIC FLOW STARTS ===
    private String startDynamicJobImpl(@NonNull String taskType, int revision, @NonNull final File file,
            @NonNull final File outputFolder, @NonNull String outputName) throws IOException, JSONException {
        String output = null;
        if (mAccessToken != null) {
            output = createDynamicWorkflowImpl(taskType, revision, file, outputFolder, outputName);
        } else {
            String serviceId = getAuthHeaderImpl();
            if (serviceId != null) {
                mAccessToken = "Bearer " + serviceId;
                output = createDynamicWorkflowImpl(taskType, revision, file, outputFolder, outputName);
            }
        }
        return output;
    }

    /**
     * returns the output file path if successful, null if failed
     */
    @Nullable
    private String createDynamicWorkflowImpl(@NonNull String taskType, int revision, @NonNull final File file,
            @NonNull final File outputFolder, @NonNull String outputName) throws IOException, JSONException {
        // step 1 create a new job
        String jobId = createJob(taskType, revision);
        if (sDebug) {
            Log.d(TAG, "jobId: " + jobId);
        }
        // step 2 upload input file
        if (!Utils.isNullOrEmpty(jobId)) {
            boolean success = uploadInputImpl(file, jobId);
            if (success) {
                // step 3 start job
                if (sDebug) {
                    Log.d(TAG, "start job: " + outputName);
                }
                success = startJobImpl(jobId);
                if (success) {
                    // step 4 wait for job to finish
                    success = waitForJobImpl(jobId);
                    if (success) {
                        // step 5 download output file
                        String outputFilePath = downloadOutputImpl(jobId, outputFolder, outputName);
                        if (sDebug && outputFilePath != null) {
                            Log.d(TAG, "downloaded: " + outputFilePath);
                        }
                        // step 6 delete job
                        success = deleteJobImpl(jobId);
                        if (sDebug && success) {
                            // we don't really care too much about whether the delete is successful
                            Log.d(TAG, "jobId deleted: " + jobId);
                        }
                        return outputFilePath;
                    }
                }
            }
        }
        return null;
    }

    /**
     * returns job id created
     */
    @Nullable
    private String createJob(@NonNull String taskType, int taskRevision) throws JSONException, IOException {
        JSONObject jsonObject = new JSONObject();
        JSONArray taskArray = new JSONArray();
        if (mTaskJSON != null) {
            taskArray.put(mTaskJSON);
        } else {
            JSONObject task = new JSONObject();
            task.put(KEY_TaskType, taskType);
            task.put(KEY_TaskRevision, taskRevision);
            taskArray.put(task);
        }
        jsonObject.put(KEY_Tasks, taskArray);

        RequestBody body = RequestBody.create(
                sJSONType,
                jsonObject.toString()
        );

        Call<ResponseBody> response = mService.createWorkflow(mAccessToken, body);
        Response<ResponseBody> result = response.execute();
        if (result.isSuccessful() && result.body() != null) {
            String responseBody = result.body().string();
            JSONObject jsonBody = new JSONObject(responseBody);
            return jsonBody.optString("jobID");
        }
        return null;
    }

    /**
     * returns whether file is successfully uploaded
     */
    private boolean uploadInputImpl(@NonNull File file, @NonNull String jobId) throws IOException {
        MediaType contentType = MediaType.parse("application/pdf");
        RequestBody fileBody = RequestBody.create(contentType, file);
        String filename = URLEncoder.encode(file.getName(), "UTF-8");
        Call<ResponseBody> response = mService.uploadFile(mAccessToken, jobId, filename, fileBody);
        Response<ResponseBody> result = response.execute();
        return result.isSuccessful();
    }

    /**
     * returns whether job is successfully started
     */
    private boolean startJobImpl(@NonNull String jobId) throws IOException {
        return startOrCancelJobImpl(jobId, false);
    }

    /**
     * returns whether job is successfully cancelled
     */
    private boolean cancelJobImpl(@NonNull String jobId) throws IOException {
        return startOrCancelJobImpl(jobId, true);
    }

    /**
     * returns whether job is successfully started
     */
    private boolean startOrCancelJobImpl(@NonNull String jobId, boolean stop) throws IOException {
        String command = KEY_start;
        if (stop) {
            command = KEY_stop;
        }
        Call<ResponseBody> response = mService.startOrCancelJob(mAccessToken, jobId, command);
        Response<ResponseBody> result = response.execute();
        return result.isSuccessful();
    }

    /**
     * returns whether job is successfully finished
     */
    private boolean waitForJobImpl(@NonNull String jobId) throws IOException, JSONException {
        Call<ResponseBody> response = mService.waitForJob(mAccessToken, jobId);
        Response<ResponseBody> result = response.execute();
        if (result.isSuccessful() && result.body() != null) {
            String body = result.body().string();
            if (sDebug) {
                Log.d(TAG, "wait response: " + body);
            }
            JSONObject jsonBody = new JSONObject(body);
            String status = jsonBody.optString(KEY_status);
            boolean finished = jsonBody.optBoolean(KEY_finished);
            if (sDebug) {
                Log.d(TAG, "wait status response: " + status);
            }
            if (KEY_status_completed.equals(status)) {
                return true;
            }
            if (finished) {
                // job finished with a non-successful status
                return false;
            }
            return waitForJobImpl(jobId);
        }
        return false;
    }

    /**
     * returns the output file path
     */
    @Nullable
    private String downloadOutputImpl(@NonNull String jobId, @NonNull File outputFolder, @NonNull String outputName) throws IOException {
        File outputFile = new File(outputFolder, outputName);
        Call<ResponseBody> response = mService.downloadOutput(mAccessToken, jobId, KEY_file);
        Response<ResponseBody> result = response.execute();
        if (result.isSuccessful() && result.body() != null) {
            if (sDebug) {
                Log.d(TAG, "attempt to download: " + outputFile.getAbsolutePath());
            }
            BufferedSink sink = Okio.buffer(Okio.sink(outputFile));
            sink.writeAll(result.body().source());
            sink.close();
            return outputFile.getAbsolutePath();
        }
        if (sDebug) {
            Log.d(TAG, "download error code: " + result.code());
        }
        return null;
    }

    /**
     * returns whether job is successfully deleted
     */
    private boolean deleteJobImpl(@NonNull String jobId) throws IOException {
        Call<ResponseBody> response = mService.deleteJob(mAccessToken, jobId);
        Response<ResponseBody> result = response.execute();

        return result.isSuccessful();
    }

    // === END DYNAMIC FLOW STARTS ===
}
