package com.pdftron.collab.client.utils

import androidx.annotation.WorkerThread
import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Input
import com.apollographql.apollo.rx2.rxMutate
import com.apollographql.apollo.rx2.rxQuery
import com.pdftron.collab.client.mutations.LoginAnonymousMutation
import com.pdftron.collab.client.mutations.LoginMutation
import com.pdftron.collab.client.queries.GetSessionQuery
import com.pdftron.pdf.utils.Utils

internal class CollabAuthManager {

    companion object {

        @WorkerThread
        fun loginAnonymous(
            apolloClient: ApolloClient,
            anonymousUsername: String
        ): LoginAnonymousMutation.LoginAnonymous? {
            Utils.throwIfOnMainThread()

            val data = apolloClient.rxMutate(
                LoginAnonymousMutation(Input.optional(anonymousUsername))
            ).blockingGet().data
            return data?.loginAnonymous
        }

        @WorkerThread
        fun loginWithToken(apolloClient: ApolloClient, token: String): LoginMutation.Login? {
            Utils.throwIfOnMainThread()

            val data = apolloClient.rxMutate(
                LoginMutation(Input.absent(), Input.absent(), Input.optional(token))
            ).blockingGet().data
            return data?.login
        }

        @WorkerThread
        fun loginWithPassword(
            apolloClient: ApolloClient,
            email: String,
            password: String
        ): LoginMutation.Login? {
            Utils.throwIfOnMainThread()

            val data = apolloClient.rxMutate(
                LoginMutation(Input.optional(email), Input.optional(password), Input.absent())
            ).blockingGet().data
            return data?.login
        }

        @WorkerThread
        fun getUserSession(apolloClient: ApolloClient): GetSessionQuery.Session? {
            Utils.throwIfOnMainThread()

            return apolloClient.rxQuery(GetSessionQuery()).blockingFirst().data?.session
        }
    }
}