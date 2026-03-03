package com.example.calculator
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await


object ThemeManager {
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"

    fun applyTheme(theme: String) {
        when (theme) {
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    suspend fun saveThemeToCloud(theme: String) {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .set(mapOf("theme" to theme), SetOptions.merge())
    }

    suspend fun loadThemeFromCloud(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            val snapshot = FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .get()
            snapshot.await().getString("theme")
        } catch (e: Exception) {
            null
        }
    }
}