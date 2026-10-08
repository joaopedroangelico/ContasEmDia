package com.minhasdividas.app

import android.app.Application
import com.minhasdividas.app.data.PreferenciasRepo
import com.minhasdividas.app.lembretes.Lembretes
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DividasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Lembretes.criarCanal(this)
        MainScope().launch {
            val prefs = PreferenciasRepo(this@DividasApp).preferencias.first()
            Lembretes.agendar(this@DividasApp, prefs.minutosAviso)
        }
    }
}
