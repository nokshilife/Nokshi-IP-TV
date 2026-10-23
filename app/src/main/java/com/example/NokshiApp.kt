package com.example

import android.app.Application
import android.util.Log

class NokshiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d("NokshiApp", "Nokshi IP TV Application Initialized")
    }
}
