package com.example.calculatorapp.model

import net.objecthunter.exp4j.ExpressionBuilder
import net.objecthunter.exp4j.function.Function
import com.example.calculatorapp.utils.ExpressionUtils
import kotlin.math.*
import java.util.Locale

object Calculator {

    // Функции в градусах
    private val sinDeg = object : Function("sin", 1) {
        override fun apply(vararg args: Double): Double {
            return sin(Math.toRadians(args[0]))
        }
    }

    private val cosDeg = object : Function("cos", 1) {
        override fun apply(vararg args: Double): Double {
            return cos(Math.toRadians(args[0]))
        }
    }

    private val tanDeg = object : Function("tan", 1) {
        override fun apply(vararg args: Double): Double {
            val radians = Math.toRadians(args[0])
            val cosValue = cos(radians)

            // Если косинус слишком близок к нулю — тангенс не существует
            if (abs(cosValue) < 1e-10) {
                return Double.NaN
            }

            return tan(radians)
        }
    }


    fun evaluate(expression: String): String? {
        return try {
            val count = ExpressionUtils.countBracketDifference(expression)
            val exp = if (count > 0) expression + ")".repeat(count) else expression

            val result = ExpressionBuilder(exp)
                .function(sinDeg)
                .function(cosDeg)
                .function(tanDeg)
                .build()
                .evaluate()

            if (result.isNaN()) {
                "Ошибка"
            } else {
                // Округление до 1 знака после запятой
                String.format(Locale.US, "%.1f", result)
            }
        } catch (e: Exception) {
            null
        }
    }
}
