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
            val repo = PreferenciasRepo(this@DividasApp)
            repo.limparSaldoAntigo()
            val prefs = repo.preferencias.first()
            Lembretes.agendar(this@DividasApp, prefs.minutosAviso)
        }
    }
}
