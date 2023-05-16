//package com.pdftron.showcase.activities
//
//import android.content.ClipData
//import android.content.Context
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.ViewGroup
//import android.widget.ImageView
//import android.widget.LinearLayout
//import android.widget.TextView
//import android.widget.Toast
//import com.aventrix.jnanoid.jnanoid.NanoIdUtils
//import com.pdftron.collab.client.CollabClient
//import com.pdftron.collab.ui.viewer.CollabViewerBuilder2
//import com.pdftron.collab.ui.viewer.CollabViewerTabHostFragment2
//import com.pdftron.pdf.Annot
//import com.pdftron.pdf.controls.PdfViewCtrlTabHostFragment2
//import com.pdftron.pdf.tools.QuickMenu
//import com.pdftron.pdf.tools.QuickMenuItem
//import com.pdftron.pdf.tools.ToolManager
//import com.pdftron.pdf.utils.CommonToast
//import com.pdftron.pdf.utils.Utils
//import com.pdftron.pdf.widget.toolbar.component.DefaultToolbars
//import com.pdftron.showcase.R
//import com.pdftron.showcase.helpers.AppUtils
//import com.pdftron.showcase.helpers.Helpers.MOBILE
//import com.pdftron.showcase.helpers.Helpers.SHARE_ID
//import kotlinx.android.synthetic.main.content_bottom_sheet.*
//import kotlinx.android.synthetic.main.content_bottomsheet_button.*
//import kotlinx.android.synthetic.main.control_button_simple.*
//import kotlinx.coroutines.*
//import net.datafaker.Faker
//
//class DocumentCollaborationActivity : FeatureActivity() {
//
//    companion object {
//        private val TAG = DocumentCollaborationActivity::class.simpleName
//        private const val FILE_BUCKET = "https://pdftron.s3.amazonaws.com/downloads/pl/"
//        private const val DEFAULT_FILE_URL = "${FILE_BUCKET}webviewer-demo.pdf"
//        private const val DEFAULT_DEMO_URL =
//            "https://www.pdftron.com/webviewer/demo/document-collaboration"
//        private const val BARCODE_SIZE = 600
//        private const val BARCODE_SIZE_WIDTH = 660
//        private const val BARCODE_SIZE_HEIGHT = 264
//    }
//
//    private var mCollabClient = CollabClient.Builder()
//        .url("https://collab-server.pdftron.com")
//        .subscriptionUrl("wss://collab-server.pdftron.com/subscribe")
//        .build()
//
//    private var mShareId: String? = null
//    private val mFileUrl = DEFAULT_FILE_URL
//
//    private lateinit var mUsernameTextView: TextView
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        sampleFileName = "PDFTRON_about"
//        annotationLayerEnabled = false // TODO, check with core if possible to turn this on
//        initialToolbarTag = DefaultToolbars.TAG_ANNOTATE_TOOLBAR
//
//        super.onCreate(savedInstanceState)
//
//        val bottomSheetContainer = feature_content_container
//        layoutInflater.inflate(R.layout.control_button_simple, bottomSheetContainer, true)
//    }
//
//    override fun onPostCreate(savedInstanceState: Bundle?) {
//        super.onPostCreate(savedInstanceState)
//        addControl()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        mCollabClient.destroy()
//    }
//
//    override fun handleTabDocumentLoaded(tag: String) {
//        super.handleTabDocumentLoaded(tag)
//        if (mPdfViewCtrlTabHostFragment is CollabViewerTabHostFragment2) {
//            val collabHost = mPdfViewCtrlTabHostFragment as CollabViewerTabHostFragment2
//            val collabManager = collabHost.collabManager
//
//            if (collabManager != null) {
//                mShareId = getRandomShareId()
//
//                if (intent != null) {
//                    val shareId = intent.extras?.getString(SHARE_ID)
//                    if (!Utils.isNullOrEmpty(shareId)) {
//                        mShareId = shareId
//                        Log.d(TAG, "parse share id: $mShareId")
//                    }
//                }
//
//                if (mShareId == null) {
//                    Log.e(TAG, "Collaboration share id is null")
//                    return
//                }
//
//                mCollabClient.start(collabManager)
//
//                CoroutineScope(Job() + Dispatchers.IO).launch {
//
//                    var user = mCollabClient.getUserSession()
//                    if (user == null) {
//                        user = mCollabClient.loginAnonymous(getRandomName())
//                    }
//                    var document = user?.getDocument(mShareId!!)
//                    if (document == null) {
//                        Log.d(TAG, "document NOT FOUND, creating a new one")
//                        document = user?.createDocument(
//                            mShareId!!,
//                            "new_collab.pdf",
//                            true,
//                            collabManager.annotations
//                        )
//                    } else {
//                        Log.d(TAG, "document FOUND")
//                    }
//                    document?.let {
//                        Log.d(
//                            TAG,
//                            "start session: $mShareId with user ${user?.collabUser?.userName}"
//                        )
//                        it.join()
//                        it.view()
//
//                        withContext(Dispatchers.Main) {
//                            mUsernameTextView.text = "Your username: ${user?.collabUser?.userName}"
//                        }
//                    }
//                }
//
//            }
//        }
//    }
//
//    private fun addControl() {
//        val qrCodeImageView = ImageView(this).apply {
//            val lp = LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT
//            )
//            layoutParams = lp
//        }
//        val uri = getUriString(DEFAULT_DEMO_URL)
//        val bitmap = AppUtils.createImage(
//            uri,
//            "QR Code",
//            BARCODE_SIZE,
//            BARCODE_SIZE_WIDTH,
//            BARCODE_SIZE_HEIGHT
//        )
//        qrCodeImageView.setImageBitmap(bitmap)
//        button_container.addView(qrCodeImageView)
//
//        button.text = getString(R.string.collab_export_link)
//        button.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_copy_black_24dp, 0, 0, 0)
//        button.setOnClickListener {
//            copyLink()
//        }
//
//        mUsernameTextView = TextView(this, null, 0, R.style.RobotoTextViewStyle).apply {
//            val lp = LinearLayout.LayoutParams(
//                ViewGroup.LayoutParams.WRAP_CONTENT,
//                ViewGroup.LayoutParams.WRAP_CONTENT
//            )
//                .apply {
//                    setMargins(
//                        0,
//                        resources.getDimension(R.dimen.content_text_top_margin).toInt(),
//                        0,
//                        0
//                    )
//                }
//            layoutParams = lp
//        }
//        control_button_simple.addView(mUsernameTextView)
//
//        getPdfViewCtrlTabFragment()!!.addQuickMenuListener(object : ToolManager.QuickMenuListener {
//
//            override fun onQuickMenuClicked(menuItem: QuickMenuItem?): Boolean {
//                if (menuItem?.itemId == R.id.qm_note) {
//                    closeBottomSheet()
//                    return false
//                }
//                return false
//            }
//
//            override fun onShowQuickMenu(quickmenu: QuickMenu?, annot: Annot?): Boolean {
//                return false
//            }
//
//            override fun onQuickMenuShown() {
//            }
//
//            override fun onQuickMenuDismissed() {
//            }
//
//        })
//    }
//
//    override fun createPdfViewerFragment(
//        fileName: String,
//        isUrl: Boolean
//    ): PdfViewCtrlTabHostFragment2 {
//        return CollabViewerBuilder2
//            .withUri(Uri.parse(mFileUrl))
//            .usingConfig(getViewerConfig())
//            .usingNavIcon(R.drawable.ic_arrow_back_white_24dp)
//            .usingTheme(R.style.PDFTronAppTheme)
//            .build(this, CollabViewerTabHostFragment2::class.java)
//    }
//
//    private fun copyLink() {
//        if (Utils.isNullOrEmpty(mShareId)) {
//            return
//        }
//        try {
//            val uri = getUriString(DEFAULT_DEMO_URL)
//            Log.d(TAG, "copyLink: $uri")
//            val clipboard =
//                getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
//            val clip = ClipData.newPlainText("text", uri)
//            clipboard.setPrimaryClip(clip)
//            CommonToast.showText(
//                this,
//                resources.getString(R.string.tools_copy_confirmation),
//                Toast.LENGTH_SHORT
//            )
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//    }
//
//    private fun getUriString(urlString: String): String {
//        return Uri.parse(urlString)
//            .buildUpon()
//            .appendQueryParameter(SHARE_ID, mShareId)
//            .appendQueryParameter(MOBILE, "1")
//            .build().toString()
//    }
//
//    private fun getRandomName(): String {
//        val faker = Faker()
//        val firstName = try {
//            faker.name().firstName()
//        } catch (e: Exception) {
//            "John"
//        }
//        return firstName
//    }
//
//    private fun getRandomShareId(): String {
//        return NanoIdUtils.randomNanoId()
//    }
//}
