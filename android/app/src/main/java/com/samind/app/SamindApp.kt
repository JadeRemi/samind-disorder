package com.samind.app

import android.app.Application
import com.samind.app.data.db.AppDatabase

class SamindApp : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }

    companion object {
        lateinit var instance: SamindApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
