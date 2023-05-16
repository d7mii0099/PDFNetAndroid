package com.pdftron.collab.client.entities

data class CollabAnnotation(
    val id: String,
    val xfdf: String,
    val annotationId: String,
    val documentId: String,
    val authorId: String?,
)