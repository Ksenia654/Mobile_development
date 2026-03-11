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

        loginButton.setOnClickListener {
            val enteredPassword = passwordEditText.text.toString()
            val savedPassword = passwordManager.getPassword()

            if (enteredPassword == savedPassword) {
                Toast.makeText(this, "Доступ предоставлен", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Неверный пароль", Toast.LENGTH_SHORT).show()
            }
        }

        forgotPasswordButton.setOnClickListener {
            passwordManager.resetPassword()
            Toast.makeText(this, "Пароль сброшен", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, SetPasswordActivity::class.java)
            startActivity(intent)
        }
    }
}
