package com.pdftron.collab.client.entities

import com.pdftron.collab.client.type.UserTypes

data class CollabUser(
    val id: String,
    val userName: String?,
    val email: String?,
    val type: UserTypes,
    val pageNumber: Int? = null
)