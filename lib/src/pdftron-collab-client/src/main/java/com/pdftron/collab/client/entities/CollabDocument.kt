package com.pdftron.collab.client.entities

data class CollabDocument(
    val id: String,
    val name: String?,
    val isPublic: Boolean?,
    val unreadCount: Int,
    val createdAt: Any,
    val updatedAt: Any,
    val author: CollabUser,
    val annotations: List<CollabAnnotation>? = null,
    val members: List<CollabMember>? = null,
)