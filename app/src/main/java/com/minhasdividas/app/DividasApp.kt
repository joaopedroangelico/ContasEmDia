package com.minhasdividas.app

import android.app.Application
import com.minhasdividas.app.lembretes.Lembretes

class DividasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Lembretes.criarCanal(this)
        Lembretes.agendar(this)
    }
}
