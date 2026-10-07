package com.minhasdividas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.minhasdividas.app.ui.DividasViewModel
import com.minhasdividas.app.ui.TelaDividas
import com.minhasdividas.app.ui.theme.MinhasDividasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: DividasViewModel = viewModel()
            val preferencias by vm.preferencias.collectAsStateWithLifecycle()
            // Até as preferências carregarem (milissegundos), só o fundo da janela aparece — evita piscar o tema errado.
            preferencias?.let { prefs ->
                MinhasDividasTheme(tema = prefs.tema) {
                    TelaDividas(vm = vm, preferencias = prefs)
                }
            }
        }
    }
}
