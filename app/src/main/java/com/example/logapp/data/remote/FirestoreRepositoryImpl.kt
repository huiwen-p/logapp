package com.example.logapp.data.remote

import com.example.logapp.data.local.entity.ActivitySessionEntity
import com.example.logapp.data.remote.model.SessionDocument
import com.example.logapp.domain.repository.FirestoreRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.Instant
import javax.inject.Inject
import kotlin.coroutines.resume

class FirestoreRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : FirestoreRepository {

    override suspend fun uploadSession(userId: String, session: ActivitySessionEntity): Result<Unit> = suspendCancellableCoroutine { continuation ->
        val document = SessionDocument(
            id = session.id,
            activityId = session.activityId,
            startedAt = session.startedAt.toEpochMilli(),
            endedAt = session.endedAt?.toEpochMilli(),
            note = session.note,
            createdAt = session.createdAt.toEpochMilli(),
            updatedAt = session.updatedAt.toEpochMilli(),
            deletedAt = session.deletedAt?.toEpochMilli(),
            version = session.version
        )
        
        firestore.collection("users").document(userId)
            .collection("sessions").document(session.id)
            .set(document)
            .addOnSuccessListener {
                continuation.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }

    override suspend fun pullSessions(userId: String, lastSyncTime: Long): Result<List<ActivitySessionEntity>> = suspendCancellableCoroutine { continuation ->
        firestore.collection("users").document(userId)
            .collection("sessions")
            .whereGreaterThan("updatedAt", lastSyncTime)
            .get()
            .addOnSuccessListener { snapshot ->
                try {
                    val entities = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(SessionDocument::class.java)?.let { docModel ->
                            ActivitySessionEntity(
                                id = docModel.id,
                                activityId = docModel.activityId,
                                startedAt = Instant.ofEpochMilli(docModel.startedAt),
                                endedAt = docModel.endedAt?.let { Instant.ofEpochMilli(it) },
                                note = docModel.note,
                                createdAt = Instant.ofEpochMilli(docModel.createdAt),
                                updatedAt = Instant.ofEpochMilli(docModel.updatedAt),
                                deletedAt = docModel.deletedAt?.let { Instant.ofEpochMilli(it) },
                                version = docModel.version
                            )
                        }
                    }
                    continuation.resume(Result.success(entities))
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                }
            }
            .addOnFailureListener { e ->
                continuation.resume(Result.failure(e))
            }
    }
}
