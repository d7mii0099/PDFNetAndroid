package com.pdftron.collab.client.utils

import com.apollographql.apollo.api.Input
import com.pdftron.collab.client.User
import com.pdftron.collab.client.entities.CollabAnnotation
import com.pdftron.collab.client.entities.CollabMember
import com.pdftron.collab.client.entities.CollabUser
import com.pdftron.collab.client.mutations.AddDocumentMutation
import com.pdftron.collab.client.queries.GetDocumentByIdQuery
import com.pdftron.collab.client.queries.GetUserDocumentsQuery
import com.pdftron.collab.client.type.NewAnnotationInput
import com.pdftron.collab.client.type.UserTypes
import com.pdftron.collab.db.entity.AnnotationEntity
import com.pdftron.collab.service.CustomServiceUtils
import com.pdftron.collab.utils.XfdfUtils
import java.text.SimpleDateFormat
import java.util.*

class CollabClientUtils {

    companion object {

        fun convAnnotationToAnnotationEntity(annotation: CollabAnnotation): AnnotationEntity? {
            val annotationEntity = AnnotationEntity()
            annotationEntity.id = annotation.annotationId
            annotationEntity.serverId = annotation.id
            annotationEntity.documentId = annotation.documentId
            annotationEntity.xfdf = annotation.xfdf
            XfdfUtils.fillAnnotationEntity(annotation.documentId, annotationEntity)
            return if (XfdfUtils.isValidInsertEntity(annotationEntity)) {
                annotationEntity
            } else null
        }

        fun toAnnotationInput(
            documentId: String,
            annotationEntity: AnnotationEntity,
            dateAndTime: String
        ): NewAnnotationInput {
            return NewAnnotationInput(
                Input.absent(),
                CustomServiceUtils.getXfdfFromFile(annotationEntity.xfdf),
                Input.optional(annotationEntity.contents),
                Input.absent(),
                Input.optional(annotationEntity.authorId),
                annotationEntity.id,
                Input.optional(documentId),
                annotationEntity.page,
                Input.optional(annotationEntity.inReplyTo),
                dateAndTime,
                dateAndTime
            )
        }

        fun toUser(author: GetDocumentByIdQuery.Author): CollabUser {
            return CollabUser(author.id, author.userName, author.email, author.type)
        }

        fun toUser(author: GetUserDocumentsQuery.Author): CollabUser {
            return CollabUser(author.id, author.userName, author.email, author.type)
        }

        fun toUser(author: AddDocumentMutation.Author): CollabUser {
            return CollabUser(author.id, author.userName, author.email, author.type)
        }

        fun toMembers(members: List<GetDocumentByIdQuery.Member>): List<CollabMember> {
            val users = ArrayList<CollabMember>()
            for (member in members) {
                users.add(
                    CollabMember(
                        member.id,
                        member.createdAt,
                        CollabUser(
                            member.user.id,
                            member.user.userName,
                            member.user.email,
                            UserTypes.UNKNOWN__
                        )
                    )
                )
            }
            return users
        }

        fun toAnnotations(annotations: List<GetDocumentByIdQuery.Annotation>): List<CollabAnnotation> {
            val annots = ArrayList<CollabAnnotation>()
            for (annotation in annotations) {
                annots.add(
                    CollabAnnotation(
                        annotation.id,
                        annotation.xfdf,
                        annotation.annotationId,
                        annotation.documentId,
                        annotation.author?.id
                    )
                )
            }
            return annots
        }

        fun getMembership(members: List<CollabMember>?, user: User): CollabMember? {
            return members?.firstOrNull { it.user.id == user.collabUser.id }
        }

        fun getDateTime(): String {
            val format = "yyyyMMdd_HHmmss"
            val sdf = SimpleDateFormat(format, Locale.getDefault())
            return sdf.format(Date())
        }
    }
}