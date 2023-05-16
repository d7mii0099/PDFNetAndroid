package com.pdftron.android.pdfnetsdksamples.util

import android.widget.ScrollView
import android.widget.TextView
import com.pdftron.android.pdfnetsdksamples.OutputListener
import java.util.*

class LoggingOutputListener(private val mOutputTextView: TextView, private val mOutputScrollView: ScrollView) : OutputListener {

    override fun print(output: String?) {
        mOutputTextView!!.append(output)

        mOutputScrollView!!.post {
            mOutputScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
        }

    }

    override fun println(output: String?) {
        System.out.println(output)
        mOutputTextView!!.append(output + "\n")

        mOutputScrollView!!.post {
            mOutputScrollView!!.fullScroll(ScrollView.FOCUS_DOWN)
        }

    }

    override fun print() {
        print("")
    }

    override fun println() {
        println("")
    }

    override fun printError(errorMessage: String) {
        if (SHOULD_THROW) {
            throw RuntimeException(errorMessage)
        }
        println(errorMessage)
    }

    override fun printError(stackTrace: Array<StackTraceElement>) {
        if (SHOULD_THROW) {
            throw RuntimeException(Arrays.toString(stackTrace))
        }
        for (i in stackTrace.indices) {
            println(stackTrace[i].toString())
        }
    }

    companion object {

        private val SHOULD_THROW = false // flag is used for testing purposes
    }
}
