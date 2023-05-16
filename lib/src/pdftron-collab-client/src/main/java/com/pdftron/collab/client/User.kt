package com.pdftron.collab.client

import android.util.Log
import androidx.annotation.WorkerThread
import com.pdftron.collab.client.entities.CollabDocument
import com.pdftron.collab.client.entities.CollabUser
import com.pdftron.collab.client.utils.CollabClientUtils
import com.pdftron.collab.db.entity.AnnotationEntity
import com.pdftron.pdf.utils.Utils

class User(private val collabClient: CollabClient, val collabUser: CollabUser) {

    companion object {
        private const val TAG = "User"
    }

    /**
     * Gets a [[Document]] belonging to the user. Returns null if the document could not be found
     * @param id The ID of the document
     */
    @WorkerThread
    fun getDocument(id: String): Document? {
        Utils.throwIfOnMainThread()

        val data = collabClient.documentManager?.getDocumentById(id)
        data?.let {
            val collabDocument = CollabDocument(
                it.id,
                it.name,
                it.isPublic,
                it.unreadCount,
                it.createdAt,
                it.updatedAt,
                CollabClientUtils.toUser(it.author),
                CollabClientUtils.toAnnotations(it.annotations),
                CollabClientUtils.toMembers(it.members)
            )
            return Document(collabClient, collabDocument)
        }
        return null
    }

    /**
     * Gets all documents for the current user
     *
     * WARNING:
     * This function can become slow if the user belongs to a lot of documents.
     */
    @WorkerThread
    fun getAllDocuments(): ArrayList<Document> {
        Utils.throwIfOnMainThread()

        val result = ArrayList<Document>()
        val docs = collabClient.documentManager?.getUserDocuments(collabUser.id)
        docs?.let {
            for (doc in it) {
                val document = Document(
                    collabClient, CollabDocument(
                        doc.id,
                        doc.name,
                        null,
                        doc.unreadCount,
                        doc.createdAt,
                        doc.updatedAt,
                        CollabClientUtils.toUser(doc.author)
                    )
                )
                result.add(document)
            }
        }
        return result
    }

    /**
     * Creates a document entity in the database. The author of the document will be this user.
     */
    @WorkerThread
    fun createDocument(
        documentId: String,
        documentName: String,
        isPublic: Boolean,
        annotations: List<AnnotationEntity>? = null
    ): Document? {
        Utils.throwIfOnMainThread()

        val existingDoc = getDocument(documentId)
        if (existingDoc != null) {
            Log.w(TAG, "Document $documentId already exists")
            return existingDoc
        }

        val doc = collabClient.documentManager?.addDocument(
            collabUser.id, documentId, documentName, isPublic, annotations
        )
        doc?.let {
            return Document(
                collabClient, CollabDocument(
                    it.id,
                    it.name,
                    it.isPublic,
                    0,
                    it.createdAt,
                    it.createdAt,
                    CollabClientUtils.toUser(it.author)
                )
            )
        }
        return null
    }

    /**
     * Logs out the current user.
     */
    @WorkerThread
    fun logout() {
        Utils.throwIfOnMainThread()

        collabClient.collabManager.currentDocument?.let {
            collabClient.documentManager?.disconnectUserToDocument(
                it,
                collabUser.id
            )
        }

    }

}