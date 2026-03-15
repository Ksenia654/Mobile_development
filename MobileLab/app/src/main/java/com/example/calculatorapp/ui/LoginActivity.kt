package com.example.calculatorapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import com.example.calculatorapp.R
import com.example.calculatorapp.utils.PasswordManager

class LoginActivity : AppCompatActivity() {

    private lateinit var passwordManager: PasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        passwordManager = PasswordManager(this)

        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val loginButton: Button = findViewById(R.id.loginButton)
        val forgotPasswordButton: Button = findViewById(R.id.forgotPasswordButton)
        val faceIdButton: Button = findViewById(R.id.faceIdButton)   // ← добавили Face ID кнопку

        // --- Вход по паролю (оставлено без изменений) ---
        loginButton.setOnClickListener {
            val enteredPassword = passwordEditText.text.toString()
            val savedPassword = passwordManager.getPassword()

            if (enteredPassword == savedPassword) {
                Toast.makeText(this, "Доступ предоставлен", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("loggedIn", true)
                startActivity(intent)

                finish()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Вход по Face ID ---
        faceIdButton.setOnClickListener {
            if (canUseBiometrics()) {
                showBiometricPrompt()
            } else {
                Toast.makeText(this, "Биометрия недоступна", Toast.LENGTH_SHORT).show()
            }
        }

        // --- Сброс пароля (оставлено без изменений) ---
        forgotPasswordButton.setOnClickListener {
            passwordManager.resetPassword()
            Toast.makeText(this, "Пароль сброшен", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SetPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    // --- Биометрия ---

    private fun canUseBiometrics(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)

        val biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    openMainScreen()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(this@LoginActivity, "Ошибка: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(this@LoginActivity, "Не удалось распознать", Toast.LENGTH_SHORT).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Вход по Face ID")
            .setSubtitle("Используйте биометрию для входа")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK

                        or BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun openMainScreen() {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("loggedIn", true)
        startActivity(intent)
        finish()
    }
}
