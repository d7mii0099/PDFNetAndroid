//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2018 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------

package com.pdftron.android.pdfnetsdksamples

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import com.pdftron.android.pdfnetsdksamples.util.Utils
import com.pdftron.pdf.PDFNet
import com.pdftron.pdfnet.TrialKeyProvider

/**
 * An activity representing a list of Samples.
 */
class SampleListActivity : FragmentActivity(), SampleListFragment.Callbacks {

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet device.
     */
    private var mTwoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_sample_list)

        if (findViewById<View>(R.id.sample_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-large and
            // res/values-sw600dp). If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true

            // In two-pane mode, list items should be given the
            // 'activated' state when touched.
            (supportFragmentManager
                    .findFragmentById(R.id.sample_list) as SampleListFragment)
                    .setActivateOnItemClick(true)
        }

        try {
            PDFNet.initialize(this, R.raw.pdfnet, TrialKeyProvider.getLicenseKey(this))
        } catch (e: Exception) {
            showLicenseRequestDialog()
            return
        }

    }

    private fun showLicenseRequestDialog() {
        val dialog = AlertDialog.Builder(this)
                .setTitle(R.string.missing_license_key)
                .setMessage(Html.fromHtml(getString(R.string.missing_license_key_msg)))
                .setCancelable(false)
                .create()
        dialog.show()
        (dialog.findViewById(android.R.id.message) as TextView).movementMethod = LinkMovementMethod.getInstance()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.menu_about -> {
                Utils.showAbout(this)
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    /**
     * Callback method from [SampleListFragment.Callbacks]
     * indicating that the item with the given ID was selected.
     */
    override fun onItemSelected(id: Int) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            val arguments = Bundle()
            arguments.putInt(SampleDetailFragment.ARG_SAMPLE_ID, id)
            val fragment = SampleDetailFragment()
            fragment.arguments = arguments
            supportFragmentManager.beginTransaction()
                    .replace(R.id.sample_detail_container, fragment)
                    .commit()
        } else {
            // In single-pane mode, simply start the detail activity
            // for the selected item ID.
            val detailIntent = Intent(this, SampleDetailActivity::class.java)
            detailIntent.putExtra(SampleDetailFragment.ARG_SAMPLE_ID, id)
            startActivity(detailIntent)
        }
    }

    companion object {
        var instance: SampleListActivity? = null
            private set
    }
}
