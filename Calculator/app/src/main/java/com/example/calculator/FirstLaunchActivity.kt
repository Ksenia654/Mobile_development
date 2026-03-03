package com.example.calculator

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

// FirstLaunchActivity.kt
class FirstLaunchActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_first_launch)

        findViewById<Button>(R.id.btnSetup).setOnClickListener {
            val passkey = findViewById<EditText>(R.id.etPasskey).text.toString()
            val confirm = findViewById<EditText>(R.id.etConfirm).text.toString()

            if (passkey.length < 4) {
                showError("Пароль должен содержать минимум 4 символа")
                return@setOnClickListener
            }

            if (passkey != confirm) {
                showError("Пароли не совпадают")
                return@setOnClickListener
            }

            savePasskey(passkey)
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
        }
    }

    private fun savePasskey(passkey: String) {
        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        val encryptedPrefs = EncryptedSharedPreferences.create(
            applicationContext,
            "passkey_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
            .putBoolean("IS_PASSKEY_SET", true)
            .apply()

        encryptedPrefs.edit()
            .putString("PASSKEY", passkey.sha256())
            .apply()


    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun String.sha256(): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

