package com.pdftron.showcase

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.Window
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.pdftron.pdf.utils.PdfViewCtrlSettingsManager
import com.pdftron.pdf.utils.Utils
import com.pdftron.showcase.helpers.AppUtils
import com.pdftron.showcase.helpers.Helpers
import com.pdftron.showcase.helpers.SafeClickListener
import com.pdftron.showcase.helpers.SimpleScannerActivity
import com.pdftron.showcase.models.Feature
import com.pdftron.showcase.models.FeatureCategory
import com.pdftron.showcase.models.FeaturedApps
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.content_no_search_results.*
import okhttp3.OkHttpClient
import java.io.IOException
import java.io.Serializable

class MainActivity : AppCompatActivity() {

    private val TAG = "MainActivity"

    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mViewAdapter: CategoryAdapter
    private lateinit var mViewManager: RecyclerView.LayoutManager

    private var hasSearchText: Boolean = false

    private lateinit var featuredApps: FeaturedApps
    private lateinit var featureCategories: ArrayList<FeatureCategory>
    private lateinit var filteredFeatures: ArrayList<Feature>
    private lateinit var onItemClick: (Feature) -> Unit
    private lateinit var searchText: String
    private lateinit var menu: Menu
    private lateinit var manager: SearchManager
    private lateinit var search: SearchView

    // barcode
    private var mHandleBarcode = false
    private var mBarcodeLink: String? = null

    // Disposables
    private var mHttpClient: OkHttpClient? = null
    private var mDisposables = CompositeDisposable()

    private val FEATURE_REQUEST = 1000
    private val CAMERA_PERMISSION_REQUEST = 1001
    private val SCANNER_REQUEST = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // default day mode
        if (Utils.isDeviceNightMode(this)) {
            PdfViewCtrlSettingsManager.setColorMode(
                this,
                PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NIGHT
            )
        } else {
            PdfViewCtrlSettingsManager.setColorMode(
                this,
                PdfViewCtrlSettingsManager.KEY_PREF_COLOR_MODE_NORMAL
            )
        }
        requestWindowFeature(Window.FEATURE_ACTION_BAR)
        supportActionBar?.show()
        title = "Explore Apryse SDK"
        setContentView(R.layout.activity_main)

        var fileText: String = ""
        try {
            fileText = application.applicationContext.assets.open("features_category.json")
                .bufferedReader().use {
                    it.readText()
                }
        } catch (e: IOException) {
            Log.e(TAG, "failed in opening features_category")
        }
        this.featuredApps = Gson().fromJson(fileText, FeaturedApps::class.java)
        this.featureCategories = featuredApps?.categories

        filteredFeatures = featureCategories[featureCategories.count() - 1].features

        onItemClick = { feature ->
            startNewFeatureActvity(feature, null)
        }

