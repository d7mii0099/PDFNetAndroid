package com.pdftron.collab.client

import android.util.Log
import androidx.annotation.WorkerThread
import com.pdftron.collab.client.entities.CollabAnnotation
import com.pdftron.collab.client.entities.CollabDocument
import com.pdftron.collab.client.entities.CollabMember
import com.pdftron.collab.client.entities.CollabUser
import com.pdftron.collab.client.utils.CollabClientUtils
import com.pdftron.pdf.utils.Utils

class Document(private val collabClient: CollabClient, val collabDocument: CollabDocument) {

    companion object {
        private const val TAG = "Document"
    }

    /**
     * Adds the current user to the document.
     * Returns true on success, or false if not allowed.
     */
    @WorkerThread
    fun join(): Boolean {
        Utils.throwIfOnMainThread()

        val isPublic = collabDocument.isPublic ?: false
        if (!isPublic) {
            Log.w(TAG, "User cannot join a non-public document without an invite")
            return false
        }

        val isMember = this.isMember()
        if (isMember) {
            Log.w(TAG, "User is already a member of the document")
            return false
        }

        return this.inviteUsers(listOf(collabClient.currentUser!!.collabUser))
    }

    /**
     * Leaves the current document
     * Returns a boolean representing if the operation was successful
     */
    @WorkerThread
    fun leave(): Boolean {
        Utils.throwIfOnMainThread()

        val isMember = this.isMember()
        if (!isMember) {
            Log.w(TAG, "User is not a member of the document")
            return false
        }

        collabClient.currentUser?.let { user ->
            val member = CollabClientUtils.getMembership(getMembers(), user)
            member?.let {
                collabClient.documentManager?.deleteDocumentMember(it.id)
                collabClient.documentManager?.disconnectUserToDocument(
                    collabDocument.id,
                    it.user.id
                )
                return true
            }
        }
        return false
    }

    /**
     * A boolean representing if the current user is the author of the document
     */
    fun isAuthor(): Boolean {
        return collabDocument.author.id == collabClient.currentUser!!.collabUser.id
    }

    /**
     * Determines if the current user is a member of the document
     */
    fun isMember(): Boolean {
        return collabDocument.members?.any { member ->
            member.user.id == collabClient.currentUser!!.collabUser.id
        } ?: false
    }

    /**
     * Determines if the current user can join the document.
     * The criteria for joining a document are:
     * 1) The document must be public
     * 2) The user must not already be a member
     * 3) A user must be logged in
     */
    fun canJoin(): Boolean {
        val isMember = isMember()
        val isPublic = collabDocument.isPublic ?: false
        return !isMember && isPublic
    }

    /**
     * Invites a list of users to a document
     * @param users A list of users to invite
     *
     * Throws if user does not have permission to invite.
     *
     * Returns true on success.
     */
    @WorkerThread
    fun inviteUsers(users: List<CollabUser>): Boolean {
        Utils.throwIfOnMainThread()

        val data = collabClient.documentManager?.inviteUsers(
            collabDocument.id,
            users
        )
        return data != null
    }

    /**
     * Gets a list of all members of the document
     */
    fun getMembers(): List<CollabMember>? {
        return collabDocument.members
    }

    /**
     * Gets a list of all annotations for the document.
     */
    fun getAnnotations(): List<CollabAnnotation>? {
        return collabDocument.annotations
    }

    /**
     * Starts subscriptions
     */
    @WorkerThread
    fun view() {
        Utils.throwIfOnMainThread()

        collabClient.documentManager?.loadDocument(
            this,
            collabClient.currentUser!!.collabUser.id,
            collabClient.collabManager
        )
        collabClient.annotationManager?.addSubscription(
            collabDocument.id,
            collabClient.collabManager,
            collabClient.currentUser!!.collabUser.id
        )
    }

}