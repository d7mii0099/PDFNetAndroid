package com.pdftron.showcase.activities

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import com.pdftron.pdf.config.PDFViewCtrlConfig
import com.pdftron.pdf.config.ToolManagerBuilder
import com.pdftron.pdf.config.ViewerConfig
import com.pdftron.pdf.utils.AnalyticsHandlerAdapter
import com.pdftron.pdf.utils.CommonToast
import com.pdftron.pdf.utils.Utils
import com.pdftron.showcase.R
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.content_bottom_sheet.*
import java.io.File

class DigitalSignatureActivity : FeatureActivity() {

    private val DEFAULT_KEYSTORE = "https://pdftron.s3.amazonaws.com/downloads/android/pdftron.pfx"
    private val compositeDisposable = CompositeDisposable()

    private var certFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        sampleFileName = "digital_signature"
        super.onCreate(savedInstanceState)

        downloadCert()
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

    private fun downloadCert() {
        certFile = File(
            cacheDir,
            "demo_keystore_password_is_password.pfx"
        )
        if (certFile?.exists() == false) { // need to download the keystore if it doesn't exist
            if (hasInternetConnection(this)) {
                compositeDisposable.add(
                    Utils.simpleHTTPDownload(DEFAULT_KEYSTORE, certFile)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnError {
                            val runtimeException = RuntimeException(it)
                            AnalyticsHandlerAdapter.getInstance().sendException(runtimeException)
                            CommonToast.showText(
                                this,
                                R.string.dig_sig_keystore_download_error,
                                Toast.LENGTH_LONG
                            )
                            throw runtimeException
                        }
                        .doOnSubscribe {
                            val keystoreMsg =
                                String.format(
                                    resources.getString(R.string.dig_sig_default),
                                    certFile!!.absolutePath
                                )

                            val preText = feature_description.text.toString()
                            feature_description.text = ("$preText\n\n$keystoreMsg")
                        }
                        .subscribe({
                            setCert()
                        }) { throwable -> throwable.printStackTrace() }
                )
            } else {
                CommonToast.showText(this, R.string.dig_sig_network, Toast.LENGTH_LONG)
            }
        } else {
            setCert()

            val keystoreMsg =
                String.format(
                    resources.getString(R.string.dig_sig_default),
                    certFile!!.absolutePath
                )
            val preText = feature_description.text.toString()
            feature_description.text = ("$preText\n\n$keystoreMsg")
        }
    }

    private fun hasInternetConnection(context: Context?): Boolean {
        if (context == null) {
            return false
        }
        val cm =
            context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = cm.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }

    override fun handleTabDocumentLoaded(tag: String) {
        super.handleTabDocumentLoaded(tag)

        setCert()
    }

    private fun setCert() {
        certFile?.let {
            mPdfViewCtrlTabHostFragment?.currentPdfViewCtrlFragment?.toolManager?.setDigitalSignatureKeystorePath(
                it.absolutePath
            )
            mPdfViewCtrlTabHostFragment?.currentPdfViewCtrlFragment?.toolManager?.digitalSignatureKeystorePassword =
                "password"
        }
    }

    override fun getViewerConfig(): ViewerConfig {
        val pdfviewCtrlConfig = PDFViewCtrlConfig.getDefaultConfig(this)
        pdfviewCtrlConfig.isThumbnailUseEmbedded = false
        val toolManagerBuilder = ToolManagerBuilder.from()
            .setUseDigitalSignature(true)
        val builder = ViewerConfig.Builder()
        return builder
            .multiTabEnabled(true)
            .showCloseTabOption(false)
            .pdfViewCtrlConfig(pdfviewCtrlConfig)
            .toolManagerBuilder(toolManagerBuilder)
            .build()
    }

}