package com.l4kt.kofe

import android.app.Application
import com.google.firebase.FirebaseApp
import com.l4kt.kofe.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Application class for Kofe app
 */
class KofeApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        // Initialize Koin for dependency injection
        startKoin {
            androidLogger(Level.ERROR) // Set to ERROR to avoid Koin issues with Kotlin 1.4
            androidContext(this@KofeApplication)
            modules(appModule)
        }
    }
}