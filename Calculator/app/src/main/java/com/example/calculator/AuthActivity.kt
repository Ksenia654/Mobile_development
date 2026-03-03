package com.example.calculator

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest

class AuthActivity : AppCompatActivity() {
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var encryptedPreferences: EncryptedSharedPreferences

    override fun onStart() {
        super.onStart()
        checkBiometricAvailability()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        findViewById<Button>(R.id.btnBiometric).setOnClickListener {
            checkBiometricAvailability()
        }

        initEncryptedPreferences()
        setupBiometricAuth()
        setupAuthButton()
        setupResetButton()
    }

    private fun setupResetButton() {
        findViewById<Button>(R.id.btnResetPasskey).setOnClickListener {
            showResetConfirmationDialog()
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Reset Passkey")
            .setMessage("Are you sure you want to reset your passkey? This action cannot be undone.")
            .setPositiveButton("Reset") { _, _ ->
                resetPasskey()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetPasskey() {
        try {
            // Очищаем зашифрованные настройки
            encryptedPreferences.edit()
                .remove("PASSKEY")
                .apply()

            // Сбрасываем флаг установки пароля
            getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
                .putBoolean("IS_PASSKEY_SET", false)
                .apply()

            Toast.makeText(this, "Passkey reset successfully", Toast.LENGTH_SHORT).show()

            // Перенаправляем на экран установки нового пароля
            startActivity(Intent(this, FirstLaunchActivity::class.java))
            finish()

        } catch (e: Exception) {
            Toast.makeText(this, "Error resetting passkey: ${e.message}", Toast.LENGTH_LONG).show()
            Log.e("AuthActivity", "Passkey reset error", e)
        }
    }

    private fun initEncryptedPreferences() {
        try {
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
        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка инициализации хранилища", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupAuthButton() {
        findViewById<Button>(R.id.btnAuthenticate).setOnClickListener {
            val enteredPasskey = findViewById<EditText>(R.id.etPasskey).text.toString()

            if (enteredPasskey.isEmpty()) {
                showError("Введите пароль")
                return@setOnClickListener
            }

            if (checkPasskey(enteredPasskey)) {
                startMainActivity()
            } else {
                showError("Неверный пароль")
            }
        }
    }

    private fun showBiometricPrompt() {
        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupBiometricAuth() {
        val executor = ContextCompat.getMainExecutor(this)

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    startMainActivity()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Биометрическая аутентификация")
            .setSubtitle("Используйте отпечаток пальца или Face ID")
            .setNegativeButtonText("Использовать пароль")
            .build()

        // Проверка доступности биометрии
        checkBiometricAvailability()
    }

    private fun checkBiometricAvailability() {
        val biometricManager = BiometricManager.from(this)
        when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS ->
                biometricPrompt.authenticate(promptInfo)
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->
                showError("Биометрия не поддерживается")
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->
                showError("Биометрия временно недоступна")
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->
                showError("Биометрия не настроена")
        }
    }

    private fun checkPasskey(entered: String): Boolean {
        return try {
            val storedHash = encryptedPreferences.getString("PASSKEY", "") ?: ""
            val enteredHash = entered.sha256()
            enteredHash == storedHash
        } catch (e: Exception) {
            false
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun String.sha256(): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(this.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun startMainActivity() {
        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
            .putBoolean("IS_AUTHENTICATED", true)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}