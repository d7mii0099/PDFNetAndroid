package com.pdftron.collab.client

import androidx.annotation.WorkerThread
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.subscription.WebSocketSubscriptionTransport
import com.pdftron.collab.client.entities.CollabUser
import com.pdftron.collab.client.utils.CollabAnnotationManager
import com.pdftron.collab.client.utils.CollabAuthManager
import com.pdftron.collab.client.utils.CollabDocumentManager
import com.pdftron.collab.ui.viewer.CollabManager
import com.pdftron.pdf.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Entry point to authenticate and start collaborating with the server.
 */
class CollabClient private constructor(
    private val mUrl: String?,
    private val mSubscriptionUrl: String?
) {
    private lateinit var mApolloClient: ApolloClient
    private var mDocumentManager: CollabDocumentManager? = null
    private var mAnnotationManager: CollabAnnotationManager? = null
    private lateinit var mCollabManager: CollabManager
    private var mUser: User? = null

    val collabManager get() = this.mCollabManager
    val annotationManager get() = this.mAnnotationManager
    val documentManager get() = this.mDocumentManager
    val currentUser get() = this.mUser

    companion object {
        private const val TOKEN_HEADER = "authorization"
        private const val COLLAB_CONTEXT_HEADER = "collab-context"
        private const val COOKIE_FORMAT = "Bearer %s"

        @Volatile
        var sCollabAuthToken: String = ""

        @Volatile
        var sCollabContext: String = ""
    }

    data class Builder(
        var url: String? = null,
        var subscriptionUrl: String? = null
    ) {
        /**
         * Sets the server's Url.
         *
         * @param url server's Url
         * @return the Builder class instance
         */
        fun url(url: String) = apply { this.url = url }

        /**
         * Sets the subscription server's Url.
         *
         * @param subscriptionUrl subscription server's Url
         * @return the Builder class instance
         */
        fun subscriptionUrl(subscriptionUrl: String) =
            apply { this.subscriptionUrl = subscriptionUrl }

        /**
         * builds the CollabClient instance.
         *
         * @return CollabClient class instance
         */
        fun build() = CollabClient(url, subscriptionUrl)
    }

    private class AuthorizationInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val request = chain.request().newBuilder()
                .addHeader(
                    TOKEN_HEADER,
                    if (sCollabAuthToken.isNotEmpty())
                        String.format(COOKIE_FORMAT, sCollabAuthToken)
                    else
                        ""
                )
                .addHeader(COLLAB_CONTEXT_HEADER, sCollabContext)
                .build()
            return chain.proceed(request)
        }
    }

    /**
     * Starts the connection to the server and creates an instance of ApolloClient for all communications.
     *
     * @param collabManager CollabManager instance
     */
    fun start(collabManager: CollabManager) {
        val log = HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY)
        mCollabManager = collabManager
        val okHttpClient = OkHttpClient().newBuilder()
            .addInterceptor(AuthorizationInterceptor())
            .addInterceptor(log)
            .build()
        mApolloClient = ApolloClient.builder()
            .serverUrl(mUrl!!)
            .subscriptionTransportFactory(
                WebSocketSubscriptionTransport.Factory(mSubscriptionUrl!!, okHttpClient)
            )
            .okHttpClient(okHttpClient)
            .build()

        setAnnotationManager(
            CollabAnnotationManager(
                mApolloClient
            )
        )
        setDocumentManager(
            CollabDocumentManager(
                mApolloClient
            )
        )
    }

    /**
     * Logs in a user anonymously.
     * The user's info will be cached and restored if they reload the page and 'loginAnonymously' is called again.
     * @param username The display name to use for the user. Defaults to 'Guest'
     */
    @WorkerThread
    fun loginAnonymous(username: String = "Guest"): User? {
        Utils.throwIfOnMainThread()

        val loginAnonymous = CollabAuthManager.loginAnonymous(mApolloClient, username)
        loginAnonymous?.let { login ->
            login.user?.let {
                val collabUser = CollabUser(it.id, it.userName, it.email, it.type)
                mUser = postLogin(collabUser, login.token)
                return mUser
            }
        }
        return null
    }

    /**
     * Logs in a user with an authentication token.
     * Returns a user entity if the token is valid.
     * Returns null if token is invalid or any other errors occur
     * @param token The token to validate on the server
     */
    @WorkerThread
    fun loginWithToken(token: String): User? {
        Utils.throwIfOnMainThread()

        val loginWithToken = CollabAuthManager.loginWithToken(mApolloClient, token)
        loginWithToken?.let { login ->
            login.user?.let {
                val collabUser = CollabUser(it.id, it.userName, it.email, it.type)
                mUser = postLogin(collabUser, login.token)
                return mUser
            }
        }
        return null
    }

    /**
     * Logs in a user with an email and password
     * Returns a user entity if the password is valid.
     * Returns null if token is invalid or any other errors occur
     * @param email The email to send to the server
     * @param password The password to send to the server
     */
    @WorkerThread
    fun loginWithPassword(email: String, password: String): User? {
        Utils.throwIfOnMainThread()

        val loginWithPassword = CollabAuthManager.loginWithPassword(mApolloClient, email, password)
        loginWithPassword?.let { login ->
            login.user?.let {
                val collabUser = CollabUser(it.id, it.userName, it.email, it.type)
                mUser = postLogin(collabUser, login.token)
                return mUser
            }
        }
        return null
    }

    private fun postLogin(collabUser: CollabUser, token: String?): User {
        mCollabManager.setCurrentUser(collabUser.id, collabUser.userName)
        token?.let {
            sCollabAuthToken = it
        }
        return User(this, collabUser)
    }

    /**
     * Returns the currently signed in user, if it exists
     */
    @WorkerThread
    fun getUserSession(): User? {
        Utils.throwIfOnMainThread()

        val session = CollabAuthManager.getUserSession(mApolloClient)
        if (session?.token != null) {
            return loginWithToken(session.token)
        }
        return null
    }

    /**
     * Destroys all subscriptions at the end of the session.
     */
    fun destroy() {
        mAnnotationManager?.destroySubscription()
        CoroutineScope(Job() + Dispatchers.IO).launch {
            mUser?.logout()
            mDocumentManager = null
            mAnnotationManager = null
            mUser = null
        }
    }

    private fun setAnnotationManager(annotationManager: CollabAnnotationManager) {
        mAnnotationManager = annotationManager
    }

    private fun setDocumentManager(documentManager: CollabDocumentManager) {
        mDocumentManager = documentManager
    }

}