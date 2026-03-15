package com.example.calculatorapp

import android.app.Application
import com.example.calculatorapp.utils.NotificationHelper

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationHelper.init(this)
    }
}
