package com.example.subscriberapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SubscriberApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        instance = this
    }
    
    companion object {
        lateinit var instance: SubscriberApplication
            private set
    }
}
