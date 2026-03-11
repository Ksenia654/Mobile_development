package com.example.calculatorapp.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class PasswordManager(context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(password: String) {
        val editor = sharedPreferences.edit()
        editor.putString("user_password", password)
        editor.apply()
    }

    fun getPassword(): String? {
        return sharedPreferences.getString("user_password", null)
    }

    fun isPasswordSet(): Boolean {
        return sharedPreferences.contains("user_password")
    }

    fun resetPassword() {
        val editor = sharedPreferences.edit()
        editor.remove("user_password")
        editor.apply()
    }
}
