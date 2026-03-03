package com.example.calculator

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.launch
import java.security.MessageDigest

class SettingsActivity : AppCompatActivity() {
    private lateinit var encryptedPreferences: EncryptedSharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Initialize buttons
        val btnLight = findViewById<Button>(R.id.btnLightTheme)
        val btnDark = findViewById<Button>(R.id.btnDarkTheme)

        // Set click listeners
        btnLight.setOnClickListener { setTheme(ThemeManager.THEME_LIGHT) }
        btnDark.setOnClickListener { setTheme(ThemeManager.THEME_DARK) }

        val masterKey = MasterKey.Builder(applicationContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPreferences = EncryptedSharedPreferences.create(
            applicationContext,
            "passkey_store",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        ) as EncryptedSharedPreferences

        findViewById<Button>(R.id.btnChangePasskey).setOnClickListener {
            val old = findViewById<EditText>(R.id.etOldPasskey).text.toString().sha256()
            val new = findViewById<EditText>(R.id.etNewPasskey).text.toString()
            val confirm = findViewById<EditText>(R.id.etConfirmPasskey).text.toString()

            if (old != encryptedPreferences.getString("PASSKEY", "")) {
                showError("Invalid current passkey")
                return@setOnClickListener
            }

            if (new != confirm) {
                showError("New passkeys do not match")
                return@setOnClickListener
            }

            if (new.length < 4) {
                showError("Passkey must be at least 4 characters")
                return@setOnClickListener
            }

            encryptedPreferences.edit()
                .putString("PASSKEY", new.sha256())
                .apply()

            Toast.makeText(this, "Passkey changed successfully", Toast.LENGTH_SHORT).show()
            finish()

        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun String.sha256(): String {
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(toByteArray()).joinToString("") { "%02x".format(it) }
    }

    private fun setTheme(theme: String) {
        lifecycleScope.launch {
            ThemeManager.saveThemeToCloud(theme)
            ThemeManager.applyTheme(theme)
            recreate()  // Recreate activity to apply theme
        }
    }
}