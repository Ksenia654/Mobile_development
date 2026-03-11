package com.example.calculatorapp.model

import net.objecthunter.exp4j.ExpressionBuilder
import com.example.calculatorapp.utils.ExpressionUtils

object Calculator {
    fun evaluate(expression: String): String? {
        return try {
            val count = ExpressionUtils.countBracketDifference(expression)
            val exp = if (count > 0) expression + ")".repeat(count) else expression
            val result = ExpressionBuilder(exp).build().evaluate()
            if (result.isNaN()) null else result.toString()
        } catch (e: Exception) {
            null
        }
    }
}
