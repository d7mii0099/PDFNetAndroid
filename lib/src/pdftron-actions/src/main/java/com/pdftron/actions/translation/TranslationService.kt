package com.pdftron.actions.translation

import android.util.Log
import com.microsoft.azure.storage.CloudStorageAccount
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.*
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class TranslationService {

    companion object {
        const val endpoint =
            "https://pdftron-test2.cognitiveservices.azure.com/translator/text/batch/v1.0/"
        const val batch = "batches"

        const val STATUS_SUCCEED = "Succeeded"
        const val STATUS_NOT_STARTED = "NotStarted"
        const val STATUS_RUNNING = "Running"
    }

    interface TranslationServiceInterface {
        @POST(batch)
        fun translate(
            @HeaderMap headers: Map<String, String>,
            @Body requestBody: RequestBody
        ): Call<ResponseBody>

        @GET
        fun getTranslationJobStatus(
            @Url url: String,
            @HeaderMap headers: Map<String, String>
        ): Call<ResponseBody>
    }

    init {
        val client = OkHttpClient.Builder()
            .readTimeout(300, TimeUnit.SECONDS)
            .build()
        val retrofit: Retrofit = Retrofit.Builder()
            .baseUrl(endpoint)
            .client(client)
            .build()
        retrofit.create(TranslationServiceInterface::class.java).also { mService = it }
    }

    private var mService: TranslationServiceInterface

    fun uploadBlob(file: File) {
        val account = CloudStorageAccount.parse(Config.sourceContainerConnectionString)
        val blobClient = account.createCloudBlobClient()
        val container = blobClient.getContainerReference("translator-source")
        if (container.exists()) {
            val blob = container.getBlockBlobReference(file.name)
            blob.uploadFromFile(file.absolutePath)
        }
    }

    fun downloadBlob(fileName: String, targetOutputFile: File) {
        val account = CloudStorageAccount.parse(Config.sourceContainerConnectionString)
        val blobClient = account.createCloudBlobClient()
        val container = blobClient.getContainerReference("translator-target")
        if (container.exists()) {
            val blob = container.getBlockBlobReference(fileName)
            if (blob.exists()) {
                Log.d("translate", "downloading: " + targetOutputFile.absolutePath)
                blob.downloadToFile(targetOutputFile.absolutePath)
            }
        }
    }

    @Throws(IOException::class)
    fun translate(sourceName: String, targetName: String, language: String): String? {
        val mediaType: MediaType? = MediaType.parse("application/json")

        val jsonParam = JSONObject()
        val jsonArray = JSONArray()
        val param = JSONObject()
        param.put("storageType", "File")

        val sourceParam = JSONObject()
        val sourceUrl = buildBlobUrl(Config.sourceUrl, Config.sourceSAS, sourceName)
        Log.d("translate", "source: $sourceUrl")
        sourceParam.put("sourceUrl", sourceUrl)
        param.put("source", sourceParam)

        val targetParam = JSONArray()
        val lang1 = JSONObject()
        val targetUrl = buildBlobUrl(Config.targetUrl, Config.targetSAS, targetName)
        Log.d("translate", "target: $targetUrl")
        lang1.put("targetUrl", targetUrl)
        lang1.put("language", language)
        targetParam.put(lang1)
        param.put("targets", targetParam)

        jsonArray.put(param)
        jsonParam.put("inputs", jsonArray)

        val body: RequestBody = RequestBody.create(
            mediaType,
            jsonParam.toString()
        )

        val headers = HashMap<String, String>()
        headers["Ocp-Apim-Subscription-Key"] = Config.subscriptionKey
        headers["Content-type"] = "application/json"

        val response = mService.translate(headers, body)
        response.execute().let {
            Log.d("translate", "code: " + it.code())
            if (it.isSuccessful) {
                val opLocation = it.headers().get("operation-location")
                Log.d("translate", "op: $opLocation")
                return opLocation
            }
            // TODO error handling
        }
        return null
    }

    fun getTranslationJobStatus(operationLocation: String): String? {
        val headers = HashMap<String, String>()
        headers["Ocp-Apim-Subscription-Key"] = Config.subscriptionKey

        val response = mService.getTranslationJobStatus(operationLocation, headers)
        response.execute().let {
            if (it.isSuccessful) {
                val body = it.body()!!.string()
                Log.d("translate", body)
                val jsonBody = JSONObject(body)
                val status = jsonBody["status"]
                return if (status == STATUS_SUCCEED) {
                    STATUS_SUCCEED
                } else if (status == STATUS_RUNNING || status == STATUS_NOT_STARTED) {
                    val retry = it.headers()["Retry-After"]
                    var retryInterval = 1 // sec
                    try {
                        retryInterval = retry?.toInt()!!
                    } catch (ignored: Exception) {
                    }
                    Thread.sleep(retryInterval.toLong() * 1000)
                    getTranslationJobStatus(operationLocation)
                } else {
                    null
                }
            }
        }
        return null
    }

    private fun buildBlobUrl(base: String, sasToken: String, fileName: String?): String {
        return if (fileName != null) {
            "$base/$fileName?$sasToken"
        } else {
            "$base?$sasToken"
        }
    }

}