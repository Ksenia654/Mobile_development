package com.example.calculator
import android.view.GestureDetector
import android.view.MotionEvent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView
import java.math.BigDecimal
import java.math.RoundingMode
import android.widget.ImageButton
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var display: TextView
    private var currentNumber = ""
    private var firstOperand: BigDecimal? = null
    private var currentOperator: String? = null
    private var isNewCalculation = true
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var gestureDetector: GestureDetector
    private var isAuthenticated = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appPrefs = getSharedPreferences("auth_prefs", MODE_PRIVATE)
        val isPasskeySet = appPrefs.getBoolean("IS_PASSKEY_SET", false)


        if (!isPasskeySet) {
            // Если пароль не установлен, перенаправляем в FirstLaunchActivity
            startActivity(Intent(this, FirstLaunchActivity::class.java))
            finish()
            return
        }


        // Проверяем, прошёл ли пользователь аутентификацию
        isAuthenticated = appPrefs.getBoolean("IS_AUTHENTICATED", false)

        if (!isAuthenticated) {
            // Если пользователь не аутентифицирован, отправляем в AuthActivity
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        display = findViewById(R.id.display)
        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        gestureDetector = GestureDetector(this, GestureListener())

        display.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }

        FirebaseApp.initializeApp(this)
        signInAnonymously()
        // Инициализация кнопок
        listOf(
            R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4,
            R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9
        ).forEach { buttonId ->
            findViewById<Button>(buttonId).setOnClickListener { appendNumber((it as Button).text.toString()) }
        }
        findViewById<ImageButton>(R.id.btnCopy).setOnClickListener {
            copyToClipboard()
        }
        findViewById<Button>(R.id.btnDot).setOnClickListener { appendDecimalPoint() }

        listOf(
            R.id.btnAdd to "+",
            R.id.btnSubtract to "-",
            R.id.btnMultiply to "*",
            R.id.btnDivide to "/"
        ).forEach { (buttonId, operator) ->
            findViewById<Button>(buttonId).setOnClickListener { setOperator(operator) }
        }

        findViewById<Button>(R.id.btnEquals).setOnClickListener { calculate() }
        findViewById<Button>(R.id.btnClear).setOnClickListener { clear() }

        findViewById<ImageButton>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<ImageButton>(R.id.btnHistory).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }



    }


    override fun onPause() {
        super.onPause()
        getSharedPreferences("auth_prefs", MODE_PRIVATE).edit()
            .putBoolean("IS_AUTHENTICATED", false)
            .apply()
    }

    private fun isPasskeySet(): Boolean {
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

        return encryptedPrefs.getString("PASSKEY", null) != null
    }

    private fun signInAnonymously() {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d("AUTH", "Anonymous auth success")
                } else {
                    Log.e("AUTH", "Auth failed", task.exception)
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }



    private fun copyToClipboard() {
        val textToCopy = display.text.toString()
        if (textToCopy.isNotEmpty()) {
            val clip = ClipData.newPlainText("calculator_result", textToCopy)
            clipboardManager.setPrimaryClip(clip)

            Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_SHORT).show()

            display.animate()
                .scaleX(1.1f)
                .scaleY(1.1f)
                .setDuration(150)
                .withEndAction {
                    display.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .start()
                }.start()
        }
    }

    private fun appendNumber(number: String) {
        if (isNewCalculation) {
            currentNumber = ""
            isNewCalculation = false
        }

        val parts = currentNumber.split(".")
        val integerPart = parts[0]
        val decimalPart = parts.getOrElse(1) { "" }

        when {
            number == "." -> appendDecimalPoint()
            currentNumber.contains(".") && decimalPart.length < 15 -> currentNumber += number
            integerPart.length < 20 -> currentNumber += number
        }

        updateDisplay()
    }

    private fun appendDecimalPoint() {
        if (isNewCalculation) {
            currentNumber = "0"
            isNewCalculation = false
        }
        if (!currentNumber.contains(".")) {
            currentNumber += if (currentNumber.isEmpty()) "0." else "."
            updateDisplay()
        }
    }

    private fun setOperator(operator: String) {
        if (currentNumber.isNotEmpty()) {
            try {
                firstOperand = BigDecimal(currentNumber)
                currentOperator = operator
                isNewCalculation = true
                updateDisplay()
            } catch (e: NumberFormatException) {
                showError("Invalid number format")
                clear()
            }
        } else if (firstOperand != null) {
            currentOperator = operator
        }
    }

    private fun calculate() {
        if (firstOperand == null || currentOperator == null || currentNumber.isEmpty()) return

        try {
            val secondOperand = BigDecimal(currentNumber)
            val result = when (currentOperator) {
                "+" -> firstOperand!!.add(secondOperand)
                "-" -> firstOperand!!.subtract(secondOperand)
                "*" -> firstOperand!!.multiply(secondOperand)
                "/" -> {
                    if (secondOperand.compareTo(BigDecimal.ZERO) == 0) {
                        showError("Cannot divide by zero")
                        return
                    }
                    firstOperand!!.divide(secondOperand, 15, RoundingMode.HALF_UP)
                }
                else -> return
            }
            val expression = "$firstOperand $currentOperator $secondOperand"
            currentNumber = formatNumber(result)
            firstOperand = result
            isNewCalculation = true
            currentOperator = null

            updateDisplay()
            saveToHistory(expression, currentNumber)
        } catch (e: NumberFormatException) {
            showError("Invalid calculation")
            clear()
        }
    }

    data class Calculation(
        val userId: String = "",
        val expression: String = "",
        val result: String = "",
        val timestamp: Date = Date()
    )

    private fun saveToHistory(expression: String, result: String) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val calculation = Calculation(
                userId = user.uid,
                expression = expression,
                result = result
            )

            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("calculations")
                .add(calculation)
                .addOnFailureListener { e ->
                    Log.e("HISTORY", "Ошибка сохранения: ${e.message}")
                }
        }
    }

    fun loadHistory(callback: (List<Calculation>) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .collection("calculations")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { result ->
                    val items = result.map {
                        it.toObject(Calculation::class.java)
                    }
                    callback(items)
                }
                .addOnFailureListener { e ->
                    Log.e("HISTORY", "Ошибка загрузки: ${e.message}")
                    callback(emptyList())
                }
        } ?: callback(emptyList())
    }

    private fun formatNumber(number: BigDecimal): String {
        return number.setScale(15, RoundingMode.HALF_UP)
            .stripTrailingZeros()
            .toPlainString()
    }

    private fun clear() {
        currentNumber = ""
        firstOperand = null
        currentOperator = null
        isNewCalculation = true
        updateDisplay()
    }

    private fun updateDisplay() {
        display.text = when {
            currentNumber.isNotEmpty() -> currentNumber
            firstOperand != null -> formatNumber(firstOperand!!)
            else -> "0"
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("CURRENT_NUMBER", currentNumber)
        outState.putString("FIRST_OPERAND", firstOperand?.toPlainString())
        outState.putString("OPERATOR", currentOperator)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        currentNumber = savedInstanceState.getString("CURRENT_NUMBER") ?: ""
        firstOperand = savedInstanceState.getString("FIRST_OPERAND")?.let { BigDecimal(it) }
        currentOperator = savedInstanceState.getString("OPERATOR")
        updateDisplay()
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            clear()
            Toast.makeText(this@MainActivity, "Input field cleared", Toast.LENGTH_SHORT).show()
            return super.onDoubleTap(e)

        }
    }
}