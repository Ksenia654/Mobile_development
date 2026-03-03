package com.example.calculator

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

object HistoryManager {
    fun saveCalculation(operation: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("calculations")
            .add(mapOf(
                "operation" to operation,
                "timestamp" to FieldValue.serverTimestamp()
            ))
    }

    suspend fun getHistory(): List<String> {
        val user = FirebaseAuth.getInstance().currentUser ?: return emptyList()
        return try {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("calculations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()
                .map { it.getString("operation") ?: "" }
        } catch (e: Exception) {
            emptyList()
        }
    }
}