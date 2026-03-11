package com.example.calculatorapp.utils

object ExpressionUtils {
    fun countBracketDifference(expression: String): Int {
        var open = 0
        var close = 0
        expression.forEach {
            when (it) {
                '(' -> open++
                ')' -> close++
            }
        }
        return open - close
    }
}
