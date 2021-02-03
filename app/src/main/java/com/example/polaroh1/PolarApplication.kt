package com.example.polaroh1

import android.app.Application
import com.example.polaroh1.repository.RepositoryKit

class PolarApplication:Application() {

    override fun onCreate() {
        super.onCreate()
        RepositoryKit.setup(this)

    }
}