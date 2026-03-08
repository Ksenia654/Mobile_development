package com.example.calculatorapp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.R
import com.example.calculatorapp.utils.PasswordManager

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var passwordManager: PasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        passwordManager = PasswordManager(this)

        val newPasswordEditText: EditText = findViewById(R.id.newPasswordEditText)
        val confirmNewPasswordEditText: EditText = findViewById(R.id.confirmNewPasswordEditText)
        val resetPasswordButton: Button = findViewById(R.id.resetPasswordButton)
        val forgotPasswordButton: Button = findViewById(R.id.forgotPasswordButton)
        val passwordEditText: EditText = findViewById(R.id.passwordEditText)

        resetPasswordButton.setOnClickListener {
            val savedPassword = passwordManager.getPassword()
            val enteredPassword = passwordEditText.text.toString()

            val newPassword = newPasswordEditText.text.toString()
            val confirmNewPassword = confirmNewPasswordEditText.text.toString()

            if (savedPassword == enteredPassword)
            {
                if (newPassword.length >= 4)
                {
                    if (newPassword == confirmNewPassword) {
                        passwordManager.savePassword(newPassword)
                        Toast.makeText(this, "Пароль изменён", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Новые пароли не совпадают", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Пароль слишком короткий", Toast.LENGTH_SHORT).show()
                }
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
