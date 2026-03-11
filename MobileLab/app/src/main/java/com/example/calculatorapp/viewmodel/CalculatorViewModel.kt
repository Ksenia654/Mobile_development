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

class CalculatorViewModel : ViewModel() {


    private val _result = MutableLiveData<String>()
    val result: LiveData<String> = _result

    private val _memory = MutableLiveData<Double>()
    val memory: LiveData<Double> = _memory

    private val memoryManager = Memory()

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

    fun calculate(expression: String) {
        val resultValue = Calculator.evaluate(expression)
        saveCalculation(expression, resultValue.toString())
        _result.value = resultValue ?: "Ошибка"
    }

    fun memoryPlus(expression: String) {
        val value = Calculator.evaluate(expression)?.toDoubleOrNull()
        if (value != null) {
            memoryManager.add(value)
            _memory.value = memoryManager.get()
        }
    }

    fun memoryMinus(expression: String) {
        val value = Calculator.evaluate(expression)?.toDoubleOrNull()
        if (value != null) {
            memoryManager.subtract(value)
            _memory.value = memoryManager.get()
        }
    }

    fun memoryRecall() {
        _result.value = memoryManager.get().toString()
    }

    fun memoryClear() {
        memoryManager.clear()
        _memory.value = memoryManager.get()
    }

    fun applyInverse(expression: String): String {
        return try {
            val result = 1 / (Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN)
            saveCalculation("1 / " + expression, result.toString())
            _result.value = result.toString()
            result.toString()
        } catch (e: Exception) {
            "Ошибка"
        }
    }

    fun applySqrt(expression: String): String {
        return try {
            val result = sqrt((Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN))
            if (result.isNaN()) throw Exception("Not a Number")
            saveCalculation("sqrt(" + expression, result.toString())
            _result.value = result.toString()
            result.toString()
        } catch (e: Exception) {
            "Ошибка"
        }
    }

    fun applyReverseSign(expression: String): String {
        return try {
            val result = -(Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN)
            saveCalculation("-" + expression, result.toString())
            _result.value = result.toString()
            result.toString()
        } catch (e: Exception) {
            "Ошибка"
        }
    }

    fun applyAbsoluteValue(expression: String): String {
        return try {
            val result = abs((Calculator.evaluate(expression)?.toDoubleOrNull() ?: Double.NaN))
            saveCalculation("abs(" + expression, result.toString())
            _result.value = result.toString()
            result.toString()
        } catch (e: Exception) {
            "Ошибка"
        }
    }
}
