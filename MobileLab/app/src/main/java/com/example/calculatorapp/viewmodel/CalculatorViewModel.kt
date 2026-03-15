package com.example.calculatorapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.calculatorapp.model.Calculator
import com.example.calculatorapp.model.Memory
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.math.sqrt
import kotlin.math.abs
import java.util.Locale

class CalculatorViewModel : ViewModel() {

    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    private val _memory = MutableLiveData<Double>()
    val memory: LiveData<Double> = _memory

    // Универсальное уведомление об ошибке
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private val memoryManager = Memory()

    // Сохранение истории
    fun saveCalculation(expression: String, result: String) {
        val db = FirebaseFirestore.getInstance()
        val historyItem = hashMapOf(
            "expression" to expression,
            "result" to result,
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("history")
            .add(historyItem)
            .addOnSuccessListener {
                Log.d("Firestore", "История сохранена")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Ошибка сохранения", e)
            }
    }

    // Основное вычисление
    fun calculate(expression: String) {
        try {
            val resultValue = Calculator.evaluate(expression)

            if (resultValue == null || resultValue == "Ошибка") {
                _error.value = "Ошибка вычисления"
                _result.value = "Ошибка"
                return
            }

            saveCalculation(expression, resultValue)
            _result.value = resultValue

        } catch (e: Exception) {
            _error.value = "Ошибка вычисления"
            _result.value = "Ошибка"
        }
    }

    // Память +
    fun memoryPlus(expression: String) {
        val value = Calculator.evaluate(expression)?.toDoubleOrNull()
        if (value != null) {
            memoryManager.add(value)
            _memory.value = memoryManager.get()
        } else {
            _error.value = "Ошибка вычисления"
        }
    }

    // Память -
    fun memoryMinus(expression: String) {
        val value = Calculator.evaluate(expression)?.toDoubleOrNull()
        if (value != null) {
            memoryManager.subtract(value)
            _memory.value = memoryManager.get()
        } else {
            _error.value = "Ошибка вычисления"
        }
    }

    fun memoryRecall() {
        _result.value = memoryManager.get().toString()
    }

    fun memoryClear() {
        memoryManager.clear()
        _memory.value = memoryManager.get()
    }

    // 1/x
    fun applyInverse(expression: String): String {
        return try {
            val value = Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN
            val result = 1 / value

            if (result.isNaN() || result.isInfinite()) {
                _error.value = "Ошибка вычисления"
                _result.value = "Ошибка"
                return "Ошибка"
            }

            saveCalculation("1 / $expression", result.toString())
            _result.value = result.toString()
            result.toString()

        } catch (e: Exception) {
            _error.value = "Ошибка вычисления"
            "Ошибка"
        }
    }

    // √x
    fun applySqrt(expression: String): String {
        return try {
            val value = Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN
            val result = sqrt(value)

            if (result.isNaN()) {
                _error.value = "Ошибка вычисления"
                _result.value = "Ошибка"
                return "Ошибка"
            }

            val formatted = String.format(Locale.US, "%.1f", result)
            saveCalculation("sqrt($expression)", formatted)
            _result.value = formatted
            formatted

        } catch (e: Exception) {
            _error.value = "Ошибка вычисления"
            _result.value = "Ошибка"
            "Ошибка"
        }
    }

    // Смена знака
    fun applyReverseSign(expression: String): String {
        return try {
            val value = Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN
            val result = -value

            if (result.isNaN()) {
                _error.value = "Ошибка вычисления"
                return "Ошибка"
            }

            saveCalculation("-($expression)", result.toString())
            _result.value = result.toString()
            result.toString()

        } catch (e: Exception) {
            _error.value = "Ошибка вычисления"
            "Ошибка"
        }
    }

    // Модуль
    fun applyAbsoluteValue(expression: String): String {
        return try {
            val value = Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN
            val result = abs(value)

            if (result.isNaN()) {
                _error.value = "Ошибка вычисления"
                return "Ошибка"
            }

            saveCalculation("abs($expression)", result.toString())
            _result.value = result.toString()
            result.toString()

        } catch (e: Exception) {
            _error.value = "Ошибка вычисления"
            "Ошибка"
        }
    }
}
