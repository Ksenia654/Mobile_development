package com.example.calculatorapp.model

class Memory {
    private var memoryValue = 0.0

    fun add(value: Double) {
        memoryValue += value
    }

    fun subtract(value: Double) {
        memoryValue -= value
    }

    fun get(): Double {
        return memoryValue
    }

    fun clear() {
        memoryValue = 0.0
    }
}
