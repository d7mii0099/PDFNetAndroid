//---------------------------------------------------------------------------------------
// Copyright (c) 2001-2019 by PDFTron Systems Inc. All Rights Reserved.
// Consult legal.txt regarding legal and license information.
//---------------------------------------------------------------------------------------
package com.pdftron.android.pdfnetsdksamples

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.pdftron.android.pdfnetsdksamples.util.LoggingOutputListener
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

/**
 * A fragment representing a single Sample detail screen.
 */
class SampleDetailFragment
/**
 * Mandatory empty constructor for the fragment manager to instantiate the
 * fragment (e.g. upon screen orientation changes).
 */
    : Fragment() {
    /**
     * The sample content this fragment is presenting.
     */
    private var mSample: PDFNetSample? = null
    private var mOutputListener: OutputListener? = null
    private var mOutputScrollView: ScrollView? = null
    private var mOutputTextView: TextView? = null
    protected val mDisposables = CompositeDisposable()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments!!.containsKey(ARG_SAMPLE_ID)) {
            // Load the sample content specified by the fragment arguments.
            mSample = MiscellaneousSamplesApplication.instance!!
                    .content.get(arguments!!.getInt(ARG_SAMPLE_ID))
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_sample_detail, container, false)

        // Show the sample description as text in a TextView.
        if (mSample != null) {
            (rootView.findViewById<View>(R.id.sample_detail_textview) as TextView).setText(mSample!!.description)
        }
        mOutputScrollView = rootView.findViewById<View>(R.id.sample_output_scrollview) as ScrollView
        mOutputTextView = rootView.findViewById<View>(R.id.sample_output_textview) as TextView
        mOutputListener = LoggingOutputListener(mOutputTextView!!, mOutputScrollView!!)
        val buttonRun = rootView.findViewById<View>(R.id.sample_run_button) as Button
        buttonRun.setOnClickListener { v ->
            if (mSample!!.title.contains(
                            v.context.resources.getString(R.string.sample_digitalsignatures_title))) {
                mDisposables.add(
                        Completable.fromAction { mSample!!.runOnBackground() }
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe({ mSample!!.run(mOutputListener) }) { throwable -> throwable.printStackTrace() }
                )
            } else {
                mSample!!.run(mOutputListener)
            }
        }
        val buttonOpenFiles = rootView.findViewById<View>(R.id.sample_open_files_button) as Button
        buttonOpenFiles.setOnClickListener {
            val listFilesDialog = ListFilesDialogFragment(ArrayList(mSample!!.files))
            listFilesDialog.show(activity!!.supportFragmentManager, "list_files_dialog_fragment")
        }
        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mDisposables.clear()
    }

    companion object {
        /**
         * The fragment argument representing the item ID that this fragment
         * represents.
         */
        const val ARG_SAMPLE_ID = "sample_id"
    }
}