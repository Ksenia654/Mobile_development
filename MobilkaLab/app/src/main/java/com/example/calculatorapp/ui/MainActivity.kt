package com.example.calculatorapp.ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculatorapp.BuildConfig
import com.example.calculatorapp.R
import com.example.calculatorapp.utils.PasswordManager
import com.example.calculatorapp.viewmodel.CalculatorViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging

private val REQUEST_RECORD_AUDIO_PERMISSION = 200

class MainActivity : AppCompatActivity() {
    private lateinit var inputField: EditText
    private lateinit var debug: TextView
    private val viewModel: CalculatorViewModel by viewModels() // ViewModel

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var button: Button
    private lateinit var passwordManager: PasswordManager
    private var isUserLoggedIn = false
    private var isScientificMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        setContentView(R.layout.activity_main)

        inputField = findViewById(R.id.inputField)
        debug = findViewById(R.id.debug)

        // Проверка режима приложения
        if (BuildConfig.IS_DEMO) {
            Toast.makeText(this, "Demo version", Toast.LENGTH_SHORT).show()
            isScientificMode = false
        } else {
            Toast.makeText(this, "Full version", Toast.LENGTH_SHORT).show()
            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                isScientificMode = true
            } else {
                // В портретной ориентации можно будет переключать режим позже
                isScientificMode = false // или true — как хочешь по умолчанию
            }
        }
        updateCalculatorMode()

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Ошибка получения токена", task.exception)
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d("FCM", "FCM Token: $token")
        }

        checkAudioPermission()

        themeCreate()
        setupButtons()
        observeViewModel()
        speechCreate()

        val btnShowHistory = findViewById<Button>(R.id.btnShowHistory)
        btnShowHistory.setOnClickListener {
            showHistoryBottomSheet()
        }

        isUserLoggedIn = savedInstanceState?.getBoolean("isUserLoggedIn", false) ?: false
        passwordManager = PasswordManager(this)

        if (!isUserLoggedIn) {
            if (!passwordManager.isPasswordSet()) {
                val intent = Intent(this, SetPasswordActivity::class.java)
                startActivity(intent)
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                onUserLoggedIn()
            }
        }
    }

    private fun updateCalculatorMode() {
        val scientificButtons = listOf(
            R.id.buttonSin, R.id.buttonCos, R.id.buttonTan,
            R.id.buttonSqrt, R.id.buttonPower, R.id.buttonInverse,
            R.id.buttonReverse, R.id.buttonModul,
            R.id.buttonLeftBracket, R.id.buttonRightBracket
        )

        for (id in scientificButtons) {
            val view = findViewById<View>(id)
            view.visibility = if (isScientificMode) View.VISIBLE else View.GONE
        }

        val modeText = if (isScientificMode) "Научный режим" else "Обычный режим"
        Toast.makeText(this, modeText, Toast.LENGTH_SHORT).show()
    }

    private fun checkAudioPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isUserLoggedIn", isUserLoggedIn)
    }

    fun onUserLoggedIn() {
        isUserLoggedIn = true
    }

    private fun showHistoryBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_history, null)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewHistory)

        recyclerView.layoutManager = LinearLayoutManager(this)

        loadHistory { historyList ->
            recyclerView.adapter = HistoryAdapter(historyList) { selectedItem ->
                inputField.setText(selectedItem)
                dialog.dismiss()
            }
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun loadHistory(callback: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        db.collection("history")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val list = mutableListOf<String>()
                for (document in documents) {
                    val expression = document.getString("expression") ?: ""
                    list.add(expression)
                }
                callback(list)
            }
    }

    fun themeCreate() {
        val themeSwitch: Switch = findViewById(R.id.themeSwitch)

        loadTheme { currentTheme, backgroundColor, memoryButtonColor, numberButtonColor, operatorButtonColor, clearButtonColor,
                    clearText, numberText, operatorText, memoryText, bar ->

            themeSwitch.isChecked = currentTheme == "dark"

            applyTheme(this, currentTheme, backgroundColor, memoryButtonColor, numberButtonColor, operatorButtonColor, clearButtonColor,
                clearText, numberText, operatorText, memoryText, bar)
        }

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            val newTheme = if (isChecked) "dark" else "light"

            val db = FirebaseFirestore.getInstance()
            val docRef = db.collection("themes").document("b7DGBOqfbogQfqQY8hS1")

            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val themeData = hashMapOf("currentTheme" to newTheme)
                    docRef.update(themeData as Map<String, Any>)
                        .addOnSuccessListener {
                            Log.d(TAG, "Theme successfully updated.")
                            loadTheme { currentTheme, backgroundColor, memoryButtonColor, numberButtonColor, operatorButtonColor, clearButtonColor,
                                        clearText, numberText, operatorText, memoryText, bar ->
                                applyTheme(this, currentTheme, backgroundColor, memoryButtonColor, numberButtonColor, operatorButtonColor, clearButtonColor,
                                    clearText, numberText, operatorText, memoryText, bar)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating theme", e)
                        }
                } else {
                    val themeData = hashMapOf(
                        "currentTheme" to newTheme,
                        "light" to createDefaultLightTheme(),
                        "dark" to createDefaultDarkTheme()
                    )
                    docRef.set(themeData)
                        .addOnSuccessListener {
                            Log.d(TAG, "Theme successfully created.")
                            loadTheme { currentTheme, backgroundColor, memoryButtonColor, numberButtonColor, operatorButtonColor, clearButtonColor,
                                        clearText, numberText, operatorText, memoryText, bar ->
                                applyTheme(this, currentTheme, backgroundColor, memoryButtonColor, numberButtonColor, operatorButtonColor, clearButtonColor,
                                    clearText, numberText, operatorText, memoryText, bar)
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error creating theme", e)
                        }
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error checking document existence", e)
            }
        }
    }

    private fun createDefaultLightTheme(): Map<String, String> {
        return mapOf(
            "backgroundColor" to "#FFFFFF",
            "bar" to "#FFFFFF",
            "memoryButton" to "#F0F0F0",
            "numberButton" to "#FFFFFF",
            "operatorButton" to "#F0F0F0",
            "clearButton" to "#F0F0F0",
            "clearButtonText" to "#000000",
            "numberButtonText" to "#000000",
            "operatorButtonText" to "#000000",
            "memoryButtonText" to "#000000"
        )
    }

    private fun createDefaultDarkTheme(): Map<String, String> {
        return mapOf(
            "backgroundColor" to "#000000",
            "bar" to "#000000",
            "memoryButton" to "#444444",
            "numberButton" to "#333333",
            "operatorButton" to "#555555",
            "clearButton" to "#444444",
            "clearButtonText" to "#FFFFFF",
            "numberButtonText" to "#FFFFFF",
            "operatorButtonText" to "#FFFFFF",
            "memoryButtonText" to "#FFFFFF"
        )
    }


    fun speechCreate() {
        button = findViewById(R.id.voiceBtn)

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                val message = when (error) {
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Ошибка сети, попробуйте снова."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Недостаточно прав доступа для записи."
                    SpeechRecognizer.ERROR_AUDIO -> "Ошибка записи аудио."
                    SpeechRecognizer.ERROR_SERVER -> "Ошибка сервера, попробуйте позже."
                    SpeechRecognizer.ERROR_CLIENT -> "Ошибка клиента."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Время ожидания речи истекло."
                    SpeechRecognizer.ERROR_NO_MATCH -> "Речь не распознана, попробуйте снова."
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят, попробуйте позже."
                    SpeechRecognizer.ERROR_NETWORK -> "Ошибка сети."
                    else -> "Ошибка: $error"
                }
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val recognizedText = matches[0].replace('x', '*')
                    if (isMathExpression(recognizedText)) {
                        inputField.setText(recognizedText)
                    } else {
                        inputField.setText("Ошибка")
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        button.setOnClickListener {
            if (BuildConfig.IS_DEMO) {
                if (isScientificMode) {
                    isScientificMode = false
                    updateCalculatorMode()
                }
                Toast.makeText(this, "Demo version: доступен только обычный режим", Toast.LENGTH_SHORT).show()
            } else {
                if (resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE) {
                    isScientificMode = !isScientificMode
                    updateCalculatorMode()
                } else {
                    if (!isScientificMode) {
                        isScientificMode = true
                        updateCalculatorMode()
                    }
                    Toast.makeText(this, "В горизонтальном режиме включён научный калькулятор.", Toast.LENGTH_SHORT).show()
                }
            }

            startListening()
        }
    }

    fun loadTheme(callback: (String, String, String, String, String, String, String, String, String, String, String) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("themes").document("b7DGBOqfbogQfqQY8hS1").get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentTheme = document.getString("currentTheme") ?: "light"
                    val themeData = document.get(currentTheme) as? Map<*, *>
                    if (themeData != null) {
                        val backgroundColor = themeData["backgroundColor"] as? String ?: "#000000"
                        val bar = themeData["bar"] as? String ?: "#000000"
                        val memoryButton = themeData["memoryButton"] as? String ?: "#000000"
                        val numberButton = themeData["numberButton"] as? String ?: "#000000"
                        val operatorButton = themeData["operatorButton"] as? String ?: "#000000"
                        val clearButton = themeData["clearButton"] as? String ?: "#000000"

                        val clearButtonText = themeData["clearButtonText"] as? String ?: "#000000"
                        val numberButtonText = themeData["numberButtonText"] as? String ?: "#000000"
                        val operatorButtonText = themeData["operatorButtonText"] as? String ?: "#000000"
                        val memoryButtonText = themeData["memoryButtonText"] as? String ?: "#000000"

                        callback(currentTheme, backgroundColor, memoryButton, numberButton, operatorButton, clearButton,
                            clearButtonText, numberButtonText, operatorButtonText, memoryButtonText, bar)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Ошибка загрузки темы", e)
            }
    }

    fun applyTheme(context: Context, mode: String, backgroundColor: String, memoryButtonColor: String, numberButtonColor: String, operatorButtonColor: String, clearButtonColor: String,
                   clearText: String, numberText: String, operatorText: String, memoryText: String, bar: String) {

        val memoryButtons = listOf(
            R.id.buttonMemoryPlus, R.id.buttonMemoryClear, R.id.buttonMemoryMinus, R.id.buttonMemoryRecall, R.id.voiceBtn, R.id.btnShowHistory, R.id.btnChangePass
        )
        val numberButtons = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4, R.id.button5,
            R.id.button6, R.id.button7, R.id.button8, R.id.button9, R.id.buttonEqual, R.id.buttonDot, R.id.inputField
        )
        val operatorButtons = listOf(
            R.id.buttonRightBracket, R.id.buttonLeftBracket, R.id.buttonSin, R.id.buttonCos, R.id.buttonTan, R.id.buttonSqrt, R.id.buttonDelete,
            R.id.buttonDivide, R.id.buttonMultiply, R.id.buttonMinus, R.id.buttonPlus, R.id.buttonModul, R.id.buttonPower, R.id.buttonReverse, R.id.buttonInverse,
        )

        val rootView = (context as Activity).findViewById<View>(android.R.id.content)
        rootView.setBackgroundColor(Color.parseColor(backgroundColor))
        window.statusBarColor = Color.parseColor(bar)

        for (id in memoryButtons) {
            findViewById<TextView>(id).setBackgroundColor(Color.parseColor(memoryButtonColor))
        }
        for (id in numberButtons) {
            findViewById<TextView>(id).setBackgroundColor(Color.parseColor(numberButtonColor))
        }
        for (id in operatorButtons) {
            findViewById<TextView>(id).setBackgroundColor(Color.parseColor(operatorButtonColor))
        }
        findViewById<TextView>(R.id.buttonClear).setBackgroundColor(Color.parseColor(clearButtonColor))

        for (id in memoryButtons) {
            findViewById<TextView>(id).setTextColor(Color.parseColor(memoryText))
        }
        for (id in numberButtons) {
            findViewById<TextView>(id).setTextColor(Color.parseColor(numberText))
        }
        for (id in operatorButtons) {
            findViewById<TextView>(id).setTextColor(Color.parseColor(operatorText))
        }
        findViewById<TextView>(R.id.buttonClear).setTextColor(Color.parseColor(clearText))
        /*val button = (context as Activity).findViewById<Button>(R.id.myButton)
        button.setBackgroundColor(Color.parseColor(buttonColor))
        button.setTextColor(Color.parseColor(textColor))*/
    }

    private fun isMathExpression(text: String): Boolean {
        val regex = Regex("^[0-9+\\-*/().\\s]*(\\b(tan|sin|cos)\\b)?[0-9+\\-*/().\\s]*$")
        return regex.matches(text)
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")// Укажите язык распознавания

        speechRecognizer.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    private fun setupButtons() {
        val buttons = listOf(
            R.id.button0, R.id.button1, R.id.button2, R.id.button3, R.id.button4,
            R.id.button5, R.id.button6, R.id.button7, R.id.button8, R.id.button9,
            R.id.buttonLeftBracket, R.id.buttonRightBracket
        )

        for (id in buttons) {
            findViewById<Button>(id).setOnClickListener {
                inputField.append((it as Button).text)
            }
        }

        val operators = listOf(R.id.buttonPlus, R.id.buttonMinus, R.id.buttonMultiply, R.id.buttonDivide, R.id.buttonPower)
        for (id in operators) {
            findViewById<Button>(id).setOnClickListener {
                printOperator(id)
            }
        }

        val trigFunctions = listOf(R.id.buttonSin, R.id.buttonCos, R.id.buttonTan)
        for (id in trigFunctions) {
            findViewById<Button>(id).setOnClickListener {
                inputField.append("${(it as Button).text}(")
            }
        }

        findViewById<Button>(R.id.buttonDot).setOnClickListener { printDot() }

        findViewById<Button>(R.id.buttonClear).setOnClickListener { inputField.text.clear() }
        findViewById<Button>(R.id.buttonDelete).setOnClickListener { deleteLastCharacter() }
        findViewById<Button>(R.id.buttonEqual).setOnClickListener { viewModel.calculate(inputField.text.toString()) }

        findViewById<Button>(R.id.buttonMemoryPlus).setOnClickListener { viewModel.memoryPlus(inputField.text.toString()) }
        findViewById<Button>(R.id.buttonMemoryMinus).setOnClickListener { viewModel.memoryMinus(inputField.text.toString()) }
        findViewById<Button>(R.id.buttonMemoryRecall).setOnClickListener { viewModel.memoryRecall() }
        findViewById<Button>(R.id.buttonMemoryClear).setOnClickListener { viewModel.memoryClear() }

        findViewById<Button>(R.id.buttonInverse).setOnClickListener { viewModel.applyInverse(inputField.text.toString()) }
        findViewById<Button>(R.id.buttonSqrt).setOnClickListener { viewModel.applySqrt(inputField.text.toString()) }
        findViewById<Button>(R.id.buttonReverse).setOnClickListener { viewModel.applyReverseSign(inputField.text.toString()) }
        findViewById<Button>(R.id.buttonModul).setOnClickListener { viewModel.applyAbsoluteValue(inputField.text.toString()) }

        findViewById<Button>(R.id.btnChangePass).setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            startActivity(intent)
        }
    }

    private fun printOperator(buttonId: Int) {
        val text = inputField.text.toString()
        if (text == "Ошибка" || text == "NaN") {
            inputField.setText("")
        }
        var buttonText = findViewById<Button>(buttonId).text.toString()
        if (buttonText == "^")
        {
            buttonText += "("
        }

        if (text == "") {
            if (buttonText == "-") {
                inputField.setText(buttonText)
            }
            return
        }

        if (text == "-" && buttonText != "-") {
            return
        }

        if (buttonText in listOf("+", "-", "*", "/", "^")) {
            val regex = """[+\-*/^]$""".toRegex()
            val match = regex.find(text)

            if (match != null) {
                if (match.value != buttonText) {
                    inputField.setText(text.substring(0, match.range.first) + buttonText)
                }
            } else {
                inputField.append(buttonText)
            }
        } else {
            inputField.append(buttonText)
        }

        inputField.setSelection(inputField.text.length)
    }

    private fun printDot() {
        val text = inputField.text.toString()
        if (text == "Ошибка" || text == "NaN") {
            inputField.setText("")
        }
        if (text.isEmpty() || text.last() in listOf('+', '-', '*', '/')) {
            inputField.append("0.")
            return
        }

        val functions = listOf("sin(", "cos(", "tan(", "log(", "ln(")

        for (func in functions) {
            if (text.endsWith(func)) {
                return
            }
        }

        val regex = Regex("""\d+\.\d*$""")
        if (!regex.containsMatchIn(text)) {
            inputField.append(".")
        }
    }


    private fun observeViewModel() {
        viewModel.result.observe(this) { result ->
            inputField.setText(result)
        }
        viewModel.memory.observe(this) { memory ->
            debug.text = "Память: $memory"
        }
    }

    private fun deleteLastCharacter() {
        val text = inputField.text.toString()
        if (text.isNotEmpty()) {
            inputField.setText(text.dropLast(1))
        }
    }
}
