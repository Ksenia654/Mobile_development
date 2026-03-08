package com.example.calculatorapp.ui

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.calculatorapp.R
import com.example.calculatorapp.utils.PasswordManager
import android.content.Intent

class SetPasswordActivity : AppCompatActivity() {

    private lateinit var passwordManager: PasswordManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_set_password)

        passwordManager = PasswordManager(this)

        val passwordEditText: EditText = findViewById(R.id.passwordEditText)
        val confirmPasswordEditText: EditText = findViewById(R.id.confirmPasswordEditText)
        val setPasswordButton: Button = findViewById(R.id.setPasswordButton)

        setPasswordButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (password.length >= 4) {
                if (password == confirmPassword) {
                    passwordManager.savePassword(password)
                    Toast.makeText(this, "Пароль установлен", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)

                    startActivity(intent)

                    finish()
                } else {
                    Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Слишком короткий пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