        mViewManager = androidx.recyclerview.widget.LinearLayoutManager(this).apply {
            isMeasurementCacheEnabled = false
        }
        mViewAdapter =
            CategoryAdapter(this, featureCategories, filteredFeatures, hasSearchText, onItemClick)
        mRecyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = mViewManager
            setHasFixedSize(true)
            adapter = mViewAdapter
        }

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        val appLinkAction = intent.action
        val appLinkData: Uri? = intent.data
        if (Intent.ACTION_VIEW == appLinkAction && appLinkData != null) {
            var activityId = appLinkData.lastPathSegment
            var shareId: String? = null
            var fileUrl = appLinkData.getQueryParameter("fileUrl");
            if (appLinkData.toString().contains(Helpers.SHARE_ID)) {
                // deep linking with share id will look like
                // DocumentCollaboration/shareId/<id>
                val segments = appLinkData.pathSegments
                activityId = segments[segments.size - 3]
                shareId = segments[segments.size - 1]
            }
            Uri.parse("app://open.pdftron.showcase/home")
                .buildUpon()
                .appendPath(activityId)
                .build().also { appData ->

                    val args = Bundle()
                    if (shareId != null) {
                        var shareIdLink = appLinkData.toString()
                        shareIdLink = shareIdLink.replace(
                            "/" + Helpers.SHARE_ID + "/",
                            "?" + Helpers.SHARE_ID + "="
                        )
                        args.putString(Helpers.WVS_LINK_KEY, shareIdLink)
                    }
                    if (fileUrl != null && fileUrl != "null") {
                        args.putString(Helpers.FILE_URL, fileUrl)
                    }
                    startNewFeatureActvityById(activityId!!, args)

                }
        }
    }

    override fun onResumeFragments() {
        super.onResumeFragments()

        if (mHandleBarcode && mBarcodeLink != null) {
            mHandleBarcode = false
            if (mBarcodeLink!!.contains("pdftron.com")) {
                // first fetch file url
                val uri = Uri.parse(mBarcodeLink)
                val shareId = uri.getQueryParameter(Helpers.SHARE_ID)
                if (!Utils.isNullOrEmpty(shareId)) {
                    // launch activity
                    val args = Bundle()
                    args.putString(Helpers.SHARE_ID, shareId)
                    startNewFeatureActvityById("DocumentCollaboration", args)
                }
            } else {
                Toast.makeText(
                    this,
                    "Please scan the QR code at https://www.pdftron.com/webviewer/demo/document-collaboration",
                    LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDisposables.clear()
    }

    private fun startNewFeatureActvityById(activityId: String, args: Bundle?) {
        val feature = findFeatureById(activityId)
        if (feature != null) startNewFeatureActvity(feature, args)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        this.menu = menu
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {

            manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager

            search = menu.findItem(R.id.menu_search).actionView as SearchView
            search.maxWidth = Integer.MAX_VALUE

            search.setSearchableInfo(manager.getSearchableInfo(componentName))

            val menuItem = menu.findItem(R.id.menu_search) as MenuItem
            menuItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                    return true
                }

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    searchText = ""
                    hasSearchText = false
                    updateAdapter()
                    no_results_page.visibility = View.INVISIBLE
                    return true
                }
            })


            search.setOnQueryTextListener(object : SearchView.OnQueryTextListener {

                override fun onQueryTextChange(query: String): Boolean {
                    hasSearchText = true
                    if (query == "") {
                        showCategoryList()
                    } else {
                        search.suggestionsAdapter.changeCursor(null)
                        searchText = query.trim()
                        updateSearch()
                        updateAdapter()
                    }
                    return true
                }

                override fun onQueryTextSubmit(query: String): Boolean {
                    // task HERE
                    hasSearchText = true
                    search.clearFocus()
                    return true
                }
            })

        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item!!.itemId == R.id.menu_scan) {
            if (!AppUtils.hasCameraPermission(this)) {
                AppUtils.requestCameraPermissions(this, CAMERA_PERMISSION_REQUEST)
            } else {
                startScannerActivity()
            }
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showCategoryList() {

        val c = MatrixCursor(arrayOf("_id", "name"))
        for (i in 0..featureCategories.size - 1) {
            c.addRow(arrayOf(i.toString(), featureCategories[i].name!!))
        }

        search.suggestionsAdapter = CategoryListCursorAdapter(this, c, this, search)
    }

    private fun match(result: String, feature: Feature): Boolean {
        if ((feature.name?.toLowerCase()!!
                .contains(searchText.toLowerCase())) || (feature.category?.toLowerCase()!!
                .contains(searchText.toLowerCase()))
        ) {
            return true
        }
        for (tag in feature.tags) {
            if (tag.toLowerCase().contains(result) || result.toLowerCase().contains(tag)) {
                return true
            }
        }
        return false
    }

    private fun updateSearch() {
        filteredFeatures = ArrayList<Feature>()
        for (category in featureCategories) {
            for (feature in category.features) if (match(
                    searchText,
                    feature
                ) && filteredFeatures.contains(feature) == false
            ) {
                filteredFeatures.add(feature)
            }
        }

        val newRelatedFeatures = ArrayList<Feature>()
        for (feature in filteredFeatures) {
            for (relatedFeature in feature.relatedFeatures) {
                val f = findFeatureById(relatedFeature)
                if (f != null && !filteredFeatures.contains(f)) {
                    newRelatedFeatures.add(f)
                }
            }
        }
        filteredFeatures.addAll(newRelatedFeatures)
    }

    private fun findFeatureById(id: String): Feature? {
        for (featureCategory in featureCategories) {
            for (feature in featureCategory.features) {
                if (id == feature.id) {
                    return feature
                }
            }
        }
        return null
    }

    private fun updateAdapter() {
        mRecyclerView.adapter =
            CategoryAdapter(this, featureCategories, filteredFeatures, hasSearchText, onItemClick)
        if (filteredFeatures.count() > 0) {
            no_results_page.visibility = View.INVISIBLE
        } else {
            no_results_page.visibility = View.VISIBLE
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == FEATURE_REQUEST) {
                val feature = data?.extras?.get("feature")
                if (feature != null) {
                    startNewFeatureActvity(feature as Feature, data, null)
                }
            } else if (requestCode == SCANNER_REQUEST) {
                mHandleBarcode = true
                mBarcodeLink = data?.getStringExtra("scan_result")
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (AppUtils.verifyPermissions(grantResults)) {
                startScannerActivity()
            }
        }
    }

    private fun startScannerActivity() {
        val intent = Intent(this, SimpleScannerActivity::class.java)
        startActivityForResult(intent, SCANNER_REQUEST)
    }

    private fun startNewFeatureActvity(feature: Feature, bundle: Bundle?) {
        startNewFeatureActvity(feature, null, bundle)
    }

    private fun startNewFeatureActvity(feature: Feature, data: Intent?, bundle: Bundle?) {
        var intent = Intent()
        try {
            intent.setClassName(packageName, packageName + ".activities." + feature.id + "Activity")
            intent.putExtra("feature", feature as Serializable)
            if (data != null) {
                intent.putExtras(data)
            }
            if (bundle != null) {
                intent.putExtras(bundle)
            }
            startActivityForResult(intent, FEATURE_REQUEST)
        } catch (error: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "Class Not Exist for this feature. Or there's something wrong when opening this activity",
                LENGTH_SHORT
            ).show()
        }
    }

    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
    }

    fun updateSearchCategory(id: Long) {
        hasSearchText = true
        searchText = featureCategories[id.toInt()].name!!
        search.setQuery(searchText, false)
        filteredFeatures = featureCategories[id.toInt()].features
        updateAdapter()
    }

}

fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {

    val safeClickListener = SafeClickListener {
        onSafeClick(it)
    }
    setOnClickListener(safeClickListener)
}