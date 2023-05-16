package com.pdftron.collab.client.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.api.Response
import com.apollographql.apollo.rx2.rxMutate
import com.apollographql.apollo.rx2.rxSubscribe
import com.pdftron.collab.client.mutations.AddAnnotationMutation
import com.pdftron.collab.client.mutations.DeleteAnnotationMutation
import com.pdftron.collab.client.mutations.EditAnnotationMutation
import com.pdftron.collab.client.subscriptions.OnAnnotationChangedSubscription
import com.pdftron.collab.client.type.ChangeEventTypes
import com.pdftron.collab.client.type.EditAnnotationInput
import com.pdftron.collab.client.type.QuerySettings
import com.pdftron.collab.client.type.XFDFTypes
import com.pdftron.collab.db.entity.AnnotationEntity
import com.pdftron.collab.service.CustomServiceUtils
import com.pdftron.collab.ui.viewer.CollabManager
import com.pdftron.collab.utils.XfdfUtils
import com.pdftron.pdf.utils.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import io.reactivex.subscribers.DisposableSubscriber

class CollabAnnotationManager(
    apolloClient: ApolloClient
) {
    companion object {
        private val TAG = CollabAnnotationManager::class.qualifiedName
    }

    private val mDisposables = CompositeDisposable()
    private var mApolloClient: ApolloClient = apolloClient

    fun addSubscription(docId: String, collabManager: CollabManager, userId: String) {
        //Receives remote changes from server
        subscribeOnAnnotationChanged(collabManager, userId)

        //Sends local changes to server
        collabManager.setCollabManagerListener { _: String?, annotations: ArrayList<AnnotationEntity>?, _: String?, _: String? ->
            if (annotations != null) {
                for (annotation in annotations) {
                    when (annotation.at) {
                        XfdfUtils.OP_ADD -> addAnnotation(
                            collabManager,
                            docId,
                            annotation
                        )
                        XfdfUtils.OP_MODIFY -> modifyAnnotation(annotation)
                        XfdfUtils.OP_REMOVE -> deleteAnnotation(annotation)
                    }
                }
            }
        }
    }

    @WorkerThread
    private fun addAnnotation(
        collabManager: CollabManager,
        documentId: String,
        annotationEntity: AnnotationEntity
    ) {
        Utils.throwIfOnMainThread()
        val newAnnotationInput = CollabClientUtils.toAnnotationInput(
            documentId, annotationEntity, CollabClientUtils.getDateTime()
        )
        val observable = mApolloClient.rxMutate(
            AddAnnotationMutation(newAnnotationInput)
        )
        val data = observable.blockingGet().data
        if (data != null) {
            val annotationId = annotationEntity.id
            val serverId = data.addAnnotation.id
            annotationEntity.serverId = serverId
            collabManager.updateAnnotationServerIdSync(annotationId, serverId)
        }
    }

    @WorkerThread
    private fun modifyAnnotation(annotationEntity: AnnotationEntity) {
        Utils.throwIfOnMainThread()
        if (annotationEntity.serverId == null) {
            throw NullPointerException("ServerId cannot be null")
        }
        val editAnnotationInput = EditAnnotationInput(
            Input.optional(CustomServiceUtils.getXfdfFromFile(annotationEntity.xfdf)),
            Input.optional(annotationEntity.contents),
            Input.optional(annotationEntity.page),
            Input.absent(),
            CollabClientUtils.getDateTime()
        )
        val observable = mApolloClient.rxMutate(
            EditAnnotationMutation(
                annotationEntity.serverId, editAnnotationInput
            )
        )
        Log.e(
            TAG, observable.blockingGet().data?.editAnnotation?.toString()
                ?: "EditAnnotationMutation Failed."
        )
    }

    @WorkerThread
    private fun deleteAnnotation(annotationEntity: AnnotationEntity) {
        Utils.throwIfOnMainThread()
        if (annotationEntity.serverId == null) {
            throw NullPointerException("ServerId cannot be null")
        }
        val observable = mApolloClient.rxMutate(
            DeleteAnnotationMutation(annotationEntity.serverId)
        )
        Log.e(
            TAG, observable.blockingGet().data?.deleteAnnotation?.toString()
                ?: "DeleteAnnotationMutation Failed."
        )
    }

    private fun subscribeOnAnnotationChanged(
        collabManager: CollabManager,
        userId: String
    ) {
        val querySetting = QuerySettings(Input.optional(XFDFTypes.COMMAND_ALL))
        mDisposables.add(
            mApolloClient.rxSubscribe(
                OnAnnotationChangedSubscription(
                    userId,
                    Input.optional(querySetting)
                )
            )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(object :
                    DisposableSubscriber<Response<OnAnnotationChangedSubscription.Data>>() {
                    override fun onNext(it: Response<OnAnnotationChangedSubscription.Data>?) {
                        val data = it?.data
                        if (data != null) {
                            val annotationChanged = data.annotationChanged
                            if (annotationChanged.annotation.author != null &&
                                annotationChanged.annotation.author.id != userId
                            ) {
                                when (annotationChanged.action) {
                                    ChangeEventTypes.ADD, ChangeEventTypes.EDIT ->
                                        collabManager.importAnnotationCommand(annotationChanged.annotation.xfdf)
                                    ChangeEventTypes.DELETE ->
                                        collabManager.deleteAnnotation(annotationChanged.annotation.annotationId)
                                    else -> {
                                    }
                                }
                            }
                        }
                    }

                    override fun onError(e: Throwable?) {
                        Log.e(TAG, e?.message, e)
                    }

                    override fun onComplete() {
                        Log.d(TAG, "Subscription exhausted")
                    }
                }
                )
        )
    }

    fun destroySubscription() {
        mDisposables.clear()
    }
}