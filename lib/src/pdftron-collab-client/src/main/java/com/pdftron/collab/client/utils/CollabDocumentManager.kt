package com.pdftron.collab.client.utils

import android.util.Log
import androidx.annotation.WorkerThread
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.rx2.rxMutate
import com.apollographql.apollo.rx2.rxQuery
import com.pdftron.collab.client.Document
import com.pdftron.collab.client.entities.CollabUser
import com.pdftron.collab.client.mutations.*
import com.pdftron.collab.client.queries.GetDocumentByIdQuery
import com.pdftron.collab.client.queries.GetUserDocumentsQuery
import com.pdftron.collab.client.type.*
import com.pdftron.collab.db.entity.AnnotationEntity
import com.pdftron.collab.ui.viewer.CollabManager
import com.pdftron.pdf.utils.Utils

class CollabDocumentManager(
    apolloClient: ApolloClient
) {
    companion object {
        private val TAG = CollabDocumentManager::class.qualifiedName
    }

    private var mApolloClient: ApolloClient = apolloClient

    @WorkerThread
    fun loadDocument(document: Document, userId: String, collabManager: CollabManager) {
        Utils.throwIfOnMainThread()

        val collabDocument = document.collabDocument

        // set document
        collabManager.setCurrentDocument(collabDocument.id)

        // add users
        collabDocument.members?.let {
            for (member in it) {
                collabManager.addUser(member.user.id, member.user.userName)
            }
        }
        // add annotations
        collabDocument.annotations?.let {
            for (annotation in it) {
                val annotationEntity = CollabClientUtils.convAnnotationToAnnotationEntity(
                    annotation
                )
                if (annotationEntity != null) {
                    collabManager.addAnnotation(annotationEntity)
                }
            }
        }
        // connect to session
        connectUserToDocument(collabDocument.id, userId)
    }

    @WorkerThread
    fun addDocument(
        userId: String,
        documentId: String,
        documentName: String,
        isPublic: Boolean,
        annotations: List<AnnotationEntity>? = null
    ): AddDocumentMutation.AddDocument? {
        Utils.throwIfOnMainThread()

        val newDocumentInput = NewDocumentInput(
            Input.optional(documentId),
            Input.optional(documentName),
            userId,
            Input.optional(isPublic),
            CollabClientUtils.getDateTime(),
            CollabClientUtils.getDateTime()
        )
        val newAnnotsInput = ArrayList<NewAnnotationInput>()
        annotations?.let {
            for (annot in annotations) {
                val annotInput = CollabClientUtils.toAnnotationInput(
                    documentId, annot, CollabClientUtils.getDateTime()
                )
                newAnnotsInput.add(annotInput)
            }
        }
        val observable = mApolloClient.rxMutate(
            AddDocumentMutation(
                newDocumentInput,
                Input.optional(newAnnotsInput)
            )
        )
        return observable.blockingGet().data?.addDocument
    }

    @WorkerThread
    fun inviteUsers(
        documentId: String,
        users: List<CollabUser>
    ): InviteUsersToDocumentMutation.Data? {
        Utils.throwIfOnMainThread()

        val toInvite = ArrayList<InvitedUserInput>()
        for (user in users) {
            val inviteInput = InvitedUserInput(
                Input.optional(user.id),
                Input.optional(user.userName),
                Input.optional(user.email)
            )
            toInvite.add(inviteInput)
        }
        val observable = mApolloClient.rxMutate(
            InviteUsersToDocumentMutation(documentId, Input.optional(toInvite))
        )
        return observable.blockingGet().data
    }

    @WorkerThread
    fun connectUserToDocument(documentId: String, userId: String) {
        Utils.throwIfOnMainThread()

        val observable = mApolloClient.rxMutate(
            ConnectUserToDocumentMutation(
                documentId, userId
            )
        )
        Log.e(
            TAG,
            observable.blockingGet().data?.connectUserToDocument?.toString()
                ?: "ConnectUserToDocumentMutation Failed."
        )
    }

    @WorkerThread
    fun disconnectUserToDocument(documentId: String, userId: String) {
        Utils.throwIfOnMainThread()

        val observable = mApolloClient.rxMutate(
            DeleteConnectedDocUserMutation(
                DeleteConnectedDocUserInput(documentId, userId)
            )
        )
        Log.e(
            TAG,
            observable.blockingGet().data?.deleteConnectedDocUser?.toString()
                ?: "DeleteConnectedDocUserMutation Failed."
        )
    }

    @WorkerThread
    fun deleteDocumentMember(memberId: String) {
        Utils.throwIfOnMainThread()

        val deleteMemberInput = DeleteDocumentMemberInput(
            memberId
        )
        val observable = mApolloClient.rxMutate(
            LeaveDocumentMutation(
                deleteMemberInput
            )
        )
        Log.e(
            TAG,
            observable.blockingGet().data?.deleteDocumentMember?.toString()
                ?: "LeaveDocumentMutation Failed."
        )
    }

    @WorkerThread
    fun getDocumentById(documentId: String): GetDocumentByIdQuery.Document? {
        Utils.throwIfOnMainThread()

        val observable = mApolloClient.rxQuery(GetDocumentByIdQuery(documentId))
        return observable.blockingFirst().data?.document
    }

    @WorkerThread
    fun getUserDocuments(userId: String): List<GetUserDocumentsQuery.Document>? {
        Utils.throwIfOnMainThread()

        val observable = mApolloClient.rxQuery(GetUserDocumentsQuery(userId))
        return observable.blockingFirst().data?.user?.documents
    }
}